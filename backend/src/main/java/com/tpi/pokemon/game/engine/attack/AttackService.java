package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.damage.DamageCalculation;
import com.tpi.pokemon.game.engine.damage.DamageCalculator;
import com.tpi.pokemon.game.engine.event.AttackDeclaredEvent;
import com.tpi.pokemon.game.engine.event.AttackResolvedEvent;
import com.tpi.pokemon.game.engine.event.DamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.DamageCalculatedEvent;
import com.tpi.pokemon.game.engine.event.EnergyCostValidatedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.turn.EndTurnCommand;
import com.tpi.pokemon.game.engine.turn.TurnManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AttackService {
    private final TurnManager turnManager;
    private final EnergyCostValidator energyCostValidator;
    private final DamageCalculator damageCalculator;

    public AttackService() {
        this(new TurnManager());
    }

    public AttackService(TurnManager turnManager) {
        this(turnManager, new EnergyCostValidator(), new DamageCalculator());
    }

    public AttackService(TurnManager turnManager, EnergyCostValidator energyCostValidator, DamageCalculator damageCalculator) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
        this.energyCostValidator = Objects.requireNonNull(energyCostValidator, "energyCostValidator must not be null");
        this.damageCalculator = Objects.requireNonNull(damageCalculator, "damageCalculator must not be null");
    }

    public GameState declareAttack(GameState state, DeclareAttackCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");

        validateTurn(state, command.playerId());
        PlayerGameState attackerPlayer = getPlayerState(state, command.playerId());
        PlayerGameState defenderPlayer = getPlayerState(state, opponentOf(state, command.playerId()));
        PokemonInPlay attacker = attackerPlayer.getBoard().getActivePokemon()
                .map(ActivePokemon::getPokemon)
                .orElseThrow(() -> new AttackException("Attacking player has no active Pokemon"));
        PokemonInPlay defender = defenderPlayer.getBoard().getActivePokemon()
                .map(ActivePokemon::getPokemon)
                .orElseThrow(() -> new AttackException("Defending player has no active Pokemon"));
        AttackDefinition attack = attacker.getTopCard().definition().attackById(command.attackId())
                .orElseThrow(() -> new AttackException("Attack does not exist on active Pokemon"));

        if (!energyCostValidator.hasEnoughEnergy(attacker, attack)) {
            throw new AttackException("Not enough energy to declare attack");
        }
        DamageCalculation damage = damageCalculator.calculate(attacker, defender, attack);

        PokemonInPlay damagedDefender = defender.applyDamage(damage.finalDamage());
        PlayerGameState updatedDefenderPlayer = withActivePokemon(defenderPlayer, damagedDefender);

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new AttackDeclaredEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId(), attack.name()));
        events.add(new EnergyCostValidatedEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId()));
        events.add(new DamageCalculatedEvent(state.getGameId(), attacker.getTopCard().id(), defender.getTopCard().id(), damage.baseDamage(), damage.weaknessApplied(), damage.resistanceApplied(), damage.finalDamage()));
        events.add(new DamageAppliedEvent(state.getGameId(), defender.getTopCard().id(), damage.finalDamage(), damage.countersAdded(), damagedDefender.getDamageCounters()));
        events.add(new AttackResolvedEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId()));

        GameState attackState = new GameState(
                state.getGameId(),
                GameStatus.ACTIVE,
                playerOneOf(state, updatedDefenderPlayer),
                playerTwoOf(state, updatedDefenderPlayer),
                state.getTurnState().enterAttack(),
                state.getActiveStadium().orElse(null),
                events
        );
        return turnManager.endTurn(attackState, new EndTurnCommand(command.playerId()));
    }

    private void validateTurn(GameState state, PlayerId playerId) {
        if (state.getStatus() != GameStatus.ACTIVE) {
            throw new AttackException("Game must be ACTIVE");
        }
        TurnState turn = state.getTurnState();
        if (!playerId.equals(turn.currentPlayer())) {
            throw new AttackException("Only the current player can attack");
        }
        if (turn.phase() != TurnPhase.MAIN) {
            throw new AttackException("Attack can only be declared from MAIN phase");
        }
        if (playerId.equals(turn.startingPlayer()) && turn.turnNumber() == 1) {
            throw new AttackException("Starting player cannot attack on their first turn");
        }
    }

    private PlayerGameState withActivePokemon(PlayerGameState player, PokemonInPlay activePokemon) {
        BoardState board = player.getBoard();
        BoardState updatedBoard = new BoardState(new ActivePokemon(activePokemon), board.getBench(), board.getActiveStadium().orElse(null));
        return new PlayerGameState(player.getPlayerId(), player.getDeck(), player.getHand(), player.getPrizeCards(), player.getDiscardPile(), updatedBoard, player.getTurnsTaken());
    }

    private PlayerGameState getPlayerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new AttackException("Player is not part of this game");
    }

    private PlayerId opponentOf(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState().getPlayerId();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState().getPlayerId();
        }
        throw new AttackException("Player is not part of this game");
    }

    private PlayerGameState playerOneOf(GameState state, PlayerGameState updatedPlayer) {
        return state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerOneState();
    }

    private PlayerGameState playerTwoOf(GameState state, PlayerGameState updatedPlayer) {
        return state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerTwoState();
    }
}
