package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Un règlement (`StlSettlement`) vu depuis un dossier : total, part affectée et lignes de ce dossier.")
public record ReglementDossierDto(

        @Schema(description = "`StlSettlement.PKey`, le `reglementId` des routes statut, pièces jointes et reçu.")
        UUID id,

        @Schema(description = "Référence (`StlSettlement.Code`) : n° de chèque, de virement…")
        String reference,

        @Schema(description = "Date de saisie (`StlSettlement.DocDate`).")
        LocalDateTime dateDocument,

        @Schema(description = "Date d'échéance du règlement (`StlSettlement.EcheanceDate`).")
        LocalDateTime dateEcheance,

        @Schema(description = "Date de règlement effectif (`SettlementDate`), renseignée seulement si `POINTED`.")
        LocalDateTime dateReglement,

        @Schema(description = "Montant total du règlement (`StlSettlement.Amount`).", example = "52200.00")
        BigDecimal montant,

        BigDecimal montantAffecte,

        BigDecimal resteAAffecter,

        BigDecimal montantAffecteAutresDossiers,

        UUID typeId,
        String typeCode,
        String typeLibelle,
        UUID banqueId,
        String banqueNom,

        @Schema(description = "Qui a réglé (`TierPayerId`, sinon `TierId`).")
        UUID payeurId,
        String payeurNom,

        String notes,

        @Schema(description = "IN_WORK (saisi), POINTED (encaissé) ou CANCELED ; `null` sur d'anciennes lignes.",
                example = "IN_WORK")
        String statut,

        @Schema(description = "Lignes de ce dossier, par date d'échéance croissante.")
        List<AffectationReglementDto> affectations
) {
}
