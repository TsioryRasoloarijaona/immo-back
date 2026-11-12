package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
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
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_EXPORTER;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E6 — FEATURE-lots-liste-parite.md : 409 si une FK {@code NO_ACTION} cite le lot (décompte, pièce d'achat), message du spec,
 * lot conservé ; sinon 204, lot et enfants {@code ON DELETE CASCADE} ({@code PrjMarketLine}) disparus ; 404 inconnu ou
 * invisible ; ordre des refus 403 → 404 → 409 (droit contrôlé avant l'existence).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsSuppressionTest {

    private static final String MESSAGE_409 = "Ce lot est utilisé ailleurs (décomptes, achats…) et ne peut pas être supprimé.";
    private static final String COMPTE_LOT = "SELECT COUNT(*) FROM PrjMarket WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID projet;
    private UUID invisible;
    private UUID statut;
    private RequestPostProcessor admin;
    private RequestPostProcessor sansDroit;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        UUID entite = fixture.entiteJuridique("SE-" + jeton);
        projet = fixture.projet("SP-" + jeton, entite);
        invisible = fixture.projet("SX-" + jeton, entite);
        statut = fixture.statut(ENTREPRENEURS, "SS-" + jeton);
        admin = connecte(fixture.administrateur("lots.sup." + jeton, projet), "Administrateur");
        sansDroit = connecte(fixture.avecDroits("lots.sup.nd." + jeton, List.of(DROIT_EXPORTER), projet), "Commercial");
    }

    private UUID lot(UUID dansProjet, String code) {
        return fixture.lot(dansProjet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    private int existe(UUID lot) {
        return fixture.compter(COMPTE_LOT, lot.toString());
    }

    // ------------------------------------------------------------------ 409

    @Test
    void e6_lotCiteParUnDecompte_409_messageDuSpec_lotConserve() throws Exception {
        UUID lot = lot(projet, "SD");
        fixture.decompteDuLot(lot, "SD-D-" + jeton);

        Reponse r = appels.supprimer(admin, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(r.corps()).contains(MESSAGE_409);
            s.assertThat(existe(lot)).isEqualTo(1);
        });
    }

    @Test
    void e6_lotCiteParUnePieceDAchat_409_lotConserve() throws Exception {
        UUID lot = lot(projet, "SA");
        fixture.pieceQuiReferenceLeLot(lot, "SA-P-" + jeton);

        Reponse r = appels.supprimer(admin, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(existe(lot)).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------ 204

    @Test
    void e6_lotLibre_204_lotEtLignesDeBordereauSupprimes() throws Exception {
        UUID lot = lot(projet, "SL");
        UUID ligne = fixture.ligneBordereau(lot, "SL-B-" + jeton);

        Reponse r = appels.supprimer(admin, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(204);
            s.assertThat(existe(lot)).isZero();
            s.assertThat(fixture.compter("SELECT COUNT(*) FROM PrjMarketLine WHERE PKey = ?", ligne.toString()))
                    .as("enfant CASCADE").isZero();
        });
    }

    // ------------------------------------------------------------------ 404

    @Test
    void e6_lotInconnu_404() throws Exception {
        assertThat(appels.supprimer(admin, UUID.randomUUID()).statut()).isEqualTo(404);
    }

    @Test
    void e6_r3_lotInvisible_404_lotConserve() throws Exception {
        UUID lot = lot(invisible, "SI");

        Reponse r = appels.supprimer(admin, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(404);
            s.assertThat(existe(lot)).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------ ordre des refus : 403, puis 404, puis 409

    @Test
    void e6_sansDroit_idInconnu_403AvantLe404() throws Exception {
        assertThat(appels.supprimer(sansDroit, UUID.randomUUID()).statut()).isEqualTo(403);
    }

    @Test
    void e6_sansDroit_lotVisibleLibre_403_lotConserve() throws Exception {
        UUID lot = lot(projet, "SN");

        Reponse r = appels.supprimer(sansDroit, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(403);
            s.assertThat(existe(lot)).isEqualTo(1);
        });
    }

    @Test
    void e6_lotInvisibleEtCite_404AvantLe409() throws Exception {
        UUID lot = lot(invisible, "SC");
        fixture.decompteDuLot(lot, "SC-D-" + jeton);

        assertThat(appels.supprimer(admin, lot).statut()).isEqualTo(404);
    }
}
