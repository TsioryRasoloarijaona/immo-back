package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Données de la « Fiche de réservation » (modèle légataire « FICHE RESERVATION MBR »), hors JSON. */
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
        /** Entité juridique du projet ({@code CfgProject.cfgCompany}) : jamais de nom de société en dur. */
        String entrepriseNom,
        String modePaiement,
        BigDecimal montant,
        String reglePar,
        BigDecimal prixArticle,
        LocalDate dateEdition
) {
}
