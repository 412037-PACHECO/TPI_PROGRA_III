package com.tpi.pokemon.game.engine.effect.mapping;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.CardFilterSpec;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind;
import com.tpi.pokemon.game.engine.effect.ability.EffectCondition;
import com.tpi.pokemon.game.engine.effect.ability.EffectScope;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierDefinition;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
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
    private final Map<String, TrainerEffectMapping> trainerMappingsByCardId;
    private final Map<CardAttackKey, AbilityEffectMapping> abilityMappings;
    private final Map<String, List<AbilityEffectMapping>> abilityMappingsByCardId;
    private final Map<String, EnergyEffectMapping> energyMappingsByCardId;
    private final Map<String, List<Xy1AuditEntry>> auditEntriesByCardId;

    public Xy1EffectCatalog() {
        this(defaultAttackMappings(), defaultTrainerMappings(), defaultAbilityMappings(), defaultEnergyMappings(), defaultAuditEntries());
    }

    public Xy1EffectCatalog(List<AttackEffectMapping> attackMappings, List<Xy1AuditEntry> auditEntries) {
        this(attackMappings, List.of(), List.of(), List.of(), auditEntries);
    }

    public Xy1EffectCatalog(List<AttackEffectMapping> attackMappings, List<TrainerEffectMapping> trainerMappings, List<Xy1AuditEntry> auditEntries) {
        this(attackMappings, trainerMappings, List.of(), List.of(), auditEntries);
    }

    public Xy1EffectCatalog(List<AttackEffectMapping> attackMappings, List<TrainerEffectMapping> trainerMappings, List<AbilityEffectMapping> abilityMappings, List<Xy1AuditEntry> auditEntries) {
        this(attackMappings, trainerMappings, abilityMappings, List.of(), auditEntries);
    }

    public Xy1EffectCatalog(List<AttackEffectMapping> attackMappings, List<TrainerEffectMapping> trainerMappings, List<AbilityEffectMapping> abilityMappings, List<EnergyEffectMapping> energyMappings, List<Xy1AuditEntry> auditEntries) {
        this.attackMappings = indexAttackMappings(attackMappings);
        this.mappingsByCardId = indexMappingsByCardId(attackMappings);
        this.trainerMappingsByCardId = indexTrainerMappingsByCardId(trainerMappings);
        this.abilityMappings = indexAbilityMappings(abilityMappings);
        this.abilityMappingsByCardId = indexAbilityMappingsByCardId(abilityMappings);
        this.energyMappingsByCardId = indexEnergyMappingsByCardId(energyMappings);
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

    public Optional<TrainerEffectMapping> trainerMappingForCard(String cardId) {
        if (isBlank(cardId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(trainerMappingsByCardId.get(cardId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public List<EffectDefinition> effectsForTrainer(String cardId) {
        return trainerMappingForCard(cardId)
                .map(TrainerEffectMapping::playEffects)
                .orElse(List.of());
    }

    public List<CardEffectDefinition> continuousEffectsForTrainer(String cardId) {
        return trainerMappingForCard(cardId)
                .map(TrainerEffectMapping::continuousEffects)
                .orElse(List.of());
    }

    public Optional<AbilityEffectMapping> abilityMappingForName(String cardId, String abilityName) {
        if (isBlank(cardId) || isBlank(abilityName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(abilityMappings.get(CardAttackKey.of(cardId, abilityName)));
    }

    public List<AbilityEffectMapping> abilityMappingsForCard(String cardId) {
        if (isBlank(cardId)) {
            return List.of();
        }
        return abilityMappingsByCardId.getOrDefault(cardId.trim().toLowerCase(java.util.Locale.ROOT), List.of());
    }

    public List<CardEffectDefinition> continuousEffectsForPokemon(String cardId) {
        if (isBlank(cardId)) {
            return List.of();
        }
        return abilityMappingsForCard(cardId).stream()
                .flatMap(mapping -> mapping.continuousEffects().stream())
                .toList();
    }

    public Optional<EnergyEffectMapping> energyMappingForCard(String cardId) {
        if (isBlank(cardId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(energyMappingsByCardId.get(cardId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public List<EffectDefinition> effectsForEnergy(String cardId) {
        return energyMappingForCard(cardId)
                .map(EnergyEffectMapping::playEffects)
                .orElse(List.of());
    }

    public List<CardEffectDefinition> continuousEffectsForEnergy(String cardId) {
        return energyMappingForCard(cardId)
                .map(EnergyEffectMapping::continuousEffects)
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

    private static Map<String, TrainerEffectMapping> indexTrainerMappingsByCardId(List<TrainerEffectMapping> mappings) {
        Map<String, TrainerEffectMapping> indexed = new LinkedHashMap<>();
        for (TrainerEffectMapping mapping : mappings) {
            indexed.put(mapping.cardId().toLowerCase(java.util.Locale.ROOT), mapping);
        }
        return Map.copyOf(indexed);
    }

    private static Map<CardAttackKey, AbilityEffectMapping> indexAbilityMappings(List<AbilityEffectMapping> mappings) {
        Map<CardAttackKey, AbilityEffectMapping> indexed = new LinkedHashMap<>();
        for (AbilityEffectMapping mapping : mappings) {
            indexed.put(CardAttackKey.of(mapping.cardId(), mapping.abilityName()), mapping);
            indexed.put(CardAttackKey.of(mapping.cardId(), mapping.abilityId()), mapping);
        }
        return Map.copyOf(indexed);
    }

    private static Map<String, List<AbilityEffectMapping>> indexAbilityMappingsByCardId(List<AbilityEffectMapping> mappings) {
        Map<String, List<AbilityEffectMapping>> indexed = new LinkedHashMap<>();
        for (AbilityEffectMapping mapping : mappings) {
            indexed.computeIfAbsent(mapping.cardId().toLowerCase(java.util.Locale.ROOT), ignored -> new ArrayList<>()).add(mapping);
        }
        return copyListMap(indexed);
    }

    private static Map<String, EnergyEffectMapping> indexEnergyMappingsByCardId(List<EnergyEffectMapping> mappings) {
        Map<String, EnergyEffectMapping> indexed = new LinkedHashMap<>();
        for (EnergyEffectMapping mapping : mappings) {
            indexed.put(mapping.cardId().toLowerCase(java.util.Locale.ROOT), mapping);
        }
        return Map.copyOf(indexed);
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
                        "If no selected energy id is provided, the generic handler discards the first attached energy deterministically."),
                attack("xy1-2", "M Venusaur-EX", "crisis-vine", "Crisis Vine", "Your opponent's Active Pokémon is now Paralyzed and Poisoned.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.APPLY_STATUS), Xy1EffectComplexity.MEDIUM,
                        List.of(
                                EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.PARALYZED, EffectTiming.AFTER_DAMAGE),
                                EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE)),
                        "Base damage remains in AttackDefinition; this mapping applies both listed Special Conditions after damage."),
                attack("xy1-5", "Beedrill", "poison-jab", "Poison Jab", "Your opponent's Active Pokémon is now Poisoned.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.APPLY_STATUS), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE)),
                        "Flash Needle remains pending because it requires variable coin-flip damage and next-turn prevention."),
                attack("xy1-6", "Ledyba", "spinning-attack", "Spinning Attack", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-8", "Volbeat", "signal-beam", "Signal Beam", "Your opponent's Active Pokémon is now Confused.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.APPLY_STATUS), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.CONFUSED, EffectTiming.AFTER_DAMAGE)),
                        "Luring Glow remains pending for a later switch-active mapping/test pass."),
                attack("xy1-14", "Chesnaught", "touchdown", "Touchdown", "Heal 20 damage from this Pokémon.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_HEAL, Xy1EffectCategory.HEAL_DAMAGE), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.healDamage(EffectTarget.ATTACKER_ACTIVE, 20, EffectTiming.AFTER_DAMAGE)),
                        "Only the attack heal is mapped; Spiky Shield remains a pending reactive ability."),
                attack("xy1-15", "Scatterbug", "bug-bite", "Bug Bite", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-16", "Spewpa", "bug-bite", "Bug Bite", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-18", "Skiddo", "tackle", "Tackle", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Lead remains pending because it requires search/reveal/shuffle of a Supporter from deck."),
                attack("xy1-20", "Slugma", "flamethrower", "Flamethrower", "Discard an Energy attached to this Pokémon.",
                        categories(Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.discardAttachedEnergy(EffectTarget.ATTACKER_ACTIVE, 1, List.of(), EffectTiming.AFTER_DAMAGE)),
                        "The text discards any Energy attached to this Pokémon, which is supported by the generic attached-energy discard handler."),
                attack("xy1-21", "Magcargo", "heat-blast", "Heat Blast", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Magma Mantle remains pending because it conditionally discards/checks the top deck card and modifies damage."),
                attack("xy1-22", "Pansear", "live-coal", "Live Coal", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-22", "Pansear", "fireworks", "Fireworks", "Discard an Energy attached to this Pokémon.",
                        categories(Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.discardAttachedEnergy(EffectTarget.ATTACKER_ACTIVE, 1, List.of(), EffectTiming.AFTER_DAMAGE)),
                        "The text discards any Energy attached to this Pokémon, which is supported by the generic attached-energy discard handler."),
                attack("xy1-24", "Fennekin", "will-o-wisp", "Will-O-Wisp", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-27", "Fletchinder", "fire-wing", "Fire Wing", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Flame Charge remains pending because it searches deck, attaches Energy, and shuffles."),
                attack("xy1-29", "Blastoise-EX", "splash-bomb", "Splash Bomb", "Flip a coin. If tails, this Pokémon does 30 damage to itself.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP), Xy1EffectComplexity.MEDIUM,
                        List.of(EffectDefinition.coinFlip(
                                null,
                                EffectDefinition.dealDamage(EffectTarget.ATTACKER_ACTIVE, 30, EffectTiming.AFTER_DAMAGE),
                                EffectTiming.AFTER_DAMAGE)),
                        "Rapid Spin remains pending because it requires coordinated switching for both players."),
                attack("xy1-31", "Shellder", "rain-splash", "Rain Splash", "",
                        categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW,
                        List.of(),
                        "Damage-only attack audited; no EffectDefinition required."),
                attack("xy1-32", "Cloyster", "clamp-crush", "Clamp Crush", "Flip a coin. If heads, your opponent's Active Pokémon is now Paralyzed and discard an Energy attached to that Pokémon.",
                        categories(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.MEDIUM,
                        List.of(EffectDefinition.coinFlip(
                                EffectDefinition.composite(List.of(
                                        EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.PARALYZED, EffectTiming.AFTER_DAMAGE),
                                        EffectDefinition.discardAttachedEnergy(EffectTarget.DEFENDER_ACTIVE, 1, List.of(), EffectTiming.AFTER_DAMAGE)),
                                        EffectTiming.AFTER_DAMAGE),
                                null,
                                EffectTiming.AFTER_DAMAGE)),
                        "Spike Cannon remains pending because it requires variable damage based on five coin flips.")
        );
    }

    private static List<TrainerEffectMapping> defaultTrainerMappings() {
        return List.of(
                trainer("xy1-119", "Hard Charm", "Pokémon Tool", "Any damage done to the Pokémon this card is attached to by an opponent's attack is reduced by 20 (after applying Weakness and Resistance).",
                        categories(Xy1EffectCategory.TOOL_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM,
                        List.of(),
                        List.of(continuousTrainerEffect("hard-charm-damage-reduction", "Hard Charm", EffectSourceKind.TOOL, EffectScope.SELF,
                                new ModifierDefinition(ModifierType.DAMAGE, ModifierOperation.SUBTRACT, ModifierLayer.AFTER_WEAKNESS_RESISTANCE, 20, ModifierTargetRole.DEFENDER))),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Tool attachment rule is handled by TurnActionService; this mapping only declares the continuous damage reduction."),
                trainer("xy1-121", "Muscle Band", "Pokémon Tool", "The attacks of the Pokémon this card is attached to do 20 more damage to your opponent's Active Pokémon (before applying Weakness and Resistance).",
                        categories(Xy1EffectCategory.TOOL_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM,
                        List.of(),
                        List.of(continuousTrainerEffect("muscle-band-damage-bonus", "Muscle Band", EffectSourceKind.TOOL, EffectScope.SELF,
                                new ModifierDefinition(ModifierType.DAMAGE, ModifierOperation.ADD, ModifierLayer.BEFORE_WEAKNESS_RESISTANCE, 20, ModifierTargetRole.ATTACKER))),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Tool attachment rule is handled by TurnActionService; this mapping only declares the continuous attack damage bonus."),
                trainer("xy1-123", "Professor's Letter", "Item", "Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.",
                        categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.SHUFFLE_DECK), Xy1EffectComplexity.MEDIUM,
                        List.of(
                                EffectDefinition.searchDeck(EffectTarget.ACTING_PLAYER, 2, CardFilterSpec.subtype(CardSubtype.BASIC_ENERGY), List.of(), true, true, EffectTiming.ON_PLAY_TRAINER),
                                EffectDefinition.shuffleDeck(EffectTarget.ACTING_PLAYER, EffectTiming.ON_PLAY_TRAINER)),
                        List.of(),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET),
                        false,
                        "Mapping captures search/reveal/shuffle structure, but public hidden-zone selection and reveal privacy remain pending."),
                trainer("xy1-125", "Roller Skates", "Item", "Flip a coin. If heads, draw 3 cards.",
                        categories(Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.coinFlip(
                                EffectDefinition.drawCards(EffectTarget.ACTING_PLAYER, 3, EffectTiming.ON_PLAY_TRAINER),
                                null,
                                EffectTiming.ON_PLAY_TRAINER)),
                        List.of(),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Coin-flip draw maps directly to CoinFlipEffectHandler plus DrawCardsEffectHandler."),
                trainer("xy1-129", "Team Flare Grunt", "Supporter", "Discard an Energy attached to your opponent's Active Pokémon.",
                        categories(Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW,
                        List.of(EffectDefinition.discardAttachedEnergy(EffectTarget.DEFENDER_ACTIVE, 1, List.of(), EffectTiming.ON_PLAY_TRAINER)),
                        List.of(),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Supporter play-limit remains handled by TurnActionService; mapping only discards one attached Energy from opponent's Active Pokémon.")
        );
    }

    private static List<AbilityEffectMapping> defaultAbilityMappings() {
        return List.of(
                ability("xy1-95", "Slurpuff", "sweet-veil", "Sweet Veil", "Each of your Pokémon that has any Fairy Energy attached to it can't be affected by any Special Conditions. (Remove any Special Conditions affecting those Pokémon.)",
                        categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT), Xy1EffectComplexity.HIGH,
                        List.of(new CardEffectDefinition(
                                "sweet-veil-special-condition-prevention",
                                "Sweet Veil",
                                EffectSourceKind.POKEMON_ABILITY,
                                EffectActivationKind.CONTINUOUS,
                                EffectTiming.CONTINUOUS,
                                EffectScope.OWN_POKEMON,
                                EffectCondition.targetHasAttachedEnergyProviding(EnergyType.FAIRY),
                                List.of(new ModifierDefinition(ModifierType.PREVENT_SPECIAL_CONDITION, ModifierOperation.PREVENT, ModifierLayer.PREVENTION, 0, ModifierTargetRole.DEFAULT_TARGET)))),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET),
                        false,
                        "Prevents new Special Conditions for own Pokémon with attached Fairy Energy; continuous cleanup/removal of existing conditions remains pending."),
                ability("xy1-114", "Furfrou", "fur-coat", "Fur Coat", "Any damage done to this Pokémon by attacks is reduced by 20 (after applying Weakness and Resistance).",
                        categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM,
                        List.of(new CardEffectDefinition(
                                "fur-coat-damage-reduction",
                                "Fur Coat",
                                EffectSourceKind.POKEMON_ABILITY,
                                EffectActivationKind.CONTINUOUS,
                                EffectTiming.CONTINUOUS,
                                EffectScope.SELF,
                                EffectCondition.always(),
                                List.of(new ModifierDefinition(ModifierType.DAMAGE, ModifierOperation.SUBTRACT, ModifierLayer.AFTER_WEAKNESS_RESISTANCE, 20, ModifierTargetRole.DEFENDER)))),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Continuous self damage reduction maps directly to ModifierResolver after Weakness/Resistance."),
                ability("xy1-14", "Chesnaught", "spiky-shield", "Spiky Shield", "If this Pokémon is your Active Pokémon and is damaged by an opponent's attack (even if this Pokémon is Knocked Out), put 3 damage counters on the Attacking Pokémon.",
                        categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH,
                        List.of(),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET),
                        false,
                        "Requires a reactive on-damaged-by-attack resolver that can place 3 damage counters on the attacker, even if Chesnaught is Knocked Out."));
    }

    private static List<EnergyEffectMapping> defaultEnergyMappings() {
        return List.of(
                specialEnergy("xy1-130", "Double Colorless Energy", "Double Colorless Energy provides ColorlessColorless Energy.",
                        categories(Xy1EffectCategory.SPECIAL_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW,
                        EnergyProfile.of(EnergyType.COLORLESS, EnergyType.COLORLESS),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED),
                        true,
                        "Structured as two Colorless symbols in EnergyProfile; no EffectDefinition is required."),
                specialEnergy("xy1-131", "Rainbow Energy", "This card provides Colorless Energy. While in play, this card provides every type of Energy but provides only 1 Energy at a time. When you attach this card from your hand to 1 of your Pokémon, put 1 damage counter on that Pokémon.",
                        categories(Xy1EffectCategory.SPECIAL_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY, Xy1EffectCategory.ATTACH_TRIGGER, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH,
                        EnergyProfile.of(EnergyType.COLORLESS),
                        statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET),
                        false,
                        "Static COLORLESS fallback is documented only; full support needs dynamic one-at-a-time typed energy and an on-attach-from-hand damage-counter trigger."),
                basicEnergy("xy1-132", "Grass Energy", EnergyType.GRASS),
                basicEnergy("xy1-133", "Fire Energy", EnergyType.FIRE),
                basicEnergy("xy1-134", "Water Energy", EnergyType.WATER),
                basicEnergy("xy1-135", "Lightning Energy", EnergyType.LIGHTNING),
                basicEnergy("xy1-136", "Psychic Energy", EnergyType.PSYCHIC),
                basicEnergy("xy1-137", "Fighting Energy", EnergyType.FIGHTING),
                basicEnergy("xy1-138", "Darkness Energy", EnergyType.DARKNESS),
                basicEnergy("xy1-139", "Metal Energy", EnergyType.METAL),
                basicEnergy("xy1-140", "Fairy Energy", EnergyType.FAIRY));
    }

    private static EnergyEffectMapping basicEnergy(String cardId, String cardName, EnergyType type) {
        return new EnergyEffectMapping(
                cardId,
                cardName,
                CardSubtype.BASIC_ENERGY,
                "",
                categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY),
                Xy1EffectComplexity.LOW,
                EnergyProfile.basic(type),
                List.of(),
                List.of(),
                statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED),
                true,
                "Basic Energy has no textual effect mapping; behavior is structural through EnergyProfile.");
    }

    private static EnergyEffectMapping specialEnergy(String cardId, String cardName, String ruleText, Set<Xy1EffectCategory> categories, Xy1EffectComplexity complexity, EnergyProfile energyProfile, Set<Xy1AuditStatus> statuses, boolean tested, String notes) {
        return new EnergyEffectMapping(cardId, cardName, CardSubtype.SPECIAL_ENERGY, ruleText, categories, complexity, energyProfile, List.of(), List.of(), statuses, tested, notes);
    }

    private static AbilityEffectMapping ability(String cardId, String cardName, String abilityId, String abilityName, String effectText, Set<Xy1EffectCategory> categories, Xy1EffectComplexity complexity, List<CardEffectDefinition> continuousEffects, Set<Xy1AuditStatus> statuses, boolean tested, String notes) {
        return new AbilityEffectMapping(cardId, cardName, abilityId, abilityName, effectText, categories, complexity, continuousEffects, statuses, tested, notes);
    }

    private static CardEffectDefinition continuousTrainerEffect(String effectId, String name, EffectSourceKind sourceKind, EffectScope scope, ModifierDefinition modifier) {
        return new CardEffectDefinition(effectId, name, sourceKind, EffectActivationKind.CONTINUOUS, EffectTiming.CONTINUOUS, scope, EffectCondition.always(), List.of(modifier));
    }

    private static TrainerEffectMapping trainer(String cardId, String cardName, String subtype, String effectText, Set<Xy1EffectCategory> categories, Xy1EffectComplexity complexity, List<EffectDefinition> playEffects, List<CardEffectDefinition> continuousEffects, Set<Xy1AuditStatus> statuses, boolean tested, String notes) {
        return new TrainerEffectMapping(cardId, cardName, subtype, effectText, categories, complexity, playEffects, continuousEffects, statuses, tested, notes);
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
                audit("xy1-2", "M Venusaur-EX", "Pokémon", "MEGA, EX", "Crisis Vine", "none", "Mega Evolution rule; Pokémon-EX rule", "Crisis Vine applies Paralyzed and Poisoned after damage.", categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.APPLY_STATUS), Xy1EffectComplexity.MEDIUM, true, "ApplySpecialConditionEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.1 maps both Special Conditions; Mega Evolution turn-ending rule is structural/future setup, not an attack effect mapping."),
                audit("xy1-5", "Beedrill", "Pokémon", "Stage 2", "Poison Jab; Flash Needle", "none", "none", "Poison Jab applies Poisoned. Flash Needle flips 3 coins and may prevent next-turn effects.", categories(Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.PREVENT_DAMAGE), Xy1EffectComplexity.HIGH, true, "ApplySpecialConditionEffectHandler partial", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Poison Jab mapped/tested; Flash Needle remains pending."),
                audit("xy1-6", "Ledyba", "Pokémon", "Basic", "Spinning Attack", "none", "none", "Spinning Attack is damage-only.", categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW, true, "AttackService", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Damage-only mapping added in Phase 11E.1."),
                audit("xy1-8", "Volbeat", "Pokémon", "Basic", "Luring Glow; Signal Beam", "none", "none", "Luring Glow switches opponent's Active with Bench. Signal Beam applies Confused.", categories(Xy1EffectCategory.SWITCH_ACTIVE, Xy1EffectCategory.DAMAGE_PLUS_STATUS), Xy1EffectComplexity.MEDIUM, true, "ApplySpecialConditionEffectHandler partial; SwitchActiveEffectHandler available", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Signal Beam mapped/tested; Luring Glow left for later switch-focused pass."),
                audit("xy1-15", "Scatterbug", "Pokémon", "Basic", "Bug Bite", "none", "none", "Bug Bite is damage-only.", categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW, true, "AttackService", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Damage-only mapping added in Phase 11E.1."),
                audit("xy1-18", "Skiddo", "Pokémon", "Basic", "Lead; Tackle", "none", "none", "Lead searches a Supporter from deck. Tackle is damage-only.", categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.MEDIUM, true, "AttackService partial; SearchDeckEffectHandler available", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Tackle mapped/tested; Lead left pending because it needs deck search/reveal/shuffle mapping and selection contract."),
                audit("xy1-20", "Slugma", "Pokémon", "Basic", "Flamethrower", "none", "none", "Flamethrower discards an Energy attached to this Pokémon.", categories(Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW, true, "DiscardAttachedEnergyEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.1 maps self discard of any attached Energy."),
                audit("xy1-21", "Magcargo", "Pokémon", "Stage 1", "Magma Mantle; Heat Blast", "none", "none", "Magma Mantle may discard/check top deck card for more damage. Heat Blast is damage-only.", categories(Xy1EffectCategory.DISCARD_CARD, Xy1EffectCategory.MODIFY_DAMAGE, Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.MEDIUM, true, "AttackService partial", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Heat Blast mapped/tested; Magma Mantle remains pending."),
                audit("xy1-22", "Pansear", "Pokémon", "Basic", "Live Coal; Fireworks", "none", "none", "Live Coal is damage-only. Fireworks discards an Energy attached to this Pokémon.", categories(Xy1EffectCategory.DAMAGE_ONLY, Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW, true, "AttackService; DiscardAttachedEnergyEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Both attacks mapped/tested in Phase 11E.1."),
                audit("xy1-24", "Fennekin", "Pokémon", "Basic", "Will-O-Wisp", "none", "none", "Will-O-Wisp is damage-only.", categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW, true, "AttackService", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Damage-only mapping added in Phase 11E.1."),
                audit("xy1-27", "Fletchinder", "Pokémon", "Stage 1", "Flame Charge; Fire Wing", "none", "none", "Flame Charge searches and attaches Fire Energy from deck. Fire Wing is damage-only.", categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.ATTACH_ENERGY, Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.MEDIUM, true, "AttackService partial; SearchDeckEffectHandler/AttachEnergyEffectHandler available", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Fire Wing mapped/tested; Flame Charge left pending for search+attach+shuffle mapping."),
                audit("xy1-29", "Blastoise-EX", "Pokémon", "Basic, EX", "Rapid Spin; Splash Bomb", "none", "Pokémon-EX rule", "Rapid Spin switches both Active Pokémon. Splash Bomb flips a coin; tails does 30 damage to itself.", categories(Xy1EffectCategory.SWITCH_ACTIVE, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP), Xy1EffectComplexity.MEDIUM, true, "CoinFlipEffectHandler; DealDamageEffectHandler partial", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Splash Bomb mapped/tested; Rapid Spin left pending for coordinated switch mapping."),
                audit("xy1-31", "Shellder", "Pokémon", "Basic", "Rain Splash", "none", "none", "Rain Splash is damage-only.", categories(Xy1EffectCategory.DAMAGE_ONLY), Xy1EffectComplexity.LOW, true, "AttackService", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Damage-only mapping added in Phase 11E.1."),
                audit("xy1-32", "Cloyster", "Pokémon", "Stage 1", "Clamp Crush; Spike Cannon", "none", "none", "Clamp Crush flips a coin; heads Paralyzes and discards an Energy from opponent's Active. Spike Cannon flips 5 coins for variable damage.", categories(Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP, Xy1EffectCategory.DAMAGE_PLUS_STATUS, Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.MEDIUM, true, "CoinFlipEffectHandler; CompositeEffectHandler; ApplySpecialConditionEffectHandler; DiscardAttachedEnergyEffectHandler", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Clamp Crush mapped/tested; Spike Cannon remains pending due variable damage."),
                audit("xy1-115", "Cassius", "Trainer", "Supporter", "none", "none", "Supporter", "Shuffle 1 of your Pokémon and all cards attached to it into your deck.", categories(Xy1EffectCategory.SHUFFLE_DECK, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "none", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires selecting a Pokémon in play, moving it with attachments to deck, board validation, and shuffle."),
                audit("xy1-116", "Evosoda", "Trainer", "Item", "none", "none", "Item", "Search your deck for a card that evolves from 1 of your Pokémon and put it onto that Pokémon. Shuffle your deck afterward.", categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.SHUFFLE_DECK, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "SearchDeckEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires evolution legality checks and putting evolution directly onto a Pokémon."),
                audit("xy1-117", "Fairy Garden", "Trainer", "Stadium", "none", "none", "Stadium", "Each Pokémon that has any Fairy Energy attached to it has no Retreat Cost.", categories(Xy1EffectCategory.STADIUM_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_RETREAT_COST), Xy1EffectComplexity.HIGH, false, "ModifierResolver partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Retreat modifier infrastructure exists, but condition by attached Fairy Energy is not modeled yet."),
                audit("xy1-118", "Great Ball", "Trainer", "Item", "none", "none", "Item", "Look at the top 7 cards of your deck. You may reveal a Pokémon you find there and put it into your hand. Shuffle the other cards back into your deck.", categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.SHUFFLE_DECK, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "SearchDeckEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires top-7 private inspection, optional reveal, and shuffling non-selected cards."),
                audit("xy1-119", "Hard Charm", "Trainer", "Pokémon Tool", "none", "none", "Tool", "Damage done to the attached Pokémon by opponent's attacks is reduced by 20 after Weakness and Resistance.", categories(Xy1EffectCategory.TOOL_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM, true, "CardEffectDefinition; ModifierResolver", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.2 maps the continuous Tool modifier; attachment rules remain structural in TurnActionService."),
                audit("xy1-120", "Max Revive", "Trainer", "Item", "none", "none", "Item", "Put a Pokémon from your discard pile on top of your deck.", categories(Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.MEDIUM, false, "none", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires moving a selected Pokémon from discard to top of deck."),
                audit("xy1-121", "Muscle Band", "Trainer", "Pokémon Tool", "none", "none", "Tool", "Attacks of the attached Pokémon do 20 more damage to opponent's Active before Weakness and Resistance.", categories(Xy1EffectCategory.TOOL_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM, true, "CardEffectDefinition; ModifierResolver", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.2 maps the continuous Tool modifier; attachment rules remain structural in TurnActionService."),
                audit("xy1-122", "Professor Sycamore", "Trainer", "Supporter", "none", "none", "Supporter", "Discard your hand and draw 7 cards.", categories(Xy1EffectCategory.DISCARD_CARD, Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.MEDIUM, false, "DrawCardsEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "DiscardCardsEffectHandler requires explicit selected cards and cannot currently express discard whole hand dynamically."),
                audit("xy1-123", "Professor's Letter", "Trainer", "Item", "none", "none", "Item", "Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.", categories(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.SHUFFLE_DECK), Xy1EffectComplexity.MEDIUM, true, "SearchDeckEffectHandler; ShuffleDeckEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Mapping captures search/reveal/shuffle structure; public hidden-zone selection and reveal privacy remain pending."),
                audit("xy1-124", "Red Card", "Trainer", "Item", "none", "none", "Item", "Your opponent shuffles his or her hand into his or her deck and draws 4 cards.", categories(Xy1EffectCategory.SHUFFLE_DECK, Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "DrawCardsEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires opponent hand-to-deck shuffle with hidden-zone privacy and forced draw."),
                audit("xy1-125", "Roller Skates", "Trainer", "Item", "none", "none", "Item", "Flip a coin. If heads, draw 3 cards.", categories(Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.DAMAGE_PLUS_COIN_FLIP), Xy1EffectComplexity.LOW, true, "CoinFlipEffectHandler; DrawCardsEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.2 maps coin-flip draw."),
                audit("xy1-126", "Shadow Circle", "Trainer", "Stadium", "none", "none", "Stadium", "Each Pokémon that has any Darkness Energy attached to it has no Weakness.", categories(Xy1EffectCategory.STADIUM_EFFECT, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "none", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires continuous weakness suppression by attached Darkness Energy."),
                audit("xy1-127", "Shauna", "Trainer", "Supporter", "none", "none", "Supporter", "Shuffle your hand into your deck. Then, draw 5 cards.", categories(Xy1EffectCategory.DRAW_CARDS, Xy1EffectCategory.DISCARD_CARD, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.MEDIUM, false, "DrawCardsEffectHandler partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Draw handler alone is insufficient because the card must shuffle hand into deck before drawing."),
                audit("xy1-128", "Super Potion", "Trainer", "Item", "none", "none", "Item", "Heal 60 damage from 1 of your Pokémon. If you do, discard an Energy attached to that Pokémon.", categories(Xy1EffectCategory.HEAL_DAMAGE, Xy1EffectCategory.DISCARD_ENERGY, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.MEDIUM, false, "HealDamageEffectHandler; DiscardAttachedEnergyEffectHandler partial", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Requires choosing one own Pokémon and conditional discard only if healing occurs."),
                audit("xy1-129", "Team Flare Grunt", "Trainer", "Supporter", "none", "none", "Supporter", "Discard an Energy attached to your opponent's Active Pokémon.", categories(Xy1EffectCategory.DISCARD_ENERGY), Xy1EffectComplexity.LOW, true, "DiscardAttachedEnergyEffectHandler", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Phase 11E.2 maps attached Energy discard from opponent Active."),
                audit("xy1-14", "Chesnaught", "Pokémon", "Stage 2", "Touchdown", "Spiky Shield", "none", "Spiky Shield reacts when damaged by an opponent's attack; Touchdown heals 20 from this Pokémon.", categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.DAMAGE_PLUS_HEAL, Xy1EffectCategory.CONTINUOUS_EFFECT), Xy1EffectComplexity.HIGH, true, "HealDamageEffectHandler partial for Touchdown only; reactive ability resolver pending", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Touchdown mapped/tested in Phase 11E.1; Spiky Shield ability mapping remains pending because ON_DAMAGE_RECEIVED reactive resolution is not implemented."),
                audit("xy1-95", "Slurpuff", "Pokémon", "Stage 1", "Draining Kiss", "Sweet Veil", "none", "Sweet Veil prevents/removes Special Conditions for own Pokémon with Fairy Energy; Draining Kiss heals 30 from this Pokémon.", categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.DAMAGE_PLUS_HEAL), Xy1EffectComplexity.HIGH, true, "ModifierResolver PREVENT_SPECIAL_CONDITION partial; HealDamageEffectHandler partial", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Sweet Veil prevention of new Special Conditions is mapped/tested for Fairy Energy targets; removing existing Special Conditions remains pending."),
                audit("xy1-114", "Furfrou", "Pokémon", "Basic", "Energy Cutoff", "Fur Coat", "none", "Fur Coat reduces damage done to this Pokémon by attacks by 20 after Weakness and Resistance.", categories(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT, Xy1EffectCategory.MODIFY_DAMAGE), Xy1EffectComplexity.MEDIUM, true, "ModifierResolver", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Fur Coat continuous self damage reduction mapped/tested in Phase 11E.3; Energy Cutoff attack remains outside this ability mapping."),
                audit("xy1-130", "Double Colorless Energy", "Energy", "Special", "none", "none", "Double Colorless Energy provides ColorlessColorless Energy.", "Provides two Colorless Energy while attached.", categories(Xy1EffectCategory.SPECIAL_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED), true, "Mapped structurally as EnergyProfile.of(COLORLESS, COLORLESS); no EffectDefinition is needed."),
                audit("xy1-131", "Rainbow Energy", "Energy", "Special", "none", "none", "Provides Colorless; while in play provides every type but only 1 Energy at a time; when attached from hand put 1 damage counter on that Pokémon.", "Dynamic one-at-a-time typed energy plus attach-from-hand damage counter trigger.", categories(Xy1EffectCategory.SPECIAL_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY, Xy1EffectCategory.ATTACH_TRIGGER, Xy1EffectCategory.CUSTOM_REQUIRED), Xy1EffectComplexity.HIGH, false, "EnergyProfile partial only", true, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET), false, "Do not model as all EnergyTypes in EnergyProfile; current static profile cannot express one-at-a-time dynamic choice or on-attach trigger."),
                audit("xy1-132", "Grass Energy", "Energy", "Basic", "none", "none", "none", "Basic Grass Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides GRASS structurally."),
                audit("xy1-133", "Fire Energy", "Energy", "Basic", "none", "none", "none", "Basic Fire Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides FIRE structurally."),
                audit("xy1-134", "Water Energy", "Energy", "Basic", "none", "none", "none", "Basic Water Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides WATER structurally."),
                audit("xy1-135", "Lightning Energy", "Energy", "Basic", "none", "none", "none", "Basic Lightning Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides LIGHTNING structurally."),
                audit("xy1-136", "Psychic Energy", "Energy", "Basic", "none", "none", "none", "Basic Psychic Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides PSYCHIC structurally."),
                audit("xy1-137", "Fighting Energy", "Energy", "Basic", "none", "none", "none", "Basic Fighting Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides FIGHTING structurally."),
                audit("xy1-138", "Darkness Energy", "Energy", "Basic", "none", "none", "none", "Basic Darkness Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides DARKNESS structurally; does not complete Shadow Circle by itself."),
                audit("xy1-139", "Metal Energy", "Energy", "Basic", "none", "none", "none", "Basic Metal Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides METAL structurally."),
                audit("xy1-140", "Fairy Energy", "Energy", "Basic", "none", "none", "none", "Basic Fairy Energy.", categories(Xy1EffectCategory.BASIC_ENERGY, Xy1EffectCategory.PROVIDES_ENERGY), Xy1EffectComplexity.LOW, true, "EnergyProfile", false, statuses(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED, Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED), true, "No textual mapping needed; provides FAIRY structurally and can satisfy attached-energy conditions such as Sweet Veil.")
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
