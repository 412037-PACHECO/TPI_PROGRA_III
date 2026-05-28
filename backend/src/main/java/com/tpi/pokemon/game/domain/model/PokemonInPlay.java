package com.tpi.pokemon.game.domain.model;

import com.tpi.pokemon.game.domain.value.CardInstanceId;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

public final class PokemonInPlay {
    private final List<CardInstance> evolutionStack;
    private final AttachedCards attachedCards;
    private final int enteredTurnNumber;
    private final Integer lastEvolvedTurnNumber;
    private final int damageCounters;

    public PokemonInPlay(CardInstance baseCard, AttachedCards attachedCards) {
        this(List.of(baseCard), attachedCards, 0, null, 0);
    }

    public PokemonInPlay(List<CardInstance> evolutionStack, AttachedCards attachedCards, int enteredTurnNumber, Integer lastEvolvedTurnNumber) {
        this(evolutionStack, attachedCards, enteredTurnNumber, lastEvolvedTurnNumber, 0);
    }

    public PokemonInPlay(List<CardInstance> evolutionStack, AttachedCards attachedCards, int enteredTurnNumber, Integer lastEvolvedTurnNumber, int damageCounters) {
        Objects.requireNonNull(evolutionStack, "evolutionStack must not be null");
        if (evolutionStack.isEmpty()) {
            throw new IllegalArgumentException("evolutionStack must not be empty");
        }
        if (evolutionStack.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("evolutionStack must not contain null values");
        }
        if (enteredTurnNumber < 0) {
            throw new IllegalArgumentException("enteredTurnNumber must not be negative");
        }
        if (lastEvolvedTurnNumber != null && lastEvolvedTurnNumber < 0) {
            throw new IllegalArgumentException("lastEvolvedTurnNumber must not be negative");
        }
        if (damageCounters < 0) {
            throw new IllegalArgumentException("damageCounters must not be negative");
        }
        GameStateValidation.requireUniqueCards(evolutionStack, "evolution stack");
        this.evolutionStack = List.copyOf(evolutionStack);
        this.attachedCards = Objects.requireNonNull(attachedCards, "attachedCards must not be null");
        Set<CardInstanceId> stackIds = this.evolutionStack.stream().map(CardInstance::id).collect(java.util.stream.Collectors.toSet());
        if (attachedCards.getCards().stream().anyMatch(card -> stackIds.contains(card.id()))) {
            throw new IllegalArgumentException("PokemonInPlay must not attach a card from its evolution stack");
        }
        this.enteredTurnNumber = enteredTurnNumber;
        this.lastEvolvedTurnNumber = lastEvolvedTurnNumber;
        this.damageCounters = damageCounters;
    }

    public static PokemonInPlay withoutAttachments(CardInstance baseCard) {
        return new PokemonInPlay(baseCard, AttachedCards.empty());
    }

    public static PokemonInPlay playedThisTurn(CardInstance basicCard, int turnNumber) {
        return new PokemonInPlay(List.of(basicCard), AttachedCards.empty(), turnNumber, null);
    }

    /**
     * Compatibility accessor: returns the current top card, not necessarily the original basic card.
     */
    public CardInstance getBaseCard() {
        return getTopCard();
    }

    public List<CardInstance> getEvolutionStack() {
        return evolutionStack;
    }

    public CardInstance getTopCard() {
        return evolutionStack.get(evolutionStack.size() - 1);
    }

    public AttachedCards getAttachedCards() {
        return attachedCards;
    }

    public int getEnteredTurnNumber() {
        return enteredTurnNumber;
    }

    public OptionalInt getLastEvolvedTurnNumber() {
        return lastEvolvedTurnNumber == null ? OptionalInt.empty() : OptionalInt.of(lastEvolvedTurnNumber);
    }

    public int getDamageCounters() {
        return damageCounters;
    }

    public boolean wasPlayedThisTurn(int turnNumber) {
        return enteredTurnNumber == turnNumber;
    }

    public boolean evolvedThisTurn(int turnNumber) {
        return lastEvolvedTurnNumber != null && lastEvolvedTurnNumber == turnNumber;
    }

    public PokemonInPlay withAttachedEnergy(CardInstance energy) {
        return new PokemonInPlay(evolutionStack, attachedCards.withEnergy(energy), enteredTurnNumber, lastEvolvedTurnNumber, damageCounters);
    }

    public PokemonInPlay withAttachedTool(CardInstance tool) {
        return new PokemonInPlay(evolutionStack, attachedCards.withTool(tool), enteredTurnNumber, lastEvolvedTurnNumber, damageCounters);
    }

    public PokemonInPlay withoutAttachedEnergies(Set<CardInstanceId> energyIds) {
        return new PokemonInPlay(evolutionStack, attachedCards.withoutEnergies(energyIds), enteredTurnNumber, lastEvolvedTurnNumber, damageCounters);
    }

    public PokemonInPlay evolve(CardInstance evolutionCard, int turnNumber) {
        List<CardInstance> updatedStack = new java.util.ArrayList<>(evolutionStack);
        updatedStack.add(Objects.requireNonNull(evolutionCard, "evolutionCard must not be null"));
        return new PokemonInPlay(updatedStack, attachedCards, enteredTurnNumber, turnNumber, damageCounters);
    }

    public PokemonInPlay withDamageCounters(int damageCounters) {
        return new PokemonInPlay(evolutionStack, attachedCards, enteredTurnNumber, lastEvolvedTurnNumber, damageCounters);
    }

    public PokemonInPlay applyDamage(int damageAmount) {
        if (damageAmount < 0 || damageAmount % 10 != 0) {
            throw new IllegalArgumentException("damageAmount must be non-negative and a multiple of 10");
        }
        return withDamageCounters(damageCounters + (damageAmount / 10));
    }
}
