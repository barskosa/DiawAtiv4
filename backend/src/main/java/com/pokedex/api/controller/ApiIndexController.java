package com.pokedex.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/** Página inicial da API, útil para conferir rapidamente se o backend está no ar. */
@Hidden
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
        endpoints.put("POST /api/battles", "Desafia um treinador com um time sorteado do mesmo tamanho da equipe dele");
        endpoints.put("GET /api/battles", "Histórico de desafios (?limit=10)");
        endpoints.put("GET /api/battles/stats", "Vitórias e derrotas por treinador (agregação no MongoDB)");
        endpoints.put("GET /api/battles/{id}", "Estado de uma batalha");
        endpoints.put("POST /api/battles/{id}/team", "Sorteia outro time antes da primeira rodada");
        endpoints.put("POST /api/battles/{id}/rounds", "Joga uma rodada (ATTACK, SPECIAL, DEFEND ou SWITCH)");
        endpoints.put("POST /api/battles/{id}/forfeit", "Desiste do desafio");
        endpoints.put("POST /api/minigames/scores", "Salva o resultado de uma partida de minigame");
        endpoints.put("GET /api/minigames/scores?game=TYPE", "Ranking de um minigame (TYPE, NUMBER ou STAT)");
        endpoints.put("GET /api/minigames/stats", "Partidas, recorde e média de cada minigame");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Pokédex API");
        body.put("status", "online");
        body.put("documentation", "/swagger-ui.html");
        body.put("endpoints", endpoints);
        return body;
    }
}
