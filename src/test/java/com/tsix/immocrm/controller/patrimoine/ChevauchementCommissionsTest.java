package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chevauchement des commissions — FEATURE-patrimoine-tranche-regles.md §2 (CfgTranche.Verif l.2888-2941), §7
 * (ChevauchementCommissionsTest). Le spec ne donne pas la signature de {@code domain/ChevauchementCommissions} :
 * la règle est testée par {@code PUT /api/tranches/{id}/commissions}. Clé = personnel + action + commercial +
 * catégorie, null = null ; fin nulle = infini. Seuls les cas où l'arithmétique des périodes et le code OMRAN
 * concordent sont testés (voir le rapport).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChevauchementCommissionsTest {

    private static final String COMMISSIONS = "/api/tranches/{id}/commissions";
    private static final String MESSAGE = "chevauchement des périodes pour deux lignes de paramétrage identiques";
    private static final String JANVIER = "2027-01-01T00:00:00";
    private static final String MARS = "2027-03-31T00:00:00";
    private static final String AVRIL = "2027-04-01T00:00:00";
    private static final String JUIN = "2027-06-01T00:00:00";
    private static final String DECEMBRE = "2027-12-31T00:00:00";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureDossierVente vente;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1CC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private static CorpsJson periode(String debut, String fin) {
        return corps().avec("dateDebut", debut).avec("dateFin", fin);
    }

    private ResultActions envoyer(CorpsJson... lignes) throws Exception {
        return mockMvc.perform(put(COMMISSIONS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(lignes(lignes)));
    }

    // ------------------------------------------------------------------ même clé

    @Test
    void r2_memeCleNulle_periodesQuiSeChevauchent_400_messageOmran() throws Exception {
        envoyer(periode(JANVIER, JUIN), periode(AVRIL, DECEMBRE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE)));
    }

    @Test
    void r2_memeCleNulle_periodesContigues_200() throws Exception {
        envoyer(periode(JANVIER, MARS), periode(AVRIL, DECEMBRE)).andExpect(status().isOk());
    }

    @Test
    void r2_memeCommercial_periodesQuiSeChevauchent_400() throws Exception {
        UUID commercial = vente.tier("COM-" + jeton).getId();

        envoyer(periode(JANVIER, JUIN).avec("commercialId", commercial),
                periode(AVRIL, DECEMBRE).avec("commercialId", commercial))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ clés différentes

    @Test
    void r2_categoriesDifferentes_periodesQuiSeChevauchent_200() throws Exception {
        StkItemCategory c1 = fixture.categorie("CAT1-" + jeton, fixture.natureBat(), false);
        StkItemCategory c2 = fixture.categorie("CAT2-" + jeton, fixture.natureBat(), false);

        envoyer(periode(JANVIER, JUIN).avec("categorieArticleId", c1.getId()),
                periode(AVRIL, DECEMBRE).avec("categorieArticleId", c2.getId()))
                .andExpect(status().isOk());
    }

    /** null = null : une clé nulle et une clé renseignée sont deux clés différentes. */
    @Test
    void r2_commercialNulEtCommercialRenseigne_clesDifferentes_200() throws Exception {
        UUID commercial = vente.tier("COM-" + jeton).getId();

        envoyer(periode(JANVIER, JUIN), periode(AVRIL, DECEMBRE).avec("commercialId", commercial))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ fin nulle = infini

    @Test
    void r2_finNulle_deuxLignesOuvertesDeMemeCle_400() throws Exception {
        envoyer(periode(JANVIER, null), periode(JUIN, null)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_finNulle_ligneFermeeQuiDepasseLeDebutDUneLigneOuverte_400() throws Exception {
        envoyer(periode(JANVIER, DECEMBRE), periode(JUIN, null)).andExpect(status().isBadRequest());
    }
}
