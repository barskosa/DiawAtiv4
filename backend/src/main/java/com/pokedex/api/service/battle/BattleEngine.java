package com.pokedex.api.service.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleAction;
import com.pokedex.api.model.BattleEvent;
import com.pokedex.api.model.BattleFighter;

/**
 * Regras da batalha PvP (inspiradas na fórmula de dano dos jogos, simplificadas, nível 50).
 *
 * <ul>
 *   <li>HP de batalha: {@code 2 × HP × 50 / 100 + 60}.</li>
 *   <li>Investida ({@link BattleAction#ATTACK}): poder 55, sem tipo, Ataque × Defesa.</li>
 *   <li>Golpe especial ({@link BattleAction#SPECIAL}): poder 70, do 1º tipo do Pokémon,
 *       Ataque especial × Defesa especial, multiplicado pela tabela de tipos.</li>
 *   <li>Defender ({@link BattleAction#DEFEND}): age antes de tudo e reduz à metade o dano recebido
 *       na rodada; não pode ser usada duas rodadas seguidas.</li>
 *   <li>Ordem dos golpes: maior Velocidade primeiro; empate decidido no sorteio.</li>
 *   <li>Dano: {@code ⌊⌊22 × poder × A / D⌋ / 50⌋ + 2}, × efetividade × crítico (1/16, ×1,5)
 *       × variação aleatória de 0,85 a 1,00; mínimo 1 quando o golpe tem efeito.</li>
 * </ul>
 */
@Component
public class BattleEngine {

    public static final int LEVEL = 50;
    public static final int ATTACK_POWER = 55;
    public static final int SPECIAL_POWER = 70;
    public static final String ATTACK_NAME = "Investida";

    private final RandomGenerator random;

    public BattleEngine(RandomGenerator random) {
        this.random = random;
    }

    public static int battleHp(int baseHp) {
        return 2 * baseHp * LEVEL / 100 + LEVEL + 10;
    }

    /** Tipo do golpe especial: o primeiro tipo do Pokémon (sem tipo → golpe neutro). */
    public static String specialType(BattleFighter fighter) {
        return fighter.getTypes().isEmpty() ? null : fighter.getTypes().get(0);
    }

    public static String specialName(BattleFighter fighter) {
        String type = specialType(fighter);
        return type == null ? "Rajada Mística" : "Golpe " + TypeChart.label(type);
    }

    /** Prepara os dois participantes com o HP de batalha cheio. */
    public void start(Battle battle) {
        for (BattleFighter fighter : List.of(battle.getPlayer1(), battle.getPlayer2())) {
            fighter.setMaxHp(battleHp(fighter.getHp()));
            fighter.setCurrentHp(fighter.getMaxHp());
            fighter.setDefending(false);
            fighter.setLastAction(null);
        }
    }

    /** Resolve uma rodada com as ações escolhidas pelos dois jogadores. */
    public void playRound(Battle battle, BattleAction player1Action, BattleAction player2Action) {
        if (battle.isFinished()) {
            throw new ConflictException("battle", "A batalha já terminou.");
        }
        validateDefend(battle.getPlayer1(), player1Action, "player1Action");
        validateDefend(battle.getPlayer2(), player2Action, "player2Action");

        int round = battle.getRound() + 1;
        battle.setRound(round);
        BattleAction[] actions = {null, player1Action, player2Action};

        // 1) Defesas acontecem antes de qualquer golpe.
        for (int player = 1; player <= 2; player++) {
            BattleFighter fighter = battle.fighter(player);
            fighter.setDefending(actions[player] == BattleAction.DEFEND);
            if (fighter.isDefending()) {
                battle.getLog().add(new BattleEvent(round, player, BattleAction.DEFEND, "Defender", 0, 1.0, false,
                        fighter.getCurrentHp(), fighter.getPokemonName() + " se protegeu!"));
            }
        }

        // 2) Golpes em ordem de Velocidade.
        List<Integer> attackers = new ArrayList<>();
        for (int player = 1; player <= 2; player++) {
            if (actions[player] != BattleAction.DEFEND) {
                attackers.add(player);
            }
        }
        if (attackers.size() == 2 && goesSecond(battle.getPlayer1(), battle.getPlayer2())) {
            attackers = List.of(2, 1);
        }
        for (int player : attackers) {
            BattleFighter attacker = battle.fighter(player);
            BattleFighter defender = battle.opponentOf(player);
            if (attacker.isFainted() || defender.isFainted()) {
                continue;
            }
            battle.getLog().add(strike(round, player, attacker, defender, actions[player]));
            if (defender.isFainted()) {
                int loser = player == 1 ? 2 : 1;
                battle.getLog().add(new BattleEvent(round, loser, null, null, 0, 1.0, false, 0,
                        defender.getPokemonName() + " desmaiou! " + attacker.getPlayerName() + " venceu a batalha!"));
                battle.finish(player, Battle.FinishReason.KNOCKOUT);
            }
        }

        battle.getPlayer1().setLastAction(player1Action);
        battle.getPlayer2().setLastAction(player2Action);
    }

    /** Desistência: o adversário vence. */
    public void forfeit(Battle battle, int player) {
        if (battle.isFinished()) {
            throw new ConflictException("battle", "A batalha já terminou.");
        }
        int winner = player == 1 ? 2 : 1;
        BattleFighter quitter = battle.fighter(player);
        battle.getLog().add(new BattleEvent(battle.getRound(), player, null, null, 0, 1.0, false,
                quitter.getCurrentHp(), quitter.getPlayerName() + " desistiu. "
                        + battle.fighter(winner).getPlayerName() + " venceu a batalha!"));
        battle.finish(winner, Battle.FinishReason.FORFEIT);
    }

    /** {@code true} se o jogador 1 deve agir depois do jogador 2. */
    private boolean goesSecond(BattleFighter player1, BattleFighter player2) {
        if (player1.getSpeed() != player2.getSpeed()) {
            return player1.getSpeed() < player2.getSpeed();
        }
        return random.nextBoolean();
    }

    private BattleEvent strike(int round, int player, BattleFighter attacker, BattleFighter defender, BattleAction action) {
        boolean special = action == BattleAction.SPECIAL;
        String moveName = special ? specialName(attacker) : ATTACK_NAME;
        double effectiveness = special ? TypeChart.effectiveness(specialType(attacker), defender.getTypes()) : 1.0;

        StringBuilder message = new StringBuilder(attacker.getPokemonName()).append(" usou ").append(moveName).append('!');
        if (effectiveness == 0) {
            message.append(" Não teve efeito em ").append(defender.getPokemonName()).append('.');
            return new BattleEvent(round, player, action, moveName, 0, 0.0, false, defender.getCurrentHp(), message.toString());
        }

        int attackStat = special ? attacker.getSpecialAttack() : attacker.getAttack();
        int defenseStat = special ? defender.getSpecialDefense() : defender.getDefense();
        int power = special ? SPECIAL_POWER : ATTACK_POWER;
        int base = (2 * LEVEL / 5 + 2) * power * attackStat / Math.max(1, defenseStat) / 50 + 2;

        boolean critical = random.nextInt(16) == 0;
        double variation = 0.85 + random.nextDouble() * 0.15;
        double multiplier = effectiveness * (critical ? 1.5 : 1.0) * variation * (defender.isDefending() ? 0.5 : 1.0);
        int damage = Math.max(1, (int) Math.floor(base * multiplier));
        defender.setCurrentHp(Math.max(0, defender.getCurrentHp() - damage));

        if (critical) {
            message.append(" Acerto crítico!");
        }
        if (effectiveness > 1) {
            message.append(" É super efetivo!");
        } else if (effectiveness < 1) {
            message.append(" Não é muito efetivo...");
        }
        if (defender.isDefending()) {
            message.append(" A defesa reduziu o dano.");
        }
        message.append(' ').append(defender.getPokemonName()).append(" perdeu ").append(damage).append(" HP.");
        return new BattleEvent(round, player, action, moveName, damage, effectiveness, critical,
                defender.getCurrentHp(), message.toString());
    }

    private static void validateDefend(BattleFighter fighter, BattleAction action, String field) {
        if (action == null) {
            throw new InvalidRequestException(field, "Escolha a ação de " + fighter.getPlayerName() + ".");
        }
        if (action == BattleAction.DEFEND && fighter.getLastAction() == BattleAction.DEFEND) {
            throw new InvalidRequestException(field, fighter.getPokemonName()
                    + " não pode defender duas rodadas seguidas.");
        }
    }
}
