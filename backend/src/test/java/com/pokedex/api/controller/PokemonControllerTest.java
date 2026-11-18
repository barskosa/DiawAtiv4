package com.pokedex.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pokedex.api.dto.PokemonRequest;
import com.pokedex.api.exception.ConflictException;
import com.pokedex.api.exception.ResourceNotFoundException;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.service.PokemonService;
import com.pokedex.api.service.TrainerService;

/** Testa o contrato HTTP do controller (status, JSON e validação) sem banco de dados. */
@WebMvcTest(PokemonController.class)
class PokemonControllerTest {

    private static final String VALID_BODY = """
            {"name":"Pikachu","hp":35,"attack":55,"defense":30,"specialAttack":50,"specialDefense":40,"speed":90,"types":["Electric"]}
            """;

    private static final Pokemon PIKACHU = new Pokemon(25, "Pikachu", 35, 55, 30, 50, 40, 90, List.of("Electric"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PokemonService pokemonService;

    @MockitoBean
    private TrainerService trainerService;

    @Test
    void listsPokemonsPassingTheFilters() throws Exception {
        when(pokemonService.findAll(eq("pika"), eq(List.of("Electric")))).thenReturn(List.of(PIKACHU));

        mockMvc.perform(get("/api/pokemons").param("name", "pika").param("type", "Electric"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(25))
                .andExpect(jsonPath("$[0].name").value("Pikachu"))
                .andExpect(jsonPath("$[0].specialAttack").value(50))
                .andExpect(jsonPath("$[0].types[0]").value("Electric"));
    }

    @Test
    void returnsTheDocumentFormatOfTheAssignment() throws Exception {
        when(pokemonService.findById(25)).thenReturn(PIKACHU);

        mockMvc.perform(get("/api/pokemons/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.hp").value(35))
                .andExpect(jsonPath("$.attack").value(55))
                .andExpect(jsonPath("$.defense").value(30))
                .andExpect(jsonPath("$.specialAttack").value(50))
                .andExpect(jsonPath("$.specialDefense").value(40))
                .andExpect(jsonPath("$.speed").value(90))
                .andExpect(jsonPath("$.types").isArray());
    }

    @Test
    void answers404AsProblemDetail() throws Exception {
        when(pokemonService.findById(999)).thenThrow(new ResourceNotFoundException("Pokémon 999 não encontrado."));

        mockMvc.perform(get("/api/pokemons/999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.detail").value("Pokémon 999 não encontrado."));
    }

    @Test
    void answers400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/pokemons/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void createsWithLocationHeader() throws Exception {
        when(pokemonService.create(any(PokemonRequest.class))).thenReturn(PIKACHU);

        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pokemons/25")))
                .andExpect(jsonPath("$.name").value("Pikachu"));
    }

    @Test
    void rejectsInvalidBodyWithFieldErrors() throws Exception {
        String invalid = """
                {"name":" ","hp":0,"attack":256,"defense":30,"specialAttack":50,"specialDefense":40,
                 "types":["Fire","Water","Grass"]}
                """;

        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("é obrigatório"))
                .andExpect(jsonPath("$.errors.hp").value("deve ser no mínimo 1"))
                .andExpect(jsonPath("$.errors.attack").value("deve ser no máximo 255"))
                .andExpect(jsonPath("$.errors.speed").value("é obrigatório"))
                .andExpect(jsonPath("$.errors.types").value("deve ter no máximo 2 tipos"));
        verify(pokemonService, never()).create(any());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("JSON inválido"));
    }

    @Test
    void answers409OnConflict() throws Exception {
        when(pokemonService.create(any())).thenThrow(new ConflictException("name", "O nome Pikachu já existe."));

        mockMvc.perform(post("/api/pokemons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.name").value("O nome Pikachu já existe."));
    }

    @Test
    void updatesById() throws Exception {
        when(pokemonService.update(eq(25), any())).thenReturn(PIKACHU);

        mockMvc.perform(put("/api/pokemons/25").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25));
        verify(pokemonService).update(eq(25), any(PokemonRequest.class));
    }

    @Test
    void deletesWithNoContent() throws Exception {
        mockMvc.perform(delete("/api/pokemons/25")).andExpect(status().isNoContent());
        verify(pokemonService).delete(25);
    }

    @Test
    void deleteOfMissingPokemonIs404() throws Exception {
        doThrow(new ResourceNotFoundException("Pokémon 999 não encontrado.")).when(pokemonService).delete(999);

        mockMvc.perform(delete("/api/pokemons/999")).andExpect(status().isNotFound());
    }

    @Test
    void listsWithoutFilters() throws Exception {
        when(pokemonService.findAll(isNull(), isNull())).thenReturn(List.of(PIKACHU));

        mockMvc.perform(get("/api/pokemons")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
}
