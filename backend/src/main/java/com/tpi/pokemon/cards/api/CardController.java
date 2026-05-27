package com.tpi.pokemon.cards.api;

import com.tpi.pokemon.cards.application.CardCatalogService;
import com.tpi.pokemon.cards.application.CardImportSummary;
import com.tpi.pokemon.cards.application.CardMapper;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardCatalogService service;
    private final CardMapper mapper;

    public CardController(CardCatalogService service, CardMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/import/xy1")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportSummaryResponse importXy1() {
        CardImportSummary summary = service.importXy1();
        return new ImportSummaryResponse(summary.received(), summary.created(), summary.updated(), summary.skipped(), summary.errors());
    }

    @GetMapping
    public CardPageResponse findCards(
            @RequestParam(required = false) String setId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("setId", "number", "name"));
        Page<CardResponse> cards = service.findCards(setId, name, pageable).map(mapper::toResponse);
        List<CardResponse> content = cards.getContent();
        return new CardPageResponse(content, cards.getNumber(), cards.getSize(), cards.getTotalElements(), cards.getTotalPages());
    }

    @GetMapping("/{cardId}")
    public CardResponse getByCardId(@PathVariable String cardId) {
        return mapper.toResponse(service.getByCardId(cardId));
    }
}
