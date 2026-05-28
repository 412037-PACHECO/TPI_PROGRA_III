package com.tpi.pokemon.game;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.Set;

public final class GameStateFixtures {
    public static final GameId GAME_ID = new GameId("game-1");
    public static final PlayerId PLAYER_ONE = new PlayerId("player-1");
    public static final PlayerId PLAYER_TWO = new PlayerId("player-2");
    public static final CardDefinitionRef PIKACHU = new CardDefinitionRef("base1-58", "Pikachu", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC));

    private GameStateFixtures() {
    }

    public static CardInstance card(String instanceId) {
        return card(instanceId, PLAYER_ONE);
    }

    public static CardInstance card(String instanceId, PlayerId owner) {
        return new CardInstance(new CardInstanceId(instanceId), PIKACHU, owner);
    }

    public static PokemonInPlay pokemon(String instanceId) {
        return PokemonInPlay.withoutAttachments(card(instanceId));
    }

    public static PokemonInPlay pokemon(String instanceId, PlayerId owner) {
        return PokemonInPlay.withoutAttachments(card(instanceId, owner));
    }
}
