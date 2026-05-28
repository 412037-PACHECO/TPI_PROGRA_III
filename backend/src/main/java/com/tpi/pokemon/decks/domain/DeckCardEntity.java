package com.tpi.pokemon.decks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "deck_cards", uniqueConstraints = @UniqueConstraint(name = "uk_deck_cards_deck_card", columnNames = {"deck_id", "card_id"}))
public class DeckCardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @Column(name = "card_id", nullable = false, length = 80)
    private String cardId;

    @Column(nullable = false)
    private int quantity;

    public Long getId() { return id; }
    public DeckEntity getDeck() { return deck; }
    public void setDeck(DeckEntity deck) { this.deck = deck; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
