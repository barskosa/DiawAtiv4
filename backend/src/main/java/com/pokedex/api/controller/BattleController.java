package com.pokedex.api.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import com.pokedex.api.dto.BattleViews.DailyBattle;
import com.pokedex.api.dto.BattleViews.DailyRankEntry;
import com.pokedex.api.dto.BattleViews.Summary;
import com.pokedex.api.dto.BattleViews.TrainerStats;
import com.pokedex.api.model.Battle;
import com.pokedex.api.service.battle.BattleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Desafios contra os treinadores: o servidor sorteia o time, joga pelo treinador e salva a batalha. */
@RestController
@Tag(name = "Batalhas", description = "Desafios contra os treinadores com time sorteado do mesmo tamanho da equipe")
@RequestMapping("/api/battles")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @Operation(summary = "Desafia um treinador")
    @PostMapping
    public ResponseEntity<Battle> challenge(@Valid @RequestBody BattleRequests.Challenge request) {
        Battle battle = battleService.challenge(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(battle.getId()).toUri();
        return ResponseEntity.created(location).body(battle);
    }

    @Operation(summary = "Histórico de desafios")
    @GetMapping
    public List<Summary> recent(@RequestParam(defaultValue = "10") int limit) {
        return battleService.recent(limit);
    }

    @Operation(summary = "Vitórias e derrotas por treinador")
    @GetMapping("/stats")
    public List<TrainerStats> stats() {
        return battleService.stats();
    }

    @Operation(summary = "Desafio do dia: treinador e time iguais para todos os jogadores")
    @GetMapping("/daily")
    public DailyBattle daily() {
        return battleService.dailyChallenge();
    }

    @Operation(summary = "Ranking do desafio do dia (menos rodadas, depois mais HP sobrando)")
    @GetMapping("/daily/ranking")
    public List<DailyRankEntry> dailyRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int limit) {
        return battleService.dailyRanking(date, limit);
    }

    @Operation(summary = "Estado de uma batalha")
    @GetMapping("/{id}")
    public Battle findById(@PathVariable String id) {
        return battleService.findById(id);
    }

    @Operation(summary = "Sorteia outro time (antes da 1ª rodada)")
    @PostMapping("/{id}/team")
    public Battle rerollTeam(@PathVariable String id) {
        return battleService.rerollTeam(id);
    }

    @Operation(summary = "Joga uma rodada (ATTACK, SPECIAL, DEFEND ou SWITCH)")
    @PostMapping("/{id}/rounds")
    public Battle playRound(@PathVariable String id, @Valid @RequestBody BattleRequests.Round request) {
        return battleService.playRound(id, request);
    }

    @Operation(summary = "Desiste do desafio")
    @PostMapping("/{id}/forfeit")
    public Battle forfeit(@PathVariable String id) {
        return battleService.forfeit(id);
    }
}
