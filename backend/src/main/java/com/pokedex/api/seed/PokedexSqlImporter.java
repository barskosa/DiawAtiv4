package com.pokedex.api.seed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.PokemonType;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.seed.SqlScriptParser.SqlParseException;
import com.pokedex.api.seed.SqlScriptParser.SqlTable;

/**
 * Converte as tabelas relacionais do {@code Script_Pokedex.sql} nos documentos do MongoDB.
 *
 * <pre>
 * Pokemon + Rel_Pokemon_Type + Types  →  pokemons  { _id, name, hp, ..., types: ["Grass", "Poison"] }
 * Types                               →  types     { _id, name }
 * Trainers + Rel_Trainer_Pokemon      →  trainers  { _id, name, badges, pokemonIds: [25, 3, ...] }
 * </pre>
 *
 * <p>As tabelas de treinadores são opcionais: o {@code Script_Pokedex_Geracoes.sql} traz só Pokémon e tipos.</p>
 */
public final class PokedexSqlImporter {

    private static final Logger log = LoggerFactory.getLogger(PokedexSqlImporter.class);

    private PokedexSqlImporter() {
    }

    /** Documentos prontos para serem gravados. */
    public record PokedexData(List<PokemonType> types, List<Pokemon> pokemons, List<Trainer> trainers) {
    }

    public static PokedexData fromSql(String sql) {
        Map<String, SqlTable> tables = SqlScriptParser.parse(sql);

        SqlTable typeTable = required(tables, "Types");
        SqlTable pokemonTable = required(tables, "Pokemon");
        SqlTable pokemonTypeTable = required(tables, "Rel_Pokemon_Type");
        SqlTable trainerTable = tables.get("trainers");
        SqlTable trainerPokemonTable = tables.get("rel_trainer_pokemon");

        // Types → catálogo de tipos
        Map<Integer, PokemonType> typesById = new LinkedHashMap<>();
        int typeId = typeTable.columnIndex("ID_Type");
        int typeName = typeTable.columnIndex("Type");
        for (List<Object> row : typeTable.rows()) {
            PokemonType type = new PokemonType(integer(row, typeId), text(row, typeName));
            typesById.put(type.id(), type);
        }

        // Rel_Pokemon_Type → nomes dos tipos de cada Pokémon, na ordem do script
        Map<Integer, List<String>> typesByPokemon = new LinkedHashMap<>();
        int relPokemon = pokemonTypeTable.columnIndex("ID_Pokemon");
        int relType = pokemonTypeTable.columnIndex("ID_Type");
        for (List<Object> row : pokemonTypeTable.rows()) {
            PokemonType type = typesById.get(integer(row, relType));
            if (type == null) {
                log.warn("Rel_Pokemon_Type ignorada: tipo {} inexistente", row.get(relType));
                continue;
            }
            List<String> names = typesByPokemon.computeIfAbsent(integer(row, relPokemon), id -> new ArrayList<>());
            if (!names.contains(type.name())) {
                names.add(type.name());
            }
        }

        // Pokemon → documentos com atributos e tipos embutidos
        int id = pokemonTable.columnIndex("ID_Pokemon");
        int name = pokemonTable.columnIndex("Pokemon");
        int hp = pokemonTable.columnIndex("HP");
        int attack = pokemonTable.columnIndex("Attack");
        int defense = pokemonTable.columnIndex("Defense");
        int specialAttack = pokemonTable.columnIndex("Special_Attack");
        int specialDefense = pokemonTable.columnIndex("Special_Defense");
        int speed = pokemonTable.columnIndex("Speed");
        Map<Integer, Pokemon> pokemonsById = new LinkedHashMap<>();
        for (List<Object> row : pokemonTable.rows()) {
            int pokemonId = integer(row, id);
            pokemonsById.put(pokemonId, new Pokemon(pokemonId, text(row, name), integer(row, hp),
                    integer(row, attack), integer(row, defense), integer(row, specialAttack),
                    integer(row, specialDefense), integer(row, speed),
                    typesByPokemon.getOrDefault(pokemonId, List.of())));
        }
        typesByPokemon.keySet().stream()
                .filter(pokemonId -> !pokemonsById.containsKey(pokemonId))
                .forEach(pokemonId -> log.warn("Rel_Pokemon_Type ignorada: Pokémon {} inexistente", pokemonId));

        List<Trainer> trainers = trainerTable == null ? List.of() : trainers(trainerTable, trainerPokemonTable, pokemonsById);

        return new PokedexData(
                typesById.values().stream().sorted(Comparator.comparing(PokemonType::id)).toList(),
                pokemonsById.values().stream().sorted(Comparator.comparing(Pokemon::getId)).toList(),
                trainers);
    }

    private static List<Trainer> trainers(SqlTable trainerTable, SqlTable trainerPokemonTable,
            Map<Integer, Pokemon> pokemonsById) {
        if (trainerPokemonTable == null) {
            throw new SqlParseException("O script possui Trainers, mas não possui INSERT INTO Rel_Trainer_Pokemon");
        }
        // Rel_Trainer_Pokemon → equipes (mantém ordem e repetições)
        Map<Integer, List<Integer>> teams = new LinkedHashMap<>();
        int teamTrainer = trainerPokemonTable.columnIndex("ID_Trainer");
        int teamPokemon = trainerPokemonTable.columnIndex("ID_Pokemon");
        for (List<Object> row : trainerPokemonTable.rows()) {
            int pokemonId = integer(row, teamPokemon);
            if (!pokemonsById.containsKey(pokemonId)) {
                log.warn("Rel_Trainer_Pokemon ignorada: Pokémon {} inexistente", pokemonId);
                continue;
            }
            teams.computeIfAbsent(integer(row, teamTrainer), trainerId -> new ArrayList<>()).add(pokemonId);
        }

        // Trainers → documentos com a equipe por referência
        int trainerId = trainerTable.columnIndex("ID_Trainer");
        int trainerName = trainerTable.columnIndex("Trainer");
        int badges = trainerTable.columnIndex("Badges");
        List<Trainer> trainers = new ArrayList<>();
        for (List<Object> row : trainerTable.rows()) {
            int idValue = integer(row, trainerId);
            trainers.add(new Trainer(idValue, text(row, trainerName), integer(row, badges),
                    teams.getOrDefault(idValue, List.of())));
        }

        return trainers.stream().sorted(Comparator.comparing(Trainer::id)).toList();
    }

    private static SqlTable required(Map<String, SqlTable> tables, String name) {
        SqlTable table = tables.get(name.toLowerCase());
        if (table == null) {
            throw new SqlParseException("O script não possui INSERT INTO " + name);
        }
        return table;
    }

    private static int integer(List<Object> row, int index) {
        Object value = row.get(index);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new SqlParseException("Valor numérico esperado, encontrado " + value + " em " + row);
    }

    private static String text(List<Object> row, int index) {
        Object value = row.get(index);
        if (value == null) {
            throw new SqlParseException("Texto esperado, encontrado NULL em " + row);
        }
        return value.toString();
    }
}
