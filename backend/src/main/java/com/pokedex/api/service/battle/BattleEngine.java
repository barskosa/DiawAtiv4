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
import com.pokedex.api.model.BattleSide;

/**
 * Regras do desafio contra treinadores (fórmula de dano dos jogos simplificada, nível 50).
 *
 * <ul>
 *   <li>HP de batalha: {@code HP base + 60}.</li>
 *   <li>Investida ({@link BattleAction#ATTACK}): poder 55, sem tipo, Ataque × Defesa.</li>
 *   <li>Golpe especial ({@link BattleAction#SPECIAL}): poder 70, do 1º tipo do Pokémon,
 *       Ataque especial × Defesa especial, multiplicado pela tabela de tipos.</li>
 *   <li>Trocar ({@link BattleAction#SWITCH}, só o jogador) e Defender ({@link BattleAction#DEFEND})
 *       acontecem antes dos golpes. Defender reduz o dano à metade e não pode ser repetido em seguida.</li>
 *   <li>Golpes em ordem de Velocidade; empate decidido no sorteio.</li>
 *   <li>Dano: {@code ⌊⌊22 × poder × A / D⌋ / 50⌋ + 2}, × efetividade × crítico (1/16, ×1,5)
 *       × variação de 0,85 a 1,00; mínimo 1 quando o golpe tem efeito.</li>
 *   <li>Pokémon que desmaia não age; o próximo vivo do time entra. Sem Pokémon vivos, o lado perde.</li>
 * </ul>
 */
@Component
public class BattleEngine {

    public static final int LEVEL = 50;
    public static final int ATTACK_POWER = 55;
    public static final int SPECIAL_POWER = 70;
    public static final String ATTACK_NAME = "Investida";

    private final RandomGenerator random;
    private final TrainerAi trainerAi;

    public BattleEngine(RandomGenerator random, TrainerAi trainerAi) {
        this.random = random;
        this.trainerAi = trainerAi;
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

    /** Dano base, sem crítico nem variação aleatória (a IA usa para escolher o golpe). */
    public static double expectedDamage(BattleFighter attacker, BattleFighter defender, BattleAction action) {
        boolean special = action == BattleAction.SPECIAL;
        double effectiveness = special ? TypeChart.effectiveness(specialType(attacker), defender.getTypes()) : 1.0;
        return baseDamage(attacker, defender, special) * effectiveness;
    }

    private static int baseDamage(BattleFighter attacker, BattleFighter defender, boolean special) {
        int attackStat = special ? attacker.getSpecialAttack() : attacker.getAttack();
        int defenseStat = special ? defender.getSpecialDefense() : defender.getDefense();
        int power = special ? SPECIAL_POWER : ATTACK_POWER;
        return (2 * LEVEL / 5 + 2) * power * attackStat / Math.max(1, defenseStat) / 50 + 2;
    }

    /** Prepara os times com HP cheio e o primeiro Pokémon de cada lado em campo. */
    public void start(Battle battle) {
        for (BattleSide side : BattleSide.values()) {
            for (BattleFighter fighter : battle.team(side)) {
                fighter.setMaxHp(battleHp(fighter.getHp()));
                fighter.setCurrentHp(fighter.getMaxHp());
                fighter.setDefending(false);
                fighter.setLastAction(null);
            }
            battle.setActive(side, 0);
        }
    }

    /**
     * Resolve uma rodada: a ação do jogador e a escolhida pela IA do treinador.
     *
     * @param switchTo posição no time do jogador, obrigatória quando a ação é {@link BattleAction#SWITCH}
     */
    public void playRound(Battle battle, BattleAction playerAction, Integer switchTo) {
        if (battle.isFinished()) {
            throw new ConflictException("battle", "A batalha já terminou.");
        }
        validate(battle, playerAction, switchTo);

        BattleFighter trainerFighter = battle.active(BattleSide.TRAINER);
        BattleAction trainerAction = trainerAi.choose(trainerFighter, battle.active(BattleSide.PLAYER));
        int round = battle.getRound() + 1;
        battle.setRound(round);
        battle.active(BattleSide.PLAYER).setDefending(false);
        trainerFighter.setDefending(false);

        // 1) Troca do jogador acontece antes de tudo.
        if (playerAction == BattleAction.SWITCH) {
            BattleFighter leaving = battle.active(BattleSide.PLAYER);
            leaving.setLastAction(null);
            battle.setActive(BattleSide.PLAYER, switchTo);
            BattleFighter entering = battle.active(BattleSide.PLAYER);
            battle.getLog().add(BattleEvent.narration(round, BattleSide.PLAYER, BattleAction.SWITCH,
                    battle.getPlayerName() + " trocou " + leaving.getPokemonName() + " por " + entering.getPokemonName() + "!"));
        }
        BattleFighter playerFighter = battle.active(BattleSide.PLAYER);

        // 2) Defesas.
        if (playerAction == BattleAction.DEFEND) {
            defend(battle, round, BattleSide.PLAYER, playerFighter);
        }
        if (trainerAction == BattleAction.DEFEND) {
            defend(battle, round, BattleSide.TRAINER, trainerFighter);
        }

        // 3) Golpes, em ordem de Velocidade. Quem desmaiar antes de agir perde a vez.
        List<BattleSide> attackers = new ArrayList<>();
        if (isStrike(playerAction)) {
            attackers.add(BattleSide.PLAYER);
        }
        if (isStrike(trainerAction)) {
            attackers.add(BattleSide.TRAINER);
        }
        if (attackers.size() == 2 && trainerGoesFirst(playerFighter, trainerFighter)) {
            attackers = List.of(BattleSide.TRAINER, BattleSide.PLAYER);
        }
        for (BattleSide side : attackers) {
            BattleFighter attacker = side == BattleSide.PLAYER ? playerFighter : trainerFighter;
            BattleAction action = side == BattleSide.PLAYER ? playerAction : trainerAction;
            BattleFighter defender = battle.active(side.opponent());
            if (attacker.isFainted() || battle.active(side) != attacker || battle.isFinished()) {
                continue;
            }
            battle.getLog().add(strike(round, side, attacker, defender, action));
            if (defender.isFainted()) {
                handleFaint(battle, round, side.opponent(), defender);
            }
        }

        playerFighter.setLastAction(playerAction);
        trainerFighter.setLastAction(trainerAction);
    }

    /** Desistência do jogador: o treinador vence. */
    public void forfeit(Battle battle) {
        if (battle.isFinished()) {
            throw new ConflictException("battle", "A batalha já terminou.");
        }
        battle.getLog().add(BattleEvent.narration(battle.getRound(), BattleSide.PLAYER, null,
                battle.getPlayerName() + " desistiu. " + battle.getTrainer().name() + " venceu o desafio!"));
        battle.finish(BattleSide.TRAINER, Battle.FinishReason.FORFEIT);
    }

    private void handleFaint(Battle battle, int round, BattleSide side, BattleFighter fainted) {
        battle.getLog().add(BattleEvent.narration(round, side, null, fainted.getPokemonName() + " desmaiou!"));
        List<BattleFighter> team = battle.team(side);
        for (int index = 0; index < team.size(); index++) {
            if (!team.get(index).isFainted()) {
                battle.setActive(side, index);
                team.get(index).setDefending(false);
                battle.getLog().add(BattleEvent.narration(round, side, null,
                        battle.sideName(side) + " enviou " + team.get(index).getPokemonName() + "!"));
                return;
            }
        }
        BattleSide winner = side.opponent();
        String message = winner == BattleSide.PLAYER
                ? battle.getPlayerName() + " derrotou " + battle.getTrainer().name() + "! Insígnia conquistada!"
                : battle.getTrainer().name() + " venceu o desafio! Todos os Pokémon de " + battle.getPlayerName() + " desmaiaram.";
        battle.getLog().add(BattleEvent.narration(round, winner, null, message));
        battle.finish(winner, Battle.FinishReason.KNOCKOUT);
    }

    private void defend(Battle battle, int round, BattleSide side, BattleFighter fighter) {
        fighter.setDefending(true);
        battle.getLog().add(new BattleEvent(round, side, BattleAction.DEFEND, "Defender", 0, 1.0, false,
                fighter.getCurrentHp(), fighter.getPokemonName() + " se protegeu!"));
    }

    private boolean trainerGoesFirst(BattleFighter player, BattleFighter trainer) {
        if (player.getSpeed() != trainer.getSpeed()) {
            return trainer.getSpeed() > player.getSpeed();
        }
        return random.nextBoolean();
    }

    private BattleEvent strike(int round, BattleSide side, BattleFighter attacker, BattleFighter defender, BattleAction action) {
        boolean special = action == BattleAction.SPECIAL;
        String moveName = special ? specialName(attacker) : ATTACK_NAME;
        double effectiveness = special ? TypeChart.effectiveness(specialType(attacker), defender.getTypes()) : 1.0;

        StringBuilder message = new StringBuilder(attacker.getPokemonName()).append(" usou ").append(moveName).append('!');
        if (effectiveness == 0) {
            message.append(" Não teve efeito em ").append(defender.getPokemonName()).append('.');
            return new BattleEvent(round, side, action, moveName, 0, 0.0, false, defender.getCurrentHp(), message.toString());
        }

        boolean critical = random.nextInt(16) == 0;
        double variation = 0.85 + random.nextDouble() * 0.15;
        double multiplier = effectiveness * (critical ? 1.5 : 1.0) * variation * (defender.isDefending() ? 0.5 : 1.0);
        int damage = Math.max(1, (int) Math.floor(baseDamage(attacker, defender, special) * multiplier));
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
        return new BattleEvent(round, side, action, moveName, damage, effectiveness, critical,
                defender.getCurrentHp(), message.toString());
    }

    private static boolean isStrike(BattleAction action) {
        return action == BattleAction.ATTACK || action == BattleAction.SPECIAL;
    }

    private static void validate(Battle battle, BattleAction action, Integer switchTo) {
        BattleFighter active = battle.active(BattleSide.PLAYER);
        if (action == null) {
            throw new InvalidRequestException("action", "Escolha uma ação.");
        }
        if (action == BattleAction.DEFEND && active.getLastAction() == BattleAction.DEFEND) {
            throw new InvalidRequestException("action", active.getPokemonName() + " não pode defender duas rodadas seguidas.");
        }
        if (action != BattleAction.SWITCH) {
            return;
        }
        List<BattleFighter> team = battle.getPlayerTeam();
        if (switchTo == null || switchTo < 0 || switchTo >= team.size()) {
            throw new InvalidRequestException("switchTo", "Informe a posição (0 a " + (team.size() - 1)
                    + ") do Pokémon que vai entrar.");
        }
        if (switchTo == battle.getPlayerActive()) {
            throw new InvalidRequestException("switchTo", team.get(switchTo).getPokemonName() + " já está em campo.");
        }
        if (team.get(switchTo).isFainted()) {
            throw new InvalidRequestException("switchTo", team.get(switchTo).getPokemonName() + " desmaiou e não pode entrar.");
        }
    }
}
