package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Enveloppe de pagination de {@code GET /api/devis}, identique à {@code PageDto<DevisDto>} mais
 * complétée d'un agrégat {@link #sommePrixVenteTTCPage}. Cet agrégat est calculé uniquement sur
 * les offres de la page courante (celles renvoyées dans {@link #content}), pas sur l'ensemble des
 * résultats filtrés — un vrai total plein-filtre demanderait une requête d'agrégation SQL séparée
 * (SUM sur la Specification, hors pagination), non implémentée pour l'instant. Le nom du champ le
 * signale explicitement pour éviter toute confusion côté frontend.
 */
@Schema(description = "Liste paginée des offres (devis), avec la somme des prix de vente TTC de "
        + "la page courante uniquement (voir la description du champ pour le détail).")
public record DevisPageDto(
        List<DevisDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        @Schema(description = "Somme des prixVenteTTC des offres de la page courante uniquement "
                + "(pas un total plein-filtre sur l'ensemble des résultats) — offres sans montant "
                + "renseigné exclues de la somme.")
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
