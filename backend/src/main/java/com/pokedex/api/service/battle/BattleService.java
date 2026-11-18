package com.pokedex.api.service.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.pokedex.api.dto.BattleRequests;
import com.pokedex.api.dto.BattleViews.Summary;
import com.pokedex.api.dto.BattleViews.TrainerStats;
import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.exception.ResourceNotFoundException;
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.model.BattleSide;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.repository.BattleRepository;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.service.TrainerService;

/**
 * Desafios contra os treinadores: sorteio do time do jogador (mesmo tamanho da equipe do treinador),
 * rodadas, desistência, histórico e estatísticas. Cada alteração é gravada na coleção {@code battles}.
 */
@Service
public class BattleService {

    /** Só batalhas no formato de desafio (ignora documentos antigos da Arena PvP). */
    private static final Criteria CHALLENGES = Criteria.where("trainer").exists(true);

    private final BattleRepository battleRepository;
    private final PokemonRepository pokemonRepository;
    private final TrainerService trainerService;
    private final BattleEngine engine;
    private final MongoTemplate mongoTemplate;
    private final RandomGenerator random;

    public BattleService(BattleRepository battleRepository, PokemonRepository pokemonRepository,
            TrainerService trainerService, BattleEngine engine, MongoTemplate mongoTemplate, RandomGenerator random) {
        this.battleRepository = battleRepository;
        this.pokemonRepository = pokemonRepository;
        this.trainerService = trainerService;
        this.engine = engine;
        this.mongoTemplate = mongoTemplate;
        this.random = random;
    }

    public Battle challenge(BattleRequests.Challenge request) {
        Trainer trainer = trainerService.findById(request.trainerId());
        List<Pokemon> trainerTeam = trainerService.findTeam(trainer.id());
        if (trainerTeam.isEmpty()) {
            throw new InvalidRequestException("trainerId", trainer.name() + " não tem Pokémon na equipe para batalhar.");
        }

        Battle battle = new Battle();
        battle.setPlayerName(playerName(request.playerName()));
        battle.setTrainer(new Battle.TrainerInfo(trainer.id(), trainer.name(), trainer.badges()));
        battle.setTrainerTeam(trainerTeam.stream().map(BattleFighter::of).toList());
        battle.setPlayerTeam(randomTeam(trainerTeam.size()));
        battle.setCreatedAt(Instant.now());
        engine.start(battle);
        return battleRepository.insert(battle);
    }

    /** Sorteia outro time para o jogador, permitido só antes da primeira rodada. */
    public Battle rerollTeam(String id) {
        Battle battle = findById(id);
        if (battle.isFinished() || battle.getRound() > 0) {
            throw new ConflictException("battle", "O time só pode ser sorteado de novo antes da primeira rodada.");
        }
        battle.setPlayerTeam(randomTeam(battle.getTrainerTeam().size()));
        engine.start(battle);
        return battleRepository.save(battle);
    }

    public Battle findById(String id) {
        return battleRepository.findById(id)
                .filter(battle -> battle.getTrainer() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Batalha " + id + " não encontrada."));
    }

    public Battle playRound(String id, BattleRequests.Round request) {
        Battle battle = findById(id);
        engine.playRound(battle, request.action(), request.switchTo());
        return battleRepository.save(battle);
    }

    public Battle forfeit(String id) {
        Battle battle = findById(id);
        engine.forfeit(battle);
        return battleRepository.save(battle);
    }

    /** Batalhas mais recentes primeiro. */
    public List<Summary> recent(int limit) {
        Query query = new Query(CHALLENGES).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(Math.clamp(limit, 1, 50));
        return mongoTemplate.find(query, Battle.class).stream().map(Summary::of).toList();
    }

    /**
     * Desempenho do jogador contra cada treinador, calculado no MongoDB com um pipeline de agregação:
     * filtra as batalhas encerradas ($match), agrupa por treinador somando batalhas e vitórias ($group)
     * e ordena pelos treinadores mais desafiados ($sort).
     */
    public List<TrainerStats> stats() {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("status", Battle.Status.FINISHED.name())
                        .append("trainer", new Document("$exists", true))),
                new Document("$group", new Document("_id", "$trainer._id")
                        .append("trainerName", new Document("$last", "$trainer.name"))
                        .append("badges", new Document("$last", "$trainer.badges"))
                        .append("battles", new Document("$sum", 1))
                        .append("playerWins", new Document("$sum", new Document("$cond",
                                List.of(new Document("$eq", List.of("$winner", BattleSide.PLAYER.name())), 1, 0))))),
                new Document("$sort", new Document("battles", -1).append("_id", 1)));

        List<TrainerStats> stats = new ArrayList<>();
        for (Document row : mongoTemplate.getCollection("battles").aggregate(pipeline)) {
            int battles = row.getInteger("battles");
            int playerWins = row.getInteger("playerWins");
            stats.add(new TrainerStats(row.getInteger("_id"), row.getString("trainerName"), row.getInteger("badges"),
                    battles, playerWins, battles - playerWins, Math.round(playerWins * 1000.0 / battles) / 10.0));
        }
        return stats;
    }

    /** Pokémon distintos sorteados da Pokédex (embaralhamento de Fisher–Yates). */
    private List<BattleFighter> randomTeam(int size) {
        List<Pokemon> pool = new ArrayList<>(pokemonRepository.findAll());
        if (pool.size() < size) {
            throw new InvalidRequestException("trainerId", "A Pokédex não tem Pokémon suficientes para montar um time de "
                    + size + ".");
        }
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Pokemon swap = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, swap);
        }
        return pool.subList(0, size).stream().map(BattleFighter::of).toList();
    }

    private static String playerName(String name) {
        return name == null || name.isBlank() ? "Você" : name.trim().replaceAll("\\s+", " ");
    }
}
