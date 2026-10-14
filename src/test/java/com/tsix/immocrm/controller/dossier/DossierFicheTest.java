package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/dossiers/{id}/fiche} — FEATURE-vente-fiche.md §A. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierFicheTest {

    private static final String FICHE = "/api/dossiers/{id}/fiche";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private CfgTrancheRepository trancheRepository;

    @Test
    void fiche_chaqueOngletRenvoieSesChamps() throws Exception {
        CfgTranche tranche = fixture.tranche("FICHE-A", true);
        tranche.setOccupancyPermitsDate(LocalDateTime.of(2026, 3, 1, 0, 0));
        tranche.setStartDateFinalisation(LocalDateTime.of(2026, 6, 30, 0, 0));
        trancheRepository.save(tranche);
        ComFolder f = fixture.dossier("DOS-FICHE", tranche, StatutDossier.CONCRETISATION);
        f.setAgreementPrice(new BigDecimal("1150000"));
        f.setTotalToPay(new BigDecimal("1200000"));
        f.setItemPriceHT(new BigDecimal("1000000"));
        f.setUnderItemsPriceHT(new BigDecimal("50000"));
        f.setPriceAfterDiscountHT(new BigDecimal("1000000"));
        f.setPriceAfterDiscount(new BigDecimal("1200000"));
        f.setDiscount(new BigDecimal("0.05"));
        f.setStatusValidityDate(LocalDateTime.now().minusDays(2));
        f.setAdvanceDate(LocalDateTime.of(2026, 1, 10, 0, 0));
        f.setCreditApprovalDate(LocalDateTime.of(2026, 2, 1, 0, 0));
        f.setAgreementDate(LocalDateTime.of(2026, 2, 15, 0, 0));
        f.setFinalTitleDateDeposit(LocalDateTime.of(2026, 4, 1, 0, 0));
        f.setFinalTitleNumber("TF-123");
        f.setCancelDescription("Cause test");
        f.setIsPenaltyDisabled(true);
        f.setCreditAgency("Agence Maârif");
        folderRepository.save(f);

        mockMvc.perform(get(FICHE, f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.getId().toString()))
                .andExpect(jsonPath("$.enTete.prixContractuel").value(1150000))
                .andExpect(jsonPath("$.enTete.prixArticleHT").value(1000000))
                .andExpect(jsonPath("$.enTete.prixSousArticlesHT").value(50000))
                .andExpect(jsonPath("$.enTete.remisePourcentage").value(5))
                .andExpect(jsonPath("$.statut.code").value("CONCRETISATION"))
                .andExpect(jsonPath("$.statut.expire").value(true))
                .andExpect(jsonPath("$.autresMontants.superficie").value(100))
                .andExpect(jsonPath("$.autresMontants.prixVenteM2HT").value(10000))
                .andExpect(jsonPath("$.autresMontants.prixVenteM2TTC").value(12000))
                .andExpect(jsonPath("$.dates.concretisation").value("2026-01-10T00:00:00"))
                .andExpect(jsonPath("$.dates.compromis").value("2026-02-01T00:00:00"))
                .andExpect(jsonPath("$.dates.envoiNotaire").value("2026-02-15T00:00:00"))
                .andExpect(jsonPath("$.dates.permisHabiter").value("2026-03-01T00:00:00"))
                .andExpect(jsonPath("$.dates.remiseClesPrevue").value("2026-06-30T00:00:00"))
                .andExpect(jsonPath("$.datesAutres.cpfDepot").value("2026-04-01T00:00:00"))
                .andExpect(jsonPath("$.datesAutres.numeroTitre").value("TF-123"))
                .andExpect(jsonPath("$.annulation.causes").value("Cause test"))
                .andExpect(jsonPath("$.autres.penalitesDesactivees").value(true))
                .andExpect(jsonPath("$.autres.agenceCredit").value("Agence Maârif"))
                .andExpect(jsonPath("$.autres.meilleurTauxDecision").doesNotExist())
                .andExpect(jsonPath("$.aDesImpayes").value(false))
                .andExpect(jsonPath("$.instructionNonLue").value(false));
    }

    @Test
    void fiche_echeanceEchueNonSoldee_leveLeBandeauImpayes() throws Exception {
        ComFolder f = fixture.dossier("DOS-IMPAYE", fixture.tranche("FICHE-B", true), StatutDossier.RESERVATION);
        ComSettlementCategory avance = fixture.categorieEcheance("AVANCE-FICHE", false, false);
        fixture.echeance(f, avance, new BigDecimal("50000"), LocalDateTime.now().minusDays(10));

        mockMvc.perform(get(FICHE, f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aDesImpayes").value(true));
    }

    @Test
    void fiche_echeanceAVenir_pasDeBandeau() throws Exception {
        ComFolder f = fixture.dossier("DOS-AVENIR", fixture.tranche("FICHE-C", true), StatutDossier.RESERVATION);
        ComSettlementCategory avance = fixture.categorieEcheance("AVANCE-FICHE2", false, false);
        fixture.echeance(f, avance, new BigDecimal("50000"), LocalDateTime.now().plusDays(10));

        mockMvc.perform(get(FICHE, f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aDesImpayes").value(false));
    }

    @Test
    void fiche_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(FICHE, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fiche_dossierSurEntiteInactive_404() throws Exception {
        ComFolder f = fixture.dossier("DOS-INACTIF", fixture.tranche("FICHE-D", false), StatutDossier.RESERVATION);

        mockMvc.perform(get(FICHE, f.getId()))
                .andExpect(status().isNotFound());
    }
}
