package com.pokedex.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pokedex.api.dto.BattleRequests;
import com.pokedex.api.dto.BattleViews.Summary;
import com.pokedex.api.dto.BattleViews.TrainerStats;
import com.pokedex.api.model.Battle;
import com.pokedex.api.service.battle.BattleService;

import jakarta.validation.Valid;

/** Desafios contra os treinadores: o servidor sorteia o time, joga pelo treinador e salva a batalha. */
@RestController
@RequestMapping("/api/battles")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping
    public ResponseEntity<Battle> challenge(@Valid @RequestBody BattleRequests.Challenge request) {
        Battle battle = battleService.challenge(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(battle.getId()).toUri();
        return ResponseEntity.created(location).body(battle);
    }

    @GetMapping
    public List<Summary> recent(@RequestParam(defaultValue = "10") int limit) {
        return battleService.recent(limit);
    }

    @GetMapping("/stats")
    public List<TrainerStats> stats() {
        return battleService.stats();
    }

    @GetMapping("/{id}")
    public Battle findById(@PathVariable String id) {
        return battleService.findById(id);
    }

    @PostMapping("/{id}/team")
    public Battle rerollTeam(@PathVariable String id) {
        return battleService.rerollTeam(id);
    }

    @PostMapping("/{id}/rounds")
    public Battle playRound(@PathVariable String id, @Valid @RequestBody BattleRequests.Round request) {
        return battleService.playRound(id, request);
    }

    @PostMapping("/{id}/forfeit")
    public Battle forfeit(@PathVariable String id) {
        return battleService.forfeit(id);
    }
}
