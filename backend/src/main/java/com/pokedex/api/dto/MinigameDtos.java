package com.pokedex.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.pokedex.api.model.MinigameScore;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpos e respostas dos minigames ({@code /api/minigames}). */
public final class MinigameDtos {

    private MinigameDtos() {
    }

    /** {@code POST /api/minigames/scores}: resultado de uma partida. */
    public record ScoreRequest(
            @NotNull(message = "é obrigatório") MinigameScore.Game game,
            @Size(max = 20, message = "deve ter no máximo 20 caracteres") String playerName,
            @NotNull(message = "é obrigatório") @Min(value = 0, message = "não pode ser negativa") Integer score,
            @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 20, message = "deve ser no máximo 20") Integer rounds) {
    }

    /** Placar salvo e a posição dele no ranking do jogo. */
    @JsonPropertyOrder({"score", "position"})
    public record SavedScore(MinigameScore score, long position) {
    }

    /** Estatísticas de um jogo ({@code GET /api/minigames/stats}). */
    @JsonPropertyOrder({"game", "plays", "bestScore", "averageScore", "averagePercent"})
    public record GameStats(MinigameScore.Game game, int plays, int bestScore, double averageScore, double averagePercent) {
    }
}
