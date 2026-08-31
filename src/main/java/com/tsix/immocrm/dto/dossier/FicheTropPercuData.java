package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * All fields needed to render the "Trop Perçu" receipt — modeled on the legacy Stimulsoft report
 * source {@code instructions/templates/ComSaleFicheTropPercu.yes} (a "TROP PERÇU N° {code}"
 * acknowledgement that the client's settlements exceed the dossier's price, listing every
 * versement and the resulting reste-à-payer/trop-perçu). Not exposed as JSON — feeds
 * {@code FicheTropPercuPdfRenderer} only.
 */
public record FicheTropPercuData(
        String dossierCode,
        LocalDate dateEdition,
        /** {@code CfgProject.cfgCompany.description} — never hardcode a company name, it's per-project. */
        String entrepriseNom,
        String entrepriseAdresse,
        String villeSignature,
        /** Acquéreur principal en premier, puis co-acquéreurs ({@code ComFolderCustomer}), même ordre que la Fiche Bancaire. */
        List<Acquereur> acquereurs,
        /**
         * {@code |Sum(versements) - PriceAfterDiscount|} — voir {@code resteAPayer} pour le signe
         * réel. Le montant en toutes lettres (ex. "cent mille dirhams") est calculé côté
         * renderer, pas ici — voir {@code NombreEnLettresFr}.
         */
        BigDecimal montantTropPercu,
        String articleCategorieLibelle,
        String articleCode,
        String projetDescription,
        String typeLibelle,
        BigDecimal superficie,
        /** true quand {@code StkItem.titleNumber} est vide — déclenche la mention "approximative" et l'astérisque, comme la Fiche Bancaire. */
        boolean superficieApproximative,
        List<SousArticle> sousArticles,
        BigDecimal prixApresRemise,
        BigDecimal unitCost,
        /** Total des frais (lignes d'échéancier {@code excludeFromPrice=true}) — voir {@code DossierService#montants}. */
        BigDecimal fraisTotal,
        List<LigneReglement> lignesReglement,
        /** Somme brute des lignes de règlement (non signée) — correspond au "Total" du tableau des versements. */
        BigDecimal totalRegle,
        /** {@code PriceAfterDiscount - totalRegle} — négatif quand il y a effectivement un trop perçu. */
        BigDecimal resteAPayer
) {
    public record Acquereur(String titre, String nom, String pieceIdentite) {
    }

    public record SousArticle(String categorieLibelle, String categorieCode, String code, BigDecimal area) {
    }

    public record LigneReglement(LocalDate date, BigDecimal montant, boolean credit, String mode) {
    }
}
