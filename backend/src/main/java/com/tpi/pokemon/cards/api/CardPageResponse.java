package com.tpi.pokemon.cards.api;

import java.util.List;

public record CardPageResponse(
        List<CardResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
