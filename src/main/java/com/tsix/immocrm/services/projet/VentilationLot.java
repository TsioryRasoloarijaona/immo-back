package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketVentilationCategory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Seul calcul de {@code PrjMarketVentilationCategory} (FEATURE-lot-ecritures.md R12) : parts de Σ(qté × PU TTC) par catégorie,
 * sur le définitif si le lot a un fournisseur, sinon sur l'estimatif ; à défaut, tout sur la catégorie du lot.
 */
@Component
class VentilationLot {

    // Tri croissant : la dernière part, qui absorbe l'arrondi, est la plus grosse.
    private static final String MONTANTS = "select l.stlCategoryId, sum(l.quantity * l.unitPriceTTC) from %s l"
            + " where l.prjMarketId = :lot and (l.isStock is null or l.isStock = false) and l.stlCategoryId is not null"
            + " group by l.stlCategoryId order by sum(l.quantity * l.unitPriceTTC) asc";

    private final EntityManager em;

    VentilationLot(EntityManager em) {
        this.em = em;
    }

    void reecrire(PrjMarket lot, String auteur, LocalDateTime maintenant) {
        String lignes = lot.getCfgSupplier() != null ? "PrjMarketLine" : "PrjMarketLineEstimation";
        Map<UUID, BigDecimal> montants = new LinkedHashMap<>();
        em.createQuery(MONTANTS.formatted(lignes), Object[].class).setParameter("lot", lot.getId()).getResultList()
                .forEach(r -> montants.put((UUID) r[0], (BigDecimal) r[1]));
        Map<UUID, BigDecimal> parts = MontantsLot.parts(montants);
        if (parts.isEmpty() && lot.getStlCategory() != null) {
            parts = Map.of(lot.getStlCategory().getId(), BigDecimal.ONE);
        }
        em.createQuery("delete from PrjMarketVentilationCategory v where v.prjMarketId = :lot")
                .setParameter("lot", lot.getId()).executeUpdate();
        parts.forEach((categorie, taux) -> {
            PrjMarketVentilationCategory v = new PrjMarketVentilationCategory();
            v.setId(UUID.randomUUID());
            v.setPrjMarketId(lot.getId());
            v.setStlCategoryId(categorie);
            v.setVentilationRate(taux);
            v.setUserCreate(auteur);
            v.setUserCreateDateTime(maintenant);
            em.persist(v);
        });
    }
}
