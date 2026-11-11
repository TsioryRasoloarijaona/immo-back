package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LectureDepenses.REGLEMENTS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue du lot A2 (FEATURE-achat-depenses-projet.md §3.1, §4.2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;
    @Autowired private FixtureAchat achat;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
    }

    /**
     * Cas réel (yesRemsProd, 10/11) : 3 factures du projet ART portent un lot (PrjMarketId) du projet 2025-MAYLINE.
     * §3.1 : « Un lotId d'un projet hors périmètre → réponse vide ». Ici le lot B est hors périmètre de « a ».
     */
    @Test
    void revue_lotIdHorsPerimetre_factureDuProjetVisiblePortantCeLot_pageVide() throws Exception {
        UUID facture = fixture.facture(jeu.code("-FX"), LocalDate.of(2026, 3, 9), jeu.fournisseur(),
                jeu.fournisseurNom(), jeu.societe(), jeu.projetA(), jeu.lotB(), "100");
        UUID type = achat.typeReglement("VIR-" + FixtureAchat.jeton("REV"));
        UUID reglement = fixture.reglement(jeu.code("-RGX"), FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 20), type, "100");
        fixture.ligne(reglement, facture, "100");

        Reponse r = appels.lire(a, REGLEMENTS, "lotId", jeu.lotB().toString(), SIZE, TAILLE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(total(r.corps())).as(r.corps()).isZero();
    }

    @Test
    void revue_inclureAnnulesMalForme_400() throws Exception {
        Reponse r = appels.lire(a, REGLEMENTS, "inclureAnnules", "peutetre");

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
