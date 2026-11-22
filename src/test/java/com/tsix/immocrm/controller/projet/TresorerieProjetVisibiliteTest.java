package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
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

import java.time.Clock;
import java.util.List;
import java.util.Map;

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
import static com.tsix.immocrm.controller.projet.LectureTresorerie.libelles;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.mois;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.noeud;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.totalOnglet;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N5a Trésorerie projet, droits et visibilité (FEATURE-n5-tableaux-de-bord-projet.md D1, D2, R15 précisé) : projets
 * demandés ∩ habilités, société habilitée, 400 sans projet, 403 sans droit ; chaque refus a son appel témoin 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieProjetVisibiliteTest {

    private static final String MESSAGE_SANS_PROJET = "Veuillez sélectionner au moins un projet !";
    private static final boolean DEBIT = false;

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

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
    }

    private RequestPostProcessor qui(AdmUser compte) {
        return connecte(compte, FixtureTresorerieProjet.ROLE);
    }

    private Reponse arbre(RequestPostProcessor qui, String... projets) throws Exception {
        String[] params = new String[4 + 2 * projets.length];
        params[0] = ANNEE;
        params[1] = ANNEE_TEST;
        params[2] = SOCIETE;
        params[3] = jeu.societe().toString();
        for (int i = 0; i < projets.length; i++) {
            params[4 + 2 * i] = PROJETS;
            params[5 + 2 * i] = projets[i];
        }
        return appels.lire(qui, TRESORERIE, params);
    }

    private String ok(Reponse r) {
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.corps();
    }

    // ------------------------------------------------------------------ D2 / R15 intersection

    @Test
    void r15_projetHorsPerimetreSeul_arbreVide200() throws Exception {
        String corps = ok(arbre(qui(jeu.compteA()), jeu.projetB().toString()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totalOnglet(corps, ONGLET_PROJET)).isZero();
            s.assertThat(totalOnglet(corps, ONGLET_FOURNISSEUR)).isZero();
            s.assertThat(libelles(corps)).doesNotContain(jeu.fournisseurBNom());
        });
    }

    @Test
    void r15_projetsAetB_habiliteA_seulementA() throws Exception {
        String corps = ok(arbre(qui(jeu.compteA()), jeu.projetA().toString(), jeu.projetB().toString()));

        assertThat(libelles(corps)).contains(jeu.fournisseurNom()).doesNotContain(jeu.fournisseurBNom());
    }

    @Test
    void r15_temoin_habiliteAetB_voitLeProjetB() throws Exception {
        String corps = ok(arbre(qui(jeu.compteAB()), jeu.projetA().toString(), jeu.projetB().toString()));

        Map<String, Object> fb = noeud(corps, ONGLET_PROJET, DEBIT, jeu.fournisseurBNom());
        assertThat(mois(fb, 9)).as("P5").isEqualByComparingTo("1000");
    }

    @Test
    void r15_societeNonHabilitee_lignesAbsentes() throws Exception {
        String corps = ok(arbre(qui(jeu.compteSansSociete()), jeu.projetA().toString()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(libelles(corps)).doesNotContain(jeu.fournisseurNom(), jeu.lotA2Libelle());
            s.assertThat(totalOnglet(corps, ONGLET_PROJET)).isZero();
        });
    }

    @Test
    void r15_selecteur_neProposePasLeProjetNonHabilite() throws Exception {
        Reponse r = arbre(qui(jeu.compteA()), jeu.projetA().toString());
        ok(r);

        assertThat(r.ids("$.projets[*].id")).map(String::toLowerCase).doesNotContain(jeu.projetB().toString());
    }

    // ------------------------------------------------------------------ D2 aucun projet : 400

    @Test
    void d2_aucunProjet_400_avecLeMessageOmran() throws Exception {
        Reponse refus = arbre(qui(jeu.compteA()));
        Reponse temoin = arbre(qui(jeu.compteA()), jeu.projetA().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
            s.assertThat(refus.corps()).contains(MESSAGE_SANS_PROJET);
            s.assertThat(temoin.statut()).as("témoin").isEqualTo(200);
        });
    }

    @Test
    void d2_cellule_aucunProjet_400() throws Exception {
        String cle = cleFournisseur(jeu.compteA());

        Reponse refus = appels.lire(qui(jeu.compteA()), CELLULE, ANNEE, ANNEE_TEST, MOIS, "6", NOEUD, cle);
        Reponse temoin = appels.lire(qui(jeu.compteA()), CELLULE, ANNEE, ANNEE_TEST, MOIS, "6", NOEUD, cle,
                PROJETS, jeu.projetA().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
            s.assertThat(refus.corps()).contains(MESSAGE_SANS_PROJET);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ D1 droit OMRAN : 403

    @Test
    void d1_sansDroit_403() throws Exception {
        Reponse refus = arbre(qui(jeu.compteSansDroit()), jeu.projetA().toString());
        Reponse temoin = arbre(qui(jeu.compteA()), jeu.projetA().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    @Test
    void d1_cellule_sansDroit_403() throws Exception {
        String cle = cleFournisseur(jeu.compteA());

        Reponse refus = appels.lire(qui(jeu.compteSansDroit()), CELLULE, ANNEE, ANNEE_TEST, MOIS, "6", NOEUD, cle,
                PROJETS, jeu.projetA().toString());

        assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ R15 cellule hors périmètre

    @Test
    void r15_cellule_projetHorsPerimetre_aucuneLigne() throws Exception {
        String corpsAb = ok(arbre(qui(jeu.compteAB()), jeu.projetB().toString()));
        String cle = String.valueOf(noeud(corpsAb, ONGLET_PROJET, DEBIT, jeu.fournisseurBNom()).get("cle"));

        Reponse horsPerimetre = appels.lire(qui(jeu.compteA()), CELLULE, ANNEE, ANNEE_TEST, MOIS, "9", NOEUD, cle,
                PROJETS, jeu.projetB().toString());
        Reponse temoin = appels.lire(qui(jeu.compteAB()), CELLULE, ANNEE, ANNEE_TEST, MOIS, "9", NOEUD, cle,
                PROJETS, jeu.projetB().toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(horsPerimetre.statut()).as(horsPerimetre.corps()).isEqualTo(200);
            s.assertThat(lignes(horsPerimetre)).isEmpty();
            s.assertThat(lignes(temoin)).as("témoin ab").extracting(l -> String.valueOf(l.get("objetId")).toLowerCase())
                    .containsExactly(jeu.p5().toString());
        });
    }

    private String cleFournisseur(AdmUser compte) throws Exception {
        String corps = ok(arbre(qui(compte), jeu.projetA().toString()));
        return String.valueOf(noeud(corps, ONGLET_PROJET, DEBIT, jeu.fournisseurNom()).get("cle"));
    }

    private static List<Map<String, Object>> lignes(Reponse r) {
        return r.statut() == 200 ? JsonPath.read(r.corps(), "$") : List.of();
    }
}
