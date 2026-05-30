package com.tpi.pokemon.game.engine.effect.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.EffectType;
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
    void auditDoesNotClaimFullXy1Completion() {
        assertThat(catalog.expectedCardCount()).isEqualTo(146);
        assertThat(catalog.auditedCardCount()).isLessThan(catalog.expectedCardCount());
        assertThat(catalog.isCompleteAudit()).isFalse();
    }

    @Test
    void unsupportedTrainerAndAbilityGapsRemainDocumented() {
        assertThat(catalog.auditEntriesForCard("xy1-123")).singleElement()
                .satisfies(entry -> assertThat(entry.statuses()).contains(Xy1AuditStatus.NOT_IMPLEMENTED_YET));
        assertThat(catalog.auditEntriesForCard("xy1-95")).singleElement()
                .satisfies(entry -> assertThat(entry.statuses()).contains(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER));
    }
}
