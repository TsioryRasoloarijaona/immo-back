package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code PATCH} d'une échéance : défauts de catégorie, quittance figée, report de l'écart (FEATURE-echeancier-modification.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EcheancierModificationTest {

    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";
    private static final BigDecimal PRIX = new BigDecimal("1000000");
    private static final String QUITTANCE = "26/00042";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categorieRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;

    private CfgTranche tranche;
    private ComFolder dossier;
    private ComSettlementCategory frais;
    private ComDocument ligneAvance;
    private ComDocument autofinancementRecent;
    private ComDocument ligneFrais;

    /** Prix 1 000 000 = avance 300 000 + autofinancement 400 000 + 300 000 ; une ligne de frais hors prix à côté. */
    @BeforeEach
    void dossierEquilibre() {
        tranche = fixture.tranche("TR-ECH-MOD", true);
        dossier = fixture.dossier("DOS-ECH-MOD", tranche, StatutDossier.RESERVATION);
        dossier.setPriceAfterDiscount(PRIX);
        dossier.setPrice(PRIX);
        folderRepository.save(dossier);
        ComSettlementCategory avance = categorie("AVANCE-MOD", c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        ComSettlementCategory autofinancement = categorie("AUTOFIN-MOD", c -> c.setIsSaleAutoFinancment(true));
        frais = categorie("FRAIS DOSSIER-MOD", c -> c.setIsSaleExpense(true));
        ligneAvance = fixture.echeance(dossier, avance, new BigDecimal("300000"), date(2026, 10, 1));
        fixture.echeance(dossier, autofinancement, new BigDecimal("400000"), date(2027, 1, 1));
        autofinancementRecent = fixture.echeance(dossier, autofinancement, new BigDecimal("300000"), date(2027, 3, 1));
        ligneFrais = fixture.echeance(dossier, frais, new BigDecimal("5000"), date(2026, 11, 15));
        ligneFrais.setExcludeFromPrice(false);
        ligneFrais.setQuittanceCode(QUITTANCE);
        documentRepository.save(ligneFrais);
    }

    // ---- B1 / B2 : changement de catégorie ------------------------------------------------------------

    @Test
    void changementDeCategorie_exclusionEtTvaDeLaNouvelle_montantEtDateConserves() throws Exception {
        ComSettlementCategory syndic = categorie("FRAIS SYNDIC-MOD", c -> {
            c.setIsSaleExpense(true);
            c.setExcludeFromPrice(true);
            c.setVatRatio(new BigDecimal("0.1"));
        });

        modifier(ligneFrais, "{\"categorieId\":\"%s\"}".formatted(syndic.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorieId").value(syndic.getId().toString()))
                .andExpect(jsonPath("$.exclureDuPrix").value(true))
                .andExpect(jsonPath("$.tauxTVA").value(0.1))
                .andExpect(jsonPath("$.montant").value(5000))
                .andExpect(jsonPath("$.dateEcheance").value("2026-11-15T00:00:00"));
    }

    @Test
    void changementDeCategorie_exclusionEnvoyeeParLAppelant_prime() throws Exception {
        ComSettlementCategory syndic = categorie("FRAIS SYNDIC-MOD2", c -> {
            c.setIsSaleExpense(true);
            c.setExcludeFromPrice(true);
        });

        modifier(ligneFrais, "{\"categorieId\":\"%s\",\"exclureDuPrix\":false}".formatted(syndic.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exclureDuPrix").value(false));
    }

    @Test
    void memeCategorie_exclusionSaisieConservee() throws Exception {
        ComSettlementCategory horsPrix = categorie("FRAIS-HP-MOD", c -> {
            c.setIsSaleExpense(true);
            c.setExcludeFromPrice(true);
        });
        ComDocument ligne = fixture.echeance(dossier, horsPrix, new BigDecimal("800"), date(2026, 12, 1));
        ligne.setExcludeFromPrice(false);
        documentRepository.save(ligne);

        modifier(ligne, "{\"montant\":900}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exclureDuPrix").value(false));
    }

    @Test
    void changementVersUneAutreCategorieDeFrais_quittanceJamaisRenumerotee() throws Exception {
        ComSettlementCategory autresFrais = categorie("AUTRES FRAIS-MOD", c -> c.setIsSaleExpense(true));

        modifier(ligneFrais, "{\"categorieId\":\"%s\",\"montant\":6000}".formatted(autresFrais.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decompte").value(QUITTANCE));
    }

    @Test
    void ligneSansQuittancePasseeEnFrais_resteSansQuittance() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, categorie("DIVERS-MOD", c -> c.setIsCredit(true)),
                new BigDecimal("1000"), date(2026, 12, 1));

        modifier(ligne, "{\"categorieId\":\"%s\"}".formatted(frais.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decompte").doesNotExist());
        assertThat(relire(ligne).getQuittanceCode()).isEmpty();
    }

    // ---- B3 : report de l'écart, réglé, date des finalisations -----------------------------------------

    @Test
    void avanceQuiMetAJourLeSchema_reportSurLeDernierAutofinancement_ecartNul() throws Exception {
        modifier(ligneAvance, "{\"montant\":320000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(320000))
                .andExpect(jsonPath("$.ecartSchema").value(0));

        assertThat(relire(autofinancementRecent).getAmount()).isEqualByComparingTo("280000");
    }

    @Test
    void autofinancement_pasDeReport_ecartRenvoye() throws Exception {
        modifier(autofinancementRecent, "{\"montant\":250000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecartSchema").value(50000));

        assertThat(relire(ligneAvance).getAmount()).isEqualByComparingTo("300000");
    }

    @Test
    void montantSousLeDejaRegle_accepte_resteNegatif() throws Exception {
        regler(ligneFrais, "5000");

        modifier(ligneFrais, "{\"montant\":3000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(3000))
                .andExpect(jsonPath("$.montantRegle").value(5000))
                .andExpect(jsonPath("$.reste").value(-2000));
    }

    @Test
    void dateAnterieureAuxFinalisations_ligneSoldee_acceptee() throws Exception {
        ComDocument ligne = ligneBaseeSurLesFinalisations();
        regler(ligne, "1000");

        modifier(ligne, "{\"dateEcheance\":\"2027-05-01T00:00:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateEcheance").value("2027-05-01T00:00:00"));
    }

    @Test
    void dateAnterieureAuxFinalisations_ligneNonSoldee_400() throws Exception {
        ComDocument ligne = ligneBaseeSurLesFinalisations();

        modifier(ligne, "{\"dateEcheance\":\"2027-05-01T00:00:00\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void echeanceInconnue_404() throws Exception {
        mockMvc.perform(patch(ECHEANCE, dossier.getId(), UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":1}"))
                .andExpect(status().isNotFound());
    }

    // ---- Outils --------------------------------------------------------------------------------------

    private ResultActions modifier(ComDocument ligne, String corps) throws Exception {
        return mockMvc.perform(patch(ECHEANCE, dossier.getId(), ligne.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ComDocument ligneBaseeSurLesFinalisations() {
        tranche.setStartDateFinalisation(date(2027, 6, 1));
        trancheRepository.save(tranche);
        ComSettlementCategory finale = categorie("FRAIS-FINAL-MOD", c -> {
            c.setIsSaleExpense(true);
            c.setIsConfermentBasedDate(true);
        });
        return fixture.echeance(dossier, finale, new BigDecimal("1000"), date(2027, 6, 1));
    }

    private void regler(ComDocument ligne, String montant) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(new BigDecimal(montant));
        settlementRepository.save(reglement);
        StlSettlementLine part = new StlSettlementLine();
        part.setId(UUID.randomUUID());
        part.setStlSettlement(reglement);
        part.setComDocument(ligne);
        part.setAmount(new BigDecimal(montant));
        settlementLineRepository.save(part);
    }

    private ComDocument relire(ComDocument ligne) {
        entityManager.flush();
        entityManager.refresh(ligne);
        return ligne;
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
