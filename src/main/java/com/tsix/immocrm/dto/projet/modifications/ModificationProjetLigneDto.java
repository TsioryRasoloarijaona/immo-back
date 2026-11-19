package com.tsix.immocrm.dto.projet.modifications;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Demande de modification d'un acquéreur, avec son bien (article racine) et son dossier de vente.")
public record ModificationProjetLigneDto(
        @Schema(description = "ComModification.PKey.") UUID id,
        @Schema(description = "Dossier de vente (ComFolder.PKey).") UUID dossierId,
        EtatModificationProjetDto etat,
        @Schema(description = "Date de la demande.") LocalDate dateDemande,
        @Schema(description = "Tranche du bien (CfgTranche.Description), « Projet » d'OMRAN.") String projet,
        @Schema(description = "Catégorie du bien (StkItemCategory), null si absente.") String categorie,
        @Schema(description = "Type du bien (StkItemType), null si absent.") String type,
        @Schema(description = "Code du bien (StkItem.Code).") String code,
        @Schema(description = "Bloc (StkBuilding), null si absent.") String bloc,
        @Schema(description = "Étage (StkFloor.Sorting), null si absent.") Integer etage,
        @Schema(description = "Description de la demande.") String description,
        @Schema(description = "Acquéreur (CfgTier du dossier), null si absent.") String acquereur,
        @Schema(description = "Premier téléphone renseigné de l'acquéreur (fixe, autres, mobile).") String telephone,
        @Schema(description = "Statut du dossier de vente (StrStatus.Description).") String etatDossier,
        @Schema(description = "Date de validation technique.") LocalDate dateValidationTechnique,
        @Schema(description = "Date de validation client.") LocalDate dateValidationClient,
        @Schema(description = "Coût.") BigDecimal cout,
        @Schema(description = "Montant HT (CommercialAmountHT).") BigDecimal montantHT,
        @Schema(description = "Montant TTC (CommercialAmount).") BigDecimal montantTTC,
        @Schema(description = "Notes.") String notes) {
}
