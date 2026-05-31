package com.tpi.pokemon.game.engine.effect.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.attack.EnergyCostValidator;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.EffectType;
import com.tpi.pokemon.game.engine.effect.ability.EffectConditionType;
import com.tpi.pokemon.game.engine.effect.ability.EffectScope;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Xy1EffectCatalogTest {
    private final Xy1EffectCatalog catalog = new Xy1EffectCatalog();

    @Test
    void findsMappingByCardIdAndAttackName() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-1", "Poison Powder");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.APPLY_SPECIAL_CONDITION);
            assertThat(effect.target()).isEqualTo(EffectTarget.DEFENDER_ACTIVE);
            assertThat(effect.condition()).isEqualTo(SpecialCondition.POISONED);
            assertThat(effect.timing()).isEqualTo(EffectTiming.AFTER_DAMAGE);
        });
    }

    @Test
    void findsMappingByCardIdAndAttackId() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-1", "jungle-hammer");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.HEAL_DAMAGE);
            assertThat(effect.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(30);
        });
    }

    @Test
    void mappingsForCardReturnsAllMappedAttacksForCard() {
        assertThat(catalog.mappingsForCard("xy1-1"))
                .extracting(AttackEffectMapping::attackName)
                .containsExactlyInAnyOrder("Poison Powder", "Jungle Hammer");
    }

    @Test
    void unknownCardOrAttackReturnsEmptyList() {
        assertThat(catalog.effectsForAttack("xy1-999", "Poison Powder")).isEmpty();
        assertThat(catalog.effectsForAttack("xy1-1", "Unknown Attack")).isEmpty();
    }

    @Test
    void damageOnlyAttackCanBeAuditedWithEmptyEffects() {
        assertThat(catalog.effectsForAttack("xy1-10", "Vine Whip")).isEmpty();
        assertThat(catalog.mappingForAttackName("xy1-10", "Vine Whip")).hasValueSatisfying(mapping -> {
            assertThat(mapping.categories()).contains(Xy1EffectCategory.DAMAGE_ONLY);
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
        });
    }

    @Test
    void healingMappingBuildsExpectedEffectDefinition() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-10", "Leech Seed");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.HEAL_DAMAGE);
            assertThat(effect.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(10);
        });
    }

    @Test
    void drawCardsMappingBuildsExpectedEffectDefinition() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-68", "Filch");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.DRAW_CARDS);
            assertThat(effect.target()).isEqualTo(EffectTarget.ACTING_PLAYER);
            assertThat(effect.amount()).isEqualTo(1);
            assertThat(effect.timing()).isEqualTo(EffectTiming.AFTER_ATTACK);
        });
    }

    @Test
    void coinFlipMappingKeepsConditionalBranchExplicit() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-16", "Stun Spore");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.COIN_FLIP);
            assertThat(effect.headsEffect()).satisfies(heads -> {
                assertThat(heads.type()).isEqualTo(EffectType.APPLY_SPECIAL_CONDITION);
                assertThat(heads.condition()).isEqualTo(SpecialCondition.PARALYZED);
            });
            assertThat(effect.tailsEffect()).isNull();
        });
    }

    @Test
    void discardEnergyMappingUsesGenericHandlerDefinition() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-68", "Rip Claw");

        assertThat(effects).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.COIN_FLIP);
            assertThat(effect.headsEffect()).satisfies(heads -> {
                assertThat(heads.type()).isEqualTo(EffectType.DISCARD_ATTACHED_ENERGY);
                assertThat(heads.target()).isEqualTo(EffectTarget.DEFENDER_ACTIVE);
                assertThat(heads.amount()).isEqualTo(1);
            });
        });
    }

    @Test
    void phase11E1AddsVerifiedPokemonMappingsWithoutClaimingCompleteness() {
        assertThat(catalog.mappingsForCard("xy1-2")).extracting(AttackEffectMapping::attackName).containsExactly("Crisis Vine");
        assertThat(catalog.mappingsForCard("xy1-22")).extracting(AttackEffectMapping::attackName).containsExactlyInAnyOrder("Live Coal", "Fireworks");
        assertThat(catalog.mappingsForCard("xy1-32")).extracting(AttackEffectMapping::attackName).containsExactly("Clamp Crush");
        assertThat(catalog.auditedCardCount()).isLessThan(catalog.expectedCardCount());
        assertThat(catalog.isCompleteAudit()).isFalse();
    }

    @Test
    void multiStatusMappingKeepsBothConditionsExplicit() {
        List<EffectDefinition> effects = catalog.effectsForAttack("xy1-2", "Crisis Vine");

        assertThat(effects).hasSize(2);
        assertThat(effects).extracting(EffectDefinition::type).containsOnly(EffectType.APPLY_SPECIAL_CONDITION);
        assertThat(effects).extracting(EffectDefinition::condition)
                .containsExactlyInAnyOrder(SpecialCondition.PARALYZED, SpecialCondition.POISONED);
        assertThat(effects).extracting(EffectDefinition::target).containsOnly(EffectTarget.DEFENDER_ACTIVE);
    }

    @Test
    void phase11E1StatusMappingsUseExistingSpecialConditionHandler() {
        assertThat(catalog.effectsForAttack("xy1-5", "Poison Jab")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.APPLY_SPECIAL_CONDITION);
            assertThat(effect.condition()).isEqualTo(SpecialCondition.POISONED);
            assertThat(effect.target()).isEqualTo(EffectTarget.DEFENDER_ACTIVE);
        });

        assertThat(catalog.effectsForAttack("xy1-8", "Signal Beam")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.APPLY_SPECIAL_CONDITION);
            assertThat(effect.condition()).isEqualTo(SpecialCondition.CONFUSED);
            assertThat(effect.target()).isEqualTo(EffectTarget.DEFENDER_ACTIVE);
        });
    }

    @Test
    void phase11E1HealingMappingUsesExistingHealHandler() {
        assertThat(catalog.effectsForAttack("xy1-14", "Touchdown")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.HEAL_DAMAGE);
            assertThat(effect.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(20);
        });
    }

    @Test
    void selfDiscardEnergyMappingsTargetAttackerActive() {
        assertThat(catalog.effectsForAttack("xy1-20", "Flamethrower")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.DISCARD_ATTACHED_ENERGY);
            assertThat(effect.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(1);
        });

        assertThat(catalog.effectsForAttack("xy1-22", "Fireworks")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.DISCARD_ATTACHED_ENERGY);
            assertThat(effect.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(1);
        });
    }

    @Test
    void coinFlipSelfDamageMappingUsesTailsBranch() {
        assertThat(catalog.effectsForAttack("xy1-29", "Splash Bomb")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.COIN_FLIP);
            assertThat(effect.headsEffect()).isNull();
            assertThat(effect.tailsEffect()).satisfies(tails -> {
                assertThat(tails.type()).isEqualTo(EffectType.DEAL_DAMAGE);
                assertThat(tails.target()).isEqualTo(EffectTarget.ATTACKER_ACTIVE);
                assertThat(tails.amount()).isEqualTo(30);
            });
        });
    }

    @Test
    void coinFlipCompositeMappingKeepsStatusAndDiscardEnergyTogether() {
        assertThat(catalog.effectsForAttack("xy1-32", "Clamp Crush")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.COIN_FLIP);
            assertThat(effect.headsEffect()).satisfies(heads -> {
                assertThat(heads.type()).isEqualTo(EffectType.COMPOSITE);
                assertThat(heads.children()).hasSize(2);
                assertThat(heads.children()).extracting(EffectDefinition::type)
                        .containsExactly(EffectType.APPLY_SPECIAL_CONDITION, EffectType.DISCARD_ATTACHED_ENERGY);
            });
            assertThat(effect.tailsEffect()).isNull();
        });
    }

    @Test
    void trainerWithoutMappingReturnsEmptyEffects() {
        assertThat(catalog.effectsForTrainer("xy1-999")).isEmpty();
        assertThat(catalog.continuousEffectsForTrainer("xy1-999")).isEmpty();
        assertThat(catalog.trainerMappingForCard("xy1-999")).isEmpty();
    }

    @Test
    void itemCoinFlipDrawMappingBuildsExpectedEffectDefinition() {
        assertThat(catalog.effectsForTrainer("xy1-125")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.COIN_FLIP);
            assertThat(effect.timing()).isEqualTo(EffectTiming.ON_PLAY_TRAINER);
            assertThat(effect.headsEffect()).satisfies(heads -> {
                assertThat(heads.type()).isEqualTo(EffectType.DRAW_CARDS);
                assertThat(heads.target()).isEqualTo(EffectTarget.ACTING_PLAYER);
                assertThat(heads.amount()).isEqualTo(3);
            });
            assertThat(effect.tailsEffect()).isNull();
        });
    }

    @Test
    void itemSearchMappingKeepsRevealShuffleAndBasicEnergyFilterExplicit() {
        assertThat(catalog.effectsForTrainer("xy1-123")).hasSize(2);
        assertThat(catalog.effectsForTrainer("xy1-123").get(0)).satisfies(search -> {
            assertThat(search.type()).isEqualTo(EffectType.SEARCH_DECK);
            assertThat(search.target()).isEqualTo(EffectTarget.ACTING_PLAYER);
            assertThat(search.amount()).isEqualTo(2);
            assertThat(search.sourceZone()).isEqualTo(EffectCardZone.DECK);
            assertThat(search.destinationZone()).isEqualTo(EffectCardZone.HAND);
            assertThat(search.cardFilter().subtype()).isEqualTo(CardSubtype.BASIC_ENERGY);
            assertThat(search.revealSelectedCards()).isTrue();
            assertThat(search.requiresShuffle()).isTrue();
            assertThat(search.timing()).isEqualTo(EffectTiming.ON_PLAY_TRAINER);
        });
        assertThat(catalog.effectsForTrainer("xy1-123").get(1)).satisfies(shuffle -> {
            assertThat(shuffle.type()).isEqualTo(EffectType.SHUFFLE_DECK);
            assertThat(shuffle.target()).isEqualTo(EffectTarget.ACTING_PLAYER);
            assertThat(shuffle.timing()).isEqualTo(EffectTiming.ON_PLAY_TRAINER);
        });
    }

    @Test
    void supporterDiscardEnergyMappingUsesExistingAttachedEnergyHandler() {
        assertThat(catalog.effectsForTrainer("xy1-129")).singleElement().satisfies(effect -> {
            assertThat(effect.type()).isEqualTo(EffectType.DISCARD_ATTACHED_ENERGY);
            assertThat(effect.target()).isEqualTo(EffectTarget.DEFENDER_ACTIVE);
            assertThat(effect.amount()).isEqualTo(1);
            assertThat(effect.timing()).isEqualTo(EffectTiming.ON_PLAY_TRAINER);
        });
    }

    @Test
    void toolContinuousMappingsBuildExpectedModifierDefinitions() {
        assertThat(catalog.continuousEffectsForTrainer("xy1-119")).singleElement().satisfies(effect -> {
            assertThat(effect.effectId()).isEqualTo("hard-charm-damage-reduction");
            assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                assertThat(modifier.type()).isEqualTo(ModifierType.DAMAGE);
                assertThat(modifier.operation()).isEqualTo(ModifierOperation.SUBTRACT);
                assertThat(modifier.layer()).isEqualTo(ModifierLayer.AFTER_WEAKNESS_RESISTANCE);
                assertThat(modifier.amount()).isEqualTo(20);
                assertThat(modifier.targetRole()).isEqualTo(ModifierTargetRole.DEFENDER);
            });
        });

        assertThat(catalog.continuousEffectsForTrainer("xy1-121")).singleElement().satisfies(effect -> {
            assertThat(effect.effectId()).isEqualTo("muscle-band-damage-bonus");
            assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                assertThat(modifier.type()).isEqualTo(ModifierType.DAMAGE);
                assertThat(modifier.operation()).isEqualTo(ModifierOperation.ADD);
                assertThat(modifier.layer()).isEqualTo(ModifierLayer.BEFORE_WEAKNESS_RESISTANCE);
                assertThat(modifier.amount()).isEqualTo(20);
                assertThat(modifier.targetRole()).isEqualTo(ModifierTargetRole.ATTACKER);
            });
        });
    }

    @Test
    void fairyGardenContinuousMappingBuildsExpectedRetreatModifier() {
        assertThat(catalog.continuousEffectsForTrainer("xy1-117")).singleElement().satisfies(effect -> {
            assertThat(effect.effectId()).isEqualTo("fairy-garden-free-retreat");
            assertThat(effect.sourceKind()).isEqualTo(EffectSourceKind.STADIUM);
            assertThat(effect.scope()).isEqualTo(EffectScope.ANY);
            assertThat(effect.condition().type()).isEqualTo(EffectConditionType.TARGET_HAS_ATTACHED_ENERGY_PROVIDING);
            assertThat(effect.condition().energyType()).isEqualTo(EnergyType.FAIRY);
            assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                assertThat(modifier.type()).isEqualTo(ModifierType.RETREAT_COST);
                assertThat(modifier.operation()).isEqualTo(ModifierOperation.SET);
                assertThat(modifier.amount()).isZero();
            });
        });
    }

    @Test
    void shadowCircleContinuousMappingBuildsExpectedWeaknessPrevention() {
        assertThat(catalog.continuousEffectsForTrainer("xy1-126")).singleElement().satisfies(effect -> {
            assertThat(effect.effectId()).isEqualTo("shadow-circle-no-weakness");
            assertThat(effect.sourceKind()).isEqualTo(EffectSourceKind.STADIUM);
            assertThat(effect.scope()).isEqualTo(EffectScope.ANY);
            assertThat(effect.condition().type()).isEqualTo(EffectConditionType.TARGET_HAS_ATTACHED_ENERGY_PROVIDING);
            assertThat(effect.condition().energyType()).isEqualTo(EnergyType.DARKNESS);
            assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                assertThat(modifier.type()).isEqualTo(ModifierType.PREVENT_WEAKNESS);
                assertThat(modifier.operation()).isEqualTo(ModifierOperation.PREVENT);
                assertThat(modifier.layer()).isEqualTo(ModifierLayer.PREVENTION);
                assertThat(modifier.targetRole()).isEqualTo(ModifierTargetRole.DEFENDER);
            });
        });
    }

    @Test
    void abilityMappingLookupFindsFurCoatByCardIdAndAbilityName() {
        assertThat(catalog.abilityMappingForName("xy1-114", "Fur Coat")).hasValueSatisfying(mapping -> {
            assertThat(mapping.cardName()).isEqualTo("Furfrou");
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(mapping.continuousEffects()).singleElement().satisfies(effect -> {
                assertThat(effect.effectId()).isEqualTo("fur-coat-damage-reduction");
                assertThat(effect.sourceKind()).isEqualTo(EffectSourceKind.POKEMON_ABILITY);
                assertThat(effect.scope()).isEqualTo(EffectScope.SELF);
                assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                    assertThat(modifier.type()).isEqualTo(ModifierType.DAMAGE);
                    assertThat(modifier.operation()).isEqualTo(ModifierOperation.SUBTRACT);
                    assertThat(modifier.layer()).isEqualTo(ModifierLayer.AFTER_WEAKNESS_RESISTANCE);
                    assertThat(modifier.amount()).isEqualTo(20);
                    assertThat(modifier.targetRole()).isEqualTo(ModifierTargetRole.DEFENDER);
                });
            });
        });
    }

    @Test
    void abilityWithoutMappingReturnsEmptyEffects() {
        assertThat(catalog.abilityMappingForName("xy1-999", "Unknown Ability")).isEmpty();
        assertThat(catalog.abilityMappingsForCard("xy1-999")).isEmpty();
        assertThat(catalog.continuousEffectsForPokemon("xy1-999")).isEmpty();
    }

    @Test
    void sweetVeilAbilityKeepsFairyEnergyConditionExplicit() {
        assertThat(catalog.abilityMappingForName("xy1-95", "Sweet Veil")).hasValueSatisfying(mapping -> {
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(mapping.statuses()).doesNotContain(Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(mapping.continuousEffects()).singleElement().satisfies(effect -> {
                assertThat(effect.scope()).isEqualTo(EffectScope.OWN_POKEMON);
                assertThat(effect.condition().type()).isEqualTo(EffectConditionType.TARGET_HAS_ATTACHED_ENERGY_PROVIDING);
                assertThat(effect.condition().energyType()).isEqualTo(EnergyType.FAIRY);
                assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                    assertThat(modifier.type()).isEqualTo(ModifierType.PREVENT_SPECIAL_CONDITION);
                    assertThat(modifier.operation()).isEqualTo(ModifierOperation.PREVENT);
                });
            });
        });
    }

    @Test
    void attachedEnergyConditionMatchesOnlyWhenTargetHasFairyEnergy() {
        var sweetVeil = catalog.continuousEffectsForPokemon("xy1-95").get(0);
        PokemonInPlay withFairyEnergy = pokemon("target").withAttachedEnergy(energy("fairy", EnergyType.FAIRY));
        PokemonInPlay withWaterEnergy = pokemon("target-2").withAttachedEnergy(energy("water", EnergyType.WATER));

        assertThat(sweetVeil.condition().matches(withFairyEnergy)).isTrue();
        assertThat(sweetVeil.condition().matches(withWaterEnergy)).isFalse();
    }

    @Test
    void spikyShieldIsMappedAsReactiveOnDamageReceivedAbility() {
        assertThat(catalog.abilityMappingForName("xy1-14", "Spiky Shield")).hasValueSatisfying(mapping -> {
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(mapping.statuses()).doesNotContain(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(mapping.tested()).isTrue();
            assertThat(mapping.continuousEffects()).singleElement().satisfies(effect -> {
                assertThat(effect.activationKind()).isEqualTo(com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind.REACTIVE);
                assertThat(effect.timing()).isEqualTo(EffectTiming.ON_DAMAGE_RECEIVED);
                assertThat(effect.scope()).isEqualTo(EffectScope.OWN_ACTIVE);
                assertThat(effect.modifiers()).singleElement().satisfies(modifier -> {
                    assertThat(modifier.type()).isEqualTo(ModifierType.PLACE_DAMAGE_COUNTERS);
                    assertThat(modifier.operation()).isEqualTo(ModifierOperation.ADD);
                    assertThat(modifier.amount()).isEqualTo(3);
                    assertThat(modifier.targetRole()).isEqualTo(ModifierTargetRole.ATTACKER);
                });
            });
        });
    }

    @Test
    void basicEnergyCardsAreAuditedWithoutTextualEffects() {
        assertThat(catalog.energyMappingForCard("xy1-132")).hasValueSatisfying(mapping -> {
            assertThat(mapping.cardName()).isEqualTo("Grass Energy");
            assertThat(mapping.subtype()).isEqualTo(CardSubtype.BASIC_ENERGY);
            assertThat(mapping.energyProfile().provides()).containsExactly(EnergyType.GRASS);
            assertThat(mapping.playEffects()).isEmpty();
            assertThat(mapping.continuousEffects()).isEmpty();
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER, Xy1AuditStatus.FULLY_TESTED);
        });

        assertThat(catalog.energyMappingForCard("xy1-140")).hasValueSatisfying(mapping -> {
            assertThat(mapping.cardName()).isEqualTo("Fairy Energy");
            assertThat(mapping.subtype()).isEqualTo(CardSubtype.BASIC_ENERGY);
            assertThat(mapping.energyProfile().provides()).containsExactly(EnergyType.FAIRY);
            assertThat(mapping.playEffects()).isEmpty();
            assertThat(mapping.continuousEffects()).isEmpty();
        });
    }

    @Test
    void energyWithoutMappingOrTextualEffectReturnsEmptyEffects() {
        assertThat(catalog.effectsForEnergy("xy1-132")).isEmpty();
        assertThat(catalog.continuousEffectsForEnergy("xy1-132")).isEmpty();
        assertThat(catalog.energyMappingForCard("xy1-999")).isEmpty();
        assertThat(catalog.effectsForEnergy("xy1-999")).isEmpty();
        assertThat(catalog.continuousEffectsForEnergy("xy1-999")).isEmpty();
    }

    @Test
    void doubleColorlessEnergyProvidesTwoColorlessEnergy() {
        assertThat(catalog.energyMappingForCard("xy1-130")).hasValueSatisfying(mapping -> {
            assertThat(mapping.cardName()).isEqualTo("Double Colorless Energy");
            assertThat(mapping.subtype()).isEqualTo(CardSubtype.SPECIAL_ENERGY);
            assertThat(mapping.energyProfile().provides()).containsExactly(EnergyType.COLORLESS, EnergyType.COLORLESS);
            assertThat(mapping.playEffects()).isEmpty();
            assertThat(mapping.continuousEffects()).isEmpty();
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
        });
    }

    @Test
    void doubleColorlessEnergyPaysTwoColorlessCost() {
        EnergyEffectMapping mapping = catalog.energyMappingForCard("xy1-130").orElseThrow();
        PokemonInPlay attacker = pokemon("attacker")
                .withAttachedEnergy(specialEnergy("dce", "Double Colorless Energy", mapping.energyProfile()));
        AttackDefinition attack = new AttackDefinition("two-colorless", "Two Colorless", List.of(EnergyType.COLORLESS, EnergyType.COLORLESS), 20);

        assertThat(new EnergyCostValidator().hasEnoughEnergy(attacker, attack)).isTrue();
    }

    @Test
    void rainbowEnergyMapsDynamicSingleSymbolAndAttachTrigger() {
        assertThat(catalog.energyMappingForCard("xy1-131")).hasValueSatisfying(mapping -> {
            assertThat(mapping.cardName()).isEqualTo("Rainbow Energy");
            assertThat(mapping.subtype()).isEqualTo(CardSubtype.SPECIAL_ENERGY);
            assertThat(mapping.energyProfile().provides()).containsExactly(EnergyType.COLORLESS);
            assertThat(mapping.energyProfile().providesAnyTypeWhileAttached()).isTrue();
            assertThat(mapping.energyProfile().attachDamageCountersFromHand()).isEqualTo(1);
            assertThat(mapping.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(mapping.statuses()).doesNotContain(Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(mapping.tested()).isTrue();
        });
    }

    @Test
    void rainbowEnergyCanSatisfyAttachedDarknessAndFairyEnergyConditions() {
        EnergyEffectMapping rainbow = catalog.energyMappingForCard("xy1-131").orElseThrow();
        PokemonInPlay withRainbow = pokemon("rainbow-target")
                .withAttachedEnergy(specialEnergy("rainbow-instance", "Rainbow Energy", rainbow.energyProfile()));

        assertThat(catalog.continuousEffectsForPokemon("xy1-95").get(0).condition().matches(withRainbow)).isTrue();
        assertThat(catalog.continuousEffectsForTrainer("xy1-126").get(0).condition().matches(withRainbow)).isTrue();
    }

    @Test
    void attachedEnergyConditionStillMatchesBasicFairyEnergyAfterEnergyMappings() {
        var sweetVeil = catalog.continuousEffectsForPokemon("xy1-95").get(0);
        EnergyEffectMapping fairyEnergy = catalog.energyMappingForCard("xy1-140").orElseThrow();
        PokemonInPlay withFairyEnergy = pokemon("target-3")
                .withAttachedEnergy(basicEnergy("xy1-140-instance", "Fairy Energy", fairyEnergy.energyProfile()));

        assertThat(sweetVeil.condition().matches(withFairyEnergy)).isTrue();
    }

    @Test
    void phase11E4AddsEnergyMappingsWithoutBreakingPreviousXy1Mappings() {
        assertThat(catalog.effectsForAttack("xy1-1", "Poison Powder")).isNotEmpty();
        assertThat(catalog.effectsForTrainer("xy1-125")).isNotEmpty();
        assertThat(catalog.abilityMappingForName("xy1-114", "Fur Coat")).isPresent();
        assertThat(catalog.energyMappingForCard("xy1-130")).isPresent();
        assertThat(catalog.expectedCardCount()).isEqualTo(146);
        assertThat(catalog.isCompleteAudit()).isFalse();
    }

    @Test
    void pendingTrainerMappingsDoNotClaimFullyTested() {
        assertThat(catalog.auditEntriesForCard("xy1-127")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.tested()).isFalse();
        });
        assertThat(catalog.auditEntriesForCard("xy1-123")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.tested()).isFalse();
        });
    }

    @Test
    void phase11G1CriticalEffectAuditEntriesClaimTestedOnlyForClosedCards() {
        assertThat(catalog.auditEntriesForCard("xy1-95")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.tested()).isTrue();
        });
        assertThat(catalog.auditEntriesForCard("xy1-126")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.tested()).isTrue();
        });
        assertThat(catalog.auditEntriesForCard("xy1-131")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.tested()).isTrue();
        });
    }

    @Test
    void auditDoesNotClaimFullXy1Completion() {
        assertThat(catalog.expectedCardCount()).isEqualTo(146);
        assertThat(catalog.auditedCardCount()).isLessThan(catalog.expectedCardCount());
        assertThat(catalog.isCompleteAudit()).isFalse();
    }

    @Test
    void unsupportedTrainerAndAbilityGapsRemainDocumented() {
        assertThat(catalog.auditEntriesForCard("xy1-123")).singleElement()
                .satisfies(entry -> assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.NOT_IMPLEMENTED_YET));
        assertThat(catalog.auditEntriesForCard("xy1-14")).singleElement()
                .satisfies(entry -> assertThat(entry.statuses()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED));
    }

    private static PokemonInPlay pokemon(String id) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                "Pokemon " + id,
                CardSupertype.POKEMON,
                Set.of(CardSubtype.BASIC));
        return new PokemonInPlay(new CardInstance(new CardInstanceId(id), definition, new PlayerId("p1")), AttachedCards.empty());
    }

    private static CardInstance energy(String id, EnergyType type) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                type + " Energy",
                CardSupertype.ENERGY,
                Set.of(CardSubtype.BASIC_ENERGY),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.basic(type));
        return new CardInstance(new CardInstanceId(id), definition, new PlayerId("p1"));
    }

    private static CardInstance basicEnergy(String id, String name, EnergyProfile profile) {
        return energyCard(id, name, CardSubtype.BASIC_ENERGY, profile);
    }

    private static CardInstance specialEnergy(String id, String name, EnergyProfile profile) {
        return energyCard(id, name, CardSubtype.SPECIAL_ENERGY, profile);
    }

    private static CardInstance energyCard(String id, String name, CardSubtype subtype, EnergyProfile profile) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                name,
                CardSupertype.ENERGY,
                Set.of(subtype),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                profile);
        return new CardInstance(new CardInstanceId(id), definition, new PlayerId("p1"));
    }
}
