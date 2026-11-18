package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Placar dos minigames com MongoDB real: gravação, validação, ranking e estatísticas. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MinigameApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clearScores() {
        mongoTemplate.dropCollection("minigame_scores");
    }

    private ResultActions save(String game, String name, int score, int rounds) throws Exception {
        return mockMvc.perform(post("/api/minigames/scores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"game\":\"%s\",\"playerName\":\"%s\",\"score\":%d,\"rounds\":%d}".formatted(game, name, score, rounds)));
    }

    @Test
    void savesAScoreAndReturnsTheRankingPosition() throws Exception {
        save("NUMBER", "  Ana  ", 420, 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score.game").value("NUMBER"))
                .andExpect(jsonPath("$.score.playerName").value("Ana"))
                .andExpect(jsonPath("$.score.maxScore").value(500))
                .andExpect(jsonPath("$.position").value(1));
        save("NUMBER", "Bruno", 300, 5).andExpect(jsonPath("$.position").value(2));
        save("NUMBER", "Caio", 480, 5).andExpect(jsonPath("$.position").value(1));
        save("NUMBER", "", 100, 5).andExpect(jsonPath("$.score.playerName").value("Anônimo"));

        assertThat(mongoTemplate.getCollection("minigame_scores").countDocuments()).isEqualTo(4);
    }

    @Test
    void leaderboardIsPerGameAndOrderedByScore() throws Exception {
        save("TYPE", "Ana", 350, 5);
        save("TYPE", "Bruno", 500, 5);
        save("TYPE", "Caio", 350, 5);
        save("STAT", "Dora", 500, 5);

        mockMvc.perform(get("/api/minigames/scores").param("game", "TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].playerName", contains("Bruno", "Ana", "Caio")));
        mockMvc.perform(get("/api/minigames/scores").param("game", "TYPE").param("limit", "1"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/minigames/scores").param("game", "NUMBER")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void statsAggregateEveryGame() throws Exception {
        save("STAT", "Ana", 300, 5);
        save("STAT", "Bruno", 500, 5);
        save("NUMBER", "Caio", 90, 1);

        mockMvc.perform(get("/api/minigames/stats"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].game").value("TYPE"))
                .andExpect(jsonPath("$[0].plays").value(0))
                .andExpect(jsonPath("$[1].game").value("NUMBER"))
                .andExpect(jsonPath("$[1].averagePercent").value(90.0))
                .andExpect(jsonPath("$[2].game").value("STAT"))
                .andExpect(jsonPath("$[2].plays").value(2))
                .andExpect(jsonPath("$[2].bestScore").value(500))
                .andExpect(jsonPath("$[2].averageScore").value(400.0))
                .andExpect(jsonPath("$[2].averagePercent").value(80.0));
    }

    @Test
    void validatesTheScore() throws Exception {
        save("TYPE", "Ana", 501, 5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.score").value("A pontuação máxima em 5 rodadas é 500."));
        save("TYPE", "Ana", -1, 5).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.score").exists());
        save("TYPE", "Ana", 10, 0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.rounds").exists());
        save("XADREZ", "Ana", 10, 1).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/minigames/scores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.game").value("é obrigatório"));
        mockMvc.perform(get("/api/minigames/scores"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O parâmetro game é obrigatório."));
        mockMvc.perform(get("/api/minigames/scores").param("game", "XADREZ")).andExpect(status().isBadRequest());
    }
}
