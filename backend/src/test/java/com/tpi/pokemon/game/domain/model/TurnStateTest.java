package com.tpi.pokemon.game.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpi.pokemon.game.domain.enums.TurnPhase;
import org.junit.jupiter.api.Test;

class TurnStateTest {
    @Test
    void notStartedInitializesAllOncePerTurnFlagsAsFalse() {
        TurnState turnState = TurnState.notStarted();

        assertThat(turnState.currentPlayer()).isNull();
        assertThat(turnState.turnNumber()).isZero();
        assertThat(turnState.phase()).isEqualTo(TurnPhase.NOT_STARTED);
        assertThat(turnState.energyAttachedThisTurn()).isFalse();
        assertThat(turnState.supporterPlayedThisTurn()).isFalse();
        assertThat(turnState.stadiumPlayedThisTurn()).isFalse();
        assertThat(turnState.retreatedThisTurn()).isFalse();
    }
}
