package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * N5a Trésorerie projet (FEATURE-n5-tableaux-de-bord-projet.md, « Réponses aux questions du test-writer »). Aujourd'hui =
 * {@link #AUJOURDHUI} (horloge injectée par le test). Année testée 2077, vide ailleurs. Détail du jeu : {@link Jeu}.
 */
@Component
public class FixtureTresorerieProjet {

    public static final String ROLE = "Commercial";
    public static final String DROIT = "Yes.Prj.Front.Tdb.ExpensesByYear";
    public static final int ANNEE = 2077;
    public static final LocalDate AUJOURDHUI = LocalDate.of(2077, 6, 15);

    private static final String P_ID = "id";

    private final EntityManager em;
    private final FixtureAchat achat;
    private final FixtureVisibiliteOmran visibilite;
    private final FixtureProjetConfig config;

    public FixtureTresorerieProjet(EntityManager em, FixtureAchat achat, FixtureVisibiliteOmran visibilite,
                                   FixtureProjetConfig config) {
        this.em = em;
        this.achat = achat;
        this.visibilite = visibilite;
        this.config = config;
    }

    /**
     * <pre>
     * société S ; projets A (habilité pour « a ») et B (habilité pour « ab » seulement), même société.
     * lot A1 fournisseur F « Fournisseur J » ; lot A2 sans fournisseur (libellé J-LA2) ; lot A3 sans fournisseur (J-LA3) ;
     * lot B1 fournisseur FB « FournB J ».
     * Prévisions de lot (PrjMarketPaymentPrevision) :
     *   P0 A1 2076-11-10  40 → reportée au 2077-06-15     P1 A1 2077-03-10 100 → reportée
     *   P6 A1 2077-06-25  50 → reportée (≤ fin du mois)    P2 A1 2077-09-20 200
     *   P3 A2 2077-08-05 300                                P4 A3 2077-10-01   0 (R4 : absente)
     *   P7 A1 2078-01-10 500 (autre année)                  P5 B1 2077-09-20 1000 (projet B)
     * Achats (BuyDocument, CanBePayed = 1 sauf mention, module PRJ) :
     *   FA1 INVOICE lot A1, F, à payer 1000, échéance 2077-11-10 ; règlement IN_WORK 400 + CANCELED 250 → reste 600
     *   FA2 INVOICE lot A2, F, à payer 120, échéance 2077-02-01 → reportée au 2077-06-15
     *   AV1 REFUND  lot A2, F, à payer 70, échéance 2077-08-12 → Encaissement
     *   absentes : FX sans PrjMarketId (« FourX J ») ; FP CanBePayed = 0 (« FourP J ») ; FS reste 0,30 (« FourS J ») ;
     *              FR IsFinancialRefund = 0 (« FourR J »)
     * Comptes : a = société + A + droit ; ab = société + A + B + droit ; sansDroit = société + A sans droit ;
     *           sansSociete = A + droit, sans la société.
     * </pre>
     * Onglet Projet attendu pour « a », projets A et B : Décaissement → A → « Fournisseur J » juin 310 (40+100+50+120),
     * sept. 200, nov. 600 ; « J-LA2 » août 300. Encaissement → A → « Fournisseur J » août 70.
     */
    public record Jeu(String jeton, UUID societe, UUID projetA, UUID projetB, UUID lotA1, UUID lotA2, UUID lotB1,
                      String fournisseurNom, String fournisseurBNom, String lotA2Libelle, String lotA3Libelle,
                      UUID p0, UUID p1, UUID p2, UUID p3, UUID p6, UUID p5, UUID fa1, UUID fa2, UUID av1,
                      String absentSansLot, String absentNonPayable, String absentSolde, String absentRefundFaux,
                      AdmUser compteA, AdmUser compteAB, AdmUser compteSansDroit, AdmUser compteSansSociete) {
    }

    public Jeu jeu() {
        String j = FixtureAchat.jeton("TRE");
        UUID societe = achat.societe("SOC-" + j);
        String nomF = "Fournisseur " + j;
        UUID f = achat.fournisseur("FOU-" + j, nomF);
        String nomFb = "FournB " + j;
        UUID fb = achat.fournisseur("FOB-" + j, nomFb);
        UUID projetA = achat.projet(j + "-PA", societe);
        UUID projetB = achat.projet(j + "-PB", societe);
        UUID lotA1 = achat.lot(j + "-LA1", projetA);
        achat.poserSur("PrjMarket", lotA1, "CfgSupplierId", f);
        UUID lotA2 = achat.lot(j + "-LA2", projetA);
        UUID lotA3 = achat.lot(j + "-LA3", projetA);
        UUID lotB1 = achat.lot(j + "-LB1", projetB);
        achat.poserSur("PrjMarket", lotB1, "CfgSupplierId", fb);

        UUID p0 = prevision(lotA1, LocalDate.of(2076, 11, 10), "40");
        UUID p1 = prevision(lotA1, LocalDate.of(2077, 3, 10), "100");
        UUID p6 = prevision(lotA1, LocalDate.of(2077, 6, 25), "50");
        UUID p2 = prevision(lotA1, LocalDate.of(2077, 9, 20), "200");
        UUID p3 = prevision(lotA2, LocalDate.of(2077, 8, 5), "300");
        prevision(lotA3, LocalDate.of(2077, 10, 1), "0");
        prevision(lotA1, LocalDate.of(2078, 1, 10), "500");
        UUID p5 = prevision(lotB1, LocalDate.of(2077, 9, 20), "1000");

        UUID type = achat.typeReglement("VIR-" + j);
        UUID fa1 = achatLot(j + "-FA1", FixtureAchat.FACTURE, f, nomF, societe, projetA, lotA1, "1000",
                LocalDate.of(2077, 11, 10));
        achat.reglement(fa1, j + "-RG1", new BigDecimal("400"), FixtureAchat.EN_COURS, LocalDate.of(2077, 5, 2), type);
        achat.reglement(fa1, j + "-RG2", new BigDecimal("250"), FixtureAchat.ANNULE, LocalDate.of(2077, 5, 3), type);
        UUID fa2 = achatLot(j + "-FA2", FixtureAchat.FACTURE, f, nomF, societe, projetA, lotA2, "120",
                LocalDate.of(2077, 2, 1));
        UUID av1 = achatLot(j + "-AV1", FixtureAchat.AVOIR, f, nomF, societe, projetA, lotA2, "70",
                LocalDate.of(2077, 8, 12));

        String nomX = "FourX " + j;
        achatLot(j + "-FX", FixtureAchat.FACTURE, achat.fournisseur("FOX-" + j, nomX), nomX, societe, projetA, null,
                "5000", LocalDate.of(2077, 9, 1));
        String nomP = "FourP " + j;
        UUID fp = achatLot(j + "-FP", FixtureAchat.FACTURE, achat.fournisseur("FOP-" + j, nomP), nomP, societe, projetA,
                lotA1, "3000", LocalDate.of(2077, 9, 1));
        achat.poser(fp, "CanBePayed", false);
        String nomS = "FourS " + j;
        UUID fs = achatLot(j + "-FS", FixtureAchat.FACTURE, achat.fournisseur("FOS-" + j, nomS), nomS, societe, projetA,
                lotA1, "100", LocalDate.of(2077, 9, 1));
        achat.reglement(fs, j + "-RG3", new BigDecimal("99.7"), FixtureAchat.EN_COURS, LocalDate.of(2077, 5, 4), type);
        String nomR = "FourR " + j;
        UUID fr = achatLot(j + "-FR", FixtureAchat.FACTURE, achat.fournisseur("FOR-" + j, nomR), nomR, societe, projetA,
                lotA1, "800", LocalDate.of(2077, 9, 1));
        achat.poser(fr, "IsFinancialRefund", false);

        AdmUser a = compte("TREA-" + j, societe, true, projetA);
        AdmUser ab = compte("TREAB-" + j, societe, true, projetA, projetB);
        AdmUser sansDroit = compte("TREN-" + j, societe, false, projetA);
        AdmUser sansSociete = compte("TRES-" + j, null, true, projetA);

        return new Jeu(j, societe, projetA, projetB, lotA1, lotA2, lotB1, nomF, nomFb, j + "-LA2", j + "-LA3",
                p0, p1, p2, p3, p6, p5, fa1, fa2, av1, nomX, nomP, nomS, nomR, a, ab, sansDroit, sansSociete);
    }

    /** Projet V de la société du jeu, habilité pour {@code compteA} ; lot de fournisseur « FournV » ; prévision 800 au 20/09. */
    public record Ventile(UUID projet, String fournisseurNom) {
    }

    /** Projet V ventilé sur une tranche à {@code taux} ({@code PrjProjectCfgTrancheVentilation}). */
    public Ventile ventile(Jeu jeu, String taux) {
        String nom = "FournV " + jeu.jeton();
        UUID projet = achat.projet(jeu.jeton() + "-PV", jeu.societe());
        UUID lot = achat.lot(jeu.jeton() + "-LV1", projet);
        achat.poserSur("PrjMarket", lot, "CfgSupplierId", achat.fournisseur("FOV-" + jeu.jeton(), nom));
        prevision(lot, LocalDate.of(2077, 9, 20), "800");
        config.ventilation(projet, config.trancheDuProjet("TV-" + jeu.jeton(), projet), new BigDecimal(taux));
        achat.habiliter(jeu.compteA(), projet);
        return new Ventile(projet, nom);
    }

    /** Ligne {@code PrjMarketPaymentPrevision} du lot ; rend son PKey. */
    public UUID prevision(UUID lot, LocalDate date, String montant) {
        UUID id = UUID.randomUUID();
        em.flush();
        em.createNativeQuery("""
                        INSERT INTO PrjMarketPaymentPrevision (PKey, PaymentDate, Amount, Description, PrjMarketId, Achievement)
                        VALUES (:id, :date, :montant, :description, :lot, 0)""")
                .setParameter(P_ID, id).setParameter("date", date.atStartOfDay())
                .setParameter("montant", new BigDecimal(montant)).setParameter("description", "Prévision " + montant)
                .setParameter("lot", lot).executeUpdate();
        return id;
    }

    /** Pièce payable du module PRJ, {@code lot} nul possible, échéance donnée. */
    private UUID achatLot(String code, String nature, UUID fournisseur, String nom, UUID societe, UUID projet, UUID lot,
                          String aPayer, LocalDate echeance) {
        UUID id = achat.piece(Piece.facture(code, echeance.minusDays(30), fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)).nature(nature).lot(lot));
        achat.poser(id, "MaturityDate", echeance.atStartOfDay());
        achat.poser(id, "CanBePayed", true);
        visibilite.modulePiece(id, visibilite.modulePrj());
        return id;
    }

    private AdmUser compte(String code, UUID societe, boolean droit, UUID... projets) {
        AdmUser compte = achat.compte(code, ROLE);
        if (societe != null) {
            visibilite.habiliterSociete(compte, societe);
        }
        for (UUID projet : projets) {
            achat.habiliter(compte, projet);
        }
        if (droit) {
            achat.droitOmran(compte, DROIT);
        }
        return compte;
    }
}
