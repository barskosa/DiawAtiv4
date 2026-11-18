package com.pokedex.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.pokedex.api.dto.MinigameDtos.GameStats;
import com.pokedex.api.dto.MinigameDtos.SavedScore;
import com.pokedex.api.dto.MinigameDtos.ScoreRequest;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.MinigameScore;
import com.pokedex.api.repository.MinigameScoreRepository;

/**
 * Placar dos minigames: grava o resultado de cada partida e monta o ranking e as estatísticas
 * de cada jogo. As partidas em si acontecem no navegador, com Pokémon sorteados da API.
 */
@Service
public class MinigameService {

    private final MinigameScoreRepository repository;
    private final MongoTemplate mongoTemplate;

    public MinigameService(MinigameScoreRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public SavedScore save(ScoreRequest request) {
        int maxScore = request.rounds() * MinigameScore.POINTS_PER_ROUND;
        if (request.score() > maxScore) {
            throw new InvalidRequestException("score", "A pontuação máxima em " + request.rounds() + " rodadas é "
                    + maxScore + ".");
        }
        String name = request.playerName() == null || request.playerName().isBlank()
                ? "Anônimo" : request.playerName().trim().replaceAll("\\s+", " ");
        MinigameScore saved = repository.insert(new MinigameScore(null, request.game(), name, request.score(),
                request.rounds(), maxScore, Instant.now()));
        long position = repository.countByGameAndScoreGreaterThan(saved.game(), saved.score()) + 1;
        return new SavedScore(saved, position);
    }

    public List<MinigameScore> leaderboard(MinigameScore.Game game, int limit) {
        return repository.findByGameOrderByScoreDescCreatedAtAsc(game, PageRequest.of(0, Math.clamp(limit, 1, 50)));
    }

    /**
     * Partidas, melhor pontuação e médias por jogo, calculadas no MongoDB ($group + $sort).
     * Jogos ainda sem partidas aparecem zerados.
     */
    public List<GameStats> stats() {
        List<Document> pipeline = List.of(
                new Document("$group", new Document("_id", "$game")
                        .append("plays", new Document("$sum", 1))
                        .append("bestScore", new Document("$max", "$score"))
                        .append("averageScore", new Document("$avg", "$score"))
                        .append("averagePercent", new Document("$avg",
                                new Document("$divide", List.of("$score", "$maxScore"))))),
                new Document("$sort", new Document("_id", 1)));

        List<GameStats> stats = new ArrayList<>();
        for (MinigameScore.Game game : MinigameScore.Game.values()) {
            stats.add(new GameStats(game, 0, 0, 0, 0));
        }
        for (Document row : mongoTemplate.getCollection("minigame_scores").aggregate(pipeline)) {
            MinigameScore.Game game = MinigameScore.Game.valueOf(row.getString("_id"));
            stats.set(game.ordinal(), new GameStats(game, row.getInteger("plays"), row.getInteger("bestScore"),
                    Math.round(row.getDouble("averageScore") * 10) / 10.0,
                    Math.round(row.getDouble("averagePercent") * 1000) / 10.0));
        }
        return stats;
    }
}
