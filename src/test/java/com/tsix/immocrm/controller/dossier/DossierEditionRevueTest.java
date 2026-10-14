package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot B (FEATURE-vente-edition.md) : invariants d'argent sur données réelles, droits, client. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierEditionRevueTest {

    private static final String MODIFIER = "/api/dossiers/{id}";
    private static final String ADMIN = "Administrateur";
    private static final String CHEF = "Commercial Chef";
    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private StrStatusRepository statusRepository;
    @Autowired private EntityManager em;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        dossier = fixture.dossier("DOS-REV", fixture.tranche("REV-TR", true), StatutDossier.CONCRETISATION);
    }

    private ResultActions modifier(ComFolder d, String role, String corps) throws Exception {
        return mockMvc.perform(patch(MODIFIER, d.getId()).with(user("revue").roles(role))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ComFolder relu(ComFolder d) {
        em.flush();
        em.clear();
        return folderRepository.findById(d.getId()).orElseThrow();
    }

    /**
     * Forme de 18 dossiers de yesRemsProd (APF 2026/00381, ARS 2026/00401…) : prix article HT renseigné, mais
     * superficie et prix du m² HT nuls. OMRAN recalcule depuis ItemPriceHT stocké ; un recalcul depuis surface × m²
     * remet le prix à zéro.
     */
    private void dossierSansSurfaceNiPrixM2() {
        BigDecimal ht = new BigDecimal("1000000");
        dossier.setArea(null);
        dossier.setUnitCostHT(null);
        dossier.setItemPriceHT(ht);
        dossier.setItemPrice(new BigDecimal("1200000"));
        dossier.setPriceBeforeDiscountHT(ht);
        dossier.setPriceBeforeDiscount(new BigDecimal("1200000"));
        dossier.setPriceAfterDiscountHT(ht);
        dossier.setPriceAfterDiscount(new BigDecimal("1200000"));
        dossier.setPriceHT(ht);
        dossier.setPrice(new BigDecimal("1200000"));
        dossier.setDiscountAmount(BigDecimal.ZERO);
        folderRepository.save(dossier);
        em.flush();
    }

    @Test
    void tvaSeule_dossierSansSurfaceNiPrixM2_prixHTConserveEtTTCRecalcule() throws Exception {
        dossierSansSurfaceNiPrixM2();

        modifier(dossier, ADMIN, "{\"tva\":0.1}").andExpect(status().isOk());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1100000"));
    }

    @Test
    void integrerModificationsAuPrix_dossierSansSurfaceNiPrixM2_prixConserve() throws Exception {
        dossierSansSurfaceNiPrixM2();

        modifier(dossier, ADMIN, "{\"integrerModificationsAuPrix\":true}").andExpect(status().isOk());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1200000"));
    }

    @Test
    void champAViderDUnGroupeInterdit_parmiDesChampsPermis_403() throws Exception {
        CfgTier commercial = fixture.tier("COM-REV");
        dossier.setCfgCommercial(commercial);
        folderRepository.save(dossier);

        modifier(dossier, CHEF, "{\"minute\":\"2025-03-01T00:00:00\",\"champsAVider\":[\"commercialId\"]}")
                .andExpect(status().isForbidden());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getAgreementSavingDate()).isNull(),
                () -> assertThat(f.getCfgCommercial()).isNotNull());
    }

    @Test
    void memeRemiseRenvoyee_pasDeDateDeChangement() throws Exception {
        dossier.setDiscountAmount(new BigDecimal("50000"));
        folderRepository.save(dossier);

        modifier(dossier, ADMIN, "{\"remiseMontantHT\":50000}").andExpect(status().isOk());

        assertThat(relu(dossier).getDiscountChangeDate()).isNull();
    }

    @Test
    void dossierSorti_noteDUnCommercial_409() throws Exception {
        ComFolder annule = fixture.dossier("DOS-REV-ANN", fixture.tranche("REV-ANN", true), StatutDossier.ANNULATION);

        modifier(annule, COMMERCIAL, "{\"note\":\"Rappeler\"}").andExpect(status().isConflict());

        assertThat(relu(annule).getNotes()).isNull();
    }

    @Test
    void dossierSorti_annulationEtDateMelees_409_rienNEstEcrit() throws Exception {
        ComFolder annule = fixture.dossier("DOS-REV-AN2", fixture.tranche("REV-AN2", true), StatutDossier.DESISTEMENT);

        modifier(annule, ADMIN, "{\"annulationCauses\":\"x\",\"minute\":\"2025-03-01T00:00:00\"}")
                .andExpect(status().isConflict());

        ComFolder f = relu(annule);
        assertAll(
                () -> assertThat(f.getCancelDescription()).isNull(),
                () -> assertThat(f.getAgreementSavingDate()).isNull());
    }

    private CfgTierType type(String code) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode(code);
            t.setDescription(code);
            return tierTypeRepository.save(t);
        });
    }

    @Test
    void changementDeClient_ancienAvecUnAutreDossierActif_resteClient() throws Exception {
        dossier.getStrStatus().setTransformeProspectEnClient(true);
        statusRepository.save(dossier.getStrStatus());
        CfgTier ancien = dossier.getCfgCustomer();
        ancien.setCfgTierType(type("CLI"));
        tierRepository.save(ancien);
        ComFolder autre = fixture.dossier("DOS-REV-AUT", fixture.tranche("REV-AUT", true), StatutDossier.CONCRETISATION);
        autre.getStrStatus().setTransformeProspectEnClient(true);
        statusRepository.save(autre.getStrStatus());
        autre.setCfgCustomer(ancien);
        autre.setDocDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        folderRepository.save(autre);
        CfgTier nouveau = fixture.tier("NOUV-REV");
        nouveau.setCfgTierType(type("PRO"));
        tierRepository.save(nouveau);

        modifier(dossier, ADMIN, "{\"clientId\":\"%s\"}".formatted(nouveau.getId())).andExpect(status().isOk());

        em.flush();
        em.clear();
        assertAll(
                () -> assertThat(tierRepository.findById(ancien.getId()).orElseThrow().getCfgTierType().getCode())
                        .isEqualTo("CLI"),
                () -> assertThat(tierRepository.findById(nouveau.getId()).orElseThrow().getCfgTierType().getCode())
                        .isEqualTo("CLI"));
    }
}
