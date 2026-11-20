package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_CONFIDENTIEL;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_AJOUT_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_ECARTER_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.DROIT_SUPPRESSION_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b R15 (présélection des fournisseurs), R7, R8 (phase 002), R9 / D6 / D7 sur les routes {@code /fournisseurs}
 * (FEATURE-n6-demandes-achat.md). {@code fid} = PKey de la ligne {@code BuySelectionSupplier} ; corps d'ajout {@code {fournisseurId}}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatFournisseursTest {

    private static final String FOURNISSEURS = "/fournisseurs";
    private static final String FOURNISSEUR_ID = "fournisseurId";
    private static final String DOUBLON = "Ce fournisseur est déja ajouté";
    private static final String PRESELECTIONS = "SELECT COUNT(*) FROM BuySelectionSupplier WHERE BuySelectionId = ?";
    private static final String PRESELECTION = "SELECT COUNT(*) FROM BuySelectionSupplier WHERE PKey = ?";
    private static final String ECARTE = "SELECT IsRejected FROM BuySelectionSupplier WHERE PKey = ?";
    private static final String OFFRE_ECARTEE = "SELECT IsRejected FROM BuyDocument WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("FO");
        qLibre = qui(b.libre(c, "n6b.fo.libre." + c.jeton()));
    }

    private UUID dia(String suffixe, UUID statut) {
        return demandes.demande(c, c.code(suffixe), statut);
    }

    private UUID tiers(String suffixe) {
        return achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
    }

    private static CorpsJson ajout(UUID tiers) {
        return corps().avec(FOURNISSEUR_ID, tiers);
    }

    private static String fid(UUID preselection) {
        return FOURNISSEURS + "/" + preselection;
    }

    private RequestPostProcessor compteSans(String droit, String suffixe) {
        return qui(demandes.libre(c, "n6b.fo." + suffixe + "." + c.jeton(), sauf(droit)));
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void r15_lecture_listeLesFournisseursPreselectionnes() throws Exception {
        UUID dia = dia("LEC", c.prep());
        UUID t = tiers("LEC");
        b.preselection(dia, t, LocalDate.of(2026, 3, 20));

        Reponse r = appels.lire(qLibre, dia, FOURNISSEURS);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.corps()).contains("Fournisseur LEC " + c.jeton());
    }

    // ------------------------------------------------------------------ R15

    @Test
    void r15_ajout_puisDoublon_409_uneSeuleLigne() throws Exception {
        UUID dia = dia("DBL", c.prep());
        UUID t = tiers("DBL");

        Reponse premier = appels.poster(qLibre, dia, FOURNISSEURS, ajout(t));
        Reponse doublon = appels.poster(qLibre, dia, FOURNISSEURS, ajout(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(premier.statut()).as(premier.corps()).isBetween(200, 299);
            s.assertThat(doublon.statut()).as(doublon.corps()).isEqualTo(409);
            s.assertThat(doublon.corps()).contains(DOUBLON);
            s.assertThat(b.compterApresRefus(PRESELECTIONS, dia.toString())).isEqualTo(1);
        });
    }

    @Test
    void r15_suppression_fournisseurAyantDesOffres_409_sansOffre_supprime() throws Exception {
        UUID dia = dia("SUP", c.prep());
        UUID avecOffre = tiers("SUPA");
        UUID sansOffre = tiers("SUPB");
        UUID pa = b.preselection(dia, avecOffre, LocalDate.of(2026, 3, 20));
        UUID pb = b.preselection(dia, sansOffre, LocalDate.of(2026, 3, 20));
        b.offre(dia, avecOffre, c.code("OSUP"), false);

        Reponse refus = appels.retirer(qLibre, dia, fid(pa));
        Reponse temoin = appels.retirer(qLibre, dia, fid(pb));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.compterApresRefus(PRESELECTION, pa.toString())).isEqualTo(1);
            s.assertThat(b.compterApresRefus(PRESELECTION, pb.toString())).isZero();
        });
    }

    @Test
    void r15_ecarter_fournisseurDeLOffreSelectionnee_409_autreFournisseur_ecarte() throws Exception {
        UUID dia = dia("ECS", c.prep());
        UUID retenu = tiers("ECSA");
        UUID autre = tiers("ECSB");
        UUID pa = b.preselection(dia, retenu, LocalDate.of(2026, 3, 20));
        UUID pb = b.preselection(dia, autre, LocalDate.of(2026, 3, 20));
        b.offre(dia, retenu, c.code("OECSA"), true);
        b.offre(dia, autre, c.code("OECSB"), false);

        Reponse refus = appels.poster(qLibre, dia, fid(pa) + "/ecarter", corps());
        Reponse temoin = appels.poster(qLibre, dia, fid(pb) + "/ecarter", corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.lignesApresRefus(ECARTE, pa.toString())).singleElement()
                    .satisfies(l -> assertThat(l.get("IsRejected")).isEqualTo(false));
        });
    }

    @Test
    void r15_ecarter_propageIsRejectedASesOffres_reactiver_remetA0() throws Exception {
        UUID dia = dia("ECP", c.prep());
        UUID cible = tiers("ECPA");
        UUID autre = tiers("ECPB");
        UUID pa = b.preselection(dia, cible, LocalDate.of(2026, 3, 20));
        b.preselection(dia, autre, LocalDate.of(2026, 3, 20));
        UUID o1 = b.offre(dia, cible, c.code("OECP1"), false);
        UUID o2 = b.offre(dia, cible, c.code("OECP2"), false);
        UUID oAutre = b.offre(dia, autre, c.code("OECP3"), false);

        Reponse ecarter = appels.poster(qLibre, dia, fid(pa) + "/ecarter", corps());
        Object fournisseurEcarte = b.lignes(ECARTE, pa.toString()).getFirst().get("IsRejected");
        Object o1Ecartee = b.lignes(OFFRE_ECARTEE, o1.toString()).getFirst().get("IsRejected");
        Object o2Ecartee = b.lignes(OFFRE_ECARTEE, o2.toString()).getFirst().get("IsRejected");
        Object autreApresEcart = b.lignes(OFFRE_ECARTEE, oAutre.toString()).getFirst().get("IsRejected");
        Reponse reactiver = appels.poster(qLibre, dia, fid(pa) + "/reactiver", corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ecarter.statut()).as(ecarter.corps()).isBetween(200, 299);
            s.assertThat(fournisseurEcarte).isEqualTo(true);
            s.assertThat(o1Ecartee).isEqualTo(true);
            s.assertThat(o2Ecartee).isEqualTo(true);
            s.assertThat(autreApresEcart).isEqualTo(false);
            s.assertThat(reactiver.statut()).as(reactiver.corps()).isBetween(200, 299);
            s.assertThat(b.lignes(ECARTE, pa.toString()).getFirst().get("IsRejected")).isEqualTo(false);
            s.assertThat(b.lignes(OFFRE_ECARTEE, o1.toString()).getFirst().get("IsRejected")).isEqualTo(false);
            s.assertThat(b.lignes(OFFRE_ECARTEE, o2.toString()).getFirst().get("IsRejected")).isEqualTo(false);
        });
    }

    // ------------------------------------------------------------------ R7, R8

    @Test
    void r8_ajout_horsPhasePreselection_409() throws Exception {
        UUID horsPhase = dia("PH0", c.depouil());
        UUID enPhase = dia("PH1", c.prep());
        UUID t = tiers("PH");

        Reponse refus = appels.poster(qLibre, horsPhase, FOURNISSEURS, ajout(t));
        Reponse temoin = appels.poster(qLibre, enPhase, FOURNISSEURS, ajout(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.compterApresRefus(PRESELECTIONS, horsPhase.toString())).isZero();
        });
    }

    @Test
    void r8_ecarter_horsPhasePreselection_409() throws Exception {
        UUID horsPhase = dia("PE0", c.depouil());
        UUID enPhase = dia("PE1", c.prep());
        UUID t = tiers("PE");
        UUID p0 = b.preselection(horsPhase, t, LocalDate.of(2026, 3, 20));
        UUID p1 = b.preselection(enPhase, t, LocalDate.of(2026, 3, 20));

        Reponse refus = appels.poster(qLibre, horsPhase, fid(p0) + "/ecarter", corps());
        Reponse temoin = appels.poster(qLibre, enPhase, fid(p1) + "/ecarter", corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void r7_demandeNonDerniere_ajout_409() throws Exception {
        UUID ancienne = dia("R7A", c.prep());
        demandes.poser(DEMANDE, ancienne, "IsTheLast", false);
        UUID derniere = dia("R7B", c.prep());
        UUID t = tiers("R7");

        Reponse refus = appels.poster(qLibre, ancienne, FOURNISSEURS, ajout(t));
        Reponse temoin = appels.poster(qLibre, derniere, FOURNISSEURS, ajout(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
            s.assertThat(b.compterApresRefus(PRESELECTIONS, ancienne.toString())).isZero();
        });
    }

    // ------------------------------------------------------------------ D7 : droits

    @Test
    void d7_sansDroitAjoutFournisseur_403() throws Exception {
        UUID dia = dia("D7A", c.prep());
        UUID t = tiers("D7A");

        Reponse refus = appels.poster(compteSans(DROIT_AJOUT_FOURNISSEUR, "sansajout"), dia, FOURNISSEURS, ajout(t));
        Reponse temoin = appels.poster(qLibre, dia, FOURNISSEURS, ajout(t));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d7_sansDroitSuppressionFournisseur_403() throws Exception {
        UUID dia = dia("D7S", c.prep());
        UUID p = b.preselection(dia, tiers("D7S"), LocalDate.of(2026, 3, 20));

        Reponse refus = appels.retirer(compteSans(DROIT_SUPPRESSION_FOURNISSEUR, "sanssup"), dia, fid(p));
        int apresRefus = b.compterApresRefus(PRESELECTION, p.toString());
        Reponse temoin = appels.retirer(qLibre, dia, fid(p));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(apresRefus).isEqualTo(1);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    @Test
    void d7_sansDroitEcarterFournisseur_403() throws Exception {
        UUID dia = dia("D7E", c.prep());
        UUID p = b.preselection(dia, tiers("D7E"), LocalDate.of(2026, 3, 20));

        Reponse refus = appels.poster(compteSans(DROIT_ECARTER_FOURNISSEUR, "sansecart"), dia, fid(p) + "/ecarter", corps());
        Reponse temoin = appels.poster(qLibre, dia, fid(p) + "/ecarter", corps());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isBetween(200, 299);
        });
    }

    // ------------------------------------------------------------------ D6 confidentialité, R9 visibilité

    @Test
    void d6_procedureConfidentielle_sansLeDroit_preselection_403() throws Exception {
        UUID confidentielle = demandes.procedure("PCONF-" + c.jeton(), c.entite());
        demandes.confidentielle(confidentielle);
        UUID diaConf = demandes.demande(c, c.code("CONF"), c.prep(), confidentielle, LocalDate.of(2026, 3, 10));
        UUID diaOrdinaire = dia("ORD", c.prep());
        RequestPostProcessor sansConf = compteSans(DROIT_CONFIDENTIEL, "sansconf");

        Reponse refus = appels.lire(sansConf, diaConf, FOURNISSEURS);
        Reponse temoinOrdinaire = appels.lire(sansConf, diaOrdinaire, FOURNISSEURS);
        Reponse temoinAvecDroit = appels.lire(qLibre, diaConf, FOURNISSEURS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoinOrdinaire.statut()).as(temoinOrdinaire.corps()).isEqualTo(200);
            s.assertThat(temoinAvecDroit.statut()).as(temoinAvecDroit.corps()).isEqualTo(200);
        });
    }

    @Test
    void r9_horsPerimetre_404_avantLe403DeDroit() throws Exception {
        UUID horsPerimetre = dia("HP", c.prep());
        demandes.poser(DEMANDE, horsPerimetre, "PrjProjectId", demandes.projetDe(c, "PRJ-HP-" + c.jeton()));
        UUID visible = dia("VIS", c.prep());
        UUID t = tiers("HP");

        Reponse lecture = appels.lire(qLibre, horsPerimetre, FOURNISSEURS);
        Reponse ajoutSansDroit = appels.poster(compteSans(DROIT_AJOUT_FOURNISSEUR, "hp"), horsPerimetre, FOURNISSEURS, ajout(t));
        Reponse temoin = appels.lire(qLibre, visible, FOURNISSEURS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
            s.assertThat(ajoutSansDroit.statut()).as(ajoutSansDroit.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void r15_doublon_messageDansLeDetail() throws Exception {
        UUID dia = dia("DBM", c.prep());
        UUID t = tiers("DBM");
        b.preselection(dia, t, LocalDate.of(2026, 3, 20));

        Reponse doublon = appels.poster(qLibre, dia, FOURNISSEURS, ajout(t));

        assertThat(doublon.statut()).as(doublon.corps()).isEqualTo(409);
        assertThat(doublon.valeur(DETAIL)).isEqualTo(DOUBLON);
    }
}
