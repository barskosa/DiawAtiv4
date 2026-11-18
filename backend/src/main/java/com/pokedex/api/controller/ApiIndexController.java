package com.pokedex.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Página inicial da API, útil para conferir rapidamente se o backend está no ar. */
@RestController
public class ApiIndexController {

    @GetMapping({"/", "/api"})
    public Map<String, Object> index() {
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("GET /api/pokemons", "Lista os Pokémon (?name=char, ?type=Fire&type=Flying)");
        endpoints.put("GET /api/pokemons/{id}", "Busca um Pokémon pelo número");
        endpoints.put("POST /api/pokemons", "Cadastra um Pokémon");
        endpoints.put("PUT /api/pokemons/{id}", "Atualiza um Pokémon");
        endpoints.put("DELETE /api/pokemons/{id}", "Remove um Pokémon");
        endpoints.put("GET /api/pokemons/{id}/trainers", "Treinadores que possuem o Pokémon");
        endpoints.put("GET /api/types", "Lista os tipos");
        endpoints.put("GET /api/trainers", "Lista os treinadores");
        endpoints.put("GET /api/trainers/{id}", "Busca um treinador");
        endpoints.put("GET /api/trainers/{id}/pokemons", "Equipe do treinador");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Pokédex API");
        body.put("status", "online");
        body.put("endpoints", endpoints);
        return body;
    }
}
