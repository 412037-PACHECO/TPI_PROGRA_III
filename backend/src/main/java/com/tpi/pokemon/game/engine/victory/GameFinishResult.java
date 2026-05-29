package com.tpi.pokemon.game.engine.victory;

import com.tpi.pokemon.game.domain.value.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record GameFinishResult(
        GameFinishType type,
        PlayerId winnerId,
        PlayerId loserId,
        List<FinishReason> reasons
) {
    public GameFinishResult {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(reasons, "reasons must not be null");
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons must not be empty");
        }
        if (reasons.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("reasons must not contain null values");
        }
        reasons = List.copyOf(reasons);
        if (type == GameFinishType.SINGLE_WINNER && (winnerId == null || loserId == null)) {
            throw new IllegalArgumentException("single winner result requires winner and loser");
        }
    }

    public static GameFinishResult singleWinner(PlayerId winnerId, PlayerId loserId, FinishReason reason) {
        return new GameFinishResult(GameFinishType.SINGLE_WINNER, winnerId, loserId, List.of(reason));
    }

    public static GameFinishResult singleWinner(PlayerId winnerId, PlayerId loserId, List<FinishReason> reasons) {
        return new GameFinishResult(GameFinishType.SINGLE_WINNER, winnerId, loserId, reasons);
    }

    public static GameFinishResult suddenDeathRequired(List<FinishReason> reasons) {
        return new GameFinishResult(GameFinishType.SUDDEN_DEATH_REQUIRED, null, null, reasons);
    }

    public Optional<PlayerId> winner() {
        return Optional.ofNullable(winnerId);
    }

    public Optional<PlayerId> loser() {
        return Optional.ofNullable(loserId);
    }
}
