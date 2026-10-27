package com.tsix.immocrm.dto.patrimoine.impressions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Une lettre de Relance 2 : un acquéreur d'un dossier retenu (même ordre que les pages du PDF).")
public record LigneRelance2Dto(
        @Schema(description = "Id du dossier (ComFolder.PKey).")
        UUID dossierId,
        @Schema(description = "Code du dossier.", example = "APF 2022/0012")
        String codeDossier,
        @Schema(description = "Code de l'article du dossier (sous-article compris).", example = "APF-0-12")
        String article,
        @Schema(description = "Libellé du statut du dossier (StrStatus.Description).", example = "CONCRETISATION")
        String statut,
        @Schema(description = "Nom de l'acquéreur destinataire de la lettre.")
        String acquereur,
        @Schema(description = "Vrai pour un co-acquéreur (ComFolderCustomer), faux pour le client principal.")
        boolean estCoAcquereur,
        @Schema(description = "Prix de vente du dossier (onglet Montants, prixVente.montant).", example = "1200000.00")
        BigDecimal prixVente,
        @Schema(description = "Réglé sur le prix (prixVente.regle), règlements annulés exclus.", example = "250000.00")
        BigDecimal prixRegle,
        @Schema(description = "Reste du prix (prixVente.reste), imprimé dans la lettre ; toujours > 0.", example = "950000.00")
        BigDecimal reste,
        @Schema(description = "Date de 2e relance du dossier (Revival2), null si absente.")
        LocalDate dateRelance2) {
}
