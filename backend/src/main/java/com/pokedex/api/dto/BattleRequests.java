package com.pokedex.api.dto;

import com.pokedex.api.model.BattleAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpos das requisições de batalha ({@code /api/battles}). */
public final class BattleRequests {

    private BattleRequests() {
    }

    /**
     * {@code POST /api/battles}: treinador desafiado e nome do jogador (opcional). Com {@code daily: true}
     * é o desafio do dia: o treinador e o time do jogador são os do dia, e {@code trainerId} é dispensado.
     */
    public record Challenge(
            @Positive(message = "deve ser um número positivo") Integer trainerId,
            @Size(max = 20, message = "deve ter no máximo 20 caracteres") String playerName,
            Boolean daily) {

        public boolean isDaily() {
            return Boolean.TRUE.equals(daily);
        }
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
