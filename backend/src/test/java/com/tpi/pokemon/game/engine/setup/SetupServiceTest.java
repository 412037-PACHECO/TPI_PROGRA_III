package com.tpi.pokemon.game.engine.setup;

import static com.tpi.pokemon.game.GameStateFixtures.GAME_ID;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_ONE;
import static com.tpi.pokemon.game.GameStateFixtures.PLAYER_TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.InitialActivePokemonSelectedEvent;
import com.tpi.pokemon.game.engine.event.InitialBenchSelectedEvent;
import com.tpi.pokemon.game.engine.event.InitialHandDrawnEvent;
import com.tpi.pokemon.game.engine.event.MulliganBonusCardsDrawnEvent;
import com.tpi.pokemon.game.engine.event.MulliganPerformedEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsSetEvent;
import com.tpi.pokemon.game.engine.event.SetupCompletedEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import com.tpi.pokemon.game.engine.random.StartingPlayerSelector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SetupServiceTest {
    private static final CardDefinitionRef BASIC_POKEMON = new CardDefinitionRef(
            "basic-pokemon",
            "Basic Pokemon",
            CardSupertype.POKEMON,
            Set.of(CardSubtype.BASIC)
    );
    private static final CardDefinitionRef STAGE_1_POKEMON = new CardDefinitionRef(
            "stage-1-pokemon",
            "Stage 1 Pokemon",
            CardSupertype.POKEMON,
            Set.of(CardSubtype.STAGE_1)
    );
    private static final CardDefinitionRef TRAINER = new CardDefinitionRef(
            "trainer",
            "Trainer",
            CardSupertype.TRAINER,
            Set.of(CardSubtype.ITEM)
    );

    @Test
    void startSetupWithValidDecksDrawsSevenCardsAndNoMulligans() {
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_ONE), drawNoBonusCards());
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(0));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));

        GameState setup = service.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck));

        assertThat(setup.getStatus()).isEqualTo(GameStatus.SETUP);
        assertThat(setup.getPlayerOneState().getHand().getCards()).hasSize(7);
        assertThat(setup.getPlayerTwoState().getHand().getCards()).hasSize(7);
        assertThat(setup.getPlayerOneState().getDeck().getCards()).hasSize(53);
        assertThat(setup.getPlayerTwoState().getDeck().getCards()).hasSize(53);
        assertThat(eventsOfType(setup, InitialHandDrawnEvent.class)).hasSize(2);
        assertThat(eventsOfType(setup, MulliganPerformedEvent.class)).isEmpty();
    }

    @Test
    void startSetupRepeatsAfterOneMulliganUntilInitialHandHasBasicPokemon() {
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(10));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));
        SetupService service = setupService(
                sequenceShuffler(playerOneDeck, orderedWithBasicAt(playerOneDeck, 0), playerTwoDeck),
                fixedStartingPlayer(PLAYER_ONE),
                drawNoBonusCards()
        );

        GameState setup = service.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck));

        assertThat(setup.getPlayerOneState().getHand().getCards()).anyMatch(card -> card.definition().isBasicPokemon());
        assertThat(setup.getPlayerOneState().getHand().getCards()).hasSize(7);
        assertThat(eventsOfType(setup, InitialHandDrawnEvent.class)).hasSize(3);
        assertThat(eventsOfType(setup, MulliganPerformedEvent.class))
                .extracting(MulliganPerformedEvent::playerId, MulliganPerformedEvent::mulliganNumber)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(PLAYER_ONE, 1));
    }

    @Test
    void startSetupSupportsRepeatedMulligansUntilBasicPokemonIsFound() {
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(20));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));
        SetupService service = setupService(
                sequenceShuffler(playerOneDeck, orderedWithBasicAt(playerOneDeck, 10), orderedWithBasicAt(playerOneDeck, 0), playerTwoDeck),
                fixedStartingPlayer(PLAYER_ONE),
                drawNoBonusCards()
        );

        GameState setup = service.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck));

        assertThat(eventsOfType(setup, MulliganPerformedEvent.class))
                .extracting(MulliganPerformedEvent::playerId, MulliganPerformedEvent::mulliganNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(PLAYER_ONE, 1),
                        org.assertj.core.groups.Tuple.tuple(PLAYER_ONE, 2)
                );
    }

    @Test
    void startSetupTracksMulligansPerPlayerAndAppliesOpponentBonusDrawPolicy() {
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(20));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));
        SetupService service = setupService(
                sequenceShuffler(playerOneDeck, orderedWithBasicAt(playerOneDeck, 10), orderedWithBasicAt(playerOneDeck, 0), playerTwoDeck),
                fixedStartingPlayer(PLAYER_ONE),
                drawAllOpponentMulligans()
        );

        GameState setup = service.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck));

        assertThat(setup.getPlayerOneState().getHand().getCards()).hasSize(7);
        assertThat(setup.getPlayerOneState().getDeck().getCards()).hasSize(53);
        assertThat(setup.getPlayerTwoState().getHand().getCards()).hasSize(9);
        assertThat(setup.getPlayerTwoState().getDeck().getCards()).hasSize(51);
        assertThat(eventsOfType(setup, MulliganBonusCardsDrawnEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.playerId()).isEqualTo(PLAYER_TWO);
                    assertThat(event.count()).isEqualTo(2);
                    assertThat(event.cardIds()).hasSize(2);
                });
    }

    @Test
    void startSetupRejectsMulliganBonusPolicyDrawingMoreThanOpponentMulligansOrNegativeCards() {
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(10));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));

        SetupService tooMany = setupService(
                sequenceShuffler(playerOneDeck, orderedWithBasicAt(playerOneDeck, 0), playerTwoDeck),
                fixedStartingPlayer(PLAYER_ONE),
                (playerId, opponentMulligans, state) -> opponentMulligans + 1
        );
        assertThatThrownBy(() -> tooMany.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Mulligan bonus draw policy");

        SetupService negative = setupService(
                sequenceShuffler(playerOneDeck, orderedWithBasicAt(playerOneDeck, 0), playerTwoDeck),
                fixedStartingPlayer(PLAYER_ONE),
                (playerId, opponentMulligans, state) -> playerId.equals(PLAYER_TWO) ? -1 : 0
        );
        assertThatThrownBy(() -> negative.startSetup(createdGame(), new StartSetupCommand(playerOneDeck, playerTwoDeck)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Mulligan bonus draw policy");
    }

    @Test
    void startSetupRejectsInvalidDeckSizeDeckWithoutBasicPokemonAndWrongOwnership() {
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_ONE), drawNoBonusCards());
        List<CardInstance> playerOneDeck = deck(PLAYER_ONE, "p1", Set.of(0));
        List<CardInstance> playerTwoDeck = deck(PLAYER_TWO, "p2", Set.of(0));

        assertThatThrownBy(() -> service.startSetup(createdGame(), new StartSetupCommand(playerOneDeck.subList(0, 59), playerTwoDeck)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("exactly 60");
        assertThatThrownBy(() -> service.startSetup(createdGame(), new StartSetupCommand(deck(PLAYER_ONE, "p1-no-basic", Set.of()), playerTwoDeck)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Basic Pokemon");

        List<CardInstance> wrongOwnershipDeck = new ArrayList<>(playerOneDeck);
        wrongOwnershipDeck.set(1, card("wrong-owner", PLAYER_TWO, TRAINER));
        assertThatThrownBy(() -> service.startSetup(createdGame(), new StartSetupCommand(wrongOwnershipDeck, playerTwoDeck)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("not owned");
    }

    @Test
    void chooseInitialPokemonMovesValidActiveAndBenchFromHandToBoard() {
        CardInstance active = card("active", PLAYER_ONE, BASIC_POKEMON);
        CardInstance benchOne = card("bench-1", PLAYER_ONE, BASIC_POKEMON);
        CardInstance benchTwo = card("bench-2", PLAYER_ONE, BASIC_POKEMON);
        CardInstance trainer = card("trainer", PLAYER_ONE, TRAINER);
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_ONE), drawNoBonusCards());

        GameState updated = service.chooseInitialPokemon(
                setupStateWithPlayerOneHand(List.of(active, benchOne, benchTwo, trainer)),
                chooseCommand(active, benchOne, benchTwo)
        );

        assertThat(updated.getPlayerOneState().getBoard().getActivePokemon()).isPresent();
        assertThat(updated.getPlayerOneState().getBoard().getActivePokemon().orElseThrow().getPokemon().getBaseCard()).isEqualTo(active);
        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon())
                .extracting(pokemon -> pokemon.getBaseCard().id())
                .containsExactly(benchOne.id(), benchTwo.id());
        assertThat(updated.getPlayerOneState().getHand().getCards()).containsExactly(trainer);
        assertThat(eventsOfType(updated, InitialActivePokemonSelectedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, InitialBenchSelectedEvent.class)).hasSize(1);
    }

    @Test
    void chooseInitialPokemonRejectsInvalidActiveAndBenchSelections() {
        CardInstance basic = card("basic", PLAYER_ONE, BASIC_POKEMON);
        CardInstance otherBasic = card("other-basic", PLAYER_ONE, BASIC_POKEMON);
        CardInstance trainer = card("trainer", PLAYER_ONE, TRAINER);
        CardInstance stageOne = card("stage-one", PLAYER_ONE, STAGE_1_POKEMON);
        List<CardInstance> tooManySelectedPokemon = IntStream.range(0, 7)
                .mapToObj(index -> card("bench-" + index, PLAYER_ONE, BASIC_POKEMON))
                .toList();
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_ONE), drawNoBonusCards());

        assertThatThrownBy(() -> service.chooseInitialPokemon(setupStateWithPlayerOneHand(List.of(basic)), new ChooseInitialPokemonCommand(PLAYER_ONE, id("missing"), List.of())))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("must be in hand");
        assertThatThrownBy(() -> service.chooseInitialPokemon(setupStateWithPlayerOneHand(List.of(trainer)), new ChooseInitialPokemonCommand(PLAYER_ONE, trainer.id(), List.of())))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Basic Pokemon");
        assertThatThrownBy(() -> service.chooseInitialPokemon(
                setupStateWithPlayerOneHand(tooManySelectedPokemon),
                new ChooseInitialPokemonCommand(PLAYER_ONE, tooManySelectedPokemon.get(0).id(), tooManySelectedPokemon.stream().skip(1).map(CardInstance::id).toList())
        ))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("more than 5");
        assertThatThrownBy(() -> service.chooseInitialPokemon(setupStateWithPlayerOneHand(List.of(basic, stageOne)), chooseCommand(basic, stageOne)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Basic Pokemon");
        assertThatThrownBy(() -> service.chooseInitialPokemon(setupStateWithPlayerOneHand(List.of(basic, otherBasic)), chooseCommand(basic, basic)))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void chooseInitialPokemonAllowsUpToFiveBenchPokemon() {
        CardInstance active = card("active", PLAYER_ONE, BASIC_POKEMON);
        List<CardInstance> bench = IntStream.range(0, 5)
                .mapToObj(index -> card("bench-" + index, PLAYER_ONE, BASIC_POKEMON))
                .toList();
        List<CardInstance> hand = new ArrayList<>();
        hand.add(active);
        hand.addAll(bench);
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_ONE), drawNoBonusCards());

        GameState updated = service.chooseInitialPokemon(
                setupStateWithPlayerOneHand(hand),
                new ChooseInitialPokemonCommand(PLAYER_ONE, active.id(), bench.stream().map(CardInstance::id).toList())
        );

        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon()).hasSize(5);
        assertThat(updated.getPlayerOneState().getHand().getCards()).isEmpty();
    }

    @Test
    void completeSetupRequiresBothActivePokemonSetsPrizesAndPreparesFirstTurnWithoutDrawing() {
        SetupService service = setupService(identityShuffler(), fixedStartingPlayer(PLAYER_TWO), drawNoBonusCards());
        GameState setup = service.startSetup(createdGame(), new StartSetupCommand(deck(PLAYER_ONE, "p1", Set.of(0)), deck(PLAYER_TWO, "p2", Set.of(0))));

        CardInstance playerOneActive = setup.getPlayerOneState().getHand().getCards().stream().filter(card -> card.definition().isBasicPokemon()).findFirst().orElseThrow();
        CardInstance playerTwoActive = setup.getPlayerTwoState().getHand().getCards().stream().filter(card -> card.definition().isBasicPokemon()).findFirst().orElseThrow();
        GameState withPlayerOneActive = service.chooseInitialPokemon(setup, new ChooseInitialPokemonCommand(PLAYER_ONE, playerOneActive.id(), List.of()));

        assertThatThrownBy(() -> service.completeSetup(withPlayerOneActive))
                .isInstanceOf(SetupException.class)
                .hasMessageContaining("Both players");

        GameState ready = service.chooseInitialPokemon(withPlayerOneActive, new ChooseInitialPokemonCommand(PLAYER_TWO, playerTwoActive.id(), List.of()));
        int playerOneHandBeforeComplete = ready.getPlayerOneState().getHand().getCards().size();
        int playerTwoHandBeforeComplete = ready.getPlayerTwoState().getHand().getCards().size();

        GameState active = service.completeSetup(ready);

        assertThat(active.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(active.getPlayerOneState().getPrizeCards().getCards()).hasSize(6);
        assertThat(active.getPlayerTwoState().getPrizeCards().getCards()).hasSize(6);
        assertThat(active.getPlayerOneState().getDeck().getCards()).hasSize(47);
        assertThat(active.getPlayerTwoState().getDeck().getCards()).hasSize(47);
        assertThat(active.getPlayerOneState().getHand().getCards()).hasSize(playerOneHandBeforeComplete);
        assertThat(active.getPlayerTwoState().getHand().getCards()).hasSize(playerTwoHandBeforeComplete);
        assertThat(active.getTurnState()).isEqualTo(TurnState.preparedForFirstTurn(PLAYER_TWO));
        assertThat(active.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(active.getTurnState().turnNumber()).isZero();
        assertThat(active.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
        assertThat(eventsOfType(active, PrizeCardsSetEvent.class)).hasSize(2);
        assertThat(eventsOfType(active, SetupCompletedEvent.class)).hasSize(1);
    }

    private static SetupService setupService(DeckShuffler deckShuffler, StartingPlayerSelector startingPlayerSelector, MulliganBonusDrawPolicy policy) {
        return new SetupService(deckShuffler, startingPlayerSelector, policy);
    }

    private static DeckShuffler identityShuffler() {
        return List::copyOf;
    }

    private static DeckShuffler sequenceShuffler(List<CardInstance>... shuffledDecks) {
        ArrayDeque<List<CardInstance>> sequence = new ArrayDeque<>(List.of(shuffledDecks));
        return ignored -> List.copyOf(sequence.removeFirst());
    }

    private static StartingPlayerSelector fixedStartingPlayer(PlayerId playerId) {
        return (playerOne, playerTwo) -> playerId;
    }

    private static MulliganBonusDrawPolicy drawNoBonusCards() {
        return (playerId, opponentMulligans, state) -> 0;
    }

    private static MulliganBonusDrawPolicy drawAllOpponentMulligans() {
        return (playerId, opponentMulligans, state) -> opponentMulligans;
    }

    private static GameState createdGame() {
        return GameState.created(GAME_ID, PLAYER_ONE, PLAYER_TWO);
    }

    private static GameState setupStateWithPlayerOneHand(List<CardInstance> hand) {
        return new GameState(
                GAME_ID,
                GameStatus.SETUP,
                new PlayerGameState(PLAYER_ONE, DeckZone.empty(), new HandZone(hand), PrizeCards.empty(), DiscardPile.empty(), BoardState.empty()),
                PlayerGameState.empty(PLAYER_TWO),
                TurnState.notStarted(),
                List.of()
        );
    }

    private static ChooseInitialPokemonCommand chooseCommand(CardInstance active, CardInstance... bench) {
        return new ChooseInitialPokemonCommand(PLAYER_ONE, active.id(), List.of(bench).stream().map(CardInstance::id).toList());
    }

    private static List<CardInstance> deck(PlayerId owner, String prefix, Set<Integer> basicIndexes) {
        return IntStream.range(0, 60)
                .mapToObj(index -> card(prefix + "-" + index, owner, basicIndexes.contains(index) ? BASIC_POKEMON : TRAINER))
                .toList();
    }

    private static List<CardInstance> orderedWithBasicAt(List<CardInstance> deck, int targetIndex) {
        CardInstance basic = deck.stream().filter(card -> card.definition().isBasicPokemon()).findFirst().orElseThrow();
        List<CardInstance> ordered = new ArrayList<>(deck);
        ordered.remove(basic);
        ordered.add(targetIndex, basic);
        return List.copyOf(ordered);
    }

    private static CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(id(id), definition, owner);
    }

    private static CardInstanceId id(String id) {
        return new CardInstanceId(id);
    }

    private static <T extends GameEvent> List<T> eventsOfType(GameState state, Class<T> type) {
        return state.getEvents().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
