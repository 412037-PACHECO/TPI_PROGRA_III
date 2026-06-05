package com.tpi.pokemon.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:game-application-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GameApplicationServiceTest {
    @Autowired
    private GameApplicationService applicationService;

    @Autowired
    private GameQueryService queryService;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private GameSnapshotRepository snapshotRepository;

    @Autowired
    private GameActionLogRepository actionLogRepository;

    @Test
    void createsWaitingGameWithoutExposingIncompleteGameStateSnapshot() {
        GameSessionSummary created = applicationService.createWaitingGame(" player-one ");

        assertThat(created.gameId()).isNotBlank();
        assertThat(created.playerOneId()).isEqualTo("player-one");
        assertThat(created.playerTwoId()).isNull();
        assertThat(created.status()).isEqualTo(GameSessionStatus.WAITING);
        assertThat(queryService.waitingGames()).extracting(GameSessionSummary::gameId).contains(created.gameId());
        assertThat(snapshotRepository.findTopByGameIdOrderBySequenceDesc(created.gameId())).isEmpty();
    }

    @Test
    void joinsWaitingGameAndPersistsInitialSnapshotAndActionLog() {
        GameSessionSummary created = applicationService.createWaitingGame("player-one");

        GameSessionSummary joined = applicationService.joinGame(created.gameId(), "player-two");

        assertThat(joined.playerOneId()).isEqualTo("player-one");
        assertThat(joined.playerTwoId()).isEqualTo("player-two");
        assertThat(joined.status()).isEqualTo("CREATED");
        assertThat(sessionRepository.findByGameId(created.gameId())).hasValueSatisfying(session -> {
            assertThat(session.getStatus()).isEqualTo("CREATED");
            assertThat(session.getPhase()).isEqualTo("NOT_STARTED");
        });
        assertThat(snapshotRepository.findTopByGameIdOrderBySequenceDesc(created.gameId())).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.getSequence()).isEqualTo(1);
            assertThat(snapshot.getReason()).isEqualTo("GAME_CREATED");
            assertThat(snapshot.getStatus()).isEqualTo("CREATED");
        });
        assertThat(actionLogRepository.findByGameIdOrderBySequenceAsc(created.gameId()))
                .extracting(log -> log.getActionType())
                .containsExactly("GAME_JOINED");
    }

    @Test
    void preventsSamePlayerOrThirdPlayerJoining() {
        GameSessionSummary created = applicationService.createWaitingGame("player-one");

        assertThatThrownBy(() -> applicationService.joinGame(created.gameId(), "player-one"))
                .isInstanceOf(InvalidGameCommandException.class)
                .hasMessage("A player cannot join their own game");

        applicationService.joinGame(created.gameId(), "player-two");

        assertThatThrownBy(() -> applicationService.joinGame(created.gameId(), "player-three"))
                .isInstanceOf(GameAlreadyFullException.class)
                .hasMessage("Game " + created.gameId() + " is already full");
    }

    @Test
    void returnsHistoryOnlyForExistingGames() {
        GameSessionSummary created = applicationService.createWaitingGame("player-one");
        applicationService.joinGame(created.gameId(), "player-two");

        assertThat(queryService.history(created.gameId()))
                .extracting(GameActionLogSummary::actionType)
                .containsExactly("GAME_JOINED");
        assertThatThrownBy(() -> queryService.history("missing-game"))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void waitingListExcludesJoinedGames() {
        GameSessionSummary waiting = applicationService.createWaitingGame("waiting-player");
        GameSessionSummary joined = applicationService.createWaitingGame("joined-player-one");

        applicationService.joinGame(joined.gameId(), "joined-player-two");

        assertThat(queryService.waitingGames())
                .extracting(GameSessionSummary::gameId)
                .contains(waiting.gameId())
                .doesNotContain(joined.gameId());
    }
}
