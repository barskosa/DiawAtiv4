package com.pokedex.api.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.pokedex.api.seed.SqlScriptParser.SqlParseException;
import com.pokedex.api.seed.SqlScriptParser.SqlTable;

class SqlScriptParserTest {

    @Test
    void readsInsertsAndIgnoresOtherStatements() {
        String sql = """
                -- comentário de linha
                CREATE TABLE Pokemon (`ID_Pokemon` int not null, `Pokemon` varchar(11), PRIMARY KEY (ID_Pokemon));
                /* comentário
                   de bloco; com ponto e vírgula */
                INSERT INTO Pokemon
                    (`ID_Pokemon`, `Pokemon`)
                VALUES
                    (83, 'Farfetch''d'),
                    (122, 'Mr. Mime');
                INSERT INTO Pokemon (ID_Pokemon, Pokemon) VALUES (999, NULL);
                SELECT * FROM Pokemon;
                """;

        Map<String, SqlTable> tables = SqlScriptParser.parse(sql);

        assertThat(tables).containsOnlyKeys("pokemon");
        SqlTable pokemon = tables.get("pokemon");
        assertThat(pokemon.name()).isEqualTo("Pokemon");
        assertThat(pokemon.columns()).containsExactly("ID_Pokemon", "Pokemon");
        assertThat(pokemon.rows()).containsExactly(
                List.of(83L, "Farfetch'd"),
                List.of(122L, "Mr. Mime"),
                Arrays.asList(999L, null));
        assertThat(pokemon.columnIndex("pokemon")).isEqualTo(1);
    }

    @Test
    void acceptsTheLastStatementWithoutSemicolon() {
        Map<String, SqlTable> tables = SqlScriptParser.parse("INSERT INTO Types (ID_Type, Type) VALUES (0, 'Typeless')");

        assertThat(tables.get("types").rows()).containsExactly(List.of(0L, "Typeless"));
    }

    @Test
    void rejectsRowsWithWrongNumberOfValues() {
        assertThatThrownBy(() -> SqlScriptParser.parse("INSERT INTO Types (ID_Type, Type) VALUES (1);"))
                .isInstanceOf(SqlParseException.class)
                .hasMessageContaining("1 valores, mas 2 colunas");
    }

    @Test
    void rejectsUnclosedText() {
        assertThatThrownBy(() -> SqlScriptParser.parse("INSERT INTO Types (ID_Type, Type) VALUES (1, 'Normal);"))
                .isInstanceOf(SqlParseException.class)
                .hasMessageContaining("Texto não fechado");
    }
}
