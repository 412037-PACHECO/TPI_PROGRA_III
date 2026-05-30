package com.tpi.pokemon.cards.application.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1AuditStatus;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCatalog;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCategory;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectComplexity;
import java.util.List;
import org.junit.jupiter.api.Test;

class Xy1AuditReportGeneratorTest {
    private final Xy1EffectCatalog effectCatalog = new Xy1EffectCatalog();
    private final Xy1CardClassifier classifier = new Xy1CardClassifier(new ObjectMapper(), effectCatalog);
    private final Xy1AuditReportGenerator generator = new Xy1AuditReportGenerator(classifier);

    @Test
    void auditCardWithoutAttacksDoesNotFail() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-100", "100", "No Attack", "Pokémon", "Basic", null, null, null));

        assertThat(entry.attacks()).isEmpty();
        assertThat(entry.abilities()).isEmpty();
        assertThat(entry.rules()).isEmpty();
        assertThat(entry.implementationStatus()).contains(Xy1AuditStatus.DATA_IMPORTED, Xy1AuditStatus.EFFECT_CLASSIFIED);
    }

    @Test
    void auditCardWithBaseDamageAttackDetectsDamageOnly() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-101", "101", "Damage Only", "Pokémon", "Basic", attacks("Tackle", "30", ""), null, null));

        assertThat(entry.attacks()).singleElement().satisfies(attack -> {
            assertThat(attack.effectCategories()).contains(Xy1EffectCategory.DAMAGE_ONLY);
            assertThat(attack.supportedByCurrentEngine()).isTrue();
            assertThat(attack.complexity()).isEqualTo(Xy1EffectComplexity.LOW);
        });
    }

    @Test
    void auditCardWithConditionAttackDetectsStatusCategories() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-102", "102", "Poisoner", "Pokémon", "Basic", attacks("Poison Sting", "20", "Your opponent's Active Pokémon is now Poisoned."), null, null));

        assertThat(entry.attacks()).singleElement().satisfies(attack -> {
            assertThat(attack.effectCategories()).contains(Xy1EffectCategory.APPLY_STATUS, Xy1EffectCategory.DAMAGE_PLUS_STATUS);
            assertThat(attack.genericHandlersRequired()).contains("ApplySpecialConditionEffectHandler");
            assertThat(attack.implementationStatus()).contains(Xy1AuditStatus.EFFECT_SUPPORTED_BY_GENERIC_HANDLER);
        });
    }

    @Test
    void auditCardWithAbilityDetectsAbilityAndGap() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-103", "103", "Ability User", "Pokémon", "Stage 1", null, abilities("Sweet Veil", "Each of your Pokémon that has any Fairy Energy attached to it can't be affected by any Special Conditions."), null));

        assertThat(entry.abilities()).singleElement().satisfies(ability -> {
            assertThat(ability.effectCategories()).contains(Xy1EffectCategory.ABILITY_PASSIVE, Xy1EffectCategory.CONTINUOUS_EFFECT);
            assertThat(ability.effectCategories()).doesNotContain(Xy1EffectCategory.APPLY_STATUS, Xy1EffectCategory.ATTACH_ENERGY);
            assertThat(ability.customHandlerRequired()).isTrue();
            assertThat(ability.implementationStatus()).contains(Xy1AuditStatus.REQUIRES_CUSTOM_HANDLER, Xy1AuditStatus.NOT_IMPLEMENTED_YET);
        });
    }

    @Test
    void auditCardWithRuleTextDetectsExRuleAsStructurallySupported() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-104", "104", "EX", "Pokémon", "Basic, EX", null, null, rules("Pokémon-EX rule: When a Pokémon-EX has been Knocked Out, your opponent takes 2 Prize cards.")));

        assertThat(entry.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.supportedByCurrentEngine()).isTrue();
            assertThat(rule.notes()).anyMatch(note -> note.contains("CardSubtype.EX"));
        });
    }

    @Test
    void cardWithoutMappingIsNotMarkedMappedOrFullyTested() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-105", "105", "Unmapped", "Pokémon", "Basic", attacks("Heal", "20", "Heal 10 damage from this Pokémon."), null, null));

        assertThat(entry.hasExplicitMapping()).isFalse();
        assertThat(entry.tested()).isFalse();
        assertThat(entry.implementationStatus()).doesNotContain(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
    }

    @Test
    void cardWithExistingMappingIsMarkedMappedAndTestedForCurrentScope() {
        Xy1CardAuditEntry entry = classifier.classify(card("xy1-1", "1", "Venusaur-EX", "Pokémon", "Basic, EX", attacks("Poison Powder", "60", "Your opponent's Active Pokémon is now Poisoned."), null, null));

        assertThat(entry.hasExplicitMapping()).isTrue();
        assertThat(entry.tested()).isTrue();
        assertThat(entry.implementationStatus()).contains(Xy1AuditStatus.EFFECT_MAPPED, Xy1AuditStatus.FULLY_TESTED);
    }

    @Test
    void reportDoesNotMarkXy1CompleteWhenCardsAreMissing() {
        Xy1AuditReport report = generator.generate(List.of(card("xy1-1", "1", "Venusaur-EX", "Pokémon", "Basic, EX", attacks("Poison Powder", "60", "Your opponent's Active Pokémon is now Poisoned."), null, null)));

        assertThat(report.expectedCardCount()).isEqualTo(146);
        assertThat(report.importedCardCount()).isEqualTo(1);
        assertThat(report.allExpectedCardsImported()).isFalse();
        assertThat(report.fullSetImplementationComplete()).isFalse();
    }

    @Test
    void reportDetectsUnsupportedGapsAcrossCards() {
        Xy1AuditReport report = generator.generate(List.of(
                card("xy1-123", "123", "Professor's Letter", "Trainer", "Item", null, null, rules("Search your deck for up to 2 basic Energy cards, reveal them, and put them into your hand. Shuffle your deck afterward.")),
                card("xy1-127", "127", "Shauna", "Trainer", "Supporter", null, null, rules("Shuffle your hand into your deck. Then, draw 5 cards."))
        ));

        assertThat(report.unsupportedEffects()).isNotEmpty();
        assertThat(report.unsupportedEffects()).extracting(Xy1UnsupportedEffectReport::category)
                .contains(Xy1EffectCategory.SEARCH_DECK, Xy1EffectCategory.DRAW_CARDS);
    }

    private CardEntity card(String cardId, String number, String name, String supertype, String subtypes, String attacks, String abilities, String rules) {
        CardEntity card = new CardEntity();
        card.setCardId(cardId);
        card.setSetId("xy1");
        card.setNumber(number);
        card.setName(name);
        card.setSupertype(supertype);
        card.setSubtypes(subtypes);
        card.setAttacks(attacks);
        card.setAbilities(abilities);
        card.setRules(rules);
        return card;
    }

    private String attacks(String name, String damage, String text) {
        return "[{\"name\":\"" + name + "\",\"damage\":\"" + damage + "\",\"text\":\"" + text + "\"}]";
    }

    private String abilities(String name, String text) {
        return "[{\"name\":\"" + name + "\",\"type\":\"Ability\",\"text\":\"" + text + "\"}]";
    }

    private String rules(String text) {
        return "[\"" + text + "\"]";
    }
}
