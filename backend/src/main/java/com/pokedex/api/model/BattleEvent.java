package com.pokedex.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Um acontecimento narrado da batalha (golpe, defesa, nocaute, desistência).
 *
 * @param player        jogador que agiu (1 ou 2)
 * @param damage        dano causado (0 quando não houve)
 * @param effectiveness multiplicador de tipo do golpe (1.0 = normal)
 * @param targetHp      HP do alvo depois da ação
 */
@JsonPropertyOrder({"round", "player", "action", "moveName", "damage", "effectiveness", "critical", "targetHp", "message"})
public record BattleEvent(int round, int player, BattleAction action, String moveName, int damage,
        double effectiveness, boolean critical, int targetHp, String message) {
}
