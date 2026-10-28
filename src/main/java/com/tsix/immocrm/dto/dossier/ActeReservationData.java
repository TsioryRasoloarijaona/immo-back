package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Données de l'« Acte de réservation » (modèle {@code instructions/templates/ACTE RESERVATION.pdf}), hors JSON. */
public record ActeReservationData(
        String dossierCode,
        String clientTitre,
        String clientNom,
        String clientCin,
        String clientAdresse,
        String clientTelephone,
        String projetNom,
        String projetAdresse,
        /** Entité juridique du projet ({@code CfgProject.cfgCompany}) : jamais de nom de société en dur. */
        String entrepriseNom,
        /** Lot terrain ({@code StkKind} TER) : le modèle omet étage, immeuble, « achèvement des travaux »
         * et la section SAV/Assurance/Syndic (les suivantes sont renumérotées). */
        boolean estTerrain,
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
        LocalDate dateEdition,
        /** Acquéreur principal puis co-acquéreurs, une page chacun du formulaire Barid ; vide si le dossier est sorti. */
        List<Destinataire> destinataires
) {
    public record SousArticle(String typeLabel, BigDecimal superficie) {
    }

    public record LignePaiement(BigDecimal montant, BigDecimal pct, LocalDate date) {
    }

    /** Destinataire d'un formulaire Barid Al-Maghrib (FEATURE-vente-formulaire-barid.md §3) ; valeur absente = null. */
    public record Destinataire(String titre, String nom, String adresse, String adresseSansCodePostal, String codePostal,
                               String societe, String societeAdresseSansCodePostal, String societeCodePostal,
                               String dossier, String tranche, String statut, String role) {
    }
}
