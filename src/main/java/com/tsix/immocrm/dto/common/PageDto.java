package com.tsix.immocrm.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Enveloppe de toutes les listes GET ; les détails et agrégats ne sont pas enveloppés. */
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
