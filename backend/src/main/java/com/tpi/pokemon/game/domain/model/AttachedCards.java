package com.tpi.pokemon.game.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AttachedCards {
    private final List<CardInstance> energies;
    private final CardInstance tool;

    public AttachedCards(List<CardInstance> cards) {
        this(cards, null);
    }

    public AttachedCards(List<CardInstance> energies, CardInstance tool) {
        Objects.requireNonNull(energies, "energies must not be null");
        if (energies.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("energies must not contain null values");
        }
        List<CardInstance> allCards = new java.util.ArrayList<>(energies);
        if (tool != null) {
            allCards.add(tool);
        }
        GameStateValidation.requireUniqueCards(allCards, "attached cards");
        this.energies = List.copyOf(energies);
        this.tool = tool;
    }

    public static AttachedCards empty() {
        return new AttachedCards(List.of());
    }

    public List<CardInstance> getCards() {
        if (tool == null) {
            return energies;
        }
        List<CardInstance> cards = new java.util.ArrayList<>(energies);
        cards.add(tool);
        return List.copyOf(cards);
    }

    public List<CardInstance> getEnergies() {
        return energies;
    }

    public Optional<CardInstance> getTool() {
        return Optional.ofNullable(tool);
    }

    public AttachedCards withEnergy(CardInstance energy) {
        Objects.requireNonNull(energy, "energy must not be null");
        List<CardInstance> updated = new java.util.ArrayList<>(energies);
        updated.add(energy);
        return new AttachedCards(updated, tool);
    }

    public AttachedCards withTool(CardInstance tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        if (this.tool != null) {
            throw new IllegalArgumentException("Pokemon already has a tool attached");
        }
        return new AttachedCards(energies, tool);
    }

    public AttachedCards withoutEnergies(Set<com.tpi.pokemon.game.domain.value.CardInstanceId> energyIds) {
        Objects.requireNonNull(energyIds, "energyIds must not be null");
        return new AttachedCards(energies.stream().filter(card -> !energyIds.contains(card.id())).toList(), tool);
    }
}
