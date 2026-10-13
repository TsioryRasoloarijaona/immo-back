package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Données du reçu « Trop Perçu » (modèle {@code instructions/templates/ComSaleFicheTropPercu.yes}), hors JSON. */
public record FicheTropPercuData(
        String dossierCode,
        LocalDate dateEdition,
        /** {@code CfgProject.cfgCompany} : jamais de nom de société en dur. */
        String entrepriseNom,
        String entrepriseAdresse,
        String villeSignature,
        /** Acquéreur principal puis co-acquéreurs ({@code ComFolderCustomer}). */
        List<Acquereur> acquereurs,
        /** {@code MontantsDossierDto.tropPercu} (formule de {@code ComSaleFolderView}) : 0 pour un dossier débiteur. */
        BigDecimal montantTropPercu,
        String articleCategorieLibelle,
        String articleCode,
        String projetDescription,
        String typeLibelle,
        BigDecimal superficie,
        /** {@code StkItem.titleNumber} vide : mention « approximative » et astérisque, comme la Fiche Bancaire. */
        boolean superficieApproximative,
        List<SousArticle> sousArticles,
        BigDecimal prixApresRemise,
        BigDecimal unitCost,
        /** Lignes d'échéancier {@code excludeFromPrice=true} — voir {@code DossierService#montants}. */
        BigDecimal fraisTotal,
        List<LigneReglement> lignesReglement,
        /** Somme brute, non signée, des lignes de règlement : le « Total » du tableau des versements. */
        BigDecimal totalRegle,
        /** Reste du net à payer de {@code GET .../montants} (frais compris), ramené à 0 : l'excédent est le trop-perçu. */
        BigDecimal resteAPayer
) {
    public record Acquereur(String titre, String nom, String pieceIdentite) {
    }

    public record SousArticle(String categorieLibelle, String categorieCode, String code, BigDecimal area) {
    }

    public record LigneReglement(LocalDate date, BigDecimal montant, boolean credit, String mode) {
    }
}
