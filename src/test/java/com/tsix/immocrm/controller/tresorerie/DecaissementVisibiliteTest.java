package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
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

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md : visibilité de la saisie (règle 6) — un règlement, une pièce, une société ou un
 * fournisseur hors périmètre du compte rendent 404, comme la lecture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementVisibiliteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID pa;
    private UUID pinv;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DVI");
        qui = connecte(fixture.compte(s, "DVIA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU,
                FixtureDecaissements.DROIT_SUPPRIMER, FixtureDecaissements.DROIT_SANS_AFFECTATION), FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
        pinv = fixture.piece(s, "-FINV", s.c().projetNonHabilite(), "1000");
    }

    private UUID reglementSurPiece(String suffixe, UUID piece) {
        UUID r = tresorerie.reglement(Reglement.debit(s.code(suffixe), s.c(), "300"));
        tresorerie.ligne(r, piece, "300");
        return r;
    }

    private Map<String, Object> corpsSurPa(String suffixe) {
        return corps(s, s.code(suffixe), "300", List.of(ligne(pa, "300")));
    }

    @Test
    void put_404_reglementDontLUniqueLigneEstSurProjetNonHabilite() throws Exception {
        UUID invisible = reglementSurPiece("-RI", pinv);

        Reponse r = appels.modifier(qui, invisible, corpsSurPa("-RI2"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void delete_404_reglementDontLUniqueLigneEstSurProjetNonHabilite() throws Exception {
        UUID invisible = reglementSurPiece("-RJ", pinv);

        Reponse r = appels.supprimer(qui, invisible);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.nombre("StlSettlement", "PKey", invisible)).as("règlement conservé").isEqualTo(1);
    }

    @Test
    void temoin_reglementSurPieceVisible_ficheLue200() throws Exception {
        UUID visible = reglementSurPiece("-RV", pa);

        Reponse r = appels.fiche(qui, visible);

        assertThat(r.statut()).isEqualTo(200);
    }

    @Test
    void delete_404_reglementSansLigneDUneSocieteNonHabilitee() throws Exception {
        UUID autre = tresorerie.reglement(Reglement.debit(s.code("-RS"), s.c(), "300").societe(s.autreSociete()));

        Reponse r = appels.supprimer(qui, autre);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void post_404_pieceInvisible() throws Exception {
        Reponse r = appels.creer(qui, corps(s, s.code("-RP"), "300", List.of(ligne(pinv, "300"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void post_404_societeNonHabilitee() throws Exception {
        Map<String, Object> corps = corps(s, s.code("-RA"), "300", List.of());
        corps.put("societeId", s.autreSociete());
        corps.put("compteId", s.compteAutreSociete());

        Reponse r = appels.creer(qui, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void post_404_fournisseurInconnu() throws Exception {
        Map<String, Object> corps = corps(s, s.code("-RF"), "300", List.of());
        corps.put("fournisseurId", UUID.randomUUID());

        Reponse r = appels.creer(qui, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void put_404_idInconnu() throws Exception {
        Reponse r = appels.modifier(qui, UUID.randomUUID(), corpsSurPa("-RU"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }
}
