package com.tpi.pokemon.game.engine.attack;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
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
import com.tpi.pokemon.game.engine.event.ConfusionCheckResolvedEvent;
import com.tpi.pokemon.game.engine.event.DamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.DamageCalculatedEvent;
import com.tpi.pokemon.game.engine.event.EnergyCostValidatedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.effect.EffectDefinition;
import com.tpi.pokemon.game.engine.effect.EffectExecutionContext;
import com.tpi.pokemon.game.engine.effect.EffectExecutionService;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.knockout.PostAttackResolutionService;
import com.tpi.pokemon.game.engine.random.CoinFlipProvider;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import com.tpi.pokemon.game.engine.random.RandomCoinFlipProvider;
import com.tpi.pokemon.game.engine.special.StatusEffectManager;
import com.tpi.pokemon.game.engine.event.SpecialConditionDamageAppliedEvent;
import com.tpi.pokemon.game.engine.turn.EndTurnCommand;
import com.tpi.pokemon.game.engine.turn.TurnManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AttackService {
    private final TurnManager turnManager;
    private final EnergyCostValidator energyCostValidator;
    private final DamageCalculator damageCalculator;
    private final PostAttackResolutionService postAttackResolutionService;
    private final StatusEffectManager statusEffectManager;
    private final CoinFlipProvider coinFlipProvider;
    private final EffectExecutionService effectExecutionService;

    public AttackService() {
        this(new TurnManager());
    }

    public AttackService(TurnManager turnManager) {
        this(turnManager, new EnergyCostValidator(), new DamageCalculator(), new PostAttackResolutionService(), new StatusEffectManager(), new RandomCoinFlipProvider(), new EffectExecutionService());
    }

    public AttackService(TurnManager turnManager, EnergyCostValidator energyCostValidator, DamageCalculator damageCalculator, PostAttackResolutionService postAttackResolutionService) {
        this(turnManager, energyCostValidator, damageCalculator, postAttackResolutionService, new StatusEffectManager(), new RandomCoinFlipProvider(), new EffectExecutionService());
    }

    public AttackService(TurnManager turnManager, EnergyCostValidator energyCostValidator, DamageCalculator damageCalculator, PostAttackResolutionService postAttackResolutionService, StatusEffectManager statusEffectManager, CoinFlipProvider coinFlipProvider) {
        this(turnManager, energyCostValidator, damageCalculator, postAttackResolutionService, statusEffectManager, coinFlipProvider, new EffectExecutionService());
    }

    public AttackService(TurnManager turnManager, EnergyCostValidator energyCostValidator, DamageCalculator damageCalculator, PostAttackResolutionService postAttackResolutionService, StatusEffectManager statusEffectManager, CoinFlipProvider coinFlipProvider, EffectExecutionService effectExecutionService) {
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
        this.energyCostValidator = Objects.requireNonNull(energyCostValidator, "energyCostValidator must not be null");
        this.damageCalculator = Objects.requireNonNull(damageCalculator, "damageCalculator must not be null");
        this.postAttackResolutionService = Objects.requireNonNull(postAttackResolutionService, "postAttackResolutionService must not be null");
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager, "statusEffectManager must not be null");
        this.coinFlipProvider = Objects.requireNonNull(coinFlipProvider, "coinFlipProvider must not be null");
        this.effectExecutionService = Objects.requireNonNull(effectExecutionService, "effectExecutionService must not be null");
    }

    public GameState declareAttack(GameState state, DeclareAttackCommand command) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (!state.getGameId().equals(command.gameId())) {
            throw new AttackException("Command gameId does not match state gameId");
        }

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

        if (statusEffectManager.hasCondition(attacker, SpecialCondition.ASLEEP)) {
            throw new AttackException("Asleep Pokemon cannot attack");
        }
        if (statusEffectManager.hasCondition(attacker, SpecialCondition.PARALYZED)) {
            throw new AttackException("Paralyzed Pokemon cannot attack");
        }

        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(new AttackDeclaredEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId(), attack.name()));

        if (statusEffectManager.hasCondition(attacker, SpecialCondition.CONFUSED)) {
            CoinFlipResult confusionResult = coinFlipProvider.flip();
            events.add(new ConfusionCheckResolvedEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId(), confusionResult));
            if (confusionResult == CoinFlipResult.TAILS) {
                return resolveFailedConfusedAttack(state, command.playerId(), defenderPlayer.getPlayerId(), attacker, attack, events);
            }
        }

        if (!energyCostValidator.hasEnoughEnergy(attacker, attack)) {
            throw new AttackException("Not enough energy to declare attack");
        }
        DamageCalculation damage = damageCalculator.calculate(attacker, defender, attack);

        PokemonInPlay damagedDefender = defender.applyDamage(damage.finalDamage());
        PlayerGameState updatedDefenderPlayer = withActivePokemon(defenderPlayer, damagedDefender);

        events.add(new EnergyCostValidatedEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId()));
        events.add(new DamageCalculatedEvent(state.getGameId(), attacker.getTopCard().id(), defender.getTopCard().id(), damage.baseDamage(), damage.weaknessApplied(), damage.resistanceApplied(), damage.finalDamage()));
        events.add(new DamageAppliedEvent(state.getGameId(), defender.getTopCard().id(), damage.finalDamage(), damage.countersAdded(), damagedDefender.getDamageCounters()));
        GameState attackState = new GameState(
                state.getGameId(),
                GameStatus.ACTIVE,
                playerOneOf(state, updatedDefenderPlayer),
                playerTwoOf(state, updatedDefenderPlayer),
                state.getTurnState().enterAttack(),
                state.getActiveStadium().orElse(null),
                events
        );
        GameState afterEffects = executeAttackEffects(attackState, command.playerId(), defenderPlayer.getPlayerId(), attack, events);
        events.add(new AttackResolvedEvent(state.getGameId(), command.playerId(), attacker.getTopCard().id(), attack.attackId()));
        GameState resolvedAttackState = new GameState(
                afterEffects.getGameId(),
                afterEffects.getStatus(),
                afterEffects.getPlayerOneState(),
                afterEffects.getPlayerTwoState(),
                afterEffects.getTurnState(),
                afterEffects.getActiveStadium().orElse(null),
                afterEffects.getFinishResult().orElse(null),
                afterEffects.getPendingActiveReplacement().orElse(null),
                events
        );
        return postAttackResolutionService.resolveAfterAttack(resolvedAttackState, command.playerId(), defenderPlayer.getPlayerId(), events);
    }

    private GameState executeAttackEffects(GameState state, PlayerId attackerId, PlayerId defenderId, AttackDefinition attack, List<GameEvent> events) {
        List<EffectDefinition> effects = attack.effects().stream()
                .filter(effect -> effect.timing() == EffectTiming.AFTER_DAMAGE || effect.timing() == EffectTiming.AFTER_ATTACK)
                .toList();
        if (effects.isEmpty()) {
            return state;
        }
        EffectExecutionContext context = new EffectExecutionContext(state, attackerId, defenderId, attack.attackId(), events, coinFlipProvider);
        return effectExecutionService.executeAll(effects, context).state();
    }

    private GameState resolveFailedConfusedAttack(GameState state, PlayerId attackerId, PlayerId defenderId, PokemonInPlay attacker, AttackDefinition attack, List<GameEvent> events) {
        PokemonInPlay damagedAttacker = attacker.applyDamage(30);
        PlayerGameState updatedAttackerPlayer = withActivePokemon(getPlayerState(state, attackerId), damagedAttacker);
        events.add(new SpecialConditionDamageAppliedEvent(state.getGameId(), attackerId, attacker.getTopCard().id(), SpecialCondition.CONFUSED, 30, damagedAttacker.getDamageCounters()));
        events.add(new DamageAppliedEvent(state.getGameId(), attacker.getTopCard().id(), 30, 3, damagedAttacker.getDamageCounters()));
        events.add(new AttackResolvedEvent(state.getGameId(), attackerId, attacker.getTopCard().id(), attack.attackId()));
        GameState confusedState = new GameState(
                state.getGameId(),
                GameStatus.ACTIVE,
                playerOneOf(state, updatedAttackerPlayer),
                playerTwoOf(state, updatedAttackerPlayer),
                state.getTurnState().enterAttack(),
                state.getActiveStadium().orElse(null),
                events
        );
        GameState afterKnockout = postAttackResolutionService.resolveActiveKnockout(confusedState, attackerId, defenderId, events);
        if (afterKnockout.getStatus() != GameStatus.ACTIVE || afterKnockout.getPendingActiveReplacement().isPresent()) {
            return afterKnockout;
        }
        return turnManager.endTurn(afterKnockout, new EndTurnCommand(attackerId));
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
