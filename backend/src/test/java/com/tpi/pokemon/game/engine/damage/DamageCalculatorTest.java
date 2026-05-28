package com.tpi.pokemon.game.engine.damage;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageCalculatorTest {
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

    private PokemonInPlay attacker(PokemonType type) {
        return PokemonInPlay.withoutAttachments(card("attacker-card", ATTACKER, pokemonDefinition("attacker-def", type, List.of(HIT_30, HIT_10), List.of(), List.of())));
    }

    private PokemonInPlay defender(List<Weakness> weaknesses, List<Resistance> resistances) {
        return PokemonInPlay.withoutAttachments(card("defender-card", DEFENDER, pokemonDefinition("defender-def", PokemonType.GRASS, List.of(), weaknesses, resistances)));
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances) {
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
                EnergyProfile.none()
        );
    }

    private CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }
}
