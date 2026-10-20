package com.tsix.immocrm.controller.desistement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDesistements;
import com.tsix.immocrm.fixtures.FixtureDesistements.Parametres;
import com.tsix.immocrm.fixtures.FixtureDesistements.Scene;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import jakarta.persistence.EntityManager;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des désistements (FEATURE-desistements.md §10, réponse 2) : écritures réservées à Administrateur,
 * Direction Générale, Commercial Chef ; un commercial simple, même habilité au projet, reçoit 403 sur chacune ;
 * lecture de la liste au périmètre. Chaque refus est apparié à la même requête acceptée pour un rôle autorisé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DesistementSecuriteTest {

    private static final String CREER = "/api/dossiers/{id}/desistement";
    private static final String LISTE = "/api/desistements";
    private static final String DESISTEMENT = "/api/desistements/{id}";
    private static final String VALIDATION = "/api/desistements/{id}/validation";
    private static final String REMBOURSEMENT = "/api/desistements/{id}/remboursement";
    private static final String ADMIN = "Administrateur";
    private static final String DIRECTION = "Direction Générale";
    private static final String CHEF = "Commercial Chef";
    private static final String COMMERCIAL = "Commercial";
    private static final String CREATION = "{\"dateDemande\":\"2026-10-01T00:00:00\",\"observations\":\"Financement refusé\"}";
    private static final String VALIDATION_COMPLETE =
            "{\"dateValidation\":\"2026-10-05T00:00:00\",\"dateLegalisation\":\"2026-10-06T00:00:00\"}";
    private static final String LIGNES_IDS = "$.lignes[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDesistements desistements;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private EntityManager em;

    private Scene scene;
    private AdmUser admin;
    private AdmUser direction;
    private AdmUser commercial;

    @BeforeEach
    void setUp() {
        scene = desistements.scene("SEC", Parametres.standard());
        admin = fixture.compte("TST-DES-SEC-ADM", ADMIN);
        direction = fixture.compte("TST-DES-SEC-DG", DIRECTION);
        commercial = desistements.compteHabilite("TST-DES-SEC-COM", scene.projetId(), COMMERCIAL);
    }

    @Test
    void creation_commercialSimple403_chefHabilite201() throws Exception {
        AdmUser chef = desistements.compteHabilite("TST-DES-SEC-CHEF", scene.projetId(), CHEF);

        executer(post(CREER, scene.dossierId()), CREATION, commercial, COMMERCIAL).andExpect(status().isForbidden());
        executer(post(CREER, scene.dossierId()), CREATION, chef, CHEF).andExpect(status().isCreated());
    }

    @Test
    void modification_commercialSimple403_directionGeneraleAcceptee() throws Exception {
        UUID id = creerCommeAdmin();
        String corps = "{\"observations\":\"Client injoignable\"}";

        executer(patch(DESISTEMENT, id), corps, commercial, COMMERCIAL).andExpect(status().isForbidden());
        executer(patch(DESISTEMENT, id), corps, direction, DIRECTION).andExpect(status().is2xxSuccessful());
    }

    @Test
    void validation_commercialSimple403_dossierInchange_directionGeneraleAcceptee() throws Exception {
        UUID id = creerCommeAdmin();

        executer(post(VALIDATION, id), VALIDATION_COMPLETE, commercial, COMMERCIAL).andExpect(status().isForbidden());
        assertThat(statutDuDossier()).isEqualTo(StatutDossier.RESERVATION.getCode());

        executer(post(VALIDATION, id), VALIDATION_COMPLETE, direction, DIRECTION).andExpect(status().is2xxSuccessful());
    }

    @Test
    void suppression_commercialSimple403_directionGeneraleAcceptee() throws Exception {
        UUID id = creerCommeAdmin();

        mockMvc.perform(delete(DESISTEMENT, id).with(comme(commercial, COMMERCIAL))).andExpect(status().isForbidden());
        mockMvc.perform(delete(DESISTEMENT, id).with(comme(direction, DIRECTION))).andExpect(status().is2xxSuccessful());
    }

    @Test
    void remboursement_commercialSimple403_directionGeneraleAcceptee() throws Exception {
        UUID id = creerCommeAdmin();
        executer(post(VALIDATION, id), VALIDATION_COMPLETE, admin, ADMIN).andExpect(status().is2xxSuccessful());
        String corps = "{\"montant\":1000,\"dateEcheance\":\"2026-10-10T00:00:00\",\"typeId\":\""
                + scene.typeReglementId() + "\"}";

        executer(post(REMBOURSEMENT, id), corps, commercial, COMMERCIAL).andExpect(status().isForbidden());
        executer(post(REMBOURSEMENT, id), corps, direction, DIRECTION).andExpect(status().isCreated());
    }

    /** Lecture au périmètre : la liste montre le désistement au commercial habilité au projet, pas à un autre. */
    @Test
    void liste_auPerimetre_visiblePourLHabilite_invisibleHorsProjet() throws Exception {
        UUID id = creerCommeAdmin();
        Scene autre = desistements.scene("SEC2", Parametres.standard());
        AdmUser horsProjet = desistements.compteHabilite("TST-DES-SEC-HORS", autre.projetId(), COMMERCIAL);

        mockMvc.perform(get(LISTE).param("size", "200").with(comme(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES_IDS, hasItem(id.toString())));
        mockMvc.perform(get(LISTE).param("size", "200").with(comme(horsProjet, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LIGNES_IDS, not(hasItem(id.toString()))));
        mockMvc.perform(get(DESISTEMENT, id).with(comme(horsProjet, COMMERCIAL))).andExpect(status().isNotFound());
        mockMvc.perform(get(CREER, scene.dossierId()).with(comme(horsProjet, COMMERCIAL)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ outils

    private static RequestPostProcessor comme(AdmUser compte, String role) {
        return FixtureDossierVente.connecte(compte, role);
    }

    private ResultActions executer(MockHttpServletRequestBuilder requete, String corps, AdmUser compte, String role)
            throws Exception {
        return mockMvc.perform(requete.with(comme(compte, role)).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID creerCommeAdmin() throws Exception {
        String reponse = executer(post(CREER, scene.dossierId()), CREATION, admin, ADMIN)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private String statutDuDossier() {
        em.flush();
        em.clear();
        return folderRepository.findById(scene.dossierId()).orElseThrow().getStrStatus().getCode();
    }
}
