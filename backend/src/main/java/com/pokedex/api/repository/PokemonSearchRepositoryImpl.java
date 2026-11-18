package com.pokedex.api.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.pokedex.api.model.Pokemon;

class PokemonSearchRepositoryImpl implements PokemonSearchRepository {

    private final MongoTemplate mongoTemplate;

    PokemonSearchRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Pokemon> search(String name, Collection<String> types, boolean typeless) {
        List<Criteria> filters = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            // Pattern.quote evita que caracteres como "." em "Mr. Mime" sejam tratados como regex.
            filters.add(Criteria.where("name").regex(Pattern.quote(name.trim()), "i"));
        }
        if (types != null && !types.isEmpty()) {
            filters.add(Criteria.where("types").all(types));
        }
        if (typeless) {
            filters.add(Criteria.where("types").size(0));
        }

        Query query = filters.isEmpty() ? new Query() : new Query(new Criteria().andOperator(filters));
        return mongoTemplate.find(query.with(Sort.by("id")), Pokemon.class);
    }
}
