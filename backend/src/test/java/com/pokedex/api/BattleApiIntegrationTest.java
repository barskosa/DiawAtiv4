package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.pokedex.api.seed.DatabaseSeeder;

/** Arena PvP de ponta a ponta: API → serviço → motor → coleção battles no MongoDB real. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BattleApiIntegrationTest {

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

    private ResultActions createBattle(int pokemon1, int pokemon2) throws Exception {
        return mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON).content("""
                {"player1":{"name":"Ana","pokemonId":%d},"player2":{"pokemonId":%d}}""".formatted(pokemon1, pokemon2)));
    }

    private String createBattleId(int pokemon1, int pokemon2) throws Exception {
        String body = createBattle(pokemon1, pokemon2).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions round(String id, String action1, String action2) throws Exception {
        return mockMvc.perform(post("/api/battles/" + id + "/rounds").contentType(MediaType.APPLICATION_JSON)
                .content("{\"player1Action\":\"%s\",\"player2Action\":\"%s\"}".formatted(action1, action2)));
    }

    private ResultActions forfeit(String id, int player) throws Exception {
        return mockMvc.perform(post("/api/battles/" + id + "/forfeit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"player\":" + player + "}"));
    }

    @Test
    void createsABattleWithFullHpAndSavesIt() throws Exception {
        createBattle(25, 7)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/battles/")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.round").value(0))
                .andExpect(jsonPath("$.player1.playerName").value("Ana"))
                .andExpect(jsonPath("$.player2.playerName").value("Jogador 2"))
                .andExpect(jsonPath("$.player1.pokemonName").value("Pikachu"))
                .andExpect(jsonPath("$.player1.maxHp").value(95))
                .andExpect(jsonPath("$.player1.currentHp").value(95))
                .andExpect(jsonPath("$.player2.types[0]").value("Water"));

        assertThat(mongoTemplate.getCollection("battles").countDocuments()).isEqualTo(1);
    }

    @Test
    void playsRoundsUntilSomeoneWins() throws Exception {
        String id = createBattleId(6, 9); // Charizard x Blastoise

        String body = "";
        for (int i = 0; i < 100; i++) {
            body = round(id, "SPECIAL", "ATTACK").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            if ("FINISHED".equals(JsonPath.read(body, "$.status"))) {
                break;
            }
        }

        assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("FINISHED");
        assertThat((String) JsonPath.read(body, "$.finishReason")).isEqualTo("KNOCKOUT");
        assertThat((String) JsonPath.read(body, "$.log[0].moveName")).isIn("Golpe Fogo", "Investida");
        mockMvc.perform(get("/api/battles/" + id)).andExpect(jsonPath("$.status").value("FINISHED"));
        round(id, "ATTACK", "ATTACK").andExpect(status().isConflict());
    }

    @Test
    void validatesActionsAndIds() throws Exception {
        String id = createBattleId(25, 7);

        round(id, "DEFEND", "ATTACK").andExpect(status().isOk());
        round(id, "DEFEND", "ATTACK")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.player1Action").value("Pikachu não pode defender duas rodadas seguidas."));
        round(id, "VOAR", "ATTACK").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/battles/" + id + "/rounds").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.player1Action").value("é obrigatório"));
        createBattle(25, 999).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON).content("{\"player1\":{\"pokemonId\":25}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.player2").value("é obrigatório"));
        mockMvc.perform(get("/api/battles/naoexiste")).andExpect(status().isNotFound());
        forfeit(id, 3).andExpect(status().isBadRequest());
    }

    @Test
    void battleKeepsASnapshotOfThePokemon() throws Exception {
        String id = createBattleId(25, 7);

        mockMvc.perform(put("/api/pokemons/25").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Pikachu Gigante","hp":200,"attack":55,"defense":30,"specialAttack":50,"specialDefense":40,"speed":90,"types":["Electric"]}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/battles/" + id))
                .andExpect(jsonPath("$.player1.pokemonName").value("Pikachu"))
                .andExpect(jsonPath("$.player1.maxHp").value(95));
    }

    @Test
    void historyAndRankingComeFromFinishedBattles() throws Exception {
        String first = createBattleId(25, 7);   // Pikachu x Squirtle
        forfeit(first, 2).andExpect(status().isOk()).andExpect(jsonPath("$.winner").value(1))
                .andExpect(jsonPath("$.finishReason").value("FORFEIT"));
        String second = createBattleId(25, 4);  // Pikachu x Charmander
        forfeit(second, 1).andExpect(jsonPath("$.winner").value(2));
        createBattleId(1, 150);                 // em andamento: fora do ranking
        forfeit(first, 1).andExpect(status().isConflict());

        mockMvc.perform(get("/api/battles"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].player1.pokemonName").value("Bulbasaur"))
                .andExpect(jsonPath("$[1].winner").value(2));
        mockMvc.perform(get("/api/battles").param("limit", "1")).andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/battles/ranking"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].pokemonName").value("Charmander"))
                .andExpect(jsonPath("$[0].wins").value(1))
                .andExpect(jsonPath("$[0].battles").value(1))
                .andExpect(jsonPath("$[0].winRate").value(100.0))
                .andExpect(jsonPath("$[1].pokemonName").value("Pikachu"))
                .andExpect(jsonPath("$[1].wins").value(1))
                .andExpect(jsonPath("$[1].battles").value(2))
                .andExpect(jsonPath("$[1].winRate").value(50.0))
                .andExpect(jsonPath("$[2].pokemonName").value("Squirtle"))
                .andExpect(jsonPath("$[2].wins").value(0));
    }
}
