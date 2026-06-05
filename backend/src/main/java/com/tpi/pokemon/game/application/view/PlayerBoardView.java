package com.tpi.pokemon.game.application.view;

import java.util.List;

public record PlayerBoardView(PokemonInPlayView activePokemon, List<PokemonInPlayView> bench) {
}
