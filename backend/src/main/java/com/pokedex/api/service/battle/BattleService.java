package com.pokedex.api.service.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.pokedex.api.dto.BattleRequests;
import com.pokedex.api.dto.BattleViews.RankingEntry;
import com.pokedex.api.dto.BattleViews.Summary;
import com.pokedex.api.exception.ResourceNotFoundException;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.repository.BattleRepository;
import com.pokedex.api.service.PokemonService;

/**
 * Casos de uso da Arena PvP: criar batalhas, jogar rodadas, desistir, histórico e ranking.
 * Cada alteração é gravada na coleção {@code battles}.
 */
@Service
public class BattleService {

    private final BattleRepository battleRepository;
    private final PokemonService pokemonService;
    private final BattleEngine engine;
    private final MongoTemplate mongoTemplate;

    public BattleService(BattleRepository battleRepository, PokemonService pokemonService, BattleEngine engine,
            MongoTemplate mongoTemplate) {
        this.battleRepository = battleRepository;
        this.pokemonService = pokemonService;
        this.engine = engine;
        this.mongoTemplate = mongoTemplate;
    }

    public Battle create(BattleRequests.CreateBattle request) {
        Battle battle = new Battle();
        battle.setPlayer1(BattleFighter.of(playerName(request.player1().name(), 1),
                pokemonService.findById(request.player1().pokemonId())));
        battle.setPlayer2(BattleFighter.of(playerName(request.player2().name(), 2),
                pokemonService.findById(request.player2().pokemonId())));
        battle.setCreatedAt(Instant.now());
        engine.start(battle);
        return battleRepository.insert(battle);
    }

    public Battle findById(String id) {
        return battleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batalha " + id + " não encontrada."));
    }

    public Battle playRound(String id, BattleRequests.Round request) {
        Battle battle = findById(id);
        engine.playRound(battle, request.player1Action(), request.player2Action());
        return battleRepository.save(battle);
    }

    public Battle forfeit(String id, int player) {
        Battle battle = findById(id);
        engine.forfeit(battle, player);
        return battleRepository.save(battle);
    }

    /** Batalhas mais recentes primeiro. */
    public List<Summary> recent(int limit) {
        int size = Math.clamp(limit, 1, 50);
        return battleRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(Summary::of).getContent();
    }

    /**
     * Vitórias por Pokémon nas batalhas encerradas, calculadas no próprio MongoDB com um pipeline de
     * agregação: cada batalha vira dois participantes ($project + $unwind), que são agrupados por
     * Pokémon ($group) e ordenados pelo número de vitórias.
     */
    public List<RankingEntry> ranking(int limit) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("status", Battle.Status.FINISHED.name())),
                new Document("$project", new Document("participants", List.of(
                        participant("$player1", 1),
                        participant("$player2", 2)))),
                new Document("$unwind", "$participants"),
                new Document("$group", new Document("_id", "$participants.pokemonId")
                        .append("pokemonName", new Document("$last", "$participants.pokemonName"))
                        .append("wins", new Document("$sum", "$participants.won"))
                        .append("battles", new Document("$sum", 1))),
                new Document("$sort", new Document("wins", -1).append("battles", 1).append("_id", 1)),
                new Document("$limit", Math.clamp(limit, 1, 50)));

        List<RankingEntry> ranking = new ArrayList<>();
        for (Document row : mongoTemplate.getCollection("battles").aggregate(pipeline)) {
            int wins = row.getInteger("wins");
            int battles = row.getInteger("battles");
            ranking.add(new RankingEntry(row.getInteger("_id"), row.getString("pokemonName"), wins, battles,
                    Math.round(wins * 1000.0 / battles) / 10.0));
        }
        return ranking;
    }

    private static Document participant(String fighter, int player) {
        return new Document("pokemonId", fighter + ".pokemonId")
                .append("pokemonName", fighter + ".pokemonName")
                .append("won", new Document("$cond", List.of(new Document("$eq", List.of("$winner", player)), 1, 0)));
    }

    private static String playerName(String name, int player) {
        return name == null || name.isBlank() ? "Jogador " + player : name.trim().replaceAll("\\s+", " ");
    }
}
