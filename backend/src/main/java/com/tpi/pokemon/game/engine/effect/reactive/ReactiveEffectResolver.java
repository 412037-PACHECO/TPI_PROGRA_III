package com.tpi.pokemon.game.engine.effect.reactive;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierDefinition;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
import com.tpi.pokemon.game.engine.event.DamageCountersPlacedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.ReactiveEffectTriggeredEvent;
import java.util.List;
import java.util.Objects;

public final class ReactiveEffectResolver {
    public GameState resolveDamageReceived(ReactiveEffectContext context) {
        Objects.requireNonNull(context, "context must not be null");
        DamageReceivedContext damageContext = context.damageReceived();
        if (context.trigger() != ReactiveEffectTrigger.DAMAGE_RECEIVED
                || damageContext.damage() <= 0
                || damageContext.source().type() != DamageSourceType.ATTACK
                || damageContext.source().sourcePlayerId().equals(damageContext.damagedPlayerId())) {
            return context.state();
        }

        GameState current = context.state();
        PlayerGameState damagedPlayer = playerState(current, damageContext.damagedPlayerId());
        PokemonInPlay currentDamaged = damagedPlayer.getBoard().getActivePokemon()
                .map(ActivePokemon::getPokemon)
                .orElse(null);
        if (currentDamaged == null || !currentDamaged.getTopCard().id().equals(damageContext.damagedPokemon().getTopCard().id())) {
            return current;
        }

        for (CardEffectDefinition effect : currentDamaged.getTopCard().definition().effects()) {
            if (effect.activationKind() != EffectActivationKind.REACTIVE || effect.timing() != EffectTiming.ON_DAMAGE_RECEIVED) {
                continue;
            }
            if (!effect.condition().matches(currentDamaged)) {
                continue;
            }
            for (ModifierDefinition modifier : effect.modifiers()) {
                if (modifier.type() != ModifierType.PLACE_DAMAGE_COUNTERS
                        || modifier.operation() != ModifierOperation.ADD
                        || modifier.targetRole() != ModifierTargetRole.ATTACKER) {
                    continue;
                }
                current = placeCountersOnAttacker(current, damageContext, effect, modifier, context.events());
            }
        }
        return current;
    }

    private GameState placeCountersOnAttacker(GameState state, DamageReceivedContext damageContext, CardEffectDefinition effect, ModifierDefinition modifier, List<GameEvent> events) {
        PlayerId attackerId = damageContext.source().sourcePlayerId();
        PlayerGameState attackerPlayer = playerState(state, attackerId);
        PokemonInPlay attacker = attackerPlayer.getBoard().getActivePokemon()
                .map(ActivePokemon::getPokemon)
                .orElse(null);
        if (attacker == null || !attacker.getTopCard().id().equals(damageContext.source().sourcePokemon().getTopCard().id())) {
            return state;
        }
        PokemonInPlay damagedAttacker = attacker.withDamageCounters(attacker.getDamageCounters() + modifier.amount());
        BoardState updatedBoard = new BoardState(new ActivePokemon(damagedAttacker), attackerPlayer.getBoard().getBench(), attackerPlayer.getBoard().getActiveStadium().orElse(null));
        PlayerGameState updatedAttacker = attackerPlayer.withBoard(updatedBoard);
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(attackerId) ? updatedAttacker : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(attackerId) ? updatedAttacker : state.getPlayerTwoState();
        events.add(new ReactiveEffectTriggeredEvent(state.getGameId(), damageContext.damagedPokemon().getTopCard().id(), effect.effectId()));
        events.add(new DamageCountersPlacedEvent(state.getGameId(), damageContext.damagedPlayerId(), damagedAttacker.getTopCard().id(), modifier.amount(), damagedAttacker.getDamageCounters(), effect.effectId()));
        return new GameState(state.getGameId(), GameStatus.ACTIVE, playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), events);
    }

    private PlayerGameState playerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new IllegalArgumentException("Player is not part of this game");
    }
}
