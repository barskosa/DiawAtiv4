package com.pokedex.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pokedex.api.dto.PokemonRequest;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.service.PokemonService;
import com.pokedex.api.service.TrainerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Pokémon", description = "CRUD dos Pokémon importados do Script_Pokedex.sql, com busca por nome e tipo")
@RequestMapping("/api/pokemons")
public class PokemonController {

    private final PokemonService pokemonService;
    private final TrainerService trainerService;

    public PokemonController(PokemonService pokemonService, TrainerService trainerService) {
        this.pokemonService = pokemonService;
        this.trainerService = trainerService;
    }

    /**
     * {@code GET /api/pokemons} lista todos; {@code ?name=char} busca pelo nome e
     * {@code ?type=Grass&type=Poison} filtra por tipo (o Pokémon precisa ter todos os tipos).
     */
    @Operation(summary = "Lista os Pokémon (filtros opcionais: name e type)")
    @GetMapping
    public List<Pokemon> findAll(@RequestParam(required = false) String name,
            @RequestParam(name = "type", required = false) List<String> types) {
        return pokemonService.findAll(name, types);
    }

    @Operation(summary = "Busca um Pokémon pelo número da Pokédex")
    @GetMapping("/{id}")
    public Pokemon findById(@PathVariable Integer id) {
        return pokemonService.findById(id);
    }

    /** Treinadores que possuem o Pokémon na equipe. */
    @Operation(summary = "Treinadores que têm o Pokémon na equipe")
    @GetMapping("/{id}/trainers")
    public List<Trainer> findTrainers(@PathVariable Integer id) {
        pokemonService.findById(id);
        return trainerService.findByPokemon(id);
    }

    @Operation(summary = "Cadastra um Pokémon (id opcional)")
    @PostMapping
    public ResponseEntity<Pokemon> create(@Valid @RequestBody PokemonRequest request) {
        Pokemon created = pokemonService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Atualiza um Pokémon")
    @PutMapping("/{id}")
    public Pokemon update(@PathVariable Integer id, @Valid @RequestBody PokemonRequest request) {
        return pokemonService.update(id, request);
    }

    @Operation(summary = "Remove um Pokémon e o retira das equipes")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        pokemonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
