package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seule règle du verrou d'un décompte (facturé, ou un autre décompte du lot a un numéro supérieur : {@code W:IsLocked})
 * et des droits OMRAN des actions (FEATURE-decomptes-parite.md §3-§8, Q11).
 */
@Component
public class VerrouDecompte {

    public static final String MODIFIER = "Yes.Prj.Front.Market.WorkingOutList.toolNew";
    public static final String SUPPRIMER = "Yes.Prj.Front.Market.WorkingOutList.toolDelete";
    public static final String RECALCULER = "Yes.Prj.Front.Market.MarketMonitoringList.RecalculateWorkingOut";
    public static final String PAR_MONTANT = "Yes.Prj.Front.Market.MarketMonitoringList.CanEditWorkingOutRetainByAmount";
    static final String FACTURE = "facturé";
    static final String PAS_LE_DERNIER = "pas le dernier du lot";

    private final DroitsOmran droits;
    private final EntityManager em;

    public VerrouDecompte(DroitsOmran droits, EntityManager em) {
        this.droits = droits;
        this.em = em;
    }

    /** Numéro le plus haut de chaque lot. */
    public Map<UUID, Integer> derniersNumeros(Collection<UUID> lots) {
        Map<UUID, Integer> m = new HashMap<>();
        if (!lots.isEmpty()) {
            em.createQuery("select o.prjMarket.id, max(o.sorting) from PrjWorkingOut o where o.prjMarket.id in :lots"
                            + " group by o.prjMarket.id", Object[].class)
                    .setParameter("lots", lots).getResultList()
                    .forEach(r -> m.put((UUID) r[0], (Integer) r[1]));
        }
        return m;
    }

    /** Motif du verrou, null si le décompte est modifiable. */
    public static String motif(PrjWorkingOut w, Integer dernierNumero) {
        if (w.getBuyDocument() != null) {
            return FACTURE;
        }
        boolean dernier = dernierNumero == null || (w.getSorting() != null && w.getSorting() >= dernierNumero);
        return dernier ? null : PAS_LE_DERNIER;
    }

    public String motif(PrjWorkingOut w) {
        UUID lot = w.getPrjMarket().getId();
        return motif(w, derniersNumeros(List.of(lot)).get(lot));
    }

    public Set<String> droitsCourants() {
        return droits.parmi(MODIFIER, SUPPRIMER, RECALCULER, PAR_MONTANT);
    }

    public void exiger(String... codes) {
        droits.exiger(codes);
    }
}
