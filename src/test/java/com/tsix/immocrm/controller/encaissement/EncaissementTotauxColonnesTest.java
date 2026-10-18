package com.tsix.immocrm.controller.encaissement;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/encaissements/totaux-colonnes} — voir {@code instructions/FEATURE-totaux-listes.md}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EncaissementTotauxColonnesTest {

    private static final String URL_TOTAUX = "/api/encaissements/totaux-colonnes";
    private static final String URL_LISTE = "/api/encaissements";
    private static final String AGREGATS = "agregats";
    private static final String REFERENCE = "reference";
    private static final String NOMBRE_TOTAL = "$.nombreTotal";
    private static final String TOTAL_ELEMENTS = "$.totalElements";
    private static final String EN_COURS = "IN_WORK";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private EntityManager entityManager;

    private void relireDepuisLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private ComDocument newEcheance(String suffix) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TCE-" + suffix);
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-TCE-" + suffix);
        folder.setStkVocation(vocation);
        folderRepository.save(folder);

        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal("1000"));
        return documentRepository.save(doc);
    }

    private StlSettlement newEncaissement(String reference, BigDecimal montant, String etat, boolean credit) {
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode(reference);
        settlement.setDocDate(LocalDateTime.of(2026, 9, 16, 8, 30, 0));
        settlement.setAmount(montant);
        settlement.setStatus(etat);
        settlement.setIsCredit(credit);
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(newEcheance(reference));
        line.setAmount(montant);
        settlementLineRepository.save(line);
        return settlement;
    }

    private StlSettlement newEncaissement(String reference, BigDecimal montant) {
        return newEncaissement(reference, montant, EN_COURS, true);
    }

    private long totalElementsDeLaListe(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, TOTAL_ELEMENTS)).longValue();
    }

    private long nombreTotalDesTotaux(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, NOMBRE_TOTAL)).longValue();
    }

    // ------------------------------------------------------------------ R1 — mêmes lignes que la liste

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_avecFiltreEtat() throws Exception {
        newEncaissement("TCEA-1", new BigDecimal("100.00"));
        newEncaissement("TCEA-2", new BigDecimal("200.00"));
        newEncaissement("TCEA-3", new BigDecimal("400.00"), "POINTED", true);
        relireDepuisLaBase();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(REFERENCE, "TCEA-").param("etat", EN_COURS));
        assertThat(liste).isEqualTo(2L);

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEA-").param("etat", EN_COURS)
                        .param(AGREGATS, "montantBrut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(liste))
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux.montantBrut").value(300.0));
    }

    /** R1 : un décaissement ({@code IsCredit = 0}) est hors de la liste, donc hors des totaux. */
    @Test
    void r1_decaissement_horsDesTotauxCommeDeLaListe() throws Exception {
        newEncaissement("TCEB-1", new BigDecimal("500.00"));
        newEncaissement("TCEB-2", new BigDecimal("-470.00"), EN_COURS, false);
        relireDepuisLaBase();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(REFERENCE, "TCEB-"));
        assertThat(liste).isEqualTo(1L);

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEB-").param(AGREGATS, "montantBrut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(liste))
                .andExpect(jsonPath(NOMBRE_TOTAL).value(1))
                .andExpect(jsonPath("$.totaux.montantBrut").value(500.0));
    }

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_sansFiltre() throws Exception {
        newEncaissement("TCEC-1", new BigDecimal("100.00"));
        relireDepuisLaBase();

        long liste = totalElementsDeLaListe(get(URL_LISTE));
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX));

        assertThat(totaux).isEqualTo(liste).isPositive();
    }

    @Test
    void sansAgregats_seulLeNombreTotal() throws Exception {
        newEncaissement("TCED-1", BigDecimal.TEN);
        newEncaissement("TCED-2", BigDecimal.ONE);
        relireDepuisLaBase();

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCED-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux").isEmpty());
    }

    // ------------------------------------------------------------------ R3 — toutes les lignes, pas une page

    @Test
    void r3_agregats_portentSurToutesLesLignes_pasSurLaPage() throws Exception {
        for (int i = 1; i <= 12; i++) {
            newEncaissement("TCEE-%02d".formatted(i), BigDecimal.valueOf(i));
        }
        relireDepuisLaBase();

        mockMvc.perform(get(URL_LISTE).param(REFERENCE, "TCEE-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(12));

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEE-")
                        .param(AGREGATS, "montantBrut", "montantBrut:AVG", "montantBrut:MIN", "montantBrut:MAX",
                                "reference:COUNT", "dateDocument:MIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(12))
                .andExpect(jsonPath("$.totaux.montantBrut").value(78.0))
                .andExpect(jsonPath("$.totaux['montantBrut:AVG']").value(6.5))
                .andExpect(jsonPath("$.totaux['montantBrut:MIN']").value(1.0))
                .andExpect(jsonPath("$.totaux['montantBrut:MAX']").value(12.0))
                .andExpect(jsonPath("$.totaux['reference:COUNT']").value(12))
                .andExpect(jsonPath("$.totaux['dateDocument:MIN']").value(Matchers.startsWith("2026-09-16")));
    }

    @Test
    void extremumEtMoyenne_surColonneVide_rendentNull() throws Exception {
        newEncaissement("TCEF-1", new BigDecimal("100.00"));
        newEncaissement("TCEF-2", new BigDecimal("200.00"));
        relireDepuisLaBase();

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEF-")
                        .param(AGREGATS, "montantBase:MIN", "montantBase:MAX", "montantBase:AVG",
                                "dateReglement:MIN", "dateReglement:MAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux['montantBase:MIN']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['montantBase:MAX']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['montantBase:AVG']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['dateReglement:MIN']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['dateReglement:MAX']").value(nullValue()));
    }

    // ------------------------------------------------------------------ R7 — annulés comptés, hors des sommes

    @Test
    void r7_reglementAnnule_compteDansNombreTotal_pasDansLeMontant() throws Exception {
        newEncaissement("TCEG-1", new BigDecimal("300.00"));
        newEncaissement("TCEG-2", new BigDecimal("700.00"), "CANCELED", true);
        relireDepuisLaBase();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(REFERENCE, "TCEG-"));
        assertThat(liste).isEqualTo(2L);

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEG-")
                        .param(AGREGATS, "montantBrut", "montantNet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(liste))
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux.montantBrut").value(300.0))
                .andExpect(jsonPath("$.totaux.montantNet").value(300.0));
    }

    // ------------------------------------------------------------------ R4 / R5 — jetons refusés

    @Test
    void r4_sommeSurUneDate_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "dateDocument:SUM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r4_nombreSurUnTexte_rend200() throws Exception {
        newEncaissement("TCEH-1", new BigDecimal("100.00"));
        relireDepuisLaBase();

        mockMvc.perform(get(URL_TOTAUX).param(REFERENCE, "TCEH-").param(AGREGATS, "etat:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux['etat:COUNT']").value(1));
    }

    @Test
    void jetonInconnu_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "montantBrut:MEDIANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fonctionsValides", Matchers.hasItems("SUM", "AVG", "MIN", "MAX", "COUNT")));
    }

    @Test
    void r5_colonneSansChemin_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "dossierCode:COUNT"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "montantBrutRestant:MIN"))
                .andExpect(status().isBadRequest());
    }
}
