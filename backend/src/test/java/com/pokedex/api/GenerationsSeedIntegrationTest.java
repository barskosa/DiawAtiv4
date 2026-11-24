package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.pokedex.api.seed.DatabaseSeeder;

/**
 * Importação do Script_Pokedex_Geracoes.sql (Pokémon #152 a #1025 e o tipo Fada) junto com o
 * Script_Pokedex.sql da atividade.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GenerationsSeedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DatabaseSeeder seeder;

    @BeforeEach
    void restoreScriptData() throws Exception {
        mongoTemplate.getDb().drop();
        seeder.run(null);
    }

    @Test
    void importsAllNineGenerations() throws Exception {
        assertThat(mongoTemplate.getCollection("pokemons").countDocuments()).isEqualTo(1025);
        assertThat(mongoTemplate.getCollection("types").countDocuments()).isEqualTo(19);
        assertThat(mongoTemplate.getCollection("trainers").countDocuments()).isEqualTo(12);

        // A 1ª geração continua vindo do script da atividade
        mockMvc.perform(get("/api/pokemons/25")).andExpect(jsonPath("$.name").value("Pikachu"));
        mockMvc.perform(get("/api/pokemons/152")).andExpect(jsonPath("$.name").value("Chikorita"));
        mockMvc.perform(get("/api/pokemons/1025")).andExpect(jsonPath("$.name").value("Pecharunt"));
        mockMvc.perform(get("/api/pokemons/865")).andExpect(jsonPath("$.name").value("Sirfetch'd"));
        mockMvc.perform(get("/api/pokemons").param("type", "Fairy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("Togepi", "Sylveon")));
        mockMvc.perform(get("/api/pokemons").param("type", "Typeless"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void doesNotBringBackADeletedPokemonOnRestart() throws Exception {
        mockMvc.perform(delete("/api/pokemons/700")).andExpect(status().isNoContent());

        seeder.run(null);

        mockMvc.perform(get("/api/pokemons/700")).andExpect(status().isNotFound());
        assertThat(mongoTemplate.getCollection("pokemons").countDocuments()).isEqualTo(1024);
    }

    @Test
    void newPokemonGetsTheNextFreeNumberAfterTheNinthGeneration() throws Exception {
        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Pokémon de Teste","hp":50,"attack":50,"defense":50,"specialAttack":50,
                         "specialDefense":50,"speed":50,"types":["Fairy"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1026))
                .andExpect(jsonPath("$.types[0]").value("Fairy"));
    }
}
