package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.CODE;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.PROJET;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.PROJETS;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.REFERENTIELS;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.SOCIETE_ID;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.STATUTS_LOT;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot P9a (FEATURE-projet-config.md) : écarts relevés hors des tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetConfigRevueTest {

    // Genre de lot « Lot Concessionnaires » de prod : 2 statuts, hors des 3 genres retenus (réponse 3 du manager).
    private static final UUID LOT_CONCESSIONNAIRES = UUID.fromString("E7999F74-7507-40D9-AA78-A2825C42B614");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;
    @Autowired private EntityManager entityManager;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID societe;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
    }

    private RequestPostProcessor enCommercial() {
        return FixtureDossierVente.connecte(commercial, "Commercial");
    }

    private CorpsJson corpsProjet(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Projet " + code).avec(SOCIETE_ID, societe);
    }

    /** §9 : règle d'URL pour que le 403 passe avant un 400 de paramètre typé. */
    @Test
    void nonAdministrateur_idMalForme_403EtNon400() throws Exception {
        mockMvc.perform(put(PROJET, "pas-un-uuid").with(enCommercial())
                        .contentType(MediaType.APPLICATION_JSON).content(corpsProjet("PU-" + jeton).json()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdministrateur_corpsIllisible_403EtNon400() throws Exception {
        mockMvc.perform(post(PROJETS).with(enCommercial())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isForbidden());
    }

    /** Règle 4 : espaces rognés — l'espace insécable (U+00A0) n'est pas retiré par {@code trim()}. */
    @Test
    void codeEnDouble_espaceInsecable_409() throws Exception {
        mockMvc.perform(post(PROJETS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsProjet("PV-" + jeton).json()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PROJETS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsProjet(" pv-" + jeton).json()))
                .andExpect(status().isConflict());
    }

    /** @Schema « Sociétés, actives ou non » : 17 des 25 projets de prod portent une société inactive. */
    @Test
    void referentiels_societeInactiveIncluse() throws Exception {
        UUID inactive = patrimoine.entite("SOI-" + jeton, false).getId();

        mockMvc.perform(get(REFERENTIELS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societes[*].id", hasItem(inactive.toString())));
    }

    /** Réponse 3 du manager : les statuts des 3 genres retenus seulement ; prod en a 2 de plus chez Administration et Concessionnaires. */
    @Test
    void statutsLot_genreConcessionnaires_exclu() throws Exception {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'PrjMarketDealers', N'Lot Concessionnaires')""")
                .setParameter("id", LOT_CONCESSIONNAIRES).executeUpdate();
        UUID statut = fixture.statut(LOT_CONCESSIONNAIRES, "STD-" + jeton);

        mockMvc.perform(get(STATUTS_LOT).with(enCommercial()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(statut.toString()))));
    }
}
