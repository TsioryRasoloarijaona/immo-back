package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue de FEATURE-dossier-finitions.md : droits de suppression (§3) et projection de l'écart (§1). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierFinitionsRevueTest {

    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";
    private static final String PERIODIQUE = "/api/dossiers/{id}/echeancier/periodique";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categorieRepository;
    @Autowired private StrStatusRepository statusRepository;

    private ComFolder dossier;
    private ComSettlementCategory avance;
    private ComSettlementCategory versement;
    private ComDocument autofinancementRecent;

    /** Prix 1 000 000 = avance 300 000 + autofinancement 400 000 + 300 000 ; statut qui vérifie le schéma. */
    @BeforeEach
    void dossierEquilibre() {
        CfgTranche tranche = fixture.tranche("TR-REV-FIN", true);
        dossier = fixture.dossier("DOS-REV-FIN", tranche, StatutDossier.RESERVATION);
        dossier.getStrStatus().setVerificationSchema(true);
        statusRepository.save(dossier.getStrStatus());
        dossier.setPriceAfterDiscount(new BigDecimal("1000000"));
        folderRepository.save(dossier);
        avance = categorie("AVANCE-REV-FIN", c -> c.setIsSaleAdvance(true));
        versement = categorie("VERS-REV-FIN", c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        ComSettlementCategory autofinancement = categorie("AUTOFIN-REV-FIN", c -> c.setIsSaleAutoFinancment(true));
        fixture.echeance(dossier, avance, new BigDecimal("300000"), date(2026, 10, 1));
        fixture.echeance(dossier, autofinancement, new BigDecimal("400000"), date(2027, 1, 1));
        autofinancementRecent = fixture.echeance(dossier, autofinancement, new BigDecimal("300000"), date(2027, 3, 1));
    }

    @Test
    void suppression_parDirectionGenerale_200() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, versement, BigDecimal.ZERO, date(2026, 11, 1));

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId())
                        .with(user("dg").roles("Direction Générale")))
                .andExpect(status().isOk());
    }

    @Test
    void suppression_parResponsableMarketing_200() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, versement, BigDecimal.ZERO, date(2026, 11, 1));

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId())
                        .with(user("rmc").roles("Responsable marketing et communication")))
                .andExpect(status().isOk());
    }

    @Test
    void suppression_parCommercialAide_403_ligneConservee() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, versement, BigDecimal.ZERO, date(2026, 11, 1));

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId())
                        .with(user("aide").roles("Commercial Aide")))
                .andExpect(status().isForbidden());

        entityManager.flush();
        assertThat(documentRepository.existsById(ligne.getId())).isTrue();
    }

    /** Écart −50 000 → +40 000 : plus petit en valeur absolue, accepté par le garde-fou. */
    @Test
    void dejaDesequilibre_ecartQuiChangeDeSigneMaisReduit_passe() throws Exception {
        autofinancementRecent.setAmount(new BigDecimal("350000"));
        documentRepository.save(autofinancementRecent);
        entityManager.flush();

        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":260000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecartSchema").value(40000));
    }

    /**
     * Ligne de 0,40 non réglée, préexistante : la périodicité la retire avec l'autofinancement soldé ; l'écart réel
     * passerait de 0 à 0,40, la projection doit donc refuser, sans écrire.
     */
    @Test
    void periodicite_retraitDUneLigneNullePreexistante_creuseLEcart_409() throws Exception {
        dossier.setPriceAfterDiscount(new BigDecimal("1000000.40"));
        folderRepository.save(dossier);
        fixture.echeance(dossier, avance, new BigDecimal("0.40"), date(2026, 10, 2));
        entityManager.flush();

        mockMvc.perform(post(PERIODIQUE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId":"%s","montantTotal":300000,"nombre":3,"periodicite":"MENSUELLE",
                                 "premiereDate":"2026-12-01T00:00:00"}""".formatted(versement.getId())))
                .andExpect(status().isConflict());

        entityManager.flush();
        assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId())).hasSize(4);
    }

    private ComSettlementCategory categorie(String code, Consumer<ComSettlementCategory> drapeaux) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setIsCredit(true);
        c.setVatRatio(new BigDecimal("0.2"));
        drapeaux.accept(c);
        return categorieRepository.save(c);
    }

    private static LocalDateTime date(int annee, int mois, int jour) {
        return LocalDateTime.of(annee, mois, jour, 0, 0);
    }
}
