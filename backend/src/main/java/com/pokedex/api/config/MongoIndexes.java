package com.pokedex.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Cria, na inicialização, os índices usados pelas consultas da aplicação. createIndex é
 * idempotente: se o índice já existe, nada muda.
 *
 * <ul>
 *   <li>pokemons.types: filtro por tipo ({@code ?type=Fire}).</li>
 *   <li>trainers.pokemonIds: treinadores que têm um Pokémon e o {@code $pull} ao excluí-lo.</li>
 *   <li>battles.createdAt: histórico (mais recentes primeiro).</li>
 *   <li>battles.status + trainer._id: estatísticas por treinador ($match/$group).</li>
 *   <li>minigame_scores.game + score + createdAt: ranking de cada minigame e posição do placar.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MongoIndexes implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexes.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexes(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        create("pokemons", new Index().on("types", Sort.Direction.ASC).named("types"));
        create("trainers", new Index().on("pokemonIds", Sort.Direction.ASC).named("pokemonIds"));
        create("battles", new Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc"));
        create("battles", new Index().on("status", Sort.Direction.ASC).on("trainer._id", Sort.Direction.ASC)
                .named("status_trainer"));
        create("minigame_scores", new Index().on("game", Sort.Direction.ASC).on("score", Sort.Direction.DESC)
                .on("createdAt", Sort.Direction.ASC).named("game_score_createdAt"));
        log.info("Índices do MongoDB conferidos");
    }

    private void create(String collection, Index index) {
        mongoTemplate.indexOps(collection).createIndex(index);
    }
}
