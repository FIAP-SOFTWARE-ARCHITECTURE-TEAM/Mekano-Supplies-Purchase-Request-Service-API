package com.fiap.mekano.supplies.rest.api.dto;

import java.util.List;

public record EstoquePageResponse(
        List<EstoqueResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
