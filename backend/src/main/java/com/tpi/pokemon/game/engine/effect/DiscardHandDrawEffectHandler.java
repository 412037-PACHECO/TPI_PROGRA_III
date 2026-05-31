package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardDrawEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.CardsDiscardedEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import java.util.ArrayList;
import java.util.List;

public final class DiscardHandDrawEffectHandler implements EffectHandler {
    @Override public EffectType type() { return EffectType.DISCARD_HAND_DRAW; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        List<CardInstance> discarded = new ArrayList<>(player.getHand().getCards());
        int drawCount = Math.min(definition.amount(), player.getDeck().getCards().size());
        List<CardInstance> drawn = new ArrayList<>(player.getDeck().getCards().subList(0, drawCount));
        List<CardInstance> deck = new ArrayList<>(player.getDeck().getCards().subList(drawCount, player.getDeck().getCards().size()));
        PlayerGameState updated = EffectStateSupport.withDeckHandAndDiscard(player, new DeckZone(deck), new HandZone(drawn), player.getDiscardPile().withCardsAdded(discarded));
        context.events().add(new CardsDiscardedEvent(context.state().getGameId(), playerId, discarded.stream().map(CardInstance::id).toList(), "EFFECT:" + context.sourceId()));
        context.events().add(new CardDrawEffectResolvedEvent(context.state().getGameId(), playerId, definition.amount(), drawn.stream().map(CardInstance::id).toList()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updated));
    }
}
