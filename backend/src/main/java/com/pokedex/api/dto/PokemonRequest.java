package com.pokedex.api.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo das requisições POST e PUT de {@code /api/pokemons}.
 *
 * <p>{@code id} é opcional no cadastro (o próximo número livre é usado) e, na edição, deve ser
 * omitido ou igual ao da URL. {@code types} aceita de 0 a 2 nomes do catálogo, em qualquer
 * combinação de maiúsculas; lista vazia representa um Pokémon sem tipo.</p>
 */
public record PokemonRequest(
        @Positive(message = "deve ser um número positivo")
        Integer id,

        @NotBlank(message = "é obrigatório")
        @Size(max = 30, message = "deve ter no máximo 30 caracteres")
        String name,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer hp,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer attack,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer defense,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer specialAttack,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer specialDefense,

        @NotNull(message = "é obrigatório") @Min(value = 1, message = "deve ser no mínimo 1") @Max(value = 255, message = "deve ser no máximo 255")
        Integer speed,

        @Size(max = 2, message = "deve ter no máximo 2 tipos")
        List<@NotBlank(message = "não pode ser vazio") String> types) {
}
