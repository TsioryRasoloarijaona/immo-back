package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de la position fournisseurs, partagés par la liste, l'export et la fenêtre « Ligne de solde ». */
public record FiltresPositionsFournisseurs(
        @Parameter(description = "Projets (PrjProject.id), paramètre répété ; exclut les mouvements sans projet ; hors périmètre ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "Sociétés (CfgCompany.id), paramètre répété ; une société non habilitée est ignorée.")
        List<UUID> societeIds,
        @Parameter(description = "Début de période (DocDate) inclus ; absent : solde début à 0.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Fin de période (DocDate) incluse ; absent : solde fin sur tout l'historique.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Nom du fournisseur, espaces et casse ignorés.")
        String search,
        @Parameter(description = "Masque les fournisseurs dont le solde fin est entre −1 et 1 ; absent : vrai.")
        Boolean masquerResteZero) {

    public FiltresPositionsFournisseurs {
        projetIds = projetIds == null ? List.of() : projetIds;
        societeIds = societeIds == null ? List.of() : societeIds;
        masquerResteZero = !Boolean.FALSE.equals(masquerResteZero);
    }
}
