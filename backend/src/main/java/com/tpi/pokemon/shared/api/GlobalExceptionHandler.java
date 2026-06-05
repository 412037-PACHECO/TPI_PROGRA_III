package com.tpi.pokemon.shared.api;

import com.tpi.pokemon.cards.application.CardNotFoundException;
import com.tpi.pokemon.cards.infrastructure.PokemonTcgApiException;
import com.tpi.pokemon.decks.application.CatalogCardNotFoundException;
import com.tpi.pokemon.decks.application.DeckCardNotFoundException;
import com.tpi.pokemon.decks.application.DeckInvalidOperationException;
import com.tpi.pokemon.decks.application.DeckNotFoundException;
import com.tpi.pokemon.game.application.GameAlreadyFullException;
import com.tpi.pokemon.game.application.DeckNotValidForGameException;
import com.tpi.pokemon.game.application.GameNotInExpectedStateException;
import com.tpi.pokemon.game.application.GameNotFoundException;
import com.tpi.pokemon.game.application.InvalidGameCommandException;
import com.tpi.pokemon.game.application.PendingSelectionException;
import com.tpi.pokemon.game.application.UnauthorizedGameActionException;
import com.tpi.pokemon.game.application.UnauthorizedGameViewException;
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

    @ExceptionHandler({DeckNotFoundException.class, DeckCardNotFoundException.class, CatalogCardNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleDeckNotFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DeckInvalidOperationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidDeckOperation(DeckInvalidOperationException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PokemonTcgApiException.class)
    ResponseEntity<ApiErrorResponse> handleExternalApi(PokemonTcgApiException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleGameNotFound(GameNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({InvalidGameCommandException.class, GameAlreadyFullException.class, GameNotInExpectedStateException.class, PendingSelectionException.class, DeckNotValidForGameException.class})
    ResponseEntity<ApiErrorResponse> handleInvalidGameCommand(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({UnauthorizedGameViewException.class, UnauthorizedGameActionException.class})
    ResponseEntity<ApiErrorResponse> handleUnauthorizedGameView(RuntimeException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}
