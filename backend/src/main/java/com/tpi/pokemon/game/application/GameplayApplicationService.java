package com.tpi.pokemon.game.application;

import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.decks.api.DeckValidationResponse;
import com.tpi.pokemon.decks.application.DeckService;
import com.tpi.pokemon.decks.application.DeckValidator;
import com.tpi.pokemon.decks.domain.DeckCardEntity;
import com.tpi.pokemon.decks.domain.DeckEntity;
import com.tpi.pokemon.game.api.AttachEnergyRequest;
import com.tpi.pokemon.game.api.ChooseInitialPokemonRequest;
import com.tpi.pokemon.game.api.DeclareAttackRequest;
import com.tpi.pokemon.game.api.EndTurnRequest;
import com.tpi.pokemon.game.api.EvolvePokemonRequest;
import com.tpi.pokemon.game.api.PlayBasicPokemonRequest;
import com.tpi.pokemon.game.api.PokemonTargetRequest;
import com.tpi.pokemon.game.api.ReplaceActiveRequest;
import com.tpi.pokemon.game.api.RetreatRequest;
import com.tpi.pokemon.game.api.StartGameRequest;
import com.tpi.pokemon.game.api.StartTurnRequest;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.action.AttachEnergyCommand;
import com.tpi.pokemon.game.engine.action.EvolvePokemonCommand;
import com.tpi.pokemon.game.engine.action.PokemonTarget;
import com.tpi.pokemon.game.engine.action.PokemonTargetZone;
import com.tpi.pokemon.game.engine.action.PutBasicPokemonOnBenchCommand;
import com.tpi.pokemon.game.engine.action.RetreatActivePokemonCommand;
import com.tpi.pokemon.game.engine.action.TurnActionService;
import com.tpi.pokemon.game.engine.attack.AttackService;
import com.tpi.pokemon.game.engine.attack.DeclareAttackCommand;
import com.tpi.pokemon.game.engine.knockout.ActivePokemonReplacementResolver;
import com.tpi.pokemon.game.engine.knockout.ReplaceActivePokemonCommand;
import com.tpi.pokemon.game.engine.setup.ChooseInitialPokemonCommand;
import com.tpi.pokemon.game.engine.setup.SetupService;
import com.tpi.pokemon.game.engine.setup.StartSetupCommand;
import com.tpi.pokemon.game.engine.turn.EndTurnCommand;
import com.tpi.pokemon.game.engine.turn.StartTurnCommand;
import com.tpi.pokemon.game.engine.turn.TurnManager;
import com.tpi.pokemon.game.realtime.GameRealtimeEventType;
import com.tpi.pokemon.game.realtime.GameRealtimePublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GameplayApplicationService {
    private final GamePersistenceService persistenceService;
    private final GameQueryService queryService;
    private final DeckService deckService;
    private final DeckValidator deckValidator;
    private final CardRepository cardRepository;
    private final GameDeckCardMapper cardMapper;
    private final GameRealtimePublisher realtimePublisher;
    private final TurnManager turnManager = new TurnManager();
    private final TurnActionService turnActionService = new TurnActionService();
    private final AttackService attackService = new AttackService(turnManager);
    private final ActivePokemonReplacementResolver replacementResolver = new ActivePokemonReplacementResolver(turnManager);

    public GameplayApplicationService(GamePersistenceService persistenceService, GameQueryService queryService, DeckService deckService, DeckValidator deckValidator, CardRepository cardRepository, GameDeckCardMapper cardMapper, GameRealtimePublisher realtimePublisher) {
        this.persistenceService = persistenceService;
        this.queryService = queryService;
        this.deckService = deckService;
        this.deckValidator = deckValidator;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional
    public GameViewResponse startGame(String gameId, StartGameRequest request) {
        if (request == null || request.playerOneDeckId() == null || request.playerTwoDeckId() == null) {
            throw new InvalidGameCommandException("playerId, playerOneDeckId and playerTwoDeckId are required");
        }
        PlayerId actor = player(request.playerId(), "playerId");
        GameState state = requireState(gameId);
        requirePlayerInGame(state, actor);
        DeckEntity playerOneDeck = validDeck(request.playerOneDeckId());
        DeckEntity playerTwoDeck = validDeck(request.playerTwoDeckId());
        requireDeckOwner(playerOneDeck, state.getPlayerOneState().getPlayerId());
        requireDeckOwner(playerTwoDeck, state.getPlayerTwoState().getPlayerId());
        List<CardInstance> playerOneCards = deckCards(playerOneDeck, state.getPlayerOneState().getPlayerId(), "p1");
        List<CardInstance> playerTwoCards = deckCards(playerTwoDeck, state.getPlayerTwoState().getPlayerId(), "p2");
        SetupService setupService = new SetupService(
                deck -> new java.util.ArrayList<>(deck),
                (one, two) -> one,
                (playerId, opponentMulligans, setupState) -> 0
        );
        GameState updated = execute("START_GAME", GameRealtimeEventType.GAME_STARTED, state, actor, Map.of("playerId", actor.value(), "playerOneDeckId", request.playerOneDeckId(), "playerTwoDeckId", request.playerTwoDeckId()), ignored -> setupService.startSetup(state, new StartSetupCommand(playerOneCards, playerTwoCards)));
        return queryService.view(gameId, actor.value());
    }

    @Transactional
    public GameViewResponse chooseInitial(String gameId, ChooseInitialPokemonRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "CHOOSE_INITIAL_POKEMON", GameRealtimeEventType.SETUP_UPDATED, request, state -> new SetupService(deck -> deck, (one, two) -> one, (p, m, s) -> 0)
                .chooseInitialPokemon(state, new ChooseInitialPokemonCommand(player, cardId(request.activePokemonId(), "activePokemonId"), cardIds(request.benchPokemonIds()))));
    }

    @Transactional
    public GameViewResponse completeSetup(String gameId, String playerId, String startingPlayerId) {
        PlayerId player = player(playerId, "playerId");
        return executeAndView(gameId, player, "COMPLETE_SETUP", GameRealtimeEventType.SETUP_UPDATED, Map.of("playerId", player.value()), state -> new SetupService(deck -> deck, (one, two) -> startingPlayerId == null || startingPlayerId.isBlank() ? one : requireStartingPlayer(startingPlayerId, one, two), (p, m, s) -> 0).completeSetup(state));
    }

    @Transactional
    public GameViewResponse startTurn(String gameId, StartTurnRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "START_TURN", GameRealtimeEventType.TURN_STARTED, request, state -> turnManager.startTurn(state, new StartTurnCommand(player)));
    }

    @Transactional
    public GameViewResponse playBasic(String gameId, PlayBasicPokemonRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "PLAY_BASIC", GameRealtimeEventType.BASIC_PLAYED, request, state -> turnActionService.putBasicPokemonOnBench(state, new PutBasicPokemonOnBenchCommand(player, cardId(request.cardInstanceId(), "cardInstanceId"))));
    }

    @Transactional
    public GameViewResponse attachEnergy(String gameId, AttachEnergyRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "ATTACH_ENERGY", GameRealtimeEventType.ENERGY_ATTACHED, request, state -> turnActionService.attachEnergy(state, new AttachEnergyCommand(player, cardId(request.energyCardInstanceId(), "energyCardInstanceId"), target(request.target()))));
    }

    @Transactional
    public GameViewResponse evolve(String gameId, EvolvePokemonRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "EVOLVE", GameRealtimeEventType.POKEMON_EVOLVED, request, state -> turnActionService.evolvePokemon(state, new EvolvePokemonCommand(player, cardId(request.evolutionCardInstanceId(), "evolutionCardInstanceId"), target(request.target()))));
    }

    @Transactional
    public GameViewResponse retreat(String gameId, RetreatRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        if (request.benchIndex() == null) throw new InvalidGameCommandException("benchIndex is required");
        return executeAndView(gameId, player, "RETREAT", GameRealtimeEventType.RETREAT_PERFORMED, request, state -> turnActionService.retreatActivePokemon(state, new RetreatActivePokemonCommand(player, request.benchIndex(), cardIds(request.energyCardInstanceIdsToDiscard()))));
    }

    @Transactional
    public GameViewResponse attack(String gameId, DeclareAttackRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "DECLARE_ATTACK", GameRealtimeEventType.ATTACK_DECLARED, request, state -> attackService.declareAttack(state, new DeclareAttackCommand(new GameId(gameId), player, required(request.attackId(), "attackId"))));
    }

    @Transactional
    public GameViewResponse endTurn(String gameId, EndTurnRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        return executeAndView(gameId, player, "END_TURN", GameRealtimeEventType.TURN_ENDED, request, state -> turnManager.endTurn(state, new EndTurnCommand(player)));
    }

    @Transactional
    public GameViewResponse replaceActive(String gameId, ReplaceActiveRequest request) {
        requireBody(request);
        PlayerId player = player(request == null ? null : request.playerId(), "playerId");
        if (request.benchIndex() == null) throw new InvalidGameCommandException("benchIndex is required");
        return executeAndView(gameId, player, "REPLACE_ACTIVE", GameRealtimeEventType.ACTIVE_REPLACED, request, state -> replacementResolver.replaceActive(state, new ReplaceActivePokemonCommand(player, request.benchIndex())));
    }

    private GameViewResponse executeAndView(String gameId, PlayerId player, String actionType, GameRealtimeEventType eventType, Object request, java.util.function.Function<GameState, GameState> action) {
        GameState current = requireState(gameId);
        requirePlayerInGame(current, player);
        GameState updated = execute(actionType, eventType, current, player, request, action);
        return queryService.view(updated.getGameId().value(), player.value());
    }

    private void requirePlayerInGame(GameState state, PlayerId player) {
        if (!state.getPlayerOneState().getPlayerId().equals(player) && !state.getPlayerTwoState().getPlayerId().equals(player)) {
            throw new UnauthorizedGameActionException("Player " + player.value() + " is not part of game " + state.getGameId().value());
        }
    }

    private GameState execute(String actionType, GameRealtimeEventType eventType, GameState current, PlayerId actor, Object commandPayload, java.util.function.Function<GameState, GameState> action) {
        GameState updated;
        try {
            updated = action.apply(current);
        } catch (RuntimeException exception) {
            throw new InvalidGameCommandException(exception.getMessage());
        }
        List<Map<String, String>> eventDelta = updated.getEvents().stream()
                .skip(current.getEvents().size())
                .map(event -> Map.of("type", event.getClass().getSimpleName()))
                .toList();
        persistenceService.persistActionResult(updated, new GameActionLogCommand(updated.getGameId(), actor, actionType, commandPayload, Map.of("status", updated.getStatus().name()), eventDelta), null, actionType);
        publishGameplayAfterCommit(updated, actor, actionType, eventType);
        return updated;
    }

    private void publishGameplayAfterCommit(GameState updated, PlayerId actor, String actionType, GameRealtimeEventType eventType) {
        String gameId = updated.getGameId().value();
        String playerOneId = updated.getPlayerOneState().getPlayerId().value();
        String playerTwoId = updated.getPlayerTwoState().getPlayerId().value();
        GameViewResponse playerOneView = queryService.view(gameId, playerOneId);
        GameViewResponse playerTwoView = queryService.view(gameId, playerTwoId);
        List<GameLogPublicView> publicLog = queryService.publicHistory(gameId, playerOneId);
        boolean finished = updated.getStatus() == GameStatus.FINISHED;
        publishAfterCommit(() -> realtimePublisher.publishGameplayAction(gameId, eventType, actor.value(), actionType, playerOneView, playerTwoView, publicLog, finished));
    }

    private void publishAfterCommit(Runnable publication) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publication.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publication.run();
            }
        });
    }

    private GameState requireState(String gameId) {
        try {
            return persistenceService.loadLatestGameState(new GameId(gameId)).orElseThrow(() -> new GameNotInExpectedStateException("Game " + gameId + " has no persisted state"));
        } catch (GamePersistenceException exception) {
            throw new GameNotInExpectedStateException(exception.getMessage());
        }
    }

    private DeckEntity validDeck(Long deckId) {
        DeckEntity deck = deckService.getDeck(deckId);
        DeckValidationResponse validation = deckValidator.validate(deck);
        if (!validation.valid()) {
            throw new DeckNotValidForGameException("Deck " + deckId + " is not valid for game: " + validation.errors());
        }
        return deck;
    }

    private void requireDeckOwner(DeckEntity deck, PlayerId playerId) {
        if (deck.getOwnerName() == null || !deck.getOwnerName().equalsIgnoreCase(playerId.value())) {
            throw new DeckNotValidForGameException("Deck " + deck.getId() + " does not belong to player " + playerId.value());
        }
    }

    private List<CardInstance> deckCards(DeckEntity deck, PlayerId owner, String prefix) {
        List<CardInstance> cards = new ArrayList<>();
        int sequence = 1;
        for (DeckCardEntity deckCard : deck.getCards()) {
            CardEntity card = cardRepository.findByCardId(deckCard.getCardId()).orElseThrow(() -> new DeckNotValidForGameException("Card " + deckCard.getCardId() + " is missing from catalog"));
            for (int i = 0; i < deckCard.getQuantity(); i++) {
                cards.add(cardMapper.toInstance(card, owner, owner.value() + "-" + prefix + "-" + deckCard.getCardId() + "-" + sequence++));
            }
        }
        return cards;
    }

    private PlayerId requireStartingPlayer(String value, PlayerId one, PlayerId two) {
        PlayerId selected = new PlayerId(value.trim());
        if (!selected.equals(one) && !selected.equals(two)) throw new InvalidGameCommandException("startingPlayerId must belong to the game");
        return selected;
    }

    private PlayerId player(String value, String fieldName) {
        return new PlayerId(required(value, fieldName));
    }

    private CardInstanceId cardId(String value, String fieldName) {
        return new CardInstanceId(required(value, fieldName));
    }

    private List<CardInstanceId> cardIds(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> cardId(value, "cardInstanceId")).toList();
    }

    private PokemonTarget target(PokemonTargetRequest request) {
        if (request == null || request.zone() == null || request.zone().isBlank()) throw new InvalidGameCommandException("target.zone is required");
        PokemonTargetZone zone;
        try {
            zone = PokemonTargetZone.valueOf(request.zone().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGameCommandException("target.zone must be ACTIVE or BENCH");
        }
        if (zone == PokemonTargetZone.ACTIVE) return PokemonTarget.active();
        if (request.benchIndex() == null) throw new InvalidGameCommandException("target.benchIndex is required for BENCH target");
        return PokemonTarget.bench(request.benchIndex());
    }

    private void requireBody(Object request) {
        if (request == null) throw new InvalidGameCommandException("request body is required");
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new InvalidGameCommandException(fieldName + " is required");
        return value.trim();
    }
}
