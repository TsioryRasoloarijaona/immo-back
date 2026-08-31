package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * All fields needed to render the "Fiche Bancaire" document (PDF), modeled on the legacy
 * "FICHE BANCAIRE" Stimulsoft template — a mortgage-application support sheet listing every
 * acquéreur's civil status/identity, the property's full designation, and the price/frais
 * réglé-reste breakdown. Not exposed as JSON — feeds {@code FicheBancairePdfRenderer} only.
 */
public record FicheBancaireData(
        String dossierCode,
        /** {@code ComFolder.BankRecordDate} — null means the legacy form itself is blocked
         * ("Vous devez saisir la date de la fiche bancaire...") until it's set. */
        LocalDate dateFiche,
        String entrepriseNom,
        String projetNom,
        String projetAdresse,
        String villeSignature,

        /** Acquéreur principal (ComFolder.cfgCustomer) suivi des co-acquéreurs (ComFolderCustomer). */
        List<Acquereur> acquereurs,

        // -- II. Désignation du bien --
        String categorieLibelle,
        String immeubleCode,
        String etage,
        String articleCode,
        String fractionDivisee,
        String proprieteDite,
        String logementUsage,
        BigDecimal superficie,
        String titreFoncierMereImmeuble,
        String titreFoncierParcellaire,
        BigDecimal partiesDivisesSur10000,
        BigDecimal partiesIndivisesSur10000,
        String fractionDiviseeTerrain,
        BigDecimal superficieSousArticles,

        // -- III. Prix du bien / modalités de paiement --
        BigDecimal prixApresRemise,
        BigDecimal montantVerse,
        BigDecimal reliquat,
        BigDecimal fraisDossier,
        BigDecimal fraisDossierVerse,
        BigDecimal fraisDossierReliquat,
        BigDecimal fraisSyndic,
        BigDecimal fraisSyndicVerse,
        BigDecimal fraisSyndicReliquat
) {
    public record Acquereur(
            String nom,
            String profession,
            String adresse,
            LocalDate dateNaissance,
            String lieuNaissance,
            String telephone,
            String pieceIdentite,
            BigDecimal partPercent,
            String situationFamiliale
    ) {
    }
}
