package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-vente-echeancier.md §2 (acquéreurs) : des parts dont la somme saisie vaut exactement 100 % sont
 * acceptées, même quand chaque part arrondie au ratio de la colonne (6 décimales) ne retombe pas sur 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierAcquereursRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;

    @Test
    void troisTiers_sommeSaisieExactementCent_acceptee() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-ACQ-REV", fixture.tranche("TR-ACQ-REV", true), StatutDossier.RESERVATION);
        dossier.setPartPercent(BigDecimal.ONE);
        folderRepository.save(dossier);
        CfgTier premier = fixture.tier("CO-REV-1");
        CfgTier second = fixture.tier("CO-REV-2");

        // 33,33333 + 33,33333 + 33,33334 = 100 ; en ratio à 6 décimales : 0,333333 × 3 = 0,999999.
        mockMvc.perform(put("/api/dossiers/{id}/acquereurs", dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quotePartPrincipal":33.33333,"coAcquereurs":[
                                  {"tiersId":"%s","quotePart":33.33333},{"tiersId":"%s","quotePart":33.33334}]}"""
                                .formatted(premier.getId(), second.getId())))
                .andExpect(status().isOk());
    }
}
