package com.tpi.pokemon.game.application.view;

import java.util.List;

public record PokemonInPlayView(
        CardView topCard,
        java.util.List<CardView> evolutionStack,
        List<AttachedCardView> attachedEnergies,
        AttachedCardView tool,
        int damageCounters,
        int damageAmount,
        List<String> specialConditions,
        int enteredTurnNumber,
        Integer lastEvolvedTurnNumber
) {
}
