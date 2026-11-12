package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.REFERENTIELS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.TAILLE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CONTENU_IDS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.ENTITE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.LOTS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.SEARCH;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.SIZE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.STATUT_IDS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E1 (filtres {@code statutIds}, {@code entiteId}, nouveaux champs), E2 (référentiels), R3, R4 — FEATURE-lots-liste-parite.md.
 * <pre>
 *   entité E1 : projet P1 (habilité) — L1 (S1), L2 (S2), LF (statut Fournisseurs SF) ; projet P3 (non habilité) — LX (S1)
 *   entité E2 : projet P2 (habilité) — L3 (S3), L4 (S1)          entité E3 : projet P4 (non habilité)
 *   LS : lot sans projet (S1)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsListePariteTest {

    private static final String GENRE_CHAMP = "genre";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;

    private LotsPariteSupport appels;
    private String jeton;
    private RequestPostProcessor qui;
    private UUID e1;
    private UUID e2;
    private UUID e3;
    private UUID p1;
    private UUID p2;
    private UUID s1;
    private UUID s2;
    private UUID sf;
    private UUID l1;
    private UUID l2;
    private UUID l3;
    private UUID l4;
    private UUID lf;
    private UUID lx;
    private UUID ls;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        e1 = fixture.entiteJuridique("E1-" + jeton);
        e2 = fixture.entiteJuridique("E2-" + jeton);
        e3 = fixture.entiteJuridique("E3-" + jeton);
        p1 = fixture.projet("P1-" + jeton, e1);
        p2 = fixture.projet("P2-" + jeton, e2);
        UUID p3 = fixture.projet("P3-" + jeton, e1);
        fixture.projet("P4-" + jeton, e3);
        s1 = fixture.statut(ENTREPRENEURS, "S1-" + jeton);
        s2 = fixture.statut(ENTREPRENEURS, "S2-" + jeton);
        UUID s3 = fixture.statut(ENTREPRENEURS, "S3-" + jeton);
        sf = fixture.statut(FOURNISSEURS, "SF-" + jeton);
        l1 = fixture.lot(p1, "L1-" + jeton, ENTREPRENEURS, s1);
        l2 = fixture.lot(p1, "L2-" + jeton, ENTREPRENEURS, s2);
        lf = fixture.lot(p1, "LF-" + jeton, FOURNISSEURS, sf);
        l3 = fixture.lot(p2, "L3-" + jeton, ENTREPRENEURS, s3);
        l4 = fixture.lot(p2, "L4-" + jeton, ENTREPRENEURS, s1);
        lx = fixture.lot(p3, "LX-" + jeton, ENTREPRENEURS, s1);
        ls = fixture.lot(null, "LS-" + jeton, ENTREPRENEURS, s1);
        qui = connecte(fixture.administrateur("lots.l1." + jeton, p1, p2), "Administrateur");
    }

    private List<String> ids(String... filtres) throws Exception {
        String[] params = new String[filtres.length + 2];
        System.arraycopy(filtres, 0, params, 0, filtres.length);
        params[filtres.length] = SIZE;
        params[filtres.length + 1] = TAILLE;
        Reponse r = appels.lire(qui, LOTS, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.ids(CONTENU_IDS);
    }

    private static String[] tous(UUID... lots) {
        String[] s = new String[lots.length];
        for (int i = 0; i < lots.length; i++) {
            s[i] = lots[i].toString();
        }
        return s;
    }

    // ------------------------------------------------------------------ statutIds (E1)

    @Test
    void e1_statutIds_aucun_tousLesLotsVisibles() throws Exception {
        assertThat(ids()).containsExactlyInAnyOrder(tous(l1, l2, lf, l3, l4));
    }

    @Test
    void e1_statutIds_un_seulementCeStatut() throws Exception {
        assertThat(ids(STATUT_IDS, s1.toString())).containsExactlyInAnyOrder(tous(l1, l4));
    }

    @Test
    void e1_statutIds_deux_unionDesDeuxStatuts() throws Exception {
        assertThat(ids(STATUT_IDS, s1.toString(), STATUT_IDS, s2.toString())).containsExactlyInAnyOrder(tous(l1, l2, l4));
    }

    @Test
    void e1_statutIds_statutDUnAutreGenre() throws Exception {
        assertThat(ids(STATUT_IDS, sf.toString(), STATUT_IDS, s2.toString())).containsExactlyInAnyOrder(tous(lf, l2));
    }

    // ------------------------------------------------------------------ entiteId (E1)

    @Test
    void e1_entiteId_lotsDesProjetsDeLEntite() throws Exception {
        assertThat(ids(ENTITE_ID, e2.toString())).containsExactlyInAnyOrder(tous(l3, l4));
    }

    @Test
    void e1_entiteId_etStatutIds_combinesEnEt() throws Exception {
        assertThat(ids(ENTITE_ID, e2.toString(), STATUT_IDS, s1.toString())).containsExactly(l4.toString());
    }

    @Test
    void e1_projetIds_etStatutIds_combinesEnEt() throws Exception {
        assertThat(ids(PROJET_IDS, p1.toString(), STATUT_IDS, s2.toString())).containsExactly(l2.toString());
    }

    @Test
    void e1_recherche_etEntiteId_combinesEnEt() throws Exception {
        List<String> bonneEntite = ids(SEARCH, "L3-" + jeton, ENTITE_ID, e2.toString());
        List<String> autreEntite = ids(SEARCH, "L3-" + jeton, ENTITE_ID, e1.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(bonneEntite).containsExactly(l3.toString());
            s.assertThat(autreEntite).isEmpty();
        });
    }

    @Test
    void e1_entiteEtProjetIncompatibles_pageVide() throws Exception {
        assertThat(ids(ENTITE_ID, e1.toString(), PROJET_IDS, p2.toString())).isEmpty();
    }

    // ------------------------------------------------------------------ R3 visibilité, R4 sans projet

    @Test
    void r3_entiteId_nOuvrePasLesProjetsNonHabilites() throws Exception {
        assertThat(ids(ENTITE_ID, e1.toString())).containsExactlyInAnyOrder(tous(l1, l2, lf)).doesNotContain(lx.toString());
    }

    @Test
    void r3_statutIds_nOuvrePasLesLotsInvisibles() throws Exception {
        assertThat(ids(STATUT_IDS, s1.toString())).doesNotContain(lx.toString());
    }

    @Test
    void r4_lotSansProjet_exclu_memeFiltreSurSonStatut() throws Exception {
        assertThat(ids(STATUT_IDS, s1.toString())).isNotEmpty().doesNotContain(ls.toString());
    }

    // ------------------------------------------------------------------ nouveaux champs de ligne (E1)

    @Test
    void e1_notes_dePrjMarketNotes() throws Exception {
        fixture.notes(l1, "Autres notes " + jeton);

        assertThat(appels.ligne(qui, l1, "L1-" + jeton)).containsEntry("notes", "Autres notes " + jeton);
    }

    @Test
    void e1_genre_entrepreneursEtFournisseurs_parStrEntityId() throws Exception {
        Map<String, Object> entrepreneurs = appels.ligne(qui, l1, "L1-" + jeton);
        Map<String, Object> fournisseurs = appels.ligne(qui, lf, "LF-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(entrepreneurs).containsEntry(GENRE_CHAMP, ENTREPRENEURS);
            s.assertThat(fournisseurs).containsEntry(GENRE_CHAMP, FOURNISSEURS);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENTREPRENEURS", "FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
    void e1_genre_lesCinqGenres(String genre) throws Exception {
        UUID statut = fixture.statut(genre, "SG-" + genre + "-" + jeton);
        UUID lot = fixture.lot(p1, "LG-" + jeton, genre, statut);

        assertThat(appels.ligne(qui, lot, "LG-" + jeton)).containsEntry(GENRE_CHAMP, genre);
    }

    @Test
    void e1_statutCouleur_memeValeurQueLeReferentiel_nonNulle() throws Exception {
        Reponse referentiels = appels.lire(qui, REFERENTIELS);
        List<String> couleurs = referentiels.ids("$.statutsLot[?(@.id == '" + s1 + "')].couleur");

        assertThat(couleurs).singleElement().isNotNull()
                .isEqualTo(appels.ligne(qui, l1, "L1-" + jeton).get("statutCouleur"));
    }

    // ------------------------------------------------------------------ référentiels (E2)

    @Test
    void e2_entites_cellesQuiPortentUnProjetVisible() throws Exception {
        Reponse r = appels.lire(qui, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(200);
            s.assertThat(r.ids("$.entites[*].id")).contains(e1.toString(), e2.toString()).doesNotContain(e3.toString());
            s.assertThat(r.ids("$.entites[?(@.id == '" + e1 + "')].code")).containsExactly("E1-" + jeton);
            s.assertThat(r.ids("$.entites[?(@.id == '" + e1 + "')].libelle")).containsExactly("Entite E1-" + jeton);
        });
    }

    @Test
    void e2_projets_portentEntiteId() throws Exception {
        Reponse r = appels.lire(qui, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.projets[?(@.id == '" + p1 + "')].entiteId")).containsExactly(e1.toString());
            s.assertThat(r.ids("$.projets[?(@.id == '" + p2 + "')].entiteId")).containsExactly(e2.toString());
        });
    }

    @Test
    void e2_statutsLot_portentGenre() throws Exception {
        Reponse r = appels.lire(qui, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.statutsLot[?(@.id == '" + s1 + "')].genre")).containsExactly(ENTREPRENEURS);
            s.assertThat(r.ids("$.statutsLot[?(@.id == '" + sf + "')].genre")).containsExactly(FOURNISSEURS);
        });
    }
}
