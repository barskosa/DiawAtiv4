package com.pokedex.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.service.TrainerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Treinadores", description = "Treinadores e suas equipes")
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @Operation(summary = "Lista os treinadores")
    @GetMapping
    public List<Trainer> findAll() {
        return trainerService.findAll();
    }

    @Operation(summary = "Busca um treinador")
    @GetMapping("/{id}")
    public Trainer findById(@PathVariable Integer id) {
        return trainerService.findById(id);
    }

    /** Pokémon da equipe do treinador (documentos completos). */
    @Operation(summary = "Equipe do treinador com os dados dos Pokémon")
    @GetMapping("/{id}/pokemons")
    public List<Pokemon> findTeam(@PathVariable Integer id) {
        return trainerService.findTeam(id);
    }
}
