package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Transitions éditables — FEATURE-statuts-achat-parite.md « Réponses du manager (12/11, 2ᵉ passe) » n° 1 : POST 201 /
 * PUT 200 / DELETE 204 relus par GET /transitions ; 400 (id mal formé, statut inconnu, départ = arrivée), 404, 409 (couple
 * départ-arrivée déjà présent, un PUT ne se compare pas à soi), 403 sans {@code Yes.Buy.Front.Config.BuyDocumentStatus}
 * avant tout 400 / 409 et sans écriture ; transition citée par l'historique supprimable (aucune FK). Corps tirés des noms
 * du DTO de lecture {@code TransitionAchatDto} (ids des statuts à la place des codes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransitionsAchatEcritureTest {

    private static final String TRANSITION = TRANSITIONS + "/{id}";
    private static final String ORDRE = "ordre";
    private static final String DESCRIPTION = "description";
    private static final String DEPART = "statutDepartId";
    private static final String ARRIVEE = "statutArriveeId";
    private static final String NOTE = "noteObligatoire";
    private static final String DECLENCHEUR = "valideeParDeclencheur";
    private static final String NOMBRE_PASSAGES = "nombrePassages";
    private static final String GESTION = "gestionId";
    private static final String ROLES_ECRITS = "roleIds";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID enCours;
    private UUID valide;
    private UUID cloture;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2W");
        admin = statuts.administrateur(jeton);
        enCours = config.statut(Statut.simple("EC-" + jeton, 1));
        valide = config.statut(Statut.simple("VA-" + jeton, 2));
        cloture = config.statut(Statut.simple("CL-" + jeton, 3));
    }

    // ------------------------------------------------------------------ outils

    private Map<String, Object> corps(UUID depart, UUID arrivee, String description) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put(ORDRE, 4);
        c.put(DESCRIPTION, description);
        c.put(DEPART, depart.toString());
        c.put(ARRIVEE, arrivee.toString());
        c.put(NOTE, true);
        c.put(DECLENCHEUR, false);
        return c;
    }

    /** Corps avec {@code gestionId} (null = sans gestion) et {@code roleIds} (réponses 2ᵉ passe n° 7). */
    private Map<String, Object> corps(UUID depart, UUID arrivee, String description, UUID gestion, UUID... roles) {
        Map<String, Object> c = corps(depart, arrivee, description);
        c.put(GESTION, gestion == null ? null : gestion.toString());
        c.put(ROLES_ECRITS, java.util.Arrays.stream(roles).map(UUID::toString).toList());
        return c;
    }

    /** Ids des rôles relus par GET /transitions ({@code roles[].id}), en minuscules. */
    @SuppressWarnings("unchecked")
    private List<String> rolesLus(UUID transition) throws Exception {
        Map<String, Object> t = relire(transition);
        assertThat(t).isNotNull();
        return ((List<Map<String, Object>>) t.get("roles")).stream()
                .map(r -> r.get("id").toString().toLowerCase()).toList();
    }

    private MockHttpServletRequestBuilder creation(Map<String, Object> corps) {
        return post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    private MockHttpServletRequestBuilder modification(Object id, Map<String, Object> corps) {
        return put(TRANSITION, id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    private UUID creer(Map<String, Object> corps) throws Exception {
        return id(mockMvc.perform(creation(corps).with(admin)).andExpect(status().isCreated()).andReturn());
    }

    /** Ligne relue par GET /transitions, ids des statuts en minuscules (SQL Server les rend parfois en majuscules). */
    private Map<String, Object> relire(UUID id) throws Exception {
        Map<String, Object> l = ligne(lire(mockMvc, TRANSITIONS, admin), id);
        if (l == null) {
            return null;
        }
        Map<String, Object> copie = new LinkedHashMap<>(l);
        copie.replaceAll((k, v) -> (DEPART.equals(k) || ARRIVEE.equals(k)) && v != null ? v.toString().toLowerCase() : v);
        return copie;
    }

    private int transitionsDecrites(String description) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM BuyDocumentWorkFlow WHERE Description = ?", Integer.class,
                description);
        return n == null ? 0 : n;
    }

    private int compter(String sql, UUID id) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }

    private int transitionsParId(UUID id) {
        return compter("SELECT COUNT(*) FROM BuyDocumentWorkFlow WHERE PKey = ?", id);
    }

    // ------------------------------------------------------------------ 201 / 200 / 204

    /** POST → 201, relue par GET /transitions avec chaque champ du corps. */
    @Test
    void creation_201_relueParGetTransitions() throws Exception {
        String description = "Valider " + jeton;

        UUID id = creer(corps(enCours, valide, description));

        Map<String, Object> t = relire(id);
        assertThat(t).isNotNull()
                .containsEntry(ORDRE, 4)
                .containsEntry(DESCRIPTION, description)
                .containsEntry(DEPART, enCours.toString())
                .containsEntry(ARRIVEE, valide.toString())
                .containsEntry("statutDepartCode", "EC-" + jeton)
                .containsEntry("statutArriveeCode", "VA-" + jeton)
                .containsEntry(NOTE, true)
                .containsEntry(DECLENCHEUR, false)
                .containsEntry(NOMBRE_PASSAGES, 0);
    }

    /** POST répond la transition créée. */
    @Test
    void creation_reponseEstLaTransitionCreee() throws Exception {
        String description = "Réponse " + jeton;

        MvcResult r = mockMvc.perform(creation(corps(enCours, valide, description)).with(admin))
                .andExpect(status().isCreated()).andReturn();

        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"description\":\"" + description + "\"");
    }

    /** PUT → 200, les champs modifiés (arrivée, ordre, description, drapeaux) sont relus. */
    @Test
    void modification_200_relue() throws Exception {
        UUID id = creer(corps(enCours, valide, "Avant " + jeton));
        Map<String, Object> c = corps(enCours, cloture, "Après " + jeton);
        c.put(ORDRE, 9);
        c.put(NOTE, false);
        c.put(DECLENCHEUR, true);

        mockMvc.perform(modification(id, c).with(admin)).andExpect(status().isOk());

        assertThat(relire(id)).isNotNull()
                .containsEntry(ORDRE, 9)
                .containsEntry(DESCRIPTION, "Après " + jeton)
                .containsEntry(ARRIVEE, cloture.toString())
                .containsEntry(NOTE, false)
                .containsEntry(DECLENCHEUR, true);
    }

    /** DELETE → 204, absente de GET /transitions. */
    @Test
    void suppression_204_absenteDeLaListe() throws Exception {
        UUID id = creer(corps(enCours, valide, "Supprimer " + jeton));

        mockMvc.perform(delete(TRANSITION, id).with(admin)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(transitionsParId(id)).isZero(),
                () -> assertThat(relire(id)).isNull());
    }

    /** Citée par l'historique (aucune FK BuyDocumentStatusHistory → BuyDocumentWorkFlow) → 204. */
    @Test
    void suppression_citeeParLHistorique_204() throws Exception {
        UUID t = config.transition(null, 1, "Historique " + jeton, enCours, valide);
        UUID piece = config.piece(valide, null);
        config.passage(piece, t, valide, LocalDateTime.of(2022, 3, 1, 10, 0));

        mockMvc.perform(delete(TRANSITION, t).with(admin)).andExpect(status().isNoContent());

        assertThat(transitionsParId(t)).isZero();
    }

    /** Transition à rôles : la FK BuyDocumentWorkFlowAdmRole (NO_ACTION, présente en CI) impose d'ôter les liens → 204. */
    @Test
    void suppression_avecRoles_204_liensSupprimes() throws Exception {
        UUID t = config.transition(null, 1, "Roles " + jeton, enCours, valide);
        config.lier(t, config.role("RW-" + jeton));

        mockMvc.perform(delete(TRANSITION, t).with(admin)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(transitionsParId(t)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentWorkFlowAdmRole WHERE WorkFlowId = ?", t)).isZero());
    }

    // ------------------------------------------------------------------ 400 / 404

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void idMalForme_400(String methode) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(methode), TRANSITION, "pas-un-uuid").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corps(enCours, valide, "Mal formé " + jeton))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creation_statutDepartInconnu_400_sansEcriture() throws Exception {
        String description = "Départ inconnu " + jeton;

        mockMvc.perform(creation(corps(UUID.randomUUID(), valide, description)).with(admin)).andExpect(status().isBadRequest());

        assertThat(transitionsDecrites(description)).isZero();
    }

    @Test
    void creation_statutArriveeInconnu_400_sansEcriture() throws Exception {
        String description = "Arrivée inconnue " + jeton;

        mockMvc.perform(creation(corps(enCours, UUID.randomUUID(), description)).with(admin)).andExpect(status().isBadRequest());

        assertThat(transitionsDecrites(description)).isZero();
    }

    @Test
    void creation_departEgalArrivee_400_sansEcriture() throws Exception {
        String description = "Boucle " + jeton;

        mockMvc.perform(creation(corps(enCours, enCours, description)).with(admin)).andExpect(status().isBadRequest());

        assertThat(transitionsDecrites(description)).isZero();
    }

    @Test
    void modification_departEgalArrivee_400_inchangee() throws Exception {
        UUID id = creer(corps(enCours, valide, "Stable " + jeton));

        mockMvc.perform(modification(id, corps(valide, valide, "Boucle " + jeton)).with(admin))
                .andExpect(status().isBadRequest());

        assertThat(relire(id)).isNotNull().containsEntry(DESCRIPTION, "Stable " + jeton)
                .containsEntry(DEPART, enCours.toString());
    }

    @Test
    void modification_statutInconnu_400_inchangee() throws Exception {
        UUID id = creer(corps(enCours, valide, "Stable " + jeton));

        mockMvc.perform(modification(id, corps(enCours, UUID.randomUUID(), "Inconnu " + jeton)).with(admin))
                .andExpect(status().isBadRequest());

        assertThat(relire(id)).isNotNull().containsEntry(ARRIVEE, valide.toString());
    }

    /** 404 sur id inconnu ; témoin 200 sur une transition existante (la route existe). */
    @Test
    void modification_idInconnu_404_temoin200() throws Exception {
        UUID id = creer(corps(enCours, valide, "Témoin " + jeton));

        mockMvc.perform(modification(id, corps(enCours, valide, "Témoin " + jeton)).with(admin)).andExpect(status().isOk());
        mockMvc.perform(modification(UUID.randomUUID(), corps(enCours, cloture, "Inconnue " + jeton)).with(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_idInconnu_404_temoin204() throws Exception {
        UUID id = creer(corps(enCours, valide, "Témoin " + jeton));

        mockMvc.perform(delete(TRANSITION, id).with(admin)).andExpect(status().isNoContent());
        mockMvc.perform(delete(TRANSITION, UUID.randomUUID()).with(admin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ 409 couple (départ, arrivée)

    @Test
    void creation_coupleDejaPresent_409_sansEcriture() throws Exception {
        config.transition(null, 1, "Existante " + jeton, enCours, valide);
        String description = "Doublon " + jeton;

        mockMvc.perform(creation(corps(enCours, valide, description)).with(admin)).andExpect(status().isConflict());

        assertThat(transitionsDecrites(description)).isZero();
    }

    /** Un PUT qui garde son propre couple ne se compare pas à lui-même. */
    @Test
    void modification_memeCouple_seComparePasASoi_200() throws Exception {
        UUID id = creer(corps(enCours, valide, "Soi " + jeton));

        mockMvc.perform(modification(id, corps(enCours, valide, "Soi renommée " + jeton)).with(admin))
                .andExpect(status().isOk());
    }

    @Test
    void modification_coupleDUneAutre_409_inchangee() throws Exception {
        config.transition(null, 1, "Autre " + jeton, enCours, cloture);
        UUID id = creer(corps(enCours, valide, "Mienne " + jeton));

        mockMvc.perform(modification(id, corps(enCours, cloture, "Mienne " + jeton)).with(admin))
                .andExpect(status().isConflict());

        assertThat(relire(id)).isNotNull().containsEntry(ARRIVEE, valide.toString());
    }

    // ------------------------------------------------------------------ gestion et rôles (2ᵉ passe n° 7)

    /** N° 7 : gestionId et roleIds écrits au POST, relus par GET /transitions (gestionCode, roles[]). */
    @Test
    void p7_creation_gestionEtRoles_relus() throws Exception {
        UUID gestion = config.gestion("GW-" + jeton, "Gestion " + jeton);
        UUID r1 = config.role("RA-" + jeton);
        UUID r2 = config.role("RB-" + jeton);

        UUID id = creer(corps(enCours, valide, "Gérée " + jeton, gestion, r1, r2));

        assertAll(
                () -> assertThat(relire(id)).isNotNull().containsEntry("gestionCode", "GW-" + jeton),
                () -> assertThat(rolesLus(id)).containsExactlyInAnyOrder(r1.toString(), r2.toString()));
    }

    /** N° 7 : le PUT remplace les rôles en bloc. */
    @Test
    void p7_modification_roleIds_remplacesEnBloc() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID r2 = config.role("RB-" + jeton);
        UUID r3 = config.role("RC-" + jeton);
        UUID id = creer(corps(enCours, valide, "Rôles " + jeton, null, r1, r2));

        mockMvc.perform(modification(id, corps(enCours, valide, "Rôles " + jeton, null, r3)).with(admin))
                .andExpect(status().isOk());

        assertThat(rolesLus(id)).containsExactly(r3.toString());
    }

    /** N° 7 : roleIds vide = aucun rôle. */
    @Test
    void p7_modification_roleIdsVide_aucunRole() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID id = creer(corps(enCours, valide, "Rôles " + jeton, null, r1));

        mockMvc.perform(modification(id, corps(enCours, valide, "Rôles " + jeton, null)).with(admin))
                .andExpect(status().isOk());

        assertThat(rolesLus(id)).isEmpty();
    }

    @Test
    void p7_creation_gestionInconnue_400_sansEcriture() throws Exception {
        String description = "Gestion inconnue " + jeton;

        mockMvc.perform(creation(corps(enCours, valide, description, UUID.randomUUID())).with(admin))
                .andExpect(status().isBadRequest());

        assertThat(transitionsDecrites(description)).isZero();
    }

    @Test
    void p7_creation_roleInconnu_400_sansEcriture() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        String description = "Rôle inconnu " + jeton;

        mockMvc.perform(creation(corps(enCours, valide, description, null, r1, UUID.randomUUID())).with(admin))
                .andExpect(status().isBadRequest());

        assertThat(transitionsDecrites(description)).isZero();
    }

    /** N° 7 : rôle inconnu au PUT → 400, rôles et description inchangés. */
    @Test
    void p7_modification_roleInconnu_400_inchangee() throws Exception {
        UUID r1 = config.role("RA-" + jeton);
        UUID id = creer(corps(enCours, valide, "Stable " + jeton, null, r1));

        mockMvc.perform(modification(id, corps(enCours, valide, "Changée " + jeton, null, UUID.randomUUID())).with(admin))
                .andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(relire(id)).isNotNull().containsEntry(DESCRIPTION, "Stable " + jeton),
                () -> assertThat(rolesLus(id)).containsExactly(r1.toString()));
    }

    /** N° 7 : unicité sur (gestion, départ, arrivée) — même couple dans une autre gestion → 201. */
    @Test
    void p7_memeCouple_autreGestion_201() throws Exception {
        UUID g1 = config.gestion("G1-" + jeton, "Gestion 1 " + jeton);
        UUID g2 = config.gestion("G2-" + jeton, "Gestion 2 " + jeton);
        config.transition(g1, 1, "Existante " + jeton, enCours, valide);

        mockMvc.perform(creation(corps(enCours, valide, "Autre gestion " + jeton, g2)).with(admin))
                .andExpect(status().isCreated());
    }

    /** N° 7 : même couple dans la même gestion → 409, rien d'écrit. */
    @Test
    void p7_memeCouple_memeGestion_409_sansEcriture() throws Exception {
        UUID g1 = config.gestion("G1-" + jeton, "Gestion 1 " + jeton);
        config.transition(g1, 1, "Existante " + jeton, enCours, valide);
        String description = "Même gestion " + jeton;

        mockMvc.perform(creation(corps(enCours, valide, description, g1)).with(admin)).andExpect(status().isConflict());

        assertThat(transitionsDecrites(description)).isZero();
    }

    // ------------------------------------------------------------------ 403 (mêmes droits que les statuts)

    @Test
    void creation_sansLeDroit_403_sansEcriture_temoin201() throws Exception {
        RequestPostProcessor sansDroit = statuts.compte(jeton + "N");
        RequestPostProcessor avecDroit = statuts.compte(jeton + "O", FixtureStatutsAchat.DROIT_STATUTS);
        String description = "Droit " + jeton;

        mockMvc.perform(creation(corps(enCours, valide, description)).with(sansDroit)).andExpect(status().isForbidden());
        int apresRefus = transitionsDecrites(description);
        mockMvc.perform(creation(corps(enCours, valide, description)).with(avecDroit)).andExpect(status().isCreated());

        assertAll(
                () -> assertThat(apresRefus).isZero(),
                () -> assertThat(transitionsDecrites(description)).isEqualTo(1));
    }

    @Test
    void modification_sansLeDroit_403_inchangee_temoin200() throws Exception {
        UUID t = config.transition(null, 1, "Initiale " + jeton, enCours, valide);
        RequestPostProcessor sansDroit = statuts.compte(jeton + "N");
        RequestPostProcessor avecDroit = statuts.compte(jeton + "O", FixtureStatutsAchat.DROIT_STATUTS);

        mockMvc.perform(modification(t, corps(enCours, valide, "Pirate " + jeton)).with(sansDroit))
                .andExpect(status().isForbidden());
        Map<String, Object> apresRefus = relire(t);
        mockMvc.perform(modification(t, corps(enCours, valide, "Légitime " + jeton)).with(avecDroit))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(apresRefus).isNotNull().containsEntry(DESCRIPTION, "Initiale " + jeton),
                () -> assertThat(relire(t)).isNotNull().containsEntry(DESCRIPTION, "Légitime " + jeton));
    }

    @Test
    void suppression_sansLeDroit_403_conservee_temoin204() throws Exception {
        UUID t = config.transition(null, 1, "Gardée " + jeton, enCours, valide);
        RequestPostProcessor sansDroit = statuts.compte(jeton + "N");
        RequestPostProcessor avecDroit = statuts.compte(jeton + "O", FixtureStatutsAchat.DROIT_STATUTS);

        mockMvc.perform(delete(TRANSITION, t).with(sansDroit)).andExpect(status().isForbidden());
        int apresRefus = transitionsParId(t);
        mockMvc.perform(delete(TRANSITION, t).with(avecDroit)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(apresRefus).isEqualTo(1),
                () -> assertThat(transitionsParId(t)).isZero());
    }

    /** Ordre 403 → 400 → 409 : un compte sans droit reçoit 403 sur un corps invalide (départ = arrivée) ou en double. */
    @Test
    void sansLeDroit_corpsInvalideOuEnDouble_403() throws Exception {
        config.transition(null, 1, "Existante " + jeton, enCours, valide);
        RequestPostProcessor sansDroit = statuts.compte(jeton + "N");

        mockMvc.perform(creation(corps(enCours, enCours, "Boucle " + jeton)).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(creation(corps(enCours, valide, "Doublon " + jeton)).with(sansDroit)).andExpect(status().isForbidden());
    }

    /** Ordre 404 → 403 : sans droit, un id inconnu répond 404 ; témoin : 403 sur une transition existante. */
    @Test
    void sansLeDroit_idInconnu_404_temoin403() throws Exception {
        UUID t = config.transition(null, 1, "Existante " + jeton, enCours, valide);
        RequestPostProcessor sansDroit = statuts.compte(jeton + "N");
        List<MockHttpServletRequestBuilder> existantes = List.of(modification(t, corps(enCours, valide, "X " + jeton)),
                delete(TRANSITION, t));
        List<MockHttpServletRequestBuilder> inconnues = List.of(
                modification(UUID.randomUUID(), corps(enCours, cloture, "Y " + jeton)), delete(TRANSITION, UUID.randomUUID()));

        for (MockHttpServletRequestBuilder r : existantes) {
            mockMvc.perform(r.with(sansDroit)).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder r : inconnues) {
            mockMvc.perform(r.with(sansDroit)).andExpect(status().isNotFound());
        }
    }
}
