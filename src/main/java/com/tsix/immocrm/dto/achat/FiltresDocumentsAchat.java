package com.tsix.immocrm.dto.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de la liste des pièces d'achat, partagés par la liste et l'export ; liste vide ou absente = pas de filtre. */
public record FiltresDocumentsAchat(
        @Parameter(description = "Natures (DocType), paramètre répété ; valeur inconnue = 400.", example = "INVOICE")
        List<NatureDocumentAchat> natures,
        @Parameter(description = "Statuts (BuyDocumentStatus.id), paramètre répété.")
        List<UUID> statutIds,
        @Parameter(description = "Projets (PrjProject.id), paramètre répété ; un projet hors périmètre est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "Lot de marché (PrjMarket.id).")
        UUID lotId,
        @Parameter(description = "Fournisseur (CfgTier.id).")
        UUID fournisseurId,
        @Parameter(description = "Date de la pièce à partir de ce jour inclus.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date de la pièce jusqu'à ce jour inclus.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "true : pièces dont le reste à régler dépasse 0,05 (trop réglées exclues).")
        Boolean nonSoldes,
        @Parameter(description = "Recherche insensible à la casse : code, référence, description, fournisseur.")
        String search) {

    public FiltresDocumentsAchat {
        natures = natures == null ? List.of() : natures;
        statutIds = statutIds == null ? List.of() : statutIds;
        projetIds = projetIds == null ? List.of() : projetIds;
        nonSoldes = Boolean.TRUE.equals(nonSoldes);
    }
}
