package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuration Projet, parité OMRAN — FEATURE-projets-config-parite.md §1 (4 dates), §2 (nivellement, projet référence,
 * lot gestionnaire du compte prorata), §3 (ventilation par tranches : lecture et suppression seulement, règles d'écriture
 * en attente du cartographe). Transitions de statut (§5) : {@link ProjetConfigTransitionsTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetConfigPariteTest {

    private static final LocalDate LANCEMENT_PREVU = LocalDate.of(2026, 3, 1);
    private static final LocalDate ACHEVEMENT_PREVU = LocalDate.of(2027, 6, 30);
    private static final LocalDate LANCEMENT_REEL = LocalDate.of(2026, 4, 15);
    private static final LocalDate ACHEVEMENT_REEL = LocalDate.of(2027, 9, 10);
    private static final String COMPTE_DATES = """
            SELECT COUNT(*) FROM PrjProject WHERE PKey = :id
              AND CONVERT(date, ProvisionalWorkStartDate) = :lp AND CONVERT(date, ProvisionalCompletingWorkDate) = :ap
              AND CONVERT(date, RealWorkStartDate) = :lr AND CONVERT(date, RealCompletingWorkDate) = :ar""";
    private static final String COMPTE_LIENS = """
            SELECT COUNT(*) FROM PrjProject WHERE PKey = :id
              AND StkLevelingId = :nivellement AND ReferenceId = :reference AND ManageProrataPrjMarketId = :lot""";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID societe;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
    }

    // ------------------------------------------------------------------ appels

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(requete.with(qui));
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private CorpsJson corpsProjet(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Projet " + code).avec(SOCIETE_ID, societe);
    }

    private CorpsJson avecDates(CorpsJson corps) {
        return corps.avec(DATE_LANCEMENT_PREVISIONNELLE, LANCEMENT_PREVU.atStartOfDay())
                .avec(DATE_ACHEVEMENT_PREVISIONNELLE, ACHEVEMENT_PREVU.atStartOfDay())
                .avec(DATE_LANCEMENT_REELLE, LANCEMENT_REEL.atStartOfDay())
                .avec(DATE_ACHEVEMENT_REELLE, ACHEVEMENT_REEL.atStartOfDay());
    }

    private int compterDates(UUID projet) {
        return fixture.compter(COMPTE_DATES, Map.of("id", projet, "lp", LANCEMENT_PREVU, "ap", ACHEVEMENT_PREVU,
                "lr", LANCEMENT_REEL, "ar", ACHEVEMENT_REEL));
    }

    private static void datesAttendues(ResultActions reponse) throws Exception {
        reponse.andExpect(jsonPath("$." + DATE_LANCEMENT_PREVISIONNELLE, startsWith(LANCEMENT_PREVU.toString())))
                .andExpect(jsonPath("$." + DATE_ACHEVEMENT_PREVISIONNELLE, startsWith(ACHEVEMENT_PREVU.toString())))
                .andExpect(jsonPath("$." + DATE_LANCEMENT_REELLE, startsWith(LANCEMENT_REEL.toString())))
                .andExpect(jsonPath("$." + DATE_ACHEVEMENT_REELLE, startsWith(ACHEVEMENT_REEL.toString())));
    }

    // ------------------------------------------------------------------ §1 : les 4 dates

    @Test
    void s1_quatreDates_luesDansLaFiche() throws Exception {
        UUID projet = fixture.projet("DL-" + jeton, societe);
        fixture.dates(projet, LANCEMENT_PREVU, ACHEVEMENT_PREVU, LANCEMENT_REEL, ACHEVEMENT_REEL);

        datesAttendues(appeler(get(PROJET, projet), connecte(admin)).andExpect(status().isOk()));
    }

    @Test
    void s1_quatreDates_ecritesParModification_200_puisEnBase() throws Exception {
        UUID projet = fixture.projet("DM-" + jeton, societe);

        datesAttendues(ecrire(put(PROJET, projet), avecDates(corpsProjet("DM-" + jeton)), connecte(admin))
                .andExpect(status().isOk()));

        assertThat(compterDates(projet)).isEqualTo(1);
    }

    @Test
    void s1_quatreDates_ecritesALaCreation_201_puisEnBase() throws Exception {
        String json = ecrire(post(PROJETS), avecDates(corpsProjet("DC-" + jeton)), connecte(admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID projet = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(compterDates(projet)).isEqualTo(1);
    }

    @Test
    void s1_modificationAvecDates_nonAdministrateur_403_datesInchangees() throws Exception {
        UUID projet = fixture.projet("DR-" + jeton, societe);

        ecrire(put(PROJET, projet), avecDates(corpsProjet("DR-" + jeton)), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND ProvisionalWorkStartDate IS NULL"
                + " AND ProvisionalCompletingWorkDate IS NULL AND RealWorkStartDate IS NULL AND RealCompletingWorkDate IS NULL",
                projet)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ §2 : nivellement, référence, lot prorata

    @Test
    void s2_liens_lusDansLaFiche() throws Exception {
        UUID projet = fixture.projet("LL-" + jeton, societe);
        UUID nivellement = fixture.nivellement("NIV-" + jeton);
        UUID reference = fixture.projet("LR-" + jeton, societe);
        UUID lot = fixture.lotDuProjet("LOT-" + jeton, projet);
        fixture.liens(projet, nivellement, reference, lot);

        appeler(get(PROJET, projet), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + NIVELLEMENT_ID).value(nivellement.toString()))
                .andExpect(jsonPath("$." + PROJET_REFERENCE_ID).value(reference.toString()))
                .andExpect(jsonPath("$." + LOT_GESTIONNAIRE_PRORATA_ID).value(lot.toString()));
    }

    @Test
    void s2_nivellement_ecritParModification_200_puisEnBase() throws Exception {
        UUID projet = fixture.projet("LN-" + jeton, societe);
        UUID nivellement = fixture.nivellement("NIV-" + jeton);

        ecrire(put(PROJET, projet), corpsProjet("LN-" + jeton).avec(NIVELLEMENT_ID, nivellement), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + NIVELLEMENT_ID).value(nivellement.toString()));

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND StkLevelingId = :n",
                Map.of("id", projet, "n", nivellement))).isEqualTo(1);
    }

    @Test
    void s2_projetReference_ecritParModification_200_puisEnBase() throws Exception {
        UUID projet = fixture.projet("LP-" + jeton, societe);
        UUID reference = fixture.projet("LPR-" + jeton, societe);

        ecrire(put(PROJET, projet), corpsProjet("LP-" + jeton).avec(PROJET_REFERENCE_ID, reference), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + PROJET_REFERENCE_ID).value(reference.toString()));

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND ReferenceId = :r",
                Map.of("id", projet, "r", reference))).isEqualTo(1);
    }

    @Test
    void s2_lotGestionnaireProrata_lotDuProjet_ecritParModification_200_puisEnBase() throws Exception {
        UUID projet = fixture.projet("LG-" + jeton, societe);
        UUID lot = fixture.lotDuProjet("LOT-" + jeton, projet);

        ecrire(put(PROJET, projet), corpsProjet("LG-" + jeton).avec(LOT_GESTIONNAIRE_PRORATA_ID, lot), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + LOT_GESTIONNAIRE_PRORATA_ID).value(lot.toString()));

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND ManageProrataPrjMarketId = :l",
                Map.of("id", projet, "l", lot))).isEqualTo(1);
    }

    @Test
    void s2_troisLiens_ecritsEnUneModification_relusParLaFiche() throws Exception {
        UUID projet = fixture.projet("LT-" + jeton, societe);
        UUID nivellement = fixture.nivellement("NIV-" + jeton);
        UUID reference = fixture.projet("LTR-" + jeton, societe);
        UUID lot = fixture.lotDuProjet("LOT-" + jeton, projet);

        ecrire(put(PROJET, projet), corpsProjet("LT-" + jeton).avec(NIVELLEMENT_ID, nivellement)
                .avec(PROJET_REFERENCE_ID, reference).avec(LOT_GESTIONNAIRE_PRORATA_ID, lot), connecte(admin))
                .andExpect(status().isOk());

        assertThat(fixture.compter(COMPTE_LIENS, Map.of("id", projet, "nivellement", nivellement,
                "reference", reference, "lot", lot))).isEqualTo(1);
        appeler(get(PROJET, projet), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + NIVELLEMENT_ID).value(nivellement.toString()))
                .andExpect(jsonPath("$." + PROJET_REFERENCE_ID).value(reference.toString()))
                .andExpect(jsonPath("$." + LOT_GESTIONNAIRE_PRORATA_ID).value(lot.toString()));
    }

    @Test
    void s2_modificationAvecLiens_nonAdministrateur_403_liensInchanges() throws Exception {
        UUID projet = fixture.projet("LX-" + jeton, societe);
        UUID nivellement = fixture.nivellement("NIV-" + jeton);

        ecrire(put(PROJET, projet), corpsProjet("LX-" + jeton).avec(NIVELLEMENT_ID, nivellement), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND StkLevelingId IS NULL", projet))
                .isEqualTo(1);
    }

    // ------------------------------------------------------------------ §2, contrat validé : refus, PUT partiel, référentiels

    private int liensVides(UUID projet) {
        return fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND StkLevelingId IS NULL"
                + " AND ReferenceId IS NULL AND ManageProrataPrjMarketId IS NULL", projet);
    }

    @Test
    void s2_nivellementInconnu_400_rienEcrit() throws Exception {
        UUID projet = fixture.projet("RN-" + jeton, societe);

        ecrire(put(PROJET, projet), corpsProjet("RN-" + jeton).avec(NIVELLEMENT_ID, UUID.randomUUID()), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(liensVides(projet)).isEqualTo(1);
    }

    @Test
    void s2_projetReferenceInconnu_400_rienEcrit() throws Exception {
        UUID projet = fixture.projet("RP-" + jeton, societe);

        ecrire(put(PROJET, projet), corpsProjet("RP-" + jeton).avec(PROJET_REFERENCE_ID, UUID.randomUUID()), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(liensVides(projet)).isEqualTo(1);
    }

    @Test
    void s2_lotInconnu_400_rienEcrit() throws Exception {
        UUID projet = fixture.projet("RL-" + jeton, societe);

        ecrire(put(PROJET, projet), corpsProjet("RL-" + jeton).avec(LOT_GESTIONNAIRE_PRORATA_ID, UUID.randomUUID()),
                connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(liensVides(projet)).isEqualTo(1);
    }

    @Test
    void s2_lotDUnAutreProjet_400_rienEcrit() throws Exception {
        UUID projet = fixture.projet("RA-" + jeton, societe);
        UUID autre = fixture.projet("RAB-" + jeton, societe);
        UUID lotAutre = fixture.lotDuProjet("LOT-" + jeton, autre);

        ecrire(put(PROJET, projet), corpsProjet("RA-" + jeton).avec(LOT_GESTIONNAIRE_PRORATA_ID, lotAutre), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(liensVides(projet)).isEqualTo(1);
    }

    @Test
    void s2_projetReferenceLuiMeme_400_rienEcrit() throws Exception {
        UUID projet = fixture.projet("RS-" + jeton, societe);

        ecrire(put(PROJET, projet), corpsProjet("RS-" + jeton).avec(PROJET_REFERENCE_ID, projet), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(liensVides(projet)).isEqualTo(1);
    }

    @Test
    void s2_putSansLesNouveauxChamps_liensEtDatesInchanges() throws Exception {
        UUID projet = fixture.projet("PI-" + jeton, societe);
        UUID nivellement = fixture.nivellement("NIV-" + jeton);
        UUID reference = fixture.projet("PIR-" + jeton, societe);
        UUID lot = fixture.lotDuProjet("LOT-" + jeton, projet);
        fixture.liens(projet, nivellement, reference, lot);
        fixture.dates(projet, LANCEMENT_PREVU, ACHEVEMENT_PREVU, LANCEMENT_REEL, ACHEVEMENT_REEL);

        ecrire(put(PROJET, projet), corpsProjet("PI-" + jeton).avec(DESCRIPTION, "Renomme " + jeton), connecte(admin))
                .andExpect(status().isOk());

        assertThat(new int[]{
                fixture.compter(COMPTE_LIENS, Map.of("id", projet, "nivellement", nivellement, "reference", reference,
                        "lot", lot)),
                compterDates(projet),
                fixture.compter("SELECT COUNT(*) FROM PrjProject WHERE PKey = :id AND Description = N'Renomme " + jeton + "'",
                        projet)})
                .containsExactly(1, 1, 1);
    }

    @Test
    void s2_referentiels_listentLesNivellements() throws Exception {
        UUID nivellement = fixture.nivellement("NIV-" + jeton);

        appeler(get(REFERENTIELS), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + NIVELLEMENTS + "[*].id", hasItem(nivellement.toString())));
    }

    // ------------------------------------------------------------------ §3 : ventilation par tranches (forme seule)

    @Test
    void s3_ventilations_listeDuProjet_trancheEtTaux() throws Exception {
        UUID projet = fixture.projet("VL-" + jeton, societe);
        UUID tranche = fixture.trancheDuProjet("TR-" + jeton, projet);
        UUID ligne = fixture.ventilation(projet, tranche, new BigDecimal("0.400000"));
        String filtre = "$[?(@.id == '" + ligne + "')]";

        String json = appeler(get(VENTILATIONS, projet), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre + ".trancheId", hasItem(tranche.toString())))
                .andReturn().getResponse().getContentAsString();

        List<Object> taux = JsonPath.read(json, filtre + ".taux");
        assertThat(taux).singleElement()
                .satisfies(t -> assertThat(new BigDecimal(t.toString())).isEqualByComparingTo("0.4"));
    }

    @Test
    void s3_ventilations_neListentPasCellesDUnAutreProjet() throws Exception {
        UUID projet = fixture.projet("VA-" + jeton, societe);
        UUID autre = fixture.projet("VB-" + jeton, societe);
        UUID tranche = fixture.trancheDuProjet("TR-" + jeton, autre);
        fixture.ventilation(autre, tranche, BigDecimal.ONE);

        appeler(get(VENTILATIONS, projet), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void s3_suppressionVentilation_204_ligneSupprimee() throws Exception {
        UUID projet = fixture.projet("VS-" + jeton, societe);
        UUID tranche = fixture.trancheDuProjet("TR-" + jeton, projet);
        UUID ligne = fixture.ventilation(projet, tranche, BigDecimal.ONE);

        appeler(delete(VENTILATION, projet, ligne), connecte(admin)).andExpect(status().isNoContent());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PKey = :id", ligne))
                .isZero();
    }

    @Test
    void s3_ventilations_nonAdministrateur_403_ligneIntacte() throws Exception {
        UUID projet = fixture.projet("VN-" + jeton, societe);
        UUID tranche = fixture.trancheDuProjet("TR-" + jeton, projet);
        UUID ligne = fixture.ventilation(projet, tranche, BigDecimal.ONE);

        appeler(get(VENTILATIONS, projet), enCommercial(commercial)).andExpect(status().isForbidden());
        appeler(delete(VENTILATION, projet, ligne), enCommercial(commercial)).andExpect(status().isForbidden());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PKey = :id", ligne))
                .isEqualTo(1);
    }
}
