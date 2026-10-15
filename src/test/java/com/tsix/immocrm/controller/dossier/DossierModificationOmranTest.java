package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrôles OMRAN de la modification de la fiche — FEATURE-dossier-modification.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierModificationOmranTest {

    private static final String MODIFIER = "/api/dossiers/{id}";
    private static final String DROITS = "/api/dossiers/{id}/edition";
    private static final String FICHE = "/api/dossiers/{id}/fiche";
    private static final String ADMIN = "Administrateur";
    private static final String CHEF = "Commercial Chef";
    private static final String VERROUS = "$.champsVerrouilles";
    private static final LocalDateTime DEBUT_JANVIER = LocalDateTime.of(2025, 1, 10, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierKindRepository tierKindRepository;
    @Autowired private EntityManager em;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        dossier = fixture.dossier("DOS-MOD", fixture.tranche("MOD-TR", true), StatutDossier.CONCRETISATION);
    }

    private ResultActions modifier(ComFolder d, String role, String corps) throws Exception {
        return mockMvc.perform(patch(MODIFIER, d.getId()).with(user("modification").roles(role))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ComFolder relu(ComFolder d) {
        em.flush();
        em.clear();
        return folderRepository.findById(d.getId()).orElseThrow();
    }

    private void enregistrer(ComFolder d) {
        folderRepository.save(d);
        em.flush();
    }

    // ------------------------------------------------------------------ B1 date du dossier

    @Test
    void dateDossier_anterieureALaProspection_400MessageOmran_rienNEstEcrit() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-MOD-" + UUID.randomUUID());
        prospection.setCfgProspect(dossier.getCfgCustomer());
        prospection.setDocDate(DEBUT_JANVIER);
        dossier.setComProspection(prospectionRepository.save(prospection));
        dossier.setDocDate(DEBUT_JANVIER);
        enregistrer(dossier);

        modifier(dossier, CHEF, "{\"dateDossier\":\"2025-01-05T00:00:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La date 'Date dossier: 05/01/2025' doit être supérieur ou égal "
                        + "à la date 'Date prospection: 10/01/2025'."));

        em.clear();
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getDocDate()).isEqualTo(DEBUT_JANVIER);
    }

    @Test
    void dateDossier_parLeChef_ecrite() throws Exception {
        modifier(dossier, CHEF, "{\"dateDossier\":\"2025-02-01T00:00:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.champsModifies", hasItem("dateDossier")));

        assertThat(relu(dossier).getDocDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 0, 0));
    }

    @Test
    void dateDossier_neSeVidePas_400() throws Exception {
        modifier(dossier, ADMIN, "{\"champsAVider\":[\"dateDossier\"]}").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ B2 prix de vente

    @Test
    void prixVenteHTEtRemisePourcentageEnsemble_400_rienNEstEcrit() throws Exception {
        modifier(dossier, ADMIN, "{\"prixVenteHT\":950000,\"remisePourcentage\":5}")
                .andExpect(status().isBadRequest());

        assertThat(relu(dossier).getDiscountAmount()).isNull();
    }

    @Test
    void prixVenteHT_nominal_montantDeRemiseEtPrixAttendus() throws Exception {
        modifier(dossier, ADMIN, "{\"prixVenteHT\":950000,\"forcerRemise\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.champsModifies", hasItem("remise")));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.05"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("950000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1140000"),
                () -> assertThat(f.getDiscountChangeDate()).isNotNull());
    }

    @Test
    void prixVenteTTC_nominal_tauxDepuisLeTTC() throws Exception {
        modifier(dossier, ADMIN, "{\"prixVenteTTC\":1140000,\"forcerRemise\":true}")
                .andExpect(status().isOk());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.05"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1140000"));
    }

    @Test
    void prixVenteHT_auDessusDuPrixAvantRemise_400() throws Exception {
        modifier(dossier, ADMIN, "{\"prixVenteHT\":1000001,\"forcerRemise\":true}")
                .andExpect(status().isBadRequest());

        assertThat(relu(dossier).getDiscountAmount()).isNull();
    }

    @Test
    void prixVenteHT_parLeChef_403() throws Exception {
        modifier(dossier, CHEF, "{\"prixVenteHT\":950000}").andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ B3 concrétisation

    @Test
    void concretisationDejaPosee_modifieeParLeChef_403_rienNEstEcrit() throws Exception {
        dossier.setAdvanceDate(DEBUT_JANVIER);
        enregistrer(dossier);

        modifier(dossier, CHEF, "{\"concretisation\":\"2025-02-01T00:00:00\",\"minute\":\"2025-03-01T00:00:00\"}")
                .andExpect(status().isForbidden());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getAdvanceDate()).isEqualTo(DEBUT_JANVIER),
                () -> assertThat(f.getAgreementSavingDate()).isNull());
    }

    @Test
    void concretisationDejaPosee_videeParLeChef_403() throws Exception {
        dossier.setAdvanceDate(DEBUT_JANVIER);
        enregistrer(dossier);

        modifier(dossier, CHEF, "{\"champsAVider\":[\"concretisation\"]}").andExpect(status().isForbidden());

        assertThat(relu(dossier).getAdvanceDate()).isEqualTo(DEBUT_JANVIER);
    }

    @Test
    void concretisationDejaPosee_modifieeParLAdministrateur_200() throws Exception {
        dossier.setAdvanceDate(DEBUT_JANVIER);
        enregistrer(dossier);

        modifier(dossier, ADMIN, "{\"concretisation\":\"2025-02-01T00:00:00\"}").andExpect(status().isOk());

        assertThat(relu(dossier).getAdvanceDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 0, 0));
    }

    @Test
    void concretisation_premierRenseignementParLeChef_200() throws Exception {
        modifier(dossier, CHEF, "{\"concretisation\":\"2025-02-01T00:00:00\"}").andExpect(status().isOk());

        assertThat(relu(dossier).getAdvanceDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 0, 0));
    }

    // ------------------------------------------------------------------ B4 impression du contrat

    @Test
    void impressionContrat_parLeChef_403_rienNEstEcrit() throws Exception {
        modifier(dossier, CHEF, "{\"impressionContrat\":\"2025-02-01T00:00:00\"}").andExpect(status().isForbidden());

        assertThat(relu(dossier).getContractPrintDate()).isNull();
    }

    @Test
    void impressionContrat_parLAdministrateur_200() throws Exception {
        modifier(dossier, ADMIN, "{\"impressionContrat\":\"2025-02-01T00:00:00\"}").andExpect(status().isOk());

        assertThat(relu(dossier).getContractPrintDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 0, 0));
    }

    // ------------------------------------------------------------------ B8 champs verrouillés

    @Test
    void champsVerrouilles_chefSurConcretisationPosee_articleSansPlafond() throws Exception {
        dossier.setAdvanceDate(DEBUT_JANVIER);
        enregistrer(dossier);
        StkItem article = dossier.getStkItem();
        article.setDiscountMax(BigDecimal.ZERO);
        stkItemRepository.save(article);

        mockMvc.perform(get(DROITS, dossier.getId()).with(user("chef").roles(CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(VERROUS, hasItem("concretisation")))
                .andExpect(jsonPath(VERROUS, hasItem("impressionContrat")))
                .andExpect(jsonPath(VERROUS, not(hasItem("remise"))));
    }

    @Test
    void champsVerrouilles_videsPourLAdministrateur() throws Exception {
        dossier.setAdvanceDate(DEBUT_JANVIER);
        enregistrer(dossier);

        mockMvc.perform(get(DROITS, dossier.getId()).with(user("admin").roles(ADMIN)))
                .andExpect(jsonPath(VERROUS).isEmpty());
    }

    @Test
    void champsVerrouilles_remisePourUnCommercialSurArticleSansPlafond() throws Exception {
        StkItem article = dossier.getStkItem();
        article.setDiscountMax(BigDecimal.ZERO);
        stkItemRepository.save(article);

        mockMvc.perform(get(DROITS, dossier.getId()).with(user("com").roles("Commercial")))
                .andExpect(jsonPath(VERROUS, hasItem("remise")));
    }

    // ------------------------------------------------------------------ B6 commercial typé

    private CfgTier commercial(String code) {
        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("COM");
        CfgTier tier = fixture.tier(code);
        tier.setCfgTierKind(tierKindRepository.save(kind));
        return tierRepository.save(tier);
    }

    @Test
    void commercialId_tiersNonCommercial_400_rienNEstEcrit() throws Exception {
        CfgTier client = fixture.tier("CLI-MOD");

        modifier(dossier, ADMIN, "{\"commercialId\":\"%s\"}".formatted(client.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("n'est pas un commercial")));

        assertThat(relu(dossier).getCfgCommercial()).isNull();
    }

    @Test
    void commercialId_commercial_200_etFicheRenseigneLesIds() throws Exception {
        CfgTier com = commercial("COM-MOD");
        UUID decision = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO ComLoanCompanyDecision (PKey, sorting, Code, Description) "
                        + "VALUES (:id, 1, 'ACC-MOD', 'ACCEPTEE MOD')")
                .setParameter("id", decision)
                .executeUpdate();

        modifier(dossier, ADMIN, "{\"commercialId\":\"%s\",\"meilleurTauxDecisionId\":\"%s\"}"
                .formatted(com.getId(), decision))
                .andExpect(status().isOk());

        relu(dossier);
        mockMvc.perform(get(FICHE, dossier.getId()).with(user("admin").roles(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autres.commercialId").value(com.getId().toString()))
                .andExpect(jsonPath("$.autres.meilleurTauxDecisionId").value(decision.toString()));
    }

    // ------------------------------------------------------------------ B7 dossier sorti

    @Test
    void dossierSorti_viderLAnnulation_400MessageOmran_rienNEstEcrit() throws Exception {
        ComFolder annule = fixture.dossier("DOS-MOD-ANN", fixture.tranche("MOD-ANN", true), StatutDossier.ANNULATION);
        annule.setCancelDate(DEBUT_JANVIER);
        annule.setCancelDescription("Financement refusé");
        enregistrer(annule);

        modifier(annule, ADMIN, "{\"champsAVider\":[\"annulationCauses\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Il faut saisir les causes de la résiliation.")));
        modifier(annule, CHEF, "{\"champsAVider\":[\"annulationDate\"]}").andExpect(status().isBadRequest());

        ComFolder f = relu(annule);
        assertAll(
                () -> assertThat(f.getCancelDate()).isEqualTo(DEBUT_JANVIER),
                () -> assertThat(f.getCancelDescription()).isEqualTo("Financement refusé"));
    }
}
