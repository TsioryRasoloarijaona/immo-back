package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
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

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.PIECES_A_AFFECTER;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.controller.tresorerie.DecaissementPropositionTest.MSG_NON_VALIDEE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-b1-reglements.md G-e (parité du picker OMRAN, réponse du manager) : une pièce dont le statut n'a pas
 * {@code IsOkForPayement = 1} n'est pas proposée à l'affectation, et POST / PUT refusent une ligne sur elle (message G-a).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementPieceNonValideeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID validee;
    private UUID nonValidee;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DNV");
        qui = connecte(fixture.compte(s, "DNVA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
        validee = fixture.piece(s, "-FV", s.c().projet(), "1000");
        nonValidee = fixture.pieceNonValidee(s, "-FNV", s.c().projet(), "1000");
    }

    private Map<String, Object> corpsSur(String suffixe, UUID piece) {
        return corps(s, s.code(suffixe), "300", List.of(ligne(piece, "300")));
    }

    // ------------------------------------------------------------------ G-e picker

    @Test
    void gE_piecesAAffecter_exclutLaPieceNonValidee() throws Exception {
        Reponse r = appels.lire(qui, PIECES_A_AFFECTER, AppelsTresorerie.MODULE, AppelsTresorerie.PRJ,
                "fournisseurId", s.c().fournisseur().toString(), "societeId", s.c().societe().toString());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<String> ids = JsonPath.read(r.corps(), "$[*].id");
        assertThat(ids).contains(validee.toString()).doesNotContain(nonValidee.toString());
    }

    // ------------------------------------------------------------------ G-e enregistrement

    @Test
    void gE_post_ligneSurPieceNonValidee_400_messageGa() throws Exception {
        Reponse r = appels.creer(qui, corpsSur("-R1", nonValidee));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MSG_NON_VALIDEE);
        Reponse temoin = appels.creer(qui, corpsSur("-R2", validee));
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(201);
    }

    @Test
    void gE_put_ligneRemplaceeParPieceNonValidee_400_messageGa() throws Exception {
        UUID autreValidee = fixture.piece(s, "-FV2", s.c().projet(), "1000");
        UUID id = appels.creerOk(qui, corpsSur("-R3", validee));

        Reponse r = appels.modifier(qui, id, corpsSur("-R3", nonValidee));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MSG_NON_VALIDEE);
        Reponse temoin = appels.modifier(qui, id, corpsSur("-R3", autreValidee));
        assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
    }
}
