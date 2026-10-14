package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.tranche.AppartenanceTranche;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Prédicats de la liste des dossiers, sans le périmètre commercial que chaque appelant ajoute. */
public final class SpecificationsListeDossiers {

    private static final String STR_STATUS = "strStatus";
    private static final String CFG_CUSTOMER = "cfgCustomer";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String MOBILE = "mobile";
    private static final String MOBILE_INDICATIF = "mobileIndicatif";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private SpecificationsListeDossiers() {
    }

    public static Specification<ComFolder> de(FiltresListeDossiers filtres, Optional<Set<UUID>> statutsVisibles) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            ajouterStatuts(predicats, root, cb, filtres, statutsVisibles);
            if (filtres.commercial() != null) {
                predicats.add(cb.equal(root.get("cfgCommercial").get("id"), filtres.commercial()));
            }
            if (filtres.projet() != null) {
                // Par la tranche de l'article principal, jamais StkItem.CfgProjectId, colonne morte.
                predicats.add(cb.equal(
                        root.get("stkItem").get("stkHierarchy").get("cfgProject").get("id"), filtres.projet()));
            }
            if (filtres.trancheIds() != null && !filtres.trancheIds().isEmpty()) {
                predicats.add(AppartenanceTranche.dossier(root, query, cb, filtres.trancheIds()));
            }
            ajouterDates(predicats, root, cb, filtres);
            if (StringUtils.hasText(filtres.search())) {
                predicats.add(recherche(root, query, cb, filtres.search()));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    /**
     * Sans filtre de statut, les sortis (annulation / résiliation) sont exclus comme chez OMRAN
     * ({@code IsCancellation = 0}) ; un statut demandé explicitement est servi tel quel.
     */
    private static void ajouterStatuts(List<Predicate> predicats, Root<ComFolder> root, CriteriaBuilder cb,
                                       FiltresListeDossiers filtres, Optional<Set<UUID>> statutsVisibles) {
        boolean statutsDemandes = filtres.statuts() != null && !filtres.statuts().isEmpty();
        if (statutsDemandes) {
            List<String> codes = filtres.statuts().stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
            predicats.add(cb.lower(root.get(STR_STATUS).get(CODE)).in(codes));
        }
        if (!statutsDemandes && !filtres.inclureSortis()) {
            Join<ComFolder, StrStatus> statut = root.join(STR_STATUS, JoinType.LEFT);
            predicats.add(cb.and(pasVrai(cb, statut.get("annulation")), pasVrai(cb, statut.get("resiliation"))));
        }
        // Contrat de StatutsVisibles (lot A) : ensemble vide = aucun dossier.
        statutsVisibles.ifPresent(ids -> predicats.add(ids.isEmpty()
                ? cb.disjunction()
                : root.get(STR_STATUS).get("id").in(ids)));
    }

    /** NULL vaut faux, comme {@code StrStatus.estActif}. */
    private static Predicate pasVrai(CriteriaBuilder cb, Path<Boolean> drapeau) {
        return cb.or(cb.isNull(drapeau), cb.isFalse(drapeau));
    }

    private static void ajouterDates(List<Predicate> predicats, Root<ComFolder> root, CriteriaBuilder cb,
                                     FiltresListeDossiers filtres) {
        boolean periode = filtres.dateDebut() != null || filtres.dateFin() != null;
        if (!periode && filtres.dateVide() == null) {
            return;
        }
        ChampDateDossier champ = filtres.champDate() != null ? filtres.champDate() : ChampDateDossier.RESERVATION;
        Path<LocalDateTime> colonne = chemin(root, champ.getChampEntite());
        if (filtres.dateVide() != null) {
            predicats.add(Boolean.TRUE.equals(filtres.dateVide()) ? cb.isNull(colonne) : cb.isNotNull(colonne));
            return;
        }
        if (filtres.dateDebut() != null) {
            predicats.add(cb.greaterThanOrEqualTo(colonne, filtres.dateDebut()));
        }
        if (filtres.dateFin() != null) {
            predicats.add(cb.lessThanOrEqualTo(colonne, filtres.dateFin()));
        }
    }

    /** Jointures externes : « date vide » doit garder le dossier sans prospection ou sans article. */
    static Path<LocalDateTime> chemin(Root<ComFolder> root, String chemin) {
        String[] maillons = chemin.split("\\.");
        From<?, ?> courant = root;
        for (int i = 0; i < maillons.length - 1; i++) {
            courant = courant.join(maillons[i], JoinType.LEFT);
        }
        return courant.get(maillons[maillons.length - 1]);
    }

    /**
     * Les 16 champs d'OMRAN ({@code SaleMonitoringList.GetList}), espaces retirés des deux côtés, plus
     * prénom et nom seuls. Le mobile se cherche nu, avec indicatif, « + » ou « 00 » devant.
     */
    private static Predicate recherche(Root<ComFolder> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                       String search) {
        String motif = "%" + search.replace(" ", "").toLowerCase(Locale.ROOT) + "%";
        Join<ComFolder, CfgTier> client = root.join(CFG_CUSTOMER, JoinType.LEFT);
        Join<ComFolder, StkItem> article = root.join("stkItem", JoinType.LEFT);
        Expression<String> indicatifMobile = cb.concat(compact(cb, client.get(MOBILE_INDICATIF)),
                compact(cb, client.get(MOBILE)));
        List<Expression<String>> champs = List.of(
                root.get(CODE),
                client.get("socialReason"),
                client.get(DESCRIPTION),
                client.get(FIRST_NAME),
                client.get(LAST_NAME),
                cb.concat(compact(cb, client.get(FIRST_NAME)), compact(cb, client.get(LAST_NAME))),
                cb.concat(compact(cb, client.get(LAST_NAME)), compact(cb, client.get(FIRST_NAME))),
                client.get("iDNumber"),
                client.get(MOBILE),
                indicatifMobile,
                cb.concat(cb.literal("+"), indicatifMobile),
                cb.concat(cb.literal("00"), indicatifMobile),
                article.get(CODE),
                article.get(DESCRIPTION),
                article.join("stkItemCategory", JoinType.LEFT).get(CODE),
                article.join("stkItemType", JoinType.LEFT).get(CODE),
                article.join("stkPresentationType", JoinType.LEFT).get(CODE));
        List<Predicate> ou = new ArrayList<>();
        for (Expression<String> champ : champs) {
            ou.add(cb.like(cb.lower(compact(cb, champ)), motif));
        }
        ou.add(cb.exists(coAcquereur(root, query, cb, motif)));
        return cb.or(ou.toArray(new Predicate[0]));
    }

    private static Subquery<UUID> coAcquereur(Root<ComFolder> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                              String motif) {
        Subquery<UUID> sous = query.subquery(UUID.class);
        Root<ComFolderCustomer> acquereur = sous.from(ComFolderCustomer.class);
        Join<ComFolderCustomer, CfgTier> tiers = acquereur.join("cfgTier");
        sous.select(acquereur.get("id")).where(
                cb.equal(acquereur.get("comFolder"), root),
                cb.like(cb.lower(compact(cb, tiers.get(DESCRIPTION))), motif));
        return sous;
    }

    /** Espaces retirés, comme le {@code REPLACE(x, ' ', '')} d'OMRAN. */
    static Expression<String> compact(CriteriaBuilder cb, Expression<String> valeur) {
        return cb.function("REPLACE", String.class, valeur, cb.literal(" "), cb.literal(""));
    }
}
