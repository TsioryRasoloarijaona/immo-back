package com.tsix.immocrm.services.suiviaffectations;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres communs à la liste, aux groupes, aux totaux et à l'export du suivi des affectations (R5). */
public record FiltresSuiviAffectations(
        @Parameter(description = "Id(s) d'entité (société du projet, CfgCompany.id), répétable.") List<UUID> entiteId,
        @Parameter(description = "Id du projet (CfgProject.id) de la tranche.") UUID projetId,
        @Parameter(description = "Id(s) de tranche (CfgTranche.id), répétable.") List<UUID> trancheId,
        @Parameter(description = "Id(s) de catégorie d'échéance (ComSettlementCategory.id) ou SANS_CATEGORIE.",
                example = "SANS_CATEGORIE") List<String> categorieId,
        @Parameter(description = "Id(s) de type de pièce (StlSettlementType.id), répétable.") List<UUID> typeId,
        @Parameter(description = "Code(s) de statut du dossier (StrStatus.Code) ou SANS_STATUT ; inconnu = 400.",
                example = "RESERVATION") List<String> statut,
        @Parameter(description = "État(s) de pièce : IN_WORK, POINTED, CANCELED ou SANS_ETAT ; inconnu = 400.",
                example = "IN_WORK") List<String> etatPiece,
        @Parameter(description = "TOUS (défaut), CREDIT (IsCredit ≠ 0, NULL compris) ou DEBIT (IsCredit = 0).")
        Sens sens,
        @Parameter(description = "Recherche multi-champs, espaces ignorés, insensible à la casse.") String search,
        @Parameter(description = "Borne basse incluse de dateMouvement, ISO.", example = "2025-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Borne haute incluse de dateMouvement, ISO ; du > au = 400.", example = "2025-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {

    public enum Sens { TOUS, CREDIT, DEBIT }

    public FiltresSuiviAffectations {
        sens = sens != null ? sens : Sens.TOUS;
    }
}
