package com.pokedex.api.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Documento da coleção {@code trainers} (tabelas {@code Trainers} e {@code Rel_Trainer_Pokemon}).
 *
 * <p>A equipe é guardada como referência ({@code pokemonIds}), porque a relação é N:N e o Pokémon
 * pode ser editado ou excluído de forma independente. A lista mantém a ordem e as repetições do
 * script: Koga, por exemplo, tem três Venonat.</p>
 */
@Document(collection = "trainers")
@JsonPropertyOrder({"id", "name", "badges", "pokemonIds"})
public record Trainer(@Id Integer id, String name, int badges, List<Integer> pokemonIds) {

    public Trainer {
        pokemonIds = pokemonIds == null ? List.of() : List.copyOf(pokemonIds);
    }
}
