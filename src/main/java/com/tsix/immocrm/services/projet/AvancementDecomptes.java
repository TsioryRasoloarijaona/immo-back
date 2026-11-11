package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculDecompte.Ligne;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * « Dépa. TTC » et « Ava. % » de la liste, comme {@code PrjWorkingOutView} / {@code PrjWorkingOutLineView} : dépassement
 * du bordereau par ligne (cumul antérieur = Σ des décomptes de numéro inférieur), avancement HT hors dépassement.
 * Voir FEATURE-decomptes-parite.md « Formules des colonnes de la liste ».
 */
@Component
public class AvancementDecomptes {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** Dépassement TTC de la période et avancement cumulé en pourcentage (0 sans bordereau). */
    public record Avancement(BigDecimal depassementTTC, BigDecimal avaPct) {
    }

    private record Marche(BigDecimal quantite, BigDecimal prixHT, BigDecimal fodec) {
        BigDecimal base() {
            return z(prixHT).multiply(z(quantite));
        }

        BigDecimal ht() {
            return new Ligne(quantite, prixHT, fodec, null).ht();
        }
    }

    private final EntityManager em;

    public AvancementDecomptes(EntityManager em) {
        this.em = em;
    }

    public Map<UUID, Avancement> parDecompte(Collection<UUID> lots) {
        Map<UUID, Avancement> resultat = new HashMap<>();
        if (lots.isEmpty()) {
            return resultat;
        }
        Map<UUID, Marche> marche = new HashMap<>();
        Map<UUID, BigDecimal> totalLot = new HashMap<>();
        em.createQuery("select m.prjMarketId, m.id, m.quantity, m.unitPriceHT, m.fodecRatio from PrjMarketLine m"
                        + " where m.prjMarketId in :lots", Object[].class).setParameter("lots", lots).getResultList()
                .forEach(r -> {
                    Marche m = new Marche((BigDecimal) r[2], (BigDecimal) r[3], (BigDecimal) r[4]);
                    marche.put((UUID) r[1], m);
                    totalLot.merge((UUID) r[0], m.ht(), BigDecimal::add);
                });
        List<Object[]> lignes = em.createQuery("""
                        select o.id, o.prjMarket.id, o.amountHT, l.prjMarketLineId, l.quantity, l.unitPriceHT, l.fodecRatio,
                               l.vatRatio
                        from PrjWorkingOut o left join PrjWorkingOutLine l on l.prjWorkingOutId = o.id
                        where o.prjMarket.id in :lots order by o.prjMarket.id, o.sorting, o.docDate, o.id""", Object[].class)
                .setParameter("lots", lots).getResultList();
        Cumul cumul = new Cumul(marche, totalLot);
        for (int i = 0; i < lignes.size(); i++) {
            Object[] r = lignes.get(i);
            cumul.ajouter(r);
            boolean finDuDecompte = i + 1 == lignes.size() || !lignes.get(i + 1)[0].equals(r[0]);
            if (finDuDecompte) {
                resultat.put((UUID) r[0], cumul.clore((UUID) r[1], (BigDecimal) r[2]));
            }
        }
        return resultat;
    }

    /** Parcours des décomptes d'un lot par numéro croissant ; les quantités d'un décompte deviennent l'antérieur du suivant. */
    private static final class Cumul {
        private final Map<UUID, Marche> marche;
        private final Map<UUID, BigDecimal> totalLot;
        private final Map<UUID, BigDecimal> anterieur = new HashMap<>();
        private final Map<UUID, BigDecimal> periode = new HashMap<>();
        private final Map<UUID, BigDecimal> avanceLot = new HashMap<>();
        private BigDecimal depassementHT = BigDecimal.ZERO;
        private BigDecimal depassementTTC = BigDecimal.ZERO;

        Cumul(Map<UUID, Marche> marche, Map<UUID, BigDecimal> totalLot) {
            this.marche = marche;
            this.totalLot = totalLot;
        }

        void ajouter(Object[] r) {
            UUID ligneMarche = (UUID) r[3];
            if (ligneMarche == null) {
                return;
            }
            BigDecimal quantite = z((BigDecimal) r[4]);
            periode.merge(ligneMarche, quantite, BigDecimal::add);
            Marche m = marche.get(ligneMarche);
            if (m == null) {
                return;
            }
            BigDecimal prix = (BigDecimal) r[5];
            BigDecimal fodec = (BigDecimal) r[6];
            BigDecimal ancienne = anterieur.getOrDefault(ligneMarche, BigDecimal.ZERO);
            BigDecimal base = z(prix).multiply(quantite);
            BigDecimal baseAncienne = z(prix).multiply(ancienne);
            BigDecimal ht = new Ligne(quantite, prix, fodec, null).ht();
            BigDecimal htAncien = new Ligne(ancienne, prix, fodec, null).ht();
            BigDecimal cumule = base.add(baseAncienne).compareTo(m.base()) > 0 ? ht.add(htAncien).subtract(m.ht())
                    : BigDecimal.ZERO;
            BigDecimal cumuleAncien = baseAncienne.compareTo(m.base()) > 0 ? htAncien.subtract(m.ht()) : BigDecimal.ZERO;
            BigDecimal depasse = cumule.subtract(cumuleAncien);
            depassementHT = depassementHT.add(depasse);
            depassementTTC = depassementTTC.add(depasse.multiply(BigDecimal.ONE.add(z((BigDecimal) r[7]))));
        }

        Avancement clore(UUID lot, BigDecimal montantHT) {
            BigDecimal avance = avanceLot.merge(lot, z(montantHT).subtract(depassementHT), BigDecimal::add);
            BigDecimal total = totalLot.getOrDefault(lot, BigDecimal.ZERO);
            BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO : avance.multiply(CENT).divide(total, 2, RoundingMode.HALF_UP);
            Avancement a = new Avancement(depassementTTC.setScale(2, RoundingMode.HALF_UP), pct);
            periode.forEach((k, v) -> anterieur.merge(k, v, BigDecimal::add));
            periode.clear();
            depassementHT = BigDecimal.ZERO;
            depassementTTC = BigDecimal.ZERO;
            return a;
        }
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
