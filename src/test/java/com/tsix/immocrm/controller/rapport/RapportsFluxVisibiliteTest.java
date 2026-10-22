package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
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
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_MOIS;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ACTIVITE_PAR_PROJET;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.COMPARAISON;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ETAT_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.encaissementsReel;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.sommeDesChiffres;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visibilité des rapports de flux (FEATURE-tdb-vente-rapports-flux.md §2 RV1–RV2, §8.1 R-V1…R-V3, I2, I3). Année 2024 ; dans chaque
 * tranche : un dossier MIN concrétisé 03/2024, acte 04/2024 (1 200 000), encaissé en 05/2024 :
 * <pre>
 *   TA : projet A (entité active)  — encaissement 1 000
 *   TB : projet B (entité active)  — encaissement 7 000
 *   TI : projet I (entité INACTIVE) — encaissement 3 000
 * </pre>
 * Le commercial A est affecté au seul projet A ; le compte libre n'a pas de périmètre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsFluxVisibiliteTest {

    private static final int ANNEE = 2024;
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String TOTAL = "total";
    private static final String PROJETS = "projets";
    private static final String TRANCHE = "tranche";
    private static final String VERSEMENTS = "versements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole roleLibre;
    private CfgProject projetA;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("VFX");
        prefixe = jeu.prefixe();
        roleLibre = rapports.role("Libre " + prefixe, false);
        Statuts st = jeu.statuts();
        projetA = jeu.projet("PA", true);
        ta = jeu.tranche("TA", projetA);
        tb = jeu.tranche("TB", jeu.projet("PB", true));
        ti = jeu.tranche("TI", jeu.projet("PI", false));
        for (Map.Entry<CfgTranche, String> e : Map.of(ta, "1000", tb, "7000", ti, "3000").entrySet()) {
            ComFolder d = jeu.dater(jeu.dossier(e.getKey(), st.minute()), LocalDate.of(ANNEE, 3, 3),
                    LocalDate.of(ANNEE, 4, 4), null, null);
            jeu.encaisser(d, e.getValue(), LocalDate.of(ANNEE, 5, 5));
        }
        fixture.relire();
    }

    private RequestPostProcessor libre() {
        return connecte(rapports.compte("vfx.libre." + prefixe, roleLibre));
    }

    private RequestPostProcessor commercialA() {
        AdmUser compte = rapports.compte("vfx.com." + prefixe, roleLibre);
        ev.affecter(compte, projetA);
        rapports.relire();
        return connecte(compte);
    }

    private Map<String, Object> encaissementsN14(RequestPostProcessor qui, CfgTranche... tranches) throws Exception {
        return section(appels.sections(demande(ETAT_VENTES, qui, ANNEE, tranches)), ENCAISSEMENTS);
    }

    private static List<Object> tranchesDuN6(Map<String, Object> projets) {
        return lignes(projets).stream().map(l -> l.get(TRANCHE)).toList();
    }

    // ------------------------------------------------------------------ R-V1 / I2 : périmètre commercial

    @Test
    void r_v1_perimetreProjetA_n14_totalDesEncaissements_egalTrancheA() throws Exception {
        Map<String, Object> s = encaissementsN14(commercialA());

        assertThat(nombre(totaux(s).get(TOTAL))).isEqualByComparingTo("1000");
    }

    @Test
    void r_v1_perimetreProjetA_n6_seuleLaTrancheA() throws Exception {
        Map<String, Object> s = section(appels.sections(demande(ACTIVITE_PAR_PROJET, commercialA(), ANNEE)), PROJETS);

        assertThat(tranchesDuN6(s)).containsExactly(ta.getDescription());
        assertThat(nombre(totaux(s).get(VERSEMENTS))).isEqualByComparingTo("1000");
    }

    @Test
    void r_v1_perimetreProjetA_n5_encaissementsDeLaTrancheA() throws Exception {
        Map<String, Object> mensuel = section(appels.sections(demande(ACTIVITE_PAR_MOIS, commercialA(), ANNEE)),
                "mensuel");

        assertThat(nombre(totaux(mensuel).get(encaissementsReel(mensuel)))).isEqualByComparingTo("1000");
    }

    @Test
    void r_v1_perimetreProjetA_n3_trancheA_encaissementsDeA() throws Exception {
        Map<String, Object> s = section(appels.sections(demande(COMPARAISON, commercialA(), null, ta)), ENCAISSEMENTS);

        assertThat(nombre(totaux(s).get("reel"))).isEqualByComparingTo("1000");
    }

    // ------------------------------------------------------------------ R-V2 : entité inactive

    @Test
    void r_v2_entiteInactive_absenteDesLignesEtDesTotaux_n14EtN6() throws Exception {
        RequestPostProcessor libre = libre();

        Map<String, Object> n14 = encaissementsN14(libre);
        Map<String, Object> n6 = section(appels.sections(demande(ACTIVITE_PAR_PROJET, libre, ANNEE)), PROJETS);

        assertThat(nombre(totaux(n14).get(TOTAL))).isEqualByComparingTo("8000");
        assertThat(tranchesDuN6(n6)).contains(ta.getDescription(), tb.getDescription())
                .doesNotContain(ti.getDescription());
        assertThat(nombre(totaux(n6).get(VERSEMENTS))).isEqualByComparingTo("8000");
    }

    @Test
    void r_v2_trancheIdDUneEntiteInactive_aucunChiffre() throws Exception {
        assertThat(sommeDesChiffres(encaissementsN14(libre(), ti))).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ R-V3 / I3 : trancheId hors portée

    @Test
    void r_v3_trancheIdHorsPerimetre_quatreRapports_aucunChiffre_jamaisToutes() throws Exception {
        RequestPostProcessor qui = commercialA();

        SoftAssertions soft = new SoftAssertions();
        for (String code : List.of(ETAT_VENTES, ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON)) {
            List<Map<String, Object>> sections = appels.sections(demande(code, qui, ANNEE, tb));
            soft.assertThat(sections).as(code).isNotEmpty();
            for (Map<String, Object> s : sections) {
                soft.assertThat(sommeDesChiffres(s)).as("%s.%s", code, s.get("id")).isEqualByComparingTo("0");
            }
        }
        soft.assertAll();
    }

    @Test
    void r_v3_trancheIdHorsPerimetre_n6EtN3_sectionsSansLigne() throws Exception {
        RequestPostProcessor qui = commercialA();

        Map<String, Object> n6 = section(appels.sections(demande(ACTIVITE_PAR_PROJET, qui, ANNEE, tb)), PROJETS);
        List<Map<String, Object>> n3 = appels.sections(demande(COMPARAISON, qui, null, tb));

        assertThat(lignes(n6)).isEmpty();
        assertThat(n3).isNotEmpty().allSatisfy(s -> assertThat(lignes(s)).isEmpty());
    }
}
