package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Données du PDF « Fiche Offre vente » ({@code instructions/templates/priceOffer/OFFRE DE PRIX.pdf}). Pas exposé en JSON. */
public record OffreDevisData(
        String code,
        LocalDate date,
        String clientNom,
        String trancheDescription,
        String articleCode,
        BigDecimal superficie,
        String categorieLibelle,
        String typeLibelle,
        String commercialNom,
        String modePaiementLibelle,
        MontantsOffreDto montants,
        List<SousArticleLigne> sousArticles,
        List<LigneSchemaFinancementDto> schemaFinancement
) {
    /** Une ligne du tableau "SOUS ARTICLES" du formulaire Offre legacy. */
    public record SousArticleLigne(
            String code,
            String categorieLibelle,
            boolean toujoursAffecte,
            BigDecimal superficie,
            BigDecimal prixTTCavantRemise,
            BigDecimal remisePercent,
            BigDecimal montantRemise,
            BigDecimal prixTTCapresRemise
    ) {
    }
}
