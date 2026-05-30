package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.ConfusionCheckResolvedEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;

public final class CoinFlipEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.COIN_FLIP; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        CoinFlipResult result = context.coinFlipProvider().flip();
        context.events().add(new ConfusionCheckResolvedEvent(context.state().getGameId(), context.actingPlayerId(), null, context.sourceId(), result));
        EffectDefinition branch = result == CoinFlipResult.HEADS ? definition.headsEffect() : definition.tailsEffect();
        if (branch == null) {
            context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
            return new EffectResult(context.state());
        }
        EffectResult branchResult = executionService.execute(branch, context);
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return branchResult;
    }
}
