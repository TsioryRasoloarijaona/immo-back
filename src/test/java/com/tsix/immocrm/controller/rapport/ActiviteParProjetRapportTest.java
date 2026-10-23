package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * N° 6 {@code ComTdbActiviteParProjet} (FEATURE-tdb-vente-rapports-flux.md §4.2, §9 « N° 6 »), année 2023 :
 * <pre>
 *   Programme A / T1 : CONC concrétisé 02/2023 ; ANN concrétisé 04/2023 annulé 09/2023 ; MIN acte 05/2023 (1 200 000) ;
 *                      encaissement 5 000 en 06/2023
 *   Programme A / T2 : CLEFS livré 03/2023 ; DES désisté 10/2023
 *   Programme B / T3 : CONC concrétisé 01/2023
 *   Programme A / T4 : CONC concrétisé 01/2022 seulement (aucun mouvement en 2023)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteParProjetRapportTest {

    private static final int ANNEE = 2023;
    private static final String PROJETS = "projets";
    private static final String TRANCHE = "tranche";
    private static final String PROGRAMME = "programme";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String LIVRAISONS = "livraisons";
    private static final String DESISTEMENTS = "desistements";
    private static final String CA_TTC = "caTtc";
    private static final String VERSEMENTS = "versements";
    private static final List<String> CHIFFRES = List.of(CONCRETISATIONS, LIVRAISONS, DESISTEMENTS, CA_TTC, VERSEMENTS);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgProject projetA;
    private CfgProject projetB;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;
    private CfgTranche t4;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N06");
        libre = connecte(rapports.compte("n06." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        projetA = jeu.projet("PA", true);
        projetB = jeu.projet("PB", true);
        t1 = jeu.tranche("T1", projetA);
        t2 = jeu.tranche("T2", projetA);
        t3 = jeu.tranche("T3", projetB);
        t4 = jeu.tranche("T4", projetA);

        ComFolder c1 = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2023, 2, 2), null, null, null);
        jeu.encaisser(c1, "5000", j(2023, 6, 6));
        jeu.dater(jeu.dossier(t1, st.annulation()), j(2023, 4, 4), null, null, j(2023, 9, 9));
        jeu.dater(jeu.dossier(t1, st.minute()), null, j(2023, 5, 5), null, null);
        jeu.dater(jeu.dossier(t2, st.remiseClefs()), null, null, j(2023, 3, 3), null);
        jeu.dater(jeu.dossier(t2, st.desistement()), null, null, null, j(2023, 10, 10));
        jeu.dater(jeu.dossier(t3, st.concretisation()), j(2023, 1, 1), null, null, null);
        jeu.dater(jeu.dossier(t4, st.concretisation()), j(2022, 1, 1), null, null, null);
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private Map<String, Object> projets() throws Exception {
        return section(appels.sections(demande(ACTIVITE_PAR_PROJET, libre, ANNEE, t1, t2, t3, t4)), PROJETS);
    }

    @Test
    void n6_uneLigneParTrancheActive_trancheSansMouvementAbsente_trieeProgrammePuisTranche() throws Exception {
        Map<String, Object> s = projets();

        assertThat(lignes(s)).extracting(l -> l.get(PROGRAMME), l -> l.get(TRANCHE)).containsExactly(
                tuple(projetA.getDescription(), t1.getDescription()),
                tuple(projetA.getDescription(), t2.getDescription()),
                tuple(projetB.getDescription(), t3.getDescription()));
    }

    @Test
    void n6_valeursParTranche_concretisationsAvecAnnulees() throws Exception {
        Map<String, Object> s = projets();
        Map<String, Object> l1 = ligne(s, TRANCHE, t1.getDescription());
        Map<String, Object> l2 = ligne(s, TRANCHE, t2.getDescription());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l1.get(CONCRETISATIONS))).isEqualByComparingTo("2");
        soft.assertThat(nombre(l1.get(CA_TTC))).isEqualByComparingTo("1200000");
        soft.assertThat(nombre(l1.get(VERSEMENTS))).isEqualByComparingTo("5000");
        soft.assertThat(nombre(l1.get(LIVRAISONS))).isEqualByComparingTo("0");
        soft.assertThat(nombre(l2.get(LIVRAISONS))).isEqualByComparingTo("1");
        soft.assertThat(nombre(l2.get(DESISTEMENTS))).isEqualByComparingTo("1");
        soft.assertThat(nombre(l2.get(CONCRETISATIONS))).isEqualByComparingTo("0");
        soft.assertThat(nombre(ligne(s, TRANCHE, t3.getDescription()).get(CONCRETISATIONS))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void n6_totauxSomme_egalSommeDesLignes_egalFluxDeLAnnee() throws Exception {
        Map<String, Object> s = projets();

        SoftAssertions soft = new SoftAssertions();
        for (String nom : CHIFFRES) {
            soft.assertThat(colonne(s, nom).get("fonctionTotal")).as(nom).isEqualTo("SOMME");
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertThat(nombre(totaux(s).get(CONCRETISATIONS))).isEqualByComparingTo("3");
        soft.assertThat(nombre(totaux(s).get(LIVRAISONS))).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(s).get(DESISTEMENTS))).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(s).get(CA_TTC))).isEqualByComparingTo("1200000");
        soft.assertThat(nombre(totaux(s).get(VERSEMENTS))).isEqualByComparingTo("5000");
        soft.assertAll();
    }

    @Test
    void n6_colonnes_typesEtLibelles_sansGraphique_titreAvecLAnnee() throws Exception {
        Map<String, Object> s = projets();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(noms(s)).containsExactly(PROGRAMME, TRANCHE, CONCRETISATIONS, LIVRAISONS, DESISTEMENTS, CA_TTC,
                VERSEMENTS);
        soft.assertThat(colonne(s, PROGRAMME).get("type")).isEqualTo("TEXTE");
        soft.assertThat(colonne(s, TRANCHE).get("type")).isEqualTo("TEXTE");
        for (String nom : List.of(CONCRETISATIONS, LIVRAISONS, DESISTEMENTS)) {
            soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo("NOMBRE");
        }
        soft.assertThat(colonne(s, CA_TTC).get("type")).isEqualTo("MONTANT");
        soft.assertThat(colonne(s, VERSEMENTS).get("type")).isEqualTo("MONTANT");
        soft.assertThat(String.valueOf(colonne(s, CONCRETISATIONS).get("libelle"))).contains("dont annulées");
        soft.assertThat(String.valueOf(colonne(s, CA_TTC).get("libelle"))).contains(String.valueOf(ANNEE));
        soft.assertThat((List<?>) s.get("graphiques")).isNullOrEmpty();
        soft.assertThat(String.valueOf(s.get("titre"))).contains(String.valueOf(ANNEE));
        soft.assertAll();
    }

    @Test
    void n6_anneeSansAucunMouvement_aucuneLigne() throws Exception {
        Map<String, Object> s = section(appels.sections(demande(ACTIVITE_PAR_PROJET, libre, 2020, t1, t2, t3, t4)),
                PROJETS);

        assertThat(lignes(s)).isEmpty();
    }
}
