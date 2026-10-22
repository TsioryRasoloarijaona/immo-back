package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Visibilité des données par le socle (FEATURE-tdb-vente-rapports-socle.md §3.4 RV1–RV2, RD7, §9.1 « Visibilité »), via
 * le n° 12 (section {@code stock}, colonne {@code trancheCode}) et les options du paramètre {@code trancheId}.
 * <pre>
 *   TA : projet A, entité active, en vente        TB : projet B, entité active, en vente
 *   TI : projet I, entité INACTIVE, en vente      TN : projet A, PAS en vente (IsForSale = 0)
 *   TP : sans projet, en vente
 * </pre>
 * Un article VENTE par tranche. Tous les comptes voient le rapport (rôle libre) : seule la visibilité des données joue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsVisibiliteTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String TRANCHE_CODE = "trancheCode";
    private static final String STOCK = "stock";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole roleLibre;
    private CfgProject projetA;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;
    private CfgTranche tn;
    private CfgTranche tp;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RVI");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        roleLibre = fixture.role("Libre " + prefixe, false);

        projetA = ev.projet("PA-" + prefixe, true);
        ta = ev.tranche("TA-" + prefixe, projetA, true);
        tb = ev.tranche("TB-" + prefixe, ev.projet("PB-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        tn = ev.tranche("TN-" + prefixe, projetA, false);
        tp = ev.tranche("TP-" + prefixe, null, true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        for (CfgTranche t : List.of(ta, tb, ti, tn, tp)) {
            ev.article(t.getCode() + "-1", t, categorie, type);
        }
        fixture.relire();
    }

    private RequestPostProcessor libre() {
        return connecte(fixture.compte("vis.libre." + prefixe, roleLibre));
    }

    /** Rôle libre pour les rapports, non exempté du périmètre, affecté au seul projet A. */
    private RequestPostProcessor commercialA() {
        AdmUser compte = fixture.compte("vis.com." + prefixe, roleLibre);
        ev.affecter(compte, projetA);
        fixture.relire();
        return connecte(compte);
    }

    private List<String> options(RequestPostProcessor qui) throws Exception {
        return JsonPath.read(appels.corps(get(descripteur(VENTE, SUIVI_GLOBAL)).with(qui)),
                "$.parametres[?(@.nom == 'trancheId')].options[*].valeur");
    }

    private List<Object> tranchesDuStock(MockHttpServletRequestBuilder requete) throws Exception {
        return lignes(section(appels.sections(requete), STOCK)).stream().map(l -> l.get(TRANCHE_CODE)).toList();
    }

    private static String id(CfgTranche t) {
        return t.getId().toString();
    }

    /** Toutes les lignes de toutes les sections (RV1 : « résultat vide »). */
    private List<Map<String, Object>> toutesLesLignes(MockHttpServletRequestBuilder requete) throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete);
        assertThat(sections).isNotEmpty();
        List<Map<String, Object>> toutes = new ArrayList<>();
        sections.forEach(s -> toutes.addAll(lignes(s)));
        return toutes;
    }

    // ------------------------------------------------------------------ entité inactive, hors vente (RD7, §3.4)

    @Test
    void rv_options_sansEntiteInactive_niTrancheHorsVente_avecTrancheSansProjet() throws Exception {
        assertThat(options(libre())).contains(id(ta), id(tb), id(tp)).doesNotContain(id(ti), id(tn));
    }

    @Test
    void rv_chiffres_sansEntiteInactive_niTrancheHorsVente() throws Exception {
        assertThat(tranchesDuStock(get(resultat(VENTE, SUIVI_GLOBAL)).with(libre())))
                .contains(ta.getCode(), tb.getCode(), tp.getCode())
                .doesNotContain(ti.getCode(), tn.getCode());
    }

    @Test
    void rv1_trancheIdDUneEntiteInactive_resultatVide_jamaisToutes() throws Exception {
        assertThat(toutesLesLignes(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, id(ti)).with(libre())))
                .isEmpty();
    }

    @Test
    void rv1_trancheIdInconnu_ignoreSansErreur_resultatVide() throws Exception {
        assertThat(toutesLesLignes(get(resultat(VENTE, SUIVI_GLOBAL))
                .param(TRANCHE_ID, UUID.randomUUID().toString()).with(libre()))).isEmpty();
    }

    @Test
    void rv1_trancheIdHorsVente_resultatVide() throws Exception {
        assertThat(toutesLesLignes(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, id(tn)).with(libre())))
                .isEmpty();
    }

    @Test
    void rv1_melange_seuleLaTrancheVisibleRetenue() throws Exception {
        assertThat(tranchesDuStock(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, id(ta), id(ti))
                .with(libre()))).containsExactly(ta.getCode());
    }

    // ------------------------------------------------------------------ périmètre commercial (RV1–RV2)

    @Test
    void rv2_perimetreRestreint_optionsSansTrancheHorsPerimetre_trancheSansProjetVisible() throws Exception {
        assertThat(options(commercialA())).contains(id(ta), id(tp)).doesNotContain(id(tb), id(ti));
    }

    @Test
    void rv2_perimetreRestreint_chiffresSansTrancheHorsPerimetre() throws Exception {
        assertThat(tranchesDuStock(get(resultat(VENTE, SUIVI_GLOBAL)).with(commercialA())))
                .contains(ta.getCode(), tp.getCode())
                .doesNotContain(tb.getCode(), ti.getCode());
    }

    @Test
    void rv1_trancheIdHorsPerimetre_resultatVide_pasToutes() throws Exception {
        assertThat(toutesLesLignes(get(resultat(VENTE, SUIVI_GLOBAL)).param(TRANCHE_ID, id(tb))
                .with(commercialA()))).isEmpty();
    }

    @Test
    void rv2_exempte_voitToutesLesTranches_saufEntiteInactive() throws Exception {
        AdmRole direction = fixture.role("Direction Générale", false);
        AdmUser compte = fixture.compte("vis.dg." + prefixe, direction);
        ev.affecter(compte, projetA);
        fixture.relire();
        RequestPostProcessor exempte = connecte(compte);

        assertThat(options(exempte)).contains(id(ta), id(tb), id(tp)).doesNotContain(id(ti));
        assertThat(tranchesDuStock(get(resultat(VENTE, SUIVI_GLOBAL)).with(exempte)))
                .contains(ta.getCode(), tb.getCode())
                .doesNotContain(ti.getCode());
    }
}
