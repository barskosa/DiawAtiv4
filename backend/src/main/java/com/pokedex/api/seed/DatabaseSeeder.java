package com.pokedex.api.seed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.pokedex.api.config.PokedexProperties;
import com.pokedex.api.config.PokedexProperties.SeedMode;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.PokemonType;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.repository.PokemonTypeRepository;
import com.pokedex.api.repository.TrainerRepository;
import com.pokedex.api.seed.PokedexSqlImporter.PokedexData;

/**
 * Popula o MongoDB a partir do {@code Script_Pokedex.sql} quando a aplicação inicia.
 *
 * <p>No modo padrão ({@code if-empty}) cada coleção só é importada se estiver vazia, então as
 * alterações feitas pela API são mantidas entre reinicializações.</p>
 *
 * <p>Em seguida importa o {@code Script_Pokedex_Geracoes.sql} (Pokémon #152 a #1025) uma única vez:
 * a importação fica registrada na coleção {@code seed_history}, para que um Pokémon excluído pela
 * API não volte na próxima inicialização.</p>
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final PokedexProperties properties;
    private final ResourceLoader resourceLoader;
    private final PokemonRepository pokemonRepository;
    private final PokemonTypeRepository typeRepository;
    private final TrainerRepository trainerRepository;
    private final MongoTemplate mongoTemplate;

    static final String SEED_HISTORY = "seed_history";
    static final String GENERATIONS_SEED = "Script_Pokedex_Geracoes.sql";

    public DatabaseSeeder(PokedexProperties properties, ResourceLoader resourceLoader,
            PokemonRepository pokemonRepository, PokemonTypeRepository typeRepository,
            TrainerRepository trainerRepository, MongoTemplate mongoTemplate) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.pokemonRepository = pokemonRepository;
        this.typeRepository = typeRepository;
        this.trainerRepository = trainerRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try {
            seed();
        } catch (DataAccessResourceFailureException exception) {
            // A URI não é exibida porque pode conter usuário e senha.
            throw new IllegalStateException("""
                    Não foi possível conectar ao MongoDB. Confira:
                      - MongoDB local: o container está no ar? (docker compose up -d)
                      - MongoDB Atlas: a MONGODB_URI (backend/.env) está correta e o seu IP foi liberado em Network Access?""",
                    exception);
        }
    }

    private void seed() throws IOException {
        SeedMode mode = properties.seed().mode();
        if (mode == SeedMode.NEVER) {
            log.info("Importação do Script_Pokedex.sql desativada (pokedex.seed.mode=never)");
            return;
        }
        if (mode == SeedMode.ALWAYS) {
            log.info("Apagando as coleções para reimportar o Script_Pokedex.sql (pokedex.seed.mode=always)");
            pokemonRepository.deleteAll();
            typeRepository.deleteAll();
            trainerRepository.deleteAll();
            mongoTemplate.getCollection(SEED_HISTORY).deleteMany(new Document());
        }
        seedActivityScript();
        if (properties.seed().generations()) {
            seedGenerations();
        }
    }

    private void seedActivityScript() throws IOException {
        boolean typesEmpty = typeRepository.count() == 0;
        boolean pokemonsEmpty = pokemonRepository.count() == 0;
        boolean trainersEmpty = trainerRepository.count() == 0;
        if (!typesEmpty && !pokemonsEmpty && !trainersEmpty) {
            log.info("MongoDB já populado: {} Pokémon, {} tipos, {} treinadores",
                    pokemonRepository.count(), typeRepository.count(), trainerRepository.count());
            return;
        }

        PokedexData data = PokedexSqlImporter.fromSql(readScript(properties.seed().script()));
        if (typesEmpty) {
            typeRepository.insert(data.types());
        }
        if (pokemonsEmpty) {
            pokemonRepository.insert(data.pokemons());
        }
        if (trainersEmpty) {
            trainerRepository.insert(data.trainers());
        }
        log.info("Script_Pokedex.sql importado: {} Pokémon, {} tipos, {} treinadores",
                pokemonsEmpty ? data.pokemons().size() : 0,
                typesEmpty ? data.types().size() : 0,
                trainersEmpty ? data.trainers().size() : 0);
    }

    /**
     * Acrescenta os tipos que faltam (Fada) e os Pokémon #152 a #1025. Números e nomes já usados
     * por Pokémon cadastrados pela API são mantidos e não são sobrescritos.
     */
    private void seedGenerations() throws IOException {
        var history = mongoTemplate.getCollection(SEED_HISTORY);
        if (history.find(new Document("_id", GENERATIONS_SEED)).first() != null) {
            return;
        }
        PokedexData data = PokedexSqlImporter.fromSql(readScript(properties.seed().generationsScript()));

        Set<Integer> typeIds = typeRepository.findAll().stream().map(PokemonType::id).collect(Collectors.toSet());
        List<PokemonType> newTypes = data.types().stream().filter(type -> !typeIds.contains(type.id())).toList();
        if (!newTypes.isEmpty()) {
            typeRepository.insert(newTypes);
        }

        List<Pokemon> existing = pokemonRepository.findAll();
        Set<Integer> ids = existing.stream().map(Pokemon::getId).collect(Collectors.toSet());
        Set<String> names = existing.stream().map(pokemon -> pokemon.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Pokemon> newPokemons = data.pokemons().stream()
                .filter(pokemon -> !ids.contains(pokemon.getId())
                        && !names.contains(pokemon.getName().toLowerCase(Locale.ROOT)))
                .toList();
        if (!newPokemons.isEmpty()) {
            pokemonRepository.insert(newPokemons);
        }
        history.insertOne(new Document("_id", GENERATIONS_SEED).append("pokemons", newPokemons.size())
                .append("importedAt", java.util.Date.from(Instant.now())));
        log.info("{} importado: {} Pokémon (2ª à 9ª geração), {} {}", GENERATIONS_SEED,
                newPokemons.size(), newTypes.size(), newTypes.size() == 1 ? "tipo novo" : "tipos novos");
    }

    private String readScript(String location) throws IOException {
        Resource script = resourceLoader.getResource(location);
        if (!script.exists()) {
            throw new IllegalStateException("Script de dados não encontrado em " + location
                    + ". Rode o backend com o Maven a partir da pasta backend/ ou defina POKEDEX_SEED_SCRIPT.");
        }
        try (InputStream input = script.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
