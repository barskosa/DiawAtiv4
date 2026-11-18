package com.pokedex.api.exception;

/** Regra de negócio violada pelos dados enviados (ex.: tipo inexistente): respondido com HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
