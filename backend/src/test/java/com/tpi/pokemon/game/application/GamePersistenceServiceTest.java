package com.tpi.pokemon.game.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DeckZone;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.SpecialConditionSet;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.CardFilterSpec;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectType;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import com.tpi.pokemon.game.engine.knockout.ActiveReplacementReason;
import com.tpi.pokemon.game.engine.knockout.PendingActiveReplacement;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameActionLogRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSessionRepository;
import com.tpi.pokemon.game.persistence.infrastructure.GameSnapshotEntity;
import com.tpi.pokemon.game.persistence.infrastructure.GameSnapshotRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:game-persistence-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GamePersistenceServiceTest {
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    @Autowired
    private GamePersistenceService service;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private GameSnapshotRepository snapshotRepository;

    @Autowired
    private GameActionLogRepository actionLogRepository;

    @Test
    void savesSessionSnapshotAndReconstructsCompleteGameState() {
        GameId gameId = new GameId("persist-game-1");
        GameState state = complexState(gameId);

        GameSnapshotEntity snapshot = service.saveSnapshot(state, GameSnapshotCommand.automatic("AFTER_ATTACK"));
        GameState restored = service.requireLatestGameState(gameId);

        assertThat(snapshot.getSequence()).isEqualTo(1);
        assertThat(snapshot.getReason()).isEqualTo("AFTER_ATTACK");
        assertThat(sessionRepository.findByGameId(gameId.value())).hasValueSatisfying(session -> {
            assertThat(session.getPlayerOneId()).isEqualTo(PLAYER_ONE.value());
            assertThat(session.getPlayerTwoId()).isEqualTo(PLAYER_TWO.value());
            assertThat(session.getStatus()).isEqualTo(GameStatus.ACTIVE.name());
            assertThat(session.getCurrentPlayerId()).isEqualTo(PLAYER_ONE.value());
            assertThat(session.getTurnNumber()).isEqualTo(4);
        });
        assertThat(restored.getGameId()).isEqualTo(gameId);
        assertThat(restored.getTurnState().phase()).isEqualTo(TurnPhase.MAIN);
        assertThat(restored.getTurnState().energyAttachedThisTurn()).isTrue();
        assertThat(restored.getPlayerOneState().getHand().getCards()).hasSize(2);
        assertThat(restored.getPlayerOneState().getDeck().getCards()).hasSize(2);
        assertThat(restored.getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(2);
        assertThat(restored.getPlayerOneState().getDiscardPile().getCards()).hasSize(1);
        PokemonInPlay active = restored.getPlayerOneState().getBoard().getActivePokemon().orElseThrow().getPokemon();
        assertThat(active.getDamageCounters()).isEqualTo(3);
        assertThat(active.hasSpecialCondition(SpecialCondition.POISONED)).isTrue();
        assertThat(active.hasSpecialCondition(SpecialCondition.BURNED)).isTrue();
        assertThat(active.getAttachedCards().getEnergies()).hasSize(1);
        assertThat(restored.getPendingActiveReplacement()).hasValueSatisfying(pending -> {
            assertThat(pending.playerId()).isEqualTo(PLAYER_TWO);
            assertThat(pending.reason()).isEqualTo(ActiveReplacementReason.ACTIVE_KNOCKED_OUT);
        });
    }

    @Test
    void appendsImmutableLogsWithIndependentSequencesPerGame() {
        GameState firstGame = complexState(new GameId("persist-log-game-1"));
        GameState secondGame = complexState(new GameId("persist-log-game-2"));

        GameActionLogEntity first = service.appendActionLog(firstGame, command(firstGame.getGameId(), "START_TURN"));
        GameActionLogEntity second = service.appendActionLog(firstGame, command(firstGame.getGameId(), "DECLARE_ATTACK"));
        GameActionLogEntity otherGameFirst = service.appendActionLog(secondGame, command(secondGame.getGameId(), "START_TURN"));

        assertThat(first.getSequence()).isEqualTo(1);
        assertThat(second.getSequence()).isEqualTo(2);
        assertThat(otherGameFirst.getSequence()).isEqualTo(1);
        assertThat(actionLogRepository.findByGameIdOrderBySequenceAsc(firstGame.getGameId().value()))
                .extracting(GameActionLogEntity::getActionType)
                .containsExactly("START_TURN", "DECLARE_ATTACK");
        assertThat(actionLogRepository.findByGameIdOrderBySequenceAsc(secondGame.getGameId().value()))
                .extracting(GameActionLogEntity::getActionType)
                .containsExactly("START_TURN");
        assertThat(second.getCommandJson()).contains("DECLARE_ATTACK");
        assertThat(second.getResultJson()).contains("resolved");
        assertThat(second.getEventsJson()).contains("AttackDeclared");
    }

    @Test
    void persistsActionResultWithSnapshotLinkedToActionLogAndPendingEffectSelection() {
        GameState state = complexState(new GameId("persist-pending-game"));
        PendingEffectSelection pending = new PendingEffectSelection(
                PLAYER_ONE,
                EffectType.SEARCH_DECK,
                "xy1-123",
                EffectCardZone.DECK,
                EffectTarget.ACTING_PLAYER,
                0,
                2,
                CardFilterSpec.subtype(CardSubtype.BASIC_ENERGY),
                true,
                true,
                null,
                List.of(new CardInstanceId("p1-deck-1"))
        );

        GamePersistenceService.PersistedGameAction persisted = service.persistActionResult(
                state,
                command(state.getGameId(), "PLAY_TRAINER"),
                pending,
                "PENDING_SELECTION_REQUIRED"
        );

        assertThat(persisted.actionLog().getSequence()).isEqualTo(1);
        assertThat(persisted.snapshot().getSequence()).isEqualTo(1);
        assertThat(persisted.snapshot().getActionLogId()).isEqualTo(persisted.actionLog().getId());
        assertThat(persisted.snapshot().getPendingEffectType()).isEqualTo(EffectType.SEARCH_DECK.name());
        assertThat(persisted.snapshot().getPendingEffectSelectionJson()).contains("xy1-123");
        assertThat(persisted.snapshot().getSnapshotJson()).contains("pendingEffectSelection");
        assertThat(service.loadLatestPendingEffectSelection(state.getGameId())).hasValueSatisfying(restored -> {
            assertThat(restored.playerId()).isEqualTo(PLAYER_ONE);
            assertThat(restored.effectType()).isEqualTo(EffectType.SEARCH_DECK);
            assertThat(restored.revealSelectedCards()).isTrue();
            assertThat(restored.requiresShuffle()).isTrue();
            assertThat(restored.candidateCardIds()).containsExactly(new CardInstanceId("p1-deck-1"));
        });
    }

    @Test
    void latestSnapshotUsesHighestSequenceWithoutMixingGames() {
        GameState firstGame = complexState(new GameId("persist-latest-game-1"));
        GameState secondGame = complexState(new GameId("persist-latest-game-2"));

        service.saveSnapshot(firstGame, GameSnapshotCommand.automatic("FIRST"));
        service.saveSnapshot(firstGame, GameSnapshotCommand.automatic("SECOND"));
        service.saveSnapshot(secondGame, GameSnapshotCommand.automatic("OTHER"));

        assertThat(snapshotRepository.findTopByGameIdOrderBySequenceDesc(firstGame.getGameId().value()))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.getSequence()).isEqualTo(2);
                    assertThat(snapshot.getReason()).isEqualTo("SECOND");
                });
        assertThat(snapshotRepository.findTopByGameIdOrderBySequenceDesc(secondGame.getGameId().value()))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.getSequence()).isEqualTo(1));
    }

    private GameActionLogCommand command(GameId gameId, String actionType) {
        return new GameActionLogCommand(
                gameId,
                PLAYER_ONE,
                actionType,
                Map.of("type", actionType, "source", "test"),
                Map.of("status", "resolved"),
                List.of(Map.of("type", "AttackDeclared"))
        );
    }

    private GameState complexState(GameId gameId) {
        CardInstance p1Active = pokemon("p1-active", PLAYER_ONE);
        CardInstance p1Energy = energy("p1-active-energy", PLAYER_ONE, EnergyType.LIGHTNING);
        PokemonInPlay active = new PokemonInPlay(
                p1Active,
                new AttachedCards(List.of(p1Energy))
        ).withDamageCounters(3).withSpecialConditions(SpecialConditionSet.none()
                .apply(SpecialCondition.POISONED)
                .apply(SpecialCondition.BURNED));

        PlayerGameState playerOne = new PlayerGameState(
                PLAYER_ONE,
                new DeckZone(List.of(pokemon("p1-deck-1", PLAYER_ONE), pokemon("p1-deck-2", PLAYER_ONE))),
                new HandZone(List.of(pokemon("p1-hand-1", PLAYER_ONE), pokemon("p1-hand-2", PLAYER_ONE))),
                new PrizeCards(List.of(pokemon("p1-prize-1", PLAYER_ONE), pokemon("p1-prize-2", PLAYER_ONE))),
                new DiscardPile(List.of(pokemon("p1-discard-1", PLAYER_ONE))),
                new BoardState(new ActivePokemon(active), new Bench(List.of(PokemonInPlay.withoutAttachments(pokemon("p1-bench-1", PLAYER_ONE))))),
                2
        );
        PlayerGameState playerTwo = new PlayerGameState(
                PLAYER_TWO,
                new DeckZone(List.of(pokemon("p2-deck-1", PLAYER_TWO))),
                new HandZone(List.of(pokemon("p2-hand-1", PLAYER_TWO))),
                new PrizeCards(List.of(pokemon("p2-prize-1", PLAYER_TWO))),
                DiscardPile.empty(),
                new BoardState(new ActivePokemon(PokemonInPlay.withoutAttachments(pokemon("p2-active", PLAYER_TWO))), new Bench(List.of(PokemonInPlay.withoutAttachments(pokemon("p2-bench-1", PLAYER_TWO))))),
                1
        );
        return new GameState(
                gameId,
                GameStatus.ACTIVE,
                playerOne,
                playerTwo,
                new TurnState(PLAYER_ONE, PLAYER_ONE, 4, TurnPhase.MAIN, true, true, true, false, true),
                null,
                null,
                new PendingActiveReplacement(PLAYER_TWO, ActiveReplacementReason.ACTIVE_KNOCKED_OUT),
                List.of()
        );
    }

    private CardInstance pokemon(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(
                id + "-def",
                "Pokemon " + id,
                CardSupertype.POKEMON,
                Set.of(CardSubtype.BASIC)
        ), owner);
    }

    private CardInstance energy(String id, PlayerId owner, EnergyType type) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(
                id + "-def",
                type.name() + " Energy",
                CardSupertype.ENERGY,
                Set.of(CardSubtype.BASIC_ENERGY),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.basic(type)
        ), owner);
    }
}
