package com.tpi.pokemon.game.engine.attack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
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
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.AttackDeclaredEvent;
import com.tpi.pokemon.game.engine.event.AttackResolvedEvent;
import com.tpi.pokemon.game.engine.event.DamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.DamageCalculatedEvent;
import com.tpi.pokemon.game.engine.event.EnergyCostValidatedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.TurnEndedEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttackServiceTest {
    private static final GameId GAME_ID = new GameId("attack-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");
    private static final AttackDefinition SCRATCH = new AttackDefinition("scratch", "Scratch", List.of(EnergyType.COLORLESS), 30);
    private static final AttackDefinition FIRE_BLAST = new AttackDefinition("fire-blast", "Fire Blast", List.of(EnergyType.FIRE), 40);

    private final AttackService attackService = new AttackService();

    @Test
    void appliesBaseDamageToOpposingActivePokemon() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        PokemonInPlay defender = activePokemon(result.getPlayerTwoState());
        assertThat(defender.getDamageCounters()).isEqualTo(3);
    }

    @Test
    void acceptsAttackWithExactSpecificEnergyCost() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(FIRE_BLAST), List.of(energy("p1-fire", PLAYER_ONE, EnergyType.FIRE))), activeDefender()),
                command("fire-blast")
        );

        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isEqualTo(4);
    }

    @Test
    void accumulatesDamageCountersOverExistingDamage() {
        PokemonInPlay damagedDefender = activeDefender().withDamageCounters(2);

        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), damagedDefender),
                command("scratch")
        );

        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isEqualTo(5);
    }

    @Test
    void endsTurnAutomaticallyAfterAttackResolution() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        assertThat(result.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(result.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
    }

    @Test
    void appendsMainAttackAndTurnEndEvents() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        assertThat(eventsOfType(result, AttackDeclaredEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, EnergyCostValidatedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, DamageCalculatedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, DamageAppliedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, AttackResolvedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, TurnEndedEvent.class)).hasSize(1);
    }

    @Test
    void rejectsAttackWithoutEnoughEnergy() {
        GameState state = activeGame(
                activeAttacker(List.of(FIRE_BLAST), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("fire-blast")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Not enough energy to declare attack");
    }

    @Test
    void rejectsAttackWhenPlayerIsNotCurrentPlayer() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, new DeclareAttackCommand(GAME_ID, PLAYER_TWO, "scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Only the current player can attack");
    }

    @Test
    void rejectsAttackOutsideMainPhase() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender(),
                new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.DRAW, false, false, false, false, false)
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attack can only be declared from MAIN phase");
    }

    @Test
    void rejectsAttackWithoutActiveAttacker() {
        GameState state = activeGame(player(PLAYER_ONE, BoardState.empty()), playerWithActive(PLAYER_TWO, activeDefender()), defaultTurn());

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attacking player has no active Pokemon");
    }

    @Test
    void rejectsAttackWithoutActiveDefender() {
        GameState state = activeGame(
                playerWithActive(PLAYER_ONE, activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)))),
                player(PLAYER_TWO, BoardState.empty()),
                defaultTurn()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Defending player has no active Pokemon");
    }

    @Test
    void rejectsAttackThatDoesNotBelongToActivePokemon() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("missing-attack")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attack does not exist on active Pokemon");
    }

    @Test
    void rejectsStartingPlayerAttackOnTheirFirstTurn() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender(),
                new TurnState(PLAYER_ONE, PLAYER_ONE, 1, TurnPhase.MAIN, false, false, false, false, false)
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Starting player cannot attack on their first turn");
    }

    private GameState activeGame(PokemonInPlay attacker, PokemonInPlay defender) {
        return activeGame(attacker, defender, defaultTurn());
    }

    private GameState activeGame(PokemonInPlay attacker, PokemonInPlay defender, TurnState turnState) {
        return activeGame(playerWithActive(PLAYER_ONE, attacker), playerWithActive(PLAYER_TWO, defender), turnState);
    }

    private GameState activeGame(PlayerGameState playerOne, PlayerGameState playerTwo, TurnState turnState) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, turnState, List.of());
    }

    private TurnState defaultTurn() {
        return new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.MAIN, false, false, false, false, false);
    }

    private PlayerGameState playerWithActive(PlayerId playerId, PokemonInPlay active) {
        return player(playerId, new BoardState(new ActivePokemon(active), Bench.empty()));
    }

    private PlayerGameState player(PlayerId playerId, BoardState board) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), PrizeCards.empty(), DiscardPile.empty(), board, 1);
    }

    private PokemonInPlay activeAttacker(List<AttackDefinition> attacks, List<CardInstance> energies) {
        return new PokemonInPlay(
                card("p1-active", PLAYER_ONE, pokemonDefinition("p1-active-def", PokemonType.FIRE, attacks, List.of(), List.of())),
                new AttachedCards(energies)
        );
    }

    private PokemonInPlay activeDefender() {
        return PokemonInPlay.withoutAttachments(
                card("p2-active", PLAYER_TWO, pokemonDefinition("p2-active-def", PokemonType.GRASS, List.of(), List.of(), List.of()))
        );
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances) {
        return new CardDefinitionRef(
                id,
                "Pokemon " + id,
                CardSupertype.POKEMON,
                Set.of(CardSubtype.BASIC),
                null,
                1,
                60,
                List.of(type),
                attacks,
                weaknesses,
                resistances,
                EnergyProfile.none()
        );
    }

    private CardInstance energy(String id, PlayerId owner, EnergyType type) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                type + " Energy",
                CardSupertype.ENERGY,
                Set.of(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.basic(type)
        );
        return card(id, owner, definition);
    }

    private CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }

    private DeclareAttackCommand command(String attackId) {
        return new DeclareAttackCommand(GAME_ID, PLAYER_ONE, attackId);
    }

    private PokemonInPlay activePokemon(PlayerGameState player) {
        return player.getBoard().getActivePokemon().orElseThrow().getPokemon();
    }

    private <T extends GameEvent> List<T> eventsOfType(GameState state, Class<T> type) {
        return state.getEvents().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
