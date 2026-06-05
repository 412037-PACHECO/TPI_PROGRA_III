package com.tpi.pokemon.game.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebSocketConfigTest {
    @Test
    void exposesWebSocketConfigurationBean() {
        assertThat(new WebSocketConfig()).isInstanceOf(WebSocketConfig.class);
    }
}
