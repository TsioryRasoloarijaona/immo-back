package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.sommeDesChiffres;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CODES_R2B;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CONCRET_LIBRES;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ENSEMBLE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_COMPARAISON;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_CONCRETISATIONS;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TAUX_CONCRET_ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TAUX_VENTE_STOCK;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonneDe;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonnesTranches;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CONCRET_ENCAISSEMENT;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.DATE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.NON_AFFECTE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.PAR_COMMERCIAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TOTAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonneLibellee;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonnesCommerciales;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Visibilité des rapports R2a (FEATURE-tdb-vente-rapports-rythme.md §2, §7.1 R-V1…R-V3, R-V6). Année 2025 ; dossiers CONC saisis
 * le 01/03/2025, concrétisés le 03/03/2025 ; visites le 02/02/2025 :
 * <pre>
 *   TA : projet A (actif) — A : 1 dossier (encaissé 1 000), B : 1 dossier (500), sans commercial : 1 dossier ; 1 visite
 *   TB : projet B (actif) — B : 1 dossier (7 000) ; 2 visites
 *   TI : projet I (entité INACTIVE) — A : 1 dossier (3 000) ; 4 visites
 * </pre>
 * A (Commercial) et C (Commercial Chef) sont affectés au projet A, B (Commercial) au projet B ; « direction » = Direction
 * Générale (exemptée, sans restriction de rapport).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsRythmeVisibiliteTest {

    private static final String AN = "2025";
    private static final String AU = "2025-12-31";
    private static final String RESERVATIONS = "reservations";
    private static final String PAR_MOIS = "par-mois";
    private static final String PAR_ANNEE = "par-annee";
    private static final String PROJETS = "projets";
    private static final String TRANCHE = "tranche";
    private static final String PROJET = "projet";
    private static final String VISITES = "visites";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String TRANCHES = "tranches";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;

    private AppelsRapports appels;
    private String prefixe;
    private CfgProject projetA;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;
    private AdmUser a;
    private AdmUser chef;
    private AdmUser direction;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installer(rapports);
        Jeu jeu = fixture.jeu("VR2");
        prefixe = jeu.prefixe();
        projetA = jeu.projet("PA", true);
        CfgProject projetB = jeu.projet("PB", true);
        ta = jeu.tranche("TA", projetA);
        tb = jeu.tranche("TB", projetB);
        ti = jeu.tranche("TI", jeu.projet("PI", false));

        AdmRole libre = rapports.role("Libre " + prefixe, false);
        a = rapports.compte(jeton("alpha"), libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        AdmUser b = rapports.compte(jeton("beta"), libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        chef = rapports.compte(jeton("gamma"), libre, rapports.role(RolesEquipe.CHEF, true));
        direction = rapports.compte(jeton("dg"), rapports.role("Direction Générale", false));
        ev.affecter(a, projetA);
        ev.affecter(chef, projetA);
        ev.affecter(b, projetB);

        jeu.encaisser(vente(jeu, ta, a), "1000", LocalDate.of(2025, 4, 4));
        jeu.encaisser(vente(jeu, ta, b), "500", LocalDate.of(2025, 4, 4));
        vente(jeu, ta, null);
        jeu.encaisser(vente(jeu, tb, b), "7000", LocalDate.of(2025, 4, 4));
        jeu.encaisser(vente(jeu, ti, a), "3000", LocalDate.of(2025, 4, 4));
        ComActionMessageType visite = fx.type(InteractionKpi.TYPE_VISITE);
        for (Map.Entry<CfgTranche, Integer> e : Map.of(ta, 1, tb, 2, ti, 4).entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                fx.prospection(e.getKey(), null, visite, LocalDate.of(2025, 2, 2), null);
            }
        }
        rapports.relire();
    }

    private String jeton(String nom) {
        return nom + "." + prefixe;
    }

    private ComFolder vente(Jeu jeu, CfgTranche tranche, AdmUser commercial) {
        ComFolder d = jeu.dossier(tranche, jeu.statuts().concretisation());
        d.setCfgCommercial(commercial == null ? null : commercial.getCfgEmployee());
        d.setDocDate(LocalDate.of(2025, 3, 1).atStartOfDay());
        return jeu.dater(d, LocalDate.of(2025, 3, 3), null, null, null);
    }

    private List<Map<String, Object>> n8(AdmUser qui, CfgTranche... tranches) throws Exception {
        return appels.sections(demande(RYTHME_VENTES, connecte(qui), List.of(tranches), ANNEE, AN));
    }

    private List<Map<String, Object>> n21(AdmUser qui, CfgTranche... tranches) throws Exception {
        return appels.sections(demande(PAR_COMMERCIAL, connecte(qui), List.of(tranches), ANNEE, AN));
    }

    private Map<String, Object> n23(AdmUser qui, CfgTranche... tranches) throws Exception {
        return section(appels.sections(demande(CONCRET_ENCAISSEMENT, connecte(qui), List.of(tranches), DATE, AU)),
                PROJETS);
    }

    private static BigDecimal total(Map<String, Object> section, String colonne) {
        return nombre(totaux(section).get(colonne));
    }

    // ------------------------------------------------------------------ R-V1 périmètre (projet A)

    @Test
    void r_v1_n8_commercialAffecteAuProjetA_seulesLesLignesDeTA_reservationsDeTA() throws Exception {
        Map<String, Object> reservations = section(n8(a), RESERVATIONS);

        assertThat(lignes(reservations)).isNotEmpty()
                .allSatisfy(l -> assertThat(String.valueOf(l.get(TRANCHE))).contains(ta.getCode()));
        assertThat(total(reservations, TOTAL)).isEqualByComparingTo("3");
    }

    @Test
    void r_v1_n21_commercialAffecteAuProjetA_seulSesConcretisationsSurTA() throws Exception {
        List<Map<String, Object>> sections = n21(a);

        Map<String, Object> annees = section(sections, PAR_ANNEE);
        assertThat(colonnesCommerciales(annees)).containsExactly(colonneLibellee(annees, jeton("alpha")));
        assertThat(total(annees, TOTAL)).as("ni B sur TA, ni le sans commercial, ni TI")
                .isEqualByComparingTo("1");
    }

    @Test
    void r_v1_n23_commercialAffecteAuProjetA_seulLeProjetA() throws Exception {
        Map<String, Object> s = n23(a);

        assertThat(lignes(s)).singleElement()
                .satisfies(l -> assertThat(String.valueOf(l.get(PROJET))).contains(projetA.getCode()));
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(total(s, VISITES)).isEqualByComparingTo("1");
        soft.assertThat(total(s, CONCRETISATIONS)).isEqualByComparingTo("3");
        soft.assertThat(total(s, ENCAISSEMENTS)).isEqualByComparingTo("1500");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-V2 entité inactive

    @Test
    void r_v2_n8_trancheDUneEntiteInactive_absenteDesLignesEtDesTotaux() throws Exception {
        Map<String, Object> reservations = section(n8(direction, ta, tb, ti), RESERVATIONS);

        assertThat(lignesContenant(reservations, TRANCHE, ti.getCode())).isEmpty();
        assertThat(lignesContenant(reservations, TRANCHE, ta.getCode())).isNotEmpty();
        assertThat(total(reservations, TOTAL)).isEqualByComparingTo("4");
    }

    @Test
    void r_v2_n21_trancheDUneEntiteInactive_absenteDesColonnesEtDesTotaux() throws Exception {
        Map<String, Object> annees = section(n21(direction, ta, tb, ti), PAR_ANNEE);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(total(annees, TOTAL)).as("TA 3 + TB 1").isEqualByComparingTo("4");
        soft.assertThat(total(annees, colonneLibellee(annees, jeton("alpha")))).as("A sans TI")
                .isEqualByComparingTo("1");
        soft.assertThat(total(annees, NON_AFFECTE)).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void r_v2_n23_projetDUneEntiteInactive_absent_totauxSansLui() throws Exception {
        Map<String, Object> s = n23(direction, ta, tb, ti);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignes(s)).hasSize(2);
        soft.assertThat(total(s, VISITES)).isEqualByComparingTo("3");
        soft.assertThat(total(s, CONCRETISATIONS)).isEqualByComparingTo("4");
        soft.assertThat(total(s, ENCAISSEMENTS)).isEqualByComparingTo("8500");
        soft.assertAll();
    }

    @Test
    void r_v2_seuleTrancheDemandeeInactive_aucunChiffre_surLesTroisRapports() throws Exception {
        assertAucunChiffre(direction, ti);
    }

    // ------------------------------------------------------------------ R-V3 trancheId hors portée

    @Test
    void r_v3_trancheIdHorsPerimetre_200_aucunChiffre_surLesTroisRapports() throws Exception {
        assertAucunChiffre(a, tb);
    }

    private void assertAucunChiffre(AdmUser qui, CfgTranche tranche) throws Exception {
        List<Map<String, Object>> n8 = n8(qui, tranche);
        List<Map<String, Object>> n21 = n21(qui, tranche);
        Map<String, Object> n23 = n23(qui, tranche);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(n8).as(RYTHME_VENTES).isNotEmpty();
        for (Map<String, Object> s : n8) {
            soft.assertThat(lignes(s)).as("n° 8 %s", s.get("id")).isEmpty();
        }
        soft.assertThat(n21).as(PAR_COMMERCIAL).isNotEmpty();
        for (Map<String, Object> s : n21) {
            soft.assertThat(sommeDesChiffres(s)).as("n° 21 %s", s.get("id")).isEqualByComparingTo("0");
            soft.assertThat(colonnesCommerciales(s)).as("n° 21 %s", s.get("id")).isEmpty();
        }
        soft.assertThat(lignes(n23)).as(CONCRET_ENCAISSEMENT).isEmpty();
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-V6 commerciaux (n° 21)

    @Test
    void r_v6_chefAffecte_neVoitQueLesCommerciauxDeSaPortee_niNonAffecte() throws Exception {
        List<Map<String, Object>> sections = n21(chef);

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(colonnes(s)).as("%s libellés", s.get("id")).isNotEmpty()
                    .noneMatch(c -> String.valueOf(c.get("libelle")).contains(jeton("beta")));
            soft.assertThat(noms(s)).as("%s", s.get("id")).doesNotContain(NON_AFFECTE);
        }
        Map<String, Object> mois = section(sections, PAR_MOIS);
        soft.assertThat(colonnesCommerciales(mois)).containsExactly(colonneLibellee(mois, jeton("alpha")));
        soft.assertThat(total(mois, TOTAL)).as("la seule vente de A sur TA").isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void r_v6_chefAffecte_exportSansLeNomDUnCommercialHorsPortee() throws Exception {
        MockHttpServletRequestBuilder requete = get(export(VENTE, PAR_COMMERCIAL)).param("format", "xlsx")
                .param(ANNEE, AN).with(connecte(chef));
        MockHttpServletResponse reponse = appels.exporter(requete).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        String texte = texteDuClasseur(reponse.getContentAsByteArray());
        assertThat(texte).contains(jeton("alpha")).doesNotContain(jeton("beta"));
    }

    @Test
    void r_v6_trancheIdDuProjetB_pourLeChef_aucuneColonneDeB() throws Exception {
        List<Map<String, Object>> sections = n21(chef, tb);

        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(colonnes(s)).isNotEmpty()
                .noneMatch(c -> String.valueOf(c.get("libelle")).contains(jeton("beta"))));
    }

    /** Texte de toutes les cellules de toutes les feuilles. */
    private static String texteDuClasseur(byte[] contenu) throws IOException {
        StringBuilder texte = new StringBuilder();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            for (Sheet feuille : classeur) {
                for (Row ligne : feuille) {
                    for (Cell cellule : ligne) {
                        texte.append(cellule).append('\n');
                    }
                }
            }
        }
        return texte.toString();
    }

    // ------------------------------------------------------------------ R2b : n° 9, 10, 22, 24, 27

    private static final String AN_24 = "2025";

    /** Sections d'un rapport R2b ; le n° 24 reçoit {@code annee} = 2025. */
    private List<Map<String, Object>> r2b(String code, AdmUser qui, CfgTranche... tranches) throws Exception {
        if (TAUX_VENTE_STOCK.equals(code)) {
            return appels.sections(demande(code, connecte(qui), List.of(tranches), ANNEE, AN_24));
        }
        return appels.sections(demande(code, connecte(qui), List.of(tranches)));
    }

    /** Σ |valeur| des colonnes chiffrées (NOMBRE, MONTANT, POURCENTAGE) hors colonnes d'axe {@code mois} / {@code annee}. */
    private static BigDecimal chiffresHorsAxe(Map<String, Object> section) {
        BigDecimal somme = BigDecimal.ZERO;
        for (Map<String, Object> c : colonnes(section)) {
            boolean chiffree = List.of("NOMBRE", "MONTANT", "POURCENTAGE").contains(String.valueOf(c.get("type")));
            if (chiffree && !List.of("mois", ANNEE).contains(String.valueOf(c.get("nom")))) {
                for (Map<String, Object> l : lignes(section)) {
                    BigDecimal v = nombre(l.get(c.get("nom")));
                    somme = v == null ? somme : somme.add(v.abs());
                }
            }
        }
        return somme;
    }

    private void assertAucunChiffreR2b(AdmUser qui, CfgTranche tranche) throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);
        SoftAssertions soft = new SoftAssertions();
        for (String code : List.of(RYTHME_CONCRETISATIONS, RYTHME_COMPARAISON)) {
            List<Map<String, Object>> sections = r2b(code, qui, tranche);
            soft.assertThat(sections).as(code).isNotEmpty();
            for (Map<String, Object> s : sections) {
                soft.assertThat(lignes(s)).as("%s %s lignes", code, s.get("id")).isEmpty();
                soft.assertThat(colonnesTranches(s)).as("%s %s colonnes", code, s.get("id")).isEmpty();
            }
        }
        soft.assertThat(lignes(section(r2b(CONCRET_LIBRES, qui, tranche), TRANCHES))).as(CONCRET_LIBRES).isEmpty();
        for (Map<String, Object> s : r2b(TAUX_CONCRET_ANNEE, qui, tranche)) {
            soft.assertThat(lignes(s)).as("n° 27 %s", s.get("id")).isEmpty();
        }
        List<Map<String, Object>> n24 = r2b(TAUX_VENTE_STOCK, qui, tranche);
        soft.assertThat(lignes(section(n24, TRANCHES))).as("n° 24 tranches").isEmpty();
        for (Map<String, Object> s : n24) {
            soft.assertThat(chiffresHorsAxe(s)).as("n° 24 %s", s.get("id")).isEqualByComparingTo("0");
        }
        soft.assertAll();
    }

    @Test
    void r_v1_r2b_commercialAffecteAuProjetA_resultatEgalAuResultatDeTASeule_pourLesCinqCodes() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);

        SoftAssertions soft = new SoftAssertions();
        for (String code : CODES_R2B) {
            soft.assertThat(r2b(code, a)).as(code).isEqualTo(r2b(code, direction, ta));
        }
        Map<String, Object> concretisations = section(r2b(RYTHME_CONCRETISATIONS, a), CONCRETISATIONS);
        soft.assertThat(colonnesTranches(concretisations)).containsExactly(colonneDe(concretisations, ta));
        soft.assertThat(lignes(section(r2b(CONCRET_LIBRES, a), TRANCHES))).singleElement()
                .satisfies(l -> assertThat(l).containsEntry(TRANCHE, ta.getCode()));
        soft.assertThat(lignes(section(r2b(TAUX_CONCRET_ANNEE, a), TRANCHES))).singleElement()
                .satisfies(l -> assertThat(String.valueOf(l.get(TRANCHE))).contains(ta.getCode()));
        Map<String, Object> stock = section(r2b(TAUX_VENTE_STOCK, a), TRANCHES);
        soft.assertThat(lignes(stock)).singleElement()
                .satisfies(l -> assertThat(String.valueOf(l.get(TRANCHE))).contains(ta.getCode()));
        soft.assertThat(total(stock, "ventesAnnee")).as("3 concrétisations de TA en 2025").isEqualByComparingTo("3");
        soft.assertAll();
    }

    @Test
    void r_v2_r2b_trancheDUneEntiteInactive_absenteDesLignesColonnesDenominateursEtTotaux() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);

        Map<String, Object> concretisations = section(r2b(RYTHME_CONCRETISATIONS, direction, ta, tb, ti),
                CONCRETISATIONS);
        Map<String, Object> encaissements = section(r2b(RYTHME_CONCRETISATIONS, direction, ta, tb, ti), ENCAISSEMENTS);
        Map<String, Object> comparaison = section(r2b(RYTHME_COMPARAISON, direction, ta, tb, ti), CONCRETISATIONS);
        Map<String, Object> libres = section(r2b(CONCRET_LIBRES, direction, ta, tb, ti), TRANCHES);
        Map<String, Object> stock = section(r2b(TAUX_VENTE_STOCK, direction, ta, tb, ti), TRANCHES);
        Map<String, Object> annees = section(r2b(TAUX_CONCRET_ANNEE, direction, ta, tb, ti), TRANCHES);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonnesTranches(concretisations)).containsExactly(colonneDe(concretisations, ta),
                colonneDe(concretisations, tb));
        soft.assertThat(colonnesTranches(comparaison)).hasSize(2);
        soft.assertThat(total(encaissements, ENSEMBLE)).as("8 500 ÷ 4 800 000, pas 11 500 ÷ 6 000 000")
                .isEqualByComparingTo("0.18");
        soft.assertThat(lignes(libres)).extracting(l -> l.get(TRANCHE)).containsExactly(ta.getCode(), tb.getCode());
        soft.assertThat(total(libres, "nbArticles")).isEqualByComparingTo("4");
        soft.assertThat(lignesContenant(stock, TRANCHE, ti.getCode())).isEmpty();
        soft.assertThat(total(stock, "ventesAnnee")).isEqualByComparingTo("4");
        soft.assertThat(lignesContenant(annees, TRANCHE, ti.getCode())).isEmpty();
        soft.assertThat(lignes(annees)).hasSize(2);
        soft.assertAll();
    }

    @Test
    void r_v2_r2b_seuleTrancheDemandeeInactive_aucunChiffre_surLesCinqRapports() throws Exception {
        assertAucunChiffreR2b(direction, ti);
    }

    @Test
    void r_v3_r2b_trancheIdHorsPerimetre_200_aucunChiffre_surLesCinqRapports() throws Exception {
        assertAucunChiffreR2b(a, tb);
    }

    /** R-V7 : une tranche invisible du même projet (hors vente) et une tranche non demandée ne pèsent sur aucun dénominateur. */
    @Test
    void r_v7_n9_denominateurs_neLisentQueLesTranchesRetenues() throws Exception {
        RapportsRythmeSupport.installerR2b(rapports);
        Jeu jeu = fixture.jeu("VR7");
        CfgTranche horsVente = ev.tranche("HV-" + jeu.prefixe(), projetA, false);
        for (int i = 0; i < 10; i++) {
            jeu.articleLibre(horsVente, "100000000");
        }
        jeu.encaisser(jeu.dater(jeu.dossier(horsVente, jeu.statuts().concretisation()), LocalDate.of(2025, 3, 3), null,
                null, null), "9000000", LocalDate.of(2025, 4, 4));
        rapports.relire();

        List<Map<String, Object>> deA = r2b(RYTHME_CONCRETISATIONS, a);
        List<Map<String, Object>> taSeule = r2b(RYTHME_CONCRETISATIONS, direction, ta);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonnesTranches(section(deA, CONCRETISATIONS))).hasSize(1);
        soft.assertThat(total(section(deA, CONCRETISATIONS), ENSEMBLE)).as("3 ÷ 3 articles de TA")
                .isEqualByComparingTo("100");
        soft.assertThat(total(section(deA, ENCAISSEMENTS), ENSEMBLE)).as("1 500 ÷ 3 600 000")
                .isEqualByComparingTo("0.04");
        soft.assertThat(total(section(taSeule, ENCAISSEMENTS), ENSEMBLE)).as("TB non demandée : pas 8 500 ÷ 4 800 000")
                .isEqualByComparingTo("0.04");
        soft.assertAll();
    }

    @Test
    void r_v1_trancheIdsVisiblesEtHorsPerimetreMeles_commercialA_neLitQueSonPerimetre_surLeN23() throws Exception {
        Map<String, Object> s = n23(a, ta, tb, ti);

        assertThat(lignes(s)).singleElement()
                .satisfies(l -> assertThat(String.valueOf(l.get(PROJET))).contains(projetA.getCode()));
        assertThat(total(s, ENCAISSEMENTS)).isEqualByComparingTo("1500");
    }
}
