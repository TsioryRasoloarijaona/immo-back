package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureLotFiche.jour;

/**
 * Lot L3a-bis (FEATURE-lot-previsions.md) : planning et estimation de base du lot, points saisis, lignes de
 * {@code PrjMarketPaymentPrevision}. SQL natif lié (dates en {@code Timestamp}) ; parents avant enfants (FK de la CI).
 */
@Component
public class FixtureLotPrevisions {

    public static final String DROIT_PREVISIONS = "Yes.Prj.Front.Tdb.ProjectPrevision.toolNew";
    public static final String LIGNE_PERIMEE = "Ligne périmée de test";

    // PKey de production des 7 genres de ligne (P12), créés si absents
    public static final UUID ENTITE_AVANCE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B601");
    public static final UUID ENTITE_RG = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B602");
    public static final UUID ENTITE_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B604");
    public static final UUID ENTITE_FACTURE_MULTI = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B612");
    public static final UUID ENTITE_AVENANT = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B622");
    public static final UUID ENTITE_BON_COMMANDE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42C000");
    public static final UUID ENTITE_PRORATA = UUID.fromString("F9091D83-6134-4B70-BE54-B208B25F073D");

    private static final Map<UUID, String> ENTITES = Map.of(
            ENTITE_AVANCE, "PrjDocumentDebitMarketAdvance",
            ENTITE_RG, "PrjDocumentDebitMarketWarranty",
            ENTITE_DECOMPTE, "PrjInvoiceDebitWorkingOut",
            ENTITE_FACTURE_MULTI, "PrjInvoiceDebitMultiSupplier",
            ENTITE_AVENANT, "PrjInvoiceDebitMarketClause",
            ENTITE_BON_COMMANDE, "PrjMarketMultiSupplierOrder",
            ENTITE_PRORATA, "PrjDocumentDebitMarketProrataRefund");

    private static final String INSERT_POINT = """
            INSERT INTO %s (PKey, PointTime, PointDate, PointAchivement, PointPayment, PointAmount, PrjMarketId)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureLotSousFenetres sousFenetres;

    public FixtureLotPrevisions(EntityManager em, JdbcTemplate jdbc, FixtureLotSousFenetres sousFenetres) {
        this.em = em;
        this.jdbc = jdbc;
        this.sousFenetres = sousFenetres;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static BigDecimal n(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    /** Les 7 tables des sous-fenêtres et les 7 {@code StrEntity} des genres de ligne, créés si absents. */
    public void preparer() {
        sousFenetres.tables();
        ENTITES.forEach((id, code) -> maj("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?)
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)""", s(id), s(id), code, code));
    }

    /**
     * Planning du lot : prévisions ({@code StartDate}, {@code Delay}, {@code EndDate} = début + délai) et estimation de
     * base ({@code …Manual}) ; délai de paiement 0.
     */
    public void planning(UUID lot, LocalDate debut, int duree, LocalDate debutBase, int dureeBase) {
        maj("""
                UPDATE PrjMarket SET StartDate = ?, Delay = ?, EndDate = ?, StartDateManual = ?, DelayManual = ?,
                    EndDateManual = ?, PaymentDelay = 0 WHERE PKey = ?""",
                jour(debut), duree, jour(debut.plusDays(duree)), jour(debutBase), dureeBase,
                jour(debutBase.plusDays(dureeBase)), s(lot));
    }

    /** Estimation de base HT / TVA / TTC et TVA par défaut (fraction 0–1, {@code null} possible). */
    public void estimationBase(UUID lot, String ht, String tva, String ttc, String tvaParDefaut) {
        maj("""
                UPDATE PrjMarket SET EstimatedPriceHTManual = ?, EstimatedVatAmountManual = ?, EstimatedPriceTTCManual = ?,
                    DefaultVat = ? WHERE PKey = ?""", n(ht), n(tva), n(ttc), n(tvaParDefaut), s(lot));
    }

    /** Taux d'avance, de RG (fractions 0–1), durée de garantie en mois ; prorata et assurances à 0, RG non bloquée. */
    public void retenues(UUID lot, String avance, String rg, String moisGarantie) {
        maj("""
                UPDATE PrjMarket SET AdvanceRatio = ?, WarrantyRatio = ?, WarrantyInMonths = ?, ProportionRatio = 0,
                    InsuranceDECRatio = 0, InsuranceTRCRatio = 0, BlokWarrantyRestitution = 0 WHERE PKey = ?""",
                n(avance), n(rg), n(moisGarantie), s(lot));
    }

    public void rgBloquee(UUID lot) {
        maj("UPDATE PrjMarket SET BlokWarrantyRestitution = 1 WHERE PKey = ?", s(lot));
    }

    public void signature(UUID lot, LocalDate date) {
        maj("UPDATE PrjMarket SET ContractSignatureDate = ? WHERE PKey = ?", jour(date), s(lot));
    }

    /** Colonne {@code PrjMarket.EstimatedPriceTTC} (prix des prévisions quand aucune offre). */
    public void prixEstime(UUID lot, String ttc) {
        maj("UPDATE PrjMarket SET EstimatedPriceTTC = ? WHERE PKey = ?", n(ttc), s(lot));
    }

    public void multiFournisseur(UUID lot, boolean multi) {
        maj("UPDATE PrjMarket SET IsMultiSupplier = ? WHERE PKey = ?", multi, s(lot));
    }

    public void lotProrata(UUID projet, UUID lot) {
        maj("UPDATE PrjProject SET ManageProrataPrjMarketId = ? WHERE PKey = ?", s(lot), s(projet));
    }

    /** Point « Prévisions » stocké ({@code PrjMarketPrevision}). */
    public UUID pointPrevision(UUID lot, LocalDate date, String avancement, String temps, String paiement, String montant) {
        return point("PrjMarketPrevision", lot, date, avancement, temps, paiement, montant);
    }

    /** Point « Prévisions est. de base » stocké ({@code PrjMarketUserPrevision}). */
    public UUID pointBase(UUID lot, LocalDate date, String avancement, String temps, String paiement, String montant) {
        return point("PrjMarketUserPrevision", lot, date, avancement, temps, paiement, montant);
    }

    // table fixe (jamais une saisie) : l'un des deux noms ci-dessus.
    private UUID point(String table, UUID lot, LocalDate date, String avancement, String temps, String paiement,
                       String montant) {
        UUID id = UUID.randomUUID();
        maj(INSERT_POINT.formatted(table), s(id), n(temps), jour(date), n(avancement), n(paiement), n(montant), s(lot));
        return id;
    }

    /** Ligne de {@code PrjMarketPaymentPrevision} d'un ancien calcul : le recalcul doit la faire disparaître. */
    public UUID lignePerimee(UUID lot) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarketPaymentPrevision (PKey, PaymentDate, Amount, Description, PrjMarketId, Achievement)
                VALUES (?, ?, 999, ?, ?, 0)""", s(id), jour(LocalDate.of(2020, 1, 1)), LIGNE_PERIMEE, s(lot));
        return id;
    }

    /** Lignes de paiement du lot, triées par date puis libellé ; dates en {@code LocalDate}. */
    public List<Map<String, Object>> lignesPaiement(UUID lot) {
        return lignes("""
                SELECT CAST(PKey AS varchar(36)) AS id, CONVERT(date, PaymentDate) AS date, Amount, AmountHT, AmountTTC,
                    Description, Achievement, CAST(StrEntityId AS varchar(36)) AS entite
                FROM PrjMarketPaymentPrevision WHERE PrjMarketId = ? ORDER BY PaymentDate, Description""", s(lot));
    }

    /** Points stockés d'une des deux tables, triés par date. */
    public List<Map<String, Object>> pointsPrevision(UUID lot) {
        return points("PrjMarketPrevision", lot);
    }

    public List<Map<String, Object>> pointsBase(UUID lot) {
        return points("PrjMarketUserPrevision", lot);
    }

    // table fixe (jamais une saisie).
    private List<Map<String, Object>> points(String table, UUID lot) {
        return lignes("""
                SELECT CAST(PKey AS varchar(36)) AS id, CONVERT(date, PointDate) AS date, PointAchivement, PointTime,
                    PointPayment, PointAmount FROM %s WHERE PrjMarketId = ? ORDER BY PointDate""".formatted(table), s(lot));
    }

    /** Colonnes de planning et d'estimation de base du lot, relues après vidage du contexte. */
    public Map<String, Object> lot(UUID lot) {
        return lignes("""
                SELECT CONVERT(date, StartDate) AS StartDate, Delay, CONVERT(date, EndDate) AS EndDate,
                    CONVERT(date, StartDateManual) AS StartDateManual, DelayManual,
                    CONVERT(date, EndDateManual) AS EndDateManual, EstimatedPriceHTManual, EstimatedVatAmountManual,
                    EstimatedPriceTTCManual, WarrantyRatio, AdvanceRatio, Description
                FROM PrjMarket WHERE PKey = ?""", s(lot)).getFirst();
    }

    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        em.clear();
        return jdbc.queryForList(sql, parametres);
    }
}
