package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixtureTaches.ASSIGNE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.CODE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_DEBUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DECLENCHEUR_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.DUREE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DUREE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.FICHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.MODULE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.NON_COMMENCEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.NOTES;
import static com.tsix.immocrm.fixtures.FixtureTaches.PRIORITE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.PROJET_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.RACINE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TERMINEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.TYPE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Création, modification, suppression — FEATURE-admin-taches.md, règles 2, 3, 4, 6, 7. Un 404 tombe aussi sur une route
 * absente : chaque cas 404 refait d'abord le même appel sur une vraie tâche, témoin du rouge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TachesEcritureTest {

    private static final String ID = "$.id";
    private static final String DESCRIPTION_MODIFIEE = "Modifiee";
    private static final String DATE_SAISIE = "2026-11-02T09:30:00";
    private static final String TYPE_COLONNE = "AdmTaskTypeId";
    private static final String STATUT_COLONNE = "StrStatusId";
    private static final String CODE_ENVOYE = "CLIENT/1";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureTaches fixture;
    @Autowired private FixtureTachesAdmin referentiels;
    @Autowired private Clock clock;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(RACINE).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(FICHE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions lire(UUID id) throws Exception {
        return mockMvc.perform(get(FICHE, id).with(connecte(admin)));
    }

    private ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(FICHE, id).with(connecte(admin)));
    }

    private ResultActions passer(UUID id) throws Exception {
        return mockMvc.perform(post(STATUT, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(STATUT_ID, EN_COURS.fil()).json()));
    }

    private static UUID idDe(ResultActions reponse) throws Exception {
        return UUID.fromString(JsonPath.read(
                reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), ID));
    }

    private UUID typeTache(String code) {
        return referentiels.ligne(FixtureTachesAdmin.tables().filter(t -> !t.avecTri()).findFirst().orElseThrow(), code);
    }

    private UUID prioriteTache(String code) {
        return referentiels.ligne(FixtureTachesAdmin.tables().filter(FixtureTachesAdmin.TableTache::avecTri)
                .findFirst().orElseThrow(), code);
    }

    private String codeAttendu(int valeur) {
        return "%02d/%05d".formatted(LocalDate.now(clock).getYear() % 100, valeur);
    }

    private CorpsJson saisie() {
        return corps().avec(DESCRIPTION, "Tache " + jeton);
    }

    // ------------------------------------------------------------------ règle 2

    @Test
    void r2_descriptionAbsente_400_sequenceIntacte() throws Exception {
        int avant = fixture.valeurSequence();

        creer(corps().avec(NOTES, "Sans description")).andExpect(status().isBadRequest());

        assertThat(fixture.valeurSequence()).isEqualTo(avant);
    }

    @Test
    void r2_creation_201_codeIssuDeLaSequenceAdmTask() throws Exception {
        int avant = fixture.valeurSequence();

        creer(saisie()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeAttendu(avant + 1)));

        assertThat(fixture.valeurSequence()).isEqualTo(avant + 1);
    }

    @Test
    void r2_creation_entiteTache_statutNonCommencee_traceUserCreate() throws Exception {
        UUID id = idDe(creer(saisie()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.statutId").value(NON_COMMENCEE.fil()))
                .andExpect(jsonPath("$.statutLibelle").value(NON_COMMENCEE.libelle())));

        assertAll(
                () -> assertThat(fixture.guid(id, "StrEntityId")).isEqualTo(ENTITE_TACHE),
                () -> assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(NON_COMMENCEE.id()),
                () -> assertThat(fixture.texte(id, "UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(fixture.date(id, "UserCreateDateTime")).isNotNull());
    }

    @Test
    void r2_creation_ecritChaqueChampDansSaColonne() throws Exception {
        UUID type = typeTache("TYP-" + jeton);
        UUID priorite = prioriteTache("PRI-" + jeton);
        UUID declencheur = terrains.fournisseur("DEC-" + jeton, null);
        UUID gestionnaire = terrains.fournisseur("GES-" + jeton, null);
        UUID assigne = terrains.fournisseur("ASS-" + jeton, null);
        UUID module = fixture.module("MOD-" + jeton);
        UUID entite = patrimoine.entite("ENT-" + jeton, true).getId();
        UUID projet = fixture.projet("PRJ-" + jeton, entite);

        UUID id = idDe(creer(saisie().avec(TYPE_ID, type).avec(PRIORITE_ID, priorite).avec(DECLENCHEUR_ID, declencheur)
                .avec(GESTIONNAIRE_ID, gestionnaire).avec(ASSIGNE_ID, assigne).avec(MODULE_ID, module)
                .avec(ENTITE_ID, entite).avec(PROJET_ID, projet).avec(NOTES, "Notes " + jeton)
                .avec(DATE_PREVUE, DATE_SAISIE).avec(DUREE_PREVUE, 3).avec(DATE_FIN_PREVUE, "2026-11-05T10:00:00")
                .avec(DATE_DEBUT, "2026-11-03T11:00:00").avec(DUREE, 4).avec(DATE_FIN, "2026-11-07T12:00:00"))
                .andExpect(status().isCreated()));

        assertAll(
                () -> assertThat(fixture.guid(id, TYPE_COLONNE)).isEqualTo(type),
                () -> assertThat(fixture.guid(id, "AdmTaskPriorityId")).isEqualTo(priorite),
                () -> assertThat(fixture.guid(id, "CfgTierTriggerId")).isEqualTo(declencheur),
                () -> assertThat(fixture.guid(id, "CfgTierManagerId")).isEqualTo(gestionnaire),
                () -> assertThat(fixture.guid(id, "CfgTierAssignmentId")).isEqualTo(assigne),
                () -> assertThat(fixture.guid(id, "AdmModuleId")).isEqualTo(module),
                () -> assertThat(fixture.guid(id, "CfgCompanyId")).isEqualTo(entite),
                () -> assertThat(fixture.guid(id, "PrjProjectId")).isEqualTo(projet),
                () -> assertThat(fixture.texte(id, "Notes")).isEqualTo("Notes " + jeton),
                () -> assertThat(fixture.date(id, "StartDatePrev")).isEqualTo("2026-11-02T09:30"),
                () -> assertThat(fixture.texte(id, "DelayPrev")).isEqualTo("3"),
                () -> assertThat(fixture.date(id, "DueDatePrev")).isEqualTo("2026-11-05T10:00"),
                () -> assertThat(fixture.date(id, "StartDate")).isEqualTo("2026-11-03T11:00"),
                () -> assertThat(fixture.texte(id, "Delay")).isEqualTo("4"),
                () -> assertThat(fixture.date(id, "EndDate")).isEqualTo("2026-11-07T12:00"));
    }

    /** « Écran » du spec : {@code Date fin prévisionnelle} n'est pas calculée. */
    @Test
    void dateFinPrevue_nonCalculee() throws Exception {
        UUID id = idDe(creer(saisie().avec(DATE_PREVUE, DATE_SAISIE).avec(DUREE_PREVUE, 3))
                .andExpect(status().isCreated()));

        assertThat(fixture.date(id, "DueDatePrev")).isNull();
    }

    // ------------------------------------------------------------------ règle 3

    @Test
    void r3_codeDuClientIgnore_enCreation() throws Exception {
        int avant = fixture.valeurSequence();

        creer(saisie().avec(CODE, CODE_ENVOYE)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeAttendu(avant + 1)));
    }

    @Test
    void r3_codeDuClientIgnore_enModification() throws Exception {
        String code = "COD-" + jeton;
        UUID id = fixture.tache(code);

        modifier(id, corps().avec(CODE, CODE_ENVOYE).avec(DESCRIPTION, DESCRIPTION_MODIFIEE)).andExpect(status().isOk());

        assertThat(fixture.texte(id, "Code")).isEqualTo(code);
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_champAbsent_inchange_champPresent_modifie() throws Exception {
        UUID type = typeTache("TYP-" + jeton);
        String code = "MOD-" + jeton;
        UUID id = fixture.tache(code);
        fixture.definir(id, TYPE_COLONNE, type);
        fixture.definir(id, "Notes", "Notes initiales");

        modifier(id, corps().avec(DESCRIPTION, DESCRIPTION_MODIFIEE)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.texte(id, "Description")).isEqualTo(DESCRIPTION_MODIFIEE),
                () -> assertThat(fixture.guid(id, TYPE_COLONNE)).isEqualTo(type),
                () -> assertThat(fixture.texte(id, "Notes")).isEqualTo("Notes initiales"),
                () -> assertThat(fixture.texte(id, "Code")).isEqualTo(code));
    }

    @Test
    void r4_userUpdateRenseignes_userCreateConserve() throws Exception {
        UUID id = fixture.tache("TRA-" + jeton);

        modifier(id, corps().avec(NOTES, "Trace")).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.texte(id, "UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(fixture.date(id, "UserUpdateDateTime")).isNotNull(),
                () -> assertThat(fixture.texte(id, "UserCreate")).isEqualTo(FixtureTachesAdmin.AUTEUR_OMRAN));
    }

    /** « Impossible » ne dit pas si {@code PATCH} ignore le champ (200) ou le refuse (400) : seul le statut en base fait foi. */
    @Test
    void r4_statutNonModifiableParPatch() throws Exception {
        UUID id = fixture.tache("STA-" + jeton, NON_COMMENCEE);

        int reponse = modifier(id, corps().avec(STATUT_ID, TERMINEE.fil()).avec(NOTES, "Saut de statut"))
                .andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(reponse).isIn(200, 400),
                () -> assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(NON_COMMENCEE.id()));
    }

    // ------------------------------------------------------------------ règle 6 : références inconnues

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {TYPE_ID, PRIORITE_ID, DECLENCHEUR_ID, GESTIONNAIRE_ID, ASSIGNE_ID})
    void r6_referenceInconnue_400_enCreation(String champ) throws Exception {
        creer(saisie().avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {TYPE_ID, PRIORITE_ID, DECLENCHEUR_ID, GESTIONNAIRE_ID, ASSIGNE_ID})
    void r6_referenceInconnue_400_enModification(String champ) throws Exception {
        UUID id = fixture.tache("REF-" + jeton);

        modifier(id, corps().avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ règle 6 : tâche inconnue ou rendez-vous

    @Test
    void r6_lecture_404_tacheInconnue_ouRendezVous() throws Exception {
        UUID tache = fixture.tache("LEC-" + jeton);
        UUID rendezVous = fixture.rendezVous("RDV-" + jeton);

        lire(tache).andExpect(status().isOk());
        lire(UUID.randomUUID()).andExpect(status().isNotFound());
        lire(rendezVous).andExpect(status().isNotFound());
    }

    @Test
    void r6_modification_404_tacheInconnue_ouRendezVous_inchange() throws Exception {
        UUID tache = fixture.tache("ECR-" + jeton);
        UUID rendezVous = fixture.rendezVous("RDV-" + jeton);
        CorpsJson corps = corps().avec(DESCRIPTION, DESCRIPTION_MODIFIEE);

        modifier(tache, corps).andExpect(status().isOk());
        modifier(UUID.randomUUID(), corps).andExpect(status().isNotFound());
        modifier(rendezVous, corps).andExpect(status().isNotFound());

        assertThat(fixture.texte(rendezVous, "Description")).isEqualTo("Tache RDV-" + jeton);
    }

    @Test
    void r6_suppression_404_tacheInconnue_ouRendezVous_conserve() throws Exception {
        UUID tache = fixture.tache("SUP-" + jeton);
        UUID rendezVous = fixture.rendezVous("RDV-" + jeton);

        supprimer(tache).andExpect(status().isNoContent());
        supprimer(UUID.randomUUID()).andExpect(status().isNotFound());
        supprimer(rendezVous).andExpect(status().isNotFound());

        assertThat(fixture.lignes(rendezVous)).isEqualTo(1);
    }

    @Test
    void r6_passageDeStatut_404_tacheInconnue_ouRendezVous() throws Exception {
        UUID tache = fixture.tache("PAS-" + jeton);
        UUID rendezVous = fixture.rendezVous("RDV-" + jeton);

        passer(tache).andExpect(status().isOk());
        passer(UUID.randomUUID()).andExpect(status().isNotFound());
        passer(rendezVous).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ règle 7

    @Test
    void r7_suppression_204_ligneAbsente() throws Exception {
        UUID id = fixture.tache("DEL-" + jeton, EN_COURS);

        supprimer(id).andExpect(status().isNoContent());

        assertThat(fixture.lignes(id)).isZero();
    }
}
