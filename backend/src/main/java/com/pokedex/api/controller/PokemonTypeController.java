package com.pokedex.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pokedex.api.model.PokemonType;
import com.pokedex.api.service.PokemonTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Tipos", description = "Catálogo de tipos")
@RequestMapping("/api/types")
public class PokemonTypeController {

    private final PokemonTypeService typeService;

    public PokemonTypeController(PokemonTypeService typeService) {
        this.typeService = typeService;
    }

    @Operation(summary = "Lista os tipos")
    @GetMapping
    public List<PokemonType> findAll() {
        return typeService.findAll();
    }
}
