package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** « Recharger les cumuls » ({@code POST /decomptes/{id}/cumuls}, FEATURE-decomptes-parite.md §7, W:ReloadCumulLines). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesCumulsTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final String CUMULS = "/cumuls";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;

    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PCU-" + jeton);
        lot = fixture.lot(projet, "LCU-" + jeton, fixture.fournisseur("FCU-" + jeton));
        AdmUser compte = vente.compte("prj.cu.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
    }

    private UUID decompte(String code, int numero) {
        return fixture.decompte(lot, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private int cumuls(UUID decompte, RequestPostProcessor qui) throws Exception {
        decomptes.flush();
        return mockMvc.perform(post(DECOMPTES + "/" + decompte + CUMULS).with(qui)).andReturn().getResponse().getStatus();
    }

    /** Décompte 1 : A = 4 ; décompte 2 : A = 1 avec une quantité antérieure fausse (0). */
    private UUID chantier() {
        UUID a = decomptes.ligneMarche(lot, 1, "10", "100", TVA);
        UUID d1 = decompte("CU1-", 1);
        decomptes.ligne(d1, a, 1, "4", "0", "100", TVA);
        UUID d2 = decompte("CU2-", 2);
        decomptes.ligne(d2, a, 1, "1", "0", "100", TVA);
        return d2;
    }

    @Test
    void cumuls_nominal_quantiteAnterieureRelue_quantiteDePeriodeGardee() throws Exception {
        UUID d2 = chantier();

        int statut = cumuls(d2, admin);

        assertThat(statut).isEqualTo(200);
        List<Map<String, Object>> lignes = decomptes.lignes(d2);
        assertThat(lignes).hasSize(1).first().satisfies(l -> {
            assertThat(new BigDecimal(String.valueOf(l.get("QuantityOld")))).isEqualByComparingTo("4");
            assertThat(new BigDecimal(String.valueOf(l.get("Quantity")))).isEqualByComparingTo("1");
        });
    }

    @Test
    void cumuls_refus409_siPasLeDernierDuLot() throws Exception {
        UUID d1 = decompte("CV1-", 1);
        decompte("CV2-", 2);

        assertThat(cumuls(d1, admin)).isEqualTo(409);
    }

    @Test
    void cumuls_refus403_sansDroitModifier() throws Exception {
        UUID d1 = decompte("CW1-", 1);

        assertThat(cumuls(d1, decomptes.compte(projet, jeton, DROIT_SUPPRIMER))).isEqualTo(403);
    }

    @Test
    void cumuls_refus404_idInconnu() throws Exception {
        assertThat(cumuls(UUID.randomUUID(), admin)).isEqualTo(404);
    }
}
