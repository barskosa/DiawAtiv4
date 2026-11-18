package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.pokedex.api.seed.DatabaseSeeder;

/** Desafios contra treinadores de ponta a ponta: API → serviço → motor → coleção battles no MongoDB real. */
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

    private ResultActions challenge(int trainerId) throws Exception {
        return mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":%d,\"playerName\":\"Ana\"}".formatted(trainerId)));
    }

    private String challengeId(int trainerId) throws Exception {
        return JsonPath.read(challenge(trainerId).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions round(String id, String body) throws Exception {
        return mockMvc.perform(post("/api/battles/" + id + "/rounds").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void playerTeamHasTheSameSizeAsTheTrainerTeam() throws Exception {
        challenge(2) // Brock: Geodude e Onix
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/battles/")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.playerName").value("Ana"))
                .andExpect(jsonPath("$.trainer.name").value("Brock"))
                .andExpect(jsonPath("$.trainer.badges").value(1))
                .andExpect(jsonPath("$.trainerTeam[*].pokemonName").value(org.hamcrest.Matchers.contains("Geodude", "Onix")))
                .andExpect(jsonPath("$.playerTeam", hasSize(2)))
                .andExpect(jsonPath("$.playerTeam[0].currentHp").isNumber());

        for (int[] trainer : new int[][] {{1, 6}, {6, 4}, {4, 1}, {12, 5}}) { // Ash, Koga, Lt. Surge, Lance
            String body = challenge(trainer[0]).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            List<Integer> playerIds = JsonPath.read(body, "$.playerTeam[*].pokemonId");
            assertThat(playerIds).hasSize(trainer[1]).doesNotHaveDuplicates();
            assertThat((List<?>) JsonPath.read(body, "$.trainerTeam")).hasSize(trainer[1]);
        }
        assertThat(mongoTemplate.getCollection("battles").countDocuments()).isEqualTo(5);
    }

    @Test
    void canRerollTheTeamOnlyBeforeTheFirstRound() throws Exception {
        String id = challengeId(1);

        mockMvc.perform(post("/api/battles/" + id + "/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerTeam", hasSize(6)))
                .andExpect(jsonPath("$.round").value(0));

        round(id, "{\"action\":\"DEFEND\"}").andExpect(status().isOk());
        mockMvc.perform(post("/api/battles/" + id + "/team")).andExpect(status().isConflict());
    }

    @Test
    void playsUntilOneSideRunsOutOfPokemon() throws Exception {
        String id = challengeId(3); // Misty: Staryu e Starmie

        String body = "";
        for (int i = 0; i < 200; i++) {
            body = round(id, "{\"action\":\"" + (i % 2 == 0 ? "SPECIAL" : "ATTACK") + "\"}")
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            if ("FINISHED".equals(JsonPath.read(body, "$.status"))) {
                break;
            }
        }

        assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("FINISHED");
        assertThat((String) JsonPath.read(body, "$.finishReason")).isEqualTo("KNOCKOUT");
        String loser = "PLAYER".equals(JsonPath.read(body, "$.winner")) ? "trainerTeam" : "playerTeam";
        assertThat((List<Integer>) JsonPath.read(body, "$." + loser + "[*].currentHp")).containsOnly(0);
        round(id, "{\"action\":\"ATTACK\"}").andExpect(status().isConflict());
    }

    @Test
    void switchesPokemonDuringTheBattle() throws Exception {
        String id = challengeId(1);

        round(id, "{\"action\":\"SWITCH\",\"switchTo\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerActive").value(3))
                .andExpect(jsonPath("$.log[0].action").value("SWITCH"))
                .andExpect(jsonPath("$.log[0].message").value(containsString("Ana trocou")));
        round(id, "{\"action\":\"SWITCH\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.switchTo").exists());
        round(id, "{\"action\":\"SWITCH\",\"switchTo\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.switchTo").value(containsString("já está em campo")));
    }

    @Test
    void validatesRequests() throws Exception {
        String id = challengeId(2);

        round(id, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.action").value("é obrigatório"));
        round(id, "{\"action\":\"VOAR\"}").andExpect(status().isBadRequest());
        challenge(99).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.trainerId").value("é obrigatório"));
        mockMvc.perform(get("/api/battles/naoexiste")).andExpect(status().isNotFound());
    }

    @Test
    void trainerWithoutPokemonCannotBeChallenged() throws Exception {
        mockMvc.perform(delete("/api/pokemons/74")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/pokemons/95")).andExpect(status().isNoContent());

        challenge(2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.trainerId").value("Brock não tem Pokémon na equipe para batalhar."));
    }

    @Test
    void battleKeepsASnapshotOfThePokemon() throws Exception {
        String id = challengeId(2);

        mockMvc.perform(put("/api/pokemons/95").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Onix Gigante","hp":250,"attack":45,"defense":160,"specialAttack":30,"specialDefense":45,"speed":70,"types":["Rock","Ground"]}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/battles/" + id))
                .andExpect(jsonPath("$.trainerTeam[1].pokemonName").value("Onix"))
                .andExpect(jsonPath("$.trainerTeam[1].maxHp").value(95));
    }

    @Test
    void historyAndStatsPerTrainer() throws Exception {
        mockMvc.perform(post("/api/battles/" + challengeId(2) + "/forfeit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winner").value("TRAINER"))
                .andExpect(jsonPath("$.finishReason").value("FORFEIT"));
        mockMvc.perform(post("/api/battles/" + challengeId(2) + "/forfeit")).andExpect(status().isOk());
        String misty = challengeId(3);
        mockMvc.perform(post("/api/battles/" + misty + "/forfeit")).andExpect(status().isOk());
        mockMvc.perform(post("/api/battles/" + misty + "/forfeit")).andExpect(status().isConflict());
        challengeId(1); // em andamento: fora das estatísticas
        // Documento antigo da Arena PvP (sem treinador) é ignorado
        mongoTemplate.getCollection("battles").insertOne(new Document("status", "FINISHED").append("winner", 1));

        mockMvc.perform(get("/api/battles"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].trainer.name").value("Ash"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].playerTeam", hasSize(6)))
                .andExpect(jsonPath("$[1].trainer.name").value("Misty"))
                .andExpect(jsonPath("$[1].winner").value("TRAINER"));
        mockMvc.perform(get("/api/battles").param("limit", "2")).andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/battles/stats"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].trainerName").value("Brock"))
                .andExpect(jsonPath("$[0].battles").value(2))
                .andExpect(jsonPath("$[0].playerWins").value(0))
                .andExpect(jsonPath("$[0].trainerWins").value(2))
                .andExpect(jsonPath("$[0].badges").value(1))
                .andExpect(jsonPath("$[1].trainerName").value("Misty"))
                .andExpect(jsonPath("$[1].playerWinRate").value(0.0));
    }
}
