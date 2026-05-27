package com.tpi.pokemon.shared.api;

import com.tpi.pokemon.cards.application.CardNotFoundException;
import com.tpi.pokemon.cards.infrastructure.PokemonTcgApiException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(CardNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PokemonTcgApiException.class)
    ResponseEntity<ApiErrorResponse> handleExternalApi(PokemonTcgApiException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}
