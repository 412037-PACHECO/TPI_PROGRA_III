package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.CardDrawEffectResolvedEvent;
import com.tpi.pokemon.game.engine.event.CardsShuffledIntoDeckEvent;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ShuffleHandIntoDeckDrawEffectHandler implements EffectHandler {
    private final DeckShuffler deckShuffler;

    public ShuffleHandIntoDeckDrawEffectHandler() { this(new RandomDeckShuffler()); }
    public ShuffleHandIntoDeckDrawEffectHandler(DeckShuffler deckShuffler) { this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null"); }

    @Override public EffectType type() { return EffectType.SHUFFLE_HAND_INTO_DECK_DRAW; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId playerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState player = EffectStateSupport.playerState(context.state(), playerId);
        List<CardInstance> shuffledInput = new ArrayList<>(player.getDeck().getCards());
        shuffledInput.addAll(player.getHand().getCards());
        List<CardInstance> shuffled = deckShuffler.shuffle(shuffledInput);
        int drawCount = Math.min(definition.amount(), shuffled.size());
        List<CardInstance> drawn = new ArrayList<>(shuffled.subList(0, drawCount));
        List<CardInstance> deck = new ArrayList<>(shuffled.subList(drawCount, shuffled.size()));
        PlayerGameState updated = EffectStateSupport.withDeckAndHand(player, new DeckZone(deck), new HandZone(drawn));
        context.events().add(new CardsShuffledIntoDeckEvent(context.state().getGameId(), playerId, player.getHand().getCards().stream().map(CardInstance::id).toList(), shuffled.size()));
        context.events().add(new DeckShuffledEvent(context.state().getGameId(), playerId, shuffled.size()));
        context.events().add(new CardDrawEffectResolvedEvent(context.state().getGameId(), playerId, definition.amount(), drawn.stream().map(CardInstance::id).toList()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updated));
    }
}
