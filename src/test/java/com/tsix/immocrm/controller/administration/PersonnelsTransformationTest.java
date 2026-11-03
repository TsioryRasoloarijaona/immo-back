package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixturePersonnels.EtatDossier;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.ids;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.lister;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePersonnels.COLONNES_PAIE;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixturePersonnels.transformer;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Transformer en contact » — FEATURE-admin-personnels.md, règles 6 à 8 et décisions de recalage : réussite en 200, seul
 * {@code ComFolder.CfgCommercialId} d'un dossier non sorti bloque.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonnelsTransformationTest {

    private static final String ID_TYPE = "CfgTierTypeId";
    private static final String ID_NATURE = "CfgTierKindId";
    private static final List<String> COLONNES_ECRITES = List.of(ID_TYPE, ID_NATURE, "UserUpdate", "UserUpdateDateTime");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;
    @Autowired private FixturePersonnels personnels;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    private String accepte(UUID id) throws Exception {
        return mockMvc.perform(transformer(id, connecte(j.admin()))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void refus409(UUID id) throws Exception {
        Map<String, Object> avant = personnels.colonnes(id);

        String refus = mockMvc.perform(transformer(id, connecte(j.admin()))).andExpect(status().isConflict()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        fixture.oublierEcritureRefusee();

        String detail = JsonPath.read(refus, "$.detail");
        assertAll(
                () -> assertThat(detail).isNotBlank().doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("sql"),
                () -> assertThat(personnels.colonnes(id)).isEqualTo(avant));
    }

    private List<String> listeDuJeton(String famille) throws Exception {
        return ids(mockMvc.perform(lister(famille, connecte(j.admin())).param("recherche", j.jeton()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> sansColonnesEcrites(Map<String, Object> ligne) {
        Map<String, Object> reste = new HashMap<>(ligne);
        COLONNES_ECRITES.forEach(reste::remove);
        return reste;
    }

    // ------------------------------------------------------------------ règle 6

    @Test
    void r6_typePasseDeEmpACnt_natureRemiseANull() throws Exception {
        UUID id = employe(j, "E1").avec(ID_NATURE, j.nature(PERSONNELS)).inserer();

        accepte(id);

        assertThat(personnels.colonnes(id).get(ID_NATURE)).isNull();
        assertThat(fixture.ligne(id)).containsEntry(ID_TYPE, j.typeId(CONTACTS).toString().toLowerCase());
    }

    @Test
    void r6_resteInchange_paieComprise() throws Exception {
        UUID id = personnels.paieComplete(j, "E2").avec(ID_NATURE, j.nature(PERSONNELS)).inserer();
        Map<String, Object> avant = personnels.colonnes(id);

        accepte(id);

        Map<String, Object> apres = personnels.colonnes(id);
        assertAll(
                () -> assertThat(sansColonnesEcrites(apres)).isEqualTo(sansColonnesEcrites(avant)),
                () -> assertThat(COLONNES_PAIE).hasSize(14).allSatisfy(c -> assertThat(apres.get(c)).as(c).isNotNull()));
    }

    @Test
    void r6_rendLaFiche() throws Exception {
        UUID id = employe(j, "E3").inserer();

        String fiche = accepte(id);

        Map<String, Object> champs = JsonPath.read(fiche, "$");
        assertThat(champs).containsEntry("id", id.toString()).containsEntry("code", j.code("E3"));
    }

    @Test
    void r6_quittePersonnels_entreDansContacts() throws Exception {
        UUID id = employe(j, "E4").inserer();
        assertThat(listeDuJeton(PERSONNELS.famille())).containsExactly(id.toString());

        accepte(id);

        assertAll(
                () -> assertThat(listeDuJeton(PERSONNELS.famille())).isEmpty(),
                () -> assertThat(listeDuJeton(CONTACTS.famille())).containsExactly(id.toString()));
    }

    @Test
    void r6_ficheLueDansContacts_plusDansPersonnels() throws Exception {
        UUID id = employe(j, "E5").inserer();

        accepte(id);

        mockMvc.perform(lire(CONTACTS.famille(), id, connecte(j.admin()))).andExpect(status().isOk());
        mockMvc.perform(lire(PERSONNELS.famille(), id, connecte(j.admin()))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 7

    @Test
    void r7_compteLie_409_messageMetier_rienModifie() throws Exception {
        UUID id = employe(j, "E6").inserer();
        personnels.compteLie(j, id);

        refus409(id);
    }

    @Test
    void r7_commercialDUnDossierActif_409_messageMetier_rienModifie() throws Exception {
        UUID id = employe(j, "E7").inserer();
        personnels.dossierDuCommercial(j, id, EtatDossier.ACTIF);

        refus409(id);
    }

    @Test
    void r7_commercialDUnDossierSansStatut_409_rienModifie() throws Exception {
        UUID id = employe(j, "E12").inserer();
        personnels.dossierDuCommercial(j, id, EtatDossier.SANS_STATUT);

        refus409(id);
    }

    @Test
    void r7_temoin_commercialDUnDossierSorti_transformationAcceptee() throws Exception {
        UUID id = employe(j, "E8").inserer();
        personnels.dossierDuCommercial(j, id, EtatDossier.SORTI);

        accepte(id);

        assertThat(fixture.ligne(id)).containsEntry(ID_TYPE, j.typeId(CONTACTS).toString().toLowerCase());
    }

    @Test
    void r7_commercialDUnClientSeul_transformationAcceptee() throws Exception {
        UUID id = employe(j, "E13").inserer();
        j.referencer(id);

        accepte(id);

        assertThat(fixture.ligne(id)).containsEntry(ID_TYPE, j.typeId(CONTACTS).toString().toLowerCase());
    }

    // ------------------------------------------------------------------ règle 8

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS", "BANQUES", "ORGANISMES"})
    void r8_idDUnAutreType_404_rienModifie(TypeTiers type) throws Exception {
        // Témoin : la route existe pour un employé.
        accepte(employe(j, "E9").inserer());
        UUID autre = j.tiers(type, "A1").inserer();
        Map<String, Object> avant = personnels.colonnes(autre);

        mockMvc.perform(transformer(autre, connecte(j.admin()))).andExpect(status().isNotFound());

        assertThat(personnels.colonnes(autre)).isEqualTo(avant);
    }

    @Test
    void r8_idInconnu_404() throws Exception {
        accepte(employe(j, "E10").inserer());

        mockMvc.perform(transformer(UUID.randomUUID(), connecte(j.admin()))).andExpect(status().isNotFound());
    }

    @Test
    void r8_ligneSansType_404() throws Exception {
        accepte(employe(j, "E11").inserer());
        UUID sansType = j.tiersSansType("N1");

        mockMvc.perform(transformer(sansType, connecte(j.admin()))).andExpect(status().isNotFound());
    }
}
