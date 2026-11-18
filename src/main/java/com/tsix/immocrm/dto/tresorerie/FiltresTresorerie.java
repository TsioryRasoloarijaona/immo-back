package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres communs de {@code /api/tresorerie} ; liste vide ou absente = pas de filtre. */
public record FiltresTresorerie(
        @Parameter(description = "Module OMRAN des pièces : PRJ (arbre Projet) ou BUY (arbre Achat). Obligatoire, sinon 400.",
                required = true)
        ModuleTresorerie module,
        @Parameter(description = "Société (CfgCompany.PKey) ; hors périmètre = ignorée.")
        UUID societeId,
        @Parameter(description = "Projets (PrjProject.PKey), paramètre répété ; un projet hors périmètre est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "true : pièces sans projet (règlement sans ligne compris), en plus des projets cochés.")
        Boolean sansProjet,
        @Parameter(description = "Date filtrée par du / au ; absent = PIECE.")
        DateTresorerie dateSur,
        @Parameter(description = "Date filtrée à partir de ce jour inclus.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date filtrée jusqu'à ce jour inclus.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Types de règlement (StlSettlementType.PKey), paramètre répété.")
        List<UUID> typeIds,
        @Parameter(description = "Statuts de règlement (IN_WORK, POINTED, NOTPAYED, CANCELED…), paramètre répété.")
        List<String> statuts,
        @Parameter(description = "Compte bancaire (StlAccount.PKey) du règlement.")
        UUID compteId,
        @Parameter(description = "Tiers (CfgTier.PKey) du règlement ou de la pièce.")
        UUID tiersId,
        @Parameter(description = "Montant minimal (montant du règlement, net à payer d'une pièce).")
        BigDecimal montantDe,
        @Parameter(description = "Montant maximal (montant du règlement, net à payer d'une pièce).")
        BigDecimal montantA,
        @Parameter(description = "true : comptabilisés seulement ; false : non comptabilisés ; absent = tous.")
        Boolean comptabilise,
        @Parameter(description = "false : règlements annulés (CANCELED) exclus de la liste ; absent = true. Jamais dans les totaux.")
        Boolean inclureAnnules,
        @Parameter(description = "Texte cherché dans la référence, la description, le tiers, le code reçu ou la référence externe.")
        String recherche,
        @Parameter(description = "Page, à partir de 0 ; absent = 0.")
        Integer page,
        @Parameter(description = "Lignes par page, de 1 à 500 ; absent = 50.")
        Integer taille) {

    private static final int TAILLE_PAR_DEFAUT = 50;
    private static final int TAILLE_MAX = 500;

    public FiltresTresorerie {
        projetIds = projetIds == null ? List.of() : projetIds;
        typeIds = typeIds == null ? List.of() : typeIds;
        statuts = statuts == null ? List.of() : statuts;
        sansProjet = Boolean.TRUE.equals(sansProjet);
        inclureAnnules = !Boolean.FALSE.equals(inclureAnnules);
        dateSur = dateSur == null ? DateTresorerie.PIECE : dateSur;
        recherche = recherche == null || recherche.isBlank() ? null : recherche.trim();
        page = page == null ? 0 : Math.max(page, 0);
        taille = taille == null ? TAILLE_PAR_DEFAUT : Math.clamp(taille, 1, TAILLE_MAX);
    }

    /** Module OMRAN ({@code AdmModule.Code}) des écrans Projet et Achat. */
    public enum ModuleTresorerie { PRJ, BUY }

    /** Date du filtre de période ({@code StlSettlementViewFilter.GetDatesAffectationFilter}). */
    public enum DateTresorerie { PIECE, ECHEANCE, POINTAGE, POINTAGE_REPORTE }

    /** Sens d'un règlement : DEBIT = décaissement ({@code IsCredit = 0}), CREDIT = encaissement. */
    public enum SensTresorerie { DEBIT, CREDIT }
}
