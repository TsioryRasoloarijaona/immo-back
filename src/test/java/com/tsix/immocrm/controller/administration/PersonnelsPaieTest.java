package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.creer;
import static com.tsix.immocrm.fixtures.AppelsTiers.idDe;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePersonnels.COLONNES_PAIE;
import static com.tsix.immocrm.fixtures.FixturePersonnels.assertSansPaie;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixturePersonnels.transformer;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Paie absente du fil et jamais écrite — FEATURE-admin-personnels.md, règles 2 et 4, section Back. Import : {@link PersonnelsImportTest}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonnelsPaieTest {

    private static final String EMP = PERSONNELS.famille();

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;
    @Autowired private FixturePersonnels personnels;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private String reponse(MockHttpServletRequestBuilder requete, ResultMatcher attendu) throws Exception {
        return mockMvc.perform(requete).andExpect(attendu).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /** Corps valide qui tente d'écrire la paie ; les valeurs sont celles que {@code assertSansPaie} traque. */
    private Map<String, Object> corpsAvecPaie() {
        Map<String, Object> corps = corpsPhysique(j, "Amal", "Benali " + j.jeton());
        corps.putAll(Map.of("salaireBase", FixturePersonnels.SALAIRE, "numeroSecuriteSociale", FixturePersonnels.SECU,
                "dateEmbauche", "2019-03-14", "dateSortie", "2025-06-30", "admSalaryId", j.parent("AdmSalary"),
                "cfgCompanyId", j.parent("CfgCompany")));
        return corps;
    }

    // ------------------------------------------------------------------ règle 2 : réponses de succès

    @Test
    void r2_liste_sansPaie() throws Exception {
        employe(j, "E1").inserer();

        assertSansPaie(reponse(lister(EMP, connecte(j.admin())).param("recherche", j.jeton()), status().isOk()));
    }

    @Test
    void r2_fiche_sansPaie() throws Exception {
        UUID id = employe(j, "E2").inserer();

        assertSansPaie(reponse(lire(EMP, id, connecte(j.admin())), status().isOk()));
    }

    @Test
    void r2_creation_sansPaie_etRienEcrit() throws Exception {
        String fiche = reponse(creer(EMP, corpsAvecPaie(), connecte(j.admin())), status().isCreated());

        assertSansPaie(fiche);
        assertThat(personnels.colonnes(idDe(fiche))).containsEntry("SalaryBase", null)
                .containsEntry("SocialSecurityNumber", null).containsEntry("HireDate", null).containsEntry("FireDate", null)
                .containsEntry("AdmSalaryId", null);
    }

    @Test
    void r2_modification_sansPaie() throws Exception {
        UUID id = employe(j, "E3").inserer();

        assertSansPaie(reponse(modifier(EMP, id, corpsPhysique(j, "Amal", "Benali"), connecte(j.admin())), status().isOk()));
    }

    @Test
    void r2_transformation_sansPaie() throws Exception {
        UUID id = employe(j, "E4").inserer();

        assertSansPaie(reponse(transformer(id, connecte(j.admin())), status().isOk()));
    }

    // ------------------------------------------------------------------ règle 2 : réponses d'erreur

    @Test
    void r2_erreur400_sansPaie() throws Exception {
        UUID id = employe(j, "E5").inserer();
        Map<String, Object> corps = corpsAvecPaie();
        corps.put("mobile", null);

        assertSansPaie(reponse(modifier(EMP, id, corps, connecte(j.admin())), status().isBadRequest()));
    }

    @Test
    void r2_erreur409_suppression_sansPaie() throws Exception {
        fixture.poserClesEtrangeres();
        personnels.poserCleCompte();
        UUID id = employe(j, "E6").inserer();
        personnels.compteLie(j, id);

        assertSansPaie(reponse(supprimer(EMP, id, connecte(j.admin())), status().isConflict()));
        fixture.oublierEcritureRefusee();
    }

    @Test
    void r2_erreur409_transformation_sansPaie() throws Exception {
        UUID id = employe(j, "E7").inserer();
        personnels.compteLie(j, id);

        assertSansPaie(reponse(transformer(id, connecte(j.admin())), status().isConflict()));
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_modification_paieGrhEmbaucheSortieEntite_inchangees() throws Exception {
        UUID id = personnels.paieComplete(j, "E8").inserer();
        Map<String, Object> avant = personnels.colonnes(id);

        reponse(modifier(EMP, id, corpsPhysique(j, "Amal", "Benali"), connecte(j.admin())), status().isOk());

        Map<String, Object> apres = personnels.colonnes(id);
        assertThat(COLONNES_PAIE).hasSize(14).allSatisfy(c -> assertThat(apres.get(c)).as(c).isNotNull().isEqualTo(avant.get(c)));
    }

    @Test
    void r4_modification_champsDePaieEnvoyes_ignores() throws Exception {
        UUID id = personnels.paieComplete(j, "E9").inserer();
        Map<String, Object> avant = personnels.colonnes(id);
        Map<String, Object> corps = corpsAvecPaie();
        corps.put("salaireBase", 1);
        corps.put("numeroSecuriteSociale", "AUTRE");

        reponse(modifier(EMP, id, corps, connecte(j.admin())), status().isOk());

        Map<String, Object> apres = personnels.colonnes(id);
        assertThat(COLONNES_PAIE).hasSize(14).allSatisfy(c -> assertThat(apres.get(c)).as(c).isEqualTo(avant.get(c)));
    }
}
