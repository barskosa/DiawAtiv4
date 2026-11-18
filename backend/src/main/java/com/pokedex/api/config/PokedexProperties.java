package com.pokedex.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações próprias da aplicação, lidas de {@code application.properties}.
 */
@ConfigurationProperties(prefix = "pokedex")
public record PokedexProperties(Seed seed, Cors cors) {

    public PokedexProperties {
        seed = seed == null ? new Seed(null, null) : seed;
        cors = cors == null ? new Cors(null) : cors;
    }

    public record Seed(SeedMode mode, String script) {
        public Seed {
            mode = mode == null ? SeedMode.IF_EMPTY : mode;
            script = script == null || script.isBlank() ? "classpath:db/Script_Pokedex.sql" : script;
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
