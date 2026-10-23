package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.PATRIMOINE;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.XLSX;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.colonneDeLibelle;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.libellesColonnes;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.ACTIONNAIRES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ACTIONNAIRES — ENTITÉS JURIDIQUES (FEATURE-patrimoine-rapports.md §5, T8 ; Q7). Tableau croisé actionnaire × entité :
 * <pre>
 *   SH1 : EA1 10, EA2 5        SH2 : EA1 3, EI 7 (entité INACTIVE)        SH3 : aucune part
 *   SH4 : EA2 NULL             EA3 (active) : aucune part
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionnairesEntitesPatrimoineTest {

    private static final String REPARTITION = "repartition";
    private static final String ACTIONNAIRE = "actionnaire";
    private static final String TOTAL = "total";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixturePatrimoine patrimoine;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private String prefixe;
    private CfgCompany ea1;
    private CfgCompany ea2;
    private CfgCompany ea3;
    private CfgCompany ei;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("ACT");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("act." + prefixe, fixture.role("Libre " + prefixe, false)));
        ea1 = patrimoine.entite("EA1-" + prefixe, true);
        ea2 = patrimoine.entite("EA2-" + prefixe, true);
        ea3 = patrimoine.entite("EA3-" + prefixe, true);
        ei = patrimoine.entite("EI-" + prefixe, false);

        UUID sh1 = rp.actionnaire(libelle("SH1"));
        UUID sh2 = rp.actionnaire(libelle("SH2"));
        rp.actionnaire(libelle("SH3"));
        UUID sh4 = rp.actionnaire(libelle("SH4"));
        rp.part(sh1, ea1, 10);
        rp.part(sh1, ea2, 5);
        rp.part(sh2, ea1, 3);
        rp.part(sh2, ei, 7);
        rp.part(sh4, ea2, null);
        fixture.relire();
    }

    private String libelle(String nom) {
        return nom + "-" + prefixe;
    }

    private MockHttpServletRequestBuilder requete() {
        return get(resultat(PATRIMOINE, ACTIONNAIRES)).with(libre);
    }

    private Map<String, Object> repartition() throws Exception {
        return section(appels.sections(requete()), REPARTITION);
    }

    private static Map<String, Object> ligneDe(Map<String, Object> repartition, String actionnaire) {
        List<Map<String, Object>> trouvees = lignesOu(repartition, ACTIONNAIRE, actionnaire);
        assertThat(trouvees).as("ligne de %s", actionnaire).hasSize(1);
        return trouvees.get(0);
    }

    private static String colonne(Map<String, Object> repartition, CfgCompany entite) {
        String nom = colonneDeLibelle(repartition, entite.getDescription());
        assertThat(nom).as("colonne de l'entité %s", entite.getDescription()).isNotNull();
        return nom;
    }

    // ------------------------------------------------------------------ T8

    @Test
    void t8_cellules_partsParActionnaireEtEntite() throws Exception {
        Map<String, Object> repartition = repartition();
        Map<String, Object> sh1 = ligneDe(repartition, libelle("SH1"));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(sh1.get(colonne(repartition, ea1)))).isEqualByComparingTo("10");
        soft.assertThat(nombre(sh1.get(colonne(repartition, ea2)))).isEqualByComparingTo("5");
        soft.assertThat(nombre(sh1.get(TOTAL))).isEqualByComparingTo("15");
        soft.assertAll();
    }

    @Test
    void t8_actionnaireSansPart_ligneAZero() throws Exception {
        Map<String, Object> sh3 = ligneDe(repartition(), libelle("SH3"));

        assertThat(nombre(sh3.get(TOTAL))).isEqualByComparingTo("0");
    }

    @Test
    void t8_partNulle_compteeZero() throws Exception {
        Map<String, Object> sh4 = ligneDe(repartition(), libelle("SH4"));

        assertThat(nombre(sh4.get(TOTAL))).isEqualByComparingTo("0");
    }

    @Test
    void t8_totalLigne_egalSommeDesColonnesEntite() throws Exception {
        Map<String, Object> repartition = repartition();
        String c1 = colonne(repartition, ea1);
        String c2 = colonne(repartition, ea2);

        SoftAssertions soft = new SoftAssertions();
        for (String nom : List.of("SH1", "SH2", "SH3")) {
            Map<String, Object> ligne = ligneDe(repartition, libelle(nom));
            soft.assertThat(nombre(ligne.get(TOTAL))).as(nom)
                    .isEqualByComparingTo(zeroSiNul(ligne.get(c1)).add(zeroSiNul(ligne.get(c2))));
        }
        soft.assertAll();
    }

    private static BigDecimal zeroSiNul(Object valeur) {
        return valeur == null ? BigDecimal.ZERO : nombre(valeur);
    }

    @Test
    void t8_totauxDeColonne_sommeDesParts() throws Exception {
        Map<String, Object> repartition = repartition();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux(repartition).get(colonne(repartition, ea1)))).isEqualByComparingTo("13");
        soft.assertThat(nombre(totaux(repartition).get(colonne(repartition, ea2)))).isEqualByComparingTo("5");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ T3 / Q7 : entités actives seulement

    @Test
    void t3_entiteInactive_niColonne_niDansLeTotal() throws Exception {
        Map<String, Object> repartition = repartition();
        Map<String, Object> sh2 = ligneDe(repartition, libelle("SH2"));

        assertThat(libellesColonnes(repartition)).doesNotContain(ei.getDescription());
        assertThat(nombre(sh2.get(TOTAL))).isEqualByComparingTo("3");
    }

    @Test
    void t8_entiteActiveSansPart_pasDeColonne() throws Exception {
        assertThat(libellesColonnes(repartition())).doesNotContain(ea3.getDescription())
                .contains(ea1.getDescription(), ea2.getDescription());
    }

    // ------------------------------------------------------------------ paramètres

    @Test
    void parametres_aucun() throws Exception {
        mockMvc.perform(get(descripteur(PATRIMOINE, ACTIONNAIRES)).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres.length()").value(0));
    }

    @Test
    void parametres_trancheIdRefuse_400() throws Exception {
        assertThat(appels.statut(requete().param(TRANCHE_ID, UUID.randomUUID().toString()))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ T9 : export

    @Test
    void t9_export_uneFeuilleParSection_ligneTotal() throws Exception {
        RapportsPatrimoineSupport.verifierExport(appels, requete(),
                get(export(PATRIMOINE, ACTIONNAIRES)).param(FORMAT, XLSX).with(libre));
    }
}
