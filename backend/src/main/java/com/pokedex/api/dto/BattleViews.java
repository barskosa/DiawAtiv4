package com.pokedex.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.model.BattleSide;
import com.pokedex.api.model.Pokemon;

/** Respostas resumidas das batalhas. */
public final class BattleViews {

    private BattleViews() {
    }

    /** Item do histórico ({@code GET /api/battles}). */
    @JsonPropertyOrder({"id", "status", "rounds", "winner", "finishReason", "playerName", "dailyDate", "trainer",
            "playerTeam", "playerRemaining", "trainerRemaining", "createdAt", "finishedAt"})
    public record Summary(String id, Battle.Status status, int rounds, BattleSide winner, Battle.FinishReason finishReason,
            String playerName, String dailyDate, Battle.TrainerInfo trainer, List<String> playerTeam, long playerRemaining,
            long trainerRemaining, Instant createdAt, Instant finishedAt) {

        public static Summary of(Battle battle) {
            return new Summary(battle.getId(), battle.getStatus(), battle.getRound(), battle.getWinner(),
                    battle.getFinishReason(), battle.getPlayerName(), battle.getDailyDate(), battle.getTrainer(),
                    battle.getPlayerTeam().stream().map(BattleFighter::getPokemonName).toList(),
                    battle.remaining(BattleSide.PLAYER), battle.remaining(BattleSide.TRAINER),
                    battle.getCreatedAt(), battle.getFinishedAt());
        }
    }

    /** {@code GET /api/battles/daily}: treinador e time do desafio do dia (iguais para todos). */
    @JsonPropertyOrder({"date", "trainer", "trainerTeam", "playerTeam"})
    public record DailyBattle(LocalDate date, Battle.TrainerInfo trainer, List<Integer> trainerTeam,
            List<Pokemon> playerTeam) {
    }

    /**
     * Linha do ranking do dia: a melhor vitória de cada jogador (menos rodadas e, no empate, mais HP
     * sobrando no time).
     */
    @JsonPropertyOrder({"position", "playerName", "rounds", "hpPercent", "battleId", "finishedAt"})
    public record DailyRankEntry(int position, String playerName, int rounds, double hpPercent, String battleId,
            Instant finishedAt) {
    }

    /** Desempenho contra cada treinador ({@code GET /api/battles/stats}). */
    @JsonPropertyOrder({"trainerId", "trainerName", "badges", "battles", "playerWins", "trainerWins", "playerWinRate"})
    public record TrainerStats(Integer trainerId, String trainerName, int badges, int battles, int playerWins,
            int trainerWins, double playerWinRate) {
    }
}
