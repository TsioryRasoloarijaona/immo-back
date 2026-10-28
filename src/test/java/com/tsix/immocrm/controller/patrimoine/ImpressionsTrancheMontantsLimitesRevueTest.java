package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import com.tsix.immocrm.fixtures.LectureExport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.pagesPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P9f, 2e tour (M1) : cas limites du calcul par lot — échéance de catégorie au débit, échéance sans catégorie,
 * {@code Price} nul — l'aperçu et le PDF portent les montants de {@code GET /api/dossiers/{id}/montants}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTrancheMontantsLimitesRevueTest {

    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";
    private static final String LETTRES = "/api/tranches/{id}/relances-2.pdf";
    private static final String MONTANTS = "/api/dossiers/{id}/montants";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureImpressionsTranche fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private EntityManager em;

    private static BigDecimal montant(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }

    private static boolean porteMontant(String page, String entier, String centimes) {
        return Pattern.compile("(?<![0-9])" + entier + "[.,]" + centimes + "(?![0-9])").matcher(page).find();
    }

    private void echeanceSansCategorie(ComFolder dossier, String montant, String regle) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setAmount(new BigDecimal(montant));
        d.setDocDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        d.setExcludeFromPrice(false);
        em.persist(d);
        em.flush();
        ev.regler(d, new BigDecimal(regle), true, null);
    }

    @Test
    void m1_debitSansCategorieEtPrixNul_apercuEtPdfEgauxAuxMontantsDuDossier() throws Exception {
        AdmUser admin = patrimoine.administrateur();
        Jeu jeu = fixture.jeu();
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);

        // Débit : 500 000 ; crédit réglé 200 000 ; remboursement (catégorie au débit) décaissé 50 000 ⇒ réglé 150 000.
        ComFolder debit = fixture.prix(fixture.dossier(jeu, j + "-DB", statut), "500000");
        ev.regler(ev.echeance(debit, Nature.AVANCE, true, new BigDecimal("500000")), new BigDecimal("200000"), true, null);
        ev.regler(ev.echeance(debit, Nature.MODIFICATION, false, new BigDecimal("50000")), new BigDecimal("50000"), false, null);
        // Sans catégorie : 300 000 réglé 120 000,50.
        ComFolder sansCategorie = fixture.prix(fixture.dossier(jeu, j + "-SC", statut), "300000");
        echeanceSansCategorie(sansCategorie, "300000", "120000.50");
        // Price nul, décaissement 30 000,25 ⇒ reste 30 000,25 > 0.
        ComFolder prixNul = ev.prix(fixture.dossier(jeu, j + "-PN", statut), null);
        ev.regler(ev.echeance(prixNul, Nature.AVANCE, true, new BigDecimal("10000")), new BigDecimal("30000.25"), false, null);
        fixture.relire();

        String json = mockMvc.perform(get(APERCU, jeu.tranche().getId()).with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        MvcResult pdf = executer(mockMvc, get(LETTRES, jeu.tranche().getId()).with(connecte(admin)));
        assertThat(pdf.getResponse().getStatus()).isEqualTo(200);
        List<String> pages = pagesPdf(pdf.getResponse().getContentAsByteArray()).stream().map(LectureExport::compact).toList();

        for (ComFolder dossier : List.of(debit, sansCategorie, prixNul)) {
            String m = mockMvc.perform(get(MONTANTS, dossier.getId()).with(connecte(admin)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            Map<String, Object> attendu = JsonPath.read(m, "$.prixVente");
            List<Map<String, Object>> lignes = JsonPath.read(json, "$.lignes[?(@.dossierId == '" + dossier.getId() + "')]");
            BigDecimal reste = montant(attendu.get("reste"));
            String[] parties = reste.setScale(2, RoundingMode.UNNECESSARY).toPlainString().split("\\.");
            assertThat(lignes).as(dossier.getCode()).singleElement().satisfies(l -> assertAll(
                    () -> assertThat(montant(l.get("prixRegle")).add(montant(l.get("reste"))))
                            .isEqualByComparingTo(montant(l.get("prixVente"))),
                    () -> assertThat(montant(l.get("prixVente"))).isEqualByComparingTo(montant(attendu.get("montant"))),
                    () -> assertThat(montant(l.get("prixRegle"))).isEqualByComparingTo(montant(attendu.get("regle"))),
                    () -> assertThat(montant(l.get("reste"))).isEqualByComparingTo(reste)));
            assertThat(pages).as(dossier.getCode() + " reste.prix imprimé")
                    .anySatisfy(p -> assertThat(porteMontant(p, parties[0], parties[1])).isTrue());
        }
        Map<UUID, String> restesAttendus = Map.of(debit.getId(), "350000", sansCategorie.getId(), "179999.50",
                prixNul.getId(), "30000.25");
        List<Map<String, Object>> toutes = JsonPath.read(json, "$.lignes");
        assertThat(toutes).hasSize(3).allSatisfy(l -> assertThat(montant(l.get("reste")))
                .isEqualByComparingTo(restesAttendus.get(UUID.fromString(String.valueOf(l.get("dossierId"))))));
        assertThat(pages).hasSize(3);
    }
}
