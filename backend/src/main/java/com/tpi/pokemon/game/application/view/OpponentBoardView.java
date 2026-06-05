package com.tpi.pokemon.game.application.view;

import java.util.List;

public record OpponentBoardView(PokemonInPlayView activePokemon, List<PokemonInPlayView> bench) {
}
