package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculDecompte.Precedents;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lectures des écritures de décompte : enfants, cumuls des décomptes précédents du lot, restes OMRAN
 * ({@code omran-src/Yes.Rems/Yes.Buy/Utils.cs}). Sans {@code @Transactional} : joint la transaction de l'appelant.
 */
@Component
public class DonneesDecompte {

    static final String AVANCE_MARCHE = "MARKET_ADVANCE";
    static final String AVANCE_DECOMPTE = "WORKINGOUT_ADVANCE";
    private static final String LOT = "lot";
    private static final String EXCLU = "exclu";
    private static final String AVANT = "avant";

    private final EntityManager em;

    public DonneesDecompte(EntityManager em) {
        this.em = em;
    }

    /** Lignes d'une table fille ({@code PrjWorkingOutLine}, {@code …Extra}, {@code …StockLine}, {@code …RetainedLine}). */
    public <T> List<T> enfants(Class<T> table, UUID decompte) {
        return em.createQuery("select e from " + table.getSimpleName() + " e where e.prjWorkingOutId = :d order by e.sorting",
                table).setParameter("d", decompte).getResultList();
    }

    /** Tiers saisi ; inconnu → 400. */
    public CfgTier tiers(UUID id) {
        CfgTier t = em.find(CfgTier.class, id);
        if (t == null) {
            throw new InvalidRequestException("Tiers inconnu : " + id + ".");
        }
        return t;
    }

    public List<PrjMarketLine> bordereau(UUID lot) {
        return em.createQuery("select m from PrjMarketLine m where m.prjMarketId = :lot order by m.sorting", PrjMarketLine.class)
                .setParameter(LOT, lot).getResultList();
    }

    /**
     * Σ des décomptes du lot de numéro inférieur ({@code LastWorkingOut.Cumul…}). Retenues = RG + prorata + DEC + TRC
     * ({@code PreviousDeductionAmountAccumalted}) : les retenues forfaitaires n'y sont pas.
     */
    public Precedents precedents(PrjWorkingOut w) {
        if (w.getSorting() == null) {
            return Precedents.AUCUN;
        }
        Object[] s = em.createQuery("""
                        select sum(o.amountTTC), sum(o.advanceAmount), sum(o.warrantyAmount), sum(o.proportionAmount),
                               sum(o.insuranceDECAmount), sum(o.insuranceTRCAmount),
                               sum(coalesce(o.warrantyAmount, 0) + coalesce(o.proportionAmount, 0)
                                   + coalesce(o.insuranceDECAmount, 0) + coalesce(o.insuranceTRCAmount, 0))
                        from PrjWorkingOut o where o.prjMarket.id = :lot and o.sorting < :avant""", Object[].class)
                .setParameter(LOT, w.getPrjMarket().getId()).setParameter(AVANT, w.getSorting()).getSingleResult();
        return new Precedents((BigDecimal) s[0], (BigDecimal) s[1], (BigDecimal) s[2], (BigDecimal) s[3], (BigDecimal) s[4],
                (BigDecimal) s[5], (BigDecimal) s[6]);
    }

    /** Décompte du lot de numéro immédiatement inférieur, null s'il n'y en a pas. */
    public PrjWorkingOut precedent(UUID lot, Integer numero) {
        TypedQuery<PrjWorkingOut> q = em.createQuery("select o from PrjWorkingOut o where o.prjMarket.id = :lot"
                        + (numero == null ? "" : " and o.sorting < :avant") + " order by o.sorting desc", PrjWorkingOut.class)
                .setParameter(LOT, lot).setMaxResults(1);
        return (numero == null ? q : q.setParameter(AVANT, numero)).getResultStream().findFirst().orElse(null);
    }

    /** Σ AmountTTC des pièces d'achat du lot de ce type ; null s'il n'y en a aucune ({@code Utils.cs:GetAdvanceSumByMarket}). */
    public BigDecimal pieces(UUID lot, String type, UUID exclu) {
        return em.createQuery("select sum(b.amountTTC) from BuyDocument b where b.referenceType = :type"
                        + " and b.prjMarketId = :lot and b.id <> :exclu", BigDecimal.class)
                .setParameter("type", type).setParameter(LOT, lot).setParameter(EXCLU, exclu).getSingleResult();
    }

    /** Colonnes sommables ; le JPQL est constant (pas de concaténation, S2077). */
    public enum Colonne {
        PRORATA_REMBOURSE("""
                select coalesce(sum(o.prorataRefundAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu""", """
                select coalesce(sum(o.prorataRefundAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu and o.sorting < :avant"""),
        ASSURANCE_DEC("""
                select coalesce(sum(o.insuranceDECAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu""", """
                select coalesce(sum(o.insuranceDECAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu and o.sorting < :avant"""),
        ASSURANCE_TRC("""
                select coalesce(sum(o.insuranceTRCAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu""", """
                select coalesce(sum(o.insuranceTRCAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu and o.sorting < :avant"""),
        AVANCE_MARCHE("""
                select coalesce(sum(o.advanceAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu""", """
                select coalesce(sum(o.advanceAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu and o.sorting < :avant"""),
        AVANCE_DECOMPTE("""
                select coalesce(sum(o.workingOutAdvanceAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu""", """
                select coalesce(sum(o.workingOutAdvanceAmount), 0) from PrjWorkingOut o
                where o.prjMarket.id = :lot and o.id <> :exclu and o.sorting < :avant""");

        private final String jpql;
        private final String jpqlAvant;

        Colonne(String jpql, String jpqlAvant) {
            this.jpql = jpql;
            this.jpqlAvant = jpqlAvant;
        }
    }

    /** Σ d'une colonne des autres décomptes du lot, de numéro inférieur à {@code avant} s'il est donné. */
    public BigDecimal somme(Colonne colonne, UUID lot, UUID exclu, Integer avant) {
        TypedQuery<BigDecimal> q = em.createQuery(avant == null ? colonne.jpql : colonne.jpqlAvant, BigDecimal.class)
                .setParameter(LOT, lot).setParameter(EXCLU, exclu);
        return (avant == null ? q : q.setParameter(AVANT, avant)).getSingleResult();
    }

    /** Reste du remboursement prorata : 0 si le projet ne gère pas son prorata sur ce lot ({@code W:1100-1147}). */
    public BigDecimal resteProrata(PrjWorkingOut w) {
        UUID lot = w.getPrjMarket().getId();
        List<?> gestion = em.createNativeQuery("SELECT 1 FROM PrjProject WHERE PKey = :projet AND ManageProrataPrjMarketId = :lot")
                .setParameter("projet", w.getPrjMarket().getPrjProject().getId()).setParameter(LOT, lot).getResultList();
        if (gestion.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal retenu = em.createQuery("select coalesce(sum(o.proportionAmount), 0) from PrjWorkingOut o"
                        + " where o.prjMarket.prjProject.id = :projet", BigDecimal.class)
                .setParameter("projet", w.getPrjMarket().getPrjProject().getId()).getSingleResult();
        return retenu.subtract(somme(Colonne.PRORATA_REMBOURSE,lot, w.getId(), null));
    }

    /** Σ Quantity des décomptes du lot de numéro inférieur, par ligne de bordereau ({@code WL:AffectOldValues}). */
    public Map<UUID, BigDecimal> quantitesAnterieures(UUID lot, Integer numero) {
        Map<UUID, BigDecimal> m = new HashMap<>();
        if (numero == null) {
            return m;
        }
        em.createQuery("""
                        select l.prjMarketLineId, sum(l.quantity) from PrjWorkingOutLine l, PrjWorkingOut o
                        where o.id = l.prjWorkingOutId and o.prjMarket.id = :lot and o.sorting < :avant
                        group by l.prjMarketLineId""", Object[].class)
                .setParameter(LOT, lot).setParameter(AVANT, numero).getResultList()
                .forEach(r -> m.put((UUID) r[0], (BigDecimal) r[1]));
        return m;
    }

    /** Lignes de l'aperçu « recharger » ({@code W:ReloadLines}) : une par ligne du bordereau, non persistées. */
    public List<PrjWorkingOutLine> lignesRechargees(PrjWorkingOut w, List<PrjMarketLine> bordereau) {
        Map<UUID, BigDecimal> anciennes = quantitesAnterieures(w.getPrjMarket().getId(), w.getSorting());
        return bordereau.stream().map(m -> {
            PrjWorkingOutLine l = new PrjWorkingOutLine();
            l.setPrjWorkingOutId(w.getId());
            l.setPrjMarketLineId(m.getId());
            l.setSorting(m.getSorting());
            l.setCode(m.getCode());
            l.setDescription(m.getDescription());
            l.setUnity(m.getUnity());
            l.setNotes(m.getNotes());
            l.setFodecRatio(m.getFodecRatio());
            l.setVatRatio(m.getVatRatio());
            l.setUnitPriceHT(m.getUnitPriceHT());
            l.setUnitPriceTTC(m.getUnitPriceTTC());
            l.setBuyItemId(m.getBuyItemId());
            l.setStlCategoryId(m.getStlCategoryId());
            l.setQuantityOld(anciennes.getOrDefault(m.getId(), BigDecimal.ZERO));
            l.setQuantityOrigin(BigDecimal.ZERO);
            l.setQuantity(BigDecimal.ZERO);
            l.setCoefficient(BigDecimal.ONE);
            return l;
        }).toList();
    }
}
