package com.pokedex.api.repository;

/**
 * Operações de atualização das equipes dos treinadores.
 */
public interface TrainerTeamRepository {

    /**
     * Remove todas as ocorrências do Pokémon das equipes ({@code $pull}).
     *
     * @return quantidade de treinadores alterados
     */
    long removePokemonFromTeams(Integer pokemonId);
}
