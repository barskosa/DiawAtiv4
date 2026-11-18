package com.pokedex.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.pokedex.api.model.Battle;

/** Respostas resumidas da Arena PvP. */
public final class BattleViews {

    private BattleViews() {
    }

    /** Item do histórico ({@code GET /api/battles}). */
    @JsonPropertyOrder({"id", "status", "rounds", "winner", "finishReason", "player1", "player2", "createdAt", "finishedAt"})
    public record Summary(String id, Battle.Status status, int rounds, Integer winner, Battle.FinishReason finishReason,
            Side player1, Side player2, Instant createdAt, Instant finishedAt) {

        public static Summary of(Battle battle) {
            return new Summary(battle.getId(), battle.getStatus(), battle.getRound(), battle.getWinner(),
                    battle.getFinishReason(),
                    new Side(battle.getPlayer1().getPlayerName(), battle.getPlayer1().getPokemonId(),
                            battle.getPlayer1().getPokemonName(), battle.getPlayer1().getCurrentHp(), battle.getPlayer1().getMaxHp()),
                    new Side(battle.getPlayer2().getPlayerName(), battle.getPlayer2().getPokemonId(),
                            battle.getPlayer2().getPokemonName(), battle.getPlayer2().getCurrentHp(), battle.getPlayer2().getMaxHp()),
                    battle.getCreatedAt(), battle.getFinishedAt());
        }
    }

    @JsonPropertyOrder({"playerName", "pokemonId", "pokemonName", "currentHp", "maxHp"})
    public record Side(String playerName, Integer pokemonId, String pokemonName, int currentHp, int maxHp) {
    }

    /** Linha do ranking ({@code GET /api/battles/ranking}). */
    @JsonPropertyOrder({"pokemonId", "pokemonName", "wins", "battles", "winRate"})
    public record RankingEntry(Integer pokemonId, String pokemonName, int wins, int battles, double winRate) {
    }
}
