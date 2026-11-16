package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.projet.EntreesLot;
import com.tsix.immocrm.domain.projet.EntreesSimulation;
import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Echeance;
import com.tsix.immocrm.domain.projet.EntreesSimulation.LigneFixe;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Point;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.SimulationPaiementsLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;
import static java.math.BigDecimal.ZERO;

/** Entrées de {@link SimulationPaiementsLot} pour un lot (FEATURE-lot-previsions.md §3.1), lues en base. */
@Component
class LectureSimulationLot {

    private static final String POINTS = """
            SELECT PointDate, PointAchivement, PointTime FROM %s
            WHERE PrjMarketId = :lot AND PointDate IS NOT NULL AND PointAchivement IS NOT NULL ORDER BY PointDate ASC""";
    static final String TABLE_POINTS = "PrjMarketPrevision";
    static final String TABLE_POINTS_BASE = "PrjMarketUserPrevision";

    private static final String ECHEANCES = """
            SELECT Ratio, EcheanceDate, Delay, Description FROM PrjMarketWarrantyLine
            WHERE PrjMarketId = :lot ORDER BY Sorting ASC, EcheanceDate ASC""";

    // P2 : décomptes facturés ; multi-fournisseur : factures (seule leur date sert, RealMaxDate).
    private static final String DECOMPTES = """
            SELECT PKey, DocDate, AmountTTC FROM PrjWorkingOut
            WHERE PrjMarketId = :lot AND BuyDocumentId IS NOT NULL AND DocDate IS NOT NULL ORDER BY DocDate ASC, Sorting ASC""";
    private static final String FACTURES = """
            SELECT PKey, DocDate, 0 FROM BuyDocument
            WHERE PrjMarketId = :lot AND DocType = 'INVOICE' AND DocDate IS NOT NULL ORDER BY DocDate ASC""";

    private static final String AVANCES = """
            SELECT AmountTTC FROM BuyDocument
            WHERE PrjMarketId = :lot AND DocType = 'DOCUMENT_TO_PAY' AND ReferenceType = 'MARKET_ADVANCE'""";

    private static final String AVENANTS = """
            SELECT c.DocDate, c.AmountToPay FROM PrjClause c
            WHERE c.PrjMarketId = :lot AND c.DocDate IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM BuyDocument f WHERE f.PrjClauseId = c.PKey AND f.DocType = 'INVOICE')""";

    // P10 : PrevisionAmount de BuyDocumentOrderView, recalculé sur les pièces liées (formules d'AgregatsBonCommande).
    private static final String BONS_COMMANDE = """
            SELECT o.DocDate, o.Description, x.Prevision, o.AmountHT - ISNULL(f.H, 0), o.AmountTTC - ISNULL(f.T, 0)
            FROM BuyDocument o
            JOIN BuyDocumentStatus s ON s.PKey = o.BuyDocumentStatusId AND s.IsOkForPayement = 1
            OUTER APPLY (SELECT SUM(d.AmountToPay) AS P FROM BuyDocument d
                         WHERE d.BuyOrderId = o.PKey AND d.ReferenceType = 'ORDER_ADVANCE') a
            OUTER APPLY (SELECT SUM(d.AmountToPay) AS P, SUM(d.AmountHT) AS H, SUM(d.AmountTTC) AS T FROM BuyDocument d
                         WHERE d.BuyOrderId = o.PKey AND d.DocType = 'INVOICE') f
            CROSS APPLY (SELECT ISNULL(o.AmountToPay, 0) - ISNULL(a.P, 0) - ISNULL(f.P, 0) AS Prevision) x
            WHERE o.DocType = 'ORDER' AND o.PrjMarketId = :lot AND o.DocDate IS NOT NULL AND x.Prevision > 0""";

    private final MontantsListeLots montants;
    private final AvancementDecomptes avancement;
    private final EntityManager em;
    private final Clock clock;

    LectureSimulationLot(MontantsListeLots montants, AvancementDecomptes avancement, EntityManager em, Clock clock) {
        this.montants = montants;
        this.avancement = avancement;
        this.em = em;
        this.clock = clock;
    }

    SimulationPaiementsLot.Estimation estimation(PrjMarket lot) {
        return SimulationPaiementsLot.estimation(entrees(lot), LocalDate.now(clock));
    }

    List<SimulationPaiementsLot.Ligne> paiements(PrjMarket lot) {
        return SimulationPaiementsLot.paiements(entrees(lot));
    }

    private EntreesSimulation entrees(PrjMarket lot) {
        UUID id = lot.getId();
        CalculLot calcul = Objects.requireNonNull(montants.calculer(List.of(lot)).get(id));
        EntreesLot.Drapeaux d = calcul.drapeaux();
        LocalDate debutBase = jour(lot.getStartDateManual());
        int dureeBase = entier(lot.getDelayManual());
        LocalDate finBase = lot.getEndDateManual() != null || debutBase == null ? jour(lot.getEndDateManual())
                : debutBase.plusDays(dureeBase);
        EntreesSimulation.Planning planning = new EntreesSimulation.Planning(jour(lot.getStartDate()), entier(lot.getDelay()),
                debutBase, dureeBase, finBase, jour(lot.getContractSignatureDate()), entier(lot.getPaymentDelay()),
                lot.getWarrantyInMonths());
        return new EntreesSimulation(new EntreesSimulation.Etat(d.multi(), d.annule(), d.annule() || d.termine(),
                d.gestionnaire(), d.blocage()), planning, calcul.montantsPrevisions(), calcul.retenuesPrevisions(),
                points(TABLE_POINTS, id), points(TABLE_POINTS_BASE, id),
                lire(ECHEANCES, id, r -> new Echeance(zero(montant(r[0])), date(r[1]), r[2] instanceof Number n ? n.intValue() : 0,
                        (String) r[3])),
                d.multi() ? reels(FACTURES, id, Map.of()) : reels(DECOMPTES, id, avancement.parDecompte(List.of(id))),
                lire(AVANCES, id, r -> zero(montant(r[0]))),
                lire(AVENANTS, id, r -> new LigneFixe(date(r[0]), null, zero(montant(r[1])), ZERO, ZERO)),
                d.multi() ? lire(BONS_COMMANDE, id, r -> new LigneFixe(date(r[0]), (String) r[1], zero(montant(r[2])),
                        zero(montant(r[3])), zero(montant(r[4])))) : List.of());
    }

    private List<Point> points(String table, UUID lot) {
        return lire(POINTS.formatted(table), lot, r -> new Point(date(r[0]), zero(montant(r[1])), zero(montant(r[2]))));
    }

    private List<DecompteReel> reels(String sql, UUID lot, Map<UUID, AvancementDecomptes.Avancement> parDecompte) {
        return lire(sql, lot, r -> {
            AvancementDecomptes.Avancement a = parDecompte.get(uuid(r[0]));
            return new DecompteReel(date(r[1]), a == null ? ZERO : zero(MontantsLot.fraction(a.avaPct())), zero(montant(r[2])));
        });
    }

    private <T> List<T> lire(String sql, UUID lot, Function<Object[], T> ligne) {
        List<?> brutes = em.createNativeQuery(sql).setParameter("lot", lot).getResultList();
        return brutes.stream().map(b -> ligne.apply(b instanceof Object[] r ? r : new Object[]{b})).toList();
    }

    private static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }

    private static int entier(Integer valeur) {
        return valeur == null ? 0 : valeur;
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? ZERO : valeur;
    }
}
