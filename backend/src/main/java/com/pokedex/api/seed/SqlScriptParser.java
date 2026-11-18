package com.pokedex.api.seed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lê os comandos {@code INSERT INTO ... VALUES ...} de um script SQL e devolve as linhas de cada tabela.
 *
 * <p>Não é um interpretador SQL completo: os demais comandos ({@code CREATE TABLE}, {@code SELECT})
 * são ignorados. Reconhece identificadores com crases, textos com aspas simples (incluindo o escape
 * {@code ''}, como em {@code 'Farfetch''d'}), números, {@code NULL} e comentários {@code --} e
 * {@code /* *}{@code /}.</p>
 */
public final class SqlScriptParser {

    private SqlScriptParser() {
    }

    /** Linhas de uma tabela, com os valores na ordem das colunas. */
    public record SqlTable(String name, List<String> columns, List<List<Object>> rows) {

        /** Índice da coluna, sem diferenciar maiúsculas. */
        public int columnIndex(String column) {
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equalsIgnoreCase(column)) {
                    return i;
                }
            }
            throw new SqlParseException("A tabela " + name + " não possui a coluna " + column);
        }
    }

    public static class SqlParseException extends RuntimeException {
        public SqlParseException(String message) {
            super(message);
        }
    }

    /**
     * @return tabelas indexadas pelo nome em minúsculas, na ordem em que aparecem no script
     */
    public static Map<String, SqlTable> parse(String sql) {
        List<Token> tokens = tokenize(sql);
        Map<String, SqlTable> tables = new LinkedHashMap<>();
        Cursor cursor = new Cursor(tokens);

        while (cursor.hasNext()) {
            if (cursor.peekKeyword("INSERT")) {
                parseInsert(cursor, tables);
            } else {
                cursor.skipStatement();
            }
        }
        return tables;
    }

    private static void parseInsert(Cursor cursor, Map<String, SqlTable> tables) {
        cursor.expectKeyword("INSERT");
        cursor.expectKeyword("INTO");
        String tableName = cursor.expect(TokenType.IDENTIFIER).text();

        List<String> columns = new ArrayList<>();
        cursor.expectSymbol('(');
        do {
            columns.add(cursor.expect(TokenType.IDENTIFIER).text());
        } while (cursor.acceptSymbol(','));
        cursor.expectSymbol(')');
        cursor.expectKeyword("VALUES");

        List<List<Object>> rows = new ArrayList<>();
        do {
            cursor.expectSymbol('(');
            List<Object> values = new ArrayList<>();
            do {
                values.add(cursor.nextValue());
            } while (cursor.acceptSymbol(','));
            cursor.expectSymbol(')');
            if (values.size() != columns.size()) {
                throw new SqlParseException("Linha da tabela " + tableName + " com " + values.size()
                        + " valores, mas " + columns.size() + " colunas: " + values);
            }
            rows.add(Collections.unmodifiableList(values));
        } while (cursor.acceptSymbol(','));
        cursor.acceptSymbol(';');

        String key = tableName.toLowerCase(Locale.ROOT);
        SqlTable existing = tables.get(key);
        if (existing == null) {
            tables.put(key, new SqlTable(tableName, List.copyOf(columns), rows));
        } else {
            if (!existing.columns().equals(columns)) {
                throw new SqlParseException("INSERTs da tabela " + tableName + " com colunas diferentes");
            }
            existing.rows().addAll(rows);
        }
    }

    // ---------------------------------------------------------------- tokens

    private enum TokenType { WORD, IDENTIFIER, STRING, NUMBER, SYMBOL }

    private record Token(TokenType type, String text, int line) {
    }

    private static List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\n') {
                line++;
                i++;
            } else if (Character.isWhitespace(c) || c == '﻿') {
                i++;
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                if (end < 0) {
                    throw new SqlParseException("Comentário não fechado na linha " + line);
                }
                line += countLines(sql, i, end);
                i = end + 2;
            } else if (c == '`' || c == '"') {
                int end = sql.indexOf(c, i + 1);
                if (end < 0) {
                    throw new SqlParseException("Identificador não fechado na linha " + line);
                }
                tokens.add(new Token(TokenType.IDENTIFIER, sql.substring(i + 1, end), line));
                i = end + 1;
            } else if (c == '\'') {
                StringBuilder text = new StringBuilder();
                int start = line;
                i++;
                while (true) {
                    if (i >= sql.length()) {
                        throw new SqlParseException("Texto não fechado a partir da linha " + start);
                    }
                    char ch = sql.charAt(i);
                    if (ch == '\'' && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        text.append('\'');
                        i += 2;
                    } else if (ch == '\\' && i + 1 < sql.length()) {
                        text.append(sql.charAt(i + 1));
                        i += 2;
                    } else if (ch == '\'') {
                        i++;
                        break;
                    } else {
                        if (ch == '\n') {
                            line++;
                        }
                        text.append(ch);
                        i++;
                    }
                }
                tokens.add(new Token(TokenType.STRING, text.toString(), start));
            } else if (Character.isDigit(c) || (c == '-' && i + 1 < sql.length() && Character.isDigit(sql.charAt(i + 1)))) {
                int start = i;
                i++;
                while (i < sql.length() && (Character.isDigit(sql.charAt(i)) || sql.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, sql.substring(start, i), line));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    i++;
                }
                tokens.add(new Token(TokenType.WORD, sql.substring(start, i), line));
            } else {
                tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c), line));
                i++;
            }
        }
        return tokens;
    }

    private static int countLines(String text, int from, int to) {
        int lines = 0;
        for (int i = from; i < to; i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private static final class Cursor {
        private final List<Token> tokens;
        private int position;

        Cursor(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean hasNext() {
            return position < tokens.size();
        }

        boolean peekKeyword(String keyword) {
            return hasNext() && tokens.get(position).type() == TokenType.WORD
                    && tokens.get(position).text().equalsIgnoreCase(keyword);
        }

        void skipStatement() {
            while (hasNext()) {
                Token token = tokens.get(position++);
                if (token.type() == TokenType.SYMBOL && token.text().equals(";")) {
                    return;
                }
            }
        }

        Token next(String expected) {
            if (!hasNext()) {
                throw new SqlParseException("Fim inesperado do script; esperado " + expected);
            }
            return tokens.get(position++);
        }

        void expectKeyword(String keyword) {
            Token token = next(keyword);
            if (token.type() != TokenType.WORD || !token.text().equalsIgnoreCase(keyword)) {
                throw unexpected(token, keyword);
            }
        }

        Token expect(TokenType type) {
            Token token = next(type.name());
            // Nomes de tabelas e colunas podem vir sem crases.
            if (type == TokenType.IDENTIFIER && token.type() == TokenType.WORD) {
                return new Token(TokenType.IDENTIFIER, token.text(), token.line());
            }
            if (token.type() != type) {
                throw unexpected(token, type.name());
            }
            return token;
        }

        void expectSymbol(char symbol) {
            Token token = next(String.valueOf(symbol));
            if (token.type() != TokenType.SYMBOL || token.text().charAt(0) != symbol) {
                throw unexpected(token, String.valueOf(symbol));
            }
        }

        boolean acceptSymbol(char symbol) {
            if (hasNext() && tokens.get(position).type() == TokenType.SYMBOL
                    && tokens.get(position).text().charAt(0) == symbol) {
                position++;
                return true;
            }
            return false;
        }

        Object nextValue() {
            Token token = next("valor");
            return switch (token.type()) {
                case STRING -> token.text();
                case NUMBER -> token.text().contains(".")
                        ? new BigDecimal(token.text())
                        : (Object) Long.valueOf(token.text());
                case WORD -> {
                    if (token.text().equalsIgnoreCase("NULL")) {
                        yield null;
                    }
                    if (token.text().equalsIgnoreCase("TRUE") || token.text().equalsIgnoreCase("FALSE")) {
                        yield Boolean.valueOf(token.text());
                    }
                    throw unexpected(token, "valor");
                }
                default -> throw unexpected(token, "valor");
            };
        }

        private SqlParseException unexpected(Token token, String expected) {
            return new SqlParseException("Linha " + token.line() + ": esperado " + expected
                    + ", encontrado '" + token.text() + "'");
        }
    }
}
