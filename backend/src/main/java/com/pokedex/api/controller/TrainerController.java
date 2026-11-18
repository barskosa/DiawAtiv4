package com.pokedex.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.service.TrainerService;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @GetMapping
    public List<Trainer> findAll() {
        return trainerService.findAll();
    }

    @GetMapping("/{id}")
    public Trainer findById(@PathVariable Integer id) {
        return trainerService.findById(id);
    }

    /** Pokémon da equipe do treinador (documentos completos). */
    @GetMapping("/{id}/pokemons")
    public List<Pokemon> findTeam(@PathVariable Integer id) {
        return trainerService.findTeam(id);
    }
}
