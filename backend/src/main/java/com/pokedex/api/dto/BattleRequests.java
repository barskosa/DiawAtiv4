package com.pokedex.api.dto;

import com.pokedex.api.model.BattleAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpos das requisições de batalha ({@code /api/battles}). */
public final class BattleRequests {

    private BattleRequests() {
    }

    /** {@code POST /api/battles}: treinador desafiado e nome do jogador (opcional). */
    public record Challenge(
            @NotNull(message = "é obrigatório") @Positive(message = "deve ser um número positivo") Integer trainerId,
            @Size(max = 20, message = "deve ter no máximo 20 caracteres") String playerName) {
    }

    /**
     * {@code POST /api/battles/{id}/rounds}: ação do jogador (ATTACK, SPECIAL, DEFEND ou SWITCH) e,
     * para SWITCH, a posição no time do Pokémon que vai entrar.
     */
    public record Round(
            @NotNull(message = "é obrigatório") BattleAction action,
            Integer switchTo) {
    }
}
