package com.tpi.pokemon.game.engine.effect.mapping;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Xy1EffectCatalog {
    public static final String SET_ID = "xy1";
    public static final int EXPECTED_CARD_COUNT = 146;

    private final Map<CardAttackKey, AttackEffectMapping> attackMappings;
    private final Map<String, List<AttackEffectMapping>> mappingsByCardId;
    private final Map<String, List<Xy1AuditEntry>> auditEntriesByCardId;

    public Xy1EffectCatalog() {
        this(defaultAttackMappings(), defaultAuditEntries());
    }

    public Xy1EffectCatalog(List<AttackEffectMapping> attackMappings, List<Xy1AuditEntry> auditEntries) {
        this.attackMappings = indexAttackMappings(attackMappings);
        this.mappingsByCardId = indexMappingsByCardId(attackMappings);
        this.auditEntriesByCardId = indexAuditEntriesByCardId(auditEntries);
    }

    public Optional<AttackEffectMapping> mappingForAttackName(String cardId, String attackName) {
        if (isBlank(cardId) || isBlank(attackName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(attackMappings.get(CardAttackKey.of(cardId, attackName)));
    }

    public Optional<AttackEffectMapping> mappingForAttackId(String cardId, String attackId) {
        if (isBlank(cardId) || isBlank(attackId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(attackMappings.get(CardAttackKey.of(cardId, attackId)));
    }

    public List<AttackEffectMapping> mappingsForCard(String cardId) {
        if (isBlank(cardId)) {
            return List.of();
        }
        return mappingsByCardId.getOrDefault(cardId.trim().toLowerCase(java.util.Locale.ROOT), List.of());
    }

    public List<EffectDefinition> effectsForAttack(String cardId, String attackNameOrId) {
        if (isBlank(cardId) || isBlank(attackNameOrId)) {
            return List.of();
        }
        return Optional.ofNullable(attackMappings.get(CardAttackKey.of(cardId, attackNameOrId)))
                .map(AttackEffectMapping::effects)
                .orElse(List.of());
    }

    public List<Xy1AuditEntry> auditEntriesForCard(String cardId) {
        if (isBlank(cardId)) {
            return List.of();
        }
        return auditEntriesByCardId.getOrDefault(cardId.trim().toLowerCase(java.util.Locale.ROOT), List.of());
    }

    public boolean isCompleteAudit() {
        return false;
    }

    public int expectedCardCount() {
        return EXPECTED_CARD_COUNT;
    }

    public int auditedCardCount() {
        return auditEntriesByCardId.size();
    }

    private static Map<CardAttackKey, AttackEffectMapping> indexAttackMappings(List<AttackEffectMapping> mappings) {
        Map<CardAttackKey, AttackEffectMapping> indexed = new LinkedHashMap<>();
        for (AttackEffectMapping mapping : mappings) {
            indexed.put(CardAttackKey.of(mapping.cardId(), mapping.attackName()), mapping);
            indexed.put(CardAttackKey.of(mapping.cardId(), mapping.attackId()), mapping);
        }
        return Map.copyOf(indexed);
    }

    private static Map<String, List<AttackEffectMapping>> indexMappingsByCardId(List<AttackEffectMapping> mappings) {
        Map<String, List<AttackEffectMapping>> indexed = new LinkedHashMap<>();
        for (AttackEffectMapping mapping : mappings) {
            indexed.computeIfAbsent(mapping.cardId().toLowerCase(java.util.Locale.ROOT), ignored -> new ArrayList<>()).add(mapping);
        }
        return copyListMap(indexed);
    }

    private static Map<String, List<Xy1AuditEntry>> indexAuditEntriesByCardId(List<Xy1AuditEntry> entries) {
        Map<String, List<Xy1AuditEntry>> indexed = new LinkedHashMap<>();
        for (Xy1AuditEntry entry : entries) {
            indexed.computeIfAbsent(entry.cardId().toLowerCase(java.util.Locale.ROOT), ignored -> new ArrayList<>()).add(entry);
        }
        return copyListMap(indexed);
    }

    private static <T> Map<String, List<T>> copyListMap(Map<String, List<T>> source) {
        Map<String, List<T>> copied = new LinkedHashMap<>();
        source.forEach((key, value) -> copied.put(key, List.copyOf(value)));
        return Map.copyOf(copied);
    }

    private static List<AttackEffectMapping> defaultAttackMappings() {
        return List.of(
                attack("xy1-1", "Venusaur-EX", "poison-powder", "Poison Powder", "Your opponent's Active Pokémon is now Poisoned.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.APPLY_STATUS), Xy1EffectComplexity.MEDIUM,
                        List.of(EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE)),
                        "Base damage remains in AttackDefinition; this mapping only applies the secondary Poisoned condition."),
                attack("xy1-1", "Venusaur-EX", "jungle-hammer", "Jungle Hammer", "Heal 30 damage from this Pokémon.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_HEAL, Xy1EffectCategory.HEAL_DAMAGE), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.healDamage(EffectTarget.ATTACKER_ACTIVE, 30, EffectTiming.AFTER_DAMAGE)),
                        "Base damage remains in AttackDefinition; this mapping only heals the attacker."),
                attack("xy1-10", "Pansage", "vine-whip", "Vine Whip", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-10", "Pansage", "leech-seed", "Leech Seed", "Heal 10 damage from this Pokémon.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_HEAL, Xy1EffectCategory.HEAL_DAMAGE), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.healDamage(EffectTarget.ATTACKER_ACTIVE, 10, EffectTiming.AFTER_DAMAGE)),
                        "Base damage remains in AttackDefinition; this mapping only heals the attacker."),
                attack("xy1-16", "Spewpa", "stun-spore", "Stun Spore", "Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DAMAGE_PLUS_STATUS), Xy1EffectComplexity.MEDIUM,
                        List.of(EffectDefinition.coinFlip(
                                EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.PARALYZED, EffectTiming.AFTER_DAMAGE),
                                null,
                                EffectTiming.AFTER_DAMAGE)),
                        "Coin flip delegates the heads branch to ApplySpecialConditionEffectHandler; tails has no secondary effect."),
                attack("xy1-68", "Sableye", "filch", "Filch", "Draw a card.",
                        categories(Xy1EffectCategory.DRAW_CARDS), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.drawCards(EffectTarget.ACTING_PLAYER, 1, EffectTiming.AFTER_ATTACK)),
                        "Attack has no base damage; draw effect is executed after attack declaration/resolution."),
                attack("xy1-68", "Sableye", "rip-claw", "Rip Claw", "Flip a coin. If heads, discard an Energy attached to your opponent's Active Pokémon.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.MEDIUM,
                        List.of(EffectDefinition.coinFlip(
                                EffectDefinition.discardAttachedEnergy(EffectTarget.DEFENDER_ACTIVE, 1, List.of(), EffectTiming.AFTER_DAMAGE),
                                null,
                                EffectTiming.AFTER_DAMAGE)),
                        "If no selected energy id is provided, the generic handler discards the first attached energy deterministically.")
        );
    }

    private static AttackEffectMapping attack(String cardId, String cardName, String attackId, String attackName, String effectText, Set<Xy1EffectCategory> categories, Xy1EffectComplexity complexity, List<EffectDefinition> effects, String notes) {
        return new AttackEffectMapping(
                cardId,
                cardName,
                attackId,
                attackName,
                effectText,
                categories,
                complexity,
                effects,
                statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                true,
                notes
        );
    }

    private static List<Xy1AuditEntry> defaultAuditEntries() {
        return List.of(
                audit("xy1-1", "Venusaur-EX", "Pokémon", "Basic, EX", "Poison Powder; Jungle Hammer", "none", "Pokémon-EX rule", "Poison Powder applies Poisoned; Jungle Hammer heals 30 from this Pokémon.", categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.DAMAGE_PLUS_HEAL), Xy1EffectComplexity.MEDIUM, true, "ApplySpecialConditionEffectHandler; HealDamageEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Representative mapped Pokémon-EX attack effects; EX prize rule already handled structurally through subtype EX."),
                audit("xy1-10", "Pansage", "Pokémon", "Basic", "Vine Whip; Leech Seed", "none", "none", "Vine Whip is damage-only; Leech Seed heals 10 from this Pokémon.", categories(Xy1EffectCategory.DAMAGE_ONLY, Xy1EffectCategory.DAMAGE_PLUS_HEAL), Xy1EffectComplexity.LOW, true, "AttackService; HealDamageEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Includes a damage-only mapped empty effect list to prove unmapped effects are not required for base damage."),
                audit("xy1-16", "Spewpa", "Pokémon", "Stage 1", "Bug Bite; Stun Spore", "none", "none", "Stun Spore flips a coin; heads Paralyzes opponent's Active Pokémon.", categories(Xy1EffectCategory.DAMAGE_ONLY, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DAMAGE_PLUS_STATUS), Xy1EffectComplexity.MEDIUM, true, "CoinFlipEffectHandler; ApplySpecialConditionEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Representative coin flip + status mapping."),
                audit("xy1-68", "Sableye", "Pokémon", "Basic", "Filch; Rip Claw", "none", "none", "Filch draws a card. Rip Claw flips a coin; heads discards an Energy from opponent's Active Pokémon.", categories(Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.MEDIUM, true, "DrawCardsEffectHandler; CoinFlipEffectHandler; DiscardAttachedEnergyEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Representative draw and discard-energy mappings."),
                audit("xy1-123", "Professor's Letter", "Trainer", "Item", "none", "none", "Item", "Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.", categories(Xy1EffectCategory.SEARCH_DECK), Xy1EffectComplexity.MEDIUM, false, "none", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires SearchDeck + reveal + shuffle support and privacy-aware selection from deck."),
                audit("xy1-127", "Shauna", "Trainer", "Supporter", "none", "none", "Supporter", "Shuffle your hand into your deck. Then, draw 5 cards.", categories(Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.DISCARD_CARD, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.MEDIUM, false, "DrawCardsEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Draw handler alone is insufficient because the card must shuffle hand into deck before drawing."),
                audit("xy1-14", "Chesnaught", "Pokémon", "Stage 2", "Touchdown", "Spiky Shield", "none", "Spiky Shield reacts when damaged by an opponent's attack; Touchdown heals 20 from this Pokémon.", categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.DAMAGE_PLUS_HEAL, Xy1EffectCategory.CONTINUOUS_EFFECT), Xy1EffectComplexity.HIGH, false, "HealDamageEffectHandler partial for Touchdown only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Reactive ability timing is not supported by current attack effect timing."),
                audit("xy1-95", "Slurpuff", "Pokémon", "Stage 1", "Draining Kiss", "Sweet Veil", "none", "Sweet Veil prevents/removes Special Conditions for own Pokémon with Fairy Energy; Draining Kiss heals 30 from this Pokémon.", categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.DAMAGE_PLUS_HEAL), Xy1EffectComplexity.HIGH, false, "HealDamageEffectHandler partial for Draining Kiss only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Continuous prevention/removal ability requires future persistent/continuous effect support.")
        );
    }

    private static Xy1AuditEntry audit(String cardId, String name, String supertype, String subtypes, String attacks, String abilities, String rules, String effectText, Set<Xy1EffectCategory> categories, Xy1EffectComplexity complexity, boolean supported, String handlers, boolean customRequired, Set<Xy1AuditStatus> statuses, boolean tested, String notes) {
        return new Xy1AuditEntry(cardId, name, supertype, subtypes, attacks, abilities, rules, effectText, categories, complexity, supported, handlers, customRequired, statuses, tested, notes);
    }

    private static Set<Xy1EffectCategory> categories(Xy1EffectCategory first, Xy1EffectCategory... rest) {
        EnumSet<Xy1EffectCategory> set = EnumSet.of(first, rest);
        return Set.copyOf(set);
    }

    private static Set<Xy1AuditStatus> statuses(Xy1AuditStatus first, Xy1AuditStatus... rest) {
        EnumSet<Xy1AuditStatus> set = EnumSet.of(first, rest);
        return Set.copyOf(set);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
