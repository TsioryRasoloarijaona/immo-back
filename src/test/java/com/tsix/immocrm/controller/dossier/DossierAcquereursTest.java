package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Acquéreurs : répartition à 100 %, refus, prospections à archiver (FEATURE-vente-echeancier.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierAcquereursTest {

    private static final String ACQUEREURS = "/api/dossiers/{id}/acquereurs";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComFolderCustomerRepository acquereurRepository;
    @Autowired private ComProspectionRepository prospectionRepository;

    private ComFolder dossier;
    private CfgTier conjoint;

    @BeforeEach
    void dossierSeul() {
        CfgTranche tranche = fixture.tranche("TR-ACQ", true);
        dossier = fixture.dossier("DOS-ACQ", tranche, StatutDossier.RESERVATION);
        dossier.setPartPercent(java.math.BigDecimal.ONE);
        dossier = folderRepository.save(dossier);
        conjoint = fixture.tier("CONJOINT");
    }

    @Test
    void cinquanteCinquante_partsEcritesEnRatio_total100() throws Exception {
        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(repartition("50", "50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.acquereurs.length()").value(2))
                .andExpect(jsonPath("$.acquereurs[0].principal").value(true));

        entityManager.flush();
        entityManager.clear();
        List<ComFolderCustomer> lignes = acquereurRepository.findByComFolderId(dossier.getId());
        assertAll(
                () -> assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getPartPercent())
                        .isEqualByComparingTo("0.5"),
                () -> assertThat(lignes).singleElement()
                        .satisfies(l -> assertThat(l.getPartPercent()).isEqualByComparingTo("0.5")));
    }

    @Test
    void sommeDifferenteDe100_400() throws Exception {
        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(repartition("50", "49.99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("100%")));
        assertThat(acquereurRepository.findByComFolderId(dossier.getId())).isEmpty();
    }

    @Test
    void partDuPrincipalNulle_400() throws Exception {
        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(repartition("0", "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("client principal")));
    }

    @Test
    void clientPrincipalCommeCoAcquereur_400() throws Exception {
        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotePartPrincipal":50,"coAcquereurs":[{"tiersId":"%s","quotePart":50}]}"""
                                .formatted(dossier.getCfgCustomer().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("déjà affecté")));
    }

    @Test
    void listeVide_retireLesCoAcquereurs() throws Exception {
        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(repartition("50", "50"))).andExpect(status().isOk());

        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotePartPrincipal\":100,\"coAcquereurs\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acquereurs.length()").value(1));

        entityManager.flush();
        assertThat(acquereurRepository.findByComFolderId(dossier.getId())).isEmpty();
    }

    @Test
    void nouvelAcquereur_prospectionsOuvertesProposees_puisArchivees() throws Exception {
        ComProspection ouverte = prospection(conjoint, false);
        prospection(conjoint, true);

        mockMvc.perform(put(ACQUEREURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(repartition("60", "40")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectionsAArchiver.length()").value(1))
                .andExpect(jsonPath("$.prospectionsAArchiver[0].id").value(ouverte.getId().toString()));

        mockMvc.perform(post(ACQUEREURS + "/archiver-prospections", dossier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionIds\":[\"%s\"]}".formatted(ouverte.getId())))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        ComProspection relue = prospectionRepository.findById(ouverte.getId()).orElseThrow();
        assertAll(
                () -> assertThat(relue.getIsClosed()).isTrue(),
                () -> assertThat(relue.getClosingReasonText()).isEqualTo("Création dossier de vente"));
    }

    @Test
    void archiverUneProspectionEtrangere_400() throws Exception {
        ComProspection etrangere = prospection(fixture.tier("ETRANGER"), false);

        mockMvc.perform(post(ACQUEREURS + "/archiver-prospections", dossier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionIds\":[\"%s\"]}".formatted(etrangere.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dossierInconnu_404() throws Exception {
        mockMvc.perform(get(ACQUEREURS, UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private String repartition(String principal, String co) {
        return """
                {"quotePartPrincipal":%s,"coAcquereurs":[{"tiersId":"%s","quotePart":%s}]}"""
                .formatted(principal, conjoint.getId(), co);
    }

    private ComProspection prospection(CfgTier tiers, boolean fermee) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-ACQ-" + UUID.randomUUID().toString().substring(0, 8));
        p.setCfgProspect(tiers);
        p.setIsClosed(fermee);
        return prospectionRepository.save(p);
    }
}
