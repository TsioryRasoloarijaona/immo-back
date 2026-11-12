package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.projet.EntreesLot;
import com.tsix.immocrm.domain.projet.EntreesLot.Somme;
import com.tsix.immocrm.domain.projet.MontantsLot.Trio;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lit en base les entrées de {@link CalculLot} d'un lot de lots (2 000 lots au plus par requête) ; formules des sous-vues de
 * {@code PrjMarketMonitoringView}, voir FEATURE-lots-liste-parite.md R1 et FEATURE-lot-fiche-suivi.md. Liste et fiche.
 */
@Component
class MontantsListeLots {

    private static final int LOT_MAX = 2000;
    private static final String LIGNE_HT = "UnitPriceHT * (1 + FODECRatio) * Quantity";
    private static final String LIGNE_TTC = LIGNE_HT + " * (1 + VatRatio)";
    private static final String SOMMES_LIGNES = "SELECT PrjMarketId, SUM(" + LIGNE_HT + "), SUM(" + LIGNE_TTC + ") FROM ";
    private static final String DES_LOTS = " WHERE PrjMarketId IN (:lots) GROUP BY PrjMarketId";
    private static final Somme[] SOMMES = Somme.values();

    // StrStatusType des lots terminés (IsTerminated) ; le deuxième est aussi la résiliation (CancelRatio).
    private static final UUID TYPE_RESILIE = UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97");
    private static final Set<UUID> TYPES_TERMINES = Set.of(UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0"),
            TYPE_RESILIE, UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA"));

    /** Une requête rend l'id du lot puis ses sommes, dans l'ordre de {@link Somme} à partir de {@code premiere}. */
    private record Requete(String sql, Somme premiere) {
    }

    private static final Requete BORDEREAU = new Requete(SOMMES_LIGNES + "PrjMarketLine" + DES_LOTS, Somme.LIGNES_HT);
    private static final Requete ESTIMATION = new Requete(SOMMES_LIGNES + "PrjMarketLineEstimation" + DES_LOTS,
            Somme.ESTIM_HT);

    // PrjWorkingOutLineView : dépassement d'une ligne = cumul au-delà du bordereau, moins celui des décomptes de rang inférieur.
    private static final Requete DEPASSEMENTS = new Requete("""
            WITH L AS (
                SELECT w.PrjMarketId AS Lot, w.BuyDocumentId AS Facture, l.PrjMarketLineId AS Ligne, w.Sorting AS Rang,
                    l.UnitPriceHT * l.Quantity AS B, l.UnitPriceHT * (1 + l.FODECRatio) * l.Quantity AS H,
                    l.UnitPriceHT * (1 + l.FODECRatio) * l.Quantity * (1 + l.VatRatio) AS T,
                    m.UnitPriceHT * m.Quantity AS BM, m.UnitPriceHT * (1 + m.FODECRatio) * m.Quantity AS HM,
                    m.UnitPriceHT * (1 + m.FODECRatio) * m.Quantity * (1 + m.VatRatio) AS TM
                FROM PrjWorkingOutLine l
                JOIN PrjWorkingOut w ON w.PKey = l.PrjWorkingOutId
                JOIN PrjMarketLine m ON m.PKey = l.PrjMarketLineId
                WHERE w.PrjMarketId IN (:lots)),
            C AS (
                SELECT L.*,
                    SUM(B) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(B) OVER (PARTITION BY Ligne, Rang) AS BO,
                    SUM(H) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(H) OVER (PARTITION BY Ligne, Rang) AS HO,
                    SUM(T) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(T) OVER (PARTITION BY Ligne, Rang) AS TO_
                FROM L),
            D AS (
                SELECT Lot, Facture,
                    IIF(BO + B > BM, H + HO - HM, 0) - IIF(BO > BM, HO - HM, 0) AS DH,
                    IIF(BO + B > BM, T + TO_ - TM, 0) - IIF(BO > BM, TO_ - TM, 0) AS DT
                FROM C)
            SELECT Lot, SUM(DH), SUM(DT), SUM(IIF(Facture IS NULL, 0, DH)), SUM(IIF(Facture IS NULL, 0, DT))
            FROM D GROUP BY Lot""", Somme.DEP_HT);

    private static final Requete SUPPLEMENTAIRES = new Requete("""
            SELECT w.PrjMarketId, SUM(e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity),
                SUM(e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity * (1 + e.VatRatio)),
                SUM(IIF(w.BuyDocumentId IS NULL, 0, e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity)),
                SUM(IIF(w.BuyDocumentId IS NULL, 0, e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity * (1 + e.VatRatio)))
            FROM PrjWorkingOutLineExtra e JOIN PrjWorkingOut w ON w.PKey = e.PrjWorkingOutId
            WHERE w.PrjMarketId IN (:lots) GROUP BY w.PrjMarketId""", Somme.SUPP_HT);

    // Lot multi-fournisseur : dépassement d'une commande validée (comme l'offert) = Σ de ses factures au-delà de son montant.
    private static final Requete DEPASSEMENTS_COMMANDES = new Requete("""
            SELECT o.PrjMarketId, SUM(IIF(f.HT > o.AmountHT, f.HT - o.AmountHT, 0)),
                SUM(IIF(f.HT - o.AmountHT <= 0, 0, f.TTC - o.AmountTTC))
            FROM BuyDocument o
            JOIN BuyDocumentStatus s ON s.PKey = o.BuyDocumentStatusId AND s.IsOkForPayement = 1
            JOIN (SELECT BuyOrderId, SUM(AmountHT) AS HT, SUM(AmountTTC) AS TTC FROM BuyDocument
                  WHERE DocType = 'INVOICE' AND BuyOrderId IS NOT NULL GROUP BY BuyOrderId) f ON f.BuyOrderId = o.PKey
            WHERE o.DocType = 'ORDER' AND o.PrjMarketId IN (:lots) GROUP BY o.PrjMarketId""", Somme.CMD_DEP_HT);

    // Appro et fluctuations « facturés » = ceux des décomptes rattachés à une facture (BuyDocumentId).
    private static final Requete DECOMPTES = new Requete("""
            SELECT w.PrjMarketId, SUM(w.AmountHT), SUM(w.AmountTTC), SUM(w.StockAmountToPayHT), SUM(w.StockAmountToPay),
                SUM(w.FluctuationsToPayHT), SUM(w.FluctuationsToPay), SUM(w.AdvanceAmount), SUM(w.WorkingOutAdvanceAmount),
                SUM(w.WarrantyAmount), SUM(w.ProportionAmount), SUM(w.InsuranceDECAmount), SUM(w.InsuranceTRCAmount),
                SUM(w.ProrataRefundAmount), SUM(IIF(w.BuyDocumentId IS NULL, 0, w.StockAmountToPayHT)),
                SUM(IIF(w.BuyDocumentId IS NULL, 0, w.StockAmountToPay)), SUM(IIF(w.BuyDocumentId IS NULL, 0, w.FluctuationsToPayHT)),
                SUM(IIF(w.BuyDocumentId IS NULL, 0, w.FluctuationsToPay)),
                (SELECT SUM(r.Amount) FROM PrjWorkingOutRetainedLine r JOIN PrjWorkingOut w2 ON w2.PKey = r.PrjWorkingOutId
                 WHERE w2.PrjMarketId = w.PrjMarketId)
            FROM PrjWorkingOut w WHERE w.PrjMarketId IN (:lots) GROUP BY w.PrjMarketId""", Somme.DEC_HT);

    private static final Requete AVENANTS = new Requete("""
            SELECT PrjMarketId, SUM(AmountHT), SUM(AmountTTC), SUM(WarrantyAmount), SUM(ProportionAmount),
                SUM(InsuranceDECAmount), SUM(InsuranceTRCAmount)
            FROM PrjClause WHERE PrjMarketId IN (:lots) GROUP BY PrjMarketId""", Somme.AV_HT);

    // F = facture de décompte, FA = facture d'avenant (genre B622), A = avoir, C = commande validée, G = pièce de RG ;
    // un genre NULL est exclu comme dans la vue (NOT IN / <>). Régularisations : hors pièces de RG.
    private static final Requete PIECES = new Requete("""
            WITH P AS (
                SELECT d.*, IIF(d.DocType = 'INVOICE' AND d.StrEntityId <> 'E7999F70-7507-40D9-AA78-A2825C42B622', 1, 0) AS F,
                    IIF(d.DocType = 'INVOICE' AND d.StrEntityId = 'E7999F70-7507-40D9-AA78-A2825C42B622', 1, 0) AS FA,
                    IIF(d.DocType = 'REFUND', 1, 0) AS A, IIF(d.DocType = 'ORDER' AND s.IsOkForPayement = 1, 1, 0) AS C,
                    IIF(d.StrEntityId IN ('A71B4F91-AEFC-4C70-87F7-82A6C7365CB5', 'E7999F70-7507-40D9-AA78-A2825C42B602'), 1, 0) AS G,
                    IIF(d.DocType = 'REFUND', -1, 1) AS Signe
                FROM BuyDocument d LEFT JOIN BuyDocumentStatus s ON s.PKey = d.BuyDocumentStatusId
                WHERE d.PrjMarketId IN (:lots))
            SELECT PrjMarketId,
                SUM(IIF(CanBePayed = 1 AND G = 0 AND StrEntityId IS NOT NULL, (ISNULL(AmountRegul, 0) + ISNULL(FiscalStamp, 0)) * Signe, 0)),
                SUM(F * AmountHT), SUM(F * AmountTTC), SUM(F * AdvanceAmount), SUM(F * WorkingOutAdvanceAmount), SUM(F * WarrantyAmount),
                SUM(F * ProportionAmount), SUM(F * InsuranceDECAmount), SUM(F * InsuranceTRCAmount), SUM(F * RetainedAmount),
                SUM(F * ProrataRefundAmount), SUM(F * AmountRegul), SUM(F * FiscalStamp),
                SUM(FA * AmountHT), SUM(FA * AmountTTC), SUM(FA * WarrantyAmount), SUM(FA * ProportionAmount),
                SUM(FA * InsuranceDECAmount), SUM(FA * InsuranceTRCAmount), SUM(FA * AmountRegul), SUM(FA * FiscalStamp),
                SUM(A * AmountHT), SUM(A * AmountTTC), SUM(A * ProportionAmount), SUM(A * InsuranceDECAmount),
                SUM(A * InsuranceTRCAmount), SUM(A * RetainedAmount), SUM(A * StockAmountToPay), SUM(A * ProrataRefundAmount),
                SUM(A * FluctuationsToPay), SUM(A * AmountRegul), SUM(A * FiscalStamp),
                SUM(C * AmountHT), SUM(C * AmountTTC), SUM(C * ISNULL(OrderAdvanceRatio, 0) * AmountTTC),
                SUM(IIF(BuyOrderId IS NULL AND DocType IN ('INVOICE', 'REFUND'), AmountHT * Signe, 0)),
                SUM(IIF(BuyOrderId IS NULL AND DocType IN ('INVOICE', 'REFUND'), AmountTTC * Signe, 0)),
                SUM(IIF(BuyOrderId IS NOT NULL AND DocType IN ('INVOICE', 'REFUND'), AmountHT * Signe, 0)),
                SUM(IIF(ReferenceType IN ('MARKET_ADVANCE', 'ORDER_ADVANCE'), AmountTTC, 0)),
                SUM(IIF(ReferenceType = 'WORKINGOUT_ADVANCE', AmountTTC, 0)),
                SUM(G * AmountTTC), -SUM(G * AmountRegul),
                SUM(IIF(StrEntityId = 'F9091D83-6134-4B70-BE54-B208B25F073D', AmountToPay, 0))
            FROM P GROUP BY PrjMarketId""", Somme.REGUL);

    // Réglé = toutes les lignes de règlement des pièces du lot, annulées comprises.
    private static final Requete REGLEMENTS = new Requete("""
            SELECT d.PrjMarketId, SUM(l.Amount), SUM(IIF(d.DocType = 'REFUND', l.Amount, 0)),
                SUM(IIF(d.ReferenceType IN ('MARKET_ADVANCE', 'ORDER_ADVANCE'), l.Amount, 0)),
                SUM(IIF(d.ReferenceType = 'WORKINGOUT_ADVANCE', l.Amount, 0)),
                SUM(IIF(d.StrEntityId IN ('A71B4F91-AEFC-4C70-87F7-82A6C7365CB5', 'E7999F70-7507-40D9-AA78-A2825C42B602'), l.Amount, 0))
            FROM StlSettlementLine l JOIN BuyDocument d ON d.PKey = l.BuyDocumentId
            WHERE d.PrjMarketId IN (:lots) GROUP BY d.PrjMarketId""", Somme.REGLE);

    // Marché = dernier devis retenu.
    private static final Requete DEVIS = new Requete("""
            SELECT PrjMarketId, AmountHT, AmountTTC FROM (
                SELECT PrjMarketId, AmountHT, AmountTTC,
                    ROW_NUMBER() OVER (PARTITION BY PrjMarketId ORDER BY DocDate DESC, PKey ASC) AS Rang
                FROM BuyDocument WHERE DocType = 'QUOTE' AND IsSelected = 1 AND PrjMarketId IN (:lots)) q
            WHERE Rang = 1""", Somme.DEVIS_HT);

    // Lot gestionnaire du compte prorata : prorata prévu de tous les lots du projet (bordereau, sinon estimation, sinon
    // forfait) et prorata facturé du projet. Les autres lots n'ont pas de ligne (0).
    private static final Requete PRORATA_PROJET = new Requete("""
            SELECT g.PKey, SUM(ISNULL(x.ProportionRatio, 0) * COALESCE(l.T, e.T, x.EstimatedPriceTTCManual)), MAX(f.P)
            FROM PrjMarket g
            JOIN PrjProject p ON p.PKey = g.PrjProjectId AND p.ManageProrataPrjMarketId = g.PKey
            JOIN PrjMarket x ON x.PrjProjectId = g.PrjProjectId
            OUTER APPLY (SELECT SUM(UnitPriceHT * (1 + FODECRatio) * Quantity * (1 + VatRatio)) AS T
                         FROM PrjMarketLine WHERE PrjMarketId = x.PKey) l
            OUTER APPLY (SELECT SUM(UnitPriceHT * (1 + FODECRatio) * Quantity * (1 + VatRatio)) AS T
                         FROM PrjMarketLineEstimation WHERE PrjMarketId = x.PKey) e
            OUTER APPLY (SELECT SUM(b.ProportionAmount) AS P FROM BuyDocument b JOIN PrjMarket y ON y.PKey = b.PrjMarketId
                         WHERE b.DocType = 'INVOICE' AND y.PrjProjectId = g.PrjProjectId) f
            WHERE g.PKey IN (:lots) GROUP BY g.PKey""", Somme.PREV_REMB_PRORATA);

    private static final List<Requete> REQUETES = List.of(BORDEREAU, ESTIMATION, DEPASSEMENTS, SUPPLEMENTAIRES,
            DEPASSEMENTS_COMMANDES, DECOMPTES, AVENANTS, PIECES, REGLEMENTS, DEVIS, PRORATA_PROJET);

    private final EntityManager em;

    MontantsListeLots(EntityManager em) {
        this.em = em;
    }

    Map<UUID, CalculLot> calculer(List<PrjMarket> marches) {
        Map<UUID, CalculLot> parLot = new HashMap<>();
        for (int debut = 0; debut < marches.size(); debut += LOT_MAX) {
            List<PrjMarket> tranche = marches.subList(debut, Math.min(marches.size(), debut + LOT_MAX));
            parLot.putAll(calculerTranche(tranche));
        }
        return parLot;
    }

    private Map<UUID, CalculLot> calculerTranche(List<PrjMarket> marches) {
        List<UUID> ids = marches.stream().map(PrjMarket::getId).toList();
        Map<UUID, Map<Somme, BigDecimal>> sommes = new HashMap<>();
        REQUETES.forEach(q -> lire(q, ids, sommes));
        return marches.stream().collect(Collectors.toMap(PrjMarket::getId,
                m -> new CalculLot(new EntreesLot(drapeaux(m), taux(m), sommes.getOrDefault(m.getId(), Map.of())))));
    }

    /** {@code EstimatedPrice*} d'un seul lot, tel que la liste le calcule (non arrondi ; TVA = TTC − HT). */
    Trio estimation(PrjMarket m) {
        Map<UUID, Map<Somme, BigDecimal>> sommes = new HashMap<>();
        lire(ESTIMATION, List.of(m.getId()), sommes);
        Map<Somme, BigDecimal> lignes = sommes.getOrDefault(m.getId(), Map.of());
        BigDecimal ht = z(lignes.get(Somme.ESTIM_HT));
        if (ht.signum() != 0) {
            BigDecimal ttc = z(lignes.get(Somme.ESTIM_TTC));
            return new Trio(ht, ttc.subtract(ht), ttc, true);
        }
        BigDecimal forfaitHT = z(m.getEstimatedPriceHTManual());
        BigDecimal forfaitTTC = z(m.getEstimatedPriceTTCManual());
        return new Trio(forfaitHT, forfaitTTC.subtract(forfaitHT), forfaitTTC, false);
    }

    private static EntreesLot.Drapeaux drapeaux(PrjMarket m) {
        boolean multi = Boolean.TRUE.equals(m.getIsMultiSupplier());
        StrStatus statut = m.getStrStatus();
        UUID type = statut == null ? null : statut.getStrStatusTypeId();
        boolean annule = statut != null && (Boolean.TRUE.equals(statut.getAnnulation()) || TYPE_RESILIE.equals(type));
        boolean termine = multi ? Boolean.TRUE.equals(m.getIsClosed()) : annule || (type != null && TYPES_TERMINES.contains(type));
        return new EntreesLot.Drapeaux(multi, termine, annule,
                m.getId().equals(m.getPrjProject().getManageProrataPrjMarketId()),
                Boolean.TRUE.equals(m.getBlokWarrantyRestitution()),
                Boolean.TRUE.equals(m.getApplyWithholdingTaxOnAdditionalWork()));
    }

    private static EntreesLot.Taux taux(PrjMarket m) {
        return new EntreesLot.Taux(z(m.getProportionRatio()), z(m.getInsuranceDECRatio()), z(m.getInsuranceDECAmount()),
                z(m.getInsuranceTRCRatio()), z(m.getInsuranceTRCAmount()), z(m.getAdvanceRatio()), z(m.getWarrantyRatio()),
                z(m.getEstimatedPriceHTManual()), z(m.getEstimatedVatAmountManual()), z(m.getEstimatedPriceTTCManual()));
    }

    private void lire(Requete q, List<UUID> ids, Map<UUID, Map<Somme, BigDecimal>> parLot) {
        List<?> lignes = em.createNativeQuery(q.sql()).setParameter("lots", ids).getResultList();
        for (Object ligne : lignes) {
            Object[] r = (Object[]) ligne;
            Map<Somme, BigDecimal> sommes = parLot.computeIfAbsent(UUID.fromString(String.valueOf(r[0])),
                    k -> new EnumMap<>(Somme.class));
            for (int i = 1; i < r.length; i++) {
                if (r[i] != null) {
                    sommes.put(SOMMES[q.premiere().ordinal() + i - 1], new BigDecimal(r[i].toString()));
                }
            }
        }
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
