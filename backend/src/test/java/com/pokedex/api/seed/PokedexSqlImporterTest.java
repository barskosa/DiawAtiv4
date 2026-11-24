package com.pokedex.api.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.PokemonType;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.seed.PokedexSqlImporter.PokedexData;

/** Confere a conversão do Script_Pokedex.sql real da atividade. */
class PokedexSqlImporterTest {

    private static PokedexData data;

    @BeforeAll
    static void importScript() throws IOException {
        try (InputStream script = PokedexSqlImporterTest.class.getResourceAsStream("/db/Script_Pokedex.sql")) {
            assertThat(script).as("Script_Pokedex.sql empacotado pelo Maven").isNotNull();
            data = PokedexSqlImporter.fromSql(new String(script.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void importsAllRowsOfTheScript() {
        assertThat(data.pokemons()).hasSize(151);
        assertThat(data.types()).hasSize(18);
        assertThat(data.trainers()).hasSize(12);
        assertThat(data.pokemons()).extracting(Pokemon::getId).startsWith(1, 2, 3).endsWith(150, 151);
        int relations = data.pokemons().stream().mapToInt(pokemon -> pokemon.getTypes().size()).sum();
        assertThat(relations).isEqualTo(214);
        int teamMembers = data.trainers().stream().mapToInt(trainer -> trainer.pokemonIds().size()).sum();
        assertThat(teamMembers).isEqualTo(43);
    }

    @Test
    void createsThePikachuDocumentOfTheAssignment() {
        assertThat(pokemon(25)).isEqualTo(new Pokemon(25, "Pikachu", 35, 55, 30, 50, 40, 90, List.of("Electric")));
    }

    @Test
    void keepsTypeOrderAndScriptPeculiarities() {
        assertThat(pokemon(1).getTypes()).containsExactly("Grass", "Poison");
        assertThat(pokemon(6).getTypes()).containsExactly("Fire", "Flying");
        assertThat(pokemon(46).getTypes()).containsExactly("Bug", "Grass");
        assertThat(pokemon(83).getName()).isEqualTo("Farfetch'd");
        assertThat(pokemon(122).getName()).isEqualTo("Mr. Mime");
        assertThat(pokemon(151).getTypes()).as("Mew não tem tipo no script").isEmpty();
        assertThat(pokemon(113).getHp()).isEqualTo(250);
    }

    @Test
    void importsTypeCatalog() {
        assertThat(data.types()).first().isEqualTo(new PokemonType(0, "Typeless"));
        assertThat(data.types()).last().isEqualTo(new PokemonType(17, "Steel"));
        assertThat(data.types()).extracting(PokemonType::name).contains("Dark", "Dragon", "Psychic");
    }

    @Test
    void importsTrainersWithTheirTeams() {
        var trainers = data.trainers().stream().collect(Collectors.toMap(Trainer::name, Function.identity()));

        assertThat(trainers.get("Ash")).isEqualTo(new Trainer(1, "Ash", 9, List.of(25, 3, 6, 9, 131, 143)));
        assertThat(trainers.get("Koga").pokemonIds()).as("repetições do script").containsExactly(48, 48, 48, 49);
        assertThat(trainers.get("Lance").pokemonIds()).containsExactly(130, 148, 149, 142, 149);
        assertThat(trainers.get("Team Rocket").badges()).isZero();
        assertThat(trainers.get("Lt. Surge").pokemonIds()).containsExactly(26);
    }

    @Test
    void importsTheGenerationsScriptWithoutTrainers() throws IOException {
        try (InputStream script = getClass().getResourceAsStream("/db/Script_Pokedex_Geracoes.sql")) {
            assertThat(script).as("Script_Pokedex_Geracoes.sql empacotado pelo Maven").isNotNull();
            PokedexData generations = PokedexSqlImporter.fromSql(new String(script.readAllBytes(), StandardCharsets.UTF_8));

            assertThat(generations.pokemons()).hasSize(874);
            assertThat(generations.pokemons()).first().isEqualTo(
                    new Pokemon(152, "Chikorita", 45, 49, 65, 49, 65, 45, List.of("Grass")));
            assertThat(generations.pokemons()).last().extracting(Pokemon::getId).isEqualTo(1025);
            assertThat(generations.types()).last().isEqualTo(new PokemonType(18, "Fairy"));
            assertThat(generations.trainers()).isEmpty();
        }
    }

    private static Pokemon pokemon(int id) {
        return data.pokemons().stream().filter(pokemon -> pokemon.getId() == id).findFirst().orElseThrow();
    }
}
