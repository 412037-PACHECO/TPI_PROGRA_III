package com.tpi.pokemon.game.domain.model;

import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_ONE;
import static com.tpi.pokemon.game.GameStateFixtures.card;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardInstanceTest {
    @Test
    void differentInstancesCanReferenceTheSameDefinition() {
        CardDefinitionRef definition = new CardDefinitionRef("base1-58", "Pikachu");
        CardInstance firstCopy = new CardInstance(new CardInstanceId("copy-1"), definition, PLAYER_ONE);
        CardInstance secondCopy = new CardInstance(new CardInstanceId("copy-2"), definition, PLAYER_ONE);

        assertThat(firstCopy.definition()).isEqualTo(secondCopy.definition());
        assertThat(firstCopy.id()).isNotEqualTo(secondCopy.id());
    }

    @Test
    void duplicateCardInstanceIdInTheSameZoneIsRejected() {
        assertThatThrownBy(() -> new DeckZone(List.of(card("same-id"), card("same-id"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate CardInstanceId");
    }
}
