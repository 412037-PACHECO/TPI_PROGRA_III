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
import com.tpi.pokemon.game.api.PlayTrainerRequest;
import com.tpi.pokemon.game.api.ResolveSelectionRequest;
import com.tpi.pokemon.game.api.StartTurnRequest;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.GameStatus;
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
import com.tpi.pokemon.game.engine.effect.CardFilterSpec;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.EffectType;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import com.tpi.pokemon.game.realtime.GameRealtimeEventType;
import com.tpi.pokemon.game.realtime.GameRealtimePublisher;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.mockito.ArgumentCaptor;
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
        verify(realtimePublisher).publishGameplayAction(GAME_ID.value(), GameRealtimeEventType.TURN_STARTED, PLAYER_ONE.value(), "START_TURN", playerOneView, playerTwoView, publicLog, false, false);
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

    @Test
    void playTrainerWithPendingSelectionPersistsPendingAndPublishesSelectionRequired() {
        CardInstance letter = card("letter-1", new CardDefinitionRef("xy1-123", "Professor's Letter", CardSupertype.TRAINER, Set.of(CardSubtype.ITEM)));
        CardInstance energy = card("energy-1", new CardDefinitionRef("xy1-energy", "Grass Energy", CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY)));
        GameState state = activeStateWithPlayerOneZones(List.of(letter), List.of(energy), List.of());
        GameViewResponse playerOneView = org.mockito.Mockito.mock(GameViewResponse.class);
        GameViewResponse playerTwoView = org.mockito.Mockito.mock(GameViewResponse.class);
        when(persistenceService.loadLatestGameState(GAME_ID)).thenReturn(Optional.of(state));
        when(queryService.view(GAME_ID.value(), PLAYER_ONE.value())).thenReturn(playerOneView);
        when(queryService.view(GAME_ID.value(), PLAYER_TWO.value())).thenReturn(playerTwoView);
        when(queryService.publicHistory(GAME_ID.value(), PLAYER_ONE.value())).thenReturn(List.of());

        GameViewResponse response = service.playTrainer(GAME_ID.value(), new PlayTrainerRequest(PLAYER_ONE.value(), letter.id().value(), null));

        assertThat(response).isSameAs(playerOneView);
        ArgumentCaptor<PendingEffectSelection> pending = ArgumentCaptor.forClass(PendingEffectSelection.class);
        verify(persistenceService).persistActionResult(any(GameState.class), any(GameActionLogCommand.class), pending.capture(), eq("SELECTION_REQUIRED"));
        assertThat(pending.getValue().effectType()).isEqualTo(EffectType.SEARCH_DECK);
        verify(realtimePublisher).publishGameplayAction(GAME_ID.value(), GameRealtimeEventType.TRAINER_PLAYED, PLAYER_ONE.value(), "PLAY_TRAINER", playerOneView, playerTwoView, List.of(), false, true);
    }

    @Test
    void resolveSelectionRejectsWrongPlayerWithoutPersisting() {
        GameState state = activeState();
        PendingEffectSelection pending = new PendingEffectSelection(
                PLAYER_ONE,
                EffectType.SEARCH_DECK,
                "source",
                EffectCardZone.DECK,
                EffectTarget.ACTING_PLAYER,
                0,
                1,
                CardFilterSpec.any(),
                true,
                true,
                EffectDefinition.searchDeck(EffectTarget.ACTING_PLAYER, 1, CardFilterSpec.any(), List.of(), true, true, EffectTiming.ON_PLAY_TRAINER),
                List.of(new CardInstanceId("candidate-1"))
        );
        when(persistenceService.loadLatestGameState(GAME_ID)).thenReturn(Optional.of(state));
        when(persistenceService.loadLatestPendingEffectSelection(GAME_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.resolveSelection(GAME_ID.value(), new ResolveSelectionRequest(PLAYER_TWO.value(), null, List.of("candidate-1"), null)))
                .isInstanceOf(InvalidGameCommandException.class)
                .hasMessageContaining("Only the pending selection player");
        verify(persistenceService, never()).persistActionResult(any(), any(), any(), any());
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

    private GameState activeStateWithPlayerOneZones(List<CardInstance> hand, List<CardInstance> deck, List<CardInstance> discard) {
        return new GameState(
                GAME_ID,
                GameStatus.ACTIVE,
                new PlayerGameState(PLAYER_ONE, new DeckZone(deck), new HandZone(hand), PrizeCards.empty(), new DiscardPile(discard), BoardState.empty()),
                PlayerGameState.empty(PLAYER_TWO),
                mainTurn(),
                List.of()
        );
    }

    private TurnState mainTurn() {
        return new TurnState(PLAYER_ONE, PLAYER_ONE, 1, com.tpi.pokemon.game.domain.enums.TurnPhase.MAIN, true, false, false, false, false);
    }

    private CardInstance card(String id, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, PLAYER_ONE);
    }
}
