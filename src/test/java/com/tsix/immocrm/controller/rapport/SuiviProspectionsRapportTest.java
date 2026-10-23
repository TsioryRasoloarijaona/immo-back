package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.AUTRES;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.NON_DEFINIE;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.entier;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.repartition;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.totauxEgauxSommeDesLignes;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typeParametre;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typesGraphiques;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 19 SUIVI DES PROSPECTIONS (FEATURE-tdb-vente-rapports-commercial.md §5, §2, §10). Année 2076 : aucune donnée hors
 * fixture. Tranche T (projet P, actif), T2 (projet P2, hors périmètre du commercial U), TI (société inactive) :
 * <pre>
 *   T  : 60 prospections — source S1 30, S2 29, S3 1 (1,7 % : « AUTRES &lt; 2% ») ; nature N1 40, N2 20 ;
 *        type TA 40, TB 20 ; 30 en mars, 30 en mai
 *        actions Prospection : 3 en avril (résultat R1, type ATA, auteur A), 2 en juin (R2, ATB, auteur B)
 *   T2 : 3 prospections (S1)        TI : 5 prospections (S1)        sans tranche : 1 prospection (S1)
 *   une action Prospection sans prospection (avril, A)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviProspectionsRapportTest {

    private static final String CODE = "ComTdbProspectionState";
    private static final int ANNEE = 2076;
    private static final String TRANCHE_ID = "trancheId";
    private static final String TYPE = "prospections-type";
    private static final String SOURCE = "prospections-source";
    private static final String NATURE = "prospections-nature";
    private static final String MOIS = "prospections-mois";
    private static final String RESULTAT = "actions-resultat";
    private static final String TYPE_ACTION = "actions-type";
    private static final String UTILISATEUR = "actions-utilisateur";
    private static final String MOIS_ACTIONS = "actions-mois";
    private static final String NOMBRE = "nombre";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final List<String> REPARTITIONS = List.of(TYPE, SOURCE, NATURE, RESULTAT, TYPE_ACTION, UTILISATEUR);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole libre;
    private CfgProject projet;
    private CfgTranche tranche;
    private CfgTranche trancheP2;
    private CfgTranche trancheInactive;
    private AdmUser admin;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private ComProspectionOrigin s1;

    private static LocalDateTime le(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("R19");
        fixture.tables();
        fixture.rapportVente(CODE, "SUIVI DES PROSPECTIONS", "", 1700);
        libre = fixture.role("Libre " + prefixe, false);

        projet = rc.projet("P-" + prefixe, true);
        tranche = rc.tranche("T-" + prefixe, projet);
        trancheP2 = rc.tranche("T2-" + prefixe, rc.projet("P2-" + prefixe, true));
        trancheInactive = rc.tranche("TI-" + prefixe, rc.projet("PI-" + prefixe, false));

        admin = rc.compte("r19.adm." + prefixe, new CfgProject[0], fixture.role(ADMINISTRATEUR, false));
        commercialA = rc.compte("r19.a." + prefixe, new CfgProject[] {projet}, fixture.role(RolesEquipe.COMMERCIAL, true));
        commercialB = rc.compte("r19.b." + prefixe, new CfgProject[] {projet}, fixture.role(RolesEquipe.COMMERCIAL, true));

        s1 = rc.origine("S1-" + prefixe);
        ComProspectionOrigin s2 = rc.origine("S2-" + prefixe);
        ComProspectionOrigin s3 = rc.origine("S3-" + prefixe);
        ComProspectionKind n1 = rc.nature("N1-" + prefixe);
        ComProspectionKind n2 = rc.nature("N2-" + prefixe);
        ComActionMessageType ta = rc.typeAction("TA-" + prefixe);
        ComActionMessageType tb = rc.typeAction("TB-" + prefixe);

        ComProspection[] p = new ComProspection[60];
        for (int i = 0; i < 60; i++) {
            ComProspectionOrigin source = s3;
            if (i < 30) {
                source = s1;
            } else if (i < 59) {
                source = s2;
            }
            p[i] = rc.prospection(tranche, i % 2 == 0 ? le(3, 10) : le(5, 10), i < 40 ? ta : tb, source,
                    i < 40 ? n1 : n2, null);
        }
        for (int i = 0; i < 3; i++) {
            rc.prospection(trancheP2, le(3, 11), ta, s1, n1, null);
        }
        for (int i = 0; i < 5; i++) {
            rc.prospection(trancheInactive, le(3, 12), ta, s1, n1, null);
        }
        rc.prospection(null, le(3, 13), ta, s1, n1, null);

        ComActionMessageSubCategory r1 = rc.resultat("R1-" + prefixe);
        ComActionMessageSubCategory r2 = rc.resultat("R2-" + prefixe);
        ComActionMessageType ata = rc.typeAction("ATA-" + prefixe);
        ComActionMessageType atb = rc.typeAction("ATB-" + prefixe);
        for (int i = 0; i < 3; i++) {
            rc.interaction(commercialA, p[i], ata, r1, le(4, 15));
        }
        for (int i = 3; i < 5; i++) {
            rc.interaction(commercialB, p[i], atb, r2, le(6, 15));
        }
        rc.interaction(commercialA, null, ata, r1, le(4, 16));
        rc.relire();
    }

    private MockHttpServletRequestBuilder rapport(AdmUser qui, int annee, CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, CODE)).param("annee", String.valueOf(annee))
                .with(connecte(qui, ADMINISTRATEUR));
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private List<Map<String, Object>> sur(CfgTranche... tranches) throws Exception {
        return appels.sections(rapport(admin, ANNEE, tranches));
    }

    private static List<BigDecimal> nombresParMois(Map<String, Object> section) {
        return lignes(section).stream().map(l -> nombre(l.get(NOMBRE))).toList();
    }

    // ------------------------------------------------------------------ descripteur, droits, graphiques

    @Test
    void r19_descripteur_annee_tranches() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(admin)));

        assertThat(typeParametre(corps, "annee")).isEqualTo("ANNEE");
        assertThat(typeParametre(corps, TRANCHE_ID)).isEqualTo("TRANCHES");
    }

    @Test
    void transversal_roleNonLie_403() throws Exception {
        AdmUser restreint = rc.compte("r19.r." + prefixe, new CfgProject[0], fixture.role("Restreint " + prefixe, true));
        fixture.relire();

        assertThat(appels.statut(rapport(restreint, ANNEE, tranche))).isEqualTo(403);
    }

    @Test
    void r19_huitSections_sixCamemberts() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        SoftAssertions soft = new SoftAssertions();
        for (String id : REPARTITIONS) {
            soft.assertThat(typesGraphiques(section(sections, id))).as(id).containsExactly("SECTEURS");
        }
        soft.assertThat(lignes(section(sections, MOIS))).hasSize(12);
        soft.assertThat(lignes(section(sections, MOIS_ACTIONS))).hasSize(12);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ nominal chiffré

    /** RF-R3 : part &lt; 2 % fondue en « AUTRES &lt; 2% », en dernier ; somme conservée ; tri par nombre décroissant. */
    @Test
    void rf_source_moinsDe2Pourcent_autres_sommeConservee() throws Exception {
        Map<String, Object> source = section(sur(tranche), SOURCE);

        assertThat(repartition(source)).containsExactly(entry("S1-" + prefixe, 30L),
                entry("S2-" + prefixe, 29L), entry(AUTRES, 1L));
        assertThat(somme(lignes(source), NOMBRE)).isEqualByComparingTo("60");
    }

    @Test
    void rf_part_pourcentage0a100_uneDecimale() throws Exception {
        Map<String, Object> source = section(sur(tranche), SOURCE);

        assertThat(nombre(lignes(source).get(0).get("part")).doubleValue()).isCloseTo(50.0, within(0.05));
        assertThat(nombre(lignes(source).get(1).get("part")).doubleValue()).isCloseTo(48.3, within(0.05));
    }

    @Test
    void r19_typeEtNature_parLibelle() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        assertThat(repartition(section(sections, TYPE))).containsExactly(
                entry("TA-" + prefixe, 40L), entry("TB-" + prefixe, 20L));
        assertThat(repartition(section(sections, NATURE))).containsExactly(
                entry("N1-" + prefixe, 40L), entry("N2-" + prefixe, 20L));
    }

    @Test
    void r19_prospectionsParMois_douzeLignes_zeroSansDonnee() throws Exception {
        List<BigDecimal> parMois = nombresParMois(section(sur(tranche), MOIS));

        assertThat(parMois).hasSize(12);
        assertThat(parMois.get(2)).isEqualByComparingTo("30");
        assertThat(parMois.get(4)).isEqualByComparingTo("30");
        assertThat(parMois.get(0)).isEqualByComparingTo("0");
    }

    /** Actions de catégorie Prospection rattachées à une prospection ; une action sans prospection est absente. */
    @Test
    void r19_actions_resultatTypeUtilisateurMois() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(repartition(section(sections, RESULTAT))).containsExactly(
                entry("R1-" + prefixe, 3L), entry("R2-" + prefixe, 2L));
        soft.assertThat(repartition(section(sections, TYPE_ACTION))).containsExactly(
                entry("ATA-" + prefixe, 3L), entry("ATB-" + prefixe, 2L));
        soft.assertThat(repartition(section(sections, UTILISATEUR))).containsExactly(
                entry(commercialA.getDescription(), 3L), entry(commercialB.getDescription(), 2L));
        List<BigDecimal> parMois = nombresParMois(section(sections, MOIS_ACTIONS));
        soft.assertThat(parMois).hasSize(12);
        soft.assertThat(parMois.get(3)).isEqualByComparingTo("3");
        soft.assertThat(parMois.get(5)).isEqualByComparingTo("2");
        soft.assertAll();
    }

    @Test
    void r19_actionSansProspection_absenteMemeSansTranche() throws Exception {
        Map<String, Object> types = section(appels.sections(rapport(admin, ANNEE)), TYPE_ACTION);

        assertThat(somme(lignes(types), NOMBRE)).isEqualByComparingTo("5");
    }

    @Test
    void r19_valeurNulle_nonDefinie() throws Exception {
        CfgTranche tn = rc.tranche("TN-" + prefixe, projet);
        rc.prospection(tn, le(3, 10), null, s1, null, null);
        rc.prospection(tn, le(3, 10), null, s1, null, null);
        rc.prospection(tn, le(3, 10), null, null, null, null);
        rc.relire();

        Map<String, Object> source = section(sur(tn), SOURCE);
        assertThat(repartition(source)).containsExactly(entry("S1-" + prefixe, 2L), entry(NON_DEFINIE, 1L));
    }

    @Test
    void r19_anneeSansDonnee_sectionsVides() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(admin, ANNEE + 1, tranche));

        SoftAssertions soft = new SoftAssertions();
        for (String id : REPARTITIONS) {
            soft.assertThat(lignes(section(sections, id))).as(id).isEmpty();
        }
        soft.assertThat(somme(lignes(section(sections, MOIS)), NOMBRE)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r19_totaux_sommeServeurDesLignes() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        assertThat(sections).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        totauxEgauxSommeDesLignes(soft, sections);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ RE-R3 : égalité avec les écrans (admin)

    @Test
    void re_sourceEtNature_egalesAuxGroupesDesProspections() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(sansRegroupements(section(sections, SOURCE))).isNotEmpty()
                .isSubsetOf(groupesProspections("origine").entrySet());
        soft.assertThat(sansRegroupements(section(sections, NATURE))).isNotEmpty()
                .isSubsetOf(groupesProspections("etape").entrySet());
        soft.assertAll();
    }

    @Test
    void re_mois_egalAuxGroupesMensuelsDesProspections() throws Exception {
        List<BigDecimal> parMois = nombresParMois(section(sur(tranche), MOIS));
        Map<Integer, BigDecimal> ecran = parMoisEcran(appels.corps(get("/api/prospections/groupes")
                .param("groupBy", "dateDocument").param("granularite", "MOIS")
                .param(TRANCHE_ID, tranche.getId().toString()).param("dateDebut", ANNEE + "-01-01")
                .param("dateFin", ANNEE + "-12-31").param("size", "200").with(connecte(admin, ADMINISTRATEUR))));

        assertThat(ecran).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        ecran.forEach((m, n) -> soft.assertThat(parMois.get(m - 1)).as("mois %d", m).isEqualByComparingTo(n));
        soft.assertAll();
    }

    /** Q3 : le rapport compte toutes les actions du périmètre ; égalité avec l'écran vu par un administrateur. */
    @Test
    void re_actions_egalesAuxGroupesDuSuiviDesActions() throws Exception {
        List<Map<String, Object>> sections = sur(tranche);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(sansRegroupements(section(sections, RESULTAT))).isNotEmpty()
                .isSubsetOf(groupesActions("resultat").entrySet());
        soft.assertThat(sansRegroupements(section(sections, TYPE_ACTION))).isNotEmpty()
                .isSubsetOf(groupesActions("typeAction").entrySet());
        soft.assertThat(sansRegroupements(section(sections, UTILISATEUR))).isNotEmpty()
                .isSubsetOf(groupesActions("utilisateur").entrySet());
        soft.assertAll();
    }

    private static List<Map.Entry<String, Long>> sansRegroupements(Map<String, Object> section) {
        return repartition(section).entrySet().stream()
                .filter(e -> !AUTRES.equals(e.getKey()) && !NON_DEFINIE.equals(e.getKey())).toList();
    }

    private Map<String, Long> groupesProspections(String groupBy) throws Exception {
        return parLibelle(appels.corps(get("/api/prospections/groupes").param("groupBy", groupBy)
                .param(TRANCHE_ID, tranche.getId().toString()).param("dateDebut", ANNEE + "-01-01")
                .param("dateFin", ANNEE + "-12-31").param("size", "200").with(connecte(admin, ADMINISTRATEUR))));
    }

    private Map<String, Long> groupesActions(String groupBy) throws Exception {
        return parLibelle(appels.corps(get("/api/suivi-actions/groupes").param("groupBy", groupBy)
                .param("categorieId", InteractionKpi.CATEGORIE_PROSPECTION)
                .param(TRANCHE_ID, tranche.getId().toString()).param("dateDebut", ANNEE + "-01-01T00:00:00")
                .param("dateFin", (ANNEE + 1) + "-01-01T00:00:00").param("size", "200")
                .with(connecte(admin, ADMINISTRATEUR))));
    }

    private static Map<String, Long> parLibelle(String corps) {
        List<Map<String, Object>> groupes = JsonPath.read(corps, "$.content");
        Map<String, Long> parLibelle = new LinkedHashMap<>();
        groupes.forEach(g -> parLibelle.put(String.valueOf(g.get("libelle")), entier(g.get(NOMBRE))));
        return parLibelle;
    }

    /** Clé de groupe date = entier aaaamm. */
    private static Map<Integer, BigDecimal> parMoisEcran(String corps) {
        List<Map<String, Object>> groupes = JsonPath.read(corps, "$.content");
        Map<Integer, BigDecimal> parMois = new LinkedHashMap<>();
        groupes.forEach(g -> parMois.put(Integer.parseInt(String.valueOf(g.get("cle"))) % 100,
                nombre(g.get(NOMBRE))));
        return parMois;
    }

    // ------------------------------------------------------------------ RV-R3a : tranches

    @Test
    void rv_trancheSocieteInactive_aucunChiffre() throws Exception {
        Map<String, Object> source = section(sur(trancheInactive), SOURCE);

        assertThat(lignes(source)).isEmpty();
    }

    /** Sans trancheId : T (60) + T2 (3) + sans tranche (1), jamais TI ; avec trancheId = T : 60, sans tranche exclue. */
    @Test
    void rv_prospectionSansTranche_compteeSeulementSansTrancheId() throws Exception {
        Map<String, Object> toutes = section(appels.sections(rapport(admin, ANNEE)), SOURCE);
        Map<String, Object> surT = section(sur(tranche), SOURCE);

        assertThat(somme(lignes(toutes), NOMBRE)).isEqualByComparingTo("64");
        assertThat(somme(lignes(surT), NOMBRE)).isEqualByComparingTo("60");
    }

    @Test
    void rv_projetHorsPerimetre_aucunChiffre() throws Exception {
        AdmUser u = rc.compte("r19.u." + prefixe, new CfgProject[] {projet}, libre,
                fixture.role(RolesEquipe.COMMERCIAL, true));
        fixture.relire();

        Map<String, Object> source = section(appels.sections(get(resultat(VENTE, CODE))
                .param("annee", String.valueOf(ANNEE)).param(TRANCHE_ID, trancheP2.getId().toString())
                .with(connecte(u))), SOURCE);
        assertThat(lignes(source)).isEmpty();
    }
}
