package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheRegles;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Fiche « prévision » de la tranche — FEATURE-patrimoine-import-autres.md §0 (F1, F2), §1 décision 2, §3.4 :
 * {@code GET /api/tranches/{id}/previsions-base/fiche.pdf} en {@code inline}, même droit et même visibilité que
 * {@code GET /{id}/previsions-base} ; contenu = champs de {@code PrevisionsBaseDto} (section « Hypothèses », une ligne
 * par mois).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichePrevisionsBaseTest {

    private static final String FICHE = "/api/tranches/{id}/previsions-base/fiche.pdf";
    private static final String PREVISIONS = "/api/tranches/{id}/previsions-base";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheRegles regles;
    @Autowired private FixtureEntites entites;
    @Autowired private CfgTrancheRepository trancheRepository;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = FixtureExportPatrimoine.jeton();
        entite = fixture.entite("ENT-FP-" + jeton, true);
        tranche = fixture.tranche("TR-FP-" + jeton, fixture.projet("PRJ-FP-" + jeton, entite));
        tranche.setCountItemBasePrevision(3);
        tranche.setTotalSaleBasePrevision(new BigDecimal("3000000"));
        tranche.setBaseConcretisationPercent(new BigDecimal("0.4"));
        tranche.setBaseMarketingStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        tranche.setBaseEndDateConcretisation(LocalDateTime.of(2027, 3, 1, 0, 0));
        tranche.setBaseStartDateFinalisation(LocalDateTime.of(2027, 3, 1, 0, 0));
        tranche.setBaseMarketingEndDate(LocalDateTime.of(2027, 5, 1, 0, 0));
        trancheRepository.saveAndFlush(tranche);
        note(regles.lignePrevision(tranche.getId(), 2027, 1, 2, 0), "NOTEJAN" + jeton);
        note(regles.lignePrevision(tranche.getId(), 2027, 2, 1, 3), "NOTEFEV" + jeton);
        em.flush();
        em.clear();
    }

    private void note(UUID ligne, String notes) {
        em.createNativeQuery("UPDATE CfgPrevision SET Notes = :notes WHERE PKey = :id")
                .setParameter("notes", notes).setParameter("id", ligne).executeUpdate();
    }

    private MvcResult fiche(UUID id, RequestPostProcessor qui) throws Exception {
        return executer(mockMvc, get(FICHE, id).with(qui));
    }

    // ------------------------------------------------------------------ F2 contenu

    @Test
    void f2_pdfInline_200() throws Exception {
        MvcResult resultat = fiche(tranche.getId(), connecte(admin));

        assertAll(
                () -> assertThat(resultat.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE),
                () -> assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline"),
                () -> assertThat(estPdf(resultat.getResponse().getContentAsByteArray())).isTrue());
    }

    @Test
    void f2_texte_enTeteTrancheEtHypotheses_uneLigneParMois() throws Exception {
        MvcResult resultat = fiche(tranche.getId(), connecte(admin));
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);

        String texte = textePdf(resultat.getResponse().getContentAsByteArray());

        assertAll(
                () -> assertThat(compact(texte)).contains(compact(tranche.getDescription())),
                () -> assertThat(texte).containsIgnoringCase("Hypothèses"),
                () -> assertThat(compact(texte)).contains("NOTEJAN" + jeton).contains("NOTEFEV" + jeton),
                () -> assertThat(texte).contains("2027"));
    }

    // ------------------------------------------------------------------ F1 visibilité, droit

    @Test
    void f1_trancheDUneEntiteDevenueInactive_404_commeLesPrevisions() throws Exception {
        assertThat(fiche(tranche.getId(), connecte(admin)).getResponse().getStatus()).isEqualTo(200);

        entites.actif(entite.getId(), false);
        em.flush();
        em.clear();

        assertAll(
                () -> assertThat(executer(mockMvc, get(PREVISIONS, tranche.getId()).with(connecte(admin)))
                        .getResponse().getStatus()).isEqualTo(404),
                () -> assertThat(fiche(tranche.getId(), connecte(admin)).getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    void f1_trancheInconnue_404() throws Exception {
        assertThat(fiche(UUID.randomUUID(), connecte(admin)).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void r3_4_commercial_memeStatutQueLesPrevisions() throws Exception {
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(fixture.administrateur());
        em.flush();
        int previsions = executer(mockMvc, get(PREVISIONS, tranche.getId()).with(commercial)).getResponse().getStatus();
        assertThat(previsions).isEqualTo(200);

        assertThat(fiche(tranche.getId(), commercial).getResponse().getStatus()).isEqualTo(previsions);
    }
}
