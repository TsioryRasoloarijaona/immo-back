package com.tsix.immocrm.dto.achat;

import com.tsix.immocrm.domain.achat.ChampDateAchat;
import com.tsix.immocrm.domain.achat.ModuleAchat;
import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Filtres de la liste des pièces d'achat, partagés par la liste et l'export ; liste vide ou absente = pas de filtre. */
public record FiltresDocumentsAchat(
        @Parameter(description = "Natures (DocType), paramètre répété ; valeur inconnue = 400.", example = "INVOICE")
        List<NatureDocumentAchat> natures,
        @Parameter(description = "Statuts (BuyDocumentStatus.id), paramètre répété.")
        List<UUID> statutIds,
        @Parameter(description = "Projets (PrjProject.id), paramètre répété ; un projet hors périmètre est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "Lot de marché (PrjMarket.id).")
        UUID lotId,
        @Parameter(description = "Fournisseur (CfgTier.id).")
        UUID fournisseurId,
        @Parameter(description = "Date (colonne de champDate) à partir de ce jour inclus.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date (colonne de champDate) jusqu'à ce jour inclus.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "true : pièces dont le reste à régler dépasse 0,05 (trop réglées exclues).")
        Boolean nonSoldes,
        @Parameter(description = "Recherche multi-critère insensible à la casse ; champs lus : voir la description de la liste.")
        String search,
        @Parameter(description = "true : statut « OK pour paiement » seulement ; absent ou false : pas de filtre.")
        Boolean okPourPaiement,
        @Parameter(description = "true : pièces du module Projet (PRJ) seulement, comme Projet › Dépenses ; absent : tous.")
        Boolean moduleProjet,
        @Parameter(description = "Sociétés (CfgCompany.id), paramètre répété ; une société non habilitée est ignorée.")
        List<UUID> societeIds,
        @Parameter(description = "Modules de la pièce (AdmModule.Code), paramètre répété ; valeur inconnue = 400.", example = "BUY")
        List<ModuleAchat> modules,
        @Parameter(description = "Types de lot (PrjMarketType.id), paramètre répété.")
        List<UUID> typeLotIds,
        @Parameter(description = "true : archivées (IsClosed = 1) ; false : non archivées (0 ou NULL) ; absent : toutes.")
        Boolean archive,
        @Parameter(description = "true : comptabilisées (IsAccounted = 1) ; false : 0 ou NULL ; absent : toutes.")
        Boolean comptabilise,
        @Parameter(description = "Montant à payer (AmountToPay) supérieur ou égal.", example = "100")
        BigDecimal montantDe,
        @Parameter(description = "Montant à payer (AmountToPay) inférieur ou égal.", example = "5000")
        BigDecimal montantA,
        @Parameter(description = "Colonne portée par du / au : DATE_PIECE (défaut) ou ECHEANCE ; valeur inconnue = 400.")
        ChampDateAchat champDate,
        @Parameter(description = "true : la colonne de champDate est renseignée, du / au ignorés ; false ou absent : sans effet.")
        Boolean dateRenseignee) {

    public FiltresDocumentsAchat {
        natures = presents(natures);
        statutIds = presents(statutIds);
        projetIds = presents(projetIds);
        nonSoldes = Boolean.TRUE.equals(nonSoldes);
        okPourPaiement = Boolean.TRUE.equals(okPourPaiement);
        moduleProjet = Boolean.TRUE.equals(moduleProjet);
        societeIds = presents(societeIds);
        modules = presents(modules);
        typeLotIds = presents(typeLotIds);
        champDate = champDate == null ? ChampDateAchat.DATE_PIECE : champDate;
        dateRenseignee = Boolean.TRUE.equals(dateRenseignee);
    }

    /** Pièces des {@code natures} et des {@code projetIds}, sans autre filtre. */
    public static FiltresDocumentsAchat de(List<NatureDocumentAchat> natures, List<UUID> projetIds) {
        return new FiltresDocumentsAchat(natures, null, projetIds, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    // Un paramètre vide sur le fil (societeIds=) arrive en élément null : rien coché = pas de filtre.
    private static <T> List<T> presents(List<T> valeurs) {
        return valeurs == null ? List.of() : valeurs.stream().filter(Objects::nonNull).toList();
    }
}
