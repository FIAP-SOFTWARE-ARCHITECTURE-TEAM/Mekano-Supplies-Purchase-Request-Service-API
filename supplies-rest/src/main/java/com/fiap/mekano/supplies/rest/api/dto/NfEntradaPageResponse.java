package com.fiap.mekano.supplies.rest.api.dto;

import java.util.List;

public record NfEntradaPageResponse(
        List<NfEntradaResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
