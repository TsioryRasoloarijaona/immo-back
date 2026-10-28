package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CREDIT_EN_COURS;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot P9f, règles M5 et R1 (FEATURE-patrimoine-impressions-tranche.md §1.2, §1.4, §4) : l'aperçu et le PDF de la Relance 2
 * n'écrivent rien — ni {@code ComFolder} (toutes colonnes, dont {@code ContractPrintDate}), ni {@code ComActionMessage}, ni
 * {@code AdmWebHistorics}. Empreinte = comptage + {@code CHECKSUM_AGG(BINARY_CHECKSUM(*))}, avec un témoin qui prouve qu'elle
 * voit une écriture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTrancheLectureSeuleTest {

    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";
    private static final String LETTRES = "/api/tranches/{id}/relances-2.pdf";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureImpressionsTranche fixture;

    private AdmUser admin;
    private Jeu jeu;
    private List<ComFolder> dossiers;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeu = fixture.jeu();
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder a = fixture.prix(fixture.dossier(jeu, j + "-A", statut), "650000.75");
        fixture.echeance(a, Nature.AVANCE, "650000.75", "150000.25", "5000");
        fixture.coAcquereur(a, "COL" + j);
        ComFolder b = fixture.relance2(fixture.dossierSousArticle(jeu, j + "-B", fixture.statut(CREDIT_EN_COURS)),
                LocalDateTime.of(2026, 3, 2, 0, 0));
        ComFolder solde = fixture.prix(fixture.dossier(jeu, j + "-S", statut), "10000");
        fixture.echeance(solde, Nature.AVANCE, "10000", "10000", null);
        dossiers = List.of(a, b, solde);
        fixture.relire();
    }

    private List<UUID> ids() {
        return dossiers.stream().map(ComFolder::getId).toList();
    }

    private List<UUID> autres() {
        return List.of(jeu.tranche().getId(), jeu.projet().getId());
    }

    @Test
    void m5_apercuEtPdf_nEcriventRien_niDossierNiActionNiHistorique() throws Exception {
        List<Object> avant = fixture.empreinte(ids(), autres());

        int apercu = executer(mockMvc, get(APERCU, jeu.tranche().getId()).with(connecte(admin))).getResponse().getStatus();
        int pdf = executer(mockMvc, get(LETTRES, jeu.tranche().getId()).with(connecte(admin))).getResponse().getStatus();

        fixture.relire();
        List<Object> apres = fixture.empreinte(ids(), autres());
        assertAll(
                () -> assertThat(apercu).isEqualTo(200),
                () -> assertThat(pdf).isEqualTo(200),
                () -> assertThat(apres).isEqualTo(avant));
    }

    /** R1 : la date d'impression du contrat n'est écrite que par la fiche de réservation du dossier, jamais par la relance. */
    @Test
    void r1_contractPrintDate_inchangeeParLApercuEtLePdf() throws Exception {
        LocalDateTime dejaImprime = LocalDateTime.of(2025, 11, 3, 10, 0);
        fixture.ecrireDateImpressionContrat(dossiers.get(1), dejaImprime);
        fixture.relire();

        int apercu = executer(mockMvc, get(APERCU, jeu.tranche().getId()).with(connecte(admin))).getResponse().getStatus();
        int pdf = executer(mockMvc, get(LETTRES, jeu.tranche().getId()).with(connecte(admin))).getResponse().getStatus();

        assertAll(
                () -> assertThat(apercu).isEqualTo(200),
                () -> assertThat(pdf).isEqualTo(200),
                () -> assertThat(fixture.dateImpressionContrat(dossiers.get(0))).isNull(),
                () -> assertThat(fixture.dateImpressionContrat(dossiers.get(1))).isEqualTo(dejaImprime));
    }

    /** Témoin : l'empreinte change dès qu'une colonne d'un dossier change (sinon M5 ne prouverait rien). */
    @Test
    void temoin_empreinteVoitUneEcritureDeDossier() {
        List<Object> avant = fixture.empreinte(ids(), autres());

        fixture.ecrireDateImpressionContrat(dossiers.get(0), LocalDateTime.of(2026, 1, 5, 0, 0));
        fixture.relire();

        assertThat(fixture.empreinte(ids(), autres())).isNotEqualTo(avant);
    }
}
