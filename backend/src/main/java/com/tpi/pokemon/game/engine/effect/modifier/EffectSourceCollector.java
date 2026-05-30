package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EffectSourceCollector {
    public List<CardEffectSource> collect(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        List<CardEffectSource> sources = new ArrayList<>();
        collectPlayerSources(state.getPlayerOneState(), sources);
        collectPlayerSources(state.getPlayerTwoState(), sources);
        state.getActiveStadium().ifPresent(stadium -> {
            if (!stadium.card().definition().effects().isEmpty()) {
                sources.add(new CardEffectSource(stadium.card(), stadium.playedBy(), EffectSourceKind.STADIUM, false, null, null));
            }
        });
        return List.copyOf(sources);
    }

    private void collectPlayerSources(PlayerGameState player, List<CardEffectSource> sources) {
        player.getBoard().getActivePokemon().map(ActivePokemon::getPokemon).ifPresent(pokemon -> collectPokemonSources(player, pokemon, true, sources));
        for (PokemonInPlay pokemon : player.getBoard().getBench().getPokemon()) {
            collectPokemonSources(player, pokemon, false, sources);
        }
    }

    private void collectPokemonSources(PlayerGameState player, PokemonInPlay pokemon, boolean active, List<CardEffectSource> sources) {
        CardInstance topCard = pokemon.getTopCard();
        if (!topCard.definition().effects().isEmpty()) {
            sources.add(new CardEffectSource(topCard, player.getPlayerId(), EffectSourceKind.POKEMON, active, pokemon, null));
        }
        pokemon.getAttachedCards().getTool()
                .filter(tool -> !tool.definition().effects().isEmpty())
                .ifPresent(tool -> sources.add(new CardEffectSource(tool, player.getPlayerId(), EffectSourceKind.TOOL, active, null, pokemon)));
    }
}
