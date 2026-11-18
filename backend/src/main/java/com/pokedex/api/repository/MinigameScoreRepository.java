package com.pokedex.api.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.pokedex.api.model.MinigameScore;

public interface MinigameScoreRepository extends MongoRepository<MinigameScore, String> {

    /** Ranking de um jogo: maior pontuação primeiro; empate para quem fez antes. */
    List<MinigameScore> findByGameOrderByScoreDescCreatedAtAsc(MinigameScore.Game game, Pageable pageable);

    long countByGameAndScoreGreaterThan(MinigameScore.Game game, int score);
}
