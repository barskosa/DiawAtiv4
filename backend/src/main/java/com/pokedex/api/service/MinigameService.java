package com.pokedex.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.pokedex.api.dto.MinigameDtos.DailyMinigame;
import com.pokedex.api.dto.MinigameDtos.DailyRankEntry;
import com.pokedex.api.dto.MinigameDtos.GameStats;
import com.pokedex.api.dto.MinigameDtos.SavedScore;
import com.pokedex.api.dto.MinigameDtos.ScoreRequest;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.MinigameScore;
import com.pokedex.api.repository.MinigameScoreRepository;

/**
 * Placar dos minigames: grava o resultado de cada partida e monta o ranking e as estatísticas
 * de cada jogo. As partidas em si acontecem no navegador, com Pokémon sorteados da API.
 *
 * <p>O desafio do dia usa os mesmos Pokémon para todos ({@link DailyChallengeService}) e tem um
 * ranking próprio, com a melhor partida de cada jogador naquela data.</p>
 */
@Service
public class MinigameService {

    private final MinigameScoreRepository repository;
    private final MongoTemplate mongoTemplate;
    private final DailyChallengeService daily;

    public MinigameService(MinigameScoreRepository repository, MongoTemplate mongoTemplate, DailyChallengeService daily) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daily = daily;
    }

    public DailyMinigame dailyChallenge(MinigameScore.Game game) {
        LocalDate date = daily.today();
        return new DailyMinigame(date, game, MinigameScore.DAILY_ROUNDS,
                daily.pokemonsOfTheDay(date, "MINIGAME_" + game.name(), MinigameScore.DAILY_ROUNDS));
    }

    public SavedScore save(ScoreRequest request) {
        if (request.isDaily() && request.rounds() != MinigameScore.DAILY_ROUNDS) {
            throw new InvalidRequestException("rounds", "O desafio do dia tem " + MinigameScore.DAILY_ROUNDS + " rodadas.");
        }
        int maxScore = request.rounds() * MinigameScore.POINTS_PER_ROUND;
        if (request.score() > maxScore) {
            throw new InvalidRequestException("score", "A pontuação máxima em " + request.rounds() + " rodadas é "
                    + maxScore + ".");
        }
        String name = request.playerName() == null || request.playerName().isBlank()
                ? "Anônimo" : request.playerName().trim().replaceAll("\\s+", " ");
        String dailyDate = request.isDaily() ? daily.today().toString() : null;
        MinigameScore saved = repository.insert(new MinigameScore(null, request.game(), name, request.score(),
                request.rounds(), maxScore, dailyDate, Instant.now()));
        if (dailyDate != null) {
            List<DailyRankEntry> ranking = dailyRanking(saved.game(), daily.today(), 50);
            long position = ranking.stream().filter(entry -> entry.playerName().equals(name)).findFirst()
                    .map(DailyRankEntry::position).orElse(ranking.size() + 1);
            return new SavedScore(saved, position);
        }
        long position = repository.countByGameAndScoreGreaterThan(saved.game(), saved.score()) + 1;
        return new SavedScore(saved, position);
    }

    public List<MinigameScore> leaderboard(MinigameScore.Game game, int limit) {
        return repository.findByGameOrderByScoreDescCreatedAtAsc(game, PageRequest.of(0, Math.clamp(limit, 1, 50)));
    }

    /**
     * Ranking do dia: a melhor partida de cada jogador, calculada no MongoDB ($match pela data,
     * $sort pela pontuação, $group pelo jogador mantendo a primeira partida e $sort final).
     */
    public List<DailyRankEntry> dailyRanking(MinigameScore.Game game, LocalDate date, int limit) {
        LocalDate day = date == null ? daily.today() : date;
        List<Document> pipeline = List.of(
                new Document("$match", new Document("game", game.name()).append("dailyDate", day.toString())),
                new Document("$sort", new Document("score", -1).append("createdAt", 1)),
                new Document("$group", new Document("_id", "$playerName")
                        .append("score", new Document("$first", "$score"))
                        .append("maxScore", new Document("$first", "$maxScore"))
                        .append("createdAt", new Document("$first", "$createdAt"))),
                new Document("$sort", new Document("score", -1).append("createdAt", 1)),
                new Document("$limit", Math.clamp(limit, 1, 50)));

        List<DailyRankEntry> ranking = new ArrayList<>();
        for (Document row : mongoTemplate.getCollection("minigame_scores").aggregate(pipeline)) {
            ranking.add(new DailyRankEntry(ranking.size() + 1, row.getString("_id"), row.getInteger("score"),
                    row.getInteger("maxScore"), row.getDate("createdAt").toInstant()));
        }
        return ranking;
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
