package com.tpi.pokemon.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.application.DeckService;
import com.tpi.pokemon.decks.application.DeckValidator;
import com.tpi.pokemon.game.api.StartTurnRequest;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.realtime.GameRealtimeEventType;
import com.tpi.pokemon.game.realtime.GameRealtimePublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameplayApplicationServiceTest {
    private static final GameId GAME_ID = new GameId("gameplay-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    private final GamePersistenceService persistenceService = org.mockito.Mockito.mock(GamePersistenceService.class);
    private final GameQueryService queryService = org.mockito.Mockito.mock(GameQueryService.class);
    private final DeckService deckService = org.mockito.Mockito.mock(DeckService.class);
    private final DeckValidator deckValidator = org.mockito.Mockito.mock(DeckValidator.class);
    private final CardRepository cardRepository = org.mockito.Mockito.mock(CardRepository.class);
    private final GameDeckCardMapper cardMapper = org.mockito.Mockito.mock(GameDeckCardMapper.class);
    private final GameRealtimePublisher realtimePublisher = org.mockito.Mockito.mock(GameRealtimePublisher.class);
    private final GameplayApplicationService service = new GameplayApplicationService(persistenceService, queryService, deckService, deckValidator, cardRepository, cardMapper, realtimePublisher);

    @Test
    void validStartTurnPersistsActionAndReturnsSafeView() {
        GameState state = activeState();
        GameViewResponse playerOneView = org.mockito.Mockito.mock(GameViewResponse.class);
        GameViewResponse playerTwoView = org.mockito.Mockito.mock(GameViewResponse.class);
        List<GameLogPublicView> publicLog = List.of();
        when(persistenceService.loadLatestGameState(GAME_ID)).thenReturn(Optional.of(state));
        when(queryService.view(GAME_ID.value(), PLAYER_ONE.value())).thenReturn(playerOneView);
        when(queryService.view(GAME_ID.value(), PLAYER_TWO.value())).thenReturn(playerTwoView);
        when(queryService.publicHistory(GAME_ID.value(), PLAYER_ONE.value())).thenReturn(publicLog);

        GameViewResponse response = service.startTurn(GAME_ID.value(), new StartTurnRequest(PLAYER_ONE.value()));

        assertThat(response).isSameAs(playerOneView);
        verify(persistenceService).persistActionResult(any(GameState.class), any(GameActionLogCommand.class), eq(null), eq("START_TURN"));
        verify(realtimePublisher).publishGameplayAction(GAME_ID.value(), GameRealtimeEventType.TURN_STARTED, PLAYER_ONE.value(), "START_TURN", playerOneView, playerTwoView, publicLog, false);
    }

    @Test
    void outsiderActionFailsWithoutPersistingSnapshotOrLog() {
        when(persistenceService.loadLatestGameState(GAME_ID)).thenReturn(Optional.of(activeState()));

        assertThatThrownBy(() -> service.startTurn(GAME_ID.value(), new StartTurnRequest("intruder")))
                .isInstanceOf(UnauthorizedGameActionException.class)
                .hasMessage("Player intruder is not part of game gameplay-game");
        verify(persistenceService).loadLatestGameState(GAME_ID);
        verify(persistenceService, never()).persistActionResult(any(), any(), any(), any());
        verifyNoInteractions(queryService);
        verifyNoInteractions(realtimePublisher);
    }

    private GameState activeState() {
        return new GameState(
                GAME_ID,
                GameStatus.ACTIVE,
                PlayerGameState.empty(PLAYER_ONE),
                PlayerGameState.empty(PLAYER_TWO),
                TurnState.preparedForFirstTurn(PLAYER_ONE),
                List.of()
        );
    }
}
