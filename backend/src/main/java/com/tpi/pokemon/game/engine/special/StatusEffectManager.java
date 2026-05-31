package com.tpi.pokemon.game.engine.special;

import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolutionResult;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierResolver;
import com.tpi.pokemon.game.engine.effect.modifier.SpecialConditionModifierContext;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionRemovedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StatusEffectManager {
    public PokemonInPlay applyCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.applySpecialCondition(condition);
    }

    public SpecialConditionApplication applyCondition(SpecialConditionModifierContext context, ModifierResolver modifierResolver) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(modifierResolver, "modifierResolver must not be null");
        ModifierResolutionResult prevention = modifierResolver.resolveSpecialConditionPrevention(context);
        if (prevention.prevented()) {
            return new SpecialConditionApplication(context.target(), true, prevention.appliedModifiers());
        }
        return new SpecialConditionApplication(applyCondition(context.target(), context.condition()), false, prevention.appliedModifiers());
    }

    public PokemonInPlay removeCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.removeSpecialCondition(condition);
    }

    public PokemonInPlay clearSpecialConditions(PokemonInPlay pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return pokemon.clearSpecialConditions();
    }

    public boolean hasCondition(PokemonInPlay pokemon, SpecialCondition condition) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(condition, "condition must not be null");
        return pokemon.hasSpecialCondition(condition);
    }

    public boolean canAttack(PokemonInPlay pokemon) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        return !pokemon.hasSpecialCondition(SpecialCondition.ASLEEP) && !pokemon.hasSpecialCondition(SpecialCondition.PARALYZED);
    }

    public boolean canRetreat(PokemonInPlay pokemon) {
        return canAttack(pokemon);
    }

    public GameState reconcilePreventedConditions(GameState state, ModifierResolver modifierResolver, List<GameEvent> events) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(modifierResolver, "modifierResolver must not be null");
        Objects.requireNonNull(events, "events must not be null");
        GameState updated = reconcilePlayer(state, state.getPlayerOneState().getPlayerId(), modifierResolver, events);
        return reconcilePlayer(updated, updated.getPlayerTwoState().getPlayerId(), modifierResolver, events);
    }

    private GameState reconcilePlayer(GameState state, PlayerId playerId, ModifierResolver modifierResolver, List<GameEvent> events) {
        PlayerGameState player = playerState(state, playerId);
        BoardState board = player.getBoard();
        boolean changed = false;
        ActivePokemon active = board.getActivePokemon().orElse(null);
        if (active != null) {
            PokemonInPlay reconciled = reconcilePokemon(state, playerId, active.getPokemon(), modifierResolver, events);
            if (!reconciled.equals(active.getPokemon())) {
                active = new ActivePokemon(reconciled);
                changed = true;
            }
        }
        List<PokemonInPlay> bench = new ArrayList<>(board.getBench().getPokemon());
        for (int i = 0; i < bench.size(); i++) {
            PokemonInPlay reconciled = reconcilePokemon(state, playerId, bench.get(i), modifierResolver, events);
            if (!reconciled.equals(bench.get(i))) {
                bench.set(i, reconciled);
                changed = true;
            }
        }
        if (!changed) {
            return state;
        }
        PlayerGameState updatedPlayer = player.withBoard(new BoardState(active, new Bench(bench)));
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(playerId) ? updatedPlayer : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(playerId) ? updatedPlayer : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getFinishResult().orElse(null), state.getPendingActiveReplacement().orElse(null), events);
    }

    private PokemonInPlay reconcilePokemon(GameState state, PlayerId ownerId, PokemonInPlay pokemon, ModifierResolver modifierResolver, List<GameEvent> events) {
        PokemonInPlay updated = pokemon;
        for (SpecialCondition condition : SpecialCondition.values()) {
            if (!updated.hasSpecialCondition(condition)) {
                continue;
            }
            ModifierResolutionResult prevention = modifierResolver.resolveSpecialConditionPrevention(new SpecialConditionModifierContext(state, ownerId, ownerId, updated, condition));
            if (prevention.prevented()) {
                updated = updated.removeSpecialCondition(condition);
                events.add(new SpecialConditionRemovedEvent(state.getGameId(), ownerId, pokemon.getTopCard().id(), condition, "PREVENTED_BY_CONTINUOUS_EFFECT"));
            }
        }
        return updated;
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
