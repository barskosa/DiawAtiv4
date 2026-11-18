package com.pokedex.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pokedex.api.model.Trainer;

public interface TrainerRepository extends MongoRepository<Trainer, Integer>, TrainerTeamRepository {

    List<Trainer> findByPokemonIdsContainingOrderByIdAsc(Integer pokemonId);
}
