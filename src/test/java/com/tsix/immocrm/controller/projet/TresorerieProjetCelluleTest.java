package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet.Jeu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE_TEST;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.CELLULE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.MOIS;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.NOEUD;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ONGLET_FOURNISSEUR;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ONGLET_PROJET;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.PROJETS;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.SOCIETE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.TRESORERIE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.mois;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.nombre;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.noeud;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.racine;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N5a Trésorerie projet, détail d'une cellule (FEATURE-n5-tableaux-de-bord-projet.md R7, R8, D5 remplacé : report à
 * aujourd'hui avec {@code reporte} et {@code dateOrigine}). {@code mois} = 1 à 12. Aujourd'hui = 15/06/2077.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieProjetCelluleTest {

    private static final boolean DEBIT = false;
    private static final String AUJOURDHUI = FixtureTresorerieProjet.AUJOURDHUI.toString();
    private static final String DATE = "date";
    private static final String REPORTE = "reporte";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(FixtureTresorerieProjet.AUJOURDHUI.atTime(10, 0).atZone(FuseauMetier.ZONE).toInstant(),
                FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerieProjet fixture;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureTresorerieProjet.ROLE);
    }

    private String arbre() throws Exception {
        Reponse r = appels.lire(a, TRESORERIE, ANNEE, ANNEE_TEST, PROJETS, jeu.projetA().toString(),
                SOCIETE, jeu.societe().toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.corps();
    }

    private List<Map<String, Object>> cellule(Map<String, Object> noeud, int m) throws Exception {
        Reponse r = appels.lire(a, CELLULE, ANNEE, ANNEE_TEST, MOIS, String.valueOf(m),
                NOEUD, String.valueOf(noeud.get("cle")), PROJETS, jeu.projetA().toString(),
                SOCIETE, jeu.societe().toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return JsonPath.read(r.corps(), "$");
    }

    private static BigDecimal somme(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> nombre(l.get("montant"))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<String> objets(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> String.valueOf(l.get("objetId")).toLowerCase()).toList();
    }

    // ------------------------------------------------------------------ R8 somme du détail = cellule

    @Test
    void r8_sommeDuDetail_egaleLaCellule_noeudFournisseur() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        List<Map<String, Object>> lignes = cellule(f, 6);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(somme(lignes)).isEqualByComparingTo(mois(f, 6)).isEqualByComparingTo("310");
            s.assertThat(objets(lignes)).containsExactlyInAnyOrder(jeu.p0().toString(), jeu.p1().toString(),
                    jeu.p6().toString(), jeu.fa2().toString());
        });
    }

    @Test
    void r8_sommeDuDetail_egaleLaCellule_racine() throws Exception {
        Map<String, Object> debit = racine(arbre(), ONGLET_PROJET, DEBIT).orElseThrow();

        List<Map<String, Object>> lignes = cellule(debit, 8);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(somme(lignes)).as("P3 300").isEqualByComparingTo(mois(debit, 8)).isEqualByComparingTo("300");
            s.assertThat(objets(lignes)).containsExactly(jeu.p3().toString());
        });
    }

    @Test
    void r8_sommeDuDetail_egaleLaCellule_ongletFournisseur() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_FOURNISSEUR, DEBIT, jeu.fournisseurNom());

        List<Map<String, Object>> lignes = cellule(f, 11);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(somme(lignes)).as("reste FA1, règlement annulé exclu").isEqualByComparingTo(mois(f, 11))
                    .isEqualByComparingTo("600");
            s.assertThat(objets(lignes)).containsExactly(jeu.fa1().toString());
        });
    }

    @Test
    void r8_detailTrieParDate() throws Exception {
        fixture.prevision(jeu.lotA1(), LocalDate.of(2077, 9, 3), "30");
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        List<Map<String, Object>> lignes = cellule(f, 9);

        assertThat(lignes).extracting(l -> String.valueOf(l.get(DATE)).substring(0, 10))
                .containsExactly("2077-09-03", "2077-09-20");
    }

    // ------------------------------------------------------------------ R7 / D5 remplacé : report à aujourd'hui

    @Test
    void r7_previsionPassee_reporteeAujourdhui_dateOrigineGardee() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        Map<String, Object> p1 = ligne(cellule(f, 6), jeu.p1());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(p1.get(DATE))).startsWith(AUJOURDHUI);
            s.assertThat(String.valueOf(p1.get("dateOrigine"))).startsWith("2077-03-10");
            s.assertThat(p1).containsEntry(REPORTE, true);
        });
    }

    @Test
    void r7_previsionDuMoisCourantApresAujourdhui_reporteeAussi() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        Map<String, Object> p6 = ligne(cellule(f, 6), jeu.p6());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(p6.get(DATE))).startsWith(AUJOURDHUI);
            s.assertThat(String.valueOf(p6.get("dateOrigine"))).startsWith("2077-06-25");
            s.assertThat(p6).containsEntry(REPORTE, true);
        });
    }

    @Test
    void r7_previsionDUneAnneeAnterieure_reporteeDansLAnneeCourante() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        Map<String, Object> p0 = ligne(cellule(f, 6), jeu.p0());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(p0.get("dateOrigine"))).startsWith("2076-11-10");
            s.assertThat(p0).containsEntry(REPORTE, true);
        });
    }

    @Test
    void r7_achatEchu_reporteAujourdhui() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        Map<String, Object> fa2 = ligne(cellule(f, 6), jeu.fa2());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(fa2.get(DATE))).startsWith(AUJOURDHUI);
            s.assertThat(String.valueOf(fa2.get("dateOrigine"))).startsWith("2077-02-01");
            s.assertThat(fa2).containsEntry(REPORTE, true);
            s.assertThat(nombre(fa2.get("montant"))).isEqualByComparingTo("120");
        });
    }

    @Test
    void r7_previsionFuture_nonReportee() throws Exception {
        Map<String, Object> f = noeud(arbre(), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        Map<String, Object> p2 = ligne(cellule(f, 9), jeu.p2());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(p2.get(DATE))).startsWith("2077-09-20");
            s.assertThat(p2).containsEntry(REPORTE, false);
            s.assertThat(nombre(p2.get("montant"))).isEqualByComparingTo("200");
        });
    }

    private static Map<String, Object> ligne(List<Map<String, Object>> lignes, UUID objet) {
        return lignes.stream().filter(l -> objet.toString().equalsIgnoreCase(String.valueOf(l.get("objetId"))))
                .findFirst().orElseThrow(() -> new AssertionError("ligne absente : " + objet));
    }
}
