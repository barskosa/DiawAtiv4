package com.pokedex.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Documento da coleção {@code minigame_scores}: o resultado de uma partida de minigame.
 *
 * @param maxScore pontuação máxima possível na partida (100 por rodada)
 */
@Document(collection = "minigame_scores")
@JsonPropertyOrder({"id", "game", "playerName", "score", "rounds", "maxScore", "createdAt"})
public record MinigameScore(@Id String id, Game game, String playerName, int score, int rounds, int maxScore,
        Instant createdAt) {

    public static final int POINTS_PER_ROUND = 100;

    /** Os três minigames da página. */
    public enum Game {
        /** Adivinhar o tipo do Pokémon. */
        TYPE,
        /** Adivinhar o número da Pokédex (pontos pela proximidade). */
        NUMBER,
        /** Adivinhar o maior atributo. */
        STAT
    }
}
