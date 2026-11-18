package com.pokedex.api.seed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.pokedex.api.config.PokedexProperties;
import com.pokedex.api.config.PokedexProperties.SeedMode;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.repository.PokemonTypeRepository;
import com.pokedex.api.repository.TrainerRepository;
import com.pokedex.api.seed.PokedexSqlImporter.PokedexData;

/**
 * Popula o MongoDB a partir do {@code Script_Pokedex.sql} quando a aplicação inicia.
 *
 * <p>No modo padrão ({@code if-empty}) cada coleção só é importada se estiver vazia, então as
 * alterações feitas pela API são mantidas entre reinicializações.</p>
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final PokedexProperties properties;
    private final ResourceLoader resourceLoader;
    private final PokemonRepository pokemonRepository;
    private final PokemonTypeRepository typeRepository;
    private final TrainerRepository trainerRepository;

    public DatabaseSeeder(PokedexProperties properties, ResourceLoader resourceLoader,
            PokemonRepository pokemonRepository, PokemonTypeRepository typeRepository,
            TrainerRepository trainerRepository) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.pokemonRepository = pokemonRepository;
        this.typeRepository = typeRepository;
        this.trainerRepository = trainerRepository;
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
        }
        boolean typesEmpty = typeRepository.count() == 0;
        boolean pokemonsEmpty = pokemonRepository.count() == 0;
        boolean trainersEmpty = trainerRepository.count() == 0;
        if (!typesEmpty && !pokemonsEmpty && !trainersEmpty) {
            log.info("MongoDB já populado: {} Pokémon, {} tipos, {} treinadores",
                    pokemonRepository.count(), typeRepository.count(), trainerRepository.count());
            return;
        }

        PokedexData data = PokedexSqlImporter.fromSql(readScript());
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

    private String readScript() throws IOException {
        Resource script = resourceLoader.getResource(properties.seed().script());
        if (!script.exists()) {
            throw new IllegalStateException("Script de dados não encontrado em " + properties.seed().script()
                    + ". Rode o backend com o Maven a partir da pasta backend/ ou defina POKEDEX_SEED_SCRIPT.");
        }
        try (InputStream input = script.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
