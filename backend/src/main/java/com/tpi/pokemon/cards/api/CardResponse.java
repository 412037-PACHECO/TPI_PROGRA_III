package com.tpi.pokemon.cards.api;

public record CardResponse(
        String cardId,
        String name,
        String supertype,
        String subtypes,
        String setId,
        String setName,
        String number,
        String rarity,
        String hp,
        String types,
        String evolvesFrom,
        String rules,
        String attacks,
        String abilities,
        String weaknesses,
        String resistances,
        String retreatCost,
        Integer convertedRetreatCost,
        String imageSmall,
        String imageLarge) {
}
