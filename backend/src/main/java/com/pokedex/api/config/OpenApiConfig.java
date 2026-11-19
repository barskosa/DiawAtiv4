package com.pokedex.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Informações exibidas na documentação interativa da API (Swagger UI em /swagger-ui.html). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI pokedexOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Pokédex API")
                .version("1.0")
                .description("API REST da Pokédex (Atividade 04): Pokémon, tipos e treinadores importados do "
                        + "Script_Pokedex.sql para o MongoDB, desafios contra os treinadores e placar dos minigames. "
                        + "Erros seguem o padrão Problem Details (RFC 9457)."));
    }
}
