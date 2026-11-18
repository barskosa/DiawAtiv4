package com.pokedex.api.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleAction;
import com.pokedex.api.model.BattleEvent;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.model.BattleSide;
import com.pokedex.api.model.Pokemon;

/**
 * Regras do desafio contra treinadores com aleatoriedade controlada (sem crítico, variação 1,00).
 * Os danos esperados foram calculados à mão pela fórmula ⌊⌊22 × poder × A / D⌋ / 50⌋ + 2.
 */
class BattleEngineTest {

    private static final Pokemon PIKACHU = new Pokemon(25, "Pikachu", 35, 55, 30, 50, 40, 90, List.of("Electric"));
    private static final Pokemon SQUIRTLE = new Pokemon(7, "Squirtle", 44, 48, 65, 60, 54, 43, List.of("Water"));
    private static final Pokemon GEODUDE = new Pokemon(74, "Geodude", 40, 80, 100, 30, 30, 20, List.of("Rock", "Ground"));
    private static final Pokemon ONIX = new Pokemon(95, "Onix", 35, 45, 160, 30, 45, 70, List.of("Rock", "Ground"));
    private static final Pokemon MEW = new Pokemon(151, "Mew", 100, 100, 100, 100, 100, 100, List.of());

    /** Gerador previsível: todo nextInt devolve {@code roll} (crítico só com 0), nextDouble 1,0. */
    private static RandomGenerator fixed(int roll, boolean coin) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return roll;
            }

            @Override
            public double nextDouble() {
                return 1.0;
            }

            @Override
            public boolean nextBoolean() {
                return coin;
            }
        };
    }

    private static BattleEngine engine(RandomGenerator random) {
        return new BattleEngine(random, new TrainerAi(random));
    }

    /** Ana (Pikachu, Squirtle) desafia Brock (Geodude, Onix). */
    private static Battle brockChallenge(BattleEngine engine, Pokemon... playerTeam) {
        Battle battle = new Battle();
        battle.setPlayerName("Ana");
        battle.setTrainer(new Battle.TrainerInfo(2, "Brock", 1));
        battle.setPlayerTeam(List.of(playerTeam).stream().map(BattleFighter::of).toList());
        battle.setTrainerTeam(List.of(BattleFighter.of(GEODUDE), BattleFighter.of(ONIX)));
        engine.start(battle);
        return battle;
    }

    private final BattleEngine engine = engine(fixed(1, false));

    @Test
    void startsBothTeamsWithFullHp() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        assertThat(BattleEngine.battleHp(35)).isEqualTo(95);
        assertThat(battle.getPlayerTeam()).extracting(BattleFighter::getCurrentHp).containsExactly(95, 104);
        assertThat(battle.getTrainerTeam()).extracting(BattleFighter::getMaxHp).containsExactly(100, 95);
        assertThat(battle.active(BattleSide.PLAYER).getPokemonName()).isEqualTo("Pikachu");
        assertThat(battle.active(BattleSide.TRAINER).getPokemonName()).isEqualTo("Geodude");
    }

    @Test
    void typeChartCombinesBothDefenderTypes() {
        assertThat(TypeChart.effectiveness("Electric", List.of("Water"))).isEqualTo(2.0);
        assertThat(TypeChart.effectiveness("Electric", List.of("Rock", "Ground"))).isZero();
        assertThat(TypeChart.effectiveness("Ice", List.of("Dragon", "Flying"))).isEqualTo(4.0);
        assertThat(TypeChart.effectiveness("Fire", List.of("Rock", "Water"))).isEqualTo(0.25);
        assertThat(TypeChart.effectiveness("Water", List.of("Rock", "Ground"))).isEqualTo(4.0);
        assertThat(TypeChart.effectiveness(null, List.of("Ghost"))).isEqualTo(1.0);
    }

    @Test
    void trainerAiPicksTheStrongestMoveAndDefendsWhenWeak() {
        TrainerAi neverDefends = new TrainerAi(fixed(1, false));
        BattleFighter pikachu = fighter(PIKACHU);
        // Golpe Elétrico em Squirtle: 30 × 2 = 60 > Investida 22
        assertThat(neverDefends.choose(pikachu, fighter(SQUIRTLE))).isEqualTo(BattleAction.SPECIAL);
        // Golpe Elétrico não afeta Geodude (Terra): Investida
        assertThat(neverDefends.choose(pikachu, fighter(GEODUDE))).isEqualTo(BattleAction.ATTACK);
        // Geodude: Investida 66 > Golpe Pedra 25
        assertThat(neverDefends.choose(fighter(GEODUDE), pikachu)).isEqualTo(BattleAction.ATTACK);

        TrainerAi defendsWhenWeak = new TrainerAi(fixed(0, false));
        pikachu.setCurrentHp(10);
        assertThat(defendsWhenWeak.choose(pikachu, fighter(SQUIRTLE))).isEqualTo(BattleAction.DEFEND);
        pikachu.setLastAction(BattleAction.DEFEND);
        assertThat(defendsWhenWeak.choose(pikachu, fighter(SQUIRTLE))).as("não repete a defesa").isEqualTo(BattleAction.SPECIAL);
    }

    @Test
    void fasterPokemonStrikesFirst() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        engine.playRound(battle, BattleAction.ATTACK, null);

        List<BattleEvent> log = battle.getLog();
        assertThat(log).extracting(BattleEvent::side).containsExactly(BattleSide.PLAYER, BattleSide.TRAINER);
        assertThat(log.get(0).damage()).isEqualTo(15);  // Investida do Pikachu em Geodude
        assertThat(log.get(1).damage()).isEqualTo(66);  // Investida do Geodude em Pikachu
        assertThat(battle.active(BattleSide.TRAINER).getCurrentHp()).isEqualTo(85);
        assertThat(battle.active(BattleSide.PLAYER).getCurrentHp()).isEqualTo(29);
        assertThat(battle.getRound()).isEqualTo(1);
    }

    @Test
    void faintedPokemonIsReplacedAndDoesNotAct() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);
        battle.active(BattleSide.TRAINER).setCurrentHp(10);

        engine.playRound(battle, BattleAction.ATTACK, null);

        assertThat(battle.getLog()).extracting(BattleEvent::message).containsExactly(
                "Pikachu usou Investida! Geodude perdeu 15 HP.",
                "Geodude desmaiou!",
                "Brock enviou Onix!");
        assertThat(battle.getTrainerActive()).isEqualTo(1);
        assertThat(battle.active(BattleSide.PLAYER).getCurrentHp()).as("Geodude não atacou").isEqualTo(95);
        assertThat(battle.isFinished()).isFalse();
    }

    @Test
    void knockingOutTheWholeTeamWinsTheBadge() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);
        battle.getTrainerTeam().get(0).setCurrentHp(0);
        battle.setTrainerActive(1);
        battle.active(BattleSide.TRAINER).setCurrentHp(5);

        engine.playRound(battle, BattleAction.ATTACK, null);

        assertThat(battle.isFinished()).isTrue();
        assertThat(battle.getWinner()).isEqualTo(BattleSide.PLAYER);
        assertThat(battle.getFinishReason()).isEqualTo(Battle.FinishReason.KNOCKOUT);
        assertThat(battle.getLog().getLast().message()).isEqualTo("Ana derrotou Brock! Insígnia conquistada!");
        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.ATTACK, null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void losingTheLastPokemonLosesTheChallenge() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);
        battle.getPlayerTeam().get(1).setCurrentHp(0);
        battle.active(BattleSide.PLAYER).setCurrentHp(1);

        engine.playRound(battle, BattleAction.ATTACK, null);

        assertThat(battle.getWinner()).isEqualTo(BattleSide.TRAINER);
        assertThat(battle.getLog().getLast().message()).isEqualTo("Brock venceu o desafio! Todos os Pokémon de Ana desmaiaram.");
    }

    @Test
    void switchingHappensFirstAndTakesTheHit() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        engine.playRound(battle, BattleAction.SWITCH, 1);

        assertThat(battle.getPlayerActive()).isEqualTo(1);
        assertThat(battle.getLog().get(0).message()).isEqualTo("Ana trocou Pikachu por Squirtle!");
        assertThat(battle.getLog().get(1).damage()).isEqualTo(31); // Investida do Geodude em Squirtle (Defesa 65)
        assertThat(battle.getPlayerTeam()).extracting(BattleFighter::getCurrentHp).containsExactly(95, 104 - 31);
    }

    @Test
    void rejectsInvalidSwitchesAndRepeatedDefense() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.SWITCH, null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.SWITCH, 5)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.SWITCH, 0))
                .hasMessageContaining("Pikachu já está em campo");
        battle.getPlayerTeam().get(1).setCurrentHp(0);
        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.SWITCH, 1))
                .hasMessageContaining("Squirtle desmaiou e não pode entrar");
        assertThatThrownBy(() -> engine.playRound(battle, null, null)).isInstanceOf(InvalidRequestException.class);

        engine.playRound(battle, BattleAction.DEFEND, null);
        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.DEFEND, null))
                .hasMessageContaining("não pode defender duas rodadas seguidas");
        assertThat(battle.getRound()).as("rodadas inválidas não contam").isEqualTo(1);
    }

    @Test
    void defenseHalvesTheDamage() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        engine.playRound(battle, BattleAction.DEFEND, null);

        assertThat(battle.getLog()).extracting(BattleEvent::action).containsExactly(BattleAction.DEFEND, BattleAction.ATTACK);
        assertThat(battle.getLog().get(1).damage()).isEqualTo(33); // ⌊66 × 0,5⌋
    }

    @Test
    void forfeitGivesTheVictoryToTheTrainer() {
        Battle battle = brockChallenge(engine, PIKACHU, SQUIRTLE);

        engine.forfeit(battle);

        assertThat(battle.getWinner()).isEqualTo(BattleSide.TRAINER);
        assertThat(battle.getFinishReason()).isEqualTo(Battle.FinishReason.FORFEIT);
        assertThat(battle.getLog().getLast().message()).isEqualTo("Ana desistiu. Brock venceu o desafio!");
        assertThatThrownBy(() -> engine.forfeit(battle)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aFullRandomBattleAlwaysEnds() {
        BattleEngine realRandom = engine(new Random());
        Battle battle = brockChallenge(realRandom, MEW, SQUIRTLE);
        BattleAction[] cycle = {BattleAction.ATTACK, BattleAction.SPECIAL, BattleAction.DEFEND};

        for (int round = 0; round < 300 && !battle.isFinished(); round++) {
            realRandom.playRound(battle, cycle[round % 3], null);
        }

        assertThat(battle.isFinished()).isTrue();
        assertThat(battle.remaining(battle.getWinner().opponent())).isZero();
    }

    private static BattleFighter fighter(Pokemon pokemon) {
        BattleFighter fighter = BattleFighter.of(pokemon);
        fighter.setMaxHp(BattleEngine.battleHp(pokemon.getHp()));
        fighter.setCurrentHp(fighter.getMaxHp());
        return fighter;
    }
}
