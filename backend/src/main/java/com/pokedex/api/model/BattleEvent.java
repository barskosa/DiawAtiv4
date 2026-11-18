package com.pokedex.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Um acontecimento narrado da batalha (golpe, defesa, troca, desmaio, desistência).
 *
 * @param side          lado que agiu
 * @param damage        dano causado (0 quando não houve)
 * @param effectiveness multiplicador de tipo do golpe (1.0 = normal)
 * @param targetHp      HP do alvo depois da ação
 */
@JsonPropertyOrder({"round", "side", "action", "moveName", "damage", "effectiveness", "critical", "targetHp", "message"})
public record BattleEvent(int round, BattleSide side, BattleAction action, String moveName, int damage,
        double effectiveness, boolean critical, int targetHp, String message) {

    public static BattleEvent narration(int round, BattleSide side, BattleAction action, String message) {
        return new BattleEvent(round, side, action, null, 0, 1.0, false, 0, message);
    }
}
