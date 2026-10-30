package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.RACINE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.TIER_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.CODE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DECLENCHEUR_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.DUREE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_RENDEZ_VOUS;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.NOTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Création, modification, suppression — FEATURE-admin-rendez-vous.md, règles 5 à 10. Un 404 tombe aussi sur une route
 * absente : chaque cas 404 refait d'abord le même appel sur un vrai rendez-vous, témoin du rouge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousEcritureTest {

    private static final String DESCRIPTION_MODIFIEE = "Modifiee";
    private static final String DESCRIPTION_COLONNE = "Description";
    private static final String CODE_COLONNE = "Code";
    private static final String NOTES_COLONNE = "Notes";
    private static final String DATE_COLONNE = "StartDatePrev";
    private static final String DATE_FIN_COLONNE = "DueDatePrev";
    private static final String DUREE_COLONNE = "DelayPrev";
    private static final String TIER_COLONNE = "CfgProspectId";
    private static final String DECLENCHEUR_COLONNE = "CfgTierTriggerId";
    private static final String GESTIONNAIRE_COLONNE = "CfgTierManagerId";
    private static final String OBJET_COLONNE = "ObjectId";
    private static final String OBJET_LIBELLE_COLONNE = "ObjectLabel";
    private static final String OBJET_ENTITE_COLONNE = "ObjectStrEntityId";
    private static final String STATUT_COLONNE = "StrStatusId";
    private static final String CODE_ENVOYE = "CLIENT/1";
    private static final String LIBELLE_SOURCE = "Prospection : 22/01570";
    private static final String DATE_SAISIE = "2031-03-10T09:30:00";
    private static final String DATE_SAISIE_MINUTE = "2031-03-10T09:30";
    private static final String DATE_ANTERIEURE = "2031-03-09T09:30:00";
    private static final String DATE_POSTERIEURE = "2031-03-11T09:30:00";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;
    @Autowired private Clock clock;

    private AdmUser admin;
    private String jeton;
    private UUID tier;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        tier = fixture.tier("TIE-" + jeton, "Tier " + jeton);
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

    private static UUID idDe(ResultActions reponse) throws Exception {
        return UUID.fromString(JsonPath.read(
                reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id"));
    }

    private String codeAttendu(int valeur) {
        return "%02d/%05d".formatted(LocalDate.now(clock).getYear() % 100, valeur);
    }

    private CorpsJson saisie() {
        return corps().avec(DESCRIPTION, "Rendez-vous " + jeton).avec(TIER_ID, tier).avec(DATE_PREVUE, DATE_SAISIE);
    }

    private UUID rendezVous(String suffixe) {
        return fixture.rendezVous(suffixe + "-" + jeton, JOUR);
    }

    // ------------------------------------------------------------------ règle 5

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {DESCRIPTION, TIER_ID, DATE_PREVUE})
    void r5_champObligatoireAbsent_400(String champ) throws Exception {
        creer(saisie().sans(champ)).andExpect(status().isBadRequest());
    }

    @Test
    void r5_creation_201_codeIssuDeLaSequenceAdmTask() throws Exception {
        int avant = taches.valeurSequence();

        creer(saisie()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeAttendu(avant + 1)));

        assertThat(taches.valeurSequence()).isEqualTo(avant + 1);
    }

    @Test
    void r5_creation_entiteRendezVous_sansStatut_traceUserCreate() throws Exception {
        UUID id = idDe(creer(saisie()).andExpect(status().isCreated()));

        assertAll(
                () -> assertThat(taches.guid(id, "StrEntityId")).isEqualTo(ENTITE_RENDEZ_VOUS),
                () -> assertThat(taches.guid(id, STATUT_COLONNE)).isNull(),
                () -> assertThat(taches.texte(id, "UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(taches.date(id, "UserCreateDateTime")).isNotNull());
    }

    @Test
    void r5_creation_ecritChaqueChampDansSaColonne() throws Exception {
        UUID declencheur = fixture.tier("DEC-" + jeton, "Declencheur");
        UUID gestionnaire = fixture.tier("GES-" + jeton, "Gestionnaire");

        UUID id = idDe(creer(saisie().avec(DECLENCHEUR_ID, declencheur).avec(GESTIONNAIRE_ID, gestionnaire)
                .avec(DUREE_PREVUE, 3).avec(DATE_FIN_PREVUE, DATE_POSTERIEURE).avec(NOTES, "Notes " + jeton))
                .andExpect(status().isCreated()));

        assertAll(
                () -> assertThat(taches.texte(id, DESCRIPTION_COLONNE)).isEqualTo("Rendez-vous " + jeton),
                () -> assertThat(taches.guid(id, DECLENCHEUR_COLONNE)).isEqualTo(declencheur),
                () -> assertThat(taches.guid(id, GESTIONNAIRE_COLONNE)).isEqualTo(gestionnaire),
                () -> assertThat(taches.guid(id, TIER_COLONNE)).isEqualTo(tier),
                () -> assertThat(taches.date(id, DATE_COLONNE)).isEqualTo(DATE_SAISIE_MINUTE),
                () -> assertThat(taches.texte(id, DUREE_COLONNE)).isEqualTo("3"),
                () -> assertThat(taches.date(id, DATE_FIN_COLONNE)).isEqualTo("2031-03-11T09:30"),
                () -> assertThat(taches.texte(id, NOTES_COLONNE)).isEqualTo("Notes " + jeton));
    }

    // ------------------------------------------------------------------ règle 6

    @Test
    void r6_dateFinAbsente_egaleDatePrevue() throws Exception {
        UUID id = idDe(creer(saisie()).andExpect(status().isCreated()));

        assertThat(taches.date(id, DATE_FIN_COLONNE)).isEqualTo(DATE_SAISIE_MINUTE);
    }

    @Test
    void r6_creation_dateFinAvantDatePrevue_400() throws Exception {
        creer(saisie().avec(DATE_FIN_PREVUE, DATE_ANTERIEURE)).andExpect(status().isBadRequest());
    }

    @Test
    void r6_modification_lesDeuxDatesEnvoyees_finAvantDebut_400() throws Exception {
        UUID id = rendezVous("DAT");

        modifier(id, corps().avec(DATE_PREVUE, DATE_SAISIE).avec(DATE_FIN_PREVUE, DATE_ANTERIEURE))
                .andExpect(status().isBadRequest());
    }

    /** Comparaison à la date déjà en base : règle 8 (champ absent inchangé) appliquée à la règle 6. */
    @Test
    void r6_modification_finSeule_avantLaDatePrevueEnBase_400() throws Exception {
        UUID id = rendezVous("DAF");

        modifier(id, corps().avec(DATE_FIN_PREVUE, DATE_ANTERIEURE)).andExpect(status().isBadRequest());
    }

    /** Comparaison à la date déjà en base : règle 8 (champ absent inchangé) appliquée à la règle 6. */
    @Test
    void r6_modification_debutSeul_apresLaDateFinEnBase_400() throws Exception {
        UUID id = rendezVous("DAD");

        modifier(id, corps().avec(DATE_PREVUE, DATE_POSTERIEURE)).andExpect(status().isBadRequest());
    }

    /** Glisser vers un autre jour : les deux dates envoyées ensemble. */
    @Test
    void r6_modification_lesDeuxDatesValides_ecrites() throws Exception {
        UUID id = rendezVous("GLI");

        modifier(id, corps().avec(DATE_PREVUE, "2031-03-20T09:30:00").avec(DATE_FIN_PREVUE, "2031-03-20T10:30:00"))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(taches.date(id, DATE_COLONNE)).isEqualTo("2031-03-20T09:30"),
                () -> assertThat(taches.date(id, DATE_FIN_COLONNE)).isEqualTo("2031-03-20T10:30"));
    }

    // ------------------------------------------------------------------ règle 7

    @Test
    void r7_codeDuClientIgnore_enCreation() throws Exception {
        int avant = taches.valeurSequence();

        creer(saisie().avec(CODE, CODE_ENVOYE)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(codeAttendu(avant + 1)));
    }

    @Test
    void r7_codeDuClientIgnore_enModification() throws Exception {
        UUID id = rendezVous("COD");

        modifier(id, corps().avec(CODE, CODE_ENVOYE).avec(DESCRIPTION, DESCRIPTION_MODIFIEE)).andExpect(status().isOk());

        assertThat(taches.texte(id, CODE_COLONNE)).isEqualTo("COD-" + jeton);
    }

    // ------------------------------------------------------------------ règle 8

    @Test
    void r8_champAbsent_inchange_champPresent_modifie() throws Exception {
        UUID declencheur = fixture.tier("DEC-" + jeton, "Declencheur");
        UUID gestionnaire = fixture.tier("GES-" + jeton, "Gestionnaire");
        UUID id = rendezVous("MOD");
        taches.definir(id, DECLENCHEUR_COLONNE, declencheur);
        taches.definir(id, GESTIONNAIRE_COLONNE, gestionnaire);
        taches.definir(id, TIER_COLONNE, tier);
        taches.definir(id, DUREE_COLONNE, 3);
        taches.definir(id, NOTES_COLONNE, "Notes initiales");

        modifier(id, corps().avec(DESCRIPTION, DESCRIPTION_MODIFIEE)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(taches.texte(id, DESCRIPTION_COLONNE)).isEqualTo(DESCRIPTION_MODIFIEE),
                () -> assertThat(taches.guid(id, DECLENCHEUR_COLONNE)).isEqualTo(declencheur),
                () -> assertThat(taches.guid(id, GESTIONNAIRE_COLONNE)).isEqualTo(gestionnaire),
                () -> assertThat(taches.guid(id, TIER_COLONNE)).isEqualTo(tier),
                () -> assertThat(taches.date(id, DATE_COLONNE)).isEqualTo(DATE_SAISIE_MINUTE),
                () -> assertThat(taches.date(id, DATE_FIN_COLONNE)).isEqualTo("2031-03-10T10:30"),
                () -> assertThat(taches.texte(id, DUREE_COLONNE)).isEqualTo("3"),
                () -> assertThat(taches.texte(id, NOTES_COLONNE)).isEqualTo("Notes initiales"));
    }

    @Test
    void r8_chaqueChampPresent_ecrit() throws Exception {
        UUID declencheur = fixture.tier("DEC-" + jeton, "Declencheur");
        UUID gestionnaire = fixture.tier("GES-" + jeton, "Gestionnaire");
        UUID id = rendezVous("TOU");

        modifier(id, corps().avec(DECLENCHEUR_ID, declencheur).avec(GESTIONNAIRE_ID, gestionnaire).avec(TIER_ID, tier)
                .avec(DUREE_PREVUE, 4).avec(NOTES, "Notes " + jeton)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(taches.guid(id, DECLENCHEUR_COLONNE)).isEqualTo(declencheur),
                () -> assertThat(taches.guid(id, GESTIONNAIRE_COLONNE)).isEqualTo(gestionnaire),
                () -> assertThat(taches.guid(id, TIER_COLONNE)).isEqualTo(tier),
                () -> assertThat(taches.texte(id, DUREE_COLONNE)).isEqualTo("4"),
                () -> assertThat(taches.texte(id, NOTES_COLONNE)).isEqualTo("Notes " + jeton));
    }

    @Test
    void r8_userUpdateRenseignes_userCreateConserve() throws Exception {
        UUID id = rendezVous("TRA");

        modifier(id, corps().avec(NOTES, "Trace")).andExpect(status().isOk());

        assertAll(
                () -> assertThat(taches.texte(id, "UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(taches.date(id, "UserUpdateDateTime")).isNotNull(),
                () -> assertThat(taches.texte(id, "UserCreate")).isEqualTo(FixtureTachesAdmin.AUTEUR_OMRAN));
    }

    @Test
    void r8_sourceNeBougeJamais() throws Exception {
        UUID autreTier = fixture.tier("AUT-" + jeton, "Autre");
        UUID id = rendezVous("SRC");
        taches.definir(id, OBJET_COLONNE, tier);
        taches.definir(id, OBJET_LIBELLE_COLONNE, LIBELLE_SOURCE);
        taches.definir(id, OBJET_ENTITE_COLONNE, ENTITE_RENDEZ_VOUS);

        modifier(id, corps().avec(DESCRIPTION, DESCRIPTION_MODIFIEE)
                .avec("sourceId", autreTier).avec("sourceLibelle", CODE_ENVOYE).avec("sourceEntite", CODE_ENVOYE)
                .avec("objectId", autreTier).avec("objectLabel", CODE_ENVOYE).avec("objectStrEntityId", ENTITE_TACHE))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(taches.texte(id, DESCRIPTION_COLONNE)).isEqualTo(DESCRIPTION_MODIFIEE),
                () -> assertThat(taches.guid(id, OBJET_COLONNE)).isEqualTo(tier),
                () -> assertThat(taches.texte(id, OBJET_LIBELLE_COLONNE)).isEqualTo(LIBELLE_SOURCE),
                () -> assertThat(taches.guid(id, OBJET_ENTITE_COLONNE)).isEqualTo(ENTITE_RENDEZ_VOUS));
    }

    // ------------------------------------------------------------------ règle 9 : références inconnues

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {DECLENCHEUR_ID, GESTIONNAIRE_ID, TIER_ID})
    void r9_referenceInconnue_400_enCreation(String champ) throws Exception {
        creer(saisie().avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {DECLENCHEUR_ID, GESTIONNAIRE_ID, TIER_ID})
    void r9_referenceInconnue_400_enModification(String champ) throws Exception {
        UUID id = rendezVous("REF");

        modifier(id, corps().avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ règle 9 : rendez-vous inconnu ou tâche

    @Test
    void r9_lecture_404_inconnu_ouTache() throws Exception {
        UUID rendezVous = rendezVous("LEC");
        UUID tache = taches.tache("TAC-" + jeton);

        lire(rendezVous).andExpect(status().isOk());
        lire(UUID.randomUUID()).andExpect(status().isNotFound());
        lire(tache).andExpect(status().isNotFound());
    }

    @Test
    void r9_modification_404_inconnu_ouTache_inchangee() throws Exception {
        UUID rendezVous = rendezVous("ECR");
        UUID tache = taches.tache("TAC-" + jeton);
        CorpsJson corps = corps().avec(DESCRIPTION, DESCRIPTION_MODIFIEE);

        modifier(rendezVous, corps).andExpect(status().isOk());
        modifier(UUID.randomUUID(), corps).andExpect(status().isNotFound());
        modifier(tache, corps).andExpect(status().isNotFound());

        assertThat(taches.texte(tache, DESCRIPTION_COLONNE)).isEqualTo("Tache TAC-" + jeton);
    }

    @Test
    void r9_suppression_404_inconnu_ouTache_conservee() throws Exception {
        UUID rendezVous = rendezVous("SUP");
        UUID tache = taches.tache("TAC-" + jeton);

        supprimer(rendezVous).andExpect(status().isNoContent());
        supprimer(UUID.randomUUID()).andExpect(status().isNotFound());
        supprimer(tache).andExpect(status().isNotFound());

        assertThat(taches.lignes(tache)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règle 10

    @Test
    void r10_suppression_204_ligneAbsente() throws Exception {
        UUID id = rendezVous("DEL");

        supprimer(id).andExpect(status().isNoContent());

        assertThat(taches.lignes(id)).isZero();
    }
}
