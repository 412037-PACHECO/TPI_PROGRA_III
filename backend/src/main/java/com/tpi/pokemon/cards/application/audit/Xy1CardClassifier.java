package com.tpi.pokemon.cards.application.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.game.engine.effect.mapping.AttackEffectMapping;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1AuditStatus;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCatalog;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Xy1CardClassifier {
    private final ObjectMapper objectMapper;
    private final Xy1EffectCatalog effectCatalog;

    public Xy1CardClassifier(ObjectMapper objectMapper, Xy1EffectCatalog effectCatalog) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.effectCatalog = Objects.requireNonNull(effectCatalog, "effectCatalog must not be null");
    }

    public Xy1CardAuditEntry classify(CardEntity card) {
        Objects.requireNonNull(card, "card must not be null");
        List<String> notes = new ArrayList<>();
        List<Xy1AttackAuditEntry> attacks = classifyAttacks(card, notes);
        List<Xy1AbilityAuditEntry> abilities = classifyAbilities(card, notes);
        List<Xy1RuleAuditEntry> rules = classifyRules(card, notes);
        Set<Xy1EffectCategory> categories = unionCategories(attacks, abilities, rules);
        boolean mapped = !effectCatalog.mappingsForCard(card.getCardId()).isEmpty();
        boolean customRequired = attacks.stream().anyMatch(Xy1AttackAuditEntry::customHandlerRequired)
                || abilities.stream().anyMatch(Xy1AbilityAuditEntry::customHandlerRequired)
                || rules.stream().anyMatch(Xy1RuleAuditEntry::customHandlerRequired);
        boolean supported = mapped || (!categories.isEmpty() && categories.stream().allMatch(this::isCoveredByCurrentGenericHandler));
        boolean tested = mapped && effectCatalog.mappingsForCard(card.getCardId()).stream().allMatch(AttackEffectMapping::tested);
        Set<Xy1AuditStatus> statuses = statusesForCard(mapped, supported, tested, customRequired, categories);
        Xy1EffectComplexity complexity = maxComplexity(attacks, abilities, rules);
        if (!mapped) {
            notes.add("No explicit Xy1EffectCatalog mapping for this card yet.");
        }
        if (customRequired) {
            notes.add("At least one effect requires future custom handler or infrastructure.");
        }
        return new Xy1CardAuditEntry(
                card.getCardId(),
                card.getName(),
                card.getSupertype(),
                card.getSubtypes(),
                card.getNumber(),
                attacks,
                abilities,
                rules,
                categories,
                complexity,
                supported,
                mapped,
                customRequired,
                statuses,
                tested,
                notes
        );
    }

    private List<Xy1AttackAuditEntry> classifyAttacks(CardEntity card, List<String> cardNotes) {
        JsonNode attacks = arrayNode(card.getAttacks(), "attacks", cardNotes);
        if (attacks == null) {
            return List.of();
        }
        List<Xy1AttackAuditEntry> entries = new ArrayList<>();
        for (JsonNode attack : attacks) {
            String name = text(attack, "name");
            String damage = text(attack, "damage");
            String text = text(attack, "text");
            Set<Xy1EffectCategory> categories = classifyText(text, false);
            if (!damage.isBlank() && text.isBlank()) {
                categories.add(Xy1EffectCategory.DAMAGE_ONLY);
            }
            if (!damage.isBlank() && categories.contains(Xy1EffectCategory.APPLY_STATUS)) {
                categories.add(Xy1EffectCategory.DAMAGE_PLUS_STATUS);
            }
            if (!damage.isBlank() && categories.contains(Xy1EffectCategory.HEAL_DAMAGE)) {
                categories.add(Xy1EffectCategory.DAMAGE_PLUS_HEAL);
            }
            if (!damage.isBlank() && text.toLowerCase(Locale.ROOT).contains("flip")) {
                categories.add(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP);
            }
            Optional<AttackEffectMapping> mapping = effectCatalog.mappingForAttackName(card.getCardId(), name);
            boolean mapped = mapping.isPresent();
            boolean tested = mapping.map(AttackEffectMapping::tested).orElse(false);
            List<String> handlers = handlersFor(categories);
            boolean supported = mapped || (!categories.isEmpty() && categories.stream().allMatch(this::isCoveredByCurrentGenericHandler));
            boolean customRequired = requiresCustom(categories, text, mapped);
            Set<Xy1AuditStatus> statuses = statusesForEffect(mapped, supported, tested, customRequired, categories);
            List<String> notes = new ArrayList<>();
            if (mapped) {
                notes.add("Explicit mapping exists in Xy1EffectCatalog.");
            } else if (!categories.contains(Xy1EffectCategory.DAMAGE_ONLY)) {
                notes.add("Effect classified but not explicitly mapped yet.");
            }
            if (customRequired) {
                notes.add("Requires future handler/infrastructure before execution can be considered complete.");
            }
            entries.add(new Xy1AttackAuditEntry(name, damage, text, categories, complexityFor(categories), supported, handlers, customRequired, statuses, tested, notes));
        }
        return List.copyOf(entries);
    }

    private List<Xy1AbilityAuditEntry> classifyAbilities(CardEntity card, List<String> cardNotes) {
        JsonNode abilities = arrayNode(card.getAbilities(), "abilities", cardNotes);
        if (abilities == null) {
            return List.of();
        }
        List<Xy1AbilityAuditEntry> entries = new ArrayList<>();
        for (JsonNode ability : abilities) {
            String name = text(ability, "name");
            String type = text(ability, "type");
            String text = text(ability, "text");
            Set<Xy1EffectCategory> categories = classifyText(text, true);
            String normalized = normalize(text);
            if (normalized.contains("once during your turn") || normalized.contains("you may")) {
                categories.add(Xy1EffectCategory.ABILITY_ACTIVATED);
            } else {
                categories.add(Xy1EffectCategory.ABILITY_PASSIVE);
            }
            if (normalized.contains("can't") || normalized.contains("as long as") || normalized.contains("each of your")) {
                categories.add(Xy1EffectCategory.CONTINUOUS_EFFECT);
            }
            List<String> handlers = handlersFor(categories);
            boolean supported = false;
            boolean customRequired = true;
            Set<Xy1AuditStatus> statuses = statusesForEffect(false, supported, false, customRequired, categories);
            entries.add(new Xy1AbilityAuditEntry(name, type, text, categories, complexityFor(categories), supported, handlers, customRequired, statuses, false, List.of("Abilities are classified for audit only; ability execution infrastructure is pending.")));
        }
        return List.copyOf(entries);
    }

    private List<Xy1RuleAuditEntry> classifyRules(CardEntity card, List<String> cardNotes) {
        JsonNode rules = arrayNode(card.getRules(), "rules", cardNotes);
        if (rules == null) {
            return List.of();
        }
        List<Xy1RuleAuditEntry> entries = new ArrayList<>();
        for (JsonNode rule : rules) {
            String text = rule.isTextual() ? rule.asText() : rule.toString();
            Set<Xy1EffectCategory> categories = classifyText(text, false);
            boolean exRule = normalize(text).contains("pokemon-ex") || normalize(text).contains("pokémon-ex");
            boolean supported = exRule;
            boolean customRequired = !exRule && !categories.isEmpty();
            Set<Xy1AuditStatus> statuses = statusesForEffect(false, supported, false, customRequired, categories);
            List<String> notes = exRule ? List.of("Pokémon-EX prize rule is supported structurally through CardSubtype.EX.") : List.of("Rule text is classified for audit only.");
            entries.add(new Xy1RuleAuditEntry(text, categories, complexityFor(categories), supported, handlersFor(categories), customRequired, statuses, false, notes));
        }
        return List.copyOf(entries);
    }

    private JsonNode arrayNode(String json, String fieldName, List<String> notes) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                notes.add(fieldName + " JSON is not an array; field skipped.");
                return null;
            }
            return node;
        } catch (JsonProcessingException e) {
            notes.add(fieldName + " JSON could not be parsed; field skipped.");
            return null;
        }
    }

    private Set<Xy1EffectCategory> classifyText(String text, boolean ability) {
        Set<Xy1EffectCategory> categories = EnumSet.noneOf(Xy1EffectCategory.class);
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return categories;
        }
        if (containsAny(normalized, "poisoned", "burned", "asleep", "paralyzed", "confused")) categories.add(Xy1EffectCategory.APPLY_STATUS);
        if (normalized.contains("heal")) categories.add(Xy1EffectCategory.HEAL_DAMAGE);
        if (normalized.contains("draw")) categories.add(Xy1EffectCategory.DRAW_CARDS);
        if (normalized.contains("search your deck")) categories.add(Xy1EffectCategory.SEARCH_DECK);
        if (normalized.contains("shuffle your hand") || normalized.contains("put") && normalized.contains("hand") && normalized.contains("deck")) categories.add(Xy1EffectCategory.DISCARD_CARD);
        if (normalized.contains("discard") && normalized.contains("energy")) categories.add(Xy1EffectCategory.DISCARD_ENERGY);
        if (normalized.contains("discard") && !normalized.contains("energy")) categories.add(Xy1EffectCategory.DISCARD_CARD);
        if (normalized.contains("switch")) categories.add(Xy1EffectCategory.SWITCH_ACTIVE);
        if (containsAny(normalized, "attach a", "attach an", "attach 1", "attach one", "attach up to", "attach any") && normalized.contains("energy")) categories.add(Xy1EffectCategory.ATTACH_ENERGY);
        if (normalized.contains("move") && normalized.contains("energy")) categories.add(Xy1EffectCategory.MOVE_ENERGY);
        if (normalized.contains("prevent") && normalized.contains("damage")) categories.add(Xy1EffectCategory.PREVENT_DAMAGE);
        if (containsAny(normalized, "more damage", "less damage", "isn't affected by weakness", "isn't affected by resistance", "damage times", "damage counter")) categories.add(Xy1EffectCategory.MODIFY_DAMAGE);
        if (normalized.contains("retreat cost")) categories.add(Xy1EffectCategory.MODIFY_RETREAT_COST);
        if (ability && (normalized.contains("as long as") || normalized.contains("can't") || normalized.contains("each of your"))) categories.add(Xy1EffectCategory.CONTINUOUS_EFFECT);
        return categories;
    }

    private List<String> handlersFor(Set<Xy1EffectCategory> categories) {
        Set<String> handlers = new LinkedHashSet<>();
        if (categories.contains(Xy1EffectCategory.DAMAGE_ONLY)) handlers.add("AttackService");
        if (categories.contains(Xy1EffectCategory.APPLY_STATUS) || categories.contains(Xy1EffectCategory.DAMAGE_PLUS_STATUS)) handlers.add("ApplySpecialConditionEffectHandler");
        if (categories.contains(Xy1EffectCategory.HEAL_DAMAGE) || categories.contains(Xy1EffectCategory.DAMAGE_PLUS_HEAL)) handlers.add("HealDamageEffectHandler");
        if (categories.contains(Xy1EffectCategory.DRAW_CARDS)) handlers.add("DrawCardsEffectHandler");
        if (categories.contains(Xy1EffectCategory.DISCARD_ENERGY)) handlers.add("DiscardAttachedEnergyEffectHandler");
        if (categories.contains(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP)) handlers.add("CoinFlipEffectHandler");
        if (categories.size() > 1) handlers.add("CompositeEffectHandler");
        return List.copyOf(handlers);
    }

    private boolean isCoveredByCurrentGenericHandler(Xy1EffectCategory category) {
        return switch (category) {
            case DAMAGE_ONLY, DAMAGE_PLUS_STATUS, DAMAGE_PLUS_HEAL, DAMAGE_PLUS_COIN_FLIP, APPLY_STATUS, HEAL_DAMAGE, DRAW_CARDS, DISCARD_ENERGY -> true;
            default -> false;
        };
    }

    private boolean requiresCustom(Set<Xy1EffectCategory> categories, String text, boolean mapped) {
        if (categories.stream().anyMatch(category -> !isCoveredByCurrentGenericHandler(category) && category != Xy1EffectCategory.DAMAGE_ONLY)) {
            return true;
        }
        String normalized = normalize(text);
        return !mapped && (normalized.contains("choose") || normalized.contains("up to") || normalized.contains("any way you like") || normalized.contains("shuffle") && normalized.contains("hand") && normalized.contains("deck"));
    }

    private Set<Xy1AuditStatus> statusesForEffect(boolean mapped, boolean supported, boolean tested, boolean customRequired, Set<Xy1EffectCategory> categories) {
        Set<Xy1AuditStatus> statuses = EnumSet.of(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED);
        if (supported) statuses.add(Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER);
        if (mapped) statuses.add(Xy1AuditStatus.EFFECT_MAPPED);
        if (tested) statuses.add(Xy1AuditStatus.FULLY_TESTED);
        if (customRequired) statuses.add(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER);
        if (!mapped && !categories.isEmpty() && (!supported || customRequired)) statuses.add(Xy1AuditStatus.NOT_IMPLEMENTED_YET);
        return Set.copyOf(statuses);
    }

    private Set<Xy1AuditStatus> statusesForCard(boolean mapped, boolean supported, boolean tested, boolean customRequired, Set<Xy1EffectCategory> categories) {
        return statusesForEffect(mapped, supported, tested, customRequired, categories);
    }

    private Xy1EffectComplexity complexityFor(Set<Xy1EffectCategory> categories) {
        if (categories.isEmpty() || categories.equals(Set.of(Xy1EffectCategory.DAMAGE_ONLY))) {
            return Xy1EffectComplexity.LOW;
        }
        if (categories.stream().anyMatch(category -> switch (category) {
            case ABILITY_ACTIVATED, ABILITY_PASSIVE, CONTINUOUS_EFFECT, CUSTOM_REQUIRED, SEARCH_DECK, SWITCH_ACTIVE, ATTACH_ENERGY, MOVE_ENERGY, TOOL_EFFECT, STADIUM_EFFECT, PREVENT_DAMAGE, MODIFY_DAMAGE, MODIFY_RETREAT_COST, UNSUPPORTED_YET -> true;
            default -> false;
        })) {
            return Xy1EffectComplexity.HIGH;
        }
        return categories.size() > 1 || categories.contains(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP) ? Xy1EffectComplexity.MEDIUM : Xy1EffectComplexity.LOW;
    }

    private Xy1EffectComplexity maxComplexity(List<Xy1AttackAuditEntry> attacks, List<Xy1AbilityAuditEntry> abilities, List<Xy1RuleAuditEntry> rules) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(attacks.stream().map(Xy1AttackAuditEntry::complexity), abilities.stream().map(Xy1AbilityAuditEntry::complexity)),
                        rules.stream().map(Xy1RuleAuditEntry::complexity))
                .max(Comparator.comparingInt(Xy1CardClassifier::complexityRank))
                .orElse(Xy1EffectComplexity.LOW);
    }

    private static int complexityRank(Xy1EffectComplexity complexity) {
        return switch (complexity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CUSTOM -> 4;
        };
    }

    private Set<Xy1EffectCategory> unionCategories(List<Xy1AttackAuditEntry> attacks, List<Xy1AbilityAuditEntry> abilities, List<Xy1RuleAuditEntry> rules) {
        Set<Xy1EffectCategory> categories = EnumSet.noneOf(Xy1EffectCategory.class);
        attacks.forEach(entry -> categories.addAll(entry.effectCategories()));
        abilities.forEach(entry -> categories.addAll(entry.effectCategories()));
        rules.forEach(entry -> categories.addAll(entry.effectCategories()));
        return Set.copyOf(categories);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
