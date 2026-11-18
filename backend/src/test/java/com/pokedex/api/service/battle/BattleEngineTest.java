package com.pokedex.api.service.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleAction;
import com.pokedex.api.model.BattleEvent;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.model.Pokemon;

/**
 * Regras da batalha com aleatoriedade controlada: sem crítico e variação máxima (1,00), a não ser
 * quando o teste pede outra coisa. Os danos esperados foram calculados à mão pela fórmula.
 */
class BattleEngineTest {

    private static final Pokemon PIKACHU = new Pokemon(25, "Pikachu", 35, 55, 30, 50, 40, 90, List.of("Electric"));
    private static final Pokemon SQUIRTLE = new Pokemon(7, "Squirtle", 44, 48, 65, 60, 54, 43, List.of("Water"));
    private static final Pokemon GEODUDE = new Pokemon(74, "Geodude", 40, 80, 100, 30, 30, 20, List.of("Rock", "Ground"));
    private static final Pokemon MEW = new Pokemon(151, "Mew", 100, 100, 100, 100, 100, 100, List.of());

    /** Gerador previsível: crítico quando critRoll == 0; nextBoolean define empates. */
    private static RandomGenerator fixed(int critRoll, boolean coin) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return critRoll;
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

    private static Battle battle(BattleEngine engine, Pokemon first, Pokemon second) {
        Battle battle = new Battle();
        battle.setPlayer1(BattleFighter.of("Ana", first));
        battle.setPlayer2(BattleFighter.of("Bruno", second));
        engine.start(battle);
        return battle;
    }

    private final BattleEngine engine = new BattleEngine(fixed(1, false));

    @Test
    void calculatesBattleHpAtLevel50() {
        assertThat(BattleEngine.battleHp(35)).isEqualTo(95);
        assertThat(BattleEngine.battleHp(250)).isEqualTo(310);
        Battle battle = battle(engine, PIKACHU, SQUIRTLE);
        assertThat(battle.getPlayer1().getCurrentHp()).isEqualTo(95);
        assertThat(battle.getPlayer2().getMaxHp()).isEqualTo(104);
    }

    @Test
    void typeChartCombinesBothDefenderTypes() {
        assertThat(TypeChart.effectiveness("Electric", List.of("Water"))).isEqualTo(2.0);
        assertThat(TypeChart.effectiveness("Electric", List.of("Rock", "Ground"))).isZero();
        assertThat(TypeChart.effectiveness("Ice", List.of("Dragon", "Flying"))).isEqualTo(4.0);
        assertThat(TypeChart.effectiveness("Fire", List.of("Rock", "Water"))).isEqualTo(0.25);
        assertThat(TypeChart.effectiveness("Water", List.of("Rock", "Ground"))).isEqualTo(4.0);
        assertThat(TypeChart.effectiveness("Normal", List.of("Ghost", "Poison"))).isZero();
        assertThat(TypeChart.effectiveness(null, List.of("Ghost"))).isEqualTo(1.0);
        assertThat(TypeChart.effectiveness("Fire", List.of())).isEqualTo(1.0);
    }

    @Test
    void fasterPokemonStrikesFirstAndSpecialUsesTypeChart() {
        Battle battle = battle(engine, SQUIRTLE, PIKACHU); // Pikachu (jogador 2) é mais rápido

        engine.playRound(battle, BattleAction.SPECIAL, BattleAction.SPECIAL);

        List<BattleEvent> log = battle.getLog();
        assertThat(log).extracting(BattleEvent::player).containsExactly(2, 1);
        // Pikachu: ⌊⌊22×70×50/54⌋/50⌋+2 = 30, × 2 (Elétrico contra Água) = 60
        assertThat(log.get(0).damage()).isEqualTo(60);
        assertThat(log.get(0).moveName()).isEqualTo("Golpe Elétrico");
        assertThat(log.get(0).message()).contains("É super efetivo!");
        // Squirtle: ⌊⌊22×70×60/40⌋/50⌋+2 = 48
        assertThat(log.get(1).damage()).isEqualTo(48);
        assertThat(battle.getPlayer1().getCurrentHp()).isEqualTo(104 - 60);
        assertThat(battle.getPlayer2().getCurrentHp()).isEqualTo(95 - 48);
        assertThat(battle.getRound()).isEqualTo(1);
    }

    @Test
    void attackIsNeutralPhysicalDamage() {
        Battle battle = battle(engine, PIKACHU, SQUIRTLE);

        engine.playRound(battle, BattleAction.ATTACK, BattleAction.DEFEND);

        // Defesa primeiro; Investida: ⌊⌊22×55×55/65⌋/50⌋+2 = 22, metade pela defesa = 11
        assertThat(battle.getLog()).extracting(BattleEvent::action).containsExactly(BattleAction.DEFEND, BattleAction.ATTACK);
        assertThat(battle.getLog().get(1).damage()).isEqualTo(11);
        assertThat(battle.getLog().get(1).message()).contains("A defesa reduziu o dano");
    }

    @Test
    void cannotDefendTwiceInARow() {
        Battle battle = battle(engine, PIKACHU, SQUIRTLE);
        engine.playRound(battle, BattleAction.DEFEND, BattleAction.ATTACK);

        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.DEFEND, BattleAction.ATTACK))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("não pode defender duas rodadas seguidas");
        assertThat(battle.getRound()).as("rodada inválida não conta").isEqualTo(1);

        engine.playRound(battle, BattleAction.ATTACK, BattleAction.DEFEND);
        assertThat(battle.getRound()).isEqualTo(2);
    }

    @Test
    void immunityCausesNoDamage() {
        Battle battle = battle(engine, PIKACHU, GEODUDE);

        engine.playRound(battle, BattleAction.SPECIAL, BattleAction.DEFEND);

        BattleEvent strike = battle.getLog().get(1);
        assertThat(strike.damage()).isZero();
        assertThat(strike.effectiveness()).isZero();
        assertThat(strike.message()).contains("Não teve efeito");
        assertThat(battle.getPlayer2().getCurrentHp()).isEqualTo(battle.getPlayer2().getMaxHp());
    }

    @Test
    void typelessPokemonUsesNeutralSpecialMove() {
        Battle battle = battle(engine, MEW, GEODUDE);

        engine.playRound(battle, BattleAction.SPECIAL, BattleAction.DEFEND);

        assertThat(battle.getLog().get(1).moveName()).isEqualTo("Rajada Mística");
        assertThat(battle.getLog().get(1).effectiveness()).isEqualTo(1.0);
    }

    @Test
    void criticalHitMultipliesDamage() {
        BattleEngine critical = new BattleEngine(fixed(0, false));
        Battle battle = battle(critical, PIKACHU, SQUIRTLE);

        critical.playRound(battle, BattleAction.ATTACK, BattleAction.DEFEND);

        assertThat(battle.getLog().get(1).critical()).isTrue();
        assertThat(battle.getLog().get(1).damage()).isEqualTo(16); // ⌊22 × 1,5 × 0,5⌋
    }

    @Test
    void speedTieIsDecidedByTheCoin() {
        BattleEngine coinForPlayer2 = new BattleEngine(fixed(1, true));
        Battle battle = battle(coinForPlayer2, PIKACHU, PIKACHU);

        coinForPlayer2.playRound(battle, BattleAction.ATTACK, BattleAction.ATTACK);

        assertThat(battle.getLog()).extracting(BattleEvent::player).containsExactly(2, 1);
    }

    @Test
    void knockoutEndsTheBattleAndTheLoserDoesNotAct() {
        Battle battle = battle(engine, PIKACHU, SQUIRTLE);
        battle.getPlayer2().setCurrentHp(10);

        engine.playRound(battle, BattleAction.ATTACK, BattleAction.SPECIAL);

        assertThat(battle.isFinished()).isTrue();
        assertThat(battle.getWinner()).isEqualTo(1);
        assertThat(battle.getFinishReason()).isEqualTo(Battle.FinishReason.KNOCKOUT);
        assertThat(battle.getFinishedAt()).isNotNull();
        assertThat(battle.getPlayer2().getCurrentHp()).isZero();
        assertThat(battle.getLog()).hasSize(2);
        assertThat(battle.getLog().get(1).message()).contains("Squirtle desmaiou! Ana venceu a batalha!");
        assertThat(battle.getPlayer1().getCurrentHp()).as("Squirtle não chegou a atacar").isEqualTo(95);

        assertThatThrownBy(() -> engine.playRound(battle, BattleAction.ATTACK, BattleAction.ATTACK))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void forfeitGivesTheVictoryToTheOpponent() {
        Battle battle = battle(engine, PIKACHU, SQUIRTLE);

        engine.forfeit(battle, 1);

        assertThat(battle.getWinner()).isEqualTo(2);
        assertThat(battle.getFinishReason()).isEqualTo(Battle.FinishReason.FORFEIT);
        assertThat(battle.getLog().getLast().message()).isEqualTo("Ana desistiu. Bruno venceu a batalha!");
        assertThatThrownBy(() -> engine.forfeit(battle, 2)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aFullBattleAlwaysEnds() {
        BattleEngine realRandom = new BattleEngine(RandomGenerator.getDefault());
        Battle battle = battle(realRandom, MEW, SQUIRTLE);
        BattleAction[] cycle = {BattleAction.ATTACK, BattleAction.SPECIAL, BattleAction.DEFEND};

        for (int round = 0; round < 200 && !battle.isFinished(); round++) {
            realRandom.playRound(battle, cycle[round % 3], cycle[(round + 1) % 3]);
        }

        assertThat(battle.isFinished()).isTrue();
        assertThat(battle.fighter(battle.getWinner() == 1 ? 2 : 1).getCurrentHp()).isZero();
    }
}
