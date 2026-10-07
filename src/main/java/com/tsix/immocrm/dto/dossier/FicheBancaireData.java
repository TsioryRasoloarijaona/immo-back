package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Données de la « Fiche Bancaire » (modèle Stimulsoft légataire, support de demande de crédit), hors JSON. */
public record FicheBancaireData(
        String dossierCode,
        /** {@code ComFolder.BankRecordDate} : nulle, le légataire bloque l'édition (« Vous devez saisir la date… »). */
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
