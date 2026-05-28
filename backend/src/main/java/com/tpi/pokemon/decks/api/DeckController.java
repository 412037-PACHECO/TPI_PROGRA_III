package com.tpi.pokemon.decks.api;

import com.tpi.pokemon.decks.application.DeckMapper;
import com.tpi.pokemon.decks.application.DeckService;
import com.tpi.pokemon.decks.application.DeckValidator;
import com.tpi.pokemon.decks.application.DeckInvalidOperationException;
import com.tpi.pokemon.decks.domain.DeckEntity;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decks")
public class DeckController {
    private final DeckService service;
    private final DeckMapper mapper;
    private final DeckValidator validator;

    public DeckController(DeckService service, DeckMapper mapper, DeckValidator validator) {
        this.service = service;
        this.mapper = mapper;
        this.validator = validator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckDetailResponse create(@RequestBody CreateDeckRequest request) {
        return mapper.toDetailResponse(service.create(request));
    }

    @GetMapping
    public List<DeckResponse> list(@RequestParam String owner) {
        return service.listByOwner(owner).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{deckId}")
    public DeckDetailResponse get(@PathVariable Long deckId) {
        return mapper.toDetailResponse(service.getDeck(deckId));
    }

    @PutMapping("/{deckId}")
    public DeckDetailResponse update(@PathVariable Long deckId, @RequestBody UpdateDeckRequest request) {
        return mapper.toDetailResponse(service.update(deckId, request));
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long deckId) {
        service.delete(deckId);
    }

    @PutMapping("/{deckId}/cards/{cardId}")
    public DeckDetailResponse addOrUpdateCard(@PathVariable Long deckId, @PathVariable String cardId, @RequestBody AddOrUpdateDeckCardRequest request) {
        if (request == null) {
            throw new DeckInvalidOperationException("El cuerpo con quantity es obligatorio");
        }
        if (request.quantity() == null) {
            throw new DeckInvalidOperationException("El campo quantity es obligatorio");
        }
        return mapper.toDetailResponse(service.addOrUpdateCard(deckId, cardId, request.quantity()));
    }

    @DeleteMapping("/{deckId}/cards/{cardId}")
    public DeckDetailResponse removeCard(@PathVariable Long deckId, @PathVariable String cardId) {
        return mapper.toDetailResponse(service.removeCard(deckId, cardId));
    }

    @GetMapping("/{deckId}/validation")
    public DeckValidationResponse validate(@PathVariable Long deckId) {
        DeckEntity deck = service.getDeck(deckId);
        return validator.validate(deck);
    }
}
