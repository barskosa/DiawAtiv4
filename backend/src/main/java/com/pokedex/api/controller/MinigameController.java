package com.pokedex.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pokedex.api.dto.MinigameDtos.GameStats;
import com.pokedex.api.dto.MinigameDtos.SavedScore;
import com.pokedex.api.dto.MinigameDtos.ScoreRequest;
import com.pokedex.api.model.MinigameScore;
import com.pokedex.api.service.MinigameService;

import jakarta.validation.Valid;

/** Placar e ranking dos minigames (adivinhar tipo, número da Pokédex e maior atributo). */
@RestController
@RequestMapping("/api/minigames")
public class MinigameController {

    private final MinigameService minigameService;

    public MinigameController(MinigameService minigameService) {
        this.minigameService = minigameService;
    }

    @PostMapping("/scores")
    public ResponseEntity<SavedScore> save(@Valid @RequestBody ScoreRequest request) {
        SavedScore saved = minigameService.save(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/minigames/scores").queryParam("game", saved.score().game()).build().toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/scores")
    public List<MinigameScore> leaderboard(@RequestParam MinigameScore.Game game,
            @RequestParam(defaultValue = "10") int limit) {
        return minigameService.leaderboard(game, limit);
    }

    @GetMapping("/stats")
    public List<GameStats> stats() {
        return minigameService.stats();
    }
}
