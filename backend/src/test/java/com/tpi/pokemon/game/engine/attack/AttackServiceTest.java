package com.tpi.pokemon.game.engine.attack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.GameStatus;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.enums.SpecialCondition;
import com.tpi.pokemon.game.domain.enums.TurnPhase;
import com.tpi.pokemon.game.domain.model.ActivePokemon;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
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
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.AttackDeclaredEvent;
import com.tpi.pokemon.game.engine.event.AttackResolvedEvent;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacedEvent;
import com.tpi.pokemon.game.engine.event.ActivePokemonReplacementRequiredEvent;
import com.tpi.pokemon.game.engine.event.GameFinishedEvent;
import com.tpi.pokemon.game.engine.event.DamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.DamageCalculatedEvent;
import com.tpi.pokemon.game.engine.event.EnergyCostValidatedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PokemonKnockedOutEvent;
import com.tpi.pokemon.game.engine.event.PrizeCardsTakenEvent;
import com.tpi.pokemon.game.engine.event.ConfusionCheckResolvedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionDamageAppliedEvent;
import com.tpi.pokemon.game.engine.event.TurnEndedEvent;
import com.tpi.pokemon.game.engine.random.CoinFlipResult;
import com.tpi.pokemon.game.engine.special.StatusEffectManager;
import com.tpi.pokemon.game.engine.knockout.ActivePokemonReplacementResolver;
import com.tpi.pokemon.game.engine.knockout.ReplaceActivePokemonCommand;
import com.tpi.pokemon.game.engine.victory.FinishReason;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttackServiceTest {
    private static final GameId GAME_ID = new GameId("attack-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");
    private static final AttackDefinition SCRATCH = new AttackDefinition("scratch", "Scratch", List.of(EnergyType.COLORLESS), 30);
    private static final AttackDefinition FIRE_BLAST = new AttackDefinition("fire-blast", "Fire Blast", List.of(EnergyType.FIRE), 40);
    private static final AttackDefinition KNOCKOUT_HIT = new AttackDefinition("knockout-hit", "Knockout Hit", List.of(EnergyType.COLORLESS), 60);

    private final AttackService attackService = new AttackService();

    @Test
    void appliesBaseDamageToOpposingActivePokemon() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        PokemonInPlay defender = activePokemon(result.getPlayerTwoState());
        assertThat(defender.getDamageCounters()).isEqualTo(3);
    }

    @Test
    void acceptsAttackWithExactSpecificEnergyCost() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(FIRE_BLAST), List.of(energy("p1-fire", PLAYER_ONE, EnergyType.FIRE))), activeDefender()),
                command("fire-blast")
        );

        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isEqualTo(4);
    }

    @Test
    void accumulatesDamageCountersOverExistingDamage() {
        PokemonInPlay damagedDefender = activeDefender().withDamageCounters(2);

        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), damagedDefender),
                command("scratch")
        );

        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isEqualTo(5);
    }

    @Test
    void endsTurnAutomaticallyAfterAttackResolution() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        assertThat(result.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(result.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
    }

    @Test
    void appendsMainAttackAndTurnEndEvents() {
        GameState result = attackService.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))), activeDefender()),
                command("scratch")
        );

        assertThat(eventsOfType(result, AttackDeclaredEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, EnergyCostValidatedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, DamageCalculatedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, DamageAppliedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, AttackResolvedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, TurnEndedEvent.class)).hasSize(1);
    }

    @Test
    void rejectsAttackWithoutEnoughEnergy() {
        GameState state = activeGame(
                activeAttacker(List.of(FIRE_BLAST), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("fire-blast")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Not enough energy to declare attack");
    }

    @Test
    void rejectsAttackWhenPlayerIsNotCurrentPlayer() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, new DeclareAttackCommand(GAME_ID, PLAYER_TWO, "scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Only the current player can attack");
    }

    @Test
    void rejectsAttackOutsideMainPhase() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender(),
                new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.DRAW, false, false, false, false, false)
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attack can only be declared from MAIN phase");
    }

    @Test
    void rejectsAttackWithoutActiveAttacker() {
        GameState state = activeGame(player(PLAYER_ONE, BoardState.empty()), playerWithActive(PLAYER_TWO, activeDefender()), defaultTurn());

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attacking player has no active Pokemon");
    }

    @Test
    void rejectsAttackWithoutActiveDefender() {
        GameState state = activeGame(
                playerWithActive(PLAYER_ONE, activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)))),
                player(PLAYER_TWO, BoardState.empty()),
                defaultTurn()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Defending player has no active Pokemon");
    }

    @Test
    void rejectsAttackThatDoesNotBelongToActivePokemon() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("missing-attack")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Attack does not exist on active Pokemon");
    }

    @Test
    void rejectsStartingPlayerAttackOnTheirFirstTurn() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))),
                activeDefender(),
                new TurnState(PLAYER_ONE, PLAYER_ONE, 1, TurnPhase.MAIN, false, false, false, false, false)
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Starting player cannot attack on their first turn");
    }

    @Test
    void rejectsAttackWhenActivePokemonIsAsleep() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))).applySpecialCondition(SpecialCondition.ASLEEP),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Asleep Pokemon cannot attack");
    }

    @Test
    void rejectsAttackWhenActivePokemonIsParalyzed() {
        GameState state = activeGame(
                activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))).applySpecialCondition(SpecialCondition.PARALYZED),
                activeDefender()
        );

        assertThatThrownBy(() -> attackService.declareAttack(state, command("scratch")))
                .isInstanceOf(AttackException.class)
                .hasMessage("Paralyzed Pokemon cannot attack");
    }

    @Test
    void confusedAttackWithHeadsResolvesNormally() {
        AttackService service = attackServiceWithCoin(CoinFlipResult.HEADS);
        GameState result = service.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))).applySpecialCondition(SpecialCondition.CONFUSED), activeDefender()),
                command("scratch")
        );

        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isEqualTo(3);
        assertThat(activePokemon(result.getPlayerOneState()).getDamageCounters()).isZero();
        assertThat(eventsOfType(result, ConfusionCheckResolvedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.result()).isEqualTo(CoinFlipResult.HEADS));
    }

    @Test
    void confusedAttackWithTailsFailsAndDamagesAttacker() {
        AttackService service = attackServiceWithCoin(CoinFlipResult.TAILS);
        GameState result = service.declareAttack(
                activeGame(activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER))).applySpecialCondition(SpecialCondition.CONFUSED), activeDefender()),
                command("scratch")
        );

        assertThat(activePokemon(result.getPlayerOneState()).getDamageCounters()).isEqualTo(3);
        assertThat(activePokemon(result.getPlayerTwoState()).getDamageCounters()).isZero();
        assertThat(eventsOfType(result, SpecialConditionDamageAppliedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, DamageCalculatedEvent.class)).isEmpty();
        assertThat(eventsOfType(result, TurnEndedEvent.class)).hasSize(1);
    }

    @Test
    void confusedSelfDamageCanKnockOutAttackerAndAwardPrizeToOpponent() {
        AttackService service = attackServiceWithCoin(CoinFlipResult.TAILS);
        PokemonInPlay confusedAttacker = activeAttacker(List.of(SCRATCH), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)))
                .withDamageCounters(3)
                .applySpecialCondition(SpecialCondition.CONFUSED);
        GameState state = activeGame(
                playerWithActiveAndPrizes(PLAYER_ONE, confusedAttacker, prizes(PLAYER_ONE, 6)),
                playerWithActiveAndPrizes(PLAYER_TWO, activeDefender(), prizes(PLAYER_TWO, 6)),
                defaultTurn()
        );

        GameState result = service.declareAttack(state, command("scratch"));

        assertThat(result.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.getPlayerOneState().getBoard().getActivePokemon()).isEmpty();
        assertThat(result.getPlayerOneState().getDiscardPile().getCards()).extracting(CardInstance::id).contains(new CardInstanceId("p1-active"));
        assertThat(result.getPlayerTwoState().getPrizeCards().remainingCount()).isEqualTo(5);
        assertThat(result.getFinishResult()).hasValueSatisfying(finish -> assertThat(finish.winnerId()).isEqualTo(PLAYER_TWO));
    }

    @Test
    void attackThatKnocksOutDefenderDiscardsDefenderTakesPrizeAndRequiresReplacementWhenBenchExists() {
        PokemonInPlay attacker = activeAttacker(List.of(KNOCKOUT_HIT), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)));
        PokemonInPlay defender = activeDefender();
        PokemonInPlay benched = benchPokemon("p2-bench", PLAYER_TWO);
        GameState state = activeGame(
                playerWithActiveAndPrizes(PLAYER_ONE, attacker, prizes(PLAYER_ONE, 6)),
                playerWithActiveBenchAndPrizes(PLAYER_TWO, defender, List.of(benched), prizes(PLAYER_TWO, 6)),
                defaultTurn()
        );

        GameState result = attackService.declareAttack(state, command("knockout-hit"));

        assertThat(result.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.getPlayerTwoState().getBoard().getActivePokemon()).isEmpty();
        assertThat(result.getPendingActiveReplacement()).hasValueSatisfying(pending -> assertThat(pending.playerId()).isEqualTo(PLAYER_TWO));
        assertThat(result.getPlayerTwoState().getDiscardPile().getCards()).extracting(CardInstance::id).contains(new CardInstanceId("p2-active"));
        assertThat(result.getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(5);
        assertThat(eventsOfType(result, PokemonKnockedOutEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, PrizeCardsTakenEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, ActivePokemonReplacementRequiredEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, TurnEndedEvent.class)).isEmpty();
    }

    @Test
    void attackThatKnocksOutPokemonExTakesTwoPrizes() {
        PokemonInPlay attacker = activeAttacker(List.of(KNOCKOUT_HIT), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)));
        PokemonInPlay exDefender = activeDefender(Set.of(CardSubtype.BASIC, CardSubtype.EX));
        GameState state = activeGame(
                playerWithActiveAndPrizes(PLAYER_ONE, attacker, prizes(PLAYER_ONE, 6)),
                playerWithActiveBenchAndPrizes(PLAYER_TWO, exDefender, List.of(benchPokemon("p2-bench", PLAYER_TWO)), prizes(PLAYER_TWO, 6)),
                defaultTurn()
        );

        GameState result = attackService.declareAttack(state, command("knockout-hit"));

        assertThat(result.getPlayerOneState().getPrizeCards().remainingCount()).isEqualTo(4);
        assertThat(eventsOfType(result, PrizeCardsTakenEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.prizeCardIds()).hasSize(2));
    }

    @Test
    void attackThatTakesLastPrizeWinsImmediately() {
        PokemonInPlay attacker = activeAttacker(List.of(KNOCKOUT_HIT), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)));
        GameState state = activeGame(
                playerWithActiveAndPrizes(PLAYER_ONE, attacker, prizes(PLAYER_ONE, 1)),
                playerWithActiveBenchAndPrizes(PLAYER_TWO, activeDefender(), List.of(benchPokemon("p2-bench", PLAYER_TWO)), prizes(PLAYER_TWO, 6)),
                defaultTurn()
        );

        GameState result = attackService.declareAttack(state, command("knockout-hit"));

        assertThat(result.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.getFinishResult()).hasValueSatisfying(finish -> {
            assertThat(finish.winnerId()).isEqualTo(PLAYER_ONE);
            assertThat(finish.reasons()).contains(FinishReason.PRIZES_TAKEN);
        });
        assertThat(eventsOfType(result, GameFinishedEvent.class)).hasSize(1);
        assertThat(result.getPendingActiveReplacement()).isEmpty();
        assertThat(eventsOfType(result, TurnEndedEvent.class)).isEmpty();
    }

    @Test
    void attackThatKnocksOutLastPokemonWinsImmediately() {
        PokemonInPlay attacker = activeAttacker(List.of(KNOCKOUT_HIT), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)));
        GameState state = activeGame(
                playerWithActiveAndPrizes(PLAYER_ONE, attacker, prizes(PLAYER_ONE, 6)),
                playerWithActiveAndPrizes(PLAYER_TWO, activeDefender(), prizes(PLAYER_TWO, 6)),
                defaultTurn()
        );

        GameState result = attackService.declareAttack(state, command("knockout-hit"));

        assertThat(result.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.getFinishResult()).hasValueSatisfying(finish -> {
            assertThat(finish.winnerId()).isEqualTo(PLAYER_ONE);
            assertThat(finish.loserId()).isEqualTo(PLAYER_TWO);
            assertThat(finish.reasons()).contains(FinishReason.OPPONENT_HAS_NO_POKEMON_IN_PLAY);
        });
        assertThat(eventsOfType(result, GameFinishedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, TurnEndedEvent.class)).isEmpty();
    }

    @Test
    void replacingActiveFromBenchClearsPendingReplacementAndEndsTurn() {
        PokemonInPlay attacker = activeAttacker(List.of(KNOCKOUT_HIT), List.of(energy("p1-water", PLAYER_ONE, EnergyType.WATER)));
        PokemonInPlay benched = benchPokemon("p2-bench", PLAYER_TWO);
        GameState pending = attackService.declareAttack(
                activeGame(
                        playerWithActiveAndPrizes(PLAYER_ONE, attacker, prizes(PLAYER_ONE, 6)),
                        playerWithActiveBenchAndPrizes(PLAYER_TWO, activeDefender(), List.of(benched), prizes(PLAYER_TWO, 6)),
                        defaultTurn()
                ),
                command("knockout-hit")
        );

        GameState result = new ActivePokemonReplacementResolver().replaceActive(pending, new ReplaceActivePokemonCommand(PLAYER_TWO, 0));

        assertThat(result.getPendingActiveReplacement()).isEmpty();
        assertThat(activePokemon(result.getPlayerTwoState()).getTopCard().id()).isEqualTo(new CardInstanceId("p2-bench"));
        assertThat(result.getTurnState().currentPlayer()).isEqualTo(PLAYER_TWO);
        assertThat(result.getTurnState().phase()).isEqualTo(TurnPhase.NOT_STARTED);
        assertThat(eventsOfType(result, ActivePokemonReplacedEvent.class)).hasSize(1);
        assertThat(eventsOfType(result, TurnEndedEvent.class)).hasSize(1);
    }

    private GameState activeGame(PokemonInPlay attacker, PokemonInPlay defender) {
        return activeGame(attacker, defender, defaultTurn());
    }

    private GameState activeGame(PokemonInPlay attacker, PokemonInPlay defender, TurnState turnState) {
        return activeGame(playerWithActive(PLAYER_ONE, attacker), playerWithActive(PLAYER_TWO, defender), turnState);
    }

    private GameState activeGame(PlayerGameState playerOne, PlayerGameState playerTwo, TurnState turnState) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, turnState, List.of());
    }

    private TurnState defaultTurn() {
        return new TurnState(PLAYER_ONE, PLAYER_ONE, 2, TurnPhase.MAIN, false, false, false, false, false);
    }

    private PlayerGameState playerWithActive(PlayerId playerId, PokemonInPlay active) {
        return player(playerId, new BoardState(new ActivePokemon(active), Bench.empty()));
    }

    private PlayerGameState playerWithActiveAndPrizes(PlayerId playerId, PokemonInPlay active, List<CardInstance> prizes) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), new PrizeCards(prizes), DiscardPile.empty(), new BoardState(new ActivePokemon(active), Bench.empty()), 1);
    }

    private PlayerGameState playerWithActiveBenchAndPrizes(PlayerId playerId, PokemonInPlay active, List<PokemonInPlay> bench, List<CardInstance> prizes) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), new PrizeCards(prizes), DiscardPile.empty(), new BoardState(new ActivePokemon(active), new Bench(bench)), 1);
    }

    private PlayerGameState player(PlayerId playerId, BoardState board) {
        return new PlayerGameState(playerId, DeckZone.empty(), HandZone.empty(), PrizeCards.empty(), DiscardPile.empty(), board, 1);
    }

    private PokemonInPlay activeAttacker(List<AttackDefinition> attacks, List<CardInstance> energies) {
        return new PokemonInPlay(
                card("p1-active", PLAYER_ONE, pokemonDefinition("p1-active-def", PokemonType.FIRE, attacks, List.of(), List.of())),
                new AttachedCards(energies)
        );
    }

    private PokemonInPlay activeDefender() {
        return activeDefender(Set.of(CardSubtype.BASIC));
    }

    private PokemonInPlay activeDefender(Set<CardSubtype> subtypes) {
        return PokemonInPlay.withoutAttachments(
                card("p2-active", PLAYER_TWO, pokemonDefinition("p2-active-def", PokemonType.GRASS, List.of(), List.of(), List.of(), subtypes))
        );
    }

    private PokemonInPlay benchPokemon(String id, PlayerId owner) {
        return PokemonInPlay.withoutAttachments(card(id, owner, pokemonDefinition(id + "-def", PokemonType.GRASS, List.of(), List.of(), List.of())));
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances) {
        return pokemonDefinition(id, type, attacks, weaknesses, resistances, Set.of(CardSubtype.BASIC));
    }

    private CardDefinitionRef pokemonDefinition(String id, PokemonType type, List<AttackDefinition> attacks, List<Weakness> weaknesses, List<Resistance> resistances, Set<CardSubtype> subtypes) {
        return new CardDefinitionRef(
                id,
                "Pokemon " + id,
                CardSupertype.POKEMON,
                subtypes,
                null,
                1,
                60,
                List.of(type),
                attacks,
                weaknesses,
                resistances,
                EnergyProfile.none()
        );
    }

    private CardInstance energy(String id, PlayerId owner, EnergyType type) {
        CardDefinitionRef definition = new CardDefinitionRef(
                id + "-def",
                type + " Energy",
                CardSupertype.ENERGY,
                Set.of(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                EnergyProfile.basic(type)
        );
        return card(id, owner, definition);
    }

    private CardInstance card(String id, PlayerId owner, CardDefinitionRef definition) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }

    private List<CardInstance> prizes(PlayerId owner, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> card(owner.value() + "-prize-" + index, owner, new CardDefinitionRef(owner.value() + "-prize-def-" + index, "Prize " + index)))
                .toList();
    }

    private DeclareAttackCommand command(String attackId) {
        return new DeclareAttackCommand(GAME_ID, PLAYER_ONE, attackId);
    }

    private AttackService attackServiceWithCoin(CoinFlipResult result) {
        return new AttackService(new com.tpi.pokemon.game.engine.turn.TurnManager(), new EnergyCostValidator(), new com.tpi.pokemon.game.engine.damage.DamageCalculator(), new com.tpi.pokemon.game.engine.knockout.PostAttackResolutionService(), new StatusEffectManager(), () -> result);
    }

    private PokemonInPlay activePokemon(PlayerGameState player) {
        return player.getBoard().getActivePokemon().orElseThrow().getPokemon();
    }

    private <T extends GameEvent> List<T> eventsOfType(GameState state, Class<T> type) {
        return state.getEvents().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
