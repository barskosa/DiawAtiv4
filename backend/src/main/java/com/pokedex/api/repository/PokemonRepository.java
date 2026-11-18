package com.pokedex.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pokedex.api.model.Pokemon;

public interface PokemonRepository extends MongoRepository<Pokemon, Integer>, PokemonSearchRepository {

    Optional<Pokemon> findByNameIgnoreCase(String name);

    Optional<Pokemon> findTopByOrderByIdDesc();
}
