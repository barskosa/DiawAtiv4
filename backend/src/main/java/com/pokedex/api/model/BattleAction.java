package com.pokedex.api.model;

/** Ações que um jogador pode escolher em cada rodada da batalha. */
public enum BattleAction {
    /** "Investida": golpe físico sem tipo (sempre dano normal). */
    ATTACK,
    /** Golpe especial do tipo principal do Pokémon, afetado pela tabela de tipos. */
    SPECIAL,
    /** Age primeiro e reduz à metade o dano recebido na rodada; não pode ser repetida em seguida. */
    DEFEND
}
