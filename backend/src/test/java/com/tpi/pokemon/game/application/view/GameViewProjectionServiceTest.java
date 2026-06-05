package com.tpi.pokemon.game.application.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.application.UnauthorizedGameViewException;
import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
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
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.GameState;
import com.tpi.pokemon.game.domain.model.HandZone;
import com.tpi.pokemon.game.domain.model.PlayerGameState;
import com.tpi.pokemon.game.domain.model.PokemonInPlay;
import com.tpi.pokemon.game.domain.model.PrizeCards;
import com.tpi.pokemon.game.domain.model.SpecialConditionSet;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.CardFilterSpec;
import com.tpi.pokemon.game.engine.effect.EffectCardZone;
import com.tpi.pokemon.game.engine.effect.EffectTarget;
import com.tpi.pokemon.game.engine.effect.EffectType;
import com.tpi.pokemon.game.engine.effect.PendingEffectSelection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameViewProjectionServiceTest {
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    private final GameViewProjectionService service = new GameViewProjectionService();

    @Test
    void viewerSeesOwnPrivateZonesButNotDeckOrderOrPrizeCards() {
        GameViewResponse view = service.project(state(), PLAYER_ONE, null);

        assertThat(view.player().hand().cards()).extracting(CardView::instanceId).containsExactly("p1-hand-1", "p1-hand-2");
        assertThat(view.player().deck().count()).isEqualTo(2);
        assertThat(view.player().deck().orderVisible()).isFalse();
        assertThat(view.player().deck().cards()).isEmpty();
        assertThat(view.player().prizeCards().remainingCount()).isEqualTo(2);
        assertThat(view.player().prizeCards().cardsVisible()).isFalse();
        assertThat(view.player().prizeCards().cards()).isEmpty();
    }

    @Test
    void viewerDoesNotSeeOpponentHandDeckOrderOrPrizeCardsButSeesPublicBoardAndDiscard() {
        GameViewResponse view = service.project(state(), PLAYER_ONE, null);

        assertThat(view.opponent().hand().count()).isEqualTo(2);
        assertThat(view.opponent().hand().cards()).isEmpty();
        assertThat(view.opponent().deck().count()).isEqualTo(2);
        assertThat(view.opponent().deck().cards()).isEmpty();
        assertThat(view.opponent().prizeCards().remainingCount()).isEqualTo(2);
        assertThat(view.opponent().prizeCards().cards()).isEmpty();
        assertThat(view.opponent().board().activePokemon().topCard().instanceId()).isEqualTo("p2-active");
        assertThat(view.opponent().board().activePokemon().damageCounters()).isEqualTo(3);
        assertThat(view.opponent().board().activePokemon().specialConditions()).containsExactlyInAnyOrder("BURNED", "POISONED");
        assertThat(view.opponent().board().activePokemon().attachedEnergies()).hasSize(1);
        assertThat(view.opponent().discardPile().cards()).extracting(CardView::instanceId).containsExactly("p2-discard-1");
    }

    @Test
    void pendingSelectionShowsPrivateCandidatesOnlyToAuthorizedPlayer() {
        PendingEffectSelection pending = new PendingEffectSelection(
                PLAYER_ONE,
                EffectType.SEARCH_DECK,
                "xy1-123",
                EffectCardZone.DECK,
                EffectTarget.ACTING_PLAYER,
                1,
                2,
                CardFilterSpec.subtype(CardSubtype.BASIC_ENERGY),
                true,
                true,
                null,
                List.of(new CardInstanceId("p1-deck-1"), new CardInstanceId("p1-deck-2"))
        );

        GameViewResponse ownerView = service.project(state(), PLAYER_ONE, pending);
        GameViewResponse rivalView = service.project(state(), PLAYER_TWO, pending);

        assertThat(ownerView.pendingSelection().pending()).isTrue();
        assertThat(ownerView.pendingSelection().viewerMustChoose()).isTrue();
        assertThat(ownerView.pendingSelection().candidateCardIds()).containsExactly("p1-deck-1", "p1-deck-2");
        assertThat(ownerView.pendingSelection().revealSelectedCards()).isTrue();
        assertThat(rivalView.pendingSelection().pending()).isTrue();
        assertThat(rivalView.pendingSelection().viewerMustChoose()).isFalse();
        assertThat(rivalView.pendingSelection().candidateCardIds()).isEmpty();
        assertThat(rivalView.pendingSelection().revealSelectedCards()).isTrue();
    }

    @Test
    void rejectsViewerOutsideGame() {
        assertThatThrownBy(() -> service.project(state(), new PlayerId("intruder"), null))
                .isInstanceOf(UnauthorizedGameViewException.class)
                .hasMessage("Player intruder is not allowed to view game safe-view-game");
    }

    private GameState state() {
        PlayerGameState playerOne = new PlayerGameState(
                PLAYER_ONE,
                new DeckZone(List.of(pokemon("p1-deck-1", PLAYER_ONE), pokemon("p1-deck-2", PLAYER_ONE))),
                new HandZone(List.of(pokemon("p1-hand-1", PLAYER_ONE), energy("p1-hand-2", PLAYER_ONE))),
                new PrizeCards(List.of(pokemon("p1-prize-1", PLAYER_ONE), pokemon("p1-prize-2", PLAYER_ONE))),
                new DiscardPile(List.of(pokemon("p1-discard-1", PLAYER_ONE))),
                new BoardState(new ActivePokemon(PokemonInPlay.withoutAttachments(pokemon("p1-active", PLAYER_ONE))), new Bench(List.of(PokemonInPlay.withoutAttachments(pokemon("p1-bench-1", PLAYER_ONE))))),
                1
        );
        PokemonInPlay opponentActive = new PokemonInPlay(
                pokemon("p2-active", PLAYER_TWO),
                new AttachedCards(List.of(energy("p2-energy", PLAYER_TWO)))
        ).withDamageCounters(3).withSpecialConditions(SpecialConditionSet.none().apply(SpecialCondition.BURNED).apply(SpecialCondition.POISONED));
        PlayerGameState playerTwo = new PlayerGameState(
                PLAYER_TWO,
                new DeckZone(List.of(pokemon("p2-deck-1", PLAYER_TWO), pokemon("p2-deck-2", PLAYER_TWO))),
                new HandZone(List.of(pokemon("p2-hand-1", PLAYER_TWO), pokemon("p2-hand-2", PLAYER_TWO))),
                new PrizeCards(List.of(pokemon("p2-prize-1", PLAYER_TWO), pokemon("p2-prize-2", PLAYER_TWO))),
                new DiscardPile(List.of(pokemon("p2-discard-1", PLAYER_TWO))),
                new BoardState(new ActivePokemon(opponentActive), new Bench(List.of(PokemonInPlay.withoutAttachments(pokemon("p2-bench-1", PLAYER_TWO))))),
                2
        );
        return new GameState(new GameId("safe-view-game"), GameStatus.ACTIVE, playerOne, playerTwo, new TurnState(PLAYER_ONE, PLAYER_ONE, 4, TurnPhase.MAIN, true, false, false, false, false), List.of());
    }

    private CardInstance pokemon(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Pokemon " + id, CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 1, 60, List.of(), List.of(), List.of(), List.of(), EnergyProfile.none()), owner);
    }

    private CardInstance energy(String id, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), new CardDefinitionRef(id + "-def", "Energy " + id, CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY), null, null, null, List.of(), List.of(), List.of(), List.of(), EnergyProfile.basic(EnergyType.GRASS)), owner);
    }
}
