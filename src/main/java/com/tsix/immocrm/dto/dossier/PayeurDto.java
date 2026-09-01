package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Une personne susceptible de régler une échéance de ce dossier — alimente le champ « Réglé par »
 * ({@code CreerReglementRequest.payeurId}).
 */
@Schema(description = "Payeur possible d'un règlement : acquéreur principal ou co-acquéreur du dossier.")
public record PayeurDto(

        @Schema(description = "`CfgTier.PKey` — à envoyer tel quel comme `payeurId` à "
                + "`POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements`.")
        UUID id,

        @Schema(description = "Nom d'affichage du tiers.", example = "Fouad EL IHFADI")
        String nom,

        @Schema(description = "Rôle sur ce dossier. `ACQUEREUR_PRINCIPAL` (`ComFolder.CfgCustomerId`, "
                + "au plus un) ou `CO_ACQUEREUR` (`ComFolderCustomer`).",
                allowableValues = {"ACQUEREUR_PRINCIPAL", "CO_ACQUEREUR"})
        String role,

        @Schema(description = "Pièce d'identité, au même format que sur l'« Autorisation "
                + "d'encaissement » (`CIN: E400556`) — utile pour distinguer deux homonymes dans "
                + "une liste déroulante. `null` si ni le type ni le numéro ne sont renseignés.",
                example = "CIN: E400556")
        String pieceIdentite,

        @Schema(description = "Quote-part du co-acquéreur en pourcentage (`ComFolderCustomer.PartPercent`). "
                + "Toujours `null` pour l'acquéreur principal, qui n'a pas de ligne `ComFolderCustomer`.")
        BigDecimal quotePart
) {
}
