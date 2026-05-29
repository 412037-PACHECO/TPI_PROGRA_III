package com.tpi.pokemon.game.engine.knockout;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.PokemonType;
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
import com.tpi.pokemon.game.engine.event.CardsDiscardedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PokemonKnockedOutEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KnockoutResolverTest {
    private static final GameId GAME_ID = new GameId("ko-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    private final KnockoutResolver resolver = new KnockoutResolver();

    @Test
    void doesNotDetectKnockoutWhenDamageIsBelowHp() {
        PokemonInPlay pokemon = pokemon("p2-active", PLAYER_TWO, 60).withDamageCounters(5);

        assertThat(resolver.isKnockedOut(pokemon)).isFalse();
    }

    @Test
    void detectsKnockoutWhenDamageCountersTimesTenEqualsHp() {
        PokemonInPlay pokemon = pokemon("p2-active", PLAYER_TWO, 60).withDamageCounters(6);

        assertThat(resolver.isKnockedOut(pokemon)).isTrue();
    }

    @Test
    void detectsKnockoutWhenDamageCountersTimesTenExceedsHp() {
        PokemonInPlay pokemon = pokemon("p2-active", PLAYER_TWO, 60).withDamageCounters(7);

        assertThat(resolver.isKnockedOut(pokemon)).isTrue();
    }

    @Test
    void movesKnockedOutPokemonAndAttachedCardsToOwnersDiscardPile() {
        CardInstance energy = card("p2-energy", PLAYER_TWO, energyDefinition("water-energy"));
        PokemonInPlay active = new PokemonInPlay(card("p2-active", PLAYER_TWO, pokemonDefinition("p2-active-def", 60, Set.of(CardSubtype.BASIC))), new AttachedCards(List.of(energy))).withDamageCounters(6);
        GameState state = game(player(PLAYER_ONE, BoardState.empty()), player(PLAYER_TWO, new BoardState(new ActivePokemon(active), Bench.empty())));
        List<GameEvent> events = new ArrayList<>();

        KnockoutResolver.KnockoutResolution resolution = resolver.resolveActiveKnockout(state, PLAYER_TWO, events).orElseThrow();

        assertThat(resolution.state().getPlayerTwoState().getBoard().getActivePokemon()).isEmpty();
        assertThat(resolution.state().getPlayerTwoState().getDiscardPile().getCards()).extracting(CardInstance::id)
                .containsExactly(new CardInstanceId("p2-active"), new CardInstanceId("p2-energy"));
        assertThat(eventsOfType(events, PokemonKnockedOutEvent.class)).hasSize(1);
        assertThat(eventsOfType(events, CardsDiscardedEvent.class)).hasSize(1);
    }

    @Test
    void movesFullEvolutionStackAndAttachedCardsToOwnersDiscardPile() {
        CardInstance basic = card("p2-basic", PLAYER_TWO, pokemonDefinition("basic-def", 60, Set.of(CardSubtype.BASIC)));
        CardInstance stageOne = card("p2-stage1", PLAYER_TWO, pokemonDefinition("stage1-def", 80, Set.of(CardSubtype.STAGE_1)));
        CardInstance energy = card("p2-energy", PLAYER_TWO, energyDefinition("water-energy"));
        PokemonInPlay active = new PokemonInPlay(List.of(basic, stageOne), new AttachedCards(List.of(energy)), 1, 2, 8);
        GameState state = game(player(PLAYER_ONE, BoardState.empty()), player(PLAYER_TWO, new BoardState(new ActivePokemon(active), Bench.empty())));

        KnockoutResolver.KnockoutResolution resolution = resolver.resolveActiveKnockout(state, PLAYER_TWO, new ArrayList<>()).orElseThrow();

        assertThat(resolution.state().getPlayerTwoState().getDiscardPile().getCards()).extracting(CardInstance::id)
                .containsExactly(new CardInstanceId("p2-basic"), new CardInstanceId("p2-stage1"), new CardInstanceId("p2-energy"));
    }

    private GameState game(PlayerGameState playerOne, PlayerGameState playerTwo) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 2, com.tpi.pokemon.game.domain.enums.TurnPhase.ATTACK, false, false, false, false, false), List.of());
    }

    private PlayerGameState player(PlayerId playerId, BoardState board) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), PrizeCards.empty(), DiscardPile.empty(), board, 1);
    }

    private PokemonInPlay pokemon(String id, PlayerId owner, int hp) {
        return PokemonInPlay.withoutAttachments(card(id, owner, pokemonDefinition(id + "-def", hp, Set.of(CardSubtype.BASIC))));
    }

    private CardDefinitionRef pokemonDefinition(String id, int hp, Set<CardSubtype> subtypes) {
        return new CardDefinitionRef(id, "Pokemon " + id, CardSupertype.POKEMON, subtypes, null, 1, hp, List.of(PokemonType.WATER), List.of(), List.of(), List.of(), EnergyProfile.none());
    }

    private CardDefinitionRef energyDefinition(String id) {
        return new CardDefinitionRef(id, "Energy " + id, CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY), null, null, null, List.of(), List.of(), List.of(), List.of(), EnergyProfile.basic(com.tpi.pokemon.game.domain.enums.EnergyType.WATER));
    }

    private CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }

    private <T extends GameEvent> List<T> eventsOfType(List<GameEvent> events, Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
