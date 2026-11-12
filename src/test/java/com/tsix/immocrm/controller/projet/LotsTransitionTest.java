package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E7, R7 — FEATURE-lots-liste-parite.md. Statuts de test A, B, C du genre Entrepreneurs ; transitions A→B, A→C, B→C ; lot
 * sur A. Compte libre (rôle {@code WithSequenceFreeRestriction = 0}) : toutes les transitions partant de A. Compte restreint
 * (drapeau à 1) : intervenant de son rôle sur le statut A (visibilité) et sur la seule transition A→B
 * ({@code StrWorkFlowIntervener}, {@code StrStatus.cs:GetNextWorkFlowList}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsTransitionTest {

    private static final String IDS = "$[*].id";
    private static final String STATUT_DU_LOT = "SELECT COUNT(*) FROM PrjMarket WHERE PKey = ? AND StrStatusId = ?";
    private static final String HISTORIQUE = "SELECT * FROM StrHistory WHERE ObjectId = ? AND StrWorkflowId IS NOT NULL";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID statutA;
    private UUID statutB;
    private UUID statutC;
    private UUID ab;
    private UUID ac;
    private UUID bc;
    private UUID lot;
    private UUID lotInvisible;
    private AdmUser libre;
    private RequestPostProcessor qLibre;
    private RequestPostProcessor qRestreint;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        UUID entite = fixture.entiteJuridique("TE-" + jeton);
        UUID projet = fixture.projet("TRP-" + jeton, entite);
        UUID invisible = fixture.projet("TRX-" + jeton, entite);
        statutA = fixture.statut(ENTREPRENEURS, "TA-" + jeton);
        statutB = fixture.statut(ENTREPRENEURS, "TB-" + jeton);
        statutC = fixture.statut(ENTREPRENEURS, "TC-" + jeton);
        ab = fixture.transition(ENTREPRENEURS, statutA, statutB, "A>B " + jeton);
        ac = fixture.transition(ENTREPRENEURS, statutA, statutC, "A>C " + jeton);
        bc = fixture.transition(ENTREPRENEURS, statutB, statutC, "B>C " + jeton);
        lot = fixture.lot(projet, "TL-" + jeton, ENTREPRENEURS, statutA);
        lotInvisible = fixture.lot(invisible, "TX-" + jeton, ENTREPRENEURS, statutA);
        libre = fixture.avecDroits("lots.tr.libre." + jeton, List.of(), projet);
        qLibre = connecte(libre, "Commercial");
        AdmUser restreint = fixture.restreint("lots.tr.restr." + jeton, projet);
        fixture.intervenant(ENTREPRENEURS, fixture.roleDe(restreint), statutA, ab);
        qRestreint = connecte(restreint, "Commercial");
    }

    private int surStatut(UUID statut) {
        return fixture.compter(STATUT_DU_LOT, lot.toString(), statut.toString());
    }

    // ------------------------------------------------------------------ GET transitions

    @Test
    void e7_compteLibre_transitionsPartantDuStatutActuel() throws Exception {
        Reponse r = appels.transitions(qLibre, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactlyInAnyOrder(ab.toString(), ac.toString()).doesNotContain(bc.toString());
    }

    @Test
    void e7_transition_porteLeStatutDArrivee() throws Exception {
        Reponse r = appels.transitions(qLibre, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String filtre = "$[?(@.id == '" + ab + "')].";
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(filtre + "libelle")).containsExactly("A>B " + jeton);
            s.assertThat(r.ids(filtre + "statutArriveeId")).containsExactly(statutB.toString());
            s.assertThat(r.ids(filtre + "statutArriveeLibelle")).containsExactly("TB-" + jeton);
            s.assertThat(r.ids(filtre + "statutArriveeCouleur")).singleElement().isNotNull();
        });
    }

    @Test
    void e7_compteRestreint_seulementLesTransitionsDeSonIntervenant() throws Exception {
        Reponse r = appels.transitions(qRestreint, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(ab.toString());
    }

    // ------------------------------------------------------------------ R7 : exécutable = proposé

    @Test
    void r7_compteRestreint_transitionNonProposee_409_statutInchange() throws Exception {
        Reponse r = appels.executer(qRestreint, lot, ac);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(surStatut(statutA)).isEqualTo(1);
        });
    }

    @Test
    void r7_compteRestreint_transitionProposee_200() throws Exception {
        Reponse r = appels.executer(qRestreint, lot, ab);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(surStatut(statutB)).isEqualTo(1);
        });
    }

    @Test
    void r7_compteLibre_chaqueTransitionProposeeEstExecutable() throws Exception {
        List<String> proposees = appels.transitions(qLibre, lot).ids(IDS);
        assertThat(proposees).isNotEmpty();

        SoftAssertions s = new SoftAssertions();
        for (String id : proposees) {
            fixture.statutDuLot(lot, statutA);
            s.assertThat(appels.executer(qLibre, lot, UUID.fromString(id)).statut()).as(id).isEqualTo(200);
        }
        s.assertAll();
    }

    @Test
    void e7_transitionNePartantPasDuStatutActuel_409_statutInchange() throws Exception {
        Reponse r = appels.executer(qLibre, lot, bc);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(surStatut(statutA)).isEqualTo(1);
            s.assertThat(fixture.lignes(HISTORIQUE, lot.toString())).as("aucun historique").isEmpty();
        });
    }

    @Test
    void e7_transitionInconnue_409() throws Exception {
        assertThat(appels.executer(qLibre, lot, UUID.randomUUID()).statut()).isEqualTo(409);
    }

    // ------------------------------------------------------------------ exécution : ligne, statut, historique

    @Test
    void e7_execution_200_ligneDeListeAuNouveauStatut() throws Exception {
        Reponse r = appels.executer(qLibre, lot, ab);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.corps()).contains(lot.toString(), "TB-" + jeton);
            s.assertThat(surStatut(statutB)).isEqualTo(1);
        });
    }

    @Test
    void e7_execution_ligneStrHistoryAuFormatK1() throws Exception {
        assertThat(appels.executer(qLibre, lot, ab).statut()).isEqualTo(200);

        List<Map<String, Object>> lignes = fixture.lignes(HISTORIQUE, lot.toString());
        assertThat(lignes).singleElement().satisfies(h -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(h.get("StrWorkflowId"))).isEqualTo(guid(ab));
            s.assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(statutB));
            s.assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(GENRES.get(ENTREPRENEURS)));
            s.assertThat(h.get("Description"))
                    .isEqualTo("A>B " + jeton + ": TA-" + jeton + "=>TB-" + jeton + "(" + libre.getCode() + " & )");
            s.assertThat(h.get("DocDate")).isNotNull();
        }));
    }

    // ------------------------------------------------------------------ 404 (R3)

    @Test
    void e7_r3_lotInvisible_404_surGetEtPost() throws Exception {
        Reponse get = appels.transitions(qLibre, lotInvisible);
        Reponse post = appels.executer(qLibre, lotInvisible, ab);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(get.statut()).isEqualTo(404);
            s.assertThat(post.statut()).isEqualTo(404);
        });
    }

    @Test
    void e7_lotInconnu_404() throws Exception {
        assertThat(appels.transitions(qLibre, UUID.randomUUID()).statut()).isEqualTo(404);
    }
}
