package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statutsPermis;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transition;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transitionCorps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, E-B2 (transition de statut, {@code PATCH /api/achat/documents/{id}/statut}) et E-B6 ({@code actions.transitions}),
 * Vérification n° 3. Factures (aucune restriction « mes BC »).
 * <pre>
 *   gestion G : T01 S0→S1 (rôle de A) · T02 S0→S2 (rôle de A, note obligatoire) · T03 S0→S3 (déclencheur, aucun rôle)
 *               TRO SRO→S0 (rôle de A ; SRO en lecture seule)
 *   gestion G2 : S0→SX (rôle de A)    gestion G3 : aucune transition
 *   comptes : A (rôle lié), B (aucun rôle lié), ADM (Administrateur par AdmUserRole, aucun rôle lié)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatTransitionTest {

    private static final String NOTE = "Motif de la transition";
    private static final String STATUT_PIECE = "BuyDocumentStatusId";
    private static final String TRIGGER = "CfgTierTriggerId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;
    @Autowired private EntityManager em;

    private Cadre c;
    private UUID g;
    private UUID g3;
    private UUID s0;
    private UUID s1;
    private UUID s2;
    private UUID s3;
    private UUID sro;
    private UUID sx;
    private UUID t01;
    private AdmUser a;
    private AdmUser b;
    private AdmUser adm;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ATR");
        g = config.gestion("G-" + c.jeton(), "Gestion " + c.jeton());
        UUID g2 = config.gestion("G2-" + c.jeton(), "Gestion 2 " + c.jeton());
        g3 = config.gestion("G3-" + c.jeton(), "Gestion sans workflow " + c.jeton());
        s0 = nouveauStatut("S0", false);
        s1 = nouveauStatut("S1", false);
        s2 = nouveauStatut("S2", false);
        s3 = nouveauStatut("S3", false);
        sro = nouveauStatut("SRO", true);
        sx = nouveauStatut("SX", false);

        a = fixture.compteHabilite(c, "ATR-A", ROLE_JWT);
        b = fixture.compteHabilite(c, "ATR-B", ROLE_JWT);
        adm = fixture.compteHabilite(c, "ATR-ADM", ADMINISTRATEUR);
        UUID roleA = fixture.roleDe(a);

        t01 = config.transition(g, 1, "Valider", s0, s1, false, false);
        UUID t02 = config.transition(g, 2, "Annuler", s0, s2, true, false);
        config.transition(g, 3, "Demander", s0, s3, false, true);
        UUID tro = config.transition(g, 4, "Rouvrir", sro, s0, false, false);
        UUID t0x = config.transition(g2, 1, "Autre gestion", s0, sx, false, false);
        for (UUID t : List.of(t01, t02, tro, t0x)) {
            config.lier(t, roleA);
        }
    }

    private UUID nouveauStatut(String code, boolean lectureSeule) {
        return config.statut(new Statut(code + "-" + c.jeton(), "Statut " + code, 10, null, null, null, lectureSeule, null,
                null));
    }

    /** Facture de la gestion au statut donné (nul : statut NULL). */
    private UUID piece(UUID gestion, UUID statut) {
        UUID id = fixture.piece(c, "-F" + (rang++), FixtureAchat.FACTURE);
        fixture.poser(id, "StrEntityId", gestion);
        if (statut != null) {
            fixture.poser(id, STATUT_PIECE, statut);
        }
        return id;
    }

    private MvcResult patch(AdmUser qui, UUID piece, UUID cible, String note) throws Exception {
        return appeler(mockMvc, transition(piece, cible, note), connecte(qui, ROLE_JWT));
    }

    private String ficheDe(AdmUser qui, UUID piece) throws Exception {
        MvcResult r = appeler(mockMvc, fiche(piece), connecte(qui, ROLE_JWT));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    /** Lignes d'historique : statut, auteur, transition, note (identifiants en majuscules). */
    private List<Object[]> historique(UUID piece) {
        em.flush();
        List<?> lignes = em.createNativeQuery("""
                        SELECT CAST(BuyDocumentStatusId AS varchar(36)), CAST(AdmUserId AS varchar(36)),
                            CAST(buyDocumentWorkFlowId AS varchar(36)), Notes
                        FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id""")
                .setParameter("id", piece).getResultList();
        return lignes.stream().map(Object[].class::cast).toList();
    }

    private void assertRienEcrit(UUID piece, UUID statutAvant) {
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.lienDe(piece, STATUT_PIECE)).isEqualTo(statutAvant == null ? null : id(statutAvant)),
                () -> assertThat(historique(piece)).isEmpty());
    }

    // ------------------------------------------------------------------ règle 6 : écriture

    @Test
    void eb2_r6_roleAutorise_200_statutEtHistoriqueEcrits() throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = patch(a, piece, s1, null);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        List<Object[]> historique = historique(piece);
        assertAll(
                () -> assertThat(((String) JsonPath.read(corps(r), "$.statutId")).toUpperCase()).isEqualTo(id(s1)),
                () -> assertThat(fixture.lienDe(piece, STATUT_PIECE)).isEqualTo(id(s1)),
                () -> assertThat(historique).singleElement().satisfies(h -> assertAll(
                        () -> assertThat(h[0]).isEqualTo(id(s1)),
                        () -> assertThat(h[1]).isEqualTo(id(a.getId())),
                        () -> assertThat(h[2]).as("buyDocumentWorkFlowId").isEqualTo(id(t01)),
                        () -> assertThat(h[3]).as("Notes sans note saisie").isEqualTo(""))));
    }

    @Test
    void eb2_r6_userUpdatePose() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(a, piece, s1, null))).isEqualTo(200);

        em.flush();
        Object auteur = em.createNativeQuery("SELECT UserUpdate FROM BuyDocument WHERE PKey = :id").setParameter("id", piece)
                .getSingleResult();
        Object date = em.createNativeQuery("SELECT UserUpdateDateTime FROM BuyDocument WHERE PKey = :id")
                .setParameter("id", piece).getSingleResult();
        assertAll(() -> assertThat(auteur).isNotNull(), () -> assertThat(date).isNotNull());
    }

    @Test
    void eb2_r6_noteFacultativeSaisie_ecriteDansLHistorique() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(a, piece, s1, "Commentaire libre"))).isEqualTo(200);

        assertThat(historique(piece)).singleElement().satisfies(h -> assertThat(h[3]).isEqualTo("Commentaire libre"));
    }

    /** OMRAN l.141-148 : une pièce à statut IsReadOnly peut changer de statut. */
    @Test
    void eb2_r6_statutLectureSeule_peutEtreQuitte_200() throws Exception {
        UUID piece = piece(g, sro);

        MvcResult r = patch(a, piece, s0, null);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(fixture.lienDe(piece, STATUT_PIECE)).isEqualTo(id(s0));
    }

    /** Règle 6 : la transition retenue est la 1ʳᵉ par Sorting parmi les autorisées menant au statut demandé. */
    @Test
    void eb2_r6_transitionRetenue_premiereParSortingParmiLesAutorisees() throws Exception {
        UUID g4 = config.gestion("G4-" + c.jeton(), "Gestion 4 " + c.jeton());
        UUID roleA = fixture.roleDe(a);
        config.transition(g4, 1, "Non autorisée", s0, s1, false, false);
        UUID retenue = config.transition(g4, 2, "Autorisée 1", s0, s1, false, false);
        UUID suivante = config.transition(g4, 3, "Autorisée 2", s0, s1, false, false);
        config.lier(retenue, roleA);
        config.lier(suivante, roleA);
        UUID piece = piece(g4, s0);

        assertThat(statut(patch(a, piece, s1, null))).isEqualTo(200);

        assertThat(historique(piece)).singleElement().satisfies(h -> assertThat(h[2]).isEqualTo(id(retenue)));
    }

    // ------------------------------------------------------------------ règle 1 : 404

    @Test
    void eb2_r1_pieceInconnue_404PieceIntrouvable() throws Exception {
        MvcResult r = patch(a, UUID.randomUUID(), s1, null);

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(INTROUVABLE);
    }

    @Test
    void eb2_r1_pieceInconnue_corpsSansStatut_404AvantLe400() throws Exception {
        MvcResult r = appeler(mockMvc, transitionCorps(UUID.randomUUID(), "{}"), connecte(a, ROLE_JWT));

        assertThat(statut(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(INTROUVABLE);
    }

    // ------------------------------------------------------------------ règle 2 : 400 statutId absent

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"note\":\"x\"}", "{\"statutId\":null}"})
    void eb2_r2_statutIdAbsent_400_rienEcrit(String json) throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = appeler(mockMvc, transitionCorps(piece, json), connecte(a, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertRienEcrit(piece, s0);
    }

    // ------------------------------------------------------------------ règle 3 : 409

    @Test
    void eb2_r3_cibleDUneAutreGestion_409_avecTransitionsPermises() throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = patch(a, piece, sx, NOTE);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        String permises = String.valueOf((Object) JsonPath.read(corps(r), "$.transitionsPermises")).toUpperCase();
        assertThat(permises).as("transitionsPermises du compte A depuis S0").containsAnyOf(id(s1),
                ("S1-" + c.jeton()).toUpperCase());
        assertRienEcrit(piece, s0);
    }

    @Test
    void eb2_r3_cibleInaccessibleDepuisLeStatutCourant_409() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(a, piece, sro, NOTE))).isEqualTo(409);
        assertRienEcrit(piece, s0);
    }

    /** Devis : gestion sans aucune transition, statut NULL (9 devis en prod). */
    @Test
    void eb2_r3_pieceSansWorkflow_statutNull_409_memeAdministrateur() throws Exception {
        UUID devis = fixture.piece(c, "-DEVIS", FixtureAchat.DEVIS);
        fixture.poser(devis, "StrEntityId", g3);

        MvcResult r = patch(adm, devis, s1, NOTE);

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertRienEcrit(devis, null);
    }

    @Test
    void eb2_r3_statutCourantNull_dansUneGestionAWorkflow_409() throws Exception {
        UUID piece = piece(g, null);

        assertThat(statut(patch(a, piece, s1, NOTE))).isEqualTo(409);
        assertRienEcrit(piece, null);
    }

    /** Ordre : la règle 3 (aucune transition, tout rôle confondu) passe avant le 403. */
    @Test
    void eb2_r3_avantR4_compteSansRole_cibleHorsWorkflow_409() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(b, piece, sx, NOTE))).isEqualTo(409);
    }

    // ------------------------------------------------------------------ règle 4 : 403

    @Test
    void eb2_r4_compteSansRoleDeLaTransition_403_rienEcrit() throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = patch(b, piece, s1, NOTE);

        assertThat(statut(r)).as(corps(r)).isEqualTo(403);
        assertRienEcrit(piece, s0);
    }

    /** Q-B : Administrateur sans exemption — sans rôle de la transition, refusé. */
    @Test
    void eb2_r4_qB_administrateurSansRoleDeTransition_403() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(adm, piece, s1, NOTE))).isEqualTo(403);
        assertRienEcrit(piece, s0);
    }

    /** Ordre : 403 avant le 400 de note obligatoire. */
    @Test
    void eb2_r4_avantR5_compteSansRole_noteObligatoireAbsente_403() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(b, piece, s2, null))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ voie déclencheur

    @Test
    void eb2_declencheur_employeDuCompte_autoriseSansRole_200() throws Exception {
        UUID piece = piece(g, s0);
        fixture.poser(piece, TRIGGER, b.getCfgEmployee().getId());

        MvcResult r = patch(b, piece, s3, null);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(fixture.lienDe(piece, STATUT_PIECE)).isEqualTo(id(s3));
    }

    @Test
    void eb2_declencheur_autreEmploye_403() throws Exception {
        UUID piece = piece(g, s0);
        fixture.poser(piece, TRIGGER, a.getCfgEmployee().getId());

        assertThat(statut(patch(b, piece, s3, null))).isEqualTo(403);
        assertRienEcrit(piece, s0);
    }

    /** OMRAN l.527 : sans déclencheur sur la pièce, seule la voie des rôles reste (T03 n'a aucun rôle). */
    @Test
    void eb2_declencheur_pieceSansDeclencheur_rolesSeuls_403() throws Exception {
        UUID piece = piece(g, s0);

        assertAll(
                () -> assertThat(statut(patch(b, piece, s3, null))).isEqualTo(403),
                () -> assertThat(statut(patch(a, piece, s3, null))).isEqualTo(403));
    }

    // ------------------------------------------------------------------ règle 5 : note obligatoire

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void eb2_r5_noteObligatoireVide_400_rienEcrit(String note) throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = patch(a, piece, s2, note);

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertRienEcrit(piece, s0);
    }

    @Test
    void eb2_r5_noteObligatoireAbsente_400() throws Exception {
        UUID piece = piece(g, s0);

        assertThat(statut(patch(a, piece, s2, null))).isEqualTo(400);
        assertRienEcrit(piece, s0);
    }

    @Test
    void eb2_r5_noteObligatoireSaisie_200_noteDansLHistorique() throws Exception {
        UUID piece = piece(g, s0);

        MvcResult r = patch(a, piece, s2, NOTE);

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(historique(piece)).singleElement().satisfies(h -> assertThat(h[3]).isEqualTo(NOTE));
    }

    // ------------------------------------------------------------------ E-B6 actions.transitions

    @Test
    void eb6_actions_compteARole_cibleS1EtS2_noteObligatoireSurS2() throws Exception {
        UUID piece = piece(g, s0);

        String json = ficheDe(a, piece);

        List<Boolean> noteS1 = JsonPath.read(json, "$.actions.transitions[?(@.statutId == '" + s1 + "')].noteObligatoire");
        List<Boolean> noteS2 = JsonPath.read(json, "$.actions.transitions[?(@.statutId == '" + s2 + "')].noteObligatoire");
        assertAll(
                () -> assertThat(statutsPermis(json)).containsExactlyInAnyOrder(id(s1), id(s2)),
                () -> assertThat(noteS1).containsExactly(false),
                () -> assertThat(noteS2).containsExactly(true));
    }

    @Test
    void eb6_actions_declencheur_seuleS3_administrateurAucune() throws Exception {
        UUID piece = piece(g, s0);
        fixture.poser(piece, TRIGGER, b.getCfgEmployee().getId());

        String pourB = ficheDe(b, piece);
        String pourAdm = ficheDe(adm, piece);

        assertAll(
                () -> assertThat(statutsPermis(pourB)).containsExactly(id(s3)),
                () -> assertThat(statutsPermis(pourAdm)).isEmpty());
    }

    @Test
    void eb6_actions_statutsDArriveeDistincts() throws Exception {
        UUID g4 = config.gestion("G4-" + c.jeton(), "Gestion 4 " + c.jeton());
        UUID roleA = fixture.roleDe(a);
        config.lier(config.transition(g4, 1, "Voie 1", s0, s1, false, false), roleA);
        config.lier(config.transition(g4, 2, "Voie 2", s0, s1, false, false), roleA);
        UUID piece = piece(g4, s0);

        String json = ficheDe(a, piece);

        assertThat(statutsPermis(json)).containsExactly(id(s1));
    }

    /**
     * Invariant (Vérification n° 3) : pour chaque compte, chaque pièce du jeu et chaque statut candidat,
     * {@code PATCH} répond 200 ⇔ le statut figure dans {@code actions.transitions} de la fiche.
     */
    @Test
    void eb6_invariant_actionsTransitions_egalEnsembleAccepteParPatch() throws Exception {
        UUID declenchee = piece(g, s0);
        fixture.poser(declenchee, TRIGGER, b.getCfgEmployee().getId());
        Map<UUID, UUID> pieces = new LinkedHashMap<>();
        pieces.put(declenchee, s0);
        pieces.put(piece(g, sro), sro);
        pieces.put(piece(g, null), null);
        pieces.put(piece(g3, null), null);
        List<UUID> candidats = List.of(s0, s1, s2, s3, sro, sx);
        List<String> ecarts = new ArrayList<>();
        int succes = 0;

        for (AdmUser qui : List.of(a, b, adm)) {
            for (Map.Entry<UUID, UUID> p : pieces.entrySet()) {
                List<String> permis = statutsPermis(ficheDe(qui, p.getKey()));
                for (UUID cible : candidats) {
                    int code = statut(patch(qui, p.getKey(), cible, NOTE));
                    if ((code == 200) != permis.contains(id(cible))) {
                        ecarts.add("%s / %s → %s : PATCH %d, actions %s".formatted(qui.getCode(), p.getKey(), cible, code,
                                permis));
                    }
                    if (code == 200) {
                        succes++;
                        remettre(p.getKey(), p.getValue());
                    }
                }
            }
        }

        assertThat(succes).as("au moins une transition acceptée dans le jeu").isPositive();
        assertThat(ecarts).isEmpty();
    }

    private void remettre(UUID piece, UUID statut) {
        if (statut == null) {
            fixture.effacer(piece, STATUT_PIECE);
        } else {
            fixture.poser(piece, STATUT_PIECE, statut);
        }
        fixture.oublier();
    }
}
