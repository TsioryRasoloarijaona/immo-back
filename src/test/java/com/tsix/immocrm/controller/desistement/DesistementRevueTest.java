package com.tsix.immocrm.controller.desistement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDesistements;
import com.tsix.immocrm.fixtures.FixtureDesistements.Parametres;
import com.tsix.immocrm.fixtures.FixtureDesistements.Piece;
import com.tsix.immocrm.fixtures.FixtureDesistements.Scene;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
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

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_FRAIS_DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_REMBOURSEMENT_DESISTEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot désistements (FEATURE-desistements.md) : écarts trouvés en revue, scène standard de la fixture. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DesistementRevueTest {

    private static final String ADMIN = "Administrateur";
    private static final String COMMERCIAL = "Commercial";
    private static final String CREER = "/api/dossiers/{id}/desistement";
    private static final String FICHE = "/api/desistements/{id}";
    private static final String VALIDATION = "/api/desistements/{id}/validation";
    private static final String CREATION = "{\"dateDemande\":\"2026-10-01T00:00:00\",\"observations\":\"Test\"}";
    private static final String VALIDATION_COMPLETE =
            "{\"dateValidation\":\"2026-10-05T00:00:00\",\"dateLegalisation\":\"2026-10-06T00:00:00\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDesistements desistements;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private EntityManager em;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.compte("TST-DES-REV", ADMIN);
    }

    /** Sale.cs:CancelFolder L3861 : QuittanceCode de la pièce de frais = N° du désistement (prod : 21/00005 des deux côtés). */
    @Test
    void validation_pieceDeFrais_quittanceEgaleAuNumeroDuDesistement() throws Exception {
        Scene scene = desistements.scene("REV-QUIT", Parametres.standard());
        String reponse = executer(post(CREER, scene.dossierId()), CREATION).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));
        String code = JsonPath.read(reponse, "$.code");

        executer(post(VALIDATION, id), VALIDATION_COMPLETE).andExpect(status().isOk());

        em.flush();
        Object quittance = em.createNativeQuery("""
                        SELECT d.QuittanceCode FROM ComDocument d
                        JOIN ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId
                        WHERE d.ComSaleWithdrawalCancellationId = :id AND c.Code = :code""")
                .setParameter("id", id).setParameter("code", CODE_FRAIS_DESISTEMENT).getSingleResult();
        assertThat(quittance).isEqualTo(code);
    }

    /** R5 : on ne rembourse jamais plus que le total réglé (300 000), ni une pièce de frais négative. */
    @Test
    void remiseSuperieureACentPourCent_remboursementJamaisAuDelaDuRegle() throws Exception {
        Scene scene = desistements.scene("REV-REM", Parametres.standard());
        UUID id = creer(scene);
        executer(patch(FICHE, id), "{\"remise\":2}");

        executer(post(VALIDATION, id), VALIDATION_COMPLETE);

        assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT, CODE_FRAIS_DESISTEMENT))
                .isNotEmpty()
                .extracting(Piece::montant)
                .allSatisfy(m -> assertThat(m).isNotNegative().isLessThanOrEqualTo(new BigDecimal("300000")));
    }

    /** L345-368 : la pénalité saisie est celle enregistrée, pas celle recalculée depuis une remise arrondie à 6 décimales. */
    @Test
    void penaliteSaisie_conserveeAuCentime() throws Exception {
        UUID id = creer(desistements.scene("REV-PEN", Parametres.standard()));

        executer(patch(FICHE, id), "{\"penaliteApresRemise\":33333.33}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penaliteApresRemise", closeTo(33333.33, 1e-6)));
    }

    /** §11 : motif facultatif, observations facultatives ; la validation ne doit pas tomber en 400 non documenté. */
    @Test
    void validation_sansMotifNiObservations_acceptee() throws Exception {
        Scene scene = desistements.scene("REV-CAUSE", Parametres.standard());
        String reponse = executer(post(CREER, scene.dossierId()), "{\"dateDemande\":\"2026-10-01T00:00:00\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));

        executer(post(VALIDATION, id), VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());
    }

    /** Version non contestable du test verrouillé : commercial restreint à un AUTRE projet (pas non affecté, §6.3). */
    @Test
    void commercialRestreintAUnAutreProjet_neVoitNiLaLigneNiLaFiche() throws Exception {
        Scene scene = desistements.scene("REV-VIS", Parametres.standard());
        Scene autre = desistements.scene("REV-VIS2", Parametres.standard());
        UUID id = creer(scene);
        AdmUser horsProjet = desistements.compteHabilite("TST-DES-REV-HORS", autre.projetId(), COMMERCIAL);
        RequestPostProcessor comme = FixtureDossierVente.connecte(horsProjet, COMMERCIAL);

        mockMvc.perform(get("/api/desistements").param("size", "200").with(comme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[*].id", not(hasItem(id.toString()))));
        mockMvc.perform(get(FICHE, id).with(comme)).andExpect(status().isNotFound());
        mockMvc.perform(get(CREER, scene.dossierId()).with(comme)).andExpect(status().isNotFound());
    }

    private UUID creer(Scene scene) throws Exception {
        String reponse = executer(post(CREER, scene.dossierId()), CREATION).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private ResultActions executer(MockHttpServletRequestBuilder requete, String corps) throws Exception {
        return mockMvc.perform(requete.with(FixtureDossierVente.connecte(admin, ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }
}
