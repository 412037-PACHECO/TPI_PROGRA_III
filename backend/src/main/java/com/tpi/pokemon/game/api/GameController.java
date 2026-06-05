package com.tpi.pokemon.game.api;

import com.tpi.pokemon.game.application.GameApplicationService;
import com.tpi.pokemon.game.application.GameQueryService;
import com.tpi.pokemon.game.application.GameSessionSummary;
import com.tpi.pokemon.game.application.view.GameLogPublicView;
import com.tpi.pokemon.game.application.view.GameViewResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/{gameId}/view")
    public GameViewResponse view(@PathVariable String gameId, @RequestParam String viewerPlayerId) {
        return queryService.view(gameId, viewerPlayerId);
    }

    @GetMapping("/waiting")
    public List<GameResponse> waiting() {
        return queryService.waitingGames().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{gameId}/log")
    public List<GameLogPublicView> log(@PathVariable String gameId, @RequestParam String viewerPlayerId) {
        return queryService.publicHistory(gameId, viewerPlayerId);
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

}
