package com.pokedex.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.pokedex.api.dto.PokemonRequest;
import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.exception.ResourceNotFoundException;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.PokemonType;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.repository.TrainerRepository;

/**
 * Regras de negócio dos Pokémon: validação de tipos, números e nomes únicos, e manutenção das
 * equipes dos treinadores quando um Pokémon é excluído.
 */
@Service
public class PokemonService {

    private final PokemonRepository pokemonRepository;
    private final TrainerRepository trainerRepository;
    private final PokemonTypeService typeService;

    public PokemonService(PokemonRepository pokemonRepository, TrainerRepository trainerRepository,
            PokemonTypeService typeService) {
        this.pokemonRepository = pokemonRepository;
        this.trainerRepository = trainerRepository;
        this.typeService = typeService;
    }

    /**
     * Lista os Pokémon, opcionalmente filtrando por trecho do nome e por tipos (o Pokémon precisa ter
     * todos). O tipo {@code Typeless} encontra os Pokémon sem tipo.
     */
    public List<Pokemon> findAll(String name, List<String> typeNames) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasTypes = typeNames != null && typeNames.stream().anyMatch(type -> type != null && !type.isBlank());
        if (!hasName && !hasTypes) {
            return pokemonRepository.findAll(Sort.by("id"));
        }

        List<String> types = new ArrayList<>();
        boolean typeless = false;
        if (hasTypes) {
            for (String typeName : typeNames) {
                if (typeName == null || typeName.isBlank()) {
                    continue;
                }
                PokemonType type = typeService.findByName(typeName)
                        .orElseThrow(() -> new InvalidRequestException("type", "Tipo inexistente: " + typeName
                                + ". Consulte GET /api/types."));
                if (PokemonType.TYPELESS.equals(type.name())) {
                    typeless = true;
                } else if (!types.contains(type.name())) {
                    types.add(type.name());
                }
            }
        }
        if (typeless && !types.isEmpty()) {
            return List.of(); // nenhum Pokémon é, ao mesmo tempo, sem tipo e de um tipo
        }
        return pokemonRepository.search(hasName ? name : null, types, typeless);
    }

    public Pokemon findById(Integer id) {
        return pokemonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon " + id + " não encontrado."));
    }

    public Pokemon create(PokemonRequest request) {
        Integer id = request.id() != null ? request.id() : nextId();
        if (pokemonRepository.existsById(id)) {
            throw new ConflictException("id", "Já existe um Pokémon com o número " + id + ".");
        }
        Pokemon pokemon = toPokemon(id, request);
        ensureUniqueName(pokemon.getName(), id);
        try {
            return pokemonRepository.insert(pokemon);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("id", "Já existe um Pokémon com o número " + id + ".");
        }
    }

    public Pokemon update(Integer id, PokemonRequest request) {
        if (request.id() != null && !request.id().equals(id)) {
            throw new InvalidRequestException("id", "O número do Pokémon não pode ser alterado (URL: " + id
                    + ", corpo: " + request.id() + ").");
        }
        if (!pokemonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pokémon " + id + " não encontrado.");
        }
        Pokemon pokemon = toPokemon(id, request);
        ensureUniqueName(pokemon.getName(), id);
        return pokemonRepository.save(pokemon);
    }

    /** Exclui o Pokémon e o retira das equipes dos treinadores. */
    public void delete(Integer id) {
        if (!pokemonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pokémon " + id + " não encontrado.");
        }
        pokemonRepository.deleteById(id);
        trainerRepository.removePokemonFromTeams(id);
    }

    private Pokemon toPokemon(Integer id, PokemonRequest request) {
        return new Pokemon(id, normalizeName(request.name()), request.hp(), request.attack(), request.defense(),
                request.specialAttack(), request.specialDefense(), request.speed(),
                typeService.resolveForPokemon(request.types()));
    }

    private void ensureUniqueName(String name, Integer id) {
        pokemonRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("name", "O nome " + existing.getName()
                            + " já pertence ao Pokémon " + existing.getId() + ".");
                });
    }

    private Integer nextId() {
        return pokemonRepository.findTopByOrderByIdDesc().map(pokemon -> pokemon.getId() + 1).orElse(1);
    }

    private static String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
