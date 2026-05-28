package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class PlayerGameState {
    private final PlayerId playerId;
    private final DeckZone deck;
    private final HandZone hand;
    private final PrizeCards prizeCards;
    private final DiscardPile discardPile;
    private final BoardState board;

    public PlayerGameState(PlayerId playerId, DeckZone deck, HandZone hand, PrizeCards prizeCards, DiscardPile discardPile, BoardState board) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.deck = Objects.requireNonNull(deck, "deck must not be null");
        this.hand = Objects.requireNonNull(hand, "hand must not be null");
        this.prizeCards = Objects.requireNonNull(prizeCards, "prizeCards must not be null");
        this.discardPile = Objects.requireNonNull(discardPile, "discardPile must not be null");
        this.board = Objects.requireNonNull(board, "board must not be null");
        validateOwnershipAndUniqueness();
    }

    public static PlayerGameState empty(PlayerId playerId) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), PrizeCards.empty(), DiscardPile.empty(), BoardState.empty());
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public DeckZone getDeck() {
        return deck;
    }

    public HandZone getHand() {
        return hand;
    }

    public PrizeCards getPrizeCards() {
        return prizeCards;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public BoardState getBoard() {
        return board;
    }

    private void validateOwnershipAndUniqueness() {
        Set<CardInstanceId> seen = new HashSet<>();
        deck.getCards().forEach(card -> validateCard(card, seen, "deck"));
        hand.getCards().forEach(card -> validateCard(card, seen, "hand"));
        prizeCards.getCards().forEach(card -> validateCard(card, seen, "prize cards"));
        discardPile.getCards().forEach(card -> validateCard(card, seen, "discard pile"));
        board.getActivePokemon().ifPresent(active -> validatePokemon(active.getPokemon(), seen, "active Pokemon"));
        board.getBench().getPokemon().forEach(pokemon -> validatePokemon(pokemon, seen, "bench"));
    }

    private void validatePokemon(PokemonInPlay pokemon, Set<CardInstanceId> seen, String zoneName) {
        validateCard(pokemon.getBaseCard(), seen, zoneName);
        pokemon.getAttachedCards().getCards().forEach(card -> validateCard(card, seen, zoneName + " attached cards"));
    }

    private void validateCard(CardInstance card, Set<CardInstanceId> seen, String zoneName) {
        if (!playerId.equals(card.owner())) {
            throw new IllegalArgumentException("Card " + card.id().value() + " in " + zoneName + " is not owned by player " + playerId.value());
        }
        if (!seen.add(card.id())) {
            throw new IllegalArgumentException("PlayerGameState must not contain duplicate card instance: " + card.id().value());
        }
    }
}
