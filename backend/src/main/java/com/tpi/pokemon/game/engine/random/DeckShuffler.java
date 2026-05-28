package com.tpi.pokemon.game.engine.random;

import com.tpi.pokemon.game.domain.model.CardInstance;
import java.util.List;

public interface DeckShuffler {
    List<CardInstance> shuffle(List<CardInstance> deck);
}
