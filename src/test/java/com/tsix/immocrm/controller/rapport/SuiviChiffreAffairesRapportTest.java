package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.entities.str.StrStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SUIVI_CA;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N° 15 {@code ComTdbChiffreAffaire} « CHIFFRE D'AFFAIRE » (FEATURE-tdb-vente-rapports-flux.md §4.7, §9 « N° 15 », §6
 * {@code PrevisionsDepuisDossiers}). Année 2025, montants en millions de MAD. T1 : délais 60 j (notaire) / 30 j (solde) ; T2 :
 * délais NULL ; TI : entité inactive.
 * <pre>
 *   ca-notaire       P1, P2 CONC envoi notaire 15/01/2025, 2 000 000 chacun ⇒ mars 4,00 ; P3 ANN idem 3 000 000 (exclu)
 *                    P4 CONC envoi 20/11/2024, 1 500 000 ⇒ janvier 1,50 ; P5 CONC envoi 01/12/2025 ⇒ 2026 (hors année)
 *                    PN sur T2 (délai NULL), PI sur TI : ignorés ; réel : M1 MIN acte 03/03/2025 1 250 000 ⇒ mars 1,25
 *   encaissements    E1 MIN acte 10/04/2025, Price 1 100 000, PriceAfterDiscount 1 000 000 ; réglé 200 000 le 01/02 (avant),
 *   -reste           100 000 CANCELED le 01/03, frais 10 000 le 02/03, 50 000 le 20/05 (après) ⇒ prévu mai 0,80, réel mai 0,05
 *                    E2 DES acte 10/04/2025 (exclu)
 *   concretisation   Q1, Q4 CONC prévisionnelle 05/04/2025 non confirmée, Q2 idem confirmée, 600 000 chacun ⇒ juin ;
 *   -prevue          Q3 MIN idem (statut ≠ concrétisation, exclu)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviChiffreAffairesRapportTest {

    private static final int ANNEE = 2025;
    private static final String CA_NOTAIRE = "ca-notaire";
    private static final String ENCAISSEMENTS_RESTE = "encaissements-reste";
    private static final String CONCRETISATION_PREVUE = "concretisation-prevue";
    private static final String MOIS = "mois";
    private static final String PREVU = "prevu";
    private static final String REEL = "reel";
    private static final String PREVU_QUANTITE = "prevuQuantite";
    private static final String PREVU_MONTANT = "prevuMontant";
    private static final String REEL_QUANTITE = "reelQuantite";
    private static final String REEL_MONTANT = "reelMontant";
    private static final String SIX_CENT_MILLE = "600000";
    private static final List<String> MOIS_FR = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private Jeu jeu;
    private Statuts st;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche inactive;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        jeu = fixture.jeu("N15");
        libre = connecte(rapports.compte("n15." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.delais(jeu.tranche("T1", projet), 60, 30);
        t2 = jeu.delais(jeu.tranche("T2", projet), null, null);
        inactive = jeu.delais(jeu.tranche("TI", jeu.projet("PI", false)), 60, 30);

        envoiNotaire(t1, st.concretisation(), LocalDate.of(ANNEE, 1, 15), "2000000");
        envoiNotaire(t1, st.concretisation(), LocalDate.of(ANNEE, 1, 15), "2000000");
        envoiNotaire(t1, st.annulation(), LocalDate.of(ANNEE, 1, 15), "3000000");
        envoiNotaire(t1, st.concretisation(), LocalDate.of(2024, 11, 20), "1500000");
        envoiNotaire(t1, st.concretisation(), LocalDate.of(ANNEE, 12, 1), "900000");
        envoiNotaire(t2, st.concretisation(), LocalDate.of(ANNEE, 2, 1), "1000000");
        envoiNotaire(inactive, st.concretisation(), LocalDate.of(ANNEE, 1, 15), "7000000");
        jeu.prix(jeu.dater(jeu.dossier(t1, st.minute()), null, j(3, 3), null, null), new BigDecimal("1250000"), null);

        ComFolder e1 = acte(st.minute(), "1000000", null);
        jeu.prix(e1, new BigDecimal("1100000"), null);
        regler(e1, Genre.AUTOFINANCEMENT, "200000", IN_WORK, j(2, 1));
        regler(e1, Genre.AUTOFINANCEMENT, "100000", CANCELED, j(3, 1));
        regler(e1, Genre.FRAIS, "10000", IN_WORK, j(3, 2));
        regler(e1, Genre.AUTOFINANCEMENT, "50000", IN_WORK, j(5, 20));
        acte(st.desistement(), "3000000", j(5, 5));

        previsionnelle(st.concretisation(), false);
        previsionnelle(st.concretisation(), false);
        previsionnelle(st.concretisation(), true);
        previsionnelle(st.minute(), false);
        fixture.relire();
    }

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour);
    }

    private void envoiNotaire(CfgTranche tranche, StrStatus statut, LocalDate envoi, String prix) {
        ComFolder d = jeu.prix(jeu.dossier(tranche, statut), new BigDecimal(prix), null);
        jeu.accord(d, envoi, null, null);
    }

    /** Dossier de T1 à l'acte le 10/04/2025 ; {@code Price}, {@code PriceAfterDiscount}, {@code AgreementPrice} = {@code prix}. */
    private ComFolder acte(StrStatus statut, String prix, LocalDate sortie) {
        ComFolder d = jeu.dater(jeu.dossier(t1, statut), null, j(4, 10), null, sortie);
        return ev.prix(d, new BigDecimal(prix));
    }

    private void previsionnelle(StrStatus statut, boolean confirmee) {
        ComFolder d = jeu.prix(jeu.dossier(t1, statut), new BigDecimal(SIX_CENT_MILLE), null);
        jeu.accord(d, null, j(4, 5), confirmee);
    }

    private void regler(ComFolder dossier, Genre genre, String montant, String statut, LocalDate date) {
        BigDecimal valeur = new BigDecimal(montant);
        jeu.regler(jeu.echeance(dossier, jeu.categorie(genre, true), valeur, date), valeur, true, statut, null, date);
    }

    private List<Map<String, Object>> sections(CfgTranche... tranches) throws Exception {
        return appels.sections(demande(SUIVI_CA, libre, ANNEE, tranches));
    }

    private Map<String, Object> section15(String id) throws Exception {
        return section(sections(t1, t2, inactive), id);
    }

    private static BigDecimal auMois(Map<String, Object> section, int mois, String colonne) {
        return nombre(ligne(section, MOIS, MOIS_FR.get(mois - 1)).get(colonne));
    }

    @Test
    void n15_troisSections_douzeMois_colonnesDuSpec() throws Exception {
        List<Map<String, Object>> sections = sections(t1);

        SoftAssertions soft = new SoftAssertions();
        for (String id : List.of(CA_NOTAIRE, ENCAISSEMENTS_RESTE)) {
            Map<String, Object> s = section(sections, id);
            soft.assertThat(lignes(s)).as(id).extracting(l -> l.get(MOIS)).containsExactlyElementsOf(MOIS_FR);
            soft.assertThat(noms(s)).as(id).containsExactly(MOIS, PREVU, REEL);
        }
        Map<String, Object> s = section(sections, CONCRETISATION_PREVUE);
        soft.assertThat(lignes(s)).extracting(l -> l.get(MOIS)).containsExactlyElementsOf(MOIS_FR);
        soft.assertThat(noms(s)).containsExactly(MOIS, PREVU_QUANTITE, PREVU_MONTANT, REEL_QUANTITE, REEL_MONTANT);
        soft.assertAll();
    }

    @Test
    void n15_sommeParColonne() throws Exception {
        List<Map<String, Object>> sections = sections(t1);

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            for (String nom : noms(s).stream().filter(n -> !MOIS.equals(n)).toList()) {
                soft.assertThat(colonne(s, nom)).as("%s.%s", s.get("id"), nom).containsEntry("fonctionTotal", "SOMME");
                soft.assertThat(nombre(totaux(s).get(nom))).as("%s.%s", s.get("id"), nom)
                        .isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void n15_troisGraphiquesEnColonnes() throws Exception {
        List<Map<String, Object>> graphiques = sections(t1).stream()
                .flatMap(s -> ((List<Map<String, Object>>) s.get("graphiques")).stream()).toList();

        assertThat(graphiques).hasSize(3).allSatisfy(g -> assertThat(g).containsEntry("type", "COLONNES"));
    }

    // §9 n° 15 : prévu 1 = Σ Price du mois de AgreementDate + DelayLawyerMinute jours ; même prix et même date ⇒ somme des deux
    @Test
    void n15_caNotaire_prevuAuMoisDecaleDuDelaiNotaire_deuxDossiersIdentiquesAdditionnes_annuleExclu() throws Exception {
        Map<String, Object> s = section15(CA_NOTAIRE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 3, PREVU)).as("P1 + P2 (P3 annulé exclu)").isEqualByComparingTo("4.00");
        soft.assertThat(auMois(s, 1, PREVU)).as("P4, envoi 2024 + 60 j").isEqualByComparingTo("1.50");
        soft.assertThat(auMois(s, 2, PREVU)).as("PN : délai NULL ignoré").isEqualByComparingTo("0");
        soft.assertThat(nombre(totaux(s).get(PREVU))).as("P5 en 2026, PI invisible").isEqualByComparingTo("5.50");
        soft.assertAll();
    }

    @Test
    void n15_caNotaire_reel_caTtcDuMois_enMillions() throws Exception {
        Map<String, Object> s = section15(CA_NOTAIRE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 3, REEL)).as("M1").isEqualByComparingTo("1.25");
        soft.assertThat(auMois(s, 4, REEL)).as("E1 (E2 désisté exclu)").isEqualByComparingTo("1.10");
        soft.assertThat(nombre(totaux(s).get(REEL))).isEqualByComparingTo("2.35");
        soft.assertAll();
    }

    // §4.7 (a) : réglé avant l'acte selon §3.3 — CANCELED et frais ignorés ; mois de l'acte + DelayMinuteAllSettled
    @Test
    void n15_encaissementsReste_prevuEgalPrixRemiseMoinsRegleAvantActe_reglementAnnuleIgnore() throws Exception {
        Map<String, Object> s = section15(ENCAISSEMENTS_RESTE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 5, PREVU)).as("1 000 000 − 200 000, E2 désisté exclu").isEqualByComparingTo("0.80");
        soft.assertThat(nombre(totaux(s).get(PREVU))).isEqualByComparingTo("0.80");
        soft.assertAll();
    }

    @Test
    void n15_encaissementsReste_reel_encaissementsApresActeDuMois() throws Exception {
        Map<String, Object> s = section15(ENCAISSEMENTS_RESTE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 5, REEL)).isEqualByComparingTo("0.05");
        soft.assertThat(nombre(totaux(s).get(REEL))).isEqualByComparingTo("0.05");
        soft.assertAll();
    }

    @Test
    void n15_concretisationPrevue_statutExact_confirmeeEnReel_nonConfirmeeEnPrevu_auMoisDecale() throws Exception {
        Map<String, Object> s = section15(CONCRETISATION_PREVUE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 6, PREVU_QUANTITE)).as("Q1 + Q4, Q3 MIN exclu").isEqualByComparingTo("2");
        soft.assertThat(auMois(s, 6, PREVU_MONTANT)).isEqualByComparingTo("1.20");
        soft.assertThat(auMois(s, 6, REEL_QUANTITE)).isEqualByComparingTo("1");
        soft.assertThat(auMois(s, 6, REEL_MONTANT)).isEqualByComparingTo("0.60");
        soft.assertThat(nombre(totaux(s).get(PREVU_QUANTITE))).isEqualByComparingTo("2");
        soft.assertAll();
    }

    // §4.7 (c) : délai de tranche NULL ⇒ dossier ignoré avec avertissement
    @Test
    void n15_delaiDeTrancheNul_avertissementEnPlus() throws Exception {
        List<String> sansT2 = JsonPath.read(appels.corps(demande(SUIVI_CA, libre, ANNEE, t1)), "$.avertissements");
        List<String> avecT2 = JsonPath.read(appels.corps(demande(SUIVI_CA, libre, ANNEE, t1, t2)), "$.avertissements");

        assertThat(avecT2).hasSizeGreaterThan(sansT2.size());
    }

    @Test
    void n15_trancheInvisible_aucunPrevuNiReel() throws Exception {
        List<Map<String, Object>> sections = sections(inactive);

        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(RapportsFluxSupport.sommeDesChiffres(s))
                .as(String.valueOf(s.get("id"))).isEqualByComparingTo("0"));
    }
}
