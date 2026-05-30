package com.tpi.pokemon.game.engine.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
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
import com.tpi.pokemon.game.engine.event.CardDrawEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EnergyDiscardedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.HealEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionAppliedEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EffectExecutionServiceTest {
    private static final GameId GAME_ID = new GameId("effect-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    @Test
    void registryReturnsHandlerAndFailsClearlyWhenMissing() {
        EffectRegistry registry = new EffectRegistry(List.of(new HealDamageEffectHandler()));

        assertThat(registry.findHandler(EffectType.HEAL_DAMAGE)).isPresent();
        assertThatThrownBy(() -> registry.handlerFor(EffectType.DRAW_CARDS))
                .isInstanceOf(EffectException.class)
                .hasMessageContaining("No handler registered")
                .hasMessageContaining("DRAW_CARDS");
    }

    @Test
    void applySpecialConditionEffectAppliesConditionToTarget() {
        List<GameEvent> events = new ArrayList<>();
        GameState result = new EffectExecutionService().execute(
                EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE),
                context(activeBattle(), events)
        ).state();

        assertThat(activePokemon(result.getPlayerTwoState()).hasSpecialCondition(SpecialCondition.POISONED)).isTrue();
        assertThat(eventsOfType(events, SpecialConditionAppliedEvent.class)).hasSize(1);
    }

    @Test
    void healDamageEffectNeverHealsBelowZero() {
        PokemonInPlay damaged = activePokemon(activeBattle().getPlayerOneState()).withDamageCounters(1);
        GameState state = game(player(PLAYER_ONE, damaged, List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(
                EffectDefinition.healDamage(EffectTarget.ATTACKER_ACTIVE, 30, EffectTiming.AFTER_DAMAGE),
                context(state, events)
        ).state();

        assertThat(activePokemon(result.getPlayerOneState()).getDamageCounters()).isZero();
        assertThat(eventsOfType(events, HealEffectResolvedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.actualHealedAmount()).isEqualTo(10));
    }

    @Test
    void drawCardsEffectDrawsOnlyAvailableCards() {
        CardInstance deckOne = card("deck-1", PLAYER_ONE);
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE), List.of(deckOne), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(
                EffectDefinition.drawCards(EffectTarget.ATTACKER_ACTIVE, 3, EffectTiming.AFTER_ATTACK),
                context(state, events)
        ).state();

        assertThat(result.getPlayerOneState().getDeck().getCards()).isEmpty();
        assertThat(result.getPlayerOneState().getHand().getCards()).extracting(CardInstance::id).containsExactly(deckOne.id());
        assertThat(eventsOfType(events, CardDrawEffectResolvedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.requestedCount()).isEqualTo(3));
    }

    @Test
    void coinFlipEffectExecutesSelectedBranch() {
        List<GameEvent> events = new ArrayList<>();
        GameState result = new EffectExecutionService().execute(
                EffectDefinition.coinFlip(
                        EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.PARALYZED, EffectTiming.AFTER_DAMAGE),
                        EffectDefinition.applySpecialCondition(EffectTarget.DEFENDER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE),
                        EffectTiming.AFTER_DAMAGE
                ),
                new EffectExecutionContext(activeBattle(), PLAYER_ONE, PLAYER_TWO, "coin", events, () -> CoinFlipResult.HEADS)
        ).state();

        assertThat(activePokemon(result.getPlayerTwoState()).hasSpecialCondition(SpecialCondition.PARALYZED)).isTrue();
        assertThat(activePokemon(result.getPlayerTwoState()).hasSpecialCondition(SpecialCondition.POISONED)).isFalse();
    }

    @Test
    void compositeEffectExecutesChildrenInDeclaredOrder() {
        PokemonInPlay damaged = active("p1-active", PLAYER_ONE).withDamageCounters(2);
        GameState state = game(player(PLAYER_ONE, damaged, List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO), List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(
                EffectDefinition.composite(List.of(
                        EffectDefinition.healDamage(EffectTarget.ATTACKER_ACTIVE, 10, EffectTiming.AFTER_DAMAGE),
                        EffectDefinition.applySpecialCondition(EffectTarget.ATTACKER_ACTIVE, SpecialCondition.POISONED, EffectTiming.AFTER_DAMAGE)
                ), EffectTiming.AFTER_DAMAGE),
                context(state, events)
        ).state();

        assertThat(activePokemon(result.getPlayerOneState()).getDamageCounters()).isEqualTo(1);
        assertThat(activePokemon(result.getPlayerOneState()).hasSpecialCondition(SpecialCondition.POISONED)).isTrue();
        assertThat(events.indexOf(eventsOfType(events, HealEffectResolvedEvent.class).get(0)))
                .isLessThan(events.indexOf(eventsOfType(events, SpecialConditionAppliedEvent.class).get(0)));
    }

    @Test
    void discardAttachedEnergyEffectMovesEnergyToDiscard() {
        CardInstance energy = energy("p2-energy", PLAYER_TWO);
        PokemonInPlay defender = new PokemonInPlay(card("p2-active", PLAYER_TWO), new AttachedCards(List.of(energy)));
        GameState state = game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE), List.of(), List.of()), player(PLAYER_TWO, defender, List.of(), List.of()));
        List<GameEvent> events = new ArrayList<>();

        GameState result = new EffectExecutionService().execute(
                EffectDefinition.discardAttachedEnergy(EffectTarget.DEFENDER_ACTIVE, 1, List.of(energy.id()), EffectTiming.AFTER_ATTACK),
                context(state, events)
        ).state();

        assertThat(activePokemon(result.getPlayerTwoState()).getAttachedCards().getEnergies()).isEmpty();
        assertThat(result.getPlayerTwoState().getDiscardPile().getCards()).extracting(CardInstance::id).containsExactly(energy.id());
        assertThat(eventsOfType(events, EnergyDiscardedEvent.class)).hasSize(1);
    }

    private EffectExecutionContext context(GameState state, List<GameEvent> events) {
        return new EffectExecutionContext(state, PLAYER_ONE, PLAYER_TWO, "test-effect", events, () -> CoinFlipResult.HEADS);
    }

    private GameState activeBattle() {
        return game(player(PLAYER_ONE, active("p1-active", PLAYER_ONE), List.of(), List.of()), player(PLAYER_TWO, active("p2-active", PLAYER_TWO), List.of(), List.of()));
    }

    private GameState game(PlayerGameState playerOne, PlayerGameState playerTwo) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.ATTACK, false, false, false, false, false), List.of());
    }

    private PlayerGameState player(PlayerId playerId, PokemonInPlay active, List<CardInstance> deck, List<CardInstance> hand) {
        return new PlayerGameState(playerId, new DeckZone(deck), new HandZone(hand), PrizeCards.empty(), DiscardPile.empty(), new BoardState(new ActivePokemon(active), Bench.empty()), 1);
    }

    private PokemonInPlay active(String id, PlayerId owner) {
        return PokemonInPlay.withoutAttachments(card(id, owner));
    }

    private PokemonInPlay activePokemon(PlayerGameState player) {
        return player.getBoard().getActivePokemon().orElseThrow().getPokemon();
    }

    private CardInstance card(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Pokemon " + id, CardSupertype.POKEMON, Set.of(CardSubtype.BASIC)), owner);
    }

    private CardInstance energy(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Energy", CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY), null, null, null, List.of(), List.of(), List.of(), List.of(), EnergyProfile.basic(EnergyType.WATER)), owner);
    }

    private <T extends GameEvent> List<T> eventsOfType(List<GameEvent> events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
