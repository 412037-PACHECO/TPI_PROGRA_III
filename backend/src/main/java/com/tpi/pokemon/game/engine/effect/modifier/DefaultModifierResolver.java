package com.tpi.pokemon.game.engine.effect.modifier;

import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind;
import com.tpi.pokemon.game.engine.effect.ability.EffectScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DefaultModifierResolver implements ModifierResolver {
    private final EffectSourceCollector sourceCollector;

    public DefaultModifierResolver() {
        this(new EffectSourceCollector());
    }

    public DefaultModifierResolver(EffectSourceCollector sourceCollector) {
        this.sourceCollector = Objects.requireNonNull(sourceCollector, "sourceCollector must not be null");
    }

    @Override
    public ModifierResolutionResult resolveDamage(DamageModifierContext context, int currentDamage, ModifierLayer layer) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(layer, "layer must not be null");
        return resolve(context.state(), ModifierType.DAMAGE, currentDamage, layer, context.defender(), context.defenderPlayerId(), context.attacker(), context.attackerPlayerId());
    }

    @Override
    public ModifierResolutionResult resolveRetreatCost(RetreatCostModifierContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return resolve(context.state(), ModifierType.RETREAT_COST, context.printedCost(), ModifierLayer.AFTER_WEAKNESS_RESISTANCE, context.pokemon(), context.playerId(), null, null);
    }

    @Override
    public ModifierResolutionResult resolveSpecialConditionPrevention(SpecialConditionModifierContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return resolve(context.state(), ModifierType.PREVENT_SPECIAL_CONDITION, 0, ModifierLayer.PREVENTION, context.target(), context.targetPlayerId(), null, null);
    }

    private ModifierResolutionResult resolve(GameState state, ModifierType type, int initialValue, ModifierLayer layer, PokemonInPlay defaultTarget, PlayerId defaultTargetOwner, PokemonInPlay attacker, PlayerId attackerOwner) {
        int value = initialValue;
        boolean prevented = false;
        List<AppliedModifier> applied = new ArrayList<>();
        for (CardEffectSource source : sourceCollector.collect(state)) {
            for (CardEffectDefinition effect : source.card().definition().effects()) {
                if (effect.activationKind() != EffectActivationKind.CONTINUOUS || effect.timing() != EffectTiming.CONTINUOUS) {
                    continue;
                }
                PokemonInPlay target = defaultTarget;
                PlayerId targetOwner = defaultTargetOwner;
                for (ModifierDefinition modifier : effect.modifiers()) {
                    if (modifier.type() != type || modifier.layer() != layer) {
                        continue;
                    }
                    if (modifier.targetRole() == ModifierTargetRole.ATTACKER && attacker != null) {
                        target = attacker;
                        targetOwner = attackerOwner;
                    } else if (modifier.targetRole() == ModifierTargetRole.DEFENDER) {
                        target = defaultTarget;
                        targetOwner = defaultTargetOwner;
                    }
                    if (!effect.condition().matches(target) || !matchesScope(state, source, effect.scope(), target, targetOwner)) {
                        continue;
                    }
                    int before = value;
                    if (modifier.operation() == ModifierOperation.PREVENT) {
                        value = 0;
                        prevented = true;
                    } else {
                        value = apply(value, modifier, type);
                    }
                    if (before != value || modifier.operation() == ModifierOperation.PREVENT) {
                        applied.add(new AppliedModifier(source.card().id(), effect.effectId(), modifier.type(), modifier.operation(), modifier.layer(), modifier.amount(), before, value));
                    }
                }
            }
        }
        return new ModifierResolutionResult(value, prevented, applied);
    }

    private int apply(int value, ModifierDefinition modifier, ModifierType type) {
        int updated = switch (modifier.operation()) {
            case ADD -> value + modifier.amount();
            case SUBTRACT -> value - modifier.amount();
            case SET -> modifier.amount();
            case MULTIPLY -> value * modifier.amount();
            case PREVENT -> 0;
        };
        if (type == ModifierType.DAMAGE) {
            return Math.max(0, roundDownToTen(updated));
        }
        return Math.max(0, updated);
    }

    private int roundDownToTen(int value) {
        return value - Math.floorMod(value, 10);
    }

    private boolean matchesScope(GameState state, CardEffectSource source, EffectScope scope, PokemonInPlay target, PlayerId targetOwner) {
        boolean sameOwner = source.owner().equals(targetOwner);
        boolean targetActive = isActive(state, target, targetOwner);
        boolean self = source.sourcePokemon().filter(target::equals).isPresent() || source.attachedTo().filter(target::equals).isPresent();
        return switch (scope) {
            case ANY -> true;
            case OWN_ACTIVE -> sameOwner && targetActive;
            case OWN_BENCH -> sameOwner && !targetActive;
            case OWN_POKEMON -> sameOwner;
            case OPPONENT_ACTIVE -> !sameOwner && targetActive;
            case OPPONENT_BENCH -> !sameOwner && !targetActive;
            case OPPONENT_POKEMON -> !sameOwner;
            case ACTIVE -> targetActive;
            case BENCH -> !targetActive;
            case SELF -> self;
        };
    }

    private boolean isActive(GameState state, PokemonInPlay target, PlayerId targetOwner) {
        if (state.getPlayerOneState().getPlayerId().equals(targetOwner)) {
            return state.getPlayerOneState().getBoard().getActivePokemon().map(active -> active.getPokemon().equals(target)).orElse(false);
        }
        if (state.getPlayerTwoState().getPlayerId().equals(targetOwner)) {
            return state.getPlayerTwoState().getBoard().getActivePokemon().map(active -> active.getPokemon().equals(target)).orElse(false);
        }
        return false;
    }
}
