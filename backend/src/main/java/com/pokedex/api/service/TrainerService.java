package com.pokedex.api.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.pokedex.api.exception.ResourceNotFoundException;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.repository.TrainerRepository;

@Service
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final PokemonRepository pokemonRepository;

    public TrainerService(TrainerRepository trainerRepository, PokemonRepository pokemonRepository) {
        this.trainerRepository = trainerRepository;
        this.pokemonRepository = pokemonRepository;
    }

    public List<Trainer> findAll() {
        return trainerRepository.findAll(Sort.by("id"));
    }

    public Trainer findById(Integer id) {
        return trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treinador " + id + " não encontrado."));
    }

    /** Equipe do treinador com os documentos completos, na ordem e com as repetições cadastradas. */
    public List<Pokemon> findTeam(Integer trainerId) {
        Trainer trainer = findById(trainerId);
        Map<Integer, Pokemon> pokemons = pokemonRepository.findAllById(trainer.pokemonIds()).stream()
                .collect(Collectors.toMap(Pokemon::getId, Function.identity()));
        return trainer.pokemonIds().stream().map(pokemons::get).filter(Objects::nonNull).toList();
    }

    /** Treinadores que têm o Pokémon na equipe. */
    public List<Trainer> findByPokemon(Integer pokemonId) {
        return trainerRepository.findByPokemonIdsContainingOrderByIdAsc(pokemonId);
    }
}
