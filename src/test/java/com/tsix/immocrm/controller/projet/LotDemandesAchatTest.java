package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E5 — FEATURE-lot-fiche-parite.md : {@code GET /api/projet/lots/{id}/demandes-achat} rend les lignes {@code BuySelection} du lot
 * (procédure d'achat, statut, code, date, description, fournisseur, notes). Le spec ne nomme pas les champs de
 * {@code DemandeAchatLotDto} : les valeurs de la fixture sont cherchées dans le corps, pas sous un nom de champ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotDemandesAchatTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureProjetLots lots;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("DAP-" + jeton, parite.entiteJuridique("DAE-" + jeton));
        lot = parite.lot(projet, "DAL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "DAS-" + jeton));
        qui = connecte(parite.avecDroits("lots.dia." + jeton, List.of(), projet), "Commercial");
    }

    private UUID demande(UUID duLot, String code) {
        UUID procedure = fiche.procedureAchat("PROC-" + code);
        UUID statut = parite.statut(ENTREPRENEURS, "STDIA-" + code);
        UUID fournisseur = lots.fournisseur("FDIA-" + code);
        return fiche.demandeAchat(duLot, projet, code, procedure, statut, fournisseur, LocalDate.of(2026, 3, 15),
                "Notes " + code);
    }

    @Test
    void e5_demandesDuLot_200_codeDescriptionDateNotesProcedureStatut() throws Exception {
        String code = "DIA-" + jeton;
        demande(lot, code);

        Reponse r = appels.demandesAchat(qui, lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.corps()).contains(code, "DIA " + code, "2026-03-15", "Notes " + code, "PROC-" + code,
                    "STDIA-" + code);
        });
    }

    @Test
    void e5_demandeDUnAutreLot_absente() throws Exception {
        UUID autreLot = parite.lot(projet, "DAM-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "DAMS-" + jeton));
        demande(lot, "DIAA-" + jeton);
        demande(autreLot, "DIAB-" + jeton);

        Reponse r = appels.demandesAchat(qui, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.corps()).contains("DIAA-" + jeton).doesNotContain("DIAB-" + jeton);
    }

    @Test
    void e5_lotSansDemande_200() throws Exception {
        fiche.tablesDemandesAchat();

        Reponse r = appels.demandesAchat(qui, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // Vert avant le code (route absente = 404) ; les témoins 200 sont les tests ci-dessus.
    @Test
    void e5_lotInvisible_404() throws Exception {
        UUID autre = parite.projet("DAX-" + jeton, null);
        UUID invisible = parite.lot(autre, "DAX-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "DAXS-" + jeton));
        fiche.demandeAchat(invisible, autre, "DIAX-" + jeton, null, null, null, LocalDate.of(2026, 3, 15), null);

        Reponse r = appels.demandesAchat(qui, invisible);

        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.corps()).doesNotContain("DIAX-" + jeton);
    }
}
