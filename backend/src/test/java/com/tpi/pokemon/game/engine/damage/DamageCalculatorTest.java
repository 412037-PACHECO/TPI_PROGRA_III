package com.tpi.pokemon.game.engine.damage;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind;
import com.tpi.pokemon.game.engine.effect.ability.EffectCondition;
import com.tpi.pokemon.game.engine.effect.ability.EffectScope;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import com.tpi.pokemon.game.engine.effect.modifier.DamageModifierContext;
import com.tpi.pokemon.game.engine.effect.modifier.DefaultModifierResolver;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierDefinition;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageCalculatorTest {
    private static final GameId GAME_ID = new GameId("damage-modifier-test-game");
    private static final PlayerId ATTACKER = new PlayerId("attacker");
    private static final PlayerId DEFENDER = new PlayerId("defender");
    private static final AttackDefinition HIT_30 = new AttackDefinition("hit-30", "Hit 30", List.of(EnergyType.COLORLESS), 30);
    private static final AttackDefinition HIT_10 = new AttackDefinition("hit-10", "Hit 10", List.of(EnergyType.COLORLESS), 10);

    private final DamageCalculator calculator = new DamageCalculator();

    @Test
    void returnsBaseDamageWhenNoModifierApplies() {
        DamageCalculation damage = calculator.calculate(attacker(PokemonType.FIRE), defender(List.of(), List.of()), HIT_30);

        assertThat(damage.finalDamage()).isEqualTo(30);
        assertThat(damage.countersAdded()).isEqualTo(3);
        assertThat(damage.weaknessApplied()).isFalse();
        assertThat(damage.resistanceApplied()).isFalse();
    }

    @Test
    void doublesDamageWhenDefenderIsWeakToAttackerType() {
        DamageCalculation damage = calculator.calculate(
                attacker(PokemonType.FIRE),
                defender(List.of(new Weakness(PokemonType.FIRE, 2)), List.of()),
                HIT_30
        );

        assertThat(damage.finalDamage()).isEqualTo(60);
        assertThat(damage.weaknessApplied()).isTrue();
    }

    @Test
    void subtractsResistanceFromDamage() {
        DamageCalculation damage = calculator.calculate(
                attacker(PokemonType.FIRE),
                defender(List.of(), List.of(new Resistance(PokemonType.FIRE, 20))),
                HIT_30
        );

        assertThat(damage.finalDamage()).isEqualTo(10);
        assertThat(damage.resistanceApplied()).isTrue();
    }

    @Test
    void appliesWeaknessBeforeResistance() {
        DamageCalculation damage = calculator.calculate(
                attacker(PokemonType.FIRE),
                defender(List.of(new Weakness(PokemonType.FIRE, 2)), List.of(new Resistance(PokemonType.FIRE, 20))),
                HIT_30
        );

        assertThat(damage.finalDamage()).isEqualTo(40);
    }

    @Test
    void neverReturnsNegativeFinalDamage() {
        DamageCalculation damage = calculator.calculate(
                attacker(PokemonType.FIRE),
                defender(List.of(), List.of(new Resistance(PokemonType.FIRE, 20))),
                HIT_10
        );

        assertThat(damage.finalDamage()).isZero();
        assertThat(damage.countersAdded()).isZero();
    }

    @Test
    void contextualCalculationAppliesDamageModifierBeforeWeaknessAndResistance() {
        PokemonInPlay attacker = attacker(PokemonType.FIRE);
        PokemonInPlay defender = defenderWithEffects(
                List.of(new Weakness(PokemonType.FIRE, 2)),
                List.of(new Resistance(PokemonType.FIRE, 20)),
                List.of(continuousDamageModifier("hard-shell", ModifierOperation.ADD, 10, ModifierLayer.BEFORE_WEAKNESS_RESISTANCE))
        );
        GameState state = activeState(attacker, defender);

        DamageCalculation damage = calculator.calculate(new DamageModifierContext(state, ATTACKER, DEFENDER, attacker, defender, HIT_30), new DefaultModifierResolver());

        assertThat(damage.finalDamage()).isEqualTo(60);
        assertThat(damage.countersAdded()).isEqualTo(6);
        assertThat(damage.weaknessApplied()).isTrue();
        assertThat(damage.resistanceApplied()).isTrue();
        assertThat(damage.appliedModifiers()).hasSize(1);
    }

    @Test
    void contextualCalculationCanPreventAttackDamage() {
        PokemonInPlay attacker = attacker(PokemonType.FIRE);
        PokemonInPlay defender = defenderWithEffects(
                List.of(),
                List.of(),
                List.of(continuousDamageModifier("barrier", ModifierOperation.PREVENT, 0, ModifierLayer.PREVENTION))
        );
        GameState state = activeState(attacker, defender);

        DamageCalculation damage = calculator.calculate(new DamageModifierContext(state, ATTACKER, DEFENDER, attacker, defender, HIT_30), new DefaultModifierResolver());

        assertThat(damage.finalDamage()).isZero();
        assertThat(damage.countersAdded()).isZero();
        assertThat(damage.prevented()).isTrue();
        assertThat(damage.appliedModifiers()).hasSize(1);
    }

    private PokemonInPlay attacker(PokemonType type) {
        return PokemonInPlay.withoutAttachments(card("attacker-card", ATTACKER, pokemonDefinition("attacker-def", type, List.of(HIT_30, HIT_10), List.of(), List.of())));
    }

    private PokemonInPlay defender(List<Weakness> weaknesses, List<Resistance> resistances) {
        return PokemonInPlay.withoutAttachments(card("defender-card", DEFENDER, pokemonDefinition("defender-def", PokemonType.GRASS, List.of(), weaknesses, resistances)));
    }

    private PokemonInPlay defenderWithEffects(List<Weakness> weaknesses, List<Resistance> resistances, List<CardEffectDefinition> effects) {
        return PokemonInPlay.withoutAttachments(card("defender-card", DEFENDER, pokemonDefinition("defender-def", PokemonType.GRASS, List.of(), weaknesses, resistances, effects)));
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances) {
        return pokemonDefinition(id, type, attacks, weaknesses, resistances, List.of());
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances, List<CardEffectDefinition> effects) {
        return new CardDefinitionRef(
                id,
                "Pokemon " + id,
                CardSupertype.POKEMON,
                Set.of(CardSubtype.BASIC),
                null,
                1,
                60,
                List.of(type),
                attacks,
                weaknesses,
                resistances,
                EnergyProfile.none(),
                effects
        );
    }

    private CardEffectDefinition continuousDamageModifier(String effectId, ModifierOperation operation, int amount, ModifierLayer layer) {
        return new CardEffectDefinition(
                effectId,
                "Continuous damage modifier",
                EffectSourceKind.POKEMON_ABILITY,
                EffectActivationKind.CONTINUOUS,
                EffectTiming.CONTINUOUS,
                EffectScope.SELF,
                EffectCondition.always(),
                List.of(new ModifierDefinition(ModifierType.DAMAGE, operation, layer, amount, ModifierTargetRole.DEFENDER))
        );
    }

    private GameState activeState(PokemonInPlay attacker, PokemonInPlay defender) {
        return new GameState(
                GAME_ID,
                GameStatus.ACTIVE,
                player(ATTACKER, attacker),
                player(DEFENDER, defender),
                new TurnState(ATTACKER, ATTACKER, 2, TurnPhase.MAIN, false, false, false, false, false),
                List.of()
        );
    }

    private PlayerGameState player(PlayerId playerId, PokemonInPlay active) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), PrizeCards.empty(), DiscardPile.empty(), new BoardState(new ActivePokemon(active), Bench.empty()), 1);
    }

    private CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }
}
