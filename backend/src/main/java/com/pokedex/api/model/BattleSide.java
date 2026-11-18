package com.pokedex.api.model;

/** Lados da batalha: o jogador (time sorteado) e o treinador (equipe do Script_Pokedex.sql). */
public enum BattleSide {
    PLAYER,
    TRAINER;

    public BattleSide opponent() {
        return this == PLAYER ? TRAINER : PLAYER;
    }
}
