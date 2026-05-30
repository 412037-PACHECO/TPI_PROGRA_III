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
