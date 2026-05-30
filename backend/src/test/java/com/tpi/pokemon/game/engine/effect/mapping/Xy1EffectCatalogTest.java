package com.tpi.pokemon.game.engine.effect.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.EffectType;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
import java.util.List;
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
        assertThat(catalog.auditEntriesForCard("xy1-117")).singleElement().satisfies(entry -> {
            assertThat(entry.statuses()).contains(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
            assertThat(entry.statuses()).doesNotContain(Xy1AuditStatus.FULLY_TESTED);
            assertThat(entry.tested()).isFalse();
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
        assertThat(catalog.auditEntriesForCard("xy1-95")).singleElement()
                .satisfies(entry -> assertThat(entry.statuses()).contains(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER));
    }
}
