package com.tsix.immocrm.dto.suivicommerciaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vue OMRAN « Suivi des commerciaux » : indicateurs du mois et de l'année choisis.")
public record MensuelSuiviCommerciauxDto(BlocMensuel mois, BlocMensuel annee) {

    @Schema(description = "Une ligne par commercial, la ligne Total (somme) et la ligne Moyenne (somme / lignes).")
    public record BlocMensuel(List<IndicateursCommercial> lignes, IndicateursCommercial total,
                              IndicateursCommercial moyenne) {
    }

    @Schema(description = "Indicateurs d'un commercial (ou Total / Moyenne) ; un ratio à dénominateur nul vaut null.")
    public record IndicateursCommercial(
            @Schema(description = "Employé, sinon compte ; null pour Total et Moyenne.") UUID commercialCle,
            String commercialNom,
            @Schema(description = "Prospections assignées (CfgCommercialId) créées sur la période.")
            BigDecimal nbProspections,
            @Schema(description = "Visites de prospection saisies par le commercial, sans filtre d'article.")
            BigDecimal nbVisitesProspection,
            @Schema(description = "Visites de dossier de vente saisies par le commercial.") BigDecimal nbVisitesVente,
            @Schema(description = "Dossiers du commercial accordés (AgreementDate) sur la période, sortis exclus.")
            BigDecimal nbVentes,
            @Schema(description = "Ventes rattachées à une prospection.") BigDecimal nbVentesIssuesProspection,
            @Schema(description = "Σ ComFolder.Price des ventes.") BigDecimal prixArticles,
            @Schema(description = "Σ lignes de règlement au crédit et comptabilisées, datées ISNULL(EcheanceDate, DocDate).")
            BigDecimal versements,
            @Schema(description = "nbVentes / nbProspections.") BigDecimal perfVente,
            @Schema(description = "nbVentes / nbVisitesProspection.") BigDecimal perfVisite,
            @Schema(description = "nbVentesIssuesProspection / nbProspections.") BigDecimal perfProspection,
            @Schema(description = "versements / prixArticles.") BigDecimal perfRecouvrement) {
    }
}
