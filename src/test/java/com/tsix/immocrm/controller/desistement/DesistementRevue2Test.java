package com.tsix.immocrm.controller.desistement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDesistements;
import com.tsix.immocrm.fixtures.FixtureDesistements.LigneReglement;
import com.tsix.immocrm.fixtures.FixtureDesistements.Parametres;
import com.tsix.immocrm.fixtures.FixtureDesistements.Piece;
import com.tsix.immocrm.fixtures.FixtureDesistements.Scene;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_FRAIS_DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_REMBOURSEMENT_DESISTEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 2ᵉ tour de revue (argent) du lot désistements : scène standard, prix 1 000 000, réglé 300 000, frais dus 20 000. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DesistementRevue2Test {

    private static final String ADMIN = "Administrateur";
    private static final String CREER = "/api/dossiers/{id}/desistement";
    private static final String FICHE = "/api/desistements/{id}";
    private static final String VALIDATION = "/api/desistements/{id}/validation";
    private static final String REMBOURSEMENT = "/api/desistements/{id}/remboursement";
    private static final String CREATION = "{\"dateDemande\":\"2026-10-01T00:00:00\",\"observations\":\"Test\"}";
    private static final String VALIDATION_COMPLETE =
            "{\"dateValidation\":\"2026-10-05T00:00:00\",\"dateLegalisation\":\"2026-10-06T00:00:00\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDesistements desistements;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private EntityManager em;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.compte("TST-DES-REV2", ADMIN);
    }

    /**
     * Tranche à taux 0 (13 / 17 tranches en prod) : la pénalité saisie est la seule possible. Sale.cs:CancelFolder L3860
     * écrit la pénalité enregistrée ; la fiche affichait 50 000 / 230 000, la validation doit écrire ces montants.
     */
    @Test
    void tauxNul_penaliteSaisie_ecriteTelleQuelleALaValidation() throws Exception {
        Scene scene = desistements.scene("REV2-T0", Parametres.standard().avecTaux(BigDecimal.ZERO));
        UUID id = creer(scene);
        executer(patch(FICHE, id), "{\"penaliteApresRemise\":50000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penaliteApresRemise", closeTo(50000, 1e-6)))
                .andExpect(jsonPath("$.aRembourser", closeTo(230000, 1e-6)));

        executer(post(VALIDATION, id), VALIDATION_COMPLETE).andExpect(status().isOk());

        assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT))
                .extracting(Piece::montant).singleElement()
                .satisfies(m -> assertThat(m).isEqualByComparingTo("50000"));
        assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT))
                .extracting(Piece::montant).singleElement()
                .satisfies(m -> assertThat(m).isEqualByComparingTo("230000"));
    }

    /** Même tranche : une modification sans montant (observations) ne doit pas effacer la pénalité saisie. */
    @Test
    void tauxNul_penaliteSaisie_survitAUneModificationDesObservations() throws Exception {
        UUID id = creer(desistements.scene("REV2-T0-OBS", Parametres.standard().avecTaux(BigDecimal.ZERO)));
        executer(patch(FICHE, id), "{\"penaliteApresRemise\":50000}").andExpect(status().isOk());

        executer(patch(FICHE, id), "{\"observations\":\"Relance\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penaliteApresRemise", closeTo(50000, 1e-6)));
    }

    /**
     * R8 : les décaissements sur la pièce de résiliation (180 000) ne la dépassent jamais, même si un encaissement a été
     * affecté par erreur à cette pièce par {@code POST /api/dossiers/{id}/reglements}.
     */
    @Test
    void encaissementSurLaPieceDeRemboursement_neRelevePasLePlafondR8() throws Exception {
        Scene scene = desistements.scene("REV2-CRED", Parametres.standard());
        UUID id = creer(scene);
        executer(post(VALIDATION, id), VALIDATION_COMPLETE).andExpect(status().isOk());
        rembourser(id, scene, "180000").andExpect(status().isCreated());
        UUID piece = pieceDeRemboursement(id);

        executer(post("/api/dossiers/{id}/reglements", scene.dossierId()),
                "{\"montant\":180000,\"dateEcheance\":\"2026-10-10T00:00:00\",\"typeId\":\"" + scene.typeReglementId()
                        + "\",\"affectations\":[{\"echeanceId\":\"" + piece + "\",\"montant\":180000}]}");
        rembourser(id, scene, "180000");

        BigDecimal decaisse = desistements.reglementsSurCategorie(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)
                .stream().filter(l -> Boolean.FALSE.equals(l.credit())).map(LigneReglement::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(decaisse).isLessThanOrEqualTo(new BigDecimal("180000"));
    }

    /**
     * En prod le statut DESISTEMENT a « Peut effectuer un désistement » (BoolField010 = 1) : après légalisation, un second
     * désistement ne doit pas générer une seconde pièce de remboursement sur les mêmes 300 000 réglés.
     */
    @Test
    void secondDesistementApresLegalisation_pasDeSecondeQuittanceDeRemboursement() throws Exception {
        Scene scene = desistements.scene("REV2-BIS", Parametres.standard());
        UUID premier = creer(scene);
        executer(post(VALIDATION, premier), VALIDATION_COMPLETE).andExpect(status().isOk());
        desistements.eligible(statuts.statut(StatutDossier.DESISTEMENT).getId(), true);

        ResultActions second = executer(post(CREER, scene.dossierId()), CREATION);
        if (second.andReturn().getResponse().getStatus() == 201) {
            UUID id = UUID.fromString(JsonPath.read(second.andReturn().getResponse().getContentAsString(), "$.id"));
            executer(post(VALIDATION, id), VALIDATION_COMPLETE);
        }

        assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)).hasSize(1);
        mockMvc.perform(get(FICHE, premier).with(FixtureDossierVente.connecte(admin, ADMIN)))
                .andExpect(status().isOk());
    }

    private UUID pieceDeRemboursement(UUID desistementId) {
        em.flush();
        Object id = em.createNativeQuery("""
                        SELECT CAST(d.PKey AS NVARCHAR(36)) FROM ComDocument d
                        JOIN ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId
                        WHERE d.ComSaleWithdrawalCancellationId = :id AND c.Code = :code""")
                .setParameter("id", desistementId).setParameter("code", CODE_REMBOURSEMENT_DESISTEMENT)
                .getSingleResult();
        return UUID.fromString((String) id);
    }

    private ResultActions rembourser(UUID id, Scene scene, String montant) throws Exception {
        return executer(post(REMBOURSEMENT, id), "{\"montant\":" + montant
                + ",\"dateEcheance\":\"2026-10-10T00:00:00\",\"typeId\":\"" + scene.typeReglementId() + "\"}");
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
