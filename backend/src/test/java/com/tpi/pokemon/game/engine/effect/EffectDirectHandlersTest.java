package com.tpi.pokemon.game.engine.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonSwitchedEvent;
import com.tpi.pokemon.game.engine.event.CardsDiscardedEvent;
import com.tpi.pokemon.game.engine.event.DamageCountersPlacedEvent;
import com.tpi.pokemon.game.engine.event.DeckSearchedEvent;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EnergyAttachedEvent;
import com.tpi.pokemon.game.engine.event.EnergyMovedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PendingSelectionRequiredEvent;
import com.tpi.pokemon.game.engine.event.PokemonKnockedOutEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsTakenEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EffectDirectHandlersTest {
    private static final GameId GAME_ID = new GameId("direct-effect-test");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    @Test
    void defaultRegistryContainsDirectGenericHandlers() {
        EffectRegistry registry = EffectRegistry.defaultRegistry();

        assertThat(registry.findHandler(EffectType.SEARCH_DECK)).isPresent();
        assertThat(registry.findHandler(EffectType.SHUFFLE_DECK)).isPresent();
        assertThat(registry.findHandler(EffectType.DISCARD_CARDS)).isPresent();
        assertThat(registry.findHandler(EffectType.ATTACH_ENERGY)).isPresent();
        assertThat(registry.findHandler(EffectType.MOVE_ENERGY)).isPresent();
        assertThat(registry.findHandler(EffectType.SWITCH_ACTIVE)).isPresent();
        assertThat(registry.findHandler(EffectType.PLACE_DAMAGE_COUNTERS)).isPresent();
    }

    @Test
    void searchDeckMovesSelectedCardsMatchingSupertypeFromDeckToHand() {
        CardInstance energy = energy("deck-energy", PLAYER_ONE);
        CardInstance pokemon = pokemonCard("deck-pokemon", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(energy, pokemon), List.of(), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(
                EffectDefinition.searchDeck(EffectTarget.ACTING_PLAYER, 1, CardFilterSpec.supertype(CardSupertype.ENERGY), List.of(energy.id()), true, true, EffectTiming.AFTER_ATTACK),
                context(state, events)
        ).state();

        assertThat(result.getPlayerOneState().getDeck().getCards()).extracting(CardInstance::id).containsExactly(pokemon.id());
        assertThat(result.getPlayerOneState().getHand().getCards()).extracting(CardInstance::id).containsExactly(energy.id());
        assertThat(eventsOfType(events, DeckSearchedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.requiresShuffle()).isTrue());
    }

    @Test
    void searchDeckWithoutSelectionReturnsPendingSelectionWithoutMutatingZones() {
        CardInstance energy = energy("deck-energy", PLAYER_ONE);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(energy), List.of(), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        EffectResult result = new EffectExecutionService().execute(
                EffectDefinition.searchDeck(EffectTarget.ACTING_PLAYER, 2, CardFilterSpec.supertype(CardSupertype.ENERGY), List.of(), true, true, EffectTiming.AFTER_ATTACK),
                context(state, events)
        );

        assertThat(result.pendingSelectionOptional()).hasValueSatisfying(selection -> {
            assertThat(selection.effectType()).isEqualTo(EffectType.SEARCH_DECK);
            assertThat(selection.sourceZone()).isEqualTo(EffectCardZone.DECK);
            assertThat(selection.minSelections()).isZero();
            assertThat(selection.maxSelections()).isEqualTo(2);
            assertThat(selection.revealSelectedCards()).isTrue();
            assertThat(selection.requiresShuffle()).isTrue();
            assertThat(selection.continuationEffect()).isNotNull();
        });
        assertThat(result.state().getPlayerOneState().getDeck().getCards()).extracting(CardInstance::id).containsExactly(energy.id());
        assertThat(eventsOfType(events, PendingSelectionRequiredEvent.class)).hasSize(1);
    }

    @Test
    void shuffleDeckUsesInjectedShuffler() {
        CardInstance one = pokemonCard("one", PLAYER_ONE, 60);
        CardInstance two = pokemonCard("two", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(one, two), List.of(), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        EffectExecutionService service = new EffectExecutionService(new EffectRegistry(List.of(new ShuffleDeckEffectHandler(deck -> List.of(deck.get(1), deck.get(0))))));
        List<GameEvent> events = new ArrayList<>();

        GameState result = service.execute(EffectDefinition.shuffleDeck(EffectTarget.ACTING_PLAYER, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(result.getPlayerOneState().getDeck().getCards()).extracting(CardInstance::id).containsExactly(two.id(), one.id());
        assertThat(eventsOfType(events, DeckShuffledEvent.class)).hasSize(1);
    }

    @Test
    void discardCardsMovesSelectedCardFromHandToDiscard() {
        CardInstance discarded = pokemonCard("discarded", PLAYER_ONE, 60);
        CardInstance remaining = pokemonCard("remaining", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(), List.of(discarded, remaining), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.discardCards(EffectTarget.ACTING_PLAYER, EffectCardZone.HAND, 1, List.of(discarded.id()), EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(result.getPlayerOneState().getHand().getCards()).extracting(CardInstance::id).containsExactly(remaining.id());
        assertThat(result.getPlayerOneState().getDiscardPile().getCards()).extracting(CardInstance::id).containsExactly(discarded.id());
        assertThat(eventsOfType(events, CardsDiscardedEvent.class)).hasSize(1);
    }

    @Test
    void attachEnergyFromHandAttachesToOwnActivePokemon() {
        CardInstance energy = energy("hand-energy", PLAYER_ONE);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(), List.of(energy), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.attachEnergy(EffectTarget.ACTING_PLAYER, EffectCardZone.HAND, List.of(energy.id()), -1, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(result.getPlayerOneState().getHand().getCards()).isEmpty();
        assertThat(activePokemon(result.getPlayerOneState()).getAttachedCards().getEnergies()).extracting(CardInstance::id).containsExactly(energy.id());
        assertThat(eventsOfType(events, EnergyAttachedEvent.class)).hasSize(1);
    }

    @Test
    void attachRainbowEnergyEffectOnlyPlacesCounterWhenSourceZoneIsHand() {
        CardInstance rainbow = energy("rainbow-discard", PLAYER_ONE, EnergyProfile.rainbow());
        PokemonInPlay active = active("p1-active", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active, List.of(), List.of(), List.of(rainbow), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.attachEnergy(EffectTarget.ACTING_PLAYER, EffectCardZone.DISCARD, List.of(rainbow.id()), -1, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(activePokemon(result.getPlayerOneState()).getAttachedCards().getEnergies()).extracting(CardInstance::id).containsExactly(rainbow.id());
        assertThat(activePokemon(result.getPlayerOneState()).getDamageCounters()).isZero();
        assertThat(eventsOfType(events, DamageCountersPlacedEvent.class)).isEmpty();
        assertThat(eventsOfType(events, EnergyAttachedEvent.class)).hasSize(1);
    }

    @Test
    void moveEnergyMovesOwnAttachedEnergyBetweenOwnPokemon() {
        CardInstance energy = energy("attached-energy", PLAYER_ONE);
        PokemonInPlay active = new PokemonInPlay(pokemonCard("p1-active", PLAYER_ONE, 60), new AttachedCards(List.of(energy)));
        PokemonInPlay bench = active("p1-bench", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active, List.of(), List.of(), List.of(), List.of(bench)), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.moveEnergy(EffectTarget.ACTING_PLAYER, List.of(energy.id()), -1, 0, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(activePokemon(result.getPlayerOneState()).getAttachedCards().getEnergies()).isEmpty();
        assertThat(result.getPlayerOneState().getBoard().getBench().getPokemon().get(0).getAttachedCards().getEnergies()).extracting(CardInstance::id).containsExactly(energy.id());
        assertThat(eventsOfType(events, EnergyMovedEvent.class)).hasSize(1);
    }

    @Test
    void moveEnergyRejectsEnergyNotAttachedToSourcePokemon() {
        CardInstance energy = energy("missing-energy", PLAYER_ONE);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(), List.of(), List.of(), List.of(active("p1-bench", PLAYER_ONE, 60))), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));

        assertThatThrownBy(() -> new EffectExecutionService().execute(EffectDefinition.moveEnergy(EffectTarget.ACTING_PLAYER, List.of(energy.id()), -1, 0, EffectTiming.AFTER_ATTACK), context(state, new ArrayList<>())))
                .isInstanceOf(EffectException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void switchActiveSwitchesOwnActiveWithSelectedBenchPokemon() {
        PokemonInPlay active = active("p1-active", PLAYER_ONE, 60);
        PokemonInPlay bench = active("p1-bench", PLAYER_ONE, 60);
        GameState state = game(player(PLAYER_ONE, active, List.of(), List.of(), List.of(), List.of(bench)), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.switchActive(EffectTarget.ACTING_PLAYER, 0, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(activePokemon(result.getPlayerOneState()).getTopCard().id()).isEqualTo(bench.getTopCard().id());
        assertThat(result.getPlayerOneState().getBoard().getBench().getPokemon()).extracting(pokemon -> pokemon.getTopCard().id()).containsExactly(active.getTopCard().id());
        assertThat(eventsOfType(events, ActivePokemonSwitchedEvent.class)).hasSize(1);
    }

    @Test
    void opponentSwitchWithoutSelectionReturnsPendingSelection() {
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), List.of(), List.of(), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO, 60), List.of(), List.of(), List.of(), List.of(active("p2-bench", PLAYER_TWO, 60))));

        EffectResult result = new EffectExecutionService().execute(EffectDefinition.switchActive(EffectTarget.DEFENDER_ACTIVE, -1, EffectTiming.AFTER_ATTACK), context(state, new ArrayList<>()));

        assertThat(result.pendingSelectionOptional()).hasValueSatisfying(selection -> assertThat(selection.playerId()).isEqualTo(PLAYER_TWO));
    }

    @Test
    void placeDamageCountersAddsCountersAndCanKnockOutActive() {
        PokemonInPlay defender = active("p2-active", PLAYER_TWO, 60).withDamageCounters(5);
        GameState state = game(playerWithPrizes(PLAYER_ONE, active("p1-active", PLAYER_ONE, 60), prizes(PLAYER_ONE, 6)), player(PLAYER_TWO, defender, List.of(), List.of(), List.of(), List.of(active("p2-bench", PLAYER_TWO, 60))));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(EffectDefinition.placeDamageCounters(EffectTarget.DEFENDER_ACTIVE, 1, -1, EffectTiming.AFTER_ATTACK), context(state, events)).state();

        assertThat(result.getPlayerTwoState().getBoard().getActivePokemon()).isEmpty();
        assertThat(result.getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(5);
        assertThat(result.getPendingActiveReplacement()).hasValueSatisfying(pending -> assertThat(pending.playerId()).isEqualTo(PLAYER_TWO));
        assertThat(eventsOfType(events, DamageCountersPlacedEvent.class)).hasSize(1);
        assertThat(eventsOfType(events, PokemonKnockedOutEvent.class)).hasSize(1);
        assertThat(eventsOfType(events, PrizeCardsTakenEvent.class)).hasSize(1);
    }

    private EffectExecutionContext context(GameState state, List<GameEvent> events) {
        return new EffectExecutionContext(state, PLAYER_ONE, PLAYER_TWO, "direct-effect", events, () -> CoinFlipResult.HEADS);
    }

    private GameState game(PlayerGameState playerOne, PlayerGameState playerTwo) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 2, com.tpi.pokemon.game.domain.enums.TurnPhase.ATTACK, false, false, false, false, false), List.of());
    }

    private PlayerGameState player(PlayerId playerId, PokemonInPlay active, List<CardInstance> deck, List<CardInstance> hand, List<CardInstance> discard, List<PokemonInPlay> bench) {
        return new PlayerGameState(playerId, new DeckZone(deck), new HandZone(hand), PrizeCards.empty(), new DiscardPile(discard), new BoardState(new ActivePokemon(active), new Bench(bench)), 1);
    }

    private PlayerGameState playerWithPrizes(PlayerId playerId, PokemonInPlay active, List<CardInstance> prizes) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), new PrizeCards(prizes), DiscardPile.empty(), new BoardState(new ActivePokemon(active), Bench.empty()), 1);
    }

    private PokemonInPlay active(String id, PlayerId owner, int hp) {
        return PokemonInPlay.withoutAttachments(pokemonCard(id, owner, hp));
    }

    private PokemonInPlay activePokemon(PlayerGameState player) {
        return player.getBoard().getActivePokemon().orElseThrow().getPokemon();
    }

    private CardInstance pokemonCard(String id, PlayerId owner, int hp) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Pokemon " + id, CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 1, hp, List.of(), List.of(), List.of(), List.of(), EnergyProfile.none()), owner);
    }

    private CardInstance energy(String id, PlayerId owner) {
        return energy(id, owner, EnergyProfile.basic(EnergyType.WATER));
    }

    private CardInstance energy(String id, PlayerId owner, EnergyProfile profile) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Energy " + id, CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY), null, null, null, List.of(), List.of(), List.of(), List.of(), profile), owner);
    }

    private List<CardInstance> prizes(PlayerId owner, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> new CardInstance(new CardInstanceId(owner.value() + "-prize-" + index), new CardDefinitionRef("prize-def-" + index, "Prize " + index), owner))
                .toList();
    }

    private <T extends GameEvent> List<T> eventsOfType(List<GameEvent> events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
