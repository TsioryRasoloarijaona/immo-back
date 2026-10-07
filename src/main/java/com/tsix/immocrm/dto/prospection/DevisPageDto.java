package com.tsix.immocrm.dto.prospection;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * {@code PageDto<DevisDto>} plus {@link #sommePrixVenteTTCPage}, sommé sur la page courante
 * seulement : un total plein-filtre demanderait une requête d'agrégat séparée.
 */
public record DevisPageDto(
        List<DevisDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        BigDecimal sommePrixVenteTTCPage
) {
    public static DevisPageDto of(Page<DevisDto> page) {
        BigDecimal somme = page.getContent().stream()
                .map(DevisDto::montants)
                .filter(Objects::nonNull)
                .map(MontantsOffreDto::prixVenteTTC)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DevisPageDto(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), somme);
    }
}
