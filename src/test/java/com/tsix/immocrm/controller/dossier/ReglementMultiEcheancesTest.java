package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST/GET /api/dossiers/{id}/reglements} et {@code POST .../{reglementId}/affectations} :
 * un règlement réparti sur plusieurs échéances, et son reste à affecter (FEATURE-reglement-multi-echeances.md §5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementMultiEcheancesTest {

    private static final String REGLEMENTS = "/api/dossiers/{id}/reglements";
    private static final String AFFECTATIONS = "/api/dossiers/{id}/reglements/{reglementId}/affectations";
    private static final String RESTE = "$.resteAAffecter";
    private static final String AFFECTE = "$.montantAffecte";
    private static final String M_1000 = "1000.00";
    private static final String M_5000 = "5000.00";
    private static final String M_7200 = "7200.00";
    private static final String M_10000 = "10000.00";
    private static final String M_60000 = "60000.00";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private ComSettlementCategoryRepository categoryRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private EntityManager entityManager;

    private ComFolder dossier;
    private ComDocument preReservation;
    private ComDocument avance;
    private ComDocument syndic;
    private StlSettlementType type;
    private CfgTier payeur;

    @BeforeEach
    void setUp() {
        dossier = newDossier("A");
        // Restes dus 5 000 / 95 000 / 7 200, de la plus ancienne à la plus récente.
        preReservation = newEcheance(dossier, "PRE-RES-RME", M_5000, LocalDateTime.of(2026, 1, 10, 0, 0));
        avance = newEcheance(dossier, "AVANCE-RME", "95000.00", LocalDateTime.of(2026, 2, 10, 0, 0));
        syndic = newEcheance(dossier, "SYNDIC-RME", M_7200, LocalDateTime.of(2026, 3, 10, 0, 0));

        type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-RME");
        type.setDescription("VIREMENT RME");
        type.setIsReferenceMandatory(false);
        type.setIsBankMandatory(false);
        settlementTypeRepository.save(type);

        payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-RME");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ComFolder newDossier(String suffixe) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RME-" + suffixe + "-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-RME-" + suffixe);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private ComDocument newEcheance(ComFolder folder, String categorieCode, String montant, LocalDateTime date) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(categorieCode);
        categorie.setDescription("Catégorie " + categorieCode);
        categoryRepository.save(categorie);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setComSettlementCategory(categorie);
        echeance.setDocDate(date);
        echeance.setAmount(new BigDecimal(montant));
        return documentRepository.save(echeance);
    }

    private static String ligne(ComDocument echeance, String montant) {
        return "{\"echeanceId\":\"" + echeance.getId() + "\",\"montant\":" + montant + "}";
    }

    private String corps(String montant, String... lignes) {
        return "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\""
                + ",\"affectations\":[" + String.join(",", lignes) + "]}";
    }

    private ResultActions creer(UUID dossierId, String corps) throws Exception {
        return mockMvc.perform(post(REGLEMENTS, dossierId).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID creerEtLireId(String corps) throws Exception {
        String reponse = creer(dossier.getId(), corps).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private ResultActions affecter(UUID dossierId, UUID reglementId, String... lignes) throws Exception {
        return mockMvc.perform(post(AFFECTATIONS, dossierId, reglementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"affectations\":[" + String.join(",", lignes) + "]}"));
    }

    private long reglements() {
        return settlementRepository.count();
    }

    private long lignes() {
        return settlementLineRepository.count();
    }

    // ------------------------------------------------------------------
    // 1-2. Création
    // ------------------------------------------------------------------

    @Test
    void creer_surDeuxEcheances_sommeEgaleMontant_unReglementDeuxLignes() throws Exception {
        long reglementsAvant = reglements();
        long lignesAvant = lignes();

        creer(dossier.getId(), corps("52000.00", ligne(preReservation, "2000.00"), ligne(avance, "50000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(52000.00))
                .andExpect(jsonPath(AFFECTE).value(52000.00))
                .andExpect(jsonPath(RESTE).value(0.0))
                .andExpect(jsonPath("$.montantAffecteAutresDossiers").value(0.0))
                .andExpect(jsonPath("$.statut").value("IN_WORK"))
                .andExpect(jsonPath("$.affectations.length()").value(2))
                .andExpect(jsonPath("$.affectations[0].echeanceId").value(preReservation.getId().toString()))
                .andExpect(jsonPath("$.affectations[0].echeanceLibelle").value("PRE-RES-RME"))
                .andExpect(jsonPath("$.affectations[0].montant").value(2000.00))
                .andExpect(jsonPath("$.affectations[1].echeanceId").value(avance.getId().toString()));

        assertThat(new long[]{reglements() - reglementsAvant, lignes() - lignesAvant}).containsExactly(1, 2);

        // L'échéancier lit les DocDate décroissantes : avance (février) puis pré-réservation (janvier).
        mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + preReservation.getId() + "')].montantRegle").value(2000.0))
                .andExpect(jsonPath("$.content[?(@.id == '" + preReservation.getId() + "')].reste").value(3000.0))
                .andExpect(jsonPath("$.content[?(@.id == '" + avance.getId() + "')].montantRegle").value(50000.0))
                .andExpect(jsonPath("$.content[?(@.id == '" + avance.getId() + "')].reste").value(45000.0))
                .andExpect(jsonPath("$.content[?(@.id == '" + syndic.getId() + "')].montantRegle").value(0.0));
    }

    @Test
    void creer_sommeInferieureAuMontant_resteAAffecterDansLaReponseEtLaListe() throws Exception {
        creer(dossier.getId(), corps(M_60000, ligne(preReservation, M_5000), ligne(syndic, "200.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(AFFECTE).value(5200.00))
                .andExpect(jsonPath(RESTE).value(54800.00));

        mockMvc.perform(get(REGLEMENTS, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].montant").value(60000.00))
                .andExpect(jsonPath("$[0].montantAffecte").value(5200.00))
                .andExpect(jsonPath("$[0].resteAAffecter").value(54800.00))
                .andExpect(jsonPath("$[0].affectations.length()").value(2));
    }

    // ------------------------------------------------------------------
    // 3. Rejets en 400, sans aucune écriture
    // ------------------------------------------------------------------

    private void rejeteSansEcrire(String corps, String extraitMessage) throws Exception {
        long reglementsAvant = reglements();
        long lignesAvant = lignes();

        creer(dossier.getId(), corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(extraitMessage)));

        assertThat(new long[]{reglements(), lignes()}).containsExactly(reglementsAvant, lignesAvant);
    }

    @Test
    void creer_sommeSuperieureAuMontant_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps("6000.00", ligne(preReservation, M_5000), ligne(avance, "1500.00")),
                "Le reste à affecter doit être >= 0");
    }

    @Test
    void creer_ligneAuDelaDuResteDu_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps("9000.00", ligne(preReservation, "5000.01")), "PRE-RES-RME");
    }

    @Test
    void creer_ligneAZero_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps(M_1000, ligne(preReservation, M_1000), ligne(avance, "0")),
                "strictement positif");
    }

    @Test
    void creer_affectationsVides_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps(M_1000), "Au moins une échéance");
    }

    @Test
    void creer_affectationsAbsentes_400SansEcriture() throws Exception {
        long reglementsAvant = reglements();
        String sansAffectations = "{\"montant\":1000.00,\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\"}";

        creer(dossier.getId(), sansAffectations).andExpect(status().isBadRequest());

        assertThat(reglements()).isEqualTo(reglementsAvant);
    }

    @Test
    void creer_echeanceDUnAutreDossier_400SansEcriture() throws Exception {
        ComDocument etrangere = newEcheance(newDossier("B"), "AUTRE-RME", M_1000, LocalDateTime.of(2026, 1, 1, 0, 0));

        rejeteSansEcrire(corps(M_1000, ligne(etrangere, M_1000)), "n'appartient pas à ce dossier");
    }

    @Test
    void creer_echeanceEnDouble_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps("2000.00", ligne(preReservation, M_1000), ligne(preReservation, M_1000)),
                "qu'une fois");
    }

    @Test
    void creer_enTeteInvalide_400SansEcriture() throws Exception {
        rejeteSansEcrire(corps("0", ligne(preReservation, M_1000)), "strictement positif");
    }

    // ------------------------------------------------------------------
    // 4. 404 sur dossier inconnu
    // ------------------------------------------------------------------

    @Test
    void creer_dossierInconnu_404() throws Exception {
        creer(UUID.randomUUID(), corps(M_1000, ligne(preReservation, M_1000)))
                .andExpect(status().isNotFound());
    }

    @Test
    void lister_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(REGLEMENTS, UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void lister_aucunReglement_tableauVide() throws Exception {
        mockMvc.perform(get(REGLEMENTS, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void affecter_dossierInconnu_404() throws Exception {
        UUID reglementId = creerEtLireId(corps("6000.00", ligne(preReservation, M_5000)));

        affecter(UUID.randomUUID(), reglementId, ligne(avance, M_1000)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // 5. Affecter le reste
    // ------------------------------------------------------------------

    @Test
    void affecter_nominal_resteDiminueEtMontantInchange() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_60000, ligne(preReservation, M_5000)));

        affecter(dossier.getId(), reglementId, ligne(avance, "50000.00"), ligne(syndic, "200.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(60000.00))
                .andExpect(jsonPath(AFFECTE).value(55200.00))
                .andExpect(jsonPath(RESTE).value(4800.00))
                .andExpect(jsonPath("$.affectations.length()").value(3));

        entityManager.flush();
        entityManager.clear();
        assertThat(settlementRepository.findById(reglementId).orElseThrow().getAmount())
                .isEqualByComparingTo(M_60000);
    }

    @Test
    void affecter_sommeSuperieureAuReste_400SansEcriture() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_10000, ligne(preReservation, M_5000)));
        long lignesAvant = lignes();

        affecter(dossier.getId(), reglementId, ligne(avance, "5000.01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("dépassent le reste à affecter")));

        assertThat(lignes()).isEqualTo(lignesAvant);
    }

    @Test
    void affecter_echeanceDejaDansLeReglement_400SansEcriture() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_10000, ligne(preReservation, M_1000)));
        long lignesAvant = lignes();

        affecter(dossier.getId(), reglementId, ligne(preReservation, M_1000))
                .andExpect(status().isBadRequest());

        assertThat(lignes()).isEqualTo(lignesAvant);
    }

    @Test
    void affecter_reglementAnnule_409SansEcriture() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_10000, ligne(preReservation, M_1000)));
        StlSettlement reglement = settlementRepository.findById(reglementId).orElseThrow();
        reglement.setStatus("CANCELED");
        settlementRepository.save(reglement);
        long lignesAvant = lignes();

        affecter(dossier.getId(), reglementId, ligne(avance, M_1000)).andExpect(status().isConflict());

        assertThat(lignes()).isEqualTo(lignesAvant);
    }

    @Test
    void affecter_reglementSansLigneSurCeDossier_404() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_10000, ligne(preReservation, M_1000)));
        ComFolder autre = newDossier("C");
        ComDocument echeanceAutre = newEcheance(autre, "AUTRE-RME-C", M_1000, LocalDateTime.of(2026, 1, 1, 0, 0));

        affecter(autre.getId(), reglementId, ligne(echeanceAutre, "500.00")).andExpect(status().isNotFound());
    }

    @Test
    void affecter_reglementInconnu_404() throws Exception {
        affecter(dossier.getId(), UUID.randomUUID(), ligne(avance, M_1000)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // 6-7. Cohérence avec la fiche encaissement, reçu
    // ------------------------------------------------------------------

    @Test
    void resteAAffecter_egaleMontantBrutRestantDeLaFicheEncaissement() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_60000, ligne(preReservation, M_5000), ligne(avance, M_1000)));
        entityManager.flush();
        entityManager.clear();

        String liste = mockMvc.perform(get(REGLEMENTS, dossier.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String fiche = mockMvc.perform(get("/api/encaissements/" + reglementId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        BigDecimal resteDossier = new BigDecimal(String.valueOf(JsonPath.<Object>read(liste, "$[0].resteAAffecter")));
        BigDecimal montantBrutRestant = new BigDecimal(String.valueOf(JsonPath.<Object>read(fiche, "$.montantBrutRestant")));
        assertThat(resteDossier).isEqualByComparingTo("54000").isEqualByComparingTo(montantBrutRestant);
    }

    @Test
    void recu_dUnReglementADeuxLignes_200() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_7200, ligne(preReservation, M_5000), ligne(syndic, "2200.00")));

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu", dossier.getId(), reglementId))
                .andExpect(status().isOk());
    }

    @Test
    void autorisationVersement_dUnReglementADeuxLignes_200() throws Exception {
        UUID reglementId = creerEtLireId(corps(M_7200, ligne(preReservation, M_5000), ligne(syndic, "2200.00")));

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        dossier.getId(), reglementId))
                .andExpect(status().isOk());
    }
}
