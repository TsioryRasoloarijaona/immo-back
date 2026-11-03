package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.creer;
import static com.tsix.immocrm.fixtures.AppelsTiers.detailNormalise;
import static com.tsix.immocrm.fixtures.AppelsTiers.idDe;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.AppelsTiers.supprimer;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Création, modification, suppression — FEATURE-admin-personnels.md, règles 5 et 9 ; règles 5 à 10 du socle. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonnelsEcritureTest {

    private static final String EMP = PERSONNELS.famille();
    private static final String RAISON = "SocialReason";
    private static final String DESCRIPTION = "Description";
    private static final String CODE = "Code";
    private static final String ID_TYPE = "CfgTierTypeId";
    private static final String UTILISATEUR_CREATION = "UserCreate";
    private static final String FORME_ID = "formeSocialeId";
    private static final String DEVISE_ID = "deviseId";
    private static final int COMPTEUR = 880_000;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;
    @Autowired private FixturePersonnels personnels;

    private Jeu j;
    private String nom;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        nom = "Benali " + j.jeton();
    }

    private Map<String, Object> corps() {
        return corpsPhysique(j, "Amal", nom);
    }

    private UUID cree(Map<String, Object> corps) throws Exception {
        return idDe(mockMvc.perform(creer(EMP, corps, connecte(j.admin()))).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String refusCreation(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(creer(EMP, corps, connecte(j.admin()))).andExpect(status().isBadRequest()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void refusModification(UUID id, Map<String, Object> corps) throws Exception {
        mockMvc.perform(modifier(EMP, id, corps, connecte(j.admin()))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ règle 5 : personne physique obligatoire

    @Test
    void r5_personneMorale_400_enCreation_rienCree() throws Exception {
        String raison = "Societe " + j.jeton();

        refusCreation(corpsMorale(j, raison));

        assertThat(fixture.parRaisonSociale(raison)).isEmpty();
    }

    @Test
    void r5_personneMorale_400_enModification_ligneInchangee() throws Exception {
        UUID id = employe(j, "E1").inserer();
        Map<String, Object> avant = personnels.colonnes(id);

        refusModification(id, corpsMorale(j, "Societe " + j.jeton()));
        fixture.oublierEcritureRefusee();

        assertThat(personnels.colonnes(id)).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ règles 5 à 10 du socle

    @Test
    void socle5_creation_typeEmp_codeDuCompteur_indicateursAFalse_auteurPose() throws Exception {
        fixture.fixerCompteurTiers(COMPTEUR);
        Map<String, Object> corps = corps();
        corps.put("code", "PIRATE");

        UUID id = cree(corps);

        Map<String, Object> ligne = fixture.ligne(id);
        assertAll(
                () -> assertThat((String) ligne.get(CODE)).matches("\\d+/0*" + (COMPTEUR + 1)),
                () -> assertThat(ligne).containsEntry(ID_TYPE, j.typeId(PERSONNELS).toString().toLowerCase())
                        .containsEntry("IsProspect", 0).containsEntry("IsOldCustomer", 0).containsEntry("IsRntCustomer", 0)
                        .containsEntry("IsVisibleInList", 0).containsEntry("IsFundReader", 0).containsEntry("IsExtern", 0)
                        .containsEntry(UTILISATEUR_CREATION, j.admin().getDescription()));
    }

    @ParameterizedTest
    @CsvSource({"prenom, prenom", "nom, nom", "titreId, titre", "sexeId, sexe"})
    void socle6_champManquant_400_champNomme(String champ, String nomme) throws Exception {
        Map<String, Object> corps = corps();
        corps.remove(champ);

        assertThat(detailNormalise(refusCreation(corps))).contains(nomme);
    }

    @Test
    void socle6_socle7_raisonEtDescription_prenomNom_aChaqueEcriture() throws Exception {
        UUID id = cree(corps());
        assertThat(fixture.ligne(id)).containsEntry(RAISON, "Amal " + nom).containsEntry(DESCRIPTION, "Amal " + nom);

        mockMvc.perform(modifier(EMP, id, corpsPhysique(j, "Salma", nom), connecte(j.admin()))).andExpect(status().isOk());

        assertThat(fixture.ligne(id)).containsEntry(RAISON, "Salma " + nom).containsEntry(DESCRIPTION, "Salma " + nom);
    }

    @Test
    void socle8_mobileEtEmailVides_400_creationEtModification() throws Exception {
        UUID id = employe(j, "E2").inserer();
        Map<String, Object> corps = corps();
        corps.put("mobile", "  ");
        corps.put("email", "");

        refusCreation(corps);
        refusModification(id, corps);

        assertThat(fixture.parRaisonSociale("Amal " + nom)).isEmpty();
    }

    @Test
    void socle9_paysResidenceAbsent_400() throws Exception {
        Map<String, Object> corps = corps();
        corps.remove("paysResidenceId");

        refusCreation(corps);
    }

    @ParameterizedTest
    @ValueSource(strings = {"titreId", "paysResidenceId", "nationaliteId", "professionId", "banqueId", "typePieceId",
            "formeSocialeId", "deviseId"})
    void socle9_identifiantInconnu_400(String champ) throws Exception {
        Map<String, Object> corps = corps();
        corps.put(champ, UUID.randomUUID());

        refusCreation(corps);
    }

    // ------------------------------------------------------------------ recalage Q1 : forme sociale et devise facultatives

    @ParameterizedTest
    @ValueSource(strings = {"formeSocialeId", "deviseId"})
    void q1_formeOuDeviseAbsente_acceptee_enCreation(String champ) throws Exception {
        Map<String, Object> corps = corps();
        corps.remove(champ);

        assertThat(fixture.existe(cree(corps))).isTrue();
    }

    @Test
    void q1_formeEtDeviseAbsentes_acceptees_enCreation() throws Exception {
        Map<String, Object> corps = corps();
        corps.remove(FORME_ID);
        corps.remove(DEVISE_ID);

        UUID id = cree(corps);

        assertThat(fixture.ligne(id)).containsEntry("AdmSocialFormId", null).containsEntry("StlCurrencyId", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"formeSocialeId", "deviseId"})
    void q1_formeOuDeviseAbsente_acceptee_enModification(String champ) throws Exception {
        UUID id = employe(j, "E7").inserer();
        Map<String, Object> corps = corps();
        corps.remove(champ);

        mockMvc.perform(modifier(EMP, id, corps, connecte(j.admin()))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"formeSocialeId", "deviseId"})
    void q1_identifiantInconnuFourni_400_enModification(String champ) throws Exception {
        UUID id = employe(j, "E8").inserer();
        Map<String, Object> corps = corps();
        corps.put(champ, UUID.randomUUID());

        refusModification(id, corps);
    }

    @Test
    void socle9_natureDUnAutreType_400() throws Exception {
        Map<String, Object> corps = corps();
        corps.put("natureId", j.nature(CONTACTS));

        refusCreation(corps);
    }

    @Test
    void socle9_natureEmploye_acceptee() throws Exception {
        UUID nature = j.nature(PERSONNELS);
        Map<String, Object> corps = corps();
        corps.put("natureId", nature);

        UUID id = cree(corps);

        assertThat(fixture.ligne(id)).containsEntry("CfgTierKindId", nature.toString());
    }

    @Test
    void socle10_modification_figeCodeTypeAuteur_poseAuteurEtDate() throws Exception {
        UUID id = employe(j, "E3").avec(UTILISATEUR_CREATION, "Auteur initial").inserer();
        Map<String, Object> corps = corps();
        corps.put("code", "PIRATE");

        mockMvc.perform(modifier(EMP, id, corps, connecte(j.admin()))).andExpect(status().isOk());

        assertThat(fixture.ligne(id)).containsEntry(CODE, j.code("E3"))
                .containsEntry(ID_TYPE, j.typeId(PERSONNELS).toString().toLowerCase())
                .containsEntry(UTILISATEUR_CREATION, "Auteur initial")
                .containsEntry("UserUpdate", j.admin().getDescription())
                .extractingByKey("UserUpdateDateTime").isNotNull();
    }

    @Test
    void fiche_champsPersonnels_relus() throws Exception {
        Map<String, Object> corps = corps();
        corps.putAll(Map.of("numeroPiece", "CIN1", "rib", "RIB1", "iban", "IBAN1", "dateNaissance", "1990-02-03",
                "lieuNaissance", "Rabat"));

        String fiche = mockMvc.perform(creer(EMP, corps, connecte(j.admin()))).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> champs = JsonPath.read(fiche, "$");
        assertThat(champs).containsEntry("numeroPiece", "CIN1").containsEntry("rib", "RIB1").containsEntry("iban", "IBAN1")
                .containsEntry("lieuNaissance", "Rabat").containsEntry("personneMorale", false);
    }

    // ------------------------------------------------------------------ règle 9 : suppression

    @Test
    void r9_employeLieAUnCompte_409_messageMetier_rienModifie() throws Exception {
        fixture.poserClesEtrangeres();
        personnels.poserCleCompte();
        UUID id = employe(j, "E4").inserer();
        personnels.compteLie(j, id);
        Map<String, Object> avant = personnels.colonnes(id);

        String refus = mockMvc.perform(supprimer(EMP, id, connecte(j.admin()))).andExpect(status().isConflict()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        fixture.oublierEcritureRefusee();

        String detail = JsonPath.read(refus, "$.detail");
        assertAll(
                () -> assertThat(detail).isNotBlank().doesNotContainIgnoringCase("constraint").doesNotContainIgnoringCase("sql"),
                () -> assertThat(personnels.colonnes(id)).isEqualTo(avant),
                () -> assertThat(personnels.comptesLies(id)).isEqualTo(1));
    }

    @Test
    void r9_temoin_employeLibre_204() throws Exception {
        fixture.poserClesEtrangeres();
        personnels.poserCleCompte();
        UUID id = employe(j, "E5").inserer();

        mockMvc.perform(supprimer(EMP, id, connecte(j.admin()))).andExpect(status().isNoContent());

        assertThat(fixture.existe(id)).isFalse();
    }

    @Test
    void socle11_employeCommercialDUnClient_409() throws Exception {
        fixture.poserClesEtrangeres();
        UUID id = employe(j, "E6").inserer();
        j.referencer(id);

        mockMvc.perform(supprimer(EMP, id, connecte(j.admin()))).andExpect(status().isConflict());
        fixture.oublierEcritureRefusee();

        assertThat(fixture.existe(id)).isTrue();
    }

    @Test
    void temoin_fournisseur_personneMorale_toujoursAcceptee() throws Exception {
        mockMvc.perform(creer(EMP, corps(), connecte(j.admin()))).andExpect(status().isCreated());

        mockMvc.perform(creer(FOURNISSEURS.famille(), corpsMorale(j, "Societe " + j.jeton()), connecte(j.admin())))
                .andExpect(status().isCreated());
    }
}
