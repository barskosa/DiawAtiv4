package com.pokedex.api.dto;

import com.pokedex.api.model.BattleAction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpos das requisições da Arena PvP ({@code /api/battles}). */
public final class BattleRequests {

    private BattleRequests() {
    }

    /** Um jogador e o Pokémon escolhido. O nome é opcional ("Jogador 1"/"Jogador 2"). */
    public record PlayerChoice(
            @Size(max = 20, message = "deve ter no máximo 20 caracteres") String name,
            @NotNull(message = "é obrigatório") @Positive(message = "deve ser um número positivo") Integer pokemonId) {
    }

    /** {@code POST /api/battles} */
    public record CreateBattle(
            @NotNull(message = "é obrigatório") @Valid PlayerChoice player1,
            @NotNull(message = "é obrigatório") @Valid PlayerChoice player2) {
    }

    /** {@code POST /api/battles/{id}/rounds}: ATTACK, SPECIAL ou DEFEND para cada jogador. */
    public record Round(
            @NotNull(message = "é obrigatório") BattleAction player1Action,
            @NotNull(message = "é obrigatório") BattleAction player2Action) {
    }

    /** {@code POST /api/battles/{id}/forfeit}: jogador que desiste (1 ou 2). */
    public record Forfeit(
            @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser 1 ou 2") @Max(value = 2, message = "deve ser 1 ou 2") Integer player) {
    }
}
