package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LectureDepenses.DEPENSES;
import static com.tsix.immocrm.controller.projet.LectureDepenses.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.nombre;
import static com.tsix.immocrm.controller.projet.LectureDepenses.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2ᵉ revue V1 (FEATURE-visibilite-omran.md Q3 : R5 = R3 + module PRJ sur les écrans Projet › Dépenses). L'écran
 * Projet › Dépenses › Factures (front {@code FacturesProjetList.tsx}) lit {@code /api/achat/documents?natures=INVOICE}
 * sans filtre de module : une facture d'un autre module y figure alors que la synthèse l'exclut.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesFacturesModuleRevue2Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private static final String DOCUMENTS = "/api/achat/documents";

    @Test
    void r5_facturesDuProjet_memeNombreQueLaSynthese_horsModulePrjExclu() throws Exception {
        Jeu jeu = fixture.jeu();
        UUID autre = factureHorsPrj(jeu);
        RequestPostProcessor a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
        AppelsProjet appels = new AppelsProjet(mockMvc);

        Reponse synthese = appels.lire(a, DEPENSES, PROJET_IDS, jeu.projetA().toString());
        Reponse factures = appels.lire(a, DOCUMENTS, "natures", "INVOICE", "moduleProjet", "true",
                PROJET_IDS, jeu.projetA().toString(), SIZE, TAILLE);

        long nbSynthese = nombre(LectureDepenses.projet(synthese.corps(), jeu.projetA()), "nbFactures").longValue();
        assertThat(factures.ids("$.content[*].id")).as("liste Projet › Dépenses › Factures")
                .doesNotContain(autre.toString());
        assertThat(total(factures.corps())).as("même nombre que la synthèse Dépenses").isEqualTo(nbSynthese);
    }

    @Test
    void ecranAchat_sansModuleProjet_factureHorsPrjVisible() throws Exception {
        Jeu jeu = fixture.jeu();
        UUID autre = factureHorsPrj(jeu);
        RequestPostProcessor a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);

        Reponse factures = new AppelsProjet(mockMvc).lire(a, DOCUMENTS, "natures", "INVOICE",
                PROJET_IDS, jeu.projetA().toString(), SIZE, TAILLE);

        assertThat(factures.ids("$.content[*].id")).as("écran Achat : tous les modules").contains(autre.toString());
    }

    private UUID factureHorsPrj(Jeu jeu) {
        UUID autre = fixture.facture(jeu.code("-AUT"), LocalDate.of(2026, 3, 9), jeu.fournisseur(),
                jeu.fournisseurNom(), jeu.societe(), jeu.projetA(), null, "90");
        visibilite.modulePiece(autre, visibilite.autreModule("STLX-" + jeu.jeton()));
        return autre;
    }
}
