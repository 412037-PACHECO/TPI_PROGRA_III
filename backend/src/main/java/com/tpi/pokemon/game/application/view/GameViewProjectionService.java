package com.tpi.pokemon.game.application.view;

import com.tpi.pokemon.game.application.UnauthorizedGameViewException;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.model.AttachedCards;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.SpecialConditionSet;
import com.tpi.pokemon.game.domain.model.StadiumInPlay;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import com.tpi.pokemon.game.engine.victory.GameFinishResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameViewProjectionService {
    public GameViewResponse project(GameState state, PlayerId viewerPlayerId, PendingEffectSelection pendingSelection) {
        PlayerGameState viewer = requirePlayer(state, viewerPlayerId);
        PlayerGameState opponent = opponentOf(state, viewerPlayerId);

        GameFinishResult finishResult = state.getFinishResult().orElse(null);
        return new GameViewResponse(
                state.getGameId().value(),
                state.getStatus().name(),
                viewerPlayerId.value(),
                playerView(viewer),
                opponentView(opponent),
                turnView(state.getTurnState()),
                state.getActiveStadium().map(this::stadiumView).orElse(null),
                pendingSelectionView(pendingSelection, viewerPlayerId),
                finishResult == null ? null : finishResult.winner().map(PlayerId::value).orElse(null),
                finishResult == null ? null : finishResult.type().name(),
                finishResult == null ? null : finishResult.reasons().stream().map(Enum::name).findFirst().orElse(null)
        );
    }

    private PlayerGameState requirePlayer(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        throw new UnauthorizedGameViewException(playerId.value(), state.getGameId().value());
    }

    private PlayerGameState opponentOf(GameState state, PlayerId playerId) {
        if (state.getPlayerOneState().getPlayerId().equals(playerId)) {
            return state.getPlayerTwoState();
        }
        if (state.getPlayerTwoState().getPlayerId().equals(playerId)) {
            return state.getPlayerOneState();
        }
        throw new UnauthorizedGameViewException(playerId.value(), state.getGameId().value());
    }

    private PlayerPerspectiveView playerView(PlayerGameState player) {
        return new PlayerPerspectiveView(
                player.getPlayerId().value(),
                true,
                new HandView(player.getHand().getCards().size(), cards(player.getHand().getCards())),
                new DeckView(player.getDeck().getCards().size(), false, List.of()),
                new PrizeCardsView(player.getPrizeCards().remainingCount(), false, List.of()),
                discardPile(player),
                new PlayerBoardView(activePokemon(player), bench(player)),
                player.getTurnsTaken()
        );
    }

    private OpponentPerspectiveView opponentView(PlayerGameState opponent) {
        return new OpponentPerspectiveView(
                opponent.getPlayerId().value(),
                new HandView(opponent.getHand().getCards().size(), List.of()),
                new DeckView(opponent.getDeck().getCards().size(), false, List.of()),
                new PrizeCardsView(opponent.getPrizeCards().remainingCount(), false, List.of()),
                discardPile(opponent),
                new OpponentBoardView(activePokemon(opponent), bench(opponent)),
                opponent.getTurnsTaken()
        );
    }

    private DiscardPileView discardPile(PlayerGameState player) {
        return new DiscardPileView(player.getDiscardPile().getCards().size(), cards(player.getDiscardPile().getCards()));
    }

    private PokemonInPlayView activePokemon(PlayerGameState player) {
        return player.getBoard().getActivePokemon().map(active -> pokemon(active.getPokemon())).orElse(null);
    }

    private List<PokemonInPlayView> bench(PlayerGameState player) {
        return player.getBoard().getBench().getPokemon().stream().map(this::pokemon).toList();
    }

    private PokemonInPlayView pokemon(PokemonInPlay pokemon) {
        AttachedCards attached = pokemon.getAttachedCards();
        return new PokemonInPlayView(
                card(pokemon.getTopCard()),
                cards(pokemon.getEvolutionStack()),
                attached.getEnergies().stream().map(energy -> new AttachedCardView(card(energy), "ENERGY")).toList(),
                attached.getTool().map(tool -> new AttachedCardView(card(tool), "TOOL")).orElse(null),
                pokemon.getDamageCounters(),
                pokemon.getDamageCounters() * 10,
                specialConditions(pokemon.getSpecialConditions()),
                pokemon.getEnteredTurnNumber(),
                pokemon.getLastEvolvedTurnNumber().isPresent() ? pokemon.getLastEvolvedTurnNumber().getAsInt() : null
        );
    }

    private List<String> specialConditions(SpecialConditionSet conditions) {
        List<String> values = new ArrayList<>();
        for (SpecialCondition condition : SpecialCondition.values()) {
            if (conditions.has(condition)) {
                values.add(condition.name());
            }
        }
        return values;
    }

    private List<CardView> cards(List<CardInstance> cards) {
        return cards.stream().map(this::card).toList();
    }

    private CardView card(CardInstance card) {
        CardDefinitionRef definition = card.definition();
        return new CardView(
                card.id().value(),
                definition.cardId(),
                definition.name(),
                definition.supertype().name(),
                definition.subtypes().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                definition.hp(),
                definition.pokemonTypes().stream().map(Enum::name).toList(),
                definition.retreatCost()
        );
    }

    private TurnView turnView(TurnState turn) {
        return new TurnView(
                turn.currentPlayer() == null ? null : turn.currentPlayer().value(),
                turn.startingPlayer() == null ? null : turn.startingPlayer().value(),
                turn.turnNumber(),
                turn.phase().name(),
                turn.cardDrawnThisTurn(),
                turn.energyAttachedThisTurn(),
                turn.supporterPlayedThisTurn(),
                turn.stadiumPlayedThisTurn(),
                turn.retreatedThisTurn()
        );
    }

    private StadiumView stadiumView(StadiumInPlay stadium) {
        return new StadiumView(card(stadium.card()), stadium.playedBy().value(), stadium.playedTurnNumber());
    }

    private PendingSelectionView pendingSelectionView(PendingEffectSelection pending, PlayerId viewerPlayerId) {
        if (pending == null) {
            return new PendingSelectionView(false, false, null, null, null, null, null, 0, 0, false, false, List.of());
        }
        boolean viewerMustChoose = pending.playerId().equals(viewerPlayerId);
        return new PendingSelectionView(
                true,
                viewerMustChoose,
                pending.playerId().value(),
                pending.effectType().name(),
                pending.sourceId(),
                pending.sourceZone() == null ? null : pending.sourceZone().name(),
                pending.target().name(),
                pending.minSelections(),
                pending.maxSelections(),
                pending.revealSelectedCards(),
                pending.requiresShuffle(),
                viewerMustChoose ? pending.candidateCardIds().stream().map(id -> id.value()).toList() : List.of()
        );
    }
}
