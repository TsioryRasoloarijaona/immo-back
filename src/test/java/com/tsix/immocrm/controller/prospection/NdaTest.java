package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmNationalityRepository;
import com.tsix.immocrm.repositories.adm.AdmSexTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/prospections/{id}/documents/nda} — modeled on the legacy "NDA AYAD PRIVATE"
 * template. See {@code NdaService} for the field sourcing (ComProspection.cfgProspect).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class NdaTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private AdmNationalityRepository nationalityRepository;
    @Autowired
    private AdmSexTypeRepository sexTypeRepository;

    @Test
    void getNda_pdf_returnsProspectInfo() throws Exception {
        AdmNationality nationalite = new AdmNationality();
        nationalite.setId(UUID.randomUUID());
        nationalite.setCode("MA");
        nationalite.setDescription("Marocaine");
        nationalityRepository.save(nationalite);

        AdmSexType sexe = new AdmSexType();
        sexe.setId(UUID.randomUUID());
        sexe.setCode("F");
        sexTypeRepository.save(sexe);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("PROS-NDA-1");
        prospect.setFirstName("Samira");
        prospect.setLastName("TAHA");
        prospect.setAdmNationality(nationalite);
        prospect.setAdmSexType(sexe);
        prospect.setCity("CASABLANCA");
        prospect.setEMail("taha.samira@gmail.com");
        prospect.setMobile("0661258189");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-NDA-1");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        byte[] pdf = mockMvc.perform(get("/api/prospections/{id}/documents/nda", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        assertThat(text)
                .contains("Samira")
                .contains("TAHA")
                .contains("Marocaine")
                .contains("Female")
                .contains("taha.samira@gmail.com");
    }

    @Test
    void getNda_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/documents/nda", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
