package com.tpi.pokemon.game.engine.effect.mapping;

import java.util.Locale;

public record CardAttackKey(String cardId, String attackKey) {
    public CardAttackKey {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        if (attackKey == null || attackKey.isBlank()) {
            throw new IllegalArgumentException("attackKey must not be blank");
        }
        cardId = normalize(cardId);
        attackKey = normalize(attackKey);
    }

    public static CardAttackKey of(String cardId, String attackNameOrId) {
        return new CardAttackKey(cardId, attackNameOrId);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
