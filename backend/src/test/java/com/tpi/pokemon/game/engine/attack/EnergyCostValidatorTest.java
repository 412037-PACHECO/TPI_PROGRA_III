package com.tpi.pokemon.game.engine.attack;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnergyCostValidatorTest {
    private static final PlayerId PLAYER = new PlayerId("player-one");
    private static final AttackDefinition EMBER = new AttackDefinition("ember", "Ember", List.of(EnergyType.FIRE), 30);
    private static final AttackDefinition TACKLE = new AttackDefinition("tackle", "Tackle", List.of(EnergyType.COLORLESS), 10);
    private static final AttackDefinition FLAME_TAIL = new AttackDefinition("flame-tail", "Flame Tail", List.of(EnergyType.FIRE, EnergyType.COLORLESS), 30);

    private final EnergyCostValidator validator = new EnergyCostValidator();

    @Test
    void acceptsExactSpecificEnergyCost() {
        PokemonInPlay attacker = pokemonWithEnergies(energy("fire-1", EnergyType.FIRE));

        assertThat(validator.hasEnoughEnergy(attacker, EMBER)).isTrue();
    }

    @Test
    void acceptsColorlessCostPaidByAnyEnergy() {
        PokemonInPlay attacker = pokemonWithEnergies(energy("water-1", EnergyType.WATER));

        assertThat(validator.hasEnoughEnergy(attacker, TACKLE)).isTrue();
    }

    @Test
    void acceptsMixedCostByReservingSpecificEnergyBeforeColorless() {
        PokemonInPlay attacker = pokemonWithEnergies(
                energy("fire-1", EnergyType.FIRE),
                energy("water-1", EnergyType.WATER)
        );

        assertThat(validator.hasEnoughEnergy(attacker, FLAME_TAIL)).isTrue();
    }

    @Test
    void rejectsMixedCostWhenOnlyColorlessQuantityIsSatisfiedButSpecificEnergyIsMissing() {
        PokemonInPlay attacker = pokemonWithEnergies(
                energy("water-1", EnergyType.WATER),
                energy("water-2", EnergyType.WATER)
        );

        assertThat(validator.hasEnoughEnergy(attacker, FLAME_TAIL)).isFalse();
    }

    @Test
    void rejectsAttackWithoutEnoughEnergy() {
        PokemonInPlay attacker = pokemonWithEnergies(energy("fire-1", EnergyType.FIRE));

        assertThat(validator.hasEnoughEnergy(attacker, FLAME_TAIL)).isFalse();
    }

    @Test
    void rainbowEnergyCanPayOneSpecificEnergyCost() {
        PokemonInPlay attacker = pokemonWithEnergies(rainbowEnergy("rainbow-1"));

        assertThat(validator.hasEnoughEnergy(attacker, EMBER)).isTrue();
    }

    @Test
    void rainbowEnergyProvidesOnlyOneEnergySymbolAtATime() {
        AttackDefinition fireWater = new AttackDefinition("fire-water", "Fire Water", List.of(EnergyType.FIRE, EnergyType.WATER), 40);

        assertThat(validator.hasEnoughEnergy(pokemonWithEnergies(rainbowEnergy("rainbow-1")), fireWater)).isFalse();
        assertThat(validator.hasEnoughEnergy(pokemonWithEnergies(rainbowEnergy("rainbow-1"), energy("water-1", EnergyType.WATER)), fireWater)).isTrue();
    }

    private PokemonInPlay pokemonWithEnergies(CardInstance... energies) {
        return new PokemonInPlay(card("attacker", pokemonDefinition("attacker-def", List.of(EMBER, TACKLE, FLAME_TAIL))), new AttachedCards(List.of(energies)));
    }

    private CardDefinitionRef pokemonDefinition(String id, List<AttackDefinition> attacks) {
        return new CardDefinitionRef(
                id,
                "Pokemon " + id,
                CardSupertype.POKEMON,
                Set.of(CardSubtype.BASIC),
                null,
                1,
                60,
                List.of(PokemonType.FIRE),
                attacks,
                List.of(),
                List.of(),
                EnergyProfile.none()
        );
    }

    private CardInstance energy(String id, EnergyType type) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                type + " Energy",
                CardSupertype.ENERGY,
                Set.of(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.basic(type)
        );
        return card(id, definition);
    }

    private CardInstance rainbowEnergy(String id) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                "Rainbow Energy",
                CardSupertype.ENERGY,
                Set.of(CardSubtype.SPECIAL_ENERGY),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.rainbow()
        );
        return card(id, definition);
    }

    private CardInstance card(String id, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, PLAYER);
    }
}
