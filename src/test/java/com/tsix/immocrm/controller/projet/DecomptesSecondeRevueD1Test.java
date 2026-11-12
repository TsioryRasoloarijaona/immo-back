package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Seconde revue du lot D1 (règle 6) : cumul après retenues et visibilité de la création. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesSecondeRevueD1Test {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String APRES = "AmountTTCAfterDeductions";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private FixtureVisibiliteOmran visibilite;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PSR-" + jeton);
        lot = fixture.lot(projet, "LSR-" + jeton, fixture.fournisseur("FSR-" + jeton));
        AdmUser compte = vente.compte("prj.sr.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
    }

    private MockHttpServletResponse executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui)
            throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    /**
     * {@code W:AmountTTCAfterDeductions} l.1526-1536 retire {@code PreviousDeductionAmountAccumalted} = CumulRefendable +
     * CumulNonRefendableRetainsTTC de {@code PrjWorkingOutView} = Σ RG + Σ (DEC + TRC + prorata) : les retenues forfaitaires
     * n'y sont pas. Cas réel yesRemsProd 25/00487 (non verrouillé) : précédent 25/00409 à 3 600 de forfaitaires, stocké
     * 95 865 = 87 945 + 7 920 (aucune retenue retirée).
     */
    @Test
    void recalcul_cumulApresRetenues_neRetirePasLesRetenuesForfaitairesPrecedentes() throws Exception {
        UUID ligneMarche = decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
        UUID d1 = fixture.decompte(lot, "SR1-" + jeton, 1, Map.of(TTC, new BigDecimal("1000"),
                FixtureProjetLots.RG, new BigDecimal("100"), FixtureProjetLots.RETENUE_NON_REMBOURSABLE,
                new BigDecimal("300"), A_PAYER, new BigDecimal("600")));
        decomptes.poser("PrjWorkingOut", d1, "RefundableRetainer", new BigDecimal("100"));
        jdbc.update("INSERT INTO PrjWorkingOutRetainedLine (PKey, PrjWorkingOutId, Sorting, Description, Amount)"
                + " VALUES (?, ?, 1, 'Forfait', 300)", UUID.randomUUID().toString(), d1.toString());
        UUID d2 = fixture.decompte(lot, "SR2-" + jeton, 2, Map.of(TTC, BigDecimal.ZERO, A_PAYER, BigDecimal.ZERO));
        decomptes.ligne(d2, ligneMarche, 1, "1", "0", "100", "0.2");

        int statut = executer(post(DECOMPTES + "/" + d2 + "/recalcul"), admin).getStatus();

        // cumul 1 000 + 120 − RG précédente 100 − RG du mois (0,1 × 1 120 − 100 = 12) = 1 008 ; forfaitaires 300 non retirés
        assertThat(statut).isEqualTo(200);
        assertThat(new BigDecimal(String.valueOf(decomptes.lire(d2, APRES).get(APRES)))).isEqualByComparingTo("1008");
    }

    /**
     * Création depuis la fiche lot : un compte restreint ne voit pas un lot sans statut visible (V1 R1, fiche 404) ; il ne
     * doit pas pouvoir y créer un décompte (consigne du manager : lotVisible = périmètre projet + statut du lot).
     */
    @Test
    void creation_lotInvisibleParStatut_404CommeLaFicheLot() throws Exception {
        String role = "DSR-" + jeton;
        AdmUser restreint = visibilite.compte("prj.sr.r." + jeton, role, true);
        fixture.habiliter(restreint, projet);
        jdbc.update("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), DROIT_MODIFIER, DROIT_MODIFIER, visibilite.idRole(role).toString());
        RequestPostProcessor u = connecte(restreint, role);
        Reponse fiche = new AppelsProjet(mockMvc).lire(u, LOTS + "/" + lot);

        int creation = executer(post(LOTS + "/" + lot + "/decomptes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDocument\":\"2026-04-01\"}"), u).getStatus();

        assertThat(fiche.statut()).as("témoin : fiche lot invisible").isEqualTo(404);
        assertThat(creation).isEqualTo(404);
    }
}
