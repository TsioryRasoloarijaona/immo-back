package com.tsix.immocrm.services.projet;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/projet/decomptes}, liés une fois par {@code @ParameterObject @ModelAttribute}. */
public record FiltresDecomptes(
        @Parameter(description = "Projet(s) (PrjProject.PKey), répétable ; restreint au périmètre, absent = tous les projets visibles.")
        List<UUID> projetIds,
        @Parameter(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Parameter(description = "Fournisseur du décompte (CfgTier.PKey).") UUID fournisseurId,
        @Parameter(description = "Date du décompte (DocDate) minimale, incluse.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date du décompte (DocDate) maximale, incluse.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Statut de la facture du décompte (BuyDocumentStatus.PKey).") UUID statutFactureId,
        @Parameter(description = "true = seulement les décomptes sans facture ; absent ou false = tous.") Boolean sansFacture,
        @Parameter(description = "Sous-chaîne sur le code du décompte, le code ou la désignation du lot, le fournisseur.")
        String search,
        @Parameter(description = "Société (« Entité » d'OMRAN) du projet du lot (PrjProject.CfgCompanyId).") UUID societeId
) {
}
