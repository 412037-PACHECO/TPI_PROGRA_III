package com.tpi.pokemon.game.application.view;

import com.tpi.pokemon.game.application.UnauthorizedGameViewException;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogEntity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameLogProjectionService {
    public List<GameLogPublicView> project(GameState state, PlayerId viewerPlayerId, List<GameActionLogEntity> logs) {
        requirePlayer(state, viewerPlayerId);
        return logs.stream().map(this::publicLog).toList();
    }

    private void requirePlayer(GameState state, PlayerId playerId) {
        if (!state.getPlayerOneState().getPlayerId().equals(playerId) && !state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            throw new UnauthorizedGameViewException(playerId.value(), state.getGameId().value());
        }
    }

    private GameLogPublicView publicLog(GameActionLogEntity log) {
        return new GameLogPublicView(
                log.getSequence(),
                log.getTurnNumber(),
                log.getActorPlayerId(),
                log.getActionType(),
                log.getCreatedAt(),
                summary(log),
                List.of(log.getActionType())
        );
    }

    private String summary(GameActionLogEntity log) {
        String actor = log.getActorPlayerId() == null ? "System" : log.getActorPlayerId();
        return actor + " resolved " + log.getActionType();
    }
}
