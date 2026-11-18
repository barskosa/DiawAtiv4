package com.pokedex.api.exception;

/** Recurso inexistente: respondido com HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
