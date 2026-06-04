package com.tpi.pokemon.game.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.SpecialConditionSet;
import com.tpi.pokemon.game.domain.model.StadiumInPlay;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import com.tpi.pokemon.game.engine.knockout.ActiveReplacementReason;
import com.tpi.pokemon.game.engine.knockout.PendingActiveReplacement;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import com.tpi.pokemon.game.engine.victory.GameFinishResult;
import com.tpi.pokemon.game.engine.victory.GameFinishType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameStateSnapshotMapper {
    static final int SNAPSHOT_VERSION = 1;

    private final ObjectMapper objectMapper;

    public GameStateSnapshotMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(GameState state, long sequence) {
        return toJson(state, sequence, null);
    }

    public String toJson(GameState state, long sequence, PendingEffectSelection pendingEffectSelection) {
        try {
            return objectMapper.writeValueAsString(toSnapshot(state, sequence, pendingEffectSelection));
        } catch (JsonProcessingException e) {
            throw new GamePersistenceException("Could not serialize game snapshot", e);
        }
    }

    public GameState fromJson(String snapshotJson) {
        try {
            return fromSnapshot(objectMapper.readValue(snapshotJson, GameStateSnapshot.class));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new GamePersistenceException("Could not deserialize game snapshot", e);
        }
    }

    public String pendingEffectSelectionToJson(PendingEffectSelection pendingEffectSelection) {
        if (pendingEffectSelection == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pendingEffectSelection);
        } catch (JsonProcessingException e) {
            throw new GamePersistenceException("Could not serialize pending effect selection", e);
        }
    }

    public java.util.Optional<PendingEffectSelection> pendingEffectSelectionFromJson(String json) {
        if (json == null || json.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(objectMapper.readValue(json, PendingEffectSelection.class));
        } catch (JsonProcessingException e) {
            throw new GamePersistenceException("Could not deserialize pending effect selection", e);
        }
    }

    private GameStateSnapshot toSnapshot(GameState state, long sequence, PendingEffectSelection pendingEffectSelection) {
        return new GameStateSnapshot(
                SNAPSHOT_VERSION,
                sequence,
                state.getGameId().value(),
                state.getStatus(),
                toPlayerSnapshot(state.getPlayerOneState()),
                toPlayerSnapshot(state.getPlayerTwoState()),
                toTurnSnapshot(state.getTurnState()),
                state.getActiveStadium().map(this::toStadiumSnapshot).orElse(null),
                state.getFinishResult().map(this::toFinishSnapshot).orElse(null),
                state.getPendingActiveReplacement().map(this::toPendingReplacementSnapshot).orElse(null),
                pendingEffectSelection == null ? null : objectMapper.valueToTree(pendingEffectSelection)
        );
    }

    private GameState fromSnapshot(GameStateSnapshot snapshot) {
        if (snapshot.snapshotVersion() != SNAPSHOT_VERSION) {
            throw new GamePersistenceException("Unsupported game snapshot version: " + snapshot.snapshotVersion());
        }
        return new GameState(
                new GameId(snapshot.gameId()),
                snapshot.status(),
                fromPlayerSnapshot(snapshot.playerOne()),
                fromPlayerSnapshot(snapshot.playerTwo()),
                fromTurnSnapshot(snapshot.turn()),
                fromStadiumSnapshot(snapshot.activeStadium()),
                fromFinishSnapshot(snapshot.finishResult()),
                fromPendingReplacementSnapshot(snapshot.pendingActiveReplacement()),
                List.of()
        );
    }

    private PlayerSnapshot toPlayerSnapshot(PlayerGameState player) {
        return new PlayerSnapshot(
                player.getPlayerId().value(),
                toCardSnapshots(player.getDeck().getCards()),
                toCardSnapshots(player.getHand().getCards()),
                toCardSnapshots(player.getPrizeCards().getCards()),
                toCardSnapshots(player.getDiscardPile().getCards()),
                toBoardSnapshot(player.getBoard()),
                player.getTurnsTaken()
        );
    }

    private PlayerGameState fromPlayerSnapshot(PlayerSnapshot snapshot) {
        return new PlayerGameState(
                new PlayerId(snapshot.playerId()),
                new DeckZone(fromCardSnapshots(snapshot.deck())),
                new HandZone(fromCardSnapshots(snapshot.hand())),
                new PrizeCards(fromCardSnapshots(snapshot.prizeCards())),
                new DiscardPile(fromCardSnapshots(snapshot.discardPile())),
                fromBoardSnapshot(snapshot.board()),
                snapshot.turnsTaken()
        );
    }

    private BoardSnapshot toBoardSnapshot(BoardState board) {
        return new BoardSnapshot(
                board.getActivePokemon().map(ActivePokemon::getPokemon).map(this::toPokemonSnapshot).orElse(null),
                board.getBench().getPokemon().stream().map(this::toPokemonSnapshot).toList(),
                board.getActiveStadium().map(this::toStadiumSnapshot).orElse(null)
        );
    }

    private BoardState fromBoardSnapshot(BoardSnapshot snapshot) {
        PokemonInPlay active = snapshot.activePokemon() == null ? null : fromPokemonSnapshot(snapshot.activePokemon());
        return new BoardState(
                active == null ? null : new ActivePokemon(active),
                new Bench(snapshot.bench().stream().map(this::fromPokemonSnapshot).toList()),
                fromStadiumSnapshot(snapshot.activeStadium())
        );
    }

    private PokemonSnapshot toPokemonSnapshot(PokemonInPlay pokemon) {
        return new PokemonSnapshot(
                toCardSnapshots(pokemon.getEvolutionStack()),
                toCardSnapshots(pokemon.getAttachedCards().getEnergies()),
                pokemon.getAttachedCards().getTool().map(this::toCardSnapshot).orElse(null),
                pokemon.getEnteredTurnNumber(),
                pokemon.getLastEvolvedTurnNumber().isPresent() ? pokemon.getLastEvolvedTurnNumber().getAsInt() : null,
                pokemon.getDamageCounters(),
                toSpecialConditionSnapshot(pokemon.getSpecialConditions())
        );
    }

    private PokemonInPlay fromPokemonSnapshot(PokemonSnapshot snapshot) {
        return new PokemonInPlay(
                fromCardSnapshots(snapshot.evolutionStack()),
                new AttachedCards(fromCardSnapshots(snapshot.attachedEnergies()), fromCardSnapshot(snapshot.attachedTool())),
                snapshot.enteredTurnNumber(),
                snapshot.lastEvolvedTurnNumber(),
                snapshot.damageCounters(),
                fromSpecialConditionSnapshot(snapshot.specialConditions())
        );
    }

    private TurnSnapshot toTurnSnapshot(TurnState turn) {
        return new TurnSnapshot(
                valueOf(turn.currentPlayer()),
                valueOf(turn.startingPlayer()),
                turn.turnNumber(),
                turn.phase(),
                turn.cardDrawnThisTurn(),
                turn.energyAttachedThisTurn(),
                turn.supporterPlayedThisTurn(),
                turn.stadiumPlayedThisTurn(),
                turn.retreatedThisTurn()
        );
    }

    private TurnState fromTurnSnapshot(TurnSnapshot snapshot) {
        return new TurnState(
                playerIdOrNull(snapshot.currentPlayer()),
                playerIdOrNull(snapshot.startingPlayer()),
                snapshot.turnNumber(),
                snapshot.phase(),
                snapshot.cardDrawnThisTurn(),
                snapshot.energyAttachedThisTurn(),
                snapshot.supporterPlayedThisTurn(),
                snapshot.stadiumPlayedThisTurn(),
                snapshot.retreatedThisTurn()
        );
    }

    private StadiumSnapshot toStadiumSnapshot(StadiumInPlay stadium) {
        return new StadiumSnapshot(toCardSnapshot(stadium.card()), stadium.playedBy().value(), stadium.playedTurnNumber());
    }

    private StadiumInPlay fromStadiumSnapshot(StadiumSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new StadiumInPlay(fromCardSnapshot(snapshot.card()), new PlayerId(snapshot.playedBy()), snapshot.playedTurnNumber());
    }

    private FinishSnapshot toFinishSnapshot(GameFinishResult finish) {
        return new FinishSnapshot(finish.type(), valueOf(finish.winnerId()), valueOf(finish.loserId()), finish.reasons());
    }

    private GameFinishResult fromFinishSnapshot(FinishSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new GameFinishResult(snapshot.type(), playerIdOrNull(snapshot.winnerId()), playerIdOrNull(snapshot.loserId()), snapshot.reasons());
    }

    private PendingReplacementSnapshot toPendingReplacementSnapshot(PendingActiveReplacement pending) {
        return new PendingReplacementSnapshot(pending.playerId().value(), pending.reason());
    }

    private PendingActiveReplacement fromPendingReplacementSnapshot(PendingReplacementSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new PendingActiveReplacement(new PlayerId(snapshot.playerId()), snapshot.reason());
    }

    private SpecialConditionSnapshot toSpecialConditionSnapshot(SpecialConditionSet conditions) {
        return new SpecialConditionSnapshot(conditions.volatileCondition(), conditions.burned(), conditions.poisoned());
    }

    private SpecialConditionSet fromSpecialConditionSnapshot(SpecialConditionSnapshot snapshot) {
        if (snapshot == null) {
            return SpecialConditionSet.none();
        }
        return new SpecialConditionSet(snapshot.volatileCondition(), snapshot.burned(), snapshot.poisoned());
    }

    private List<CardSnapshot> toCardSnapshots(List<CardInstance> cards) {
        return cards.stream().map(this::toCardSnapshot).toList();
    }

    private List<CardInstance> fromCardSnapshots(List<CardSnapshot> cards) {
        return cards.stream().map(this::fromCardSnapshot).toList();
    }

    private CardSnapshot toCardSnapshot(CardInstance card) {
        return new CardSnapshot(card.id().value(), objectMapper.valueToTree(card.definition()), card.owner().value());
    }

    private CardInstance fromCardSnapshot(CardSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return new CardInstance(
                    new CardInstanceId(snapshot.id()),
                    objectMapper.treeToValue(snapshot.definition(), CardDefinitionRef.class),
                    new PlayerId(snapshot.owner())
            );
        } catch (JsonProcessingException e) {
            throw new GamePersistenceException("Could not deserialize card definition in game snapshot", e);
        }
    }

    private String valueOf(PlayerId playerId) {
        return playerId == null ? null : playerId.value();
    }

    private PlayerId playerIdOrNull(String value) {
        return value == null ? null : new PlayerId(value);
    }

    private record GameStateSnapshot(int snapshotVersion, long sequence, String gameId, GameStatus status, PlayerSnapshot playerOne, PlayerSnapshot playerTwo, TurnSnapshot turn, StadiumSnapshot activeStadium, FinishSnapshot finishResult, PendingReplacementSnapshot pendingActiveReplacement, JsonNode pendingEffectSelection) {}
    private record PlayerSnapshot(String playerId, List<CardSnapshot> deck, List<CardSnapshot> hand, List<CardSnapshot> prizeCards, List<CardSnapshot> discardPile, BoardSnapshot board, int turnsTaken) {}
    private record BoardSnapshot(PokemonSnapshot activePokemon, List<PokemonSnapshot> bench, StadiumSnapshot activeStadium) {}
    private record PokemonSnapshot(List<CardSnapshot> evolutionStack, List<CardSnapshot> attachedEnergies, CardSnapshot attachedTool, int enteredTurnNumber, Integer lastEvolvedTurnNumber, int damageCounters, SpecialConditionSnapshot specialConditions) {}
    private record CardSnapshot(String id, JsonNode definition, String owner) {}
    private record TurnSnapshot(String currentPlayer, String startingPlayer, int turnNumber, TurnPhase phase, boolean cardDrawnThisTurn, boolean energyAttachedThisTurn, boolean supporterPlayedThisTurn, boolean stadiumPlayedThisTurn, boolean retreatedThisTurn) {}
    private record StadiumSnapshot(CardSnapshot card, String playedBy, int playedTurnNumber) {}
    private record FinishSnapshot(GameFinishType type, String winnerId, String loserId, List<FinishReason> reasons) {}
    private record PendingReplacementSnapshot(String playerId, ActiveReplacementReason reason) {}
    private record SpecialConditionSnapshot(SpecialCondition volatileCondition, boolean burned, boolean poisoned) {}
}
