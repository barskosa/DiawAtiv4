package com.pokedex.api.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pokedex.api.dto.MinigameDtos.DailyMinigame;
import com.pokedex.api.dto.MinigameDtos.DailyRankEntry;
import com.pokedex.api.dto.MinigameDtos.GameStats;
import com.pokedex.api.dto.MinigameDtos.SavedScore;
import com.pokedex.api.dto.MinigameDtos.ScoreRequest;
import com.pokedex.api.model.MinigameScore;
import com.pokedex.api.service.MinigameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Placar e ranking dos minigames (adivinhar tipo, número da Pokédex e maior atributo). */
@RestController
@Tag(name = "Minigames", description = "Placar, ranking e estatísticas dos minigames")
@RequestMapping("/api/minigames")
public class MinigameController {

    private final MinigameService minigameService;

    public MinigameController(MinigameService minigameService) {
        this.minigameService = minigameService;
    }

    @Operation(summary = "Salva o resultado de uma partida")
    @PostMapping("/scores")
    public ResponseEntity<SavedScore> save(@Valid @RequestBody ScoreRequest request) {
        SavedScore saved = minigameService.save(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/minigames/scores").queryParam("game", saved.score().game()).build().toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @Operation(summary = "Ranking de um minigame")
    @GetMapping("/scores")
    public List<MinigameScore> leaderboard(@RequestParam MinigameScore.Game game,
            @RequestParam(defaultValue = "10") int limit) {
        return minigameService.leaderboard(game, limit);
    }

    @Operation(summary = "Desafio do dia: os mesmos Pokémon para todos os jogadores")
    @GetMapping("/daily")
    public DailyMinigame daily(@RequestParam MinigameScore.Game game) {
        return minigameService.dailyChallenge(game);
    }

    @Operation(summary = "Ranking do desafio do dia (melhor partida de cada jogador)")
    @GetMapping("/daily/ranking")
    public List<DailyRankEntry> dailyRanking(@RequestParam MinigameScore.Game game,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int limit) {
        return minigameService.dailyRanking(game, date, limit);
    }

    @Operation(summary = "Estatísticas de cada minigame")
    @GetMapping("/stats")
    public List<GameStats> stats() {
        return minigameService.stats();
    }
}
