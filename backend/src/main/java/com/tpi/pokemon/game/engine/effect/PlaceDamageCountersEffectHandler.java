package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DamageCountersPlacedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.knockout.KnockoutResolver;
import com.tpi.pokemon.game.engine.knockout.PostAttackResolutionService;
import java.util.Objects;

public final class PlaceDamageCountersEffectHandler implements EffectHandler {
    private final PostAttackResolutionService postAttackResolutionService;
    private final KnockoutResolver knockoutResolver;

    public PlaceDamageCountersEffectHandler() {
        this(new PostAttackResolutionService(), new KnockoutResolver());
    }

    public PlaceDamageCountersEffectHandler(PostAttackResolutionService postAttackResolutionService, KnockoutResolver knockoutResolver) {
        this.postAttackResolutionService = Objects.requireNonNull(postAttackResolutionService, "postAttackResolutionService must not be null");
        this.knockoutResolver = Objects.requireNonNull(knockoutResolver, "knockoutResolver must not be null");
    }

    @Override public EffectType type() { return EffectType.PLACE_DAMAGE_COUNTERS; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        PokemonInPlay target = EffectStateSupport.pokemonByBenchIndexOrActive(owner, definition.targetBenchIndex());
        PokemonInPlay updatedTarget = target.withDamageCounters(target.getDamageCounters() + definition.amount());
        BoardState board = EffectStateSupport.withPokemonAtBenchIndexOrActive(owner.getBoard(), updatedTarget, definition.targetBenchIndex());
        PlayerGameState updatedOwner = owner.withBoard(board);
        context.events().add(new DamageCountersPlacedEvent(context.state().getGameId(), context.actingPlayerId(), target.getTopCard().id(), definition.amount(), updatedTarget.getDamageCounters(), context.sourceId()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        var updatedState = EffectStateSupport.withPlayer(context.state(), updatedOwner);
        if (definition.targetBenchIndex() < 0 && knockoutResolver.isKnockedOut(updatedTarget)) {
            PlayerId prizeTaker = ownerId.equals(context.actingPlayerId()) ? context.defendingPlayerId() : context.actingPlayerId();
            updatedState = postAttackResolutionService.resolveActiveKnockout(updatedState, ownerId, prizeTaker, context.events());
        }
        return new EffectResult(updatedState);
    }
}
