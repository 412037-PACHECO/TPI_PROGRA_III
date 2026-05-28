package com.tpi.pokemon.decks.application;

public class CatalogCardNotFoundException extends RuntimeException {
    public CatalogCardNotFoundException(String cardId) {
        super("La carta " + cardId + " no existe en el catálogo local");
    }
}
