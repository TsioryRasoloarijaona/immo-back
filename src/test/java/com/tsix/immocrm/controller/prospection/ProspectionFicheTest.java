package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/prospections/{id}/documents/fiche} — modeled on the legacy "FICHE" prospection
 * template. See {@code ProspectionFicheService}, which reuses {@code ProspectionService#findById}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFicheTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComProspectionOriginRepository originRepository;

    @Test
    void getFiche_pdf_returnsProspectionSummary() throws Exception {
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORIG-FICHE-1");
        origine.setDescription("Site web");
        originRepository.save(origine);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("PROS-FICHE-1");
        prospect.setFirstName("Samira");
        prospect.setLastName("TAHA");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-FICHE-1");
        prospection.setCfgProspect(prospect);
        prospection.setComProspectionOrigin(origine);
        prospection.setPrice(new BigDecimal("500000"));
        prospection.setNotes("Recherche T3 vue mer");
        prospectionRepository.save(prospection);

        byte[] pdf = mockMvc.perform(get("/api/prospections/{id}/documents/fiche", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        assertThat(text).contains("Samira TAHA").contains("Site web").contains("Recherche T3 vue mer");
    }

    @Test
    void getFiche_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/documents/fiche", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
