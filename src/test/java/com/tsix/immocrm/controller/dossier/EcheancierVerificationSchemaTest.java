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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code VerifSchema} à l'enregistrement de l'échéancier et droits de suppression d'une échéance
 * (FEATURE-dossier-finitions.md §1, §3). Chaque refus est vérifié sans écriture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EcheancierVerificationSchemaTest {

    private static final String ECHEANCIER = "/api/dossiers/{id}/echeancier";
    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";
    private static final String PERIODIQUE = "/api/dossiers/{id}/echeancier/periodique";
    private static final String SOUSTRAIRE = "vous devez soustraire dans le schéma de financement un montant de -1 000,00 !";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categorieRepository;
    @Autowired private StrStatusRepository statusRepository;

    private ComFolder dossier;
    private ComSettlementCategory versement;
    private ComSettlementCategory autofinancement;
    private ComDocument autofinancementRecent;

    /** Prix 1 000 000 = avance 300 000 + autofinancement 400 000 + 300 000 ; statut qui vérifie le schéma. */
    @BeforeEach
    void dossierEquilibreSousVerification() {
        dossier = dossierEquilibre("DOS-VS", StatutDossier.RESERVATION);
    }

    private ComFolder dossierEquilibre(String code, StatutDossier statut) {
        CfgTranche tranche = fixture.tranche("TR-" + code, true);
        ComFolder d = fixture.dossier(code, tranche, statut);
        d.getStrStatus().setVerificationSchema(true);
        statusRepository.save(d.getStrStatus());
        d.setPriceAfterDiscount(new BigDecimal("1000000"));
        folderRepository.save(d);
        ComSettlementCategory avance = categorie("AVANCE-" + code, c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        versement = categorie("VERSEMENT-" + code, c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        autofinancement = categorie("AUTOFIN-" + code, c -> c.setIsSaleAutoFinancment(true));
        fixture.echeance(d, avance, new BigDecimal("300000"), date(2026, 10, 1));
        fixture.echeance(d, autofinancement, new BigDecimal("400000"), date(2027, 1, 1));
        autofinancementRecent = fixture.echeance(d, autofinancement, new BigDecimal("300000"), date(2027, 3, 1));
        return d;
    }

    // ---- Refus avant toute écriture --------------------------------------------------------------------

    @Test
    void ajoutQuiCreeUnEcart_409MessageOmran_aucuneLigneCreee() throws Exception {
        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(autofinancement, "1000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value("Votre schéma de financement ne correspond pas au montant "
                        + "contractuel : " + SOUSTRAIRE));

        assertThat(nombreDeLignes(dossier)).isEqualTo(3);
    }

    @Test
    void ajoutReporteSurLAutofinancement_passe() throws Exception {
        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(versement, "50000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ecartSchema").value(0));
    }

    @Test
    void modificationQuiCreeUnEcart_409_montantInchange() throws Exception {
        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":301000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(SOUSTRAIRE)));

        assertThat(montantRelu(autofinancementRecent)).isEqualByComparingTo("300000");
    }

    @Test
    void changementDeCategorieQuiSortLaLigneDuSchema_409_categorieInchangee() throws Exception {
        ComSettlementCategory frais = categorie("FRAIS-VS", c -> c.setIsSaleExpense(true));

        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"%s\"}".formatted(frais.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("vous devez ajouter")));

        entityManager.clear();
        assertThat(documentRepository.findById(autofinancementRecent.getId()).orElseThrow()
                .getComSettlementCategory().getId()).isEqualTo(autofinancement.getId());
    }

    @Test
    @WithMockUser(roles = "Administrateur")
    void suppressionQuiCreeUnEcart_409_ligneConservee() throws Exception {
        mockMvc.perform(delete(ECHEANCE, dossier.getId(), autofinancementRecent.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("vous devez ajouter")));

        entityManager.flush();
        assertThat(documentRepository.existsById(autofinancementRecent.getId())).isTrue();
    }

    @Test
    void periodiciteQuiCreeUnEcart_409_aucuneLigneCreee() throws Exception {
        mockMvc.perform(post(PERIODIQUE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId":"%s","montantTotal":30000,"nombre":3,"periodicite":"MENSUELLE",
                                 "premiereDate":"2026-12-01T00:00:00"}""".formatted(autofinancement.getId())))
                .andExpect(status().isConflict());

        assertThat(nombreDeLignes(dossier)).isEqualTo(3);
    }

    @Test
    void periodiciteReporteeEtLigneSoldeeRetiree_passe() throws Exception {
        mockMvc.perform(post(PERIODIQUE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId":"%s","montantTotal":300000,"nombre":3,"periodicite":"MENSUELLE",
                                 "premiereDate":"2026-12-01T00:00:00"}""".formatted(versement.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].ecartSchema").value(0));
    }

    // ---- Hors champ du contrôle ------------------------------------------------------------------------

    @Test
    void dossierSorti_horsSortieCommeOmran_ajoutAccepte() throws Exception {
        ComFolder annule = dossierEquilibre("DOS-VS-ANN", StatutDossier.ANNULATION);

        mockMvc.perform(post(ECHEANCIER, annule.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(autofinancement, "1000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ecartSchema").value(-1000));
    }

    @Test
    void statutSansVerification_ajoutAccepte() throws Exception {
        dossier.getStrStatus().setVerificationSchema(false);
        statusRepository.save(dossier.getStrStatus());

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(autofinancement, "1000")))
                .andExpect(status().isCreated());
    }

    // ---- Garde-fou : dossier déjà déséquilibré (écart −50 000) --------------------------------------------

    @Test
    void dejaDesequilibre_ecartReduit_passe() throws Exception {
        desequilibrer();

        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":330000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecartSchema").value(-30000));
    }

    @Test
    void dejaDesequilibre_ecartInchange_dateSeule_passe() throws Exception {
        desequilibrer();

        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dateEcheance\":\"2027-04-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecartSchema").value(-50000));
    }

    @Test
    void dejaDesequilibre_ecartCreuse_409_montantInchange() throws Exception {
        desequilibrer();

        mockMvc.perform(patch(ECHEANCE, dossier.getId(), autofinancementRecent.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":350001}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("-50 001,00")));

        assertThat(montantRelu(autofinancementRecent)).isEqualByComparingTo("350000");
    }

    // ---- Droits de suppression (§3) --------------------------------------------------------------------

    @Test
    void suppression_parUnCommercialChef_200() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, versement, BigDecimal.ZERO, date(2026, 11, 1));

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId()).with(user("chef").roles("Commercial Chef")))
                .andExpect(status().isOk());

        entityManager.flush();
        assertThat(documentRepository.existsById(ligne.getId())).isFalse();
    }

    @Test
    void suppression_parUnCommercial_403_ligneConservee() throws Exception {
        ComDocument ligne = fixture.echeance(dossier, versement, BigDecimal.ZERO, date(2026, 11, 1));

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId()).with(user("vendeur").roles("Commercial")))
                .andExpect(status().isForbidden());

        entityManager.flush();
        assertThat(documentRepository.existsById(ligne.getId())).isTrue();
    }

    // ---- Outils ----------------------------------------------------------------------------------------

    /** Autofinancement récent porté à 350 000 directement en base : écart −50 000. */
    private void desequilibrer() {
        autofinancementRecent.setAmount(new BigDecimal("350000"));
        documentRepository.save(autofinancementRecent);
        entityManager.flush();
    }

    private int nombreDeLignes(ComFolder d) {
        entityManager.flush();
        return documentRepository.findByComFolderIdOrderByDocDateAsc(d.getId()).size();
    }

    private BigDecimal montantRelu(ComDocument ligne) {
        entityManager.flush();
        entityManager.clear();
        return documentRepository.findById(ligne.getId()).orElseThrow().getAmount();
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

    private static String corps(ComSettlementCategory categorie, String montant) {
        return "{\"categorieId\":\"%s\",\"montant\":%s,\"dateEcheance\":\"2026-11-15T00:00:00\"}"
                .formatted(categorie.getId(), montant);
    }

    private static LocalDateTime date(int annee, int mois, int jour) {
        return LocalDateTime.of(annee, mois, jour, 0, 0);
    }
}
