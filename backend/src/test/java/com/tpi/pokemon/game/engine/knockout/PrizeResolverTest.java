package com.tpi.pokemon.game.engine.knockout;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsTakenEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrizeResolverTest {
    private static final GameId GAME_ID = new GameId("prize-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");
    private static final CardDefinitionRef PRIZE_DEFINITION = new CardDefinitionRef("prize-def", "Prize", CardSupertype.TRAINER, Set.of(CardSubtype.ITEM));

    private final PrizeResolver resolver = new PrizeResolver();

    @Test
    void takesOnePrizeForRegularPokemonKnockout() {
        GameState state = game(player(PLAYER_ONE, prizes(PLAYER_ONE, 6)), player(PLAYER_TWO, List.of()));
        List<GameEvent> events = new ArrayList<>();

        PrizeResolver.PrizeResolution resolution = resolver.takePrizes(state, PLAYER_ONE, 1, events);

        assertThat(resolution.state().getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(5);
        assertThat(resolution.state().getPlayerOneState().getHand().getCards()).hasSize(1);
        assertThat(eventsOfType(events, PrizeCardsTakenEvent.class)).singleElement()
                .extracting(PrizeCardsTakenEvent::remainingPrizeCount).isEqualTo(5);
    }

    @Test
    void takesTwoPrizesForPokemonExKnockout() {
        GameState state = game(player(PLAYER_ONE, prizes(PLAYER_ONE, 6)), player(PLAYER_TWO, List.of()));

        PrizeResolver.PrizeResolution resolution = resolver.takePrizes(state, PLAYER_ONE, 2, new ArrayList<>());

        assertThat(resolution.state().getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(4);
        assertThat(resolution.prizeTakenResult().takenCards()).hasSize(2);
    }

    @Test
    void takesOnlyRemainingPrizeWhenRequestedPrizeCountIsHigherThanRemaining() {
        GameState state = game(player(PLAYER_ONE, prizes(PLAYER_ONE, 1)), player(PLAYER_TWO, List.of()));

        PrizeResolver.PrizeResolution resolution = resolver.takePrizes(state, PLAYER_ONE, 2, new ArrayList<>());

        assertThat(resolution.state().getPlayerOneState().getPrizeCards().remainingCount()).isZero();
        assertThat(resolution.prizeTakenResult().takenCards()).hasSize(1);
    }

    private GameState game(PlayerGameState playerOne, PlayerGameState playerTwo) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.ATTACK, false, false, false, false, false), List.of());
    }

    private PlayerGameState player(PlayerId playerId, List<CardInstance> prizes) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), new PrizeCards(prizes), DiscardPile.empty(), BoardState.empty(), 1);
    }

    private List<CardInstance> prizes(PlayerId owner, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new CardInstance(new CardInstanceId(owner.value() + "-prize-" + index), PRIZE_DEFINITION, owner))
                .toList();
    }

    private <T extends GameEvent> List<T> eventsOfType(List<GameEvent> events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
