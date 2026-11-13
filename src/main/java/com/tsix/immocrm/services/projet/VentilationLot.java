package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketVentilationCategory;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
    private static final String SELECT = "select l.stlCategoryId, sum(l.quantity * l.unitPriceTTC) from ";
    // Comme OMRAN : la catégorie nulle est une catégorie, et le repli sur celle du lot s'écrit même si elle est nulle.
    private static final String MONTANTS = " l where l.prjMarketId = :lot and (l.isStock is null or l.isStock = false)"
            + " group by l.stlCategoryId order by sum(l.quantity * l.unitPriceTTC) asc";
    private static final String DEFINITIF = SELECT + "PrjMarketLine" + MONTANTS;
    private static final String ESTIMATIF = SELECT + "PrjMarketLineEstimation" + MONTANTS;

    private final EntityManager em;

    VentilationLot(EntityManager em) {
        this.em = em;
    }

    void reecrire(PrjMarket lot, String auteur, LocalDateTime maintenant) {
        Map<UUID, BigDecimal> montants = new LinkedHashMap<>();
        em.createQuery(lot.getCfgSupplier() != null ? DEFINITIF : ESTIMATIF, Object[].class).setParameter("lot", lot.getId()).getResultList()
                .forEach(r -> montants.put((UUID) r[0], (BigDecimal) r[1]));
        Map<UUID, BigDecimal> parts = MontantsLot.parts(montants);
        if (parts.isEmpty()) {
            parts = Collections.singletonMap(Champs.de(lot.getStlCategory(), StlCategory::getId), BigDecimal.ONE);
        }
        // em.remove et non un delete JPQL : seul l'évènement de suppression fait effacer la ligne de la copie de lecture.
        em.createQuery("from PrjMarketVentilationCategory v where v.prjMarketId = :lot", PrjMarketVentilationCategory.class)
                .setParameter("lot", lot.getId()).getResultList().forEach(em::remove);
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
