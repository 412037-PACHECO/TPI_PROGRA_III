package com.tpi.pokemon.game.engine.action;

import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.Bench;
import com.tpi.pokemon.game.domain.model.BoardState;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.DiscardPile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.StadiumInPlay;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonRetreatedEvent;
import com.tpi.pokemon.game.engine.event.BasicPokemonBenchedEvent;
import com.tpi.pokemon.game.engine.event.EnergyAttachedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PokemonEvolvedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionAppliedEvent;
import com.tpi.pokemon.game.engine.event.StadiumReplacedEvent;
import com.tpi.pokemon.game.engine.event.TrainerPlayedEvent;
import com.tpi.pokemon.game.engine.special.ApplySpecialConditionCommand;
import com.tpi.pokemon.game.engine.special.StatusEffectManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TurnActionService {
    private final StatusEffectManager statusEffectManager = new StatusEffectManager();

    public GameState putBasicPokemonOnBench(GameState state, PutBasicPokemonOnBenchCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        CardInstance card = requireCardInHand(context.playerState(), command.cardId());
        if (!card.definition().isBasicPokemon()) {
            throw new ActionException("Card must be a Basic Pokemon");
        }
        if (context.playerState().getBoard().getBench().getPokemon().size() >= Bench.MAX_SIZE) {
            throw new ActionException("Bench is full");
        }
        List<CardInstance> hand = removeFromHand(context.playerState(), card.id());
        List<PokemonInPlay> bench = new ArrayList<>(context.playerState().getBoard().getBench().getPokemon());
        bench.add(PokemonInPlay.playedThisTurn(card, context.turnState().turnNumber()));
        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), new HandZone(hand), context.playerState().getDiscardPile(), new BoardState(context.playerState().getBoard().getActivePokemon().orElse(null), new Bench(bench)), context.playerState().getTurnsTaken());
        return withEvent(context.state(), updatedPlayer, context.turnState(), context.state().getActiveStadium().orElse(null), new BasicPokemonBenchedEvent(context.state().getGameId(), command.playerId(), card.id()));
    }

    public GameState attachEnergy(GameState state, AttachEnergyCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        if (context.turnState().energyAttachedThisTurn()) {
            throw new ActionException("Energy has already been attached this turn");
        }
        CardInstance energy = requireCardInHand(context.playerState(), command.energyCardId());
        if (!energy.definition().isEnergy()) {
            throw new ActionException("Card must be an Energy");
        }
        TargetUpdate target = updateTarget(context.playerState().getBoard(), command.target(), pokemon -> pokemon.withAttachedEnergy(energy));
        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), new HandZone(removeFromHand(context.playerState(), energy.id())), context.playerState().getDiscardPile(), target.board(), context.playerState().getTurnsTaken());
        return withEvent(context.state(), updatedPlayer, context.turnState().withEnergyAttached(), context.state().getActiveStadium().orElse(null), new EnergyAttachedEvent(context.state().getGameId(), command.playerId(), energy.id(), target.updatedPokemon().getTopCard().id()));
    }

    public GameState evolvePokemon(GameState state, EvolvePokemonCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        CardInstance evolution = requireCardInHand(context.playerState(), command.evolutionCardId());
        if (!evolution.definition().canEvolve()) {
            throw new ActionException("Card must be an evolution Pokemon");
        }
        if (context.playerState().getTurnsTaken() == 1) {
            throw new ActionException("Pokemon cannot evolve on the player's first turn");
        }
        PokemonInPlay target = getTarget(context.playerState().getBoard(), command.target());
        if (target.wasPlayedThisTurn(context.turnState().turnNumber())) {
            throw new ActionException("Pokemon played this turn cannot evolve");
        }
        if (target.evolvedThisTurn(context.turnState().turnNumber())) {
            throw new ActionException("Pokemon has already evolved this turn");
        }
        if (!evolution.definition().evolvesFrom().equalsIgnoreCase(target.getTopCard().definition().name())) {
            throw new ActionException("Evolution does not match target Pokemon");
        }
        CardInstance previousTop = target.getTopCard();
        TargetUpdate update = updateTarget(context.playerState().getBoard(), command.target(), pokemon -> pokemon.evolve(evolution, context.turnState().turnNumber()));
        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), new HandZone(removeFromHand(context.playerState(), evolution.id())), context.playerState().getDiscardPile(), update.board(), context.playerState().getTurnsTaken());
        return withEvent(context.state(), updatedPlayer, context.turnState(), context.state().getActiveStadium().orElse(null), new PokemonEvolvedEvent(context.state().getGameId(), command.playerId(), evolution.id(), previousTop.id()));
    }

    public GameState retreatActivePokemon(GameState state, RetreatActivePokemonCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        if (context.turnState().retreatedThisTurn()) {
            throw new ActionException("Player has already retreated this turn");
        }
        PokemonInPlay active = context.playerState().getBoard().getActivePokemon().orElseThrow(() -> new ActionException("Active Pokemon is required")).getPokemon();
        if (active.hasSpecialCondition(SpecialCondition.ASLEEP)) {
            throw new ActionException("Asleep Pokemon cannot retreat");
        }
        if (active.hasSpecialCondition(SpecialCondition.PARALYZED)) {
            throw new ActionException("Paralyzed Pokemon cannot retreat");
        }
        List<PokemonInPlay> bench = context.playerState().getBoard().getBench().getPokemon();
        if (command.benchIndex() < 0 || command.benchIndex() >= bench.size()) {
            throw new ActionException("Bench index is invalid");
        }
        Integer cost = active.getTopCard().definition().retreatCost();
        if (cost == null) {
            throw new ActionException("Retreat cost is unknown");
        }
        List<CardInstanceId> discardIds = command.energyCardsToDiscard() == null ? List.of() : command.energyCardsToDiscard();
        if (new HashSet<>(discardIds).size() != discardIds.size()) {
            throw new ActionException("Energy cards to discard must not contain duplicates");
        }
        if (discardIds.size() < cost) {
            throw new ActionException("Not enough Energy selected to pay retreat cost");
        }
        Set<CardInstanceId> discardSet = Set.copyOf(discardIds);
        List<CardInstance> attachedEnergies = active.getAttachedCards().getEnergies();
        for (CardInstanceId id : discardSet) {
            CardInstance energy = attachedEnergies.stream().filter(card -> card.id().equals(id)).findFirst().orElseThrow(() -> new ActionException("Selected Energy is not attached to the Active Pokemon"));
            if (!energy.definition().isEnergy()) {
                throw new ActionException("Selected card must be an Energy");
            }
        }
        PokemonInPlay paidActive = active.withoutAttachedEnergies(discardSet).clearSpecialConditions();
        PokemonInPlay newActive = bench.get(command.benchIndex());
        List<PokemonInPlay> updatedBench = new ArrayList<>(bench);
        updatedBench.set(command.benchIndex(), paidActive);
        List<CardInstance> discard = new ArrayList<>(context.playerState().getDiscardPile().getCards());
        attachedEnergies.stream().filter(card -> discardSet.contains(card.id())).forEach(discard::add);
        BoardState board = new BoardState(new ActivePokemon(newActive), new Bench(updatedBench));
        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), context.playerState().getHand(), new DiscardPile(discard), board, context.playerState().getTurnsTaken());
        return withEvent(context.state(), updatedPlayer, context.turnState().withRetreated(), context.state().getActiveStadium().orElse(null), new ActivePokemonRetreatedEvent(context.state().getGameId(), command.playerId(), newActive.getTopCard().id(), discardIds));
    }

    public GameState applySpecialCondition(GameState state, ApplySpecialConditionCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        TargetUpdate update = updateTarget(context.playerState().getBoard(), command.target(), pokemon -> statusEffectManager.applyCondition(pokemon, command.condition()));
        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), context.playerState().getHand(), context.playerState().getDiscardPile(), update.board(), context.playerState().getTurnsTaken());
        return withEvent(context.state(), updatedPlayer, context.turnState(), context.state().getActiveStadium().orElse(null), new SpecialConditionAppliedEvent(context.state().getGameId(), command.playerId(), update.updatedPokemon().getTopCard().id(), command.condition()));
    }

    public GameState playTrainer(GameState state, PlayTrainerCommand command) {
        Context context = requireMainTurn(state, command.playerId());
        CardInstance trainer = requireCardInHand(context.playerState(), command.trainerCardId());
        if (!trainer.definition().isTrainer()) {
            throw new ActionException("Card must be a Trainer");
        }
        if (trainer.definition().isTool()) {
            PokemonTarget target = command.target().orElseThrow(() -> new ActionException("Tool requires a target"));
            PokemonInPlay targetPokemon = getTarget(context.playerState().getBoard(), target);
            if (targetPokemon.getAttachedCards().getTool().isPresent()) {
                throw new ActionException("Target Pokemon already has a tool");
            }
            TargetUpdate update = updateTarget(context.playerState().getBoard(), target, pokemon -> pokemon.withAttachedTool(trainer));
            PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), new HandZone(removeFromHand(context.playerState(), trainer.id())), context.playerState().getDiscardPile(), update.board(), context.playerState().getTurnsTaken());
            return withEvent(context.state(), updatedPlayer, context.turnState(), context.state().getActiveStadium().orElse(null), new TrainerPlayedEvent(context.state().getGameId(), command.playerId(), trainer.id(), "TOOL"));
        }

        TurnState updatedTurn = context.turnState();
        StadiumInPlay activeStadium = context.state().getActiveStadium().orElse(null);
        StadiumInPlay replacedStadium = null;
        List<CardInstance> discard = new ArrayList<>(context.playerState().getDiscardPile().getCards());
        List<GameEvent> extraEvents = new ArrayList<>();
        String trainerType = "ITEM";
        if (trainer.definition().isSupporter()) {
            if (updatedTurn.supporterPlayedThisTurn()) {
                throw new ActionException("Supporter has already been played this turn");
            }
            updatedTurn = updatedTurn.withSupporterPlayed();
            trainerType = "SUPPORTER";
            discard.add(trainer);
        } else if (trainer.definition().isStadium()) {
            if (updatedTurn.stadiumPlayedThisTurn()) {
                throw new ActionException("Stadium has already been played this turn");
            }
            if (activeStadium != null) {
                replacedStadium = activeStadium;
                extraEvents.add(new StadiumReplacedEvent(context.state().getGameId(), command.playerId(), trainer.id(), activeStadium.card().id()));
                if (activeStadium.card().owner().equals(command.playerId())) {
                    discard.add(activeStadium.card());
                }
            }
            activeStadium = new StadiumInPlay(trainer, command.playerId(), updatedTurn.turnNumber());
            updatedTurn = updatedTurn.withStadiumPlayed();
            trainerType = "STADIUM";
        } else {
            discard.add(trainer);
        }

        PlayerGameState updatedPlayer = rebuildPlayer(context.playerState(), new HandZone(removeFromHand(context.playerState(), trainer.id())), new DiscardPile(discard), context.playerState().getBoard(), context.playerState().getTurnsTaken());
        GameState updatedState = context.state();
        if (replacedStadium != null && !replacedStadium.card().owner().equals(command.playerId())) {
            updatedState = discardReplacedStadiumForOwner(context.state(), replacedStadium);
        }
        updatedState = replacePlayer(updatedState, updatedPlayer, updatedTurn, activeStadium, extraEvents);
        return appendEvent(updatedState, new TrainerPlayedEvent(context.state().getGameId(), command.playerId(), trainer.id(), trainerType));
    }

    private Context requireMainTurn(GameState state, PlayerId playerId) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (state.getStatus() != GameStatus.ACTIVE) {
            throw new ActionException("Game must be ACTIVE");
        }
        if (state.getTurnState().phase() != TurnPhase.MAIN) {
            throw new ActionException("Actions are only allowed during MAIN phase");
        }
        if (!playerId.equals(state.getTurnState().currentPlayer())) {
            throw new ActionException("Only the current player can act");
        }
        return new Context(state, getPlayerState(state, playerId), state.getTurnState());
    }

    private CardInstance requireCardInHand(PlayerGameState player, CardInstanceId cardId) {
        return player.getHand().getCards().stream().filter(card -> card.id().equals(cardId)).findFirst().orElseThrow(() -> new ActionException("Card must be in hand"));
    }

    private List<CardInstance> removeFromHand(PlayerGameState player, CardInstanceId cardId) {
        return player.getHand().getCards().stream().filter(card -> !card.id().equals(cardId)).toList();
    }

    private PokemonInPlay getTarget(BoardState board, PokemonTarget target) {
        Objects.requireNonNull(target, "target must not be null");
        if (target.zone() == PokemonTargetZone.ACTIVE) {
            return board.getActivePokemon().orElseThrow(() -> new ActionException("Active Pokemon is required")).getPokemon();
        }
        List<PokemonInPlay> bench = board.getBench().getPokemon();
        if (target.benchIndex() < 0 || target.benchIndex() >= bench.size()) {
            throw new ActionException("Bench index is invalid");
        }
        return bench.get(target.benchIndex());
    }

    private TargetUpdate updateTarget(BoardState board, PokemonTarget target, java.util.function.Function<PokemonInPlay, PokemonInPlay> updater) {
        Objects.requireNonNull(target, "target must not be null");
        PokemonInPlay updatedPokemon;
        if (target.zone() == PokemonTargetZone.ACTIVE) {
            PokemonInPlay active = board.getActivePokemon().orElseThrow(() -> new ActionException("Active Pokemon is required")).getPokemon();
            updatedPokemon = updater.apply(active);
            return new TargetUpdate(new BoardState(new ActivePokemon(updatedPokemon), board.getBench()), updatedPokemon);
        }
        List<PokemonInPlay> bench = new ArrayList<>(board.getBench().getPokemon());
        if (target.benchIndex() < 0 || target.benchIndex() >= bench.size()) {
            throw new ActionException("Bench index is invalid");
        }
        updatedPokemon = updater.apply(bench.get(target.benchIndex()));
        bench.set(target.benchIndex(), updatedPokemon);
        return new TargetUpdate(new BoardState(board.getActivePokemon().orElse(null), new Bench(bench)), updatedPokemon);
    }

    private PlayerGameState rebuildPlayer(PlayerGameState player, HandZone hand, DiscardPile discardPile, BoardState board, int turnsTaken) {
        return new PlayerGameState(player.getPlayerId(), player.getDeck(), hand, player.getPrizeCards(), discardPile, board, turnsTaken);
    }

    private GameState withEvent(GameState state, PlayerGameState updatedPlayer, TurnState turnState, StadiumInPlay stadium, GameEvent event) {
        return appendEvent(replacePlayer(state, updatedPlayer, turnState, stadium, List.of()), event);
    }

    private GameState replacePlayer(GameState state, PlayerGameState updatedPlayer, TurnState turnState, StadiumInPlay stadium, List<GameEvent> extraEvents) {
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(updatedPlayer.getPlayerId()) ? updatedPlayer : state.getPlayerTwoState();
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.addAll(extraEvents);
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, turnState, stadium, events);
    }

    private GameState appendEvent(GameState state, GameEvent event) {
        List<GameEvent> events = new ArrayList<>(state.getEvents());
        events.add(event);
        return new GameState(state.getGameId(), state.getStatus(), state.getPlayerOneState(), state.getPlayerTwoState(), state.getTurnState(), state.getActiveStadium().orElse(null), events);
    }

    private GameState discardReplacedStadiumForOwner(GameState state, StadiumInPlay stadium) {
        PlayerGameState owner = getPlayerState(state, stadium.card().owner());
        List<CardInstance> discard = new ArrayList<>(owner.getDiscardPile().getCards());
        discard.add(stadium.card());
        PlayerGameState updatedOwner = rebuildPlayer(owner, owner.getHand(), new DiscardPile(discard), owner.getBoard(), owner.getTurnsTaken());
        PlayerGameState playerOne = state.getPlayerOneState().getPlayerId().equals(updatedOwner.getPlayerId()) ? updatedOwner : state.getPlayerOneState();
        PlayerGameState playerTwo = state.getPlayerTwoState().getPlayerId().equals(updatedOwner.getPlayerId()) ? updatedOwner : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), null, state.getEvents());
    }

    private PlayerGameState getPlayerState(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new ActionException("Player is not part of this game");
    }

    private record Context(GameState state, PlayerGameState playerState, TurnState turnState) {}
    private record TargetUpdate(BoardState board, PokemonInPlay updatedPokemon) {}
}
