package com.pokedex.api.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte as exceções em respostas no formato Problem Details (RFC 9457), com mensagens em
 * português e, nos erros de validação, o campo {@code errors} indicando cada campo inválido.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Conflito", exception.getMessage(),
                Map.of(exception.getField(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalid(InvalidRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", exception.getMessage(),
                Map.of(exception.getField(), exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            // Campos de listas chegam como "types[0]"; o frontend exibe o erro junto de "types".
            String field = error.getField().replaceAll("\\[\\d+]$", "");
            errors.merge(field, error.getDefaultMessage(), (first, second) -> first);
        }
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Corrija os campos destacados.", errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Parâmetros inválidos na requisição.", null));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "JSON inválido",
                        "O corpo da requisição não é um JSON válido ou possui valores no formato errado.", null));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String parameter = exception.getParameterName();
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                        "O parâmetro " + parameter + " é obrigatório.", Map.of(parameter, "é obrigatório")));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String parameter = exception.getPropertyName() == null ? "parâmetro" : exception.getPropertyName();
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                        "Valor inválido para " + parameter + ": " + exception.getValue() + ".", null));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
