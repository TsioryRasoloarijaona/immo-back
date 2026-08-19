package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields needed to render the "Fiche de réservation" document (PDF or Word), modeled on the
 * legacy "FICHE RESERVATION MBR" template. Not exposed as JSON — feeds
 * {@code FicheReservationPdfRenderer} / {@code FicheReservationDocxRenderer} only.
 */
public record FicheReservationData(
        String dossierCode,
        String clientNom,
        String clientAdresse,
        String clientCin,
        String immeubleCode,
        String articleCode,
        String porte,
        String etage,
        BigDecimal superficie,
        String projetNom,
        String projetAdresse,
        String modePaiement,
        BigDecimal montant,
        String reglePar,
        BigDecimal prixArticle,
        LocalDate dateEdition
) {
}
