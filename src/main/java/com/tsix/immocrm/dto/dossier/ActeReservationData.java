package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * All fields needed to render the "Acte de réservation" contract (PDF or Word), modeled on the
 * legacy template (see {@code instructions/templates/ACTE RESERVATION.pdf}). Not exposed as
 * JSON — feeds {@code ActeReservationPdfRenderer} / {@code ActeReservationDocxRenderer} only.
 */
public record ActeReservationData(
        String dossierCode,
        String clientTitre,
        String clientNom,
        String clientCin,
        String clientAdresse,
        String clientTelephone,
        String projetNom,
        String projetAdresse,
        String articleCategorie,
        String articleCode,
        String immeubleCode,
        String etage,
        BigDecimal superficie,
        List<SousArticle> sousArticles,
        BigDecimal prixVente,
        BigDecimal avanceMontant,
        BigDecimal avancePct,
        List<LignePaiement> lignesSolde,
        BigDecimal penaliteResiliationPct,
        BigDecimal transfertProduitMontant,
        BigDecimal transfertTrancheMontant,
        BigDecimal transfertClientPct,
        Integer dureeSavMois,
        BigDecimal syndicMontantMensuel,
        Integer syndicDureeMois,
        String tribunalCompetent,
        String signataireNom,
        LocalDate dateEdition
) {
    public record SousArticle(String typeLabel, BigDecimal superficie) {
    }

    public record LignePaiement(BigDecimal montant, BigDecimal pct, LocalDate date) {
    }
}
