package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.CODE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ENTITE_ETUDE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuration des statuts et transitions des études — FEATURE-patrimoine-faisabilite-workflow.md §5 (et §4.1 : catalogue
 * des champs, MandatoryFields à virgule initiale). L'autorisation (rôles, champs) relève de la règle 6 : refus testés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigWorkflowFaisabiliteTest {

    private static final String DETAIL = "$.detail";
    private static final String STATUT_SQL = "SELECT * FROM StkFeasibilityStudyStatus WHERE PKey = :id";
    private static final String TRANSITION_SQL = "SELECT * FROM StkFeasibilityStudyWorkFlow WHERE PKey = :id";
    private static final String ROLES_SQL = "SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlowAdmRole WHERE WorkFlowId = :id";
    private static final String DOCS_TRANSITION_SQL =
            "SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlowDocumentToAttach WHERE StkFeasibilityStudyWorkFlowId = :id";
    private static final String MANDATORY = "MandatoryFields";
    private static final String CODE_OMRAN = "Code";
    private static final String AREA_OMRAN = "Area";
    private static final String DESCRIPTION_OMRAN = "Description";
    private static final UUID INCONNU = UUID.fromString("6C7D8E9F-0A1B-4C2D-9E3F-5A6B7C8D9E0F");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteWorkflow wf;

    private AdmUser admin;
    private String jeton;
    private UUID roleAdmin;
    private UUID roleDg;
    private UUID depart;
    private UUID arrivee;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        wf.typesEtat();
        admin = patrimoine.administrateur();
        jeton = jeton();
        roleAdmin = wf.roleAdministrateur();
        roleDg = wf.roleDirectionGenerale();
        depart = wf.statut("DEP-" + jeton, 911);
        arrivee = wf.statut("ARR-" + jeton, 912);
    }

    private String nom(String prefixe) {
        return prefixe + "-" + jeton;
    }

    private ResultActions envoyer(String url, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(url).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(String url, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(url, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps.json()));
    }

    private ResultActions supprimer(String url, UUID id) throws Exception {
        return mockMvc.perform(delete(url, id).with(connecte(admin)));
    }

    private static UUID idDe(ResultActions reponse) throws Exception {
        MvcResult r = reponse.andExpect(status().is2xxSuccessful()).andReturn();
        return UUID.fromString(JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id"));
    }

    private CorpsJson statutCorps(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Statut " + code).avec(TYPE_ETAT_ID, ETAT_INTERMEDIAIRE);
    }

    private CorpsJson transitionCorps(String description, UUID de, UUID vers) {
        return corps().avec(DESCRIPTION, description).avec(STATUT_DEPART_ID, de).avec(STATUT_ARRIVEE_ID, vers);
    }

    private Tuple ligne(String sql, UUID id) {
        List<Tuple> lignes = fixture.lignes(sql, id);
        assertThat(lignes).hasSize(1);
        return lignes.get(0);
    }

    private static Object vrai(Object bit) {
        return bit instanceof Boolean b ? b : ((Number) bit).intValue() == 1;
    }

    // ------------------------------------------------------------------ statuts

    /** §5 statuts : création, MandatoryFields au format OMRAN (« ,Code,Area »), défauts sorting 1, délai 0, couleur ''. */
    @Test
    void r5_statut_creation_ligneEcrite_defautsEtVirguleInitiale() throws Exception {
        UUID id = idDe(envoyer(CONFIG_STATUTS, statutCorps(nom("NOU")).avec(CHAMPS, List.of(CODE_OMRAN, AREA_OMRAN))));

        Tuple s = ligne(STATUT_SQL, id);
        assertAll(
                () -> assertThat(s.get("Code")).isEqualTo(nom("NOU")),
                () -> assertThat(s.get("Description")).isEqualTo("Statut " + nom("NOU")),
                () -> assertThat(((Number) s.get("Sorting")).intValue()).isEqualTo(1),
                () -> assertThat(((Number) s.get("Delay")).intValue()).isZero(),
                () -> assertThat(s.get("ColorName")).isEqualTo(""),
                () -> assertThat(s.get(MANDATORY)).isEqualTo(",Code,Area"),
                () -> assertThat(guid(s.get("AdmWorkFlowStateTypeId"))).isEqualTo(guid(ETAT_INTERMEDIAIRE)));
    }

    /** §4.1 compatibilité d'écriture : aucun champ → MandatoryFields = '' (jamais NULL). */
    @Test
    void r5_statut_sansChampObligatoire_mandatoryFieldsVide() throws Exception {
        UUID id = idDe(envoyer(CONFIG_STATUTS, statutCorps(nom("VID"))));

        assertThat(ligne(STATUT_SQL, id).get(MANDATORY)).isEqualTo("");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, DESCRIPTION, TYPE_ETAT_ID})
    void r5_statut_champObligatoireAbsent_400(String champ) throws Exception {
        envoyer(CONFIG_STATUTS, statutCorps(nom("OBL")).sans(champ)).andExpect(status().isBadRequest());
    }

    /** §5 : code U → 409 si pris (création). */
    @Test
    void r5_statut_codeDejaPris_creation_409() throws Exception {
        envoyer(CONFIG_STATUTS, statutCorps(nom("DEP"))).andExpect(status().isConflict());
    }

    /** §5 : code U → 409 si pris (modification), statut inchangé. */
    @Test
    void r5_statut_codeDejaPris_modification_409() throws Exception {
        modifier(CONFIG_STATUT, arrivee, corps().avec(CODE, nom("DEP"))).andExpect(status().isConflict());

        assertThat(ligne(STATUT_SQL, arrivee).get("Code")).isEqualTo(nom("ARR"));
    }

    /** §5 : documents du statut stockés (sans effet) — IsMandatory, WithTitle, type. */
    @Test
    void r5_statut_documents_stockes() throws Exception {
        UUID type = wf.typeDocument(nom("PV"));
        UUID id = idDe(envoyer(CONFIG_STATUTS, statutCorps(nom("DOC")).avec(DOCUMENTS, List.of(
                corps().avec(TYPE_DOCUMENT_ID, type).avec(EST_OBLIGATOIRE, true).avec(AVEC_TITRE, true)))));

        Tuple d = ligne("SELECT * FROM StkFeasibilityStudyStatusDocumentToAttach WHERE StkFeasibilityStudyStatusId = :id", id);
        assertAll(
                () -> assertThat(guid(d.get("AdmAttachedFileTypeId"))).isEqualTo(guid(type)),
                () -> assertThat(vrai(d.get("IsMandatory"))).isEqualTo(true),
                () -> assertThat(vrai(d.get("WithTitle"))).isEqualTo(true));
    }

    /** §5 / §4.1 : la lecture d'un statut rend les noms OMRAN de MandatoryFields. */
    @Test
    void r5_statut_lecture_champsObligatoires() throws Exception {
        UUID id = wf.statut(nom("LEC"), 913, ETAT_INTERMEDIAIRE, ",Code,Area");

        mockMvc.perform(get(CONFIG_STATUT, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(nom("LEC")))
                .andExpect(jsonPath("$.champsObligatoires", org.hamcrest.Matchers.contains(CODE_OMRAN, AREA_OMRAN)));
    }

    /** §5 suppression : 409 si une étude l'utilise. */
    @Test
    void r5_statut_suppression_utiliseParUneEtude_409() throws Exception {
        UUID etude = etude();
        wf.placer(etude, depart);

        supprimer(CONFIG_STATUT, depart).andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyStatus WHERE PKey = :id", depart)).isEqualTo(1);
    }

    /** §5 suppression : 409 si une transition l'utilise (départ ou arrivée). */
    @Test
    void r5_statut_suppression_utiliseParUneTransition_409() throws Exception {
        wf.transition(depart, arrivee, nom("T"), "", roleAdmin);

        supprimer(CONFIG_STATUT, arrivee).andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyStatus WHERE PKey = :id", arrivee)).isEqualTo(1);
    }

    /** §5 suppression : 409 si l'historique le référence (FK CASCADE → refus explicite). */
    @Test
    void r5_statut_suppression_historise_409() throws Exception {
        UUID etude = etude();
        wf.historique(etude, arrivee);

        supprimer(CONFIG_STATUT, arrivee).andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyStatusHistory WHERE StkFeasibilityStudyId = :id",
                etude)).isEqualTo(2);
    }

    /** §5 suppression d'un statut libre : la ligne disparaît. */
    @Test
    void r5_statut_suppression_libre_ligneSupprimee() throws Exception {
        supprimer(CONFIG_STATUT, arrivee).andExpect(status().is2xxSuccessful());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyStatus WHERE PKey = :id", arrivee)).isZero();
    }

    private UUID etude() throws Exception {
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        return new ApiFaisabilite(mockMvc, admin).etude(fiche("CFG-" + jeton, societe.getId()));
    }

    // ------------------------------------------------------------------ transitions

    /** §5 transitions : création — virgule initiale, StrEntityId constant, défauts (sorting 1, note/e-mail faux), rôles, documents. */
    @Test
    void r5_transition_creation_ligneEtSousListes() throws Exception {
        UUID type = wf.typeDocument(nom("PV"));
        UUID id = idDe(envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("En cours"), depart, arrivee)
                .avec(CHAMPS, List.of(CODE_OMRAN, AREA_OMRAN)).avec(ROLES, List.of(roleAdmin, roleDg))
                .avec(DOCUMENTS, List.of(corps().avec(TYPE_DOCUMENT_ID, type).avec(EST_OBLIGATOIRE, false)
                        .avec(AVEC_TITRE, false)))));

        Tuple t = ligne(TRANSITION_SQL, id);
        assertAll(
                () -> assertThat(t.get(MANDATORY)).isEqualTo(",Code,Area"),
                () -> assertThat(guid(t.get("StrEntityId"))).isEqualTo(guid(ENTITE_ETUDE)),
                () -> assertThat(guid(t.get("StartStatusId"))).isEqualTo(guid(depart)),
                () -> assertThat(guid(t.get("EndStatusId"))).isEqualTo(guid(arrivee)),
                () -> assertThat(((Number) t.get("Sorting")).intValue()).isEqualTo(1),
                () -> assertThat(vrai(t.get("NotesMandatory"))).isEqualTo(false),
                () -> assertThat(vrai(t.get("SendEmailToUsers"))).isEqualTo(false),
                () -> assertThat(fixture.compter(ROLES_SQL, id)).isEqualTo(2),
                () -> assertThat(fixture.compter(DOCS_TRANSITION_SQL, id)).isEqualTo(1));
    }

    /** §5 : notes obligatoires, notification et e-mail stockés tels que saisis (sans effet, décision 4). */
    @Test
    void r5_transition_noteNotificationEmail_stockes() throws Exception {
        UUID id = idDe(envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Notes"), depart, arrivee)
                .avec(NOTE_OBLIGATOIRE, true).avec(AVEC_NOTIFICATION, true).avec(ENVOYER_EMAIL, true)));

        Tuple t = ligne(TRANSITION_SQL, id);
        assertAll(
                () -> assertThat(vrai(t.get("NotesMandatory"))).isEqualTo(true),
                () -> assertThat(vrai(t.get("WithNotification"))).isEqualTo(true),
                () -> assertThat(vrai(t.get("SendEmailToUsers"))).isEqualTo(true));
    }

    /** Décision 4 : une transition à note obligatoire s'exécute sans note (aucun effet). */
    @Test
    void r5_transition_noteObligatoire_sansEffetALExecution() throws Exception {
        UUID id = idDe(envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Notes"), depart, arrivee)
                .avec(NOTE_OBLIGATOIRE, true).avec(ROLES, List.of(roleAdmin))));
        UUID etude = etude();
        wf.placer(etude, depart);

        mockMvc.perform(post(TRANSITIONS, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(TRANSITION_ID, id).json()))
                .andExpect(status().isOk());
    }

    /** §5 : « Le statut de départ et le statut d'arrivé doivent être différents! » (400, message exact). */
    @Test
    void r5_transition_departEgalArrivee_400_messageExact() throws Exception {
        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Boucle"), depart, depart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MSG_DEPART_ARRIVEE));
    }

    /** §5 : unicité (départ, arrivée, entité) → 409. */
    @Test
    void r5_transition_doublonDepartArrivee_409() throws Exception {
        wf.transition(depart, arrivee, nom("Existante"), "", roleAdmin);

        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Doublon"), depart, arrivee)).andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlow WHERE StartStatusId = :id", depart))
                .isEqualTo(1);
    }

    /** §5 : une transition à 0 rôle est permise (cas de « Annulation »). */
    @Test
    void r5_transition_sansRole_permise() throws Exception {
        UUID id = idDe(envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Annulation"), depart, arrivee)
                .avec(ROLES, List.of())));

        assertThat(fixture.compter(ROLES_SQL, id)).isZero();
    }

    /** §5 : rôles sans doublon — un rôle répété n'écrit jamais deux lignes (refus 400 ou dédoublonnage). */
    @Test
    void r5_transition_roleRepete_jamaisDeuxLignes() throws Exception {
        int code = envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Doublon role"), depart, arrivee)
                .avec(ROLES, List.of(roleAdmin, roleAdmin))).andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(code).isIn(200, 201, 400),
                () -> assertThat(fixture.compter("""
                        SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlowAdmRole r
                        JOIN StkFeasibilityStudyWorkFlow w ON w.PKey = r.WorkFlowId WHERE w.StartStatusId = :id""", depart))
                        .isLessThanOrEqualTo(1));
    }

    /** §5 : statut de départ inconnu → 400 nommant le champ. */
    @Test
    void r5_transition_statutInconnu_400() throws Exception {
        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Inconnu"), INCONNU, arrivee))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(STATUT_DEPART_ID)));
    }

    /** §5 : rôle inconnu → 400. */
    @Test
    void r5_transition_roleInconnu_400() throws Exception {
        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Role inconnu"), depart, arrivee).avec(ROLES, List.of(INCONNU)))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlow WHERE StartStatusId = :id", depart))
                .isZero();
    }

    /** §5 : type de document inconnu → 400. */
    @Test
    void r5_transition_typeDocumentInconnu_400() throws Exception {
        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Doc inconnu"), depart, arrivee).avec(DOCUMENTS, List.of(
                corps().avec(TYPE_DOCUMENT_ID, INCONNU).avec(EST_OBLIGATOIRE, true).avec(AVEC_TITRE, false))))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {DESCRIPTION, STATUT_DEPART_ID, STATUT_ARRIVEE_ID})
    void r5_transition_champObligatoireAbsent_400(String champ) throws Exception {
        envoyer(CONFIG_TRANSITIONS, transitionCorps(nom("Obl"), depart, arrivee).sans(champ))
                .andExpect(status().isBadRequest());
    }

    /** §5 : suppression — rôles retirés d'abord, documents effacés, transition supprimée. */
    @Test
    void r5_transition_suppression_retireRolesEtDocuments() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("A supprimer"), "", roleAdmin, roleDg);
        wf.documentDeTransition(t, wf.typeDocument(nom("PV")), true);

        supprimer(CONFIG_TRANSITION, t).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(fixture.compter(ROLES_SQL, t)).isZero(),
                () -> assertThat(fixture.compter(DOCS_TRANSITION_SQL, t)).isZero(),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkFeasibilityStudyWorkFlow WHERE PKey = :id", t))
                        .isZero());
    }

    /** Règle 6 : retirer l'Administrateur des rôles d'une transition → l'Administrateur ne peut plus l'emprunter (409). */
    @Test
    void r5_transition_patchRoles_retireAdministrateur_refuse409() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("En cours"), "", roleAdmin, roleDg);
        UUID etude = etude();
        wf.placer(etude, depart);

        modifier(CONFIG_TRANSITION, t, corps().avec(ROLES, List.of(roleDg))).andExpect(status().is2xxSuccessful());

        mockMvc.perform(post(TRANSITIONS, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(TRANSITION_ID, t).json()))
                .andExpect(status().isConflict());
        assertThat(fixture.compter(ROLES_SQL, t)).isEqualTo(1);
    }

    /** Règle 6 : ajouter un champ obligatoire à une transition → la transition est refusée tant qu'il est vide (400). */
    @Test
    void r5_transition_patchChamps_ecritVirgule_etExigeLeChamp() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("En cours"), "", roleAdmin);
        UUID etude = etude();
        wf.placer(etude, depart);
        wf.definir(etude, "Description", "");

        modifier(CONFIG_TRANSITION, t, corps().avec(CHAMPS, List.of(DESCRIPTION_OMRAN))).andExpect(status().is2xxSuccessful());

        assertThat(ligne(TRANSITION_SQL, t).get(MANDATORY)).isEqualTo(",Description");
        mockMvc.perform(post(TRANSITIONS, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(TRANSITION_ID, t).json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(PREFIXE_TRANSITION)));
    }

    /** §4.1 : retirer tous les champs → MandatoryFields = ''. */
    @Test
    void r5_transition_patchChampsVides_mandatoryFieldsVide() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("En cours"), ",Code", roleAdmin);

        modifier(CONFIG_TRANSITION, t, corps().avec(CHAMPS, List.of())).andExpect(status().is2xxSuccessful());

        assertThat(ligne(TRANSITION_SQL, t).get(MANDATORY)).isEqualTo("");
    }

    /** §4.1 : un nom inconnu stocké en base ne casse pas la lecture de la configuration (jamais 500). */
    @Test
    void r5_transition_nomInconnuStocke_lecture200() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("Bidon"), ",ChampInexistant,Code", roleAdmin);

        mockMvc.perform(get(CONFIG_TRANSITION, t).with(connecte(admin))).andExpect(status().isOk());
        mockMvc.perform(get(CONFIG_TRANSITIONS).with(connecte(admin))).andExpect(status().isOk());
    }

    /** §5 : lecture d'une transition — départ, arrivée, rôles. */
    @Test
    void r5_transition_lecture_rolesEtStatuts() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("En cours"), "", roleAdmin, roleDg);

        String json = mockMvc.perform(get(CONFIG_TRANSITION, t).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<String> roles = JsonPath.<List<String>>read(json, "$.roles").stream().map(r -> r.toUpperCase(java.util.Locale.ROOT))
                .toList();
        assertAll(
                () -> assertThat(guid(JsonPath.<String>read(json, "$.statutDepartId"))).isEqualTo(guid(depart)),
                () -> assertThat(guid(JsonPath.<String>read(json, "$.statutArriveeId"))).isEqualTo(guid(arrivee)),
                () -> assertThat(roles).containsExactlyInAnyOrder(guid(roleAdmin), guid(roleDg)));
    }

    // ------------------------------------------------------------------ catalogue des champs (§4.1)

    /** §4.1 : catalogue nom OMRAN → champ du CRM, libellé renseigné. */
    @Test
    void r4_1_catalogueDesChamps_correspondances() throws Exception {
        String json = mockMvc.perform(get(CHAMPS_OBLIGATOIRES).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, String> attendus = Map.of(CODE_OMRAN, "code", AREA_OMRAN, "superficie", "Cos", "coefEmprise",
                "SalableArea", "plancherVendable", "DocDate", "dateDocument", "TotalCostHT", "coutTotalHt",
                "PurchaseUnitCostHT", "prixAchatM2Ht", "RegistryCostsVatAmount", "fraisEnregistrementTva");
        for (Map.Entry<String, String> e : attendus.entrySet()) {
            String filtre = "$[?(@.nom == '" + e.getKey() + "')]";
            assertThat(JsonPath.<List<Object>>read(json, filtre + ".champ")).as(e.getKey()).containsExactly(e.getValue());
            assertThat(JsonPath.<List<String>>read(json, filtre + ".libelle")).as(e.getKey()).singleElement()
                    .satisfies(l -> assertThat(l).isNotBlank());
        }
    }

    /** §5 : listes des statuts et des transitions (lecture Administrateur). */
    @Test
    void r5_listes_contiennentLesLignesDeTest() throws Exception {
        UUID t = wf.transition(depart, arrivee, nom("En cours"), "", roleAdmin);

        mockMvc.perform(get(CONFIG_STATUTS).with(connecte(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + depart + "')].code").value(org.hamcrest.Matchers.contains(nom("DEP"))));
        mockMvc.perform(get(CONFIG_TRANSITIONS).with(connecte(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + t + "')].description")
                        .value(org.hamcrest.Matchers.contains(nom("En cours"))));
    }
}
