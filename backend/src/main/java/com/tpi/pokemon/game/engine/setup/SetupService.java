package com.tpi.pokemon.game.engine.setup;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.InitialActivePokemonSelectedEvent;
import com.tpi.pokemon.game.engine.event.InitialBenchSelectedEvent;
import com.tpi.pokemon.game.engine.event.InitialHandDrawnEvent;
import com.tpi.pokemon.game.engine.event.MulliganBonusCardsDrawnEvent;
import com.tpi.pokemon.game.engine.event.MulliganPerformedEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsSetEvent;
import com.tpi.pokemon.game.engine.event.SetupCompletedEvent;
import com.tpi.pokemon.game.engine.event.SetupStartedEvent;
import com.tpi.pokemon.game.engine.event.StartingPlayerSelectedEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import com.tpi.pokemon.game.engine.random.StartingPlayerSelector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SetupService {
    private static final int DECK_SIZE = 60;
    private static final int INITIAL_HAND_SIZE = 7;
    private static final int PRIZE_CARD_COUNT = 6;
    private static final int MAX_MULLIGAN_ATTEMPTS = 100;

    private final DeckShuffler deckShuffler;
    private final StartingPlayerSelector startingPlayerSelector;
    private final MulliganBonusDrawPolicy mulliganBonusDrawPolicy;

    public SetupService(DeckShuffler deckShuffler, StartingPlayerSelector startingPlayerSelector, MulliganBonusDrawPolicy mulliganBonusDrawPolicy) {
        this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null");
        this.startingPlayerSelector = Objects.requireNonNull(startingPlayerSelector, "startingPlayerSelector must not be null");
        this.mulliganBonusDrawPolicy = Objects.requireNonNull(mulliganBonusDrawPolicy, "mulliganBonusDrawPolicy must not be null");
    }

    public GameState startSetup(GameState state, StartSetupCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireStatus(state, GameStatus.CREATED, "Setup can only start from CREATED status");

        PlayerId playerOneId = state.getPlayerOneState().getPlayerId();
        PlayerId playerTwoId = state.getPlayerTwoState().getPlayerId();
        validateDeckForSetup(command.playerOneDeck(), playerOneId, "player one deck");
        validateDeckForSetup(command.playerTwoDeck(), playerTwoId, "player two deck");

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new SetupStartedEvent(state.getGameId()));

        InitialSetupDraw playerOneDraw = drawOpeningHandWithMulligans(state, playerOneId, command.playerOneDeck(), events);
        InitialSetupDraw playerTwoDraw = drawOpeningHandWithMulligans(state, playerTwoId, command.playerTwoDeck(), events);

        PlayerGameState playerOneState = playerStateAfterInitialDraw(playerOneId, playerOneDraw);
        PlayerGameState playerTwoState = playerStateAfterInitialDraw(playerTwoId, playerTwoDraw);
        GameState setupState = new GameState(state.getGameId(), GameStatus.SETUP, playerOneState, playerTwoState, TurnState.notStarted(), events);

        playerOneState = applyMulliganBonus(setupState, playerOneState, playerTwoDraw.mulliganCount(), events);
        playerTwoState = applyMulliganBonus(setupState, playerTwoState, playerOneDraw.mulliganCount(), events);

        return new GameState(state.getGameId(), GameStatus.SETUP, playerOneState, playerTwoState, TurnState.notStarted(), events);
    }

    public GameState chooseInitialPokemon(GameState state, ChooseInitialPokemonCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        requireStatus(state, GameStatus.SETUP, "Initial Pokemon can only be chosen during SETUP");

        PlayerGameState playerState = getPlayerState(state, command.playerId());
        if (playerState.getBoard().getActivePokemon().isPresent()) {
            throw new SetupException("Player has already chosen an initial Active Pokemon");
        }
        if (command.benchPokemonIds().size() > Bench.MAX_SIZE) {
            throw new SetupException("Initial bench cannot contain more than " + Bench.MAX_SIZE + " Pokemon");
        }

        Set<CardInstanceId> selectedIds = new HashSet<>();
        if (!selectedIds.add(command.activePokemonId())) {
            throw new SetupException("Selected Pokemon must not contain duplicates");
        }
        for (CardInstanceId benchPokemonId : command.benchPokemonIds()) {
            if (!selectedIds.add(benchPokemonId)) {
                throw new SetupException("Selected Pokemon must not contain duplicates");
            }
        }

        Map<CardInstanceId, CardInstance> handById = playerState.getHand().getCards().stream()
                .collect(Collectors.toMap(CardInstance::id, Function.identity()));
        CardInstance activeCard = requireBasicPokemonFromHand(handById, command.activePokemonId(), "active Pokemon");
        List<CardInstance> benchCards = command.benchPokemonIds().stream()
                .map(id -> requireBasicPokemonFromHand(handById, id, "bench Pokemon"))
                .toList();

        List<CardInstance> remainingHand = playerState.getHand().getCards().stream()
                .filter(card -> !selectedIds.contains(card.id()))
                .toList();
        BoardState board = new BoardState(
                new ActivePokemon(PokemonInPlay.withoutAttachments(activeCard)),
                new Bench(benchCards.stream().map(PokemonInPlay::withoutAttachments).toList())
        );
        PlayerGameState updatedPlayer = new PlayerGameState(
                playerState.getPlayerId(),
                playerState.getDeck(),
                new HandZone(remainingHand),
                playerState.getPrizeCards(),
                playerState.getDiscardPile(),
                board
        );

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new InitialActivePokemonSelectedEvent(state.getGameId(), command.playerId(), command.activePokemonId()));
        events.add(new InitialBenchSelectedEvent(state.getGameId(), command.playerId(), command.benchPokemonIds()));

        return withUpdatedPlayer(state, updatedPlayer, GameStatus.SETUP, state.getTurnState(), events);
    }

    public GameState completeSetup(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        requireStatus(state, GameStatus.SETUP, "Setup can only be completed from SETUP status");
        requireActivePokemon(state.getPlayerOneState());
        requireActivePokemon(state.getPlayerTwoState());

        PlayerId playerOneId = state.getPlayerOneState().getPlayerId();
        PlayerId playerTwoId = state.getPlayerTwoState().getPlayerId();
        PlayerId startingPlayer = startingPlayerSelector.selectStartingPlayer(playerOneId, playerTwoId);
        if (!playerOneId.equals(startingPlayer) && !playerTwoId.equals(startingPlayer)) {
            throw new SetupException("Starting player selector returned a player that is not in this game");
        }

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        PlayerGameState playerOneWithPrizes = setPrizeCards(state, state.getPlayerOneState(), events);
        PlayerGameState playerTwoWithPrizes = setPrizeCards(state, state.getPlayerTwoState(), events);
        events.add(new StartingPlayerSelectedEvent(state.getGameId(), startingPlayer));
        events.add(new SetupCompletedEvent(state.getGameId()));

        return new GameState(
                state.getGameId(),
                GameStatus.ACTIVE,
                playerOneWithPrizes,
                playerTwoWithPrizes,
                TurnState.preparedForFirstTurn(startingPlayer),
                events
        );
    }

    public GameState completeSetup(GameState state, CompleteSetupCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return completeSetup(state);
    }

    private InitialSetupDraw drawOpeningHandWithMulligans(GameState state, PlayerId playerId, List<CardInstance> originalDeck, List<GameEvent> events) {
        int mulligans = 0;
        for (int attempt = 0; attempt < MAX_MULLIGAN_ATTEMPTS; attempt++) {
            List<CardInstance> shuffledDeck = shuffleDeck(state, playerId, originalDeck, events);
            List<CardInstance> hand = List.copyOf(shuffledDeck.subList(0, INITIAL_HAND_SIZE));
            events.add(new InitialHandDrawnEvent(state.getGameId(), playerId, cardIds(hand)));
            if (hasBasicPokemon(hand)) {
                return new InitialSetupDraw(
                        List.copyOf(shuffledDeck.subList(INITIAL_HAND_SIZE, shuffledDeck.size())),
                        hand,
                        mulligans
                );
            }
            mulligans++;
            events.add(new MulliganPerformedEvent(state.getGameId(), playerId, mulligans, cardIds(hand)));
        }
        throw new SetupException("Unable to draw an initial hand with a Basic Pokemon after " + MAX_MULLIGAN_ATTEMPTS + " mulligans");
    }

    private List<CardInstance> shuffleDeck(GameState state, PlayerId playerId, List<CardInstance> deck, List<GameEvent> events) {
        List<CardInstance> shuffledDeck = deckShuffler.shuffle(deck);
        validateShuffledDeck(deck, shuffledDeck, playerId);
        events.add(new DeckShuffledEvent(state.getGameId(), playerId, shuffledDeck.size()));
        return List.copyOf(shuffledDeck);
    }

    private PlayerGameState applyMulliganBonus(GameState setupState, PlayerGameState playerState, int opponentMulligans, List<GameEvent> events) {
        int cardsToDraw = mulliganBonusDrawPolicy.cardsToDraw(playerState.getPlayerId(), opponentMulligans, setupState);
        if (cardsToDraw < 0 || cardsToDraw > opponentMulligans) {
            throw new SetupException("Mulligan bonus draw policy must return a value from 0 to opponent mulligans");
        }
        if (cardsToDraw == 0) {
            return playerState;
        }
        if (playerState.getDeck().getCards().size() < cardsToDraw) {
            throw new SetupException("Not enough cards in deck to draw mulligan bonus cards");
        }

        List<CardInstance> drawnCards = List.copyOf(playerState.getDeck().getCards().subList(0, cardsToDraw));
        List<CardInstance> remainingDeck = List.copyOf(playerState.getDeck().getCards().subList(cardsToDraw, playerState.getDeck().getCards().size()));
        List<CardInstance> updatedHand = new ArrayList<>(playerState.getHand().getCards());
        updatedHand.addAll(drawnCards);
        events.add(new MulliganBonusCardsDrawnEvent(setupState.getGameId(), playerState.getPlayerId(), cardsToDraw, cardIds(drawnCards)));
        return new PlayerGameState(
                playerState.getPlayerId(),
                new DeckZone(remainingDeck),
                new HandZone(updatedHand),
                playerState.getPrizeCards(),
                playerState.getDiscardPile(),
                playerState.getBoard()
        );
    }

    private PlayerGameState setPrizeCards(GameState state, PlayerGameState playerState, List<GameEvent> events) {
        if (playerState.getDeck().getCards().size() < PRIZE_CARD_COUNT) {
            throw new SetupException("Player deck must contain at least 6 cards to set Prize Cards");
        }
        List<CardInstance> prizeCards = List.copyOf(playerState.getDeck().getCards().subList(0, PRIZE_CARD_COUNT));
        List<CardInstance> remainingDeck = List.copyOf(playerState.getDeck().getCards().subList(PRIZE_CARD_COUNT, playerState.getDeck().getCards().size()));
        events.add(new PrizeCardsSetEvent(state.getGameId(), playerState.getPlayerId(), cardIds(prizeCards)));
        return new PlayerGameState(
                playerState.getPlayerId(),
                new DeckZone(remainingDeck),
                playerState.getHand(),
                new PrizeCards(prizeCards),
                playerState.getDiscardPile(),
                playerState.getBoard()
        );
    }

    private PlayerGameState playerStateAfterInitialDraw(PlayerId playerId, InitialSetupDraw draw) {
        return new PlayerGameState(
                playerId,
                new DeckZone(draw.deck()),
                new HandZone(draw.hand()),
                PrizeCards.empty(),
                DiscardPile.empty(),
                BoardState.empty()
        );
    }

    private void validateDeckForSetup(List<CardInstance> deck, PlayerId owner, String fieldName) {
        if (deck.size() != DECK_SIZE) {
            throw new SetupException(fieldName + " must contain exactly " + DECK_SIZE + " cards");
        }
        Set<CardInstanceId> ids = new HashSet<>();
        for (CardInstance card : deck) {
            if (!owner.equals(card.owner())) {
                throw new SetupException(fieldName + " contains a card not owned by " + owner.value());
            }
            if (!ids.add(card.id())) {
                throw new SetupException(fieldName + " must not contain duplicate card instances");
            }
        }
        if (!hasBasicPokemon(deck)) {
            throw new SetupException(fieldName + " must contain at least one Basic Pokemon to avoid infinite mulligans");
        }
    }

    private void validateShuffledDeck(List<CardInstance> originalDeck, List<CardInstance> shuffledDeck, PlayerId playerId) {
        Objects.requireNonNull(shuffledDeck, "deckShuffler must not return null");
        if (shuffledDeck.stream().anyMatch(Objects::isNull)) {
            throw new SetupException("Shuffled deck must not contain null cards");
        }
        if (shuffledDeck.size() != originalDeck.size()) {
            throw new SetupException("Deck shuffler must preserve deck size");
        }
        Set<CardInstanceId> originalIds = originalDeck.stream().map(CardInstance::id).collect(Collectors.toSet());
        Set<CardInstanceId> shuffledIds = shuffledDeck.stream().map(CardInstance::id).collect(Collectors.toSet());
        if (shuffledIds.size() != shuffledDeck.size() || !originalIds.equals(shuffledIds)) {
            throw new SetupException("Deck shuffler must preserve the same card instances for player " + playerId.value());
        }
    }

    private CardInstance requireBasicPokemonFromHand(Map<CardInstanceId, CardInstance> handById, CardInstanceId cardId, String label) {
        CardInstance card = handById.get(cardId);
        if (card == null) {
            throw new SetupException("Selected " + label + " must be in hand");
        }
        if (!card.definition().isBasicPokemon()) {
            throw new SetupException("Selected " + label + " must be a Basic Pokemon");
        }
        return card;
    }

    private PlayerGameState getPlayerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new SetupException("Player is not part of this game");
    }

    private GameState withUpdatedPlayer(GameState state, PlayerGameState updatedPlayer, GameStatus status, TurnState turnState, List<GameEvent> events) {
        PlayerGameState playerOneState = state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId())
                ? updatedPlayer
                : state.getPlayerOneState();
        PlayerGameState playerTwoState = state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId())
                ? updatedPlayer
                : state.getPlayerTwoState();
        return new GameState(state.getGameId(), status, playerOneState, playerTwoState, turnState, events);
    }

    private void requireStatus(GameState state, GameStatus expectedStatus, String message) {
        if (state.getStatus() != expectedStatus) {
            throw new SetupException(message);
        }
    }

    private void requireActivePokemon(PlayerGameState playerState) {
        if (playerState.getBoard().getActivePokemon().isEmpty()) {
            throw new SetupException("Both players must choose an initial Active Pokemon before setup is completed");
        }
    }

    private boolean hasBasicPokemon(List<CardInstance> cards) {
        return cards.stream().anyMatch(card -> card.definition().isBasicPokemon());
    }

    private List<CardInstanceId> cardIds(List<CardInstance> cards) {
        return cards.stream().map(CardInstance::id).toList();
    }

    private record InitialSetupDraw(List<CardInstance> deck, List<CardInstance> hand, int mulliganCount) {
    }
}
