package com.tsix.immocrm.controller.desistement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDesistements;
import com.tsix.immocrm.fixtures.FixtureDesistements.LigneReglement;
import com.tsix.immocrm.fixtures.FixtureDesistements.Parametres;
import com.tsix.immocrm.fixtures.FixtureDesistements.Piece;
import com.tsix.immocrm.fixtures.FixtureDesistements.Scene;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.fixtures.FixtureWorkflow;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_FRAIS_DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_REMBOURSEMENT_ANNULATION;
import static com.tsix.immocrm.fixtures.FixtureDesistements.CODE_REMBOURSEMENT_DESISTEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Désistements (FEATURE-desistements.md §3, §6, §7 bis, §8) : rejets d'abord, puis calcul R3-R5, effets de la
 * validation R6, remboursement R8 et suppression. Scène standard : prix 1 000 000, réglé 300 000, taux 10 % sur le
 * prix, remise ancien client 50 %, frais toujours payés restant dus 20 000 (+ 5 000 de frais non « toujours payés »).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DesistementControllerTest {

    private static final String CREER = "/api/dossiers/{id}/desistement";
    private static final String SIMULATION = "/api/dossiers/{id}/desistement/simulation";
    private static final String DESISTEMENT = "/api/desistements/{id}";
    private static final String VALIDATION = "/api/desistements/{id}/validation";
    private static final String REMBOURSEMENT = "/api/desistements/{id}/remboursement";
    private static final String ADMIN = "Administrateur";
    private static final String OBSERVATIONS = "Financement refusé";
    private static final String DATE_DEMANDE = "2026-10-01T00:00:00";
    private static final String DATE_VALIDATION = "2026-10-05T00:00:00";
    private static final String DATE_LEGALISATION = "2026-10-06T00:00:00";
    private static final String VALIDATION_COMPLETE = "{\"dateValidation\":\"" + DATE_VALIDATION
            + "\",\"dateLegalisation\":\"" + DATE_LEGALISATION + "\"}";
    private static final String PENALITE = "$.penaliteApresRemise";
    private static final String A_REMBOURSER = "$.aRembourser";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDesistements desistements;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private FixtureWorkflow workflow;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.compte("TST-DES-ADM", ADMIN);
    }

    // ------------------------------------------------------------------ rejets

    @Test
    void r1_secondDesistementNonLegalise_409() throws Exception {
        Scene scene = desistements.scene("R1", Parametres.standard());
        creerEtLireId(scene);

        creer(scene, corpsCreation(null)).andExpect(status().isConflict());
    }

    @Test
    void r2_statutSansBoolField010_creationRefusee409() throws Exception {
        Scene scene = desistements.scene("R2", Parametres.standard().avecStatutNonEligible());

        creer(scene, corpsCreation(null)).andExpect(status().isConflict());
    }

    @Test
    void r6_validationSansDateDeValidation_400_dossierInchange() throws Exception {
        Scene scene = desistements.scene("R6-SANS-DATE", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, "{\"dateLegalisation\":\"" + DATE_LEGALISATION + "\"}").andExpect(status().isBadRequest());

        assertThat(statutDuDossier(scene)).isEqualTo(StatutDossier.RESERVATION.getCode());
    }

    @Test
    void r6_penaliteNonNulleSansDateDeLegalisation_400_dossierInchange() throws Exception {
        Scene scene = desistements.scene("R6-SANS-LEG", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, "{\"dateValidation\":\"" + DATE_VALIDATION + "\"}").andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(statutDuDossier(scene)).isEqualTo(StatutDossier.RESERVATION.getCode()),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT)).isEmpty());
    }

    @Test
    void r7_syndicEnCours_409_rienNEstEcrit() throws Exception {
        Scene scene = desistements.scene("R7", Parametres.standard());
        UUID id = creerEtLireId(scene);
        desistements.syndicEnCours(scene.dossierId());

        valider(id, VALIDATION_COMPLETE).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(statutDuDossier(scene)).isEqualTo(StatutDossier.RESERVATION.getCode()),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT,
                        CODE_REMBOURSEMENT_DESISTEMENT)).isEmpty(),
                () -> assertThat(dossierDuLot(scene)).isEqualTo(scene.dossierId()));
    }

    /** §8.7 : la validation passe par changerStatut ; transition RESERVATION → DESISTEMENT non paramétrée = 409. */
    @Test
    void r6_transitionVersDesistementNonParametree_409_rienNEstEcrit() throws Exception {
        Scene scene = desistements.scene("WF", Parametres.standard());
        workflow.transition(statuts.reservation(), statuts.statut(StatutDossier.CONCRETISATION), false);
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(statutDuDossier(scene)).isEqualTo(StatutDossier.RESERVATION.getCode()),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT,
                        CODE_REMBOURSEMENT_DESISTEMENT)).isEmpty());
    }

    // ------------------------------------------------------------------ calcul R3-R5 (simulation)

    /** R3 base prix, client non ancien : la remise de la tranche ne s'applique pas. */
    @Test
    void r3_basePrix_clientNonAncien_penaliteEgaleTauxFoisPrix() throws Exception {
        Scene scene = desistements.scene("R3-PRIX", Parametres.standard());

        String json = simuler(scene);

        assertAll(
                () -> assertThat(nombre(json, PENALITE)).isEqualByComparingTo("100000"),
                () -> assertThat(nombre(json, A_REMBOURSER)).isEqualByComparingTo("180000"));
    }

    @Test
    void r3_baseRegle_penaliteSurLeTotalRegle() throws Exception {
        Scene scene = desistements.scene("R3-REGLE", Parametres.standard().avecBaseRegle());

        String json = simuler(scene);

        assertAll(
                () -> assertThat(nombre(json, PENALITE)).isEqualByComparingTo("30000"),
                () -> assertThat(nombre(json, A_REMBOURSER)).isEqualByComparingTo("250000"));
    }

    @Test
    void r3_ancienClient_remiseDeLaTrancheAppliquee() throws Exception {
        Scene scene = desistements.scene("R3-ANCIEN", Parametres.standard().avecAncienClient());

        String json = simuler(scene);

        assertAll(
                () -> assertThat(nombre(json, PENALITE)).isEqualByComparingTo("50000"),
                () -> assertThat(nombre(json, A_REMBOURSER)).isEqualByComparingTo("230000"));
    }

    /** R4 : seules les pièces IsAlwaysPayed + IsCredit + frais comptent (les 5 000 non « toujours payés » non). */
    @Test
    void r4_resteFrais_seulementLesCategoriesToujoursPayees() throws Exception {
        Scene scene = desistements.scene("R4", Parametres.standard());

        String json = simuler(scene);

        assertAll(
                () -> assertThat(nombre(json, "$.resteFraisARegler")).isEqualByComparingTo("20000"),
                () -> assertThat(nombre(json, "$.totalRegle")).isEqualByComparingTo("300000"));
    }

    @Test
    void r5_aRembourser_plancherAZero() throws Exception {
        Scene scene = desistements.scene("R5", Parametres.standard().avecRegle(new BigDecimal("50000")));

        String json = simuler(scene);

        assertThat(nombre(json, A_REMBOURSER)).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ validation R6

    @Test
    void r6_validation_piecesFraisEtRemboursement_montantsEtLienAuDesistement() throws Exception {
        Scene scene = desistements.scene("R6-PIECES", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT)).singleElement()
                        .satisfies(p -> assertAll(
                                () -> assertThat(p.montant()).isEqualByComparingTo("100000"),
                                () -> assertThat(p.desistementId()).isEqualToIgnoringCase(id.toString()))),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)).singleElement()
                        .satisfies(p -> assertAll(
                                () -> assertThat(p.montant()).isEqualByComparingTo("180000"),
                                () -> assertThat(p.desistementId()).isEqualToIgnoringCase(id.toString()))));
    }

    /** §8.4 : la catégorie IsSaleResiliation du désistement est choisie par Code, jamais celle de l'annulation. */
    @Test
    void r6_validation_pieceDeResiliation_jamaisDansLaCategorieAnnulation() throws Exception {
        Scene scene = desistements.scene("R6-CAT", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_ANNULATION)).isEmpty();
    }

    @Test
    void r6_validation_dossierEnDesistement_lotLibere_dateDAnnulationPosee() throws Exception {
        Scene scene = desistements.scene("R6-STATUT", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        ComFolder apres = dossier(scene);
        assertAll(
                () -> assertThat(apres.getStrStatus().getCode()).isEqualTo(StatutDossier.DESISTEMENT.getCode()),
                () -> assertThat(apres.getCancelDate()).isNotNull(),
                () -> assertThat(dossierDuLot(scene)).isNull());
    }

    @Test
    void r6_validation_causeDuDossier_motifDeuxPointsObservations() throws Exception {
        Scene scene = desistements.scene("R6-MOTIF", Parametres.standard());
        UUID motif = desistements.motif("Raisons personnelles");
        UUID id = creerEtLireId(scene, corpsCreation(motif));

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        assertThat(dossier(scene).getCancelDescription()).isEqualTo("Raisons personnelles : " + OBSERVATIONS);
    }

    /** §8.1 : le lien dossier ↔ désistement est ComSaleWithdrawalCancellation.ComSaleId ; validé = ValidityDate. */
    @Test
    void r6_validation_desistementRattacheAuDossier_dateDeValidationPosee() throws Exception {
        Scene scene = desistements.scene("R6-LIEN", Parametres.standard());
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        FixtureDesistements.Desistement relu = desistements.desistement(id);
        assertAll(
                () -> assertThat(relu.dossierId()).isEqualToIgnoringCase(scene.dossierId().toString()),
                () -> assertThat(relu.dateValidation()).isNotNull());
    }

    /** R6 : légalisation exigée seulement si pénalité ≠ 0 ; pénalité nulle = pas de pièce de frais. */
    @Test
    void r6_penaliteNulle_valideeSansLegalisation_sansPieceDeFrais() throws Exception {
        Scene scene = desistements.scene("R6-ZERO", Parametres.standard().avecTaux(BigDecimal.ZERO));
        UUID id = creerEtLireId(scene);

        valider(id, "{\"dateValidation\":\"" + DATE_VALIDATION + "\"}").andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT)).isEmpty(),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)).singleElement()
                        .extracting(Piece::montant).satisfies(m -> assertThat(m).isEqualByComparingTo("280000")));
    }

    /** R5 + R6 : à rembourser nul = pas de pièce de résiliation, la pièce de frais reste. */
    @Test
    void r6_aRembourserNul_aucunePieceDeResiliation() throws Exception {
        Scene scene = desistements.scene("R6-RIEN", Parametres.standard().avecRegle(new BigDecimal("50000")));
        UUID id = creerEtLireId(scene);

        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());

        assertAll(
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)).isEmpty(),
                () -> assertThat(desistements.pieces(scene.dossierId(), CODE_FRAIS_DESISTEMENT)).hasSize(1));
    }

    // ------------------------------------------------------------------ remboursement R8

    @Test
    void r8_remboursement_reglementAuDebitAffecteALaPieceDeResiliation() throws Exception {
        Scene scene = desistements.scene("R8", Parametres.standard());
        UUID id = valide(scene);

        rembourser(id, scene, "100000").andExpect(status().isCreated());

        assertThat(desistements.reglementsSurCategorie(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT))
                .singleElement()
                .satisfies(l -> assertAll(
                        () -> assertThat(l.credit()).isFalse(),
                        () -> assertThat(l.montant()).isEqualByComparingTo("100000")));
    }

    @Test
    void r8_remboursementAuDelaDuResteARembourser_409_rienNEstEcrit() throws Exception {
        Scene scene = desistements.scene("R8-PLAFOND", Parametres.standard());
        UUID id = valide(scene);

        rembourser(id, scene, "180000.01").andExpect(status().isConflict());

        assertThat(desistements.reglementsSurCategorie(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT)).isEmpty();
    }

    @Test
    void r8_remboursementsCumules_plafonnesAuReste() throws Exception {
        Scene scene = desistements.scene("R8-CUMUL", Parametres.standard());
        UUID id = valide(scene);
        rembourser(id, scene, "100000").andExpect(status().isCreated());

        rembourser(id, scene, "80000.01").andExpect(status().isConflict());
        rembourser(id, scene, "80000").andExpect(status().isCreated());

        assertThat(desistements.reglementsSurCategorie(scene.dossierId(), CODE_REMBOURSEMENT_DESISTEMENT))
                .extracting(LigneReglement::montant)
                .map(BigDecimal::stripTrailingZeros)
                .containsExactlyInAnyOrder(new BigDecimal("1E+5"), new BigDecimal("8E+4"));
    }

    // ------------------------------------------------------------------ suppression

    @Test
    void suppression_brouillon_acceptee_puisNouveauDesistementPossible() throws Exception {
        Scene scene = desistements.scene("SUPPR", Parametres.standard());
        UUID id = creerEtLireId(scene);

        mockMvc.perform(delete(DESISTEMENT, id).with(commeAdmin())).andExpect(status().is2xxSuccessful());

        creer(scene, corpsCreation(null)).andExpect(status().isCreated());
    }

    @Test
    void suppression_desistementValide_409() throws Exception {
        Scene scene = desistements.scene("SUPPR-VAL", Parametres.standard());
        UUID id = valide(scene);

        mockMvc.perform(delete(DESISTEMENT, id).with(commeAdmin())).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ outils

    private RequestPostProcessor commeAdmin() {
        return FixtureDossierVente.connecte(admin, ADMIN);
    }

    private static String corpsCreation(UUID motifId) {
        String motif = motifId == null ? "" : ",\"motifId\":\"" + motifId + "\"";
        return "{\"dateDemande\":\"" + DATE_DEMANDE + "\",\"observations\":\"" + OBSERVATIONS + "\"" + motif + "}";
    }

    private ResultActions creer(Scene scene, String corps) throws Exception {
        return mockMvc.perform(post(CREER, scene.dossierId()).with(commeAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID creerEtLireId(Scene scene) throws Exception {
        return creerEtLireId(scene, corpsCreation(null));
    }

    private UUID creerEtLireId(Scene scene, String corps) throws Exception {
        String reponse = creer(scene, corps).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private ResultActions valider(UUID id, String corps) throws Exception {
        return mockMvc.perform(post(VALIDATION, id).with(commeAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID valide(Scene scene) throws Exception {
        UUID id = creerEtLireId(scene);
        valider(id, VALIDATION_COMPLETE).andExpect(status().is2xxSuccessful());
        return id;
    }

    private ResultActions rembourser(UUID id, Scene scene, String montant) throws Exception {
        String corps = "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-10-10T00:00:00\",\"typeId\":\""
                + scene.typeReglementId() + "\"}";
        return mockMvc.perform(post(REMBOURSEMENT, id).with(commeAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String simuler(Scene scene) throws Exception {
        return mockMvc.perform(get(SIMULATION, scene.dossierId()).with(commeAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal nombre(String json, String chemin) {
        Object valeur = JsonPath.read(json, chemin);
        return new BigDecimal(valeur.toString());
    }

    private ComFolder dossier(Scene scene) {
        em.flush();
        em.clear();
        return folderRepository.findById(scene.dossierId()).orElseThrow();
    }

    private String statutDuDossier(Scene scene) {
        return dossier(scene).getStrStatus().getCode();
    }

    private UUID dossierDuLot(Scene scene) {
        em.flush();
        em.clear();
        return stkItemRepository.findById(scene.articleId()).orElseThrow().getComFolderId();
    }
}
