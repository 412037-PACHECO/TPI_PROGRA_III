package com.tpi.pokemon.game.application;

import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;

public record GameSnapshotCommand(
        String reason,
        Long actionLogId,
        PendingEffectSelection pendingEffectSelection
) {
    public static GameSnapshotCommand automatic(String reason) {
        return new GameSnapshotCommand(reason, null, null);
    }

    public GameSnapshotCommand {
        if (reason == null || reason.isBlank()) {
            reason = "STATE_PERSISTED";
        }
    }
}
