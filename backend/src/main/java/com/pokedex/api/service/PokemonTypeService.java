package com.pokedex.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.PokemonType;
import com.pokedex.api.repository.PokemonTypeRepository;

@Service
public class PokemonTypeService {

    private final PokemonTypeRepository typeRepository;

    public PokemonTypeService(PokemonTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    public List<PokemonType> findAll() {
        return typeRepository.findAll(Sort.by("id"));
    }

    /** Procura um tipo pelo nome, sem diferenciar maiúsculas. */
    public Optional<PokemonType> findByName(String name) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        return findAll().stream().filter(type -> type.name().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    /**
     * Valida os tipos de um cadastro e devolve os nomes canônicos do catálogo
     * (ex.: {@code "fire"} vira {@code "Fire"}).
     */
    public List<String> resolveForPokemon(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<String> resolved = new ArrayList<>();
        for (String name : names) {
            PokemonType type = findByName(name).orElseThrow(() -> new InvalidRequestException("types",
                    "Tipo inexistente: " + name + ". Consulte GET /api/types."));
            if (PokemonType.TYPELESS.equals(type.name())) {
                throw new InvalidRequestException("types",
                        "Para um Pokémon sem tipo, envie a lista de tipos vazia.");
            }
            if (resolved.contains(type.name())) {
                throw new InvalidRequestException("types", "O tipo " + type.name() + " foi informado duas vezes.");
            }
            resolved.add(type.name());
        }
        return resolved;
    }
}
