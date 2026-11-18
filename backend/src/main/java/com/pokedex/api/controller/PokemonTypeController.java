package com.pokedex.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pokedex.api.model.PokemonType;
import com.pokedex.api.service.PokemonTypeService;

@RestController
@RequestMapping("/api/types")
public class PokemonTypeController {

    private final PokemonTypeService typeService;

    public PokemonTypeController(PokemonTypeService typeService) {
        this.typeService = typeService;
    }

    @GetMapping
    public List<PokemonType> findAll() {
        return typeService.findAll();
    }
}
