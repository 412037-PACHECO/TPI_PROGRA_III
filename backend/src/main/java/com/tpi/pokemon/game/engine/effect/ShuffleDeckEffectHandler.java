package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.DeckShuffledEvent;
import com.tpi.pokemon.game.engine.event.EffectResolvedEvent;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.util.Objects;

public final class ShuffleDeckEffectHandler implements EffectHandler {
    private final DeckShuffler deckShuffler;

    public ShuffleDeckEffectHandler() {
        this(new RandomDeckShuffler());
    }

    public ShuffleDeckEffectHandler(DeckShuffler deckShuffler) {
        this.deckShuffler = Objects.requireNonNull(deckShuffler, "deckShuffler must not be null");
    }

    @Override public EffectType type() { return EffectType.SHUFFLE_DECK; }

    @Override
    public EffectResult execute(EffectDefinition definition, EffectExecutionContext context, EffectExecutionService executionService) {
        PlayerId ownerId = EffectStateSupport.ownerFor(context, definition.target());
        PlayerGameState owner = EffectStateSupport.playerState(context.state(), ownerId);
        DeckZone shuffledDeck = new DeckZone(deckShuffler.shuffle(owner.getDeck().getCards()));
        PlayerGameState updatedOwner = EffectStateSupport.withDeckAndHand(owner, shuffledDeck, owner.getHand());
        context.events().add(new DeckShuffledEvent(context.state().getGameId(), ownerId, shuffledDeck.getCards().size()));
        context.events().add(new EffectResolvedEvent(context.state().getGameId(), context.actingPlayerId(), definition.type(), context.sourceId()));
        return new EffectResult(EffectStateSupport.withPlayer(context.state(), updatedOwner));
    }
}
