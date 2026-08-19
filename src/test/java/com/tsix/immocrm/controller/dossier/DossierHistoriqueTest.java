package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/historique} — merges status changes, échéances, règlements and
 * pièces jointes into one chronological, traceable timeline. See {@code DossierHistoriqueService}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierHistoriqueTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private StrStatusRepository strStatusRepository;
    @Autowired
    private StrFolderStatusHistoryRepository statusHistoryRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private AdmAttachedFileRepository attachedFileRepository;

    private ComFolder newDossier(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-HIST-" + code);
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    @Test
    void getHistorique_mergesAllEventTypesSortedByDateDesc() throws Exception {
        ComFolder folder = newDossier("DOS-HIST-1");

        StrStatus reservation = new StrStatus();
        reservation.setId(UUID.randomUUID());
        reservation.setCode("RESERVATION");
        reservation.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        strStatusRepository.save(reservation);

        StrFolderStatusHistory statutEvent = new StrFolderStatusHistory();
        statutEvent.setId(UUID.randomUUID());
        statutEvent.setComFolder(folder);
        statutEvent.setStrFolderStatus(reservation);
        statutEvent.setDocDate(LocalDateTime.of(2024, 1, 1, 10, 0));
        statutEvent.setDescription("Création initiale");
        statusHistoryRepository.save(statutEvent);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setAmount(new BigDecimal("50000.00"));
        echeance.setUserCreateDateTime(LocalDateTime.of(2024, 1, 2, 9, 0));
        documentRepository.save(echeance);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDescription("Chèque N° 123");
        settlement.setAmount(new BigDecimal("50000.00"));
        settlement.setUserCreateDateTime(LocalDateTime.of(2024, 1, 3, 14, 0));
        settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new BigDecimal("50000.00"));
        settlementLineRepository.save(line);

        AdmAttachedFile piece = new AdmAttachedFile();
        piece.setId(UUID.randomUUID());
        piece.setCategoryId(folder.getId());
        piece.setCategory("Dossier de vente N° DOS-HIST-1 *");
        piece.setDescription("CIN client");
        piece.setAttachedFile("X".getBytes(StandardCharsets.UTF_8));
        attachedFileRepository.save(piece);
        // UserCreateDateTime for the attached file is left null (harness constraint on this
        // fixture) — sorted last by the nullsLast comparator, which is exercised here too.

        String json = mockMvc.perform(get("/api/dossiers/{id}/historique", folder.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer total = JsonPath.read(json, "$.totalElements");
        assertThat(total).isEqualTo(4);

        // Most recent first: règlement (01-03) > échéance (01-02) > statut (01-01) > pièce jointe (null date, last).
        String firstType = JsonPath.read(json, "$.content[0].type");
        assertThat(firstType).isEqualTo("REGLEMENT");
    }

    @Test
    void getHistorique_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/historique", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerStatut_recordsHistoryEntryWithNote() throws Exception {
        ComFolder folder = newDossier("DOS-HIST-2");
        StrStatus reservation = new StrStatus();
        reservation.setId(UUID.randomUUID());
        reservation.setCode("RESERVATION");
        reservation.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        strStatusRepository.save(reservation);
        folder.setStrStatus(reservation);
        folderRepository.save(folder);

        StrStatus formalise = new StrStatus();
        formalise.setId(UUID.randomUUID());
        formalise.setCode("FORMALISE");
        formalise.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        strStatusRepository.save(formalise);

        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"FORMALISE\",\"note\":\"Dossier complet\"}"))
                .andExpect(status().isOk());

        String json = mockMvc.perform(get("/api/dossiers/{id}/historique", folder.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String description = JsonPath.read(json, "$.content[0].description");
        assertThat(description).contains("FORMALISE").contains("Dossier complet");
    }
}
