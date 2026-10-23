package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.FICHE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.MSG_BLOQUEE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.SUPERFICIE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statuts et transitions d'une étude — FEATURE-patrimoine-faisabilite-workflow.md §4.1, §4.2, §3 (transition = écriture
 * recalculée), invariant I9 et décisions du 22/10 (transitions limitées aux lecteurs, décimal à 0 jamais vide, libellé
 * « Le champ « X » est obligatoire »). Statuts et transitions de test : jamais ceux de production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionFaisabiliteTest {

    private static final String DETAIL = "$.detail";
    private static final String DESCRIPTION_COL = "Description";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteWorkflow wf;

    private AdmUser admin;
    private String jeton;
    private UUID etude;
    private UUID roleAdmin;
    private UUID roleDg;
    private UUID enEtude;
    private UUID validee;
    private UUID rejetee;
    private UUID annulee;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        wf.typesEtat();
        admin = patrimoine.administrateur();
        jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = new ApiFaisabilite(mockMvc, admin).etude(fiche("TRW-" + jeton, societe.getId()));
        roleAdmin = wf.roleAdministrateur();
        roleDg = wf.roleDirectionGenerale();
        enEtude = wf.statut("ETU-" + jeton, 901);
        validee = wf.statut("VAL-" + jeton, 902);
        rejetee = wf.statut("REJ-" + jeton, 903, ETAT_ECHEC, "");
        annulee = wf.statut("ANN-" + jeton, 904, ETAT_ECHEC, "");
        wf.placer(etude, enEtude);
    }

    private String nom(String prefixe) {
        return prefixe + "-" + jeton;
    }

    private ResultActions executer(UUID transitionId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(TRANSITIONS, etude).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(TRANSITION_ID, transitionId).json()));
    }

    private ResultActions executer(UUID transitionId) throws Exception {
        return executer(transitionId, connecte(admin));
    }

    private RequestPostProcessor directionGenerale() {
        return avecRole(patrimoine.administrateur(), ROLE_DG);
    }

    private String transitionsPossibles(RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(TRANSITIONS, etude).with(qui)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Libellé OMRAN d'un champ du catalogue §4.1 (nom stocké → libellé), lu sur l'API de configuration. */
    private String libelleDuCatalogue(String nomOmran) throws Exception {
        String json = mockMvc.perform(get(CHAMPS_OBLIGATOIRES).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> libelles = JsonPath.read(json, "$[?(@.nom == '" + nomOmran + "')].libelle");
        assertThat(libelles).as("libellé de %s", nomOmran).hasSize(1);
        return libelles.get(0);
    }

    private List<Tuple> historique() {
        return fixture.historique(etude);
    }

    // ------------------------------------------------------------------ transition permise

    /** §4.1 / §4.2 : transition permise → 200, fiche au statut d'arrivée, statut écrit, une ligne d'historique. */
    @Test
    void r4_1_transitionPermise_200_statutEtHistorique() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin, roleDg);

        executer(t).andExpect(status().isOk()).andExpect(jsonPath("$.statutId").value(validee.toString()));

        List<Tuple> lignes = historique();
        assertAll(
                () -> assertThat(wf.statutCourant(etude)).isEqualTo(guid(validee)),
                () -> assertThat(lignes).hasSize(2),
                () -> assertThat(lignes).anySatisfy(h -> assertAll(
                        () -> assertThat(guid(h.get("statut"))).isEqualTo(guid(validee)),
                        () -> assertThat(guid(h.get("utilisateur"))).isEqualTo(guid(admin.getId())),
                        () -> assertThat(h.get("DocDate")).isNotNull())));
    }

    /** §4.1 « Prod » : Administrateur et Direction Générale voient « En cours » et « Rejet », pas « Annulation » (0 rôle). */
    @Test
    void r4_1_transitionsVisibles_adminEtDg_voientEnCoursEtRejet_pasAnnulation() throws Exception {
        UUID enCours = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin, roleDg);
        UUID rejet = wf.transition(enEtude, rejetee, nom("Rejet"), "", roleAdmin, roleDg);
        UUID annulation = wf.transition(enEtude, annulee, nom("Annulation"), "");
        UUID ailleurs = wf.transition(validee, annulee, nom("Depuis validee"), "", roleAdmin, roleDg);

        String parAdmin = transitionsPossibles(connecte(admin));
        String parDg = transitionsPossibles(directionGenerale());

        for (String json : List.of(parAdmin, parDg)) {
            List<String> ids = JsonPath.<List<String>>read(json, "$[*].id").stream()
                    .map(id -> id.toUpperCase(Locale.ROOT)).toList();
            assertThat(ids)
                    .contains(guid(enCours), guid(rejet))
                    .doesNotContain(guid(annulation), guid(ailleurs));
        }
    }

    /** §4.2 : chaque transition possible porte départ, arrivée, champs (renseigné ?) et documents (fourni ?), et « possible ». */
    @Test
    void r4_2_transitionsVisibles_detailChampsEtDocuments() throws Exception {
        UUID type = wf.typeDocument(nom("PV"));
        UUID t = wf.transition(enEtude, validee, nom("En cours"), ",Description,Area", roleAdmin);
        wf.documentDeTransition(t, type, true);
        wf.definir(etude, DESCRIPTION_COL, "");
        wf.definir(etude, "Area", BigDecimal.ZERO);

        String json = transitionsPossibles(connecte(admin));

        String racine = "$[?(@.id == '" + t + "')]";
        assertAll(
                () -> assertThat(JsonPath.<List<Object>>read(json, racine + ".statutDepartId")).singleElement()
                        .satisfies(v -> assertThat(guid(v)).isEqualTo(guid(enEtude))),
                () -> assertThat(JsonPath.<List<Object>>read(json, racine + ".statutArriveeId")).singleElement()
                        .satisfies(v -> assertThat(guid(v)).isEqualTo(guid(validee))),
                () -> assertThat(JsonPath.<List<Object>>read(json, racine + ".possible")).containsExactly(false),
                () -> assertThat(JsonPath.<List<Object>>read(json,
                        racine + ".champsObligatoires[?(@.champ == 'description')].renseigne")).containsExactly(false),
                () -> assertThat(JsonPath.<List<Object>>read(json,
                        racine + ".champsObligatoires[?(@.champ == 'superficie')].renseigne")).containsExactly(true),
                () -> assertThat(JsonPath.<List<Object>>read(json,
                        racine + ".documentsRequis[?(@.typeId == '" + type + "')].fourni")).containsExactly(false));
    }

    // ------------------------------------------------------------------ 409 : passage interdit

    /** §4.1 contrôle 1 / décision 6 : « Annulation » sans rôle → 409, transitionsAutorisees = descriptions permises. */
    @Test
    void r4_1_transitionSansRole_409_listeLesTransitionsPermises() throws Exception {
        wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin, roleDg);
        UUID annulation = wf.transition(enEtude, annulee, nom("Annulation"), "");

        executer(annulation)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, hasItem(nom("En cours"))))
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, not(hasItem(nom("Annulation")))));

        assertAll(
                () -> assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude)),
                () -> assertThat(historique()).hasSize(1));
    }

    /** §4.1 contrôle 1 : départ ≠ statut courant → 409 avec les transitions permises depuis le statut courant. */
    @Test
    void r4_1_departDifferentDuStatutCourant_409() throws Exception {
        wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        UUID ailleurs = wf.transition(validee, annulee, nom("Depuis validee"), "", roleAdmin);

        executer(ailleurs)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, hasItem(nom("En cours"))))
                .andExpect(jsonPath(TRANSITIONS_AUTORISEES, not(hasItem(nom("Depuis validee")))));

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    /** §4.1 : rôle comparé par AdmRole.Code ; une transition réservée à la DG est refusée (409) à l'Administrateur. */
    @Test
    void r4_1_transitionReserveeALaDg_administrateur_409() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("Reservee DG"), "", roleDg);

        executer(t).andExpect(status().isConflict());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    /** §4.1 : la même transition réservée à la DG est permise à un compte Direction Générale. */
    @Test
    void r4_1_transitionReserveeALaDg_directionGenerale_200() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("Reservee DG"), "", roleDg);

        executer(t, directionGenerale()).andExpect(status().isOk());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(validee));
    }

    /** §4.1 : étude bloquée → 409 « Étude bloquée », statut inchangé. */
    @Test
    void r4_1_etudeBloquee_409() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        new ApiFaisabilite(mockMvc, admin).bloquer(etude).andExpect(status().isOk());

        executer(t).andExpect(status().isConflict()).andExpect(jsonPath(DETAIL).value(containsString(MSG_BLOQUEE)));

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    // ------------------------------------------------------------------ 403 : rôle non lecteur

    /** Décision 3 (22/10) : un rôle non lecteur listé sur la transition est sans effet → 403, statut inchangé. */
    @Test
    void r4_2_roleNonLecteurListeSurLaTransition_403() throws Exception {
        String code = nom("CT");
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", wf.role(code));
        RequestPostProcessor nonLecteur = avecRole(patrimoine.administrateur(), code);

        executer(t, nonLecteur).andExpect(status().isForbidden());
        mockMvc.perform(get(TRANSITIONS, etude).with(nonLecteur)).andExpect(status().isForbidden());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    /** §4.2 : 403 Commercial sur la lecture des transitions et de l'historique. */
    @Test
    void r4_2_commercial_403() throws Exception {
        RequestPostProcessor commercial = avecRole(patrimoine.administrateur(), "Commercial");

        mockMvc.perform(get(TRANSITIONS, etude).with(commercial)).andExpect(status().isForbidden());
        mockMvc.perform(get(HISTORIQUE, etude).with(commercial)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ 400 : champs obligatoires

    /** §4.1 contrôle 2 : champ obligatoire de la transition vide → 400 « Transition : » + « Le champ « X » est obligatoire ». */
    @Test
    void r4_1_champObligatoireDeTransitionVide_400() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), ",Description", roleAdmin);
        wf.definir(etude, DESCRIPTION_COL, "");
        String libelle = libelleDuCatalogue(DESCRIPTION_COL);

        executer(t)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(PREFIXE_TRANSITION)))
                .andExpect(jsonPath(DETAIL).value(containsString(msgChamp(libelle))));

        assertAll(
                () -> assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude)),
                () -> assertThat(historique()).hasSize(1));
    }

    /** Décision 5 : un décimal à 0 n'est jamais vide — Area et AreaNet cochés, superficie 0 → 200. */
    @Test
    void r4_1_decimalAZero_nonVide_200() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), ",Area,AreaNet", roleAdmin);
        wf.definir(etude, "Area", BigDecimal.ZERO);

        executer(t).andExpect(status().isOk());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(validee));
    }

    /** §4.1 : un nom de champ inconnu stocké en base est ignoré (jamais 500). */
    @Test
    void r4_1_nomDeChampInconnu_ignore_200() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), ",ChampInexistant", roleAdmin);

        mockMvc.perform(get(TRANSITIONS, etude).with(connecte(admin))).andExpect(status().isOk());
        executer(t).andExpect(status().isOk());
    }

    /** §4.1 contrôle 4 : champ obligatoire du nouveau statut vide → 400 « Statuts : », rien n'est écrit (rollback). */
    @Test
    void r4_1_champObligatoireDuNouveauStatut_400_rollback() throws Exception {
        UUID exigeant = wf.statut(nom("EXI"), 905, ETAT_INTERMEDIAIRE, ",Description");
        UUID t = wf.transition(enEtude, exigeant, nom("Vers exigeant"), "", roleAdmin);
        wf.definir(etude, DESCRIPTION_COL, "");
        String libelle = libelleDuCatalogue(DESCRIPTION_COL);

        executer(t)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(PREFIXE_STATUTS)))
                .andExpect(jsonPath(DETAIL).value(containsString(msgChamp(libelle))));

        assertAll(
                () -> assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude)),
                () -> assertThat(historique()).hasSize(1));
    }

    /** §4.1 contrôle 4 / §12.2 : le Verif de l'étude contrôle aussi le statut courant — un PATCH y est refusé (400 « Statuts : »). */
    @Test
    void r4_1_verifDeLEtude_patch_statutCourantAvecChampVide_400() throws Exception {
        UUID exigeant = wf.statut(nom("EXI"), 905, ETAT_INTERMEDIAIRE, ",Description");
        wf.placer(etude, exigeant);
        wf.definir(etude, DESCRIPTION_COL, "");

        mockMvc.perform(patch(FICHE, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(SUPERFICIE, new BigDecimal("500")).json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(PREFIXE_STATUTS)));
    }

    // ------------------------------------------------------------------ 400 : documents

    /** §4.1 contrôle 3 / §0 point 5 : chaque ligne de document est exigée, IsMandatory non lu ; message par type manquant. */
    @Test
    void r4_1_documentManquant_400_memeNonObligatoire() throws Exception {
        UUID fourni = wf.typeDocument(nom("PV"));
        UUID manquant = wf.typeDocument(nom("Plan"));
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        wf.documentDeTransition(t, fourni, true);
        wf.documentDeTransition(t, manquant, false);
        wf.pieceJointe(etude, fourni);

        executer(t)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(msgDocument(nom("Plan")))))
                .andExpect(jsonPath(DETAIL).value(not(containsString(msgDocument(nom("PV"))))));

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(enEtude));
    }

    /** §4.1 contrôle 3 : une pièce jointe de l'étude de chaque type configuré → 200. */
    @Test
    void r4_1_documentsFournis_200() throws Exception {
        UUID pv = wf.typeDocument(nom("PV"));
        UUID plan = wf.typeDocument(nom("Plan"));
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        wf.documentDeTransition(t, pv, true);
        wf.documentDeTransition(t, plan, false);
        wf.pieceJointe(etude, pv);
        wf.pieceJointe(etude, plan);

        executer(t).andExpect(status().isOk());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(validee));
    }

    /** §4.1 contrôle 3 : la pièce d'une AUTRE étude ne compte pas (décompte par AdmAttachedFile.CategoryId = étude). */
    @Test
    void r4_1_pieceDUneAutreEtude_neComptePas_400() throws Exception {
        UUID pv = wf.typeDocument(nom("PV"));
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        wf.documentDeTransition(t, pv, true);
        UUID autre = new ApiFaisabilite(mockMvc, admin)
                .etude(fiche("AUT-" + jeton, patrimoine.entite("ENA-" + jeton, true).getId()));
        wf.pieceJointe(autre, pv);

        executer(t).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(msgDocument(nom("PV")))));
    }

    // ------------------------------------------------------------------ 404, historique, recalcul

    @Test
    void r4_2_etudeInconnue_404() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        UUID inconnue = UUID.fromString("5B6C7D8E-9F01-4A2B-8C3D-4E5F6A7B8C9D");

        mockMvc.perform(post(TRANSITIONS, inconnue).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(TRANSITION_ID, t).json())).andExpect(status().isNotFound());
        mockMvc.perform(get(TRANSITIONS, inconnue).with(connecte(admin))).andExpect(status().isNotFound());
        mockMvc.perform(get(HISTORIQUE, inconnue).with(connecte(admin))).andExpect(status().isNotFound());
    }

    /** §4.1 / §4.2 : GET historique — date, code et libellé du statut, Sorting, utilisateur (AdmUser.Description). */
    @Test
    void r4_2_historique_ligneDeLaTransition() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        executer(t).andExpect(status().isOk());

        String json = mockMvc.perform(get(HISTORIQUE, etude).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String ligne = "$[?(@.statutCode == '" + nom("VAL") + "')]";
        assertAll(
                () -> assertThat(JsonPath.<List<Object>>read(json, "$")).hasSize(2),
                () -> assertThat(JsonPath.<List<Object>>read(json, ligne + ".statutLibelle")).containsExactly(nom("VAL")),
                () -> assertThat(JsonPath.<List<Object>>read(json, ligne + ".ordre")).singleElement()
                        .satisfies(o -> assertThat(((Number) o).intValue()).isEqualTo(902)),
                () -> assertThat(JsonPath.<List<Object>>read(json, ligne + ".utilisateur")).containsExactly(AUTEUR),
                () -> assertThat(JsonPath.<List<Object>>read(json, ligne + ".date")).singleElement().isNotNull());
    }

    /** §3 : la transition est une écriture de l'étude → recalcul dans la même transaction (colonnes du bilan réécrites). */
    @Test
    void r3_transition_appelleLeRecalcul_bilanReecrit() throws Exception {
        UUID t = wf.transition(enEtude, validee, nom("En cours"), "", roleAdmin);
        wf.definir(etude, "TotalCostHT", new BigDecimal("123"));

        executer(t).andExpect(status().isOk());

        Tuple e = fixture.etude(etude);
        assertThat(decimal(e.get("TotalCostHT"))).isEqualByComparingTo("0");
    }
}
