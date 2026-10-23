package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.InstanceOfAssertFactories;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.PREVU_NON_SAISI;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N° 5 {@code ComTdbActiviteParMois} « Objectifs (ENC, CONC, CH) PAR MOIS » (FEATURE-tdb-vente-rapports-flux.md §4.1, §9 « N° 5 »),
 * sections de R1a (la section {@code situation} est en R1b). Année 2025, tranche T1 :
 * <pre>
 *   N1 CONC  concrétisé 10/02 ; avance due 100 000, réglée 30 000 le 05/03 ; frais de dossier 150 et transfert 90 en 08 ;
 *            remise 1 000
 *   A1 ANN   concrétisé 15/02, annulé 20/04 ; frais d'annulation dus 500 le 04/04 ; remise 5 000 (exclue)
 *   M1 MIN   acte 04/04 (1 200 000) ; encaissement 2 000 le 07/07 ; remise 2 000
 *   L1 CLEFS livré 06/06
 *   S1 CONC  sur sous-article, remise 700 (exclue) ;  V1 CONC concrétisé 2019, remise 300 (toutes dates)
 * </pre>
 * Les colonnes de {@code mensuel} se retrouvent par leur groupe d'en-tête (libellés du spec) et leur suffixe Prevu / Reel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteParMoisRapportTest {

    private static final int ANNEE = 2025;
    private static final String MENSUEL = "mensuel";
    private static final String CUMULS = "cumuls";
    private static final String A_CE_JOUR = "a-ce-jour";
    private static final String FRAIS = "frais";
    private static final String REMISES = "remises";
    private static final String MOIS = "mois";
    private static final String REEL = "Reel";
    private static final String PREVU = "Prevu";
    private static final String CONC_QUANTITE = "Concrétisation quantité";
    private static final String CONC_MONTANT = "Concrétisation montant (avances encaissées)";
    private static final String CA_TTC = "C.A. TTC";
    private static final String FINALISATION = "Finalisation quantité (livraisons)";
    private static final String ENCAISSEMENTS = "Encaissements (*)";
    private static final List<String> GROUPES = List.of(CONC_QUANTITE, CONC_MONTANT, CA_TTC, FINALISATION,
            ENCAISSEMENTS);
    private static final List<String> MOIS_FR = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre");
    private static final String GRAPHIQUES = "graphiques";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche t1;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N05");
        libre = connecte(rapports.compte("n05." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        ComSettlementCategory fraisDossier = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory transfert = jeu.categorie(Genre.FRAIS, true);
        ComSettlementCategory annulation = jeu.categorie(Genre.FRAIS, true);
        fixture.parametre(FixtureFluxVente.FRAIS_DOSSIER, fraisDossier.getId());
        fixture.parametre(FixtureFluxVente.FRAIS_TRANSFERT_CLIENT, transfert.getId());
        fixture.parametre(FixtureFluxVente.FRAIS_ANNULATION, annulation.getId());

        ComFolder n1 = jeu.remise(jeu.dater(jeu.dossier(t1, st.concretisation()), j(2, 10), null, null, null),
                new BigDecimal("1000"));
        jeu.echeance(n1, jeu.categorie(Genre.AVANCE, true), new BigDecimal("70000"), j(2, 10));
        jeu.regler(jeu.echeance(n1, jeu.categorie(Genre.AVANCE, true), new BigDecimal("30000"), j(3, 5)),
                new BigDecimal("30000"), true, IN_WORK, null, j(3, 5));
        jeu.regler(jeu.echeance(n1, fraisDossier, new BigDecimal("150"), j(8, 8)), new BigDecimal("150"), true,
                IN_WORK, null, j(8, 8));
        jeu.regler(jeu.echeance(n1, transfert, new BigDecimal("90"), j(8, 9)), new BigDecimal("90"), true, IN_WORK,
                null, j(8, 9));
        ComFolder a1 = jeu.remise(jeu.dater(jeu.dossier(t1, st.annulation()), j(2, 15), null, null, j(4, 20)),
                new BigDecimal("5000"));
        jeu.echeance(a1, annulation, new BigDecimal("500"), j(4, 4));
        ComFolder m1 = jeu.remise(jeu.dater(jeu.dossier(t1, st.minute()), null, j(4, 4), null, null),
                new BigDecimal("2000"));
        jeu.encaisser(m1, "2000", j(7, 7));
        jeu.dater(jeu.dossier(t1, st.remiseClefs()), null, null, j(6, 6), null);
        jeu.remise(jeu.dossierSousArticle(t1, st.concretisation()), new BigDecimal("700"));
        jeu.remise(jeu.dater(jeu.dossier(t1, st.concretisation()), LocalDate.of(2019, 1, 1), null, null, null),
                new BigDecimal("300"));
        fixture.relire();
    }

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour);
    }

    private List<Map<String, Object>> sections(int annee) throws Exception {
        return appels.sections(demande(ACTIVITE_PAR_MOIS, libre, annee, t1));
    }

    /** Nom de la colonne de {@code section} du groupe d'en-tête {@code groupe} finissant par {@code suffixe}. */
    private static String nom(Map<String, Object> section, String groupe, String suffixe) {
        List<String> trouves = colonnes(section).stream()
                .filter(c -> groupe.equals(c.get("groupe")) && String.valueOf(c.get("nom")).endsWith(suffixe))
                .map(c -> (String) c.get("nom")).toList();
        assertThat(trouves).as("colonne %s…%s", groupe, suffixe).hasSize(1);
        return trouves.get(0);
    }

    private static BigDecimal auMois(Map<String, Object> section, int mois, String colonne) {
        return nombre(ligne(section, MOIS, MOIS_FR.get(mois - 1)).get(colonne));
    }

    @Test
    void n5_mensuel_toujoursDouzeLignes_libellesDesMois_memeSansDonnees() throws Exception {
        for (int annee : List.of(ANNEE, 2020)) {
            Map<String, Object> mensuel = section(sections(annee), MENSUEL);

            assertThat(lignes(mensuel)).as("%d", annee).extracting(l -> l.get(MOIS))
                    .containsExactlyElementsOf(MOIS_FR);
        }
    }

    @Test
    void n5_mensuel_reel_nettesAvancesEncaisseesCaLivraisonsEncaissements() throws Exception {
        Map<String, Object> s = section(sections(ANNEE), MENSUEL);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(auMois(s, 2, nom(s, CONC_QUANTITE, REEL))).as("nettes : A1 exclu").isEqualByComparingTo("1");
        soft.assertThat(auMois(s, 3, nom(s, CONC_MONTANT, REEL))).as("avances encaissées, pas le dû")
                .isEqualByComparingTo("30000");
        soft.assertThat(auMois(s, 4, nom(s, CA_TTC, REEL))).isEqualByComparingTo("1200000");
        soft.assertThat(auMois(s, 6, nom(s, FINALISATION, REEL))).isEqualByComparingTo("1");
        soft.assertThat(auMois(s, 3, nom(s, ENCAISSEMENTS, REEL))).isEqualByComparingTo("30000");
        soft.assertThat(auMois(s, 7, nom(s, ENCAISSEMENTS, REEL))).isEqualByComparingTo("2000");
        soft.assertThat(nombre(totaux(s).get(nom(s, CONC_MONTANT, REEL)))).isEqualByComparingTo("30000");
        soft.assertAll();
    }

    @Test
    void n5_prevuAZero_avertissementPrevuNonSaisi() throws Exception {
        String corps = appels.corps(demande(ACTIVITE_PAR_MOIS, libre, ANNEE, t1));
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        Map<String, Object> s = section(sections, MENSUEL);

        SoftAssertions soft = new SoftAssertions();
        for (String groupe : GROUPES) {
            soft.assertThat(somme(lignes(s), nom(s, groupe, PREVU))).as(groupe).isEqualByComparingTo("0");
        }
        soft.assertThat(avertissements).anyMatch(a -> a.contains(PREVU_NON_SAISI));
        soft.assertAll();
    }

    @Test
    void n5_mensuel_typesTotauxEtTitre() throws Exception {
        Map<String, Object> s = section(sections(ANNEE), MENSUEL);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(String.valueOf(s.get("titre"))).contains("Objectifs commerciaux " + ANNEE);
        for (String groupe : GROUPES) {
            String type = List.of(CONC_QUANTITE, FINALISATION).contains(groupe) ? "NOMBRE" : "MONTANT";
            for (String suffixe : List.of(PREVU, REEL)) {
                String nom = nom(s, groupe, suffixe);
                soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo(type);
                soft.assertThat(colonne(s, nom).get("fonctionTotal")).as(nom).isEqualTo("SOMME");
                soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    @Test
    void n5_cumuls_douzeLignes_cumulProgressifDuMensuel_sansTotal() throws Exception {
        List<Map<String, Object>> sections = sections(ANNEE);
        Map<String, Object> mensuel = section(sections, MENSUEL);
        Map<String, Object> cumuls = section(sections, CUMULS);

        assertThat(lignes(cumuls)).hasSize(12);
        SoftAssertions soft = new SoftAssertions();
        for (String groupe : GROUPES) {
            String reel = nom(mensuel, groupe, REEL);
            String prefixe = reel.substring(0, reel.length() - REEL.length());
            soft.assertThat(colonnes(cumuls)).as(groupe).anyMatch(c -> (prefixe + "CumulPrevu").equals(c.get("nom")));
            BigDecimal cumul = BigDecimal.ZERO;
            for (int k = 1; k <= 12; k++) {
                cumul = cumul.add(auMois(mensuel, k, reel));
                soft.assertThat(auMois(cumuls, k, prefixe + "CumulReel")).as("%s mois %d", groupe, k)
                        .isEqualByComparingTo(cumul);
            }
        }
        Map<String, Object> totauxCumuls = totaux(cumuls);
        soft.assertThat(totauxCumuls == null || totauxCumuls.values().stream().allMatch(v -> v == null
                || v instanceof String)).as("un cumul ne s'additionne pas").isTrue();
        soft.assertAll();
    }

    @Test
    void n5_aCeJour_cinqMesures_reelDeLAnnee_tauxPourcentageZeroSiPrevuNul() throws Exception {
        List<Map<String, Object>> sections = sections(ANNEE);
        Map<String, Object> mensuel = section(sections, MENSUEL);
        Map<String, Object> aCeJour = section(sections, A_CE_JOUR);

        List<BigDecimal> reelsAnnuels = new ArrayList<>();
        for (String groupe : GROUPES) {
            reelsAnnuels.add(somme(lignes(mensuel), nom(mensuel, groupe, REEL)));
        }
        assertThat(lignes(aCeJour)).hasSize(5);
        assertThat(colonne(aCeJour, "taux")).containsEntry("type", "POURCENTAGE");
        assertThat(lignes(aCeJour)).isNotEmpty()
                .allSatisfy(l -> assertThat(nombre(l.get("taux"))).isEqualByComparingTo("0"))
                .allSatisfy(l -> assertThat(nombre(l.get("prevuACeJour"))).isEqualByComparingTo("0"));
        assertThat(lignes(aCeJour).stream().map(l -> nombre(l.get("reel"))).toList())
                .usingElementComparator(Comparator.naturalOrder())
                .containsExactlyInAnyOrderElementsOf(reelsAnnuels);
    }

    @Test
    void n5_frais_douzeLignes_dossierTransfertDesistementDus_sommes() throws Exception {
        Map<String, Object> s = section(sections(ANNEE), FRAIS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignes(s)).hasSize(12);
        soft.assertThat(auMois(s, 8, "fraisDossier")).isEqualByComparingTo("150");
        soft.assertThat(auMois(s, 8, "fraisTransfert")).isEqualByComparingTo("90");
        soft.assertThat(auMois(s, 4, "fraisDesistement")).isEqualByComparingTo("500");
        for (String nom : List.of("fraisDossier", "fraisTransfert", "fraisDesistement")) {
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertAll();
    }

    @Test
    void n5_remises_dossiersNiAnnulesNiDesistes_articleRacine_toutesDates() throws Exception {
        Map<String, Object> s = section(sections(ANNEE), REMISES);

        assertThat(lignes(s)).hasSize(1);
        assertThat(nombre(lignes(s).get(0).get("totalRemises"))).isEqualByComparingTo("3300");
    }

    @Test
    @SuppressWarnings("unchecked")
    void n5_dixGraphiques_cinqColonnesDansMensuel_cinqLignesDansCumuls() throws Exception {
        List<Map<String, Object>> sections = sections(ANNEE);
        List<Map<String, Object>> mensuel = (List<Map<String, Object>>) section(sections, MENSUEL).get(GRAPHIQUES);
        List<Map<String, Object>> cumuls = (List<Map<String, Object>>) section(sections, CUMULS).get(GRAPHIQUES);

        assertThat(mensuel).hasSize(5).allSatisfy(g -> assertThat(g).containsEntry("type", "COLONNES")
                .containsEntry("colonneCategorie", MOIS)
                .extractingByKey("colonnesValeurs").asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2));
        assertThat(cumuls).hasSize(5).allSatisfy(g -> assertThat(g).containsEntry("type", "LIGNES"));
    }
}
