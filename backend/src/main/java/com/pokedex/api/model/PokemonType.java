package com.pokedex.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento da coleção {@code types}: catálogo dos tipos (tabela {@code Types} do script).
 * É usado para validar os tipos informados no cadastro e para montar os filtros do frontend.
 */
@Document(collection = "types")
public record PokemonType(@Id Integer id, String name) {

    /** Tipo do script usado apenas como rótulo para Pokémon sem tipo (ex.: Mew). */
    public static final String TYPELESS = "Typeless";
}
