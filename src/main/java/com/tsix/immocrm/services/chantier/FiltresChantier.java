package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Specifications des listes paginées ; le périmètre y est toujours appliqué, jamais par l'appelant. */
public final class FiltresChantier {

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String PRJ_PROJECT = "prjProject";
    private static final String PRJ_MARKET = "prjMarket";

    private FiltresChantier() {
    }

    public record FiltreLots(UUID projetId, UUID statutId, UUID typeId, UUID fournisseurId, String search) {
    }

    public record FiltreDecomptes(UUID projetId, UUID lotId, UUID fournisseurId, LocalDate du, LocalDate au,
                                  UUID statutFactureId, Boolean sansFacture, String search) {
    }

    static Specification<PrjMarket> lots(FiltreLots f) {
        return (root, query, cb) -> {
            Join<PrjMarket, CfgTier> fournisseur = root.join("cfgSupplier", JoinType.LEFT);
            List<Predicate> et = new ArrayList<>();
            et.add(cb.equal(root.get(PRJ_PROJECT).get(ID), f.projetId()));
            egal(et, cb, root.get("strStatus").get(ID), f.statutId());
            egal(et, cb, root.get("prjMarketType").get(ID), f.typeId());
            egal(et, cb, fournisseur.get(ID), f.fournisseurId());
            String motif = motif(f.search());
            if (motif != null) {
                et.add(cb.or(cb.like(root.get(CODE), motif), cb.like(root.get(DESCRIPTION), motif),
                        cb.like(fournisseur.get(DESCRIPTION), motif)));
            }
            return cb.and(et.toArray(Predicate[]::new));
        };
    }

    static Specification<PrjWorkingOut> decomptes(FiltreDecomptes f, PerimetreChantier perimetre) {
        return (root, query, cb) -> {
            Join<PrjWorkingOut, PrjMarket> lot = root.join(PRJ_MARKET, JoinType.INNER);
            Path<UUID> projetId = lot.get(PRJ_PROJECT).get(ID);
            Join<PrjWorkingOut, CfgTier> fournisseur = root.join("cfgTier", JoinType.LEFT);
            Join<PrjWorkingOut, BuyDocument> facture = root.join("buyDocument", JoinType.LEFT);
            List<Predicate> et = new ArrayList<>();
            if (!perimetre.toutVoir()) {
                et.add(projetId.in(perimetre.projetIds()));
            }
            egal(et, cb, projetId, f.projetId());
            egal(et, cb, lot.get(ID), f.lotId());
            egal(et, cb, fournisseur.get(ID), f.fournisseurId());
            egal(et, cb, facture.get("buyDocumentStatus").get(ID), f.statutFactureId());
            if (f.du() != null) {
                et.add(cb.greaterThanOrEqualTo(root.get("docDate"), f.du().atStartOfDay()));
            }
            if (f.au() != null) {
                et.add(cb.lessThan(root.get("docDate"), f.au().plusDays(1).atStartOfDay()));
            }
            if (Boolean.TRUE.equals(f.sansFacture())) {
                et.add(cb.isNull(facture.get(ID)));
            }
            String motif = motif(f.search());
            if (motif != null) {
                et.add(cb.or(cb.like(root.get(CODE), motif), cb.like(lot.get(CODE), motif),
                        cb.like(lot.get(DESCRIPTION), motif), cb.like(fournisseur.get(DESCRIPTION), motif)));
            }
            return cb.and(et.toArray(Predicate[]::new));
        };
    }

    private static void egal(List<Predicate> et, CriteriaBuilder cb, Expression<UUID> chemin, UUID valeur) {
        if (valeur != null) {
            et.add(cb.equal(chemin, valeur));
        }
    }

    private static String motif(String search) {
        return search == null || search.isBlank() ? null : "%" + search.trim() + "%";
    }
}
