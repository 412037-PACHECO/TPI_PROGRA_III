package com.tpi.pokemon.game.api;

import com.tpi.pokemon.game.application.GameActionLogSummary;
import com.tpi.pokemon.game.application.GameApplicationService;
import com.tpi.pokemon.game.application.GameQueryService;
import com.tpi.pokemon.game.application.GameSessionSummary;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameApplicationService applicationService;
    private final GameQueryService queryService;

    public GameController(GameApplicationService applicationService, GameQueryService queryService) {
        this.applicationService = applicationService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse create(@RequestBody CreateGameRequest request) {
        return toResponse(applicationService.createWaitingGame(request == null ? null : request.playerOneId()));
    }

    @PostMapping("/{gameId}/join")
    public GameResponse join(@PathVariable String gameId, @RequestBody JoinGameRequest request) {
        return toResponse(applicationService.joinGame(gameId, request == null ? null : request.playerTwoId()));
    }

    @GetMapping("/{gameId}")
    public GameResponse get(@PathVariable String gameId) {
        return toResponse(queryService.get(gameId));
    }

    @GetMapping("/waiting")
    public List<GameResponse> waiting() {
        return queryService.waitingGames().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{gameId}/log")
    public List<GameActionLogResponse> log(@PathVariable String gameId) {
        return queryService.history(gameId).stream().map(this::toResponse).toList();
    }

    private GameResponse toResponse(GameSessionSummary summary) {
        return new GameResponse(
                summary.gameId(),
                summary.playerOneId(),
                summary.playerTwoId(),
                summary.status(),
                summary.currentPlayerId(),
                summary.turnNumber(),
                summary.phase(),
                summary.winnerId(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }

    private GameActionLogResponse toResponse(GameActionLogSummary summary) {
        return new GameActionLogResponse(
                summary.sequence(),
                summary.turnNumber(),
                summary.phase(),
                summary.actorPlayerId(),
                summary.actionType(),
                summary.commandJson(),
                summary.resultJson(),
                summary.eventsJson(),
                summary.createdAt()
        );
    }
}
