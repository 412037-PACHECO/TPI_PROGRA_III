package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardDrawEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import java.util.ArrayList;
import java.util.List;

public final class DrawCardsEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.DRAW_CARDS; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        int count = Math.min(definition.amount(), player.getDeck().getCards().size());
        List<CardInstance> drawn = new ArrayList<>(player.getDeck().getCards().subList(0, count));
        List<CardInstance> remainingDeck = new ArrayList<>(player.getDeck().getCards().subList(count, player.getDeck().getCards().size()));
        List<CardInstance> updatedHand = new ArrayList<>(player.getHand().getCards());
        updatedHand.addAll(drawn);
        PlayerGameState updatedPlayer = EffectStateSupport.withDeckAndHand(player, new DeckZone(remainingDeck), new HandZone(updatedHand));
        context.events().add(new CardDrawEffectResolvedEvent(context.state().getGameId(), playerId, definition.amount(), drawn.stream().map(CardInstance::id).toList()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedPlayer));
    }
}
