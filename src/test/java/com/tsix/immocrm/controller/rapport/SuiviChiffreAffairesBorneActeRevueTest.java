package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SUIVI_CA;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;

/**
 * Revue R1b, n° 15 : le réglé « avant l'acte » (prévu) et {@code ENCAISSEMENTS_APRES_ACTE} (réel) partitionnent les
 * règlements d'un dossier au jour de l'acte — la veille est retranchée du prévu, le jour même compte en réel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviChiffreAffairesBorneActeRevueTest {

    private static final int ANNEE = 2025;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;

    @Test
    void n15_reglementDuJourDeLActe_compteEnReelEtNonRetrancheDuPrevu() throws Exception {
        AppelsRapports appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N15B");
        RequestPostProcessor libre = connecte(rapports.compte("n15b." + jeu.prefixe(),
                rapports.role("Libre " + jeu.prefixe(), false)));
        CfgTranche t = jeu.delais(jeu.tranche("T1", jeu.projet("P", true)), 60, 30);
        ComFolder d = ev.prix(jeu.dater(jeu.dossier(t, jeu.statuts().minute()), null, LocalDate.of(ANNEE, 4, 10), null,
                null), new BigDecimal("1000000"));
        regler(jeu, d, "200000", LocalDate.of(ANNEE, 4, 9));
        regler(jeu, d, "300000", LocalDate.of(ANNEE, 4, 10));
        fixture.relire();

        Map<String, Object> s = section(appels.sections(demande(SUIVI_CA, libre, ANNEE, t)), "encaissements-reste");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne(s, "mois", "Mai").get("prevu"))).as("1 000 000 − 200 000 (veille)")
                .isEqualByComparingTo("0.80");
        soft.assertThat(nombre(ligne(s, "mois", "Avril").get("reel"))).as("règlement du jour de l'acte")
                .isEqualByComparingTo("0.30");
        soft.assertAll();
    }

    private static void regler(Jeu jeu, ComFolder dossier, String montant, LocalDate date) {
        BigDecimal valeur = new BigDecimal(montant);
        jeu.regler(jeu.echeance(dossier, jeu.categorie(Genre.AUTOFINANCEMENT, true), valeur, date), valeur, true,
                IN_WORK, null, date);
    }
}
