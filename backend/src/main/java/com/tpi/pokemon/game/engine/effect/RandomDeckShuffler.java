package com.tpi.pokemon.game.engine.effect;

import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.engine.random.DeckShuffler;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RandomDeckShuffler implements DeckShuffler {
    private final Random random;

    public RandomDeckShuffler() {
        this(new SecureRandom());
    }

    public RandomDeckShuffler(Random random) {
        this.random = random;
    }

    @Override
    public List<CardInstance> shuffle(List<CardInstance> deck) {
        List<CardInstance> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }
}
