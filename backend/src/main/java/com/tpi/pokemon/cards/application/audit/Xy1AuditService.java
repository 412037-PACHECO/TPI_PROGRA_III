package com.tpi.pokemon.cards.application.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.application.CardCatalogService;
import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.cards.domain.CardRepository;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCatalog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Xy1AuditService {
    private final CardRepository repository;
    private final Xy1AuditReportGenerator reportGenerator;

    public Xy1AuditService(CardRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        Xy1EffectCatalog effectCatalog = new Xy1EffectCatalog();
        this.reportGenerator = new Xy1AuditReportGenerator(new Xy1CardClassifier(objectMapper, effectCatalog));
    }

    @Transactional(readOnly = true)
    public Xy1AuditReport generateReportFromLocalCache() {
        List<CardEntity> cards = repository.findBySetIdIgnoreCase(CardCatalogService.XY1_SET_ID, Pageable.unpaged()).getContent();
        return reportGenerator.generate(cards);
    }
}
