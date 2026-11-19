package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pokedex.api.config.MongoIndexes;

/** Os índices são criados na inicialização e recriar não causa erro. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MongoIndexesIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoIndexes mongoIndexes;

    private List<String> indexes(String collection) {
        return mongoTemplate.indexOps(collection).getIndexInfo().stream().map(IndexInfo::getName).toList();
    }

    @Test
    void createsTheIndexesUsedByTheQueries() {
        mongoIndexes.run(null); // idempotente: rodar de novo não falha

        assertThat(indexes("pokemons")).contains("types");
        assertThat(indexes("trainers")).contains("pokemonIds");
        assertThat(indexes("battles")).contains("createdAt_desc", "status_trainer");
        assertThat(indexes("minigame_scores")).contains("game_score_createdAt");
    }
}
