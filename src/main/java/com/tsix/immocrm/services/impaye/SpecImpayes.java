package com.tsix.immocrm.services.impaye;

import com.tsix.immocrm.domain.ChampDateImpaye;
import com.tsix.immocrm.domain.EchuA;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.impaye.ImpayeService.FiltresImpayes;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Échéances candidates des impayés, sans le filtre « reste &gt; 0 » ni le périmètre (ajouté par l'appelant).
 * Les associations lues par la ligne sont chargées par la même requête ; article, tranche, projet et société
 * en LEFT JOIN, pour qu'un dossier sans article reste listé (R5).
 */
final class SpecImpayes {

    private static final String DESCRIPTION = "description";
    private static final String CODE = "code";
    private static final String DOC_DATE = "docDate";
    private static final String ID = "id";

    private SpecImpayes() {
    }

    /** Les jointures de la requête, chargées en même temps que l'échéance. */
    private record Jointures(Join<ComDocument, ComFolder> dossier, Join<ComDocument, ComSettlementCategory> categorie,
                             Join<ComFolder, StrStatus> statut, Join<ComFolder, StkItem> article,
                             Join<StkItem, CfgTranche> tranche, Join<CfgTranche, CfgProject> projet,
                             Join<CfgProject, CfgCompany> societe, Join<ComFolder, CfgTier> client) {
    }

    static Specification<ComDocument> de(FiltresImpayes f, LocalDate aujourdhui, int age) {
        return (root, query, cb) -> {
            Jointures j = jointures(root, chargement(query.getResultType()));
            List<Predicate> predicats = new ArrayList<>();
            // R1 : catégorie d'encaissement, dossier à un statut de vente effective (NULL exclu).
            predicats.add(cb.isTrue(j.categorie().get("isCredit")));
            predicats.add(cb.isTrue(j.statut().get("venteEffective")));
            ajouterEchu(predicats, root.get(DOC_DATE), cb, f.echu(), aujourdhui, age);
            ajouterPeriode(predicats, root.get(DOC_DATE), cb, f, age);
            ajouterReferentiels(predicats, j, cb, f);
            ajouterTextes(predicats, root, j, cb, f);
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    /** Pas de {@code fetch} dans une requête de comptage : Hibernate le refuse. */
    private static boolean chargement(Class<?> typeResultat) {
        return ComDocument.class.equals(typeResultat);
    }

    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> jointure(From<?, X> depart, String attribut, JoinType type, boolean charger) {
        return charger ? (Join<X, Y>) depart.fetch(attribut, type) : depart.join(attribut, type);
    }

    private static Jointures jointures(From<?, ComDocument> root, boolean charger) {
        Join<ComDocument, ComFolder> dossier = jointure(root, "comFolder", JoinType.INNER, charger);
        Join<ComDocument, ComSettlementCategory> categorie =
                jointure(root, "comSettlementCategory", JoinType.INNER, charger);
        Join<ComFolder, StrStatus> statut = jointure(dossier, "strStatus", JoinType.INNER, charger);
        Join<ComFolder, StkItem> article = jointure(dossier, "stkItem", JoinType.LEFT, charger);
        Join<StkItem, CfgTranche> tranche = jointure(article, "stkHierarchy", JoinType.LEFT, charger);
        Join<CfgTranche, CfgProject> projet = jointure(tranche, "cfgProject", JoinType.LEFT, charger);
        Join<CfgProject, CfgCompany> societe = jointure(projet, "cfgCompany", JoinType.LEFT, charger);
        Join<ComFolder, CfgTier> client = jointure(dossier, "cfgCustomer", JoinType.LEFT, charger);
        jointure(dossier, "cfgCommercial", JoinType.LEFT, charger);
        return new Jointures(dossier, categorie, statut, article, tranche, projet, societe, client);
    }

    /** R4 : « ≤ aujourd'hui » s'écrit {@code < demain 00:00}, comme {@code StatutEcheance.EN_RETARD}. */
    private static void ajouterEchu(List<Predicate> predicats, Path<LocalDateTime> date, CriteriaBuilder cb,
                                    EchuA echu, LocalDate aujourdhui, int age) {
        LocalDateTime demain = aujourdhui.plusDays(1).atStartOfDay();
        switch (echu != null ? echu : EchuA.ECHEANCE) {
            case ECHEANCE -> predicats.add(cb.lessThan(date, demain));
            case RECOUVREMENT -> predicats.add(cb.lessThan(date, demain.minusDays(age)));
            case TOUS -> { /* aucune condition de date */ }
        }
    }

    /** Bornes {@code ≥ du} et {@code < au + 1 jour} ; la date de recouvrement se ramène sur {@code DocDate}. */
    private static void ajouterPeriode(List<Predicate> predicats, Path<LocalDateTime> date, CriteriaBuilder cb,
                                       FiltresImpayes f, int age) {
        int decalage = f.champDate() == ChampDateImpaye.RECOUVREMENT ? age : 0;
        if (f.du() != null) {
            predicats.add(cb.greaterThanOrEqualTo(date, f.du().minusDays(decalage).atStartOfDay()));
        }
        if (f.au() != null) {
            predicats.add(cb.lessThan(date, f.au().plusDays(1L - decalage).atStartOfDay()));
        }
    }

    private static void ajouterReferentiels(List<Predicate> predicats, Jointures j, CriteriaBuilder cb,
                                            FiltresImpayes f) {
        if (!CollectionUtils.isEmpty(f.projetIds())) {
            predicats.add(j.projet().get(ID).in(f.projetIds()));
        }
        if (!CollectionUtils.isEmpty(f.trancheIds())) {
            predicats.add(j.tranche().get(ID).in(f.trancheIds()));
        }
        if (f.commercialId() != null) {
            predicats.add(cb.equal(j.dossier().get("cfgCommercial").get(ID), f.commercialId()));
        }
        if (f.categorieId() != null) {
            predicats.add(cb.equal(j.categorie().get(ID), f.categorieId()));
        }
    }

    private static void ajouterTextes(List<Predicate> predicats, From<?, ComDocument> root, Jointures j,
                                      CriteriaBuilder cb, FiltresImpayes f) {
        if (StringUtils.hasText(f.client())) {
            String motif = motif(f.client());
            predicats.add(cb.or(
                    contient(cb, j.client().get(CODE), motif),
                    contient(cb, j.client().get(DESCRIPTION), motif),
                    contient(cb, j.client().get("firstName"), motif),
                    contient(cb, j.client().get("lastName"), motif)));
        }
        if (StringUtils.hasText(f.article())) {
            String exact = f.article().trim().toLowerCase(Locale.ROOT);
            predicats.add(cb.or(
                    cb.equal(cb.lower(cb.trim(j.article().get(CODE))), exact),
                    cb.equal(cb.lower(cb.trim(j.article().get(DESCRIPTION))), exact)));
        }
        if (StringUtils.hasText(f.q())) {
            String motif = motif(f.q());
            predicats.add(cb.or(
                    contient(cb, root.get("notes"), motif),
                    contient(cb, j.categorie().get(DESCRIPTION), motif),
                    contient(cb, j.categorie().get(CODE), motif),
                    contient(cb, j.tranche().get(DESCRIPTION), motif),
                    contient(cb, j.tranche().get(CODE), motif),
                    contient(cb, j.projet().get(DESCRIPTION), motif),
                    contient(cb, j.projet().get(CODE), motif),
                    contient(cb, j.societe().get(DESCRIPTION), motif),
                    contient(cb, j.dossier().get(CODE), motif)));
        }
    }

    private static String motif(String saisie) {
        return "%" + saisie.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static Predicate contient(CriteriaBuilder cb, Expression<String> colonne, String motif) {
        return cb.like(cb.lower(colonne), motif);
    }
}
