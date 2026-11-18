package com.pokedex.api.repository;

import java.util.Collection;
import java.util.List;

import com.pokedex.api.model.Pokemon;

/**
 * Consultas que combinam filtros opcionais e por isso são montadas com {@code MongoTemplate}.
 */
public interface PokemonSearchRepository {

    /**
     * Busca Pokémon pelo trecho do nome (sem diferenciar maiúsculas) e que possuam TODOS os tipos
     * informados. Sem filtros, devolve todos, ordenados pelo número da Pokédex.
     *
     * @param name      trecho do nome, ou {@code null}
     * @param types     nomes canônicos dos tipos, ou vazio
     * @param typeless  {@code true} para buscar somente Pokémon sem tipo
     */
    List<Pokemon> search(String name, Collection<String> types, boolean typeless);
}
