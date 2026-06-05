package com.tpi.pokemon.game.api;

import com.tpi.pokemon.game.application.GameApplicationService;
import com.tpi.pokemon.game.application.GameplayApplicationService;
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
    private final GameplayApplicationService gameplayService;

    public GameController(GameApplicationService applicationService, GameQueryService queryService, GameplayApplicationService gameplayService) {
        this.applicationService = applicationService;
        this.queryService = queryService;
        this.gameplayService = gameplayService;
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

    @PostMapping("/{gameId}/reconnect")
    public GameViewResponse reconnect(@PathVariable String gameId, @RequestParam String playerId) {
        return applicationService.reconnect(gameId, playerId);
    }

    @PostMapping("/{gameId}/start")
    public GameViewResponse startGame(@PathVariable String gameId, @RequestBody StartGameRequest request) {
        return gameplayService.startGame(gameId, request);
    }

    @PostMapping("/{gameId}/setup/choose-initial")
    public GameViewResponse chooseInitial(@PathVariable String gameId, @RequestBody ChooseInitialPokemonRequest request) {
        return gameplayService.chooseInitial(gameId, request);
    }

    @PostMapping("/{gameId}/setup/complete")
    public GameViewResponse completeSetup(@PathVariable String gameId, @RequestParam String playerId, @RequestParam(required = false) String startingPlayerId) {
        return gameplayService.completeSetup(gameId, playerId, startingPlayerId);
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

    @PostMapping("/{gameId}/actions/start-turn")
    public GameViewResponse startTurn(@PathVariable String gameId, @RequestBody StartTurnRequest request) {
        return gameplayService.startTurn(gameId, request);
    }

    @PostMapping("/{gameId}/actions/play-basic")
    public GameViewResponse playBasic(@PathVariable String gameId, @RequestBody PlayBasicPokemonRequest request) {
        return gameplayService.playBasic(gameId, request);
    }

    @PostMapping("/{gameId}/actions/attach-energy")
    public GameViewResponse attachEnergy(@PathVariable String gameId, @RequestBody AttachEnergyRequest request) {
        return gameplayService.attachEnergy(gameId, request);
    }

    @PostMapping("/{gameId}/actions/evolve")
    public GameViewResponse evolve(@PathVariable String gameId, @RequestBody EvolvePokemonRequest request) {
        return gameplayService.evolve(gameId, request);
    }

    @PostMapping("/{gameId}/actions/retreat")
    public GameViewResponse retreat(@PathVariable String gameId, @RequestBody RetreatRequest request) {
        return gameplayService.retreat(gameId, request);
    }

    @PostMapping("/{gameId}/actions/attack")
    public GameViewResponse attack(@PathVariable String gameId, @RequestBody DeclareAttackRequest request) {
        return gameplayService.attack(gameId, request);
    }

    @PostMapping("/{gameId}/actions/end-turn")
    public GameViewResponse endTurn(@PathVariable String gameId, @RequestBody EndTurnRequest request) {
        return gameplayService.endTurn(gameId, request);
    }

    @PostMapping("/{gameId}/actions/replace-active")
    public GameViewResponse replaceActive(@PathVariable String gameId, @RequestBody ReplaceActiveRequest request) {
        return gameplayService.replaceActive(gameId, request);
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
