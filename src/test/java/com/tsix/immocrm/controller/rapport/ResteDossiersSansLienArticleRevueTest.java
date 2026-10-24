package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SITUATION_GLOBALE;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue R1b (2ᵉ tour) : un dossier vivant dont l'article racine n'a pas de lien retour {@code StkItem.ComFolderId}
 * (prod : ARS 2026/00378 RESERVATION 910 000 et ARS 2026/00379 CONCRETISATION, reste 42 000, tranche AYAD RESIDENCE BEL AIR).
 * La tranche d'un dossier est celle de {@code ComFolder.StkItemId} (§3.1) ; le modèle lit le reste dans
 * {@code ComSaleFolderView}, jointe par {@code ComFolder.StkItemId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResteDossiersSansLienArticleRevueTest {

    private static final int ANNEE = 2025;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private StkItemRepository articles;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("RVL");
        libre = connecte(rapports.compte("rvl." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        tranche = jeu.tranche("T", projet);
        // Lien normal : réservé, prix 900 000, rien de réglé.
        jeu.prix(jeu.dossier(tranche, st.reservation(), "1000000"), new BigDecimal("900000"), null);
        // Sans lien retour : concrétisé 01/2025, prix 700 000, rien de réglé.
        ComFolder sansLien = jeu.prix(jeu.dater(jeu.dossier(tranche, st.concretisation(), "1000000"),
                LocalDate.of(2025, 1, 5), null, null, null), new BigDecimal("700000"), null);
        StkItem article = sansLien.getStkItem();
        article.setComFolderId(null);
        articles.save(article);
        fixture.relire();
    }

    @Test
    void n5_resteARegler_compteLeDossierVivantSansLienRetourDeSonArticle() throws Exception {
        Map<String, Object> situation = section(appels.sections(demande(ACTIVITE_PAR_MOIS, libre, ANNEE, tranche)),
                "situation");

        Map<String, Object> reste = ligne(situation, "mesure", "Reste à régler");

        assertThat(nombre(reste.get("base"))).as("900 000 (lié) + 700 000 (sans lien retour)")
                .isEqualByComparingTo("1600000");
    }

    @Test
    void n11_reliquatConcretises_compteLeDossierConcretiseSansLienRetourDeSonArticle() throws Exception {
        Map<String, Object> situation = section(appels.sections(demande(SITUATION_GLOBALE, libre, null, tranche)),
                "situation");

        Map<String, Object> l = ligne(situation, "tranche", tranche.getDescription());

        assertThat(nombre(l.get("reliquatConcretises"))).as("modèle : ComSaleFolderView.PriceRest par tranche du dossier")
                .isEqualByComparingTo("700000");
    }
}
