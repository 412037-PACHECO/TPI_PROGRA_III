package com.tpi.pokemon.game.engine.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
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
import com.tpi.pokemon.game.domain.model.StadiumInPlay;
import com.tpi.pokemon.game.domain.model.TurnState;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.GameId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.event.ActivePokemonRetreatedEvent;
import com.tpi.pokemon.game.engine.event.BasicPokemonBenchedEvent;
import com.tpi.pokemon.game.engine.event.DamageCountersPlacedEvent;
import com.tpi.pokemon.game.engine.event.EnergyAttachedEvent;
import com.tpi.pokemon.game.engine.event.GameEvent;
import com.tpi.pokemon.game.engine.event.PokemonEvolvedEvent;
import com.tpi.pokemon.game.engine.event.RetreatCostModifiedEvent;
import com.tpi.pokemon.game.engine.event.StadiumReplacedEvent;
import com.tpi.pokemon.game.engine.event.TrainerPlayedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionAppliedEvent;
import com.tpi.pokemon.game.engine.event.SpecialConditionPreventedEvent;
import com.tpi.pokemon.game.engine.effect.EffectTiming;
import com.tpi.pokemon.game.engine.effect.ability.CardEffectDefinition;
import com.tpi.pokemon.game.engine.effect.ability.EffectActivationKind;
import com.tpi.pokemon.game.engine.effect.ability.EffectCondition;
import com.tpi.pokemon.game.engine.effect.ability.EffectScope;
import com.tpi.pokemon.game.engine.effect.ability.EffectSourceKind;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierDefinition;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierLayer;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierOperation;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierTargetRole;
import com.tpi.pokemon.game.engine.effect.modifier.ModifierType;
import com.tpi.pokemon.game.engine.special.ApplySpecialConditionCommand;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TurnActionServiceTest {
    private static final GameId GAME_ID = new GameId("action-test-game");
    private static final PlayerId PLAYER_ONE = new PlayerId("player-one");
    private static final PlayerId PLAYER_TWO = new PlayerId("player-two");

    private static final CardDefinitionRef BASIC = new CardDefinitionRef("basic", "Bulbasaur", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 1);
    private static final CardDefinitionRef OTHER_BASIC = new CardDefinitionRef("other-basic", "Charmander", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 0);
    private static final CardDefinitionRef STAGE_1 = new CardDefinitionRef("stage-1", "Ivysaur", CardSupertype.POKEMON, Set.of(CardSubtype.STAGE_1), "Bulbasaur", 1);
    private static final CardDefinitionRef ENERGY = new CardDefinitionRef("grass-energy", "Grass Energy", CardSupertype.ENERGY, Set.of(CardSubtype.BASIC_ENERGY));
    private static final CardDefinitionRef ITEM = new CardDefinitionRef("item", "Potion", CardSupertype.TRAINER, Set.of(CardSubtype.ITEM));
    private static final CardDefinitionRef SUPPORTER = new CardDefinitionRef("supporter", "Professor", CardSupertype.TRAINER, Set.of(CardSubtype.SUPPORTER));
    private static final CardDefinitionRef STADIUM = new CardDefinitionRef("stadium", "Arena", CardSupertype.TRAINER, Set.of(CardSubtype.STADIUM));
    private static final CardDefinitionRef TOOL = new CardDefinitionRef("tool", "Tool", CardSupertype.TRAINER, Set.of(CardSubtype.TOOL));

    private final TurnActionService service = new TurnActionService();

    @Test
    void putBasicPokemonOnBenchMovesCardFromHandAndStoresEnteredTurnNumber() {
        CardInstance basic = card("bench-basic", BASIC, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(basic), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.putBasicPokemonOnBench(state, new PutBasicPokemonOnBenchCommand(PLAYER_ONE, basic.id()));

        assertThat(updated.getPlayerOneState().getHand().getCards()).doesNotContain(basic);
        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon()).hasSize(1);
        PokemonInPlay benched = updated.getPlayerOneState().getBoard().getBench().getPokemon().get(0);
        assertThat(benched.getTopCard()).isEqualTo(basic);
        assertThat(benched.getEnteredTurnNumber()).isEqualTo(3);
        assertThat(eventsOfType(updated, BasicPokemonBenchedEvent.class)).hasSize(1);
    }

    @Test
    void putBasicPokemonOnBenchRejectsNonBasicAndFullBench() {
        CardInstance evolution = card("stage-in-hand", STAGE_1, PLAYER_ONE);
        GameState withNonBasic = activeMainState(playerWithBoard(PLAYER_ONE, List.of(evolution), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        assertThatThrownBy(() -> service.putBasicPokemonOnBench(withNonBasic, new PutBasicPokemonOnBenchCommand(PLAYER_ONE, evolution.id())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("Basic Pokemon");

        CardInstance basic = card("extra-basic", BASIC, PLAYER_ONE);
        GameState withFullBench = activeMainState(playerWithBoard(PLAYER_ONE, List.of(basic), active("active", PLAYER_ONE), fiveBenchPokemon(), 1), emptyPlayer(PLAYER_TWO), mainTurn());
        assertThatThrownBy(() -> service.putBasicPokemonOnBench(withFullBench, new PutBasicPokemonOnBenchCommand(PLAYER_ONE, basic.id())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("Bench is full");
    }

    @Test
    void attachEnergyOnceMovesCardFromHandToTargetAndRejectsSecondAttachOrOpponentAction() {
        CardInstance energy = card("energy-1", ENERGY, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(energy), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.attachEnergy(state, new AttachEnergyCommand(PLAYER_ONE, energy.id(), PokemonTarget.active()));

        assertThat(updated.getTurnState().energyAttachedThisTurn()).isTrue();
        assertThat(updated.getPlayerOneState().getHand().getCards()).doesNotContain(energy);
        assertThat(activePokemon(updated, PLAYER_ONE).getAttachedCards().getEnergies()).containsExactly(energy);
        assertThat(eventsOfType(updated, EnergyAttachedEvent.class)).hasSize(1);

        CardInstance secondEnergy = card("energy-2", ENERGY, PLAYER_ONE);
        GameState withSecondEnergyInHand = replacePlayer(updated, playerWithBoard(PLAYER_ONE, List.of(secondEnergy), activePokemon(updated, PLAYER_ONE), List.of(), 1));
        assertThatThrownBy(() -> service.attachEnergy(withSecondEnergyInHand, new AttachEnergyCommand(PLAYER_ONE, secondEnergy.id(), PokemonTarget.active())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("already been attached");
        assertThatThrownBy(() -> service.attachEnergy(state, new AttachEnergyCommand(PLAYER_TWO, energy.id(), PokemonTarget.active())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("current player");
    }

    @Test
    void attachRainbowEnergyFromHandPlacesOneDamageCounterOnTargetPokemon() {
        CardDefinitionRef rainbowDefinition = energyDefinition("rainbow-energy", "Rainbow Energy", CardSubtype.SPECIAL_ENERGY, EnergyProfile.rainbow());
        CardInstance rainbow = card("rainbow-1", rainbowDefinition, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(rainbow), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.attachEnergy(state, new AttachEnergyCommand(PLAYER_ONE, rainbow.id(), PokemonTarget.active()));

        assertThat(activePokemon(updated, PLAYER_ONE).getAttachedCards().getEnergies()).containsExactly(rainbow);
        assertThat(activePokemon(updated, PLAYER_ONE).getDamageCounters()).isEqualTo(1);
        assertThat(eventsOfType(updated, EnergyAttachedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, DamageCountersPlacedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.countersPlaced()).isEqualTo(1);
            assertThat(event.sourceId()).isEqualTo("rainbow-energy");
        });
    }

    @Test
    void evolvePokemonKeepsAttachmentsAndRejectsFirstTurnFreshPokemonAndSecondEvolution() {
        CardInstance evolution = card("ivysaur-1", STAGE_1, PLAYER_ONE);
        CardInstance attachedEnergy = card("attached-energy", ENERGY, PLAYER_ONE);
        PokemonInPlay active = new PokemonInPlay(List.of(card("bulbasaur-1", BASIC, PLAYER_ONE)), new AttachedCards(List.of(attachedEnergy)), 1, null);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(evolution), active, List.of(), 2), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.evolvePokemon(state, new EvolvePokemonCommand(PLAYER_ONE, evolution.id(), PokemonTarget.active()));

        PokemonInPlay evolved = activePokemon(updated, PLAYER_ONE);
        assertThat(evolved.getEvolutionStack()).extracting(CardInstance::id).containsExactly(active.getTopCard().id(), evolution.id());
        assertThat(evolved.getAttachedCards().getEnergies()).containsExactly(attachedEnergy);
        assertThat(evolved.getLastEvolvedTurnNumber()).hasValue(3);
        assertThat(evolved.getSpecialConditions().hasAny()).isFalse();
        assertThat(eventsOfType(updated, PokemonEvolvedEvent.class)).hasSize(1);

        CardInstance firstTurnEvolution = card("ivysaur-first-turn", STAGE_1, PLAYER_ONE);
        GameState firstTurn = activeMainState(playerWithBoard(PLAYER_ONE, List.of(firstTurnEvolution), active, List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());
        assertThatThrownBy(() -> service.evolvePokemon(firstTurn, new EvolvePokemonCommand(PLAYER_ONE, firstTurnEvolution.id(), PokemonTarget.active())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("first turn");

        CardInstance freshEvolution = card("ivysaur-fresh", STAGE_1, PLAYER_ONE);
        GameState freshPokemon = activeMainState(playerWithBoard(PLAYER_ONE, List.of(freshEvolution), PokemonInPlay.playedThisTurn(card("fresh-basic", BASIC, PLAYER_ONE), 3), List.of(), 2), emptyPlayer(PLAYER_TWO), mainTurn());
        assertThatThrownBy(() -> service.evolvePokemon(freshPokemon, new EvolvePokemonCommand(PLAYER_ONE, freshEvolution.id(), PokemonTarget.active())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("played this turn");

        CardInstance secondEvolution = card("ivysaur-second", STAGE_1, PLAYER_ONE);
        GameState alreadyEvolved = replacePlayer(updated, playerWithBoard(PLAYER_ONE, List.of(secondEvolution), evolved, List.of(), 2));
        assertThatThrownBy(() -> service.evolvePokemon(alreadyEvolved, new EvolvePokemonCommand(PLAYER_ONE, secondEvolution.id(), PokemonTarget.active())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("already evolved");
    }

    @Test
    void retreatActivePokemonPaysKnownCostSwapsWithBenchAndRejectsSecondRetreat() {
        CardInstance energy = card("retreat-energy", ENERGY, PLAYER_ONE);
        PokemonInPlay active = new PokemonInPlay(List.of(card("retreat-active", BASIC, PLAYER_ONE)), new AttachedCards(List.of(energy)), 1, null)
                .applySpecialCondition(SpecialCondition.CONFUSED)
                .applySpecialCondition(SpecialCondition.BURNED)
                .applySpecialCondition(SpecialCondition.POISONED);
        PokemonInPlay benched = PokemonInPlay.withoutAttachments(card("retreat-bench", OTHER_BASIC, PLAYER_ONE));
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), active, List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.retreatActivePokemon(state, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of(energy.id())));

        assertThat(activePokemon(updated, PLAYER_ONE).getTopCard()).isEqualTo(benched.getTopCard());
        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon().get(0).getTopCard()).isEqualTo(active.getTopCard());
        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon().get(0).getAttachedCards().getEnergies()).isEmpty();
        assertThat(updated.getPlayerOneState().getBoard().getBench().getPokemon().get(0).getSpecialConditions().hasAny()).isFalse();
        assertThat(updated.getPlayerOneState().getDiscardPile().getCards()).containsExactly(energy);
        assertThat(updated.getTurnState().retreatedThisTurn()).isTrue();
        assertThat(eventsOfType(updated, ActivePokemonRetreatedEvent.class)).hasSize(1);

        assertThatThrownBy(() -> service.retreatActivePokemon(updated, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("already retreated");
    }

    @Test
    void retreatUsesContinuousModifierToReduceCost() {
        CardInstance energy = card("reduced-retreat-energy", ENERGY, PLAYER_ONE);
        CardDefinitionRef reducedRetreatBasic = definitionWithEffects("reduced-retreat-basic", "Bulbasaur", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 2, List.of(continuousModifier("light-step", ModifierType.RETREAT_COST, ModifierOperation.SUBTRACT, 1, ModifierLayer.AFTER_WEAKNESS_RESISTANCE)));
        PokemonInPlay active = new PokemonInPlay(List.of(card("reduced-retreat-active", reducedRetreatBasic, PLAYER_ONE)), new AttachedCards(List.of(energy)), 1, null);
        PokemonInPlay benched = PokemonInPlay.withoutAttachments(card("reduced-retreat-bench", OTHER_BASIC, PLAYER_ONE));
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), active, List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.retreatActivePokemon(state, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of(energy.id())));

        assertThat(activePokemon(updated, PLAYER_ONE).getTopCard()).isEqualTo(benched.getTopCard());
        assertThat(updated.getPlayerOneState().getDiscardPile().getCards()).containsExactly(energy);
        assertThat(eventsOfType(updated, RetreatCostModifiedEvent.class)).hasSize(1);
    }

    @Test
    void retreatCostModifierCanReduceCostToZeroWithoutDiscardingEnergy() {
        CardDefinitionRef freeRetreatBasic = definitionWithEffects("free-retreat-basic", "Bulbasaur", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 1, List.of(continuousModifier("free-step", ModifierType.RETREAT_COST, ModifierOperation.SUBTRACT, 20, ModifierLayer.AFTER_WEAKNESS_RESISTANCE)));
        PokemonInPlay active = PokemonInPlay.withoutAttachments(card("free-retreat-active", freeRetreatBasic, PLAYER_ONE));
        PokemonInPlay benched = PokemonInPlay.withoutAttachments(card("free-retreat-bench", OTHER_BASIC, PLAYER_ONE));
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), active, List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.retreatActivePokemon(state, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of()));

        assertThat(activePokemon(updated, PLAYER_ONE).getTopCard()).isEqualTo(benched.getTopCard());
        assertThat(updated.getPlayerOneState().getDiscardPile().getCards()).isEmpty();
        assertThat(eventsOfType(updated, RetreatCostModifiedEvent.class)).hasSize(1);
    }

    @Test
    void fairyGardenMakesRetreatCostZeroForPokemonWithFairyEnergy() {
        CardInstance fairyEnergy = card("fairy-energy", energyDefinition("fairy-energy-def", "Fairy Energy", CardSubtype.BASIC_ENERGY, EnergyProfile.basic(com.tpi.pokemon.game.domain.enums.EnergyType.FAIRY)), PLAYER_ONE);
        PokemonInPlay active = new PokemonInPlay(List.of(card("fairy-active", BASIC, PLAYER_ONE)), new AttachedCards(List.of(fairyEnergy)), 1, null);
        PokemonInPlay benched = PokemonInPlay.withoutAttachments(card("fairy-bench", OTHER_BASIC, PLAYER_ONE));
        CardEffectDefinition fairyGardenEffect = new CardEffectDefinition(
                "fairy-garden-free-retreat",
                "Fairy Garden",
                EffectSourceKind.STADIUM,
                EffectActivationKind.CONTINUOUS,
                EffectTiming.CONTINUOUS,
                EffectScope.ANY,
                EffectCondition.targetHasAttachedEnergyProviding(com.tpi.pokemon.game.domain.enums.EnergyType.FAIRY),
                List.of(new ModifierDefinition(ModifierType.RETREAT_COST, ModifierOperation.SET, ModifierLayer.AFTER_WEAKNESS_RESISTANCE, 0, ModifierTargetRole.DEFAULT_TARGET))
        );
        CardDefinitionRef fairyGardenDefinition = definitionWithEffects("fairy-garden", "Fairy Garden", CardSupertype.TRAINER, Set.of(CardSubtype.STADIUM), null, null, List.of(fairyGardenEffect));
        CardInstance fairyGarden = card("fairy-garden-card", fairyGardenDefinition, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), active, List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn(), new StadiumInPlay(fairyGarden, PLAYER_ONE, 3));

        GameState updated = service.retreatActivePokemon(state, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of()));

        assertThat(activePokemon(updated, PLAYER_ONE).getTopCard()).isEqualTo(benched.getTopCard());
        assertThat(updated.getPlayerOneState().getDiscardPile().getCards()).isEmpty();
        assertThat(eventsOfType(updated, RetreatCostModifiedEvent.class)).hasSize(1);
    }

    @Test
    void applySpecialConditionCanBePreventedByContinuousModifier() {
        CardDefinitionRef protectedBasic = definitionWithEffects("protected-basic", "Bulbasaur", CardSupertype.POKEMON, Set.of(CardSubtype.BASIC), null, 1, List.of(continuousModifier("sweet-veil", ModifierType.PREVENT_SPECIAL_CONDITION, ModifierOperation.PREVENT, 0, ModifierLayer.PREVENTION)));
        PokemonInPlay active = PokemonInPlay.withoutAttachments(card("protected-active", protectedBasic, PLAYER_ONE));
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), active, List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.applySpecialCondition(state, new ApplySpecialConditionCommand(PLAYER_ONE, PokemonTarget.active(), SpecialCondition.POISONED));

        assertThat(activePokemon(updated, PLAYER_ONE).hasSpecialCondition(SpecialCondition.POISONED)).isFalse();
        assertThat(eventsOfType(updated, SpecialConditionPreventedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, SpecialConditionAppliedEvent.class)).isEmpty();
    }

    @Test
    void retreatRejectsAsleepAndParalyzedActivePokemon() {
        CardInstance energy = card("status-retreat-energy", ENERGY, PLAYER_ONE);
        PokemonInPlay benched = PokemonInPlay.withoutAttachments(card("status-retreat-bench", OTHER_BASIC, PLAYER_ONE));
        GameState asleep = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), new PokemonInPlay(List.of(card("asleep-active", BASIC, PLAYER_ONE)), new AttachedCards(List.of(energy)), 1, null).applySpecialCondition(SpecialCondition.ASLEEP), List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        assertThatThrownBy(() -> service.retreatActivePokemon(asleep, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of(energy.id()))))
                .isInstanceOf(ActionException.class)
                .hasMessage("Asleep Pokemon cannot retreat");

        CardInstance paralyzedEnergy = card("paralyzed-retreat-energy", ENERGY, PLAYER_ONE);
        GameState paralyzed = activeMainState(playerWithBoard(PLAYER_ONE, List.of(), new PokemonInPlay(List.of(card("paralyzed-active", BASIC, PLAYER_ONE)), new AttachedCards(List.of(paralyzedEnergy)), 1, null).applySpecialCondition(SpecialCondition.PARALYZED), List.of(benched), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        assertThatThrownBy(() -> service.retreatActivePokemon(paralyzed, new RetreatActivePokemonCommand(PLAYER_ONE, 0, List.of(paralyzedEnergy.id()))))
                .isInstanceOf(ActionException.class)
                .hasMessage("Paralyzed Pokemon cannot retreat");
    }

    @Test
    void playItemHasNoOncePerTurnLimitButSupporterDoes() {
        CardInstance itemOne = card("item-1", ITEM, PLAYER_ONE);
        CardInstance itemTwo = card("item-2", ITEM, PLAYER_ONE);
        CardInstance supporterOne = card("supporter-1", SUPPORTER, PLAYER_ONE);
        CardInstance supporterTwo = card("supporter-2", SUPPORTER, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(itemOne, itemTwo, supporterOne, supporterTwo), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState afterFirstItem = service.playTrainer(state, new PlayTrainerCommand(PLAYER_ONE, itemOne.id()));
        GameState afterSecondItem = service.playTrainer(afterFirstItem, new PlayTrainerCommand(PLAYER_ONE, itemTwo.id()));
        assertThat(afterSecondItem.getPlayerOneState().getDiscardPile().getCards()).contains(itemOne, itemTwo);
        assertThat(afterSecondItem.getTurnState().supporterPlayedThisTurn()).isFalse();

        GameState afterSupporter = service.playTrainer(afterSecondItem, new PlayTrainerCommand(PLAYER_ONE, supporterOne.id()));
        assertThat(afterSupporter.getTurnState().supporterPlayedThisTurn()).isTrue();
        assertThat(afterSupporter.getPlayerOneState().getDiscardPile().getCards()).contains(supporterOne);
        assertThatThrownBy(() -> service.playTrainer(afterSupporter, new PlayTrainerCommand(PLAYER_ONE, supporterTwo.id())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("Supporter has already been played");
    }

    @Test
    void playStadiumIsOncePerTurnAndReplacesExistingStadium() {
        CardInstance oldStadium = card("old-stadium", STADIUM, PLAYER_ONE);
        CardInstance newStadium = card("new-stadium", STADIUM, PLAYER_ONE);
        CardInstance anotherStadium = card("another-stadium", STADIUM, PLAYER_ONE);
        GameState state = activeMainState(
                playerWithBoard(PLAYER_ONE, List.of(newStadium, anotherStadium), active("active", PLAYER_ONE), List.of(), 1),
                emptyPlayer(PLAYER_TWO),
                mainTurn(),
                new StadiumInPlay(oldStadium, PLAYER_ONE, 2)
        );

        GameState updated = service.playTrainer(state, new PlayTrainerCommand(PLAYER_ONE, newStadium.id()));

        assertThat(updated.getActiveStadium()).get().extracting(StadiumInPlay::card).isEqualTo(newStadium);
        assertThat(updated.getPlayerOneState().getDiscardPile().getCards()).containsExactly(oldStadium);
        assertThat(updated.getTurnState().stadiumPlayedThisTurn()).isTrue();
        assertThat(eventsOfType(updated, StadiumReplacedEvent.class)).hasSize(1);
        assertThat(eventsOfType(updated, TrainerPlayedEvent.class)).hasSize(1);
        assertThatThrownBy(() -> service.playTrainer(updated, new PlayTrainerCommand(PLAYER_ONE, anotherStadium.id())))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("Stadium has already been played");
    }

    @Test
    void playToolAttachesToTargetAndRejectsSecondTool() {
        CardInstance toolOne = card("tool-1", TOOL, PLAYER_ONE);
        CardInstance toolTwo = card("tool-2", TOOL, PLAYER_ONE);
        GameState state = activeMainState(playerWithBoard(PLAYER_ONE, List.of(toolOne, toolTwo), active("active", PLAYER_ONE), List.of(), 1), emptyPlayer(PLAYER_TWO), mainTurn());

        GameState updated = service.playTrainer(state, new PlayTrainerCommand(PLAYER_ONE, toolOne.id(), Optional.of(PokemonTarget.active())));

        assertThat(activePokemon(updated, PLAYER_ONE).getAttachedCards().getTool()).contains(toolOne);
        assertThat(updated.getPlayerOneState().getHand().getCards()).doesNotContain(toolOne);
        assertThatThrownBy(() -> service.playTrainer(updated, new PlayTrainerCommand(PLAYER_ONE, toolTwo.id(), Optional.of(PokemonTarget.active()))))
                .isInstanceOf(ActionException.class)
                .hasMessageContaining("already has a tool");
    }

    private GameState activeMainState(PlayerGameState playerOne, PlayerGameState playerTwo, TurnState turnState) {
        return activeMainState(playerOne, playerTwo, turnState, null);
    }

    private GameState activeMainState(PlayerGameState playerOne, PlayerGameState playerTwo, TurnState turnState, StadiumInPlay stadium) {
        return new GameState(GAME_ID, GameStatus.ACTIVE, playerOne, playerTwo, turnState, stadium, List.of());
    }

    private TurnState mainTurn() {
        return new TurnState(PLAYER_ONE, PLAYER_ONE, 3, TurnPhase.MAIN, false, false, false, false, false);
    }

    private PlayerGameState playerWithBoard(PlayerId playerId, List<CardInstance> hand, PokemonInPlay active, List<PokemonInPlay> bench, int turnsTaken) {
        return new PlayerGameState(playerId, DeckZone.empty(), new HandZone(hand), PrizeCards.empty(), DiscardPile.empty(), new BoardState(new ActivePokemon(active), new Bench(bench)), turnsTaken);
    }

    private PlayerGameState emptyPlayer(PlayerId playerId) {
        return PlayerGameState.empty(playerId);
    }

    private PokemonInPlay active(String id, PlayerId owner) {
        return PokemonInPlay.withoutAttachments(card(id, BASIC, owner));
    }

    private List<PokemonInPlay> fiveBenchPokemon() {
        return List.of(
                PokemonInPlay.withoutAttachments(card("bench-1", BASIC, PLAYER_ONE)),
                PokemonInPlay.withoutAttachments(card("bench-2", BASIC, PLAYER_ONE)),
                PokemonInPlay.withoutAttachments(card("bench-3", BASIC, PLAYER_ONE)),
                PokemonInPlay.withoutAttachments(card("bench-4", BASIC, PLAYER_ONE)),
                PokemonInPlay.withoutAttachments(card("bench-5", BASIC, PLAYER_ONE))
        );
    }

    private CardInstance card(String id, CardDefinitionRef definition, PlayerId owner) {
        return new CardInstance(new CardInstanceId(id), definition, owner);
    }

    private CardDefinitionRef definitionWithEffects(String id, String name, CardSupertype supertype, Set<CardSubtype> subtypes, String evolvesFrom, Integer retreatCost, List<CardEffectDefinition> effects) {
        return new CardDefinitionRef(id, name, supertype, subtypes, evolvesFrom, retreatCost, null, List.of(), List.of(), List.of(), List.of(), com.tpi.pokemon.game.domain.model.EnergyProfile.none(), effects);
    }

    private CardDefinitionRef energyDefinition(String id, String name, CardSubtype subtype, EnergyProfile profile) {
        return new CardDefinitionRef(id, name, CardSupertype.ENERGY, Set.of(subtype), null, null, null, List.of(), List.of(), List.of(), List.of(), profile);
    }

    private CardEffectDefinition continuousModifier(String effectId, ModifierType type, ModifierOperation operation, int amount, ModifierLayer layer) {
        return new CardEffectDefinition(
                effectId,
                "Continuous modifier",
                EffectSourceKind.POKEMON_ABILITY,
                EffectActivationKind.CONTINUOUS,
                EffectTiming.CONTINUOUS,
                EffectScope.SELF,
                EffectCondition.always(),
                List.of(new ModifierDefinition(type, operation, layer, amount, ModifierTargetRole.DEFAULT_TARGET))
        );
    }

    private PokemonInPlay activePokemon(GameState state, PlayerId playerId) {
        PlayerGameState player = playerId.equals(PLAYER_ONE) ? state.getPlayerOneState() : state.getPlayerTwoState();
        return player.getBoard().getActivePokemon().orElseThrow().getPokemon();
    }

    private GameState replacePlayer(GameState state, PlayerGameState player) {
        PlayerGameState playerOne = PLAYER_ONE.equals(player.getPlayerId()) ? player : state.getPlayerOneState();
        PlayerGameState playerTwo = PLAYER_TWO.equals(player.getPlayerId()) ? player : state.getPlayerTwoState();
        return new GameState(state.getGameId(), state.getStatus(), playerOne, playerTwo, state.getTurnState(), state.getActiveStadium().orElse(null), state.getEvents());
    }

    private <T extends GameEvent> List<T> eventsOfType(GameState state, Class<T> type) {
        return state.getEvents().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
