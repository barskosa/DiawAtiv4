package com.pokedex.api.exception;

/** Dado já existente (número ou nome repetido): respondido com HTTP 409. */
public class ConflictException extends RuntimeException {

    private final String field;

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
