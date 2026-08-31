package com.tsix.immocrm.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Uniform pagination envelope for every GET list endpoint (default page size 10, see
 * {@link com.tsix.immocrm.web.PaginationDefaults}). Detail endpoints (by id) and computed
 * aggregates ({@code GET /api/dossiers/{id}/montants}, {@code GET /api/dashboard}) are single
 * objects, not collections, and are not wrapped by this DTO.
 */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
