package com.tsix.immocrm.dto.projet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Champ absent ou null = valeur stockée gardée ; les montants calculés du corps sont ignorés (recalculés). */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Enregistrer un décompte : le JSON de la fiche, champs saisissables seulement lus ; null = inchangé.")
public record EnregistrerDecompteRequest(
        @Schema(description = "Date du décompte, ≥ date du décompte précédent et ≥ date d'ordre de service du lot.")
        LocalDate dateDocument,
        @Schema(description = "Référence interne (DocRef).") String referenceInterne,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Tiers du décompte (CfgTier.PKey).") UUID fournisseurId,
        @Schema(description = "Avance basée sur le montant : avanceAmortie est alors lue.") Boolean avanceSurMontant,
        @Schema(description = "RG basée sur le montant : retenueGarantie est alors lue.") Boolean retenueGarantieSurMontant,
        @Schema(description = "Assurance décennale basée sur le montant : assuranceDEC est alors lue.") Boolean assuranceDECSurMontant,
        @Schema(description = "Assurance TRC basée sur le montant : assuranceTRC est alors lue.") Boolean assuranceTRCSurMontant,
        @Schema(description = "Compte prorata basé sur le montant : compteProrata est alors lu.") Boolean compteProrataSurMontant,
        @Schema(description = "Taux d'avance en pourcentage, lu hors « basé sur le montant ».") BigDecimal tauxAvance,
        @Schema(description = "Taux de RG en pourcentage, lu hors « basé sur le montant ».") BigDecimal tauxRetenueGarantie,
        @Schema(description = "Taux d'assurance décennale en pourcentage.") BigDecimal tauxAssuranceDEC,
        @Schema(description = "Taux d'assurance TRC en pourcentage.") BigDecimal tauxAssuranceTRC,
        @Schema(description = "Taux du compte prorata en pourcentage.") BigDecimal tauxCompteProrata,
        @Schema(description = "Avance amortie saisie (si avanceSurMontant).") BigDecimal avanceAmortie,
        @Schema(description = "Retenue de garantie saisie (si retenueGarantieSurMontant).") BigDecimal retenueGarantie,
        @Schema(description = "Assurance décennale saisie (si assuranceDECSurMontant).") BigDecimal assuranceDEC,
        @Schema(description = "Assurance TRC saisie (si assuranceTRCSurMontant).") BigDecimal assuranceTRC,
        @Schema(description = "Compte prorata saisi (si compteProrataSurMontant).") BigDecimal compteProrata,
        @Schema(description = "Avance sur décompte, plafonnée au reste à la création.") BigDecimal avanceSurDecompte,
        @Schema(description = "Remboursement du compte prorata, plafonné au reste du projet.") BigDecimal remboursementCompteProrata,
        @Schema(description = "Fluctuations, nouvelle valeur.") BigDecimal fluctuations,
        @Schema(description = "Taux de remboursement de l'approvisionnement en pourcentage.") BigDecimal tauxApprovisionnement,
        @Schema(description = "Régularisation (AmountRegul).") BigDecimal regularisation,
        @Schema(description = "Dernier décompte du lot (FinalWorkingOut).") Boolean dernier,
        @Schema(description = "Lignes ; null = inchangées ; une ligne sans id = lignes rechargées depuis le bordereau.")
        List<LigneSaisie> lignes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(name = "LigneDecompteSaisie", description = "Quantité saisie d'une ligne de décompte.")
    public record LigneSaisie(
            @Schema(description = "PrjWorkingOutLine.Pkey ; null pour une ligne de l'aperçu « lignes rechargées ».") UUID id,
            @Schema(description = "Ligne de bordereau (PrjMarketLine.PKey).") UUID ligneMarcheId,
            @Schema(description = "Quantité saisie (QuantityOrigin) ; null = inchangée.") BigDecimal quantiteSaisie,
            @Schema(description = "Coefficient ; null = inchangé (1 pour une ligne rechargée).") BigDecimal coefficient
    ) {
    }
}
