package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Lignes « Libre » de la liste des dossiers : article racine, hors annexe, sans dossier
 * ({@code ComFolderId IS NULL}) ni article principal d'un dossier en cours, dans le périmètre et une entité active.
 * Le commercial les écarte toutes ; la période ne les filtre pas (aucune date de dossier).
 */
@Component
public class LotsLibres {

    private static final String CODE = "code";

    private final EntityManager em;
    private final PerimetreCommercialService perimetreService;

    public LotsLibres(EntityManager em, PerimetreCommercialService perimetreService) {
        this.em = em;
        this.perimetreService = perimetreService;
    }

    public long compter(FiltresListeDossiers filtres) {
        if (exclutLesLibres(filtres)) {
            return 0;
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<StkItem> article = cq.from(StkItem.class);
        Jointures j = new Jointures(article);
        cq.select(cb.count(article)).where(predicats(cb, article, j, filtres, cq));
        return em.createQuery(cq).getSingleResult();
    }

    /** Tri d'OMRAN : article, tranche, projet, entité. */
    public List<StkItem> tranche(FiltresListeDossiers filtres, long debut, int nombre) {
        if (nombre <= 0 || exclutLesLibres(filtres)) {
            return List.of();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StkItem> cq = cb.createQuery(StkItem.class);
        Root<StkItem> article = cq.from(StkItem.class);
        Jointures j = new Jointures(article);
        cq.select(article).where(predicats(cb, article, j, filtres, cq)).orderBy(
                cb.asc(article.get(CODE)), cb.asc(j.tranche.get(CODE)), cb.asc(j.projet.get(CODE)),
                cb.asc(j.societe.get(CODE)), cb.asc(article.get("id")));
        return em.createQuery(cq).setFirstResult(Math.toIntExact(debut)).setMaxResults(nombre).getResultList();
    }

    /** La période ne filtre pas les lots libres : ils n'ont aucune date de dossier (FEATURE-vente-liste.md §2). */
    private static boolean exclutLesLibres(FiltresListeDossiers filtres) {
        return !filtres.lignes().avecLibres() || filtres.commercial() != null || filtres.filtreLeTotal();
    }

    private Predicate predicats(CriteriaBuilder cb, Root<StkItem> article, Jointures j,
                                FiltresListeDossiers filtres, CriteriaQuery<?> query) {
        List<Predicate> p = new ArrayList<>();
        p.add(cb.isNull(article.get("comFolderId")));
        p.add(cb.not(cb.exists(dossierEnCours(article, query, cb))));
        p.add(cb.isNull(article.get("parent")));
        p.add(cb.or(cb.isNull(article.get("isUnderItem")), cb.isFalse(article.get("isUnderItem"))));
        p.add(VisibiliteEntites.depuisProjet(j.projet, cb));
        Optional<List<UUID>> perimetre = perimetreService.perimetreCourant();
        perimetre.ifPresent(ids -> p.add(cb.or(j.projet.get("id").in(ids), cb.isNull(j.projet.get("id")))));
        if (filtres.projet() != null) {
            p.add(cb.equal(j.projet.get("id"), filtres.projet()));
        }
        if (filtres.trancheId() != null && !filtres.trancheId().isEmpty()) {
            p.add(j.tranche.get("id").in(filtres.trancheId()));
        }
        if (filtres.societeId() != null) {
            p.add(cb.equal(j.societe.get("id"), filtres.societeId()));
        }
        if (StringUtils.hasText(filtres.search())) {
            String motif = "%" + filtres.search().replace(" ", "").toLowerCase(Locale.ROOT) + "%";
            p.add(cb.or(
                    cb.like(cb.lower(SpecificationsListeDossiers.compact(cb, article.get(CODE))), motif),
                    cb.like(cb.lower(SpecificationsListeDossiers.compact(cb, article.get("description"))), motif)));
        }
        return cb.and(p.toArray(new Predicate[0]));
    }

    /**
     * Article principal ({@code ComFolder.StkItemId}) d'un dossier non sorti : pris, même si {@code StkItem.ComFolderId}
     * est vide (cas réels en base). Statut absent = dossier en cours.
     */
    private static Subquery<UUID> dossierEnCours(Root<StkItem> article, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> sous = query.subquery(UUID.class);
        Root<ComFolder> dossier = sous.from(ComFolder.class);
        Join<ComFolder, StrStatus> statut = dossier.join("strStatus", JoinType.LEFT);
        sous.select(dossier.get("id")).where(
                cb.equal(dossier.get("stkItem"), article),
                cb.or(cb.isNull(statut.get("annulation")), cb.isFalse(statut.get("annulation"))),
                cb.or(cb.isNull(statut.get("resiliation")), cb.isFalse(statut.get("resiliation"))));
        return sous;
    }

    private static final class Jointures {
        private final Join<StkItem, CfgTranche> tranche;
        private final Join<CfgTranche, CfgProject> projet;
        private final Join<CfgProject, CfgCompany> societe;

        private Jointures(Root<StkItem> article) {
            tranche = article.join("stkHierarchy", JoinType.LEFT);
            projet = tranche.join("cfgProject", JoinType.LEFT);
            societe = projet.join("cfgCompany", JoinType.LEFT);
        }
    }
}
