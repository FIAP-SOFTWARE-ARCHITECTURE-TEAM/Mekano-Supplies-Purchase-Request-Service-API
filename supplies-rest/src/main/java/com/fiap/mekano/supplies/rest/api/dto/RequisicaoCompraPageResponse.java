package com.fiap.mekano.supplies.rest.api.dto;

import java.util.List;

public record RequisicaoCompraPageResponse(
        List<RequisicaoCompraResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
