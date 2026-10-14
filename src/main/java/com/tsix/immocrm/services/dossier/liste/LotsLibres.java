package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Lignes « Libre » de la liste des dossiers : article racine, hors annexe, sans dossier
 * ({@code ComFolderId IS NULL}), dans le périmètre et une entité active. Un filtre qui ne peut viser qu'un
 * dossier (statut, commercial, date) les écarte toutes : chez OMRAN ces cellules sont vides.
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
        cq.select(cb.count(article)).where(predicats(cb, article, j, filtres));
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
        cq.select(article).where(predicats(cb, article, j, filtres)).orderBy(
                cb.asc(article.get(CODE)), cb.asc(j.tranche.get(CODE)), cb.asc(j.projet.get(CODE)),
                cb.asc(j.societe.get(CODE)), cb.asc(article.get("id")));
        return em.createQuery(cq).setFirstResult(Math.toIntExact(debut)).setMaxResults(nombre).getResultList();
    }

    private static boolean exclutLesLibres(FiltresListeDossiers filtres) {
        return !filtres.lignes().avecLibres()
                || filtres.commercial() != null
                || filtres.dateDebut() != null
                || filtres.dateFin() != null
                || Boolean.FALSE.equals(filtres.dateVide());
    }

    private Predicate predicats(CriteriaBuilder cb, Root<StkItem> article, Jointures j,
                                FiltresListeDossiers filtres) {
        List<Predicate> p = new ArrayList<>();
        p.add(cb.isNull(article.get("comFolderId")));
        p.add(cb.isNull(article.get("parent")));
        p.add(cb.or(cb.isNull(article.get("isUnderItem")), cb.isFalse(article.get("isUnderItem"))));
        p.add(VisibiliteEntites.depuisProjet(j.projet, cb));
        Optional<List<UUID>> perimetre = perimetreService.perimetreCourant();
        perimetre.ifPresent(ids -> p.add(cb.or(j.projet.get("id").in(ids), cb.isNull(j.projet.get("id")))));
        if (filtres.projet() != null) {
            p.add(cb.equal(j.projet.get("id"), filtres.projet()));
        }
        if (filtres.trancheIds() != null && !filtres.trancheIds().isEmpty()) {
            p.add(j.tranche.get("id").in(filtres.trancheIds()));
        }
        if (StringUtils.hasText(filtres.search())) {
            String motif = "%" + filtres.search().replace(" ", "").toLowerCase(Locale.ROOT) + "%";
            p.add(cb.or(
                    cb.like(cb.lower(SpecificationsListeDossiers.compact(cb, article.get(CODE))), motif),
                    cb.like(cb.lower(SpecificationsListeDossiers.compact(cb, article.get("description"))), motif)));
        }
        return cb.and(p.toArray(new Predicate[0]));
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
