package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.bson.Document;
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
 * Sobe a aplicação completa com um MongoDB real (Testcontainers) e percorre o fluxo da Pokédex:
 * importação do Script_Pokedex.sql, consultas e o CRUD persistindo no banco.
 * É ignorado automaticamente quando o Docker não está disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PokedexApiIntegrationTest {

    private static final String NEW_POKEMON = """
            {"name":"Pikachu de Teste","hp":40,"attack":60,"defense":45,"specialAttack":70,"specialDefense":55,
             "speed":100,"types":["electric","Steel"]}
            """;

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
    void importsTheScriptIntoTheThreeCollections() throws Exception {
        assertThat(mongoTemplate.getCollection("pokemons").countDocuments()).isEqualTo(151);
        assertThat(mongoTemplate.getCollection("types").countDocuments()).isEqualTo(18);
        assertThat(mongoTemplate.getCollection("trainers").countDocuments()).isEqualTo(12);
        assertThat(mongoTemplate.getCollection("pokemons").find().first())
                .as("documentos sem o campo _class").doesNotContainKey("_class");

        mockMvc.perform(get("/api/pokemons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(151)))
                .andExpect(jsonPath("$[0].name").value("Bulbasaur"))
                .andExpect(jsonPath("$[150].name").value("Mew"));
    }

    @Test
    void findsPikachuLikeTheAssignmentExample() throws Exception {
        String pikachu = mockMvc.perform(get("/api/pokemons/25"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(pikachu).isEqualTo("""
                {"id":25,"name":"Pikachu","hp":35,"attack":55,"defense":30,"specialAttack":50,\
                "specialDefense":40,"speed":90,"types":["Electric"]}""");
    }

    @Test
    void searchesByNameAndType() throws Exception {
        mockMvc.perform(get("/api/pokemons").param("name", "CHAR"))
                .andExpect(jsonPath("$[*].name", contains("Charmander", "Charmeleon", "Charizard")));
        mockMvc.perform(get("/api/pokemons").param("name", "mr."))
                .andExpect(jsonPath("$[*].name", contains("Mr. Mime")));
        mockMvc.perform(get("/api/pokemons").param("type", "Fire").param("type", "flying"))
                .andExpect(jsonPath("$[*].name", contains("Charizard", "Moltres")));
        mockMvc.perform(get("/api/pokemons").param("type", "Typeless"))
                .andExpect(jsonPath("$[*].name", contains("Mew")));
        mockMvc.perform(get("/api/pokemons").param("type", "Dark"))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/pokemons").param("type", "Fairy"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsUpdatesAndDeletesPersistingInMongo() throws Exception {
        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content(NEW_POKEMON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pokemons/152")))
                .andExpect(jsonPath("$.id").value(152))
                .andExpect(jsonPath("$.types", contains("Electric", "Steel")));

        mockMvc.perform(get("/api/pokemons/152")).andExpect(jsonPath("$.name").value("Pikachu de Teste"));

        mockMvc.perform(put("/api/pokemons/152").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_POKEMON.replace("Pikachu de Teste", "Raichu de Teste").replace("\"hp\":40", "\"hp\":80")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Raichu de Teste"))
                .andExpect(jsonPath("$.hp").value(80));
        assertThat(
                mongoTemplate.getCollection("pokemons").find(new Document("_id", 152)).first())
                .containsEntry("name", "Raichu de Teste")
                .containsEntry("hp", 80);

        mockMvc.perform(delete("/api/pokemons/152")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/pokemons/152")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pokemons")).andExpect(jsonPath("$", hasSize(151)));
    }

    @Test
    void rejectsDuplicatedNumberAndName() throws Exception {
        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_POKEMON.replace("{", "{\"id\":25,")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.id").exists());
        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_POKEMON.replace("Pikachu de Teste", "pikachu")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.name").exists());
        mockMvc.perform(put("/api/pokemons/26").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_POKEMON.replace("Pikachu de Teste", "Pikachu")))
                .andExpect(status().isConflict());
    }

    @Test
    void keepsTheSameNameWhenEditingTheSamePokemon() throws Exception {
        mockMvc.perform(put("/api/pokemons/25").contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_POKEMON.replace("Pikachu de Teste", "Pikachu").replace("\"Steel\"", "\"Water\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types", contains("Electric", "Water")));
    }

    @Test
    void deletingAPokemonRemovesItFromTrainerTeams() throws Exception {
        mockMvc.perform(get("/api/trainers/6/pokemons"))
                .andExpect(jsonPath("$[*].name", contains("Venonat", "Venonat", "Venonat", "Venomoth")));
        mockMvc.perform(get("/api/pokemons/48/trainers"))
                .andExpect(jsonPath("$[*].name", contains("Koga")));

        mockMvc.perform(delete("/api/pokemons/48")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/trainers/6")).andExpect(jsonPath("$.pokemonIds", contains(49)));
    }

    @Test
    void servesTypesAndTrainers() throws Exception {
        mockMvc.perform(get("/api/types"))
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[0].name").value("Typeless"));
        mockMvc.perform(get("/api/trainers"))
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].name").value("Ash"))
                .andExpect(jsonPath("$[0].pokemonIds", contains(25, 3, 6, 9, 131, 143)));
        mockMvc.perform(get("/api/trainers/99")).andExpect(status().isNotFound());
    }
}
