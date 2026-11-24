package com.pokedex.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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
import com.pokedex.api.model.Battle;
import com.pokedex.api.model.BattleFighter;
import com.pokedex.api.model.BattleSide;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.service.DailyChallengeService;

/** Desafios do dia: mesmo conteúdo para todos na mesma data e rankings diários. */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DailyChallengeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DailyChallengeService daily;

    @BeforeEach
    void clearResults() {
        mongoTemplate.dropCollection("minigame_scores");
        mongoTemplate.dropCollection("battles");
    }

    private ResultActions saveDaily(String game, String name, int score, int rounds) throws Exception {
        return mockMvc.perform(post("/api/minigames/scores").contentType(MediaType.APPLICATION_JSON).content(
                "{\"game\":\"%s\",\"playerName\":\"%s\",\"score\":%d,\"rounds\":%d,\"daily\":true}"
                        .formatted(game, name, score, rounds)));
    }

    @Test
    void theDailyMinigameIsTheSameForEveryone() throws Exception {
        String first = mockMvc.perform(get("/api/minigames/daily").param("game", "TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(daily.today().toString()))
                .andExpect(jsonPath("$.rounds").value(5))
                .andExpect(jsonPath("$.pokemon", hasSize(5)))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/minigames/daily").param("game", "TYPE"))
                .andReturn().getResponse().getContentAsString();
        String otherGame = mockMvc.perform(get("/api/minigames/daily").param("game", "STAT"))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(first, "$.pokemon[*].id");
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(JsonPath.<List<Integer>>read(second, "$.pokemon[*].id")).isEqualTo(ids);
        assertThat(JsonPath.<List<Integer>>read(otherGame, "$.pokemon[*].id")).isNotEqualTo(ids);
    }

    @Test
    void theDailyRankingKeepsTheBestScoreOfEachPlayer() throws Exception {
        saveDaily("TYPE", "Ana", 300, 5).andExpect(status().isCreated())
                .andExpect(jsonPath("$.score.dailyDate").value(daily.today().toString()))
                .andExpect(jsonPath("$.position").value(1));
        saveDaily("TYPE", "Bia", 400, 5).andExpect(jsonPath("$.position").value(1));
        saveDaily("TYPE", "Ana", 450, 5).andExpect(jsonPath("$.position").value(1));
        saveDaily("NUMBER", "Caio", 500, 5);
        // Partida livre não entra no ranking do dia
        mockMvc.perform(post("/api/minigames/scores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"game\":\"TYPE\",\"playerName\":\"Duda\",\"score\":500,\"rounds\":5}"));

        mockMvc.perform(get("/api/minigames/daily/ranking").param("game", "TYPE"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName").value("Ana"))
                .andExpect(jsonPath("$[0].score").value(450))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].playerName").value("Bia"));
        mockMvc.perform(get("/api/minigames/daily/ranking").param("game", "TYPE").param("date", "2020-01-01"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void theDailyMinigameHasFiveRounds() throws Exception {
        saveDaily("STAT", "Ana", 200, 3)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rounds").exists());
    }

    @Test
    void theDailyBattleUsesTheTrainerAndTeamOfTheDay() throws Exception {
        String challenge = mockMvc.perform(get("/api/battles/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(daily.today().toString()))
                .andReturn().getResponse().getContentAsString();
        int trainerId = JsonPath.read(challenge, "$.trainer.id");
        List<Integer> trainerTeam = JsonPath.read(challenge, "$.trainerTeam");
        List<Integer> playerTeam = JsonPath.read(challenge, "$.playerTeam[*].id");
        assertThat(playerTeam).hasSameSizeAs(trainerTeam);

        String battle = mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Ana\",\"daily\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dailyDate").value(daily.today().toString()))
                .andExpect(jsonPath("$.trainer.id").value(trainerId))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(battle, "$.playerTeam[*].pokemonId")).isEqualTo(playerTeam);

        String id = JsonPath.read(battle, "$.id");
        mockMvc.perform(post("/api/battles/" + id + "/team")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/battles").contentType(MediaType.APPLICATION_JSON).content("{\"playerName\":\"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.trainerId").exists());
    }

    @Test
    void theDailyBattleRankingPrefersFewerRoundsThenMoreHp() throws Exception {
        String today = daily.today().toString();
        insertBattle("Ana", today, BattleSide.PLAYER, 9, 50);
        insertBattle("Ana", today, BattleSide.PLAYER, 7, 10);
        insertBattle("Bia", today, BattleSide.PLAYER, 7, 80);
        insertBattle("Caio", today, BattleSide.TRAINER, 3, 0);
        insertBattle("Duda", "2020-01-01", BattleSide.PLAYER, 2, 100);

        mockMvc.perform(get("/api/battles/daily/ranking"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName").value("Bia"))
                .andExpect(jsonPath("$[0].rounds").value(7))
                .andExpect(jsonPath("$[0].hpPercent").value(80.0))
                .andExpect(jsonPath("$[1].playerName").value("Ana"))
                .andExpect(jsonPath("$[1].rounds").value(7));
    }

    /** Batalha encerrada com o time do jogador em {@code hpPercent}% do HP. */
    private void insertBattle(String player, String date, BattleSide winner, int rounds, int hpPercent) {
        BattleFighter fighter = BattleFighter.of(new Pokemon(1, "Bulbasaur", 100, 49, 49, 65, 65, 45, List.of("Grass")));
        fighter.setMaxHp(200);
        fighter.setCurrentHp(200 * hpPercent / 100);
        Battle battle = new Battle();
        battle.setPlayerName(player);
        battle.setDailyDate(date);
        battle.setTrainer(new Battle.TrainerInfo(3, "Misty", 2));
        battle.setPlayerTeam(List.of(fighter));
        battle.setTrainerTeam(List.of());
        battle.setRound(rounds);
        battle.setCreatedAt(Instant.now());
        battle.finish(winner, Battle.FinishReason.KNOCKOUT);
        mongoTemplate.insert(battle);
    }
}
