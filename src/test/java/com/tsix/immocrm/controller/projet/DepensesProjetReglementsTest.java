package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.projet.LectureDepenses.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.REGLEMENTS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.codesReglements;
import static com.tsix.immocrm.controller.projet.LectureDepenses.lignesDuReglement;
import static com.tsix.immocrm.controller.projet.LectureDepenses.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Liste des affectations de règlement du projet (FEATURE-achat-depenses-projet.md §4.2 {@code ReglementProjetDto} ;
 * §6 test 4). Jeu : {@link FixtureDepensesProjet}. Lignes visibles de « a » : RG1 (FA1 600), RG2 annulé (FA1 200),
 * RG3 (FH1 320), RG4 (FH2 150).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetReglementsTest {

    private static final String ANNULE = "annule";
    private static final String LIGNES = "$.content[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
    }

    private Reponse ok(String... params) throws Exception {
        String[] avecTaille = new String[params.length + 2];
        System.arraycopy(params, 0, avecTaille, 0, params.length);
        avecTaille[params.length] = SIZE;
        avecTaille[params.length + 1] = TAILLE;
        Reponse r = appels.lire(a, REGLEMENTS, avecTaille);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private Map<String, Object> seuleLigne(Reponse r, String suffixeReglement) {
        List<Map<String, Object>> lignes = lignesDuReglement(r.corps(), jeu.code(suffixeReglement));
        assertThat(lignes).as("lignes de %s", suffixeReglement).hasSize(1);
        return lignes.get(0);
    }

    // ------------------------------------------------------------------ §6.4 ligne d'un règlement CANCELED

    @Test
    void t4_ligneAnnulee_listeeAvecAnnuleVrai_parDefaut() throws Exception {
        Reponse r = ok();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(seuleLigne(r, "-RG2")).containsEntry(ANNULE, true)
                    .containsEntry("id", jeu.ligneAnnulee().toString());
            s.assertThat(seuleLigne(r, "-RG1")).containsEntry(ANNULE, false);
        });
    }

    @Test
    void t4_inclureAnnulesFaux_ligneAnnuleeAbsente() throws Exception {
        Reponse r = ok("inclureAnnules", "false");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(LIGNES)).doesNotContain(jeu.ligneAnnulee().toString())
                    .contains(jeu.ligneEnCours().toString());
            s.assertThat(lignesDuReglement(r.corps(), jeu.code("-RG2"))).isEmpty();
        });
    }

    // ------------------------------------------------------------------ §4.2 champs d'une ligne

    @Test
    void ligne_porteReglementFactureProjetLotEtDecompte() throws Exception {
        Map<String, Object> l = seuleLigne(ok(), "-RG1");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(l).containsEntry("id", jeu.ligneEnCours().toString())
                    .containsEntry("reglementId", jeu.reglementEnCours().toString())
                    .containsEntry("reglementCode", jeu.code("-RG1"))
                    .containsEntry("dateReglement", "2026-04-10")
                    .containsEntry("dateEcheance", "2026-05-10")
                    .containsEntry("typeCode", jeu.typeCode())
                    .containsEntry("typeDescription", "Type " + jeu.typeCode())
                    .containsEntry(ANNULE, false)
                    .containsEntry("factureId", jeu.factureA1().toString())
                    .containsEntry("factureCode", jeu.code("-FA1"))
                    .containsEntry("fournisseurNom", jeu.fournisseurNom())
                    .containsEntry("projetId", jeu.projetA().toString())
                    .containsEntry("projetCode", jeu.projetACode())
                    .containsEntry("lotId", jeu.lotA1().toString())
                    .containsEntry("lotCode", jeu.lotA1Code())
                    .containsEntry("decompteId", jeu.decompte1().toString())
                    .containsEntry("decompteCode", jeu.decompte1Code());
            s.assertThat(nombre(l, "montant")).isEqualByComparingTo("600");
        });
    }

    @Test
    void ligne_factureHorsLotSansDecompte_lotEtDecompteNuls() throws Exception {
        Map<String, Object> l = seuleLigne(ok(), "-RG3");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(l).containsKeys("lotId", "lotCode", "decompteId", "decompteCode")
                    .containsEntry("factureId", jeu.factureTropReglee().toString());
            s.assertThat(l.get("lotId")).isNull();
            s.assertThat(l.get("decompteId")).isNull();
            s.assertThat(nombre(l, "montant")).isEqualByComparingTo("320");
        });
    }

    // ------------------------------------------------------------------ §4.2 filtres et tri

    @Test
    void filtre_factureId_seulementLesLignesDeLaFacture() throws Exception {
        Reponse r = ok("factureId", jeu.factureA1().toString());

        assertThat(r.ids(LIGNES)).containsExactlyInAnyOrder(jeu.ligneEnCours().toString(), jeu.ligneAnnulee().toString());
    }

    @Test
    void filtre_lotId_seulementLesLignesDesFacturesDuLot() throws Exception {
        Reponse r = ok("lotId", jeu.lotA1().toString());

        assertThat(r.ids(LIGNES)).containsExactlyInAnyOrder(jeu.ligneEnCours().toString(), jeu.ligneAnnulee().toString());
    }

    @Test
    void filtre_duAu_surDateDuReglement_bornesIncluses() throws Exception {
        Reponse unJour = ok(PROJET_IDS, jeu.projetA().toString(), "du", "2026-04-12", "au", "2026-04-12");
        Reponse plage = ok(PROJET_IDS, jeu.projetA().toString(), "du", "2026-04-11", "au", "2026-04-13");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(codesReglements(unJour.corps())).containsExactly(jeu.code("-RG3"));
            s.assertThat(codesReglements(plage.corps())).containsExactlyInAnyOrder(jeu.code("-RG3"), jeu.code("-RG4"));
        });
    }

    @Test
    void tri_parDefaut_dateDecroissantePuisCodeDecroissant() throws Exception {
        Reponse r = ok(PROJET_IDS, jeu.projetA().toString());

        assertThat(codesReglements(r.corps()))
                .containsExactly(jeu.code("-RG4"), jeu.code("-RG3"), jeu.code("-RG2"), jeu.code("-RG1"));
    }
}
