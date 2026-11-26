package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.fixtures.FixtureAchat.SequenceNature;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePiecesLot.GESTION_FACTURE_DECOMPTE;

/**
 * Facture et avoir d'un décompte (FEATURE-lc8-facture-decompte.md) : cadre {@link FixturePiecesLot#cadre} complété des workflows
 * EN COURS → VALIDÉ DT des gestions {@code PrjInvoiceDebitWorkingOut} (B604) et {@code BuyRefundDebit} (B620) et de la séquence
 * « Avoir fournisseur » ; décomptes à {@code DocRef} propre, datés du {@code numero} mars 2026.
 */
@Component
public class FixtureFactureDecompte {

    public static final UUID GESTION_AVOIR = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B620");
    /** Séquence « Avoir fournisseur » à sa PKey de prod, posée à 900 : premier avoir = 21/00901. */
    public static final SequenceNature SEQUENCE_AVOIR = new SequenceNature("Avoir fournisseur",
            UUID.fromString("50C4F768-1478-5897-1245-F784D28F2001"), 900);

    /** Décompte complet : lignes 4 × 100 HT (TVA 20 %) + extra 1 × 50 HT → TTC de la facture 540. */
    public static final String NET_DECOMPTE_COMPLET = "531.5";
    /** Retenue forfaitaire ({@code PrjWorkingOutRetainedLine.Amount}) du décompte complet = {@code BuyDocument.RetainedAmount}. */
    public static final String RETENUE_FORFAITAIRE = "8";
    /** Montants du décompte complet recopiés sur la facture : colonne {@code PrjWorkingOut} → colonne {@code BuyDocument}. */
    public static final Map<String, String> RETENUES_RECOPIEES = Map.ofEntries(
            Map.entry("AdvanceAmount", "AdvanceAmount"),
            Map.entry("WorkingOutAdvanceAmount", "WorkingOutAdvanceAmount"),
            Map.entry("WarrantyAmount", "WarrantyAmount"),
            Map.entry("ProrataRefundAmount", "ProrataRefundAmount"),
            Map.entry("StockAmountToPay", "StockAmountToPay"),
            Map.entry("StockAmountToPayHT", "StockAmountToPayHT"),
            Map.entry("FluctuationsToPay", "FluctuationsToPay"),
            Map.entry("FluctuationsToPayHT", "FluctuationsToPayHT"),
            Map.entry("proportionAmount", "ProportionAmount"),
            Map.entry("insuranceDECAmount", "InsuranceDECAmount"),
            Map.entry("insuranceTRCAmount", "InsuranceTRCAmount"),
            Map.entry("AmountRegul", "AmountRegul"));

    private static final String TVA = "0.2";
    private static final String TABLE = "PrjWorkingOut";

    private final FixturePiecesLot pf;
    private final FixtureAchat achat;
    private final FixtureProjetLots projetLots;
    private final FixtureDecomptes decomptes;
    private final FixtureLotFiche lotFiche;

    public FixtureFactureDecompte(FixturePiecesLot pf, FixtureAchat achat, FixtureProjetLots projetLots,
                                  FixtureDecomptes decomptes, FixtureLotFiche lotFiche) {
        this.pf = pf;
        this.achat = achat;
        this.projetLots = projetLots;
        this.decomptes = decomptes;
        this.lotFiche = lotFiche;
    }

    public Cadre cadre(String prefixe) {
        Cadre c = pf.cadre(prefixe);
        achat.transitionDe(achat.gestionVide("PrjInvoiceDebitWorkingOut", GESTION_FACTURE_DECOMPTE), 10, c.enCours(),
                c.verrouille());
        achat.transitionDe(achat.gestionVide("BuyRefundDebit", GESTION_AVOIR), 10, c.enCours(), c.verrouille());
        achat.sequenceSaisie(SEQUENCE_AVOIR);
        return c;
    }

    public static String docRef(Cadre c, int numero) {
        return "REF" + numero + "-" + c.jeton();
    }

    public static LocalDate date(int numero) {
        return LocalDate.of(2026, 3, numero);
    }

    /** Décompte n° {@code numero} du lot, {@code DocRef} = {@link #docRef}, montants donnés (absents = NULL). */
    public UUID decompte(Cadre c, UUID lot, int numero, Map<String, BigDecimal> montants) {
        UUID d = projetLots.decompte(lot, "FDW" + numero + "-" + c.jeton(), numero, montants);
        decomptes.poser(TABLE, d, "DocRef", docRef(c, numero));
        return d;
    }

    /** Décompte TTC 1000, à payer 1000, sans retenue, une ligne de 5 × 100 HT sur le bordereau. */
    public UUID decompteSimple(Cadre c, UUID lot, int numero) {
        UUID d = decompte(c, lot, numero, Map.of(FixtureProjetLots.TTC, new BigDecimal("1000"),
                FixtureProjetLots.A_PAYER, new BigDecimal("1000")));
        UUID marche = decomptes.ligneMarche(lot, numero, "10", "100", TVA);
        decomptes.ligne(d, marche, 1, "5", "0", "100", TVA);
        return d;
    }

    /**
     * Décompte à chaque composante du net ≠ 0 : ajouts stock 30, prorata remboursé 20, fluctuations 10, régul 1,5 ; retenues
     * prorata 5, RG 25, avance 15, TRC 3, DEC 2, avance sur décompte 12, retenue forfaitaire 8 (une PrjWorkingOutRetainedLine) ;
     * nonRefundableRetainer = prorata + DEC + TRC + forfaitaire = 18 ; lignes TTC 480 + extra 60.
     * Net = 540 + 61,5 − (15 + 12 + 25 + 18) = {@value #NET_DECOMPTE_COMPLET}.
     */
    public UUID decompteComplet(Cadre c, UUID lot, int numero) {
        Map<String, BigDecimal> m = new HashMap<>();
        m.put(FixtureProjetLots.TTC, new BigDecimal("480"));
        m.put(FixtureProjetLots.AVANCE, new BigDecimal("15"));
        m.put(FixtureProjetLots.AVANCE_DECOMPTE, new BigDecimal("12"));
        m.put(FixtureProjetLots.RG, new BigDecimal("25"));
        m.put(FixtureProjetLots.RETENUE_NON_REMBOURSABLE, new BigDecimal("18"));
        m.put(FixtureProjetLots.EXTRA, new BigDecimal("60"));
        m.put(FixtureProjetLots.PRORATA, new BigDecimal("20"));
        m.put(FixtureProjetLots.STOCK, new BigDecimal("30"));
        m.put(FixtureProjetLots.FLUCTUATIONS, new BigDecimal("10"));
        m.put(FixtureProjetLots.A_PAYER, new BigDecimal(NET_DECOMPTE_COMPLET));
        UUID d = decompte(c, lot, numero, m);
        decomptes.poser(TABLE, d, "proportionAmount", new BigDecimal("5"), "insuranceDECAmount", new BigDecimal("2"),
                "insuranceTRCAmount", new BigDecimal("3"), "AmountRegul", new BigDecimal("1.5"),
                "StockAmountToPayHT", new BigDecimal("25"), "FluctuationsToPayHT", new BigDecimal("8"));
        UUID marche = decomptes.ligneMarche(lot, numero, "10", "100", TVA);
        decomptes.ligne(d, marche, 1, "4", "0", "100", TVA);
        decomptes.extra(d, "1", "50", TVA);
        lotFiche.retenueForfaitaire(d, RETENUE_FORFAITAIRE);
        return d;
    }

    /** Facture déjà posée sur le décompte ({@code BuyDocumentId}), hors de l'écrivain. */
    public UUID dejaFacture(Cadre c, UUID decompte) {
        return projetLots.facture(decompte, "FDF-" + decompte.toString().substring(0, 4) + c.jeton(), new BigDecimal("1000"));
    }

    public UUID ligneMarche(UUID lot, int ordre, String puHT) {
        return decomptes.ligneMarche(lot, ordre, "10", puHT, TVA);
    }

    public void ligne(UUID decompte, UUID ligneMarche, int ordre, String quantite, String puHT) {
        decomptes.ligne(decompte, ligneMarche, ordre, quantite, "0", puHT, TVA);
    }

    public void extra(UUID decompte, String quantite, String puHT) {
        decomptes.extra(decompte, quantite, puHT, TVA);
    }

    /** {@code PrjWorkingOut.BuyDocumentId} relu en base, en majuscules. */
    public String factureDuDecompte(UUID decompte) {
        return FixturePiecesLot.guid(decomptes.lire(decompte, "BuyDocumentId").get("BuyDocumentId"));
    }

    public Map<String, Object> decompteEnBase(UUID decompte) {
        return decomptes.lire(decompte, "*");
    }

    public void flush() {
        decomptes.flush();
    }
}
