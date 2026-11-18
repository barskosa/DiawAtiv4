package com.pokedex.api.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.pokedex.api.model.Trainer;

class TrainerTeamRepositoryImpl implements TrainerTeamRepository {

    private final MongoTemplate mongoTemplate;

    TrainerTeamRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long removePokemonFromTeams(Integer pokemonId) {
        Query teamsWithPokemon = new Query(Criteria.where("pokemonIds").is(pokemonId));
        Update pull = new Update().pull("pokemonIds", pokemonId);
        return mongoTemplate.updateMulti(teamsWithPokemon, pull, Trainer.class).getModifiedCount();
    }
}
