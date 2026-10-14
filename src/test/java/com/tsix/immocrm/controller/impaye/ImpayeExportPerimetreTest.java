package com.tsix.immocrm.controller.impaye;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R6 sur {@code GET /api/impayes/export} : le classeur ne contient que les dossiers visibles de l'appelant (revue). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ImpayeExportPerimetreTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private Clock clock;

    @Test
    void export_commercialRestreint_neContientPasLesDossiersHorsPerimetre() throws Exception {
        String jeton = "IMX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime hier = LocalDate.now(clock).minusDays(1).atStartOfDay();
        ComSettlementCategory avance = fixture.categorieEcheance("AVANCE-" + jeton, true, false, false, null);
        CfgTranche trancheA = fixture.tranche(jeton + "A", true);
        CfgTranche trancheB = fixture.tranche(jeton + "B", true);
        ComFolder dossierA = fixture.dossier(jeton + "-A", trancheA, StatutDossier.FORMALISE);
        dossierA.getStrStatus().setVenteEffective(true);
        ComFolder dossierB = fixture.dossier(jeton + "-B", trancheB, StatutDossier.FORMALISE);
        dossierB.getStrStatus().setVenteEffective(true);
        fixture.echeance(dossierA, avance, new BigDecimal("100"), hier);
        fixture.echeance(dossierB, avance, new BigDecimal("100"), hier);

        AdmUser commercial = fixture.compte("COM-" + jeton, "Commercial");
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(commercial);
        habilitation.setCfgProject(trancheA.getCfgProject());
        habilitationRepository.save(habilitation);

        MvcResult asynchrone = mockMvc.perform(get("/api/impayes/export").param("format", "xlsx")
                        .param("q", jeton).param("colonnes", "dossierCode")
                        .with(connecte(commercial, "Commercial")))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<String> codes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                codes.add(row.getCell(0).getStringCellValue());
            }
        }
        assertThat(codes).hasSize(2).contains(dossierA.getCode()).doesNotContain(dossierB.getCode());
    }
}
