package com.pokedex.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações próprias da aplicação, lidas de {@code application.properties}.
 */
@ConfigurationProperties(prefix = "pokedex")
public record PokedexProperties(Seed seed, Cors cors) {

    public PokedexProperties {
        seed = seed == null ? new Seed(null, null, null, null) : seed;
        cors = cors == null ? new Cors(null) : cors;
    }

    /**
     * @param script             o Script_Pokedex.sql da atividade (1ª geração e treinadores)
     * @param generationsScript  Pokémon da 2ª à 9ª geração, no mesmo formato
     * @param generations        se o segundo script deve ser importado
     */
    public record Seed(SeedMode mode, String script, String generationsScript, Boolean generations) {
        public Seed {
            mode = mode == null ? SeedMode.IF_EMPTY : mode;
            script = script == null || script.isBlank() ? "classpath:db/Script_Pokedex.sql" : script;
            generationsScript = generationsScript == null || generationsScript.isBlank()
                    ? "classpath:db/Script_Pokedex_Geracoes.sql" : generationsScript;
            generations = generations == null || generations;
        }
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public enum SeedMode {
        /** Importa o script apenas nas coleções que estiverem vazias. */
        IF_EMPTY,
        /** Apaga as coleções e importa o script novamente. */
        ALWAYS,
        /** Não importa dados. */
        NEVER
    }
}
