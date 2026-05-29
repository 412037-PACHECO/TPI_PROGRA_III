package com.tpi.pokemon.game.engine.random;

import java.util.Random;

public final class RandomCoinFlipProvider implements CoinFlipProvider {
    private final Random random = new Random();

    @Override
    public CoinFlipResult flip() {
        return random.nextBoolean() ? CoinFlipResult.HEADS : CoinFlipResult.TAILS;
    }
}
