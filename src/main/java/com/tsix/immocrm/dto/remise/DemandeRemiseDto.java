package com.tsix.immocrm.dto.remise;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une demande de remise d'agence et le contexte du dossier pour la décider. Taux en pourcentage 0-100.")
public record DemandeRemiseDto(
        @Schema(description = "Id de la demande.")
        UUID id,

        @Schema(description = "Id du dossier de vente concerné.")
        UUID dossierId,

        @Schema(description = "Statut de la demande.")
        StatutDemandeRemise statut,

        @Schema(description = "Taux de remise demandé par l'agence, en pourcentage.")
        BigDecimal tauxDemande,

        @Schema(description = "Taux accordé par le siège, en pourcentage ; null tant que la demande n'est pas acceptée.",
                nullable = true)
        BigDecimal tauxAccorde,

        @Schema(description = "Justification saisie par l'agence.")
        String motif,

        @Schema(description = "Motif du refus saisi par le siège ; null sauf statut REFUSEE.", nullable = true)
        String motifRefus,

        @Schema(description = "Date et heure de la demande.")
        LocalDateTime demandeLe,

        @Schema(description = "Date et heure de la décision ou de l'annulation ; null si en attente.", nullable = true)
        LocalDateTime traiteLe,

        @Schema(description = "N° du dossier (ComFolder.Code).")
        String dossierCode,

        @Schema(description = "Nom de l'agence qui porte le dossier.", nullable = true)
        String agence,

        @Schema(description = "Vendeur du dossier (commercial).", nullable = true)
        String vendeur,

        @Schema(description = "Code du lot vendu.", nullable = true)
        String lot,

        @Schema(description = "Prix de vente actuel du dossier.", nullable = true)
        BigDecimal prix,

        @Schema(description = "Taux de remise actuel du dossier en pourcentage, calculé à la lecture.", nullable = true)
        BigDecimal tauxActuel
) {
}
