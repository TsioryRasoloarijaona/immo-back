package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmSexTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GAP-05 — Omran "Général" tab identity fields on CreerProspectRequest/ProspectDetailDto. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectIdentityFieldsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdmTierTitleRepository titleRepository;

    @Autowired
    private AdmSexTypeRepository sexTypeRepository;

    @Autowired
    private AdmCountryRepository countryRepository;

    @Autowired
    private AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;

    @Test
    void create_happyPath_persistsIdentityFieldsAndPieceIdentite() throws Exception {
        AdmSexType sexe = new AdmSexType();
        sexe.setId(UUID.randomUUID());
        sexe.setCode("M-TEST");
        sexTypeRepository.save(sexe);

        AdmTierTitle titre = new AdmTierTitle();
        titre.setId(UUID.randomUUID());
        titre.setCode("M-TITRE-TEST");
        titre.setAdmSexType(sexe);
        titleRepository.save(titre);

        AdmCountry pays = new AdmCountry();
        pays.setId(UUID.randomUUID());
        pays.setCode("MG-ID-TEST");
        countryRepository.save(pays);

        AdmIdentityDocumentType docType = new AdmIdentityDocumentType();
        docType.setId(UUID.randomUUID());
        docType.setCode("CIN-TEST");
        identityDocumentTypeRepository.save(docType);

        String body = """
                {
                  "prenom": "Hery",
                  "nom": "Rakoto",
                  "titreId": "%s",
                  "sexeId": "%s",
                  "paysId": "%s",
                  "employeur": "AyadERP",
                  "anciennete": 5,
                  "filsDe": "Jean",
                  "etDe": "Marie",
                  "adresse2": "Lot II B",
                  "pieceIdentite": {
                    "typeId": "%s",
                    "numero": "101112",
                    "lieuDelivrance": "Antananarivo"
                  }
                }
                """.formatted(titre.getId(), sexe.getId(), pays.getId(), docType.getId());

        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titreId").value(titre.getId().toString()))
                .andExpect(jsonPath("$.sexeId").value(sexe.getId().toString()))
                .andExpect(jsonPath("$.paysId").value(pays.getId().toString()))
                .andExpect(jsonPath("$.employeur").value("AyadERP"))
                .andExpect(jsonPath("$.anciennete").value(5))
                .andExpect(jsonPath("$.filsDe").value("Jean"))
                .andExpect(jsonPath("$.etDe").value("Marie"))
                .andExpect(jsonPath("$.adresse2").value("Lot II B"))
                .andExpect(jsonPath("$.pieceIdentite.typeId").value(docType.getId().toString()))
                .andExpect(jsonPath("$.pieceIdentite.numero").value("101112"))
                .andExpect(jsonPath("$.pieceIdentite.lieuDelivrance").value("Antananarivo"));
    }
}
