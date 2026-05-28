package com.tpi.pokemon.game.domain.model;

import java.util.Objects;

public final class PokemonInPlay {
    private final CardInstance baseCard;
    private final AttachedCards attachedCards;

    public PokemonInPlay(CardInstance baseCard, AttachedCards attachedCards) {
        this.baseCard = Objects.requireNonNull(baseCard, "baseCard must not be null");
        this.attachedCards = Objects.requireNonNull(attachedCards, "attachedCards must not be null");
        if (attachedCards.getCards().stream().anyMatch(card -> card.id().equals(baseCard.id()))) {
            throw new IllegalArgumentException("PokemonInPlay must not attach its own base card");
        }
    }

    public static PokemonInPlay withoutAttachments(CardInstance baseCard) {
        return new PokemonInPlay(baseCard, AttachedCards.empty());
    }

    public CardInstance getBaseCard() {
        return baseCard;
    }

    public AttachedCards getAttachedCards() {
        return attachedCards;
    }
}
