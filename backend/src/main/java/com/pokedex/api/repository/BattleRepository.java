package com.pokedex.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pokedex.api.model.Battle;

public interface BattleRepository extends MongoRepository<Battle, String> {
}
