package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ventilation par tranches, écritures — FEATURE-projets-config-parite.md « Règles OMRAN confirmées » §3 et « Contrat » :
 * {@code round(Σ taux, 2) > 1} → 400 (message OMRAN), Σ < 1 acceptée, tranche déjà ventilée → 400, tranche hors des sociétés du
 * projet → 400, tranche inconnue → 400 ; écriture Administrateur. Lecture et suppression : {@link ProjetConfigPariteTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentilationProjetTest {

    private static final String MESSAGE_OMRAN = "La ventilation de votre document est incorrecte";
    private static final String COMPTE_LIGNES = "SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PrjProjectId = :id";
    private static final String TAUX_LIGNE =
            "SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PKey = :id AND VentilationRate = :taux";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID projet;
    private UUID trancheA;
    private UUID trancheB;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        UUID societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
        trancheA = fixture.trancheDeSociete("TA-" + jeton, societe);
        trancheB = fixture.trancheDeSociete("TB-" + jeton, societe);
        projet = fixture.projet("VP-" + jeton, societe);
        // Colonnes de production ajoutées à la table réduite avant tout appel (POST sur un projet sans ligne)
        UUID projetTemoin = fixture.projet("VT-" + jeton, societe);
        fixture.ventilation(projetTemoin, fixture.trancheDeSociete("TT-" + jeton, societe), BigDecimal.ONE);
    }

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static CorpsJson ligne(UUID tranche, String taux) {
        return corps().avec(TRANCHE_ID, tranche).avec(TAUX, new BigDecimal(taux));
    }

    private ResultActions ajouter(UUID tranche, String taux) throws Exception {
        return ecrire(post(VENTILATIONS, projet), ligne(tranche, taux), connecte(admin));
    }

    private int lignesDuProjet() {
        return fixture.compter(COMPTE_LIGNES, projet);
    }

    // ------------------------------------------------------------------ ajout

    @Test
    void v_ajout_2xx_ligneEcriteEtRenvoyee() throws Exception {
        String json = ajouter(trancheA, "0.6")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$." + TRANCHE_ID).value(trancheA.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(fixture.compter("""
                SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation
                WHERE PKey = :id AND PrjProjectId = :projet AND CfgTrancheId = :tranche AND VentilationRate = :taux""",
                Map.of("id", id, "projet", projet, "tranche", trancheA, "taux", new BigDecimal("0.6")))).isEqualTo(1);
    }

    @Test
    void v_sommeInferieureA1_acceptee() throws Exception {
        fixture.ventilation(projet, trancheA, new BigDecimal("0.3"));

        ajouter(trancheB, "0.4").andExpect(status().is2xxSuccessful());

        assertThat(lignesDuProjet()).isEqualTo(2);
    }

    @Test
    void v_sommeArrondieA2DecimalesEgaleA1_acceptee() throws Exception {
        fixture.ventilation(projet, trancheA, new BigDecimal("0.6"));

        ajouter(trancheB, "0.404").andExpect(status().is2xxSuccessful());

        assertThat(lignesDuProjet()).isEqualTo(2);
    }

    @Test
    void v_sommeSuperieureA1_400_messageOmran_rienEcrit() throws Exception {
        fixture.ventilation(projet, trancheA, new BigDecimal("0.6"));

        ajouter(trancheB, "0.41")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(MESSAGE_OMRAN)));

        assertThat(lignesDuProjet()).isEqualTo(1);
    }

    @Test
    void v_trancheDejaVentilee_400_rienEcrit() throws Exception {
        fixture.ventilation(projet, trancheA, new BigDecimal("0.2"));

        ajouter(trancheA, "0.2").andExpect(status().isBadRequest());

        assertThat(lignesDuProjet()).isEqualTo(1);
    }

    @Test
    void v_trancheDUneAutreSociete_400_rienEcrit() throws Exception {
        UUID autreSociete = patrimoine.entite("SOX-" + jeton, true).getId();
        UUID trancheAutre = fixture.trancheDeSociete("TX-" + jeton, autreSociete);

        ajouter(trancheAutre, "0.5").andExpect(status().isBadRequest());

        assertThat(lignesDuProjet()).isZero();
    }

    @Test
    void v_trancheInconnue_400_rienEcrit() throws Exception {
        ajouter(UUID.randomUUID(), "0.5").andExpect(status().isBadRequest());

        assertThat(lignesDuProjet()).isZero();
    }

    // ------------------------------------------------------------------ modification

    @Test
    void v_modification_200_tauxEnBase() throws Exception {
        UUID id = fixture.ventilation(projet, trancheA, new BigDecimal("0.5"));

        ecrire(put(VENTILATION, projet, id), ligne(trancheA, "0.3"), connecte(admin)).andExpect(status().isOk());

        assertThat(fixture.compter(TAUX_LIGNE, Map.of("id", id, "taux", new BigDecimal("0.3")))).isEqualTo(1);
    }

    @Test
    void v_modification_sommeDesAutresPlusNouveauTauxEgale1_acceptee() throws Exception {
        UUID id = fixture.ventilation(projet, trancheA, new BigDecimal("0.5"));
        fixture.ventilation(projet, trancheB, new BigDecimal("0.4"));

        ecrire(put(VENTILATION, projet, id), ligne(trancheA, "0.6"), connecte(admin)).andExpect(status().isOk());

        assertThat(fixture.compter(TAUX_LIGNE, Map.of("id", id, "taux", new BigDecimal("0.6")))).isEqualTo(1);
    }

    @Test
    void v_modification_sommeSuperieureA1_400_tauxInchange() throws Exception {
        UUID id = fixture.ventilation(projet, trancheA, new BigDecimal("0.5"));
        fixture.ventilation(projet, trancheB, new BigDecimal("0.4"));

        ecrire(put(VENTILATION, projet, id), ligne(trancheA, "0.7"), connecte(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(MESSAGE_OMRAN)));

        assertThat(fixture.compter(TAUX_LIGNE, Map.of("id", id, "taux", new BigDecimal("0.5")))).isEqualTo(1);
    }

    @Test
    void v_modification_versUneTrancheDejaVentilee_400() throws Exception {
        UUID id = fixture.ventilation(projet, trancheA, new BigDecimal("0.2"));
        fixture.ventilation(projet, trancheB, new BigDecimal("0.2"));

        ecrire(put(VENTILATION, projet, id), ligne(trancheB, "0.2"), connecte(admin)).andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PKey = :id AND CfgTrancheId = :t",
                Map.of("id", id, "t", trancheA))).isEqualTo(1);
    }

    // ------------------------------------------------------------------ droits

    @Test
    void v_ajout_nonAdministrateur_403_rienEcrit() throws Exception {
        ecrire(post(VENTILATIONS, projet), ligne(trancheA, "0.5"), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(lignesDuProjet()).isZero();
    }

    @Test
    void v_modification_nonAdministrateur_403_tauxInchange() throws Exception {
        UUID id = fixture.ventilation(projet, trancheA, new BigDecimal("0.5"));

        ecrire(put(VENTILATION, projet, id), ligne(trancheA, "0.3"), enCommercial(commercial))
                .andExpect(status().isForbidden());

        assertThat(fixture.compter(TAUX_LIGNE, Map.of("id", id, "taux", new BigDecimal("0.5")))).isEqualTo(1);
    }
}
