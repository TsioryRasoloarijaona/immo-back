package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.services.dossier.ParametresVente;
import com.tsix.immocrm.services.dossier.PenaliteRetardService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pénalité de retard au règlement ({@code FEATURE-impayes.md} P2-P3), paramètres OMRAN simulés : aucune base
 * de test ne porte {@code ComPenaltyRatio} ni {@code DefaultComPenaltyDocumentType}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PenaliteRetardReglementTest {

    private static final BigDecimal TAUX = new BigDecimal("0.05");

    @MockitoBean private ParametresVente parametres;
    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private PenaliteRetardService penalites;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private EntityManager em;
    @Autowired private Clock clock;

    private ComFolder dossier;
    private ComSettlementCategory avance;
    private ComSettlementCategory categoriePenalite;
    private StlSettlementType virement;
    private LocalDateTime hier;

    @BeforeEach
    void setUp() {
        String code = "PEN" + UUID.randomUUID().toString().substring(0, 8);
        CfgTranche tranche = fixture.tranche(code, true);
        dossier = fixture.dossier(code, tranche, StatutDossier.FORMALISE);
        avance = fixture.categorieEcheance("AVANCE-" + code, true, false, false, null);
        avance.setDescription("Avance");
        avance.setIsWithPenalty(true);
        categoriePenalite = fixture.categorieEcheance("PENALITE-" + code, false, false, false, null);
        categoriePenalite.setDescription("Pénalité de retard");
        virement = new StlSettlementType();
        virement.setId(UUID.randomUUID());
        virement.setCode("VIR-" + code);
        settlementTypeRepository.save(virement);
        hier = LocalDate.now(clock).minusDays(1).atStartOfDay();

        when(parametres.tauxPenalite()).thenReturn(TAUX);
        when(parametres.categoriePenalite()).thenReturn(Optional.of(categoriePenalite.getId()));
        when(parametres.ageRecouvrement()).thenReturn(90);
    }

    /** 12 000 TTC, 10 000 HT, TVA 20 %. */
    private ComDocument echeance(LocalDateTime date) {
        ComDocument echeance = fixture.echeance(dossier, avance, new BigDecimal("12000"), date);
        echeance.setAmountHT(new BigDecimal("10000"));
        echeance.setVATRatio(new BigDecimal("0.2"));
        return echeance;
    }

    private void reglerDirectement(ComDocument echeance, String montant) {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(new BigDecimal(montant));
        reglement.setIsCredit(true);
        reglement.setStatus("POINTED");
        reglement.setStlSettlementType(virement);
        settlementRepository.save(reglement);
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal(montant));
        settlementLineRepository.save(ligne);
    }

    private ComDocument relue(ComDocument echeance) {
        em.flush();
        em.clear();
        return documentRepository.findById(echeance.getId()).orElseThrow();
    }

    @Test
    void ajoutDUnReglement_creeLaPenaliteEtLaRelieALEcheance() throws Exception {
        ComDocument echeance = echeance(hier);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", dossier.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":1000,\"dateEcheance\":\"2026-09-01T10:00:00\",\"typeId\":\""
                                + virement.getId() + "\"}"))
                .andExpect(status().isCreated());

        ComDocument penalite = relue(echeance).getComPenaltyDocument();
        assertThat(penalite).isNotNull();
        assertThat(penalite.getAmountHT()).isEqualByComparingTo("500.00");
        assertThat(penalite.getAmount()).isEqualByComparingTo("600.00");
        assertThat(penalite.getAmountTTC()).isEqualByComparingTo("600.00");
        assertThat(penalite.getVATRatio()).isEqualByComparingTo("0.2");
        assertThat(penalite.getDocDate()).isEqualTo(LocalDate.now(clock).atStartOfDay());
        assertThat(penalite.getComFolder().getId()).isEqualTo(dossier.getId());
        assertThat(penalite.getComSettlementCategory().getId()).isEqualTo(categoriePenalite.getId());
        assertThat(penalite.getNotes())
                .isEqualTo("Pénalité sur : Avance du " + hier.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Test
    void reglementPartielAnterieur_kEgalResteSurMontant() {
        ComDocument echeance = echeance(hier);
        reglerDirectement(echeance, "3000");

        Optional<ComDocument> penalite = penalites.genererPenalite(echeance);

        assertThat(penalite).isPresent();
        assertThat(penalite.get().getAmountHT()).isEqualByComparingTo("375.00");
    }

    @Test
    void penaliteExistante_reutiliseeSansNouveauDocument() {
        ComDocument echeance = echeance(hier);
        ComDocument premiere = penalites.genererPenalite(echeance).orElseThrow();
        long avant = documentRepository.count();

        Optional<ComDocument> seconde = penalites.genererPenalite(echeance);

        assertThat(seconde).containsSame(premiere);
        assertThat(documentRepository.count()).isEqualTo(avant);
    }

    @Test
    void echeanceDuJour_nonEligible() {
        ComDocument echeance = echeance(LocalDate.now(clock).atStartOfDay());

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
        assertThat(echeance.getComPenaltyDocument()).isNull();
    }

    @Test
    void penaliteDesactiveeSurLeDossier_nonEligible() {
        dossier.setIsPenaltyDisabled(true);
        ComDocument echeance = echeance(hier);

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
    }

    @Test
    void categorieSansPenalite_nonEligible() {
        avance.setIsWithPenalty(false);
        ComDocument echeance = echeance(hier);

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
    }

    @Test
    void echeanceDeLaCategoriePenalite_nonEligible() {
        categoriePenalite.setIsWithPenalty(true);
        ComDocument echeance = fixture.echeance(dossier, categoriePenalite, new BigDecimal("600"), hier);

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
    }

    @Test
    void categoriePenaliteNonParametree_rienNEstCree() {
        when(parametres.categoriePenalite()).thenReturn(Optional.empty());
        ComDocument echeance = echeance(hier);
        long avant = documentRepository.count();

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
        assertThat(documentRepository.count()).isEqualTo(avant);
    }

    @Test
    void tauxNul_rienNEstCree() {
        when(parametres.tauxPenalite()).thenReturn(BigDecimal.ZERO);
        ComDocument echeance = echeance(hier);

        assertThat(penalites.genererPenalite(echeance)).isEmpty();
    }
}
