package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
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

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.COMPARAISON;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.PREVU_NON_SAISI;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 3 {@code ComTdbSaleComparaison} « COMPARAISON DES VENTES » (FEATURE-tdb-vente-rapports-flux.md §4.4, §9 « N° 3 »). Une seule
 * tranche T1, toutes années ; mois sans cellule absents :
 * <pre>
 *   M1 MIN acte 03/03/2023 (1 200 000) ; encaissement 1 000 le 05/05/2024
 *   M2 MIN acte 10/06/2024 (800 000)   ; encaissement 500 le 20/06/2024
 *   TI : tranche d'une entité inactive (invisible)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComparaisonVentesRapportTest {

    private static final String CHIFFRE_AFFAIRES = "chiffre-affaires";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String ANNEE = "annee";
    private static final String MOIS = "mois";
    private static final String BASE = "base";
    private static final String PREVISION = "prevision";
    private static final String REEL = "reel";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche t1;
    private CfgTranche invisible;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N03");
        libre = connecte(rapports.compte("n03." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        invisible = jeu.tranche("TI", jeu.projet("PI", false));

        ComFolder m1 = jeu.dater(jeu.dossier(t1, st.minute()), null, LocalDate.of(2023, 3, 3), null, null);
        jeu.encaisser(m1, "1000", LocalDate.of(2024, 5, 5));
        ComFolder m2 = jeu.prix(jeu.dater(jeu.dossier(t1, st.minute()), null, LocalDate.of(2024, 6, 10), null, null),
                new BigDecimal("800000"), null);
        jeu.encaisser(m2, "500", LocalDate.of(2024, 6, 20));
        ComFolder i1 = jeu.dater(jeu.dossier(invisible, st.minute()), null, LocalDate.of(2024, 6, 10), null, null);
        jeu.encaisser(i1, "999", LocalDate.of(2024, 6, 20));
        fixture.relire();
    }

    private List<Map<String, Object>> sections() throws Exception {
        return appels.sections(demande(COMPARAISON, libre, null, t1));
    }

    /** Lignes de l'année et du mois (libellé français, éventuellement suivi de l'année). */
    private static List<Map<String, Object>> lignesDu(Map<String, Object> section, int annee, String mois) {
        return lignes(section).stream().filter(l -> String.valueOf(annee).equals(String.valueOf(l.get(ANNEE)))
                && String.valueOf(l.get(MOIS)).startsWith(mois)).toList();
    }

    @Test
    void n3_sansTrancheId_400() throws Exception {
        assertThat(appels.statut(get(resultat(VENTE, COMPARAISON)).with(libre))).isEqualTo(400);
    }

    @Test
    void n3_parametreTrancheId_obligatoire_nonMultiple() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, COMPARAISON)).with(libre));

        List<Boolean> obligatoire = JsonPath.read(corps, "$.parametres[?(@.nom == 'trancheId')].obligatoire");
        List<Boolean> multiple = JsonPath.read(corps, "$.parametres[?(@.nom == 'trancheId')].multiple");
        assertThat(obligatoire).containsExactly(true);
        assertThat(multiple).containsExactly(false);
    }

    @Test
    void n3_trancheInvisible_sectionsVides_sansErreur() throws Exception {
        List<Map<String, Object>> sections = appels.sections(get(resultat(VENTE, COMPARAISON)).with(libre)
                .param(TRANCHE_ID, invisible.getId().toString()));

        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(lignes(s)).isEmpty());
    }

    @Test
    void n3_moisAvecDonnees_presents_reelDuMois_moisSansCelluleAbsent() throws Exception {
        List<Map<String, Object>> sections = sections();
        Map<String, Object> ca = section(sections, CHIFFRE_AFFAIRES);
        Map<String, Object> enc = section(sections, ENCAISSEMENTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignesDu(ca, 2023, "Mars")).hasSize(1)
                .allSatisfy(l -> assertThat(nombre(l.get(REEL))).isEqualByComparingTo("1200000"));
        soft.assertThat(lignesDu(ca, 2024, "Juin")).hasSize(1)
                .allSatisfy(l -> assertThat(nombre(l.get(REEL))).isEqualByComparingTo("800000"));
        soft.assertThat(lignesDu(enc, 2024, "Mai")).hasSize(1)
                .allSatisfy(l -> assertThat(nombre(l.get(REEL))).isEqualByComparingTo("1000"));
        soft.assertThat(lignesDu(enc, 2024, "Juin")).hasSize(1)
                .allSatisfy(l -> assertThat(nombre(l.get(REEL))).isEqualByComparingTo("500"));
        for (Map<String, Object> s : List.of(ca, enc)) {
            soft.assertThat(lignesDu(s, 2023, "Avril")).as("%s avril 2023", s.get("id")).isEmpty();
            soft.assertThat(lignesDu(s, 2024, "Janvier")).as("%s janvier 2024", s.get("id")).isEmpty();
        }
        soft.assertThat(nombre(totaux(enc).get(REEL))).as("tranche invisible ignorée").isEqualByComparingTo("1500");
        soft.assertAll();
    }

    @Test
    void n3_colonnes_groupeParAnnee_sousTotauxEgauxALaSommeDesMois() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : List.of(section(sections, CHIFFRE_AFFAIRES), section(sections, ENCAISSEMENTS))) {
            soft.assertThat(noms(s)).as("%s colonnes", s.get("id")).containsExactly(ANNEE, MOIS, BASE, PREVISION, REEL);
            soft.assertThat(s).containsEntry("groupePar", ANNEE);
            for (String nom : List.of(BASE, PREVISION, REEL)) {
                soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo("MONTANT");
                soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
            }
            Map<String, Map<String, Object>> parAnnee = sousTotaux(s);
            soft.assertThat(parAnnee).as("%s sous-totaux", s.get("id")).isNotNull().isNotEmpty();
            if (parAnnee != null) {
                parAnnee.forEach((annee, valeurs) -> soft.assertThat(nombre(valeurs.get(REEL))).as("%s %s", s.get("id"), annee)
                        .isEqualByComparingTo(somme(lignes(s).stream()
                                .filter(l -> annee.equals(String.valueOf(l.get(ANNEE)))).toList(), REEL)));
            }
        }
        soft.assertAll();
    }

    @Test
    void n3_basePrevisionAZero_avertissementPrevuNonSaisi() throws Exception {
        String corps = appels.corps(demande(COMPARAISON, libre, null, t1));
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(somme(lignes(s), BASE)).as("%s base", s.get("id")).isEqualByComparingTo(BigDecimal.ZERO);
            soft.assertThat(somme(lignes(s), PREVISION)).as("%s prévision", s.get("id"))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
        soft.assertThat(avertissements).anyMatch(a -> a.contains(PREVU_NON_SAISI));
        soft.assertAll();
    }
}
