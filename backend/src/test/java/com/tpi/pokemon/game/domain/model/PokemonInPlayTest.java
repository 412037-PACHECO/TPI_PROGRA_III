package com.tpi.pokemon.game.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PokemonInPlayTest {
    private static final PlayerId PLAYER = new PlayerId("player-one");
    private static final CardDefinitionRef BASIC = new CardDefinitionRef("basic", "Basic", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC));

    @Test
    void volatileSpecialConditionsReplaceEachOther() {
        PokemonInPlay pokemon = pokemon()
                .applySpecialCondition(SpecialCondition.ASLEEP)
                .applySpecialCondition(SpecialCondition.CONFUSED)
                .applySpecialCondition(SpecialCondition.PARALYZED);

        assertThat(pokemon.hasSpecialCondition(SpecialCondition.PARALYZED)).isTrue();
        assertThat(pokemon.hasSpecialCondition(SpecialCondition.ASLEEP)).isFalse();
        assertThat(pokemon.hasSpecialCondition(SpecialCondition.CONFUSED)).isFalse();
    }

    @Test
    void burnedAndPoisonedCanCoexistWithVolatileCondition() {
        PokemonInPlay pokemon = pokemon()
                .applySpecialCondition(SpecialCondition.ASLEEP)
                .applySpecialCondition(SpecialCondition.BURNED)
                .applySpecialCondition(SpecialCondition.POISONED);

        assertThat(pokemon.hasSpecialCondition(SpecialCondition.ASLEEP)).isTrue();
        assertThat(pokemon.hasSpecialCondition(SpecialCondition.BURNED)).isTrue();
        assertThat(pokemon.hasSpecialCondition(SpecialCondition.POISONED)).isTrue();
    }

    @Test
    void clearsAllSpecialConditionsWithoutClearingDamage() {
        PokemonInPlay pokemon = pokemon()
                .applyDamage(20)
                .applySpecialCondition(SpecialCondition.CONFUSED)
                .applySpecialCondition(SpecialCondition.BURNED)
                .applySpecialCondition(SpecialCondition.POISONED)
                .clearSpecialConditions();

        assertThat(pokemon.getSpecialConditions().hasAny()).isFalse();
        assertThat(pokemon.getDamageCounters()).isEqualTo(2);
    }

    private PokemonInPlay pokemon() {
        return PokemonInPlay.withoutAttachments(new CardInstance(new CardInstanceId("pokemon"), BASIC, PLAYER));
    }
}
