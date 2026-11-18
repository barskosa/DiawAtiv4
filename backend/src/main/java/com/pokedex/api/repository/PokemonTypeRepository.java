package com.pokedex.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pokedex.api.model.PokemonType;

public interface PokemonTypeRepository extends MongoRepository<PokemonType, Integer> {
}
