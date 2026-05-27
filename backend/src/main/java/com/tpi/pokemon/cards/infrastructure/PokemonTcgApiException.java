package com.tpi.pokemon.cards.infrastructure;

public class PokemonTcgApiException extends RuntimeException {
    public PokemonTcgApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
