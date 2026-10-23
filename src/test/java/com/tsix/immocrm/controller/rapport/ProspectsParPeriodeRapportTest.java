package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.lignesContenant;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.totauxEgauxSommeDesLignes;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typeParametre;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typesGraphiques;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.valeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 25 PROSPECTS PAR PÉRIODE (FEATURE-tdb-vente-rapports-commercial.md §7, décisions 5 et 9, §10). Période du lundi
 * 10/03/2025 au dimanche 16/03/2025 (semaine ISO 11), année 2025 ; tiers par {@code UserCreateDateTime} :
 * <pre>
 *   A : PRO 11/03 ×2, CLI 14/03, autre type 12/03 (exclu), PRO 20/03 (hors période), PRO 01/06/2022, PRO 01/01/2020
 *   B : PRO 12/03                       non affecté : PRO 11/03
 * </pre>
 * A et B sont Commercial affectés au projet P ; A a un rôle de rapports sans restriction (non exempté du périmètre).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProspectsParPeriodeRapportTest {

    private static final String CODE = "ComTdbProspectsPerCommercial";
    private static final String DU = "2025-03-10";
    private static final String AU = "2025-03-16";
    private static final String COMMERCIAL = "commercial";
    private static final String TOTAL = "total";
    private static final String PAR_SEMAINE = "par-semaine";
    private static final String PAR_MOIS = "par-mois";
    private static final String PAR_ANNEE = "par-annee";
    private static final String NON_AFFECTE = "Non affecté";
    private static final String PROSPECT = "PRO";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;

    private AppelsRapports appels;
    private String prefixe;
    private AdmUser commercialA;
    private AdmUser direction;

    private static LocalDateTime le(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("R25");
        fixture.tables();
        fixture.rapportVente(CODE, "PROSPECTS PAR PERIODE", "Suivi GLOBAL", 2050);
        AdmRole libre = fixture.role("Libre " + prefixe, false);
        CfgProject p = rc.projet("P-" + prefixe, true);

        commercialA = rc.compte(jeton("a"), new CfgProject[] {p}, libre, fixture.role(RolesEquipe.COMMERCIAL, true));
        AdmUser commercialB = rc.compte(jeton("b"), new CfgProject[] {p}, fixture.role(RolesEquipe.COMMERCIAL, true));
        direction = rc.compte(jeton("dg"), new CfgProject[0], fixture.role("Direction Générale", false));

        rc.tiers(PROSPECT, commercialA, le(2025, 3, 11));
        rc.tiers(PROSPECT, commercialA, le(2025, 3, 11));
        rc.tiers("CLI", commercialA, le(2025, 3, 14));
        rc.tiers("AUT-" + prefixe, commercialA, le(2025, 3, 12));
        rc.tiers(PROSPECT, commercialA, le(2025, 3, 20));
        rc.tiers(PROSPECT, commercialA, le(2022, 6, 1));
        rc.tiers(PROSPECT, commercialA, le(2020, 1, 1));
        rc.tiers(PROSPECT, commercialB, le(2025, 3, 12));
        rc.tiers(PROSPECT, null, le(2025, 3, 11));
        rc.relire();
    }

    private String jeton(String qui) {
        return "r25." + qui + "." + prefixe;
    }

    private MockHttpServletRequestBuilder rapport(AdmUser qui) {
        return get(resultat(VENTE, CODE)).param("du", DU).param("au", AU).param("annee", "2025").with(connecte(qui));
    }

    // ------------------------------------------------------------------ descripteur, droits

    /** Pas de filtre tranche : un tiers n'a pas de tranche. */
    @Test
    void r25_descripteur_periodeEtAnnee_sansTranche() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(direction)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(typeParametre(corps, "du")).isEqualTo("DATE");
        soft.assertThat(typeParametre(corps, "au")).isEqualTo("DATE");
        soft.assertThat(typeParametre(corps, "annee")).isEqualTo("ANNEE");
        soft.assertThat(typeParametre(corps, "trancheId")).isNull();
        soft.assertAll();
    }

    @Test
    void transversal_roleNonLie_403() throws Exception {
        AdmUser restreint = rc.compte(jeton("r"), new CfgProject[0], fixture.role("Restreint " + prefixe, true));
        fixture.relire();

        assertThat(appels.statut(rapport(restreint))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ nominal

    /** Q5 : semaine ISO 8601 ; colonne s11 libellée « S11 ». */
    @Test
    void r25_parSemaine_isoS11() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction)), PAR_SEMAINE);

        assertThat(colonnes(s)).anySatisfy(c -> assertThat(c).containsEntry("nom", "s11").containsEntry("libelle", "S11"));
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(s, COMMERCIAL, jeton("a"), "s11")).isEqualByComparingTo("3");
        soft.assertThat(valeur(s, COMMERCIAL, jeton("a"), TOTAL)).isEqualByComparingTo("3");
        soft.assertThat(valeur(s, COMMERCIAL, jeton("b"), "s11")).isEqualByComparingTo("1");
        soft.assertAll();
    }

    /** Prospects et clients comptés (codes PRO / CLI), un tiers d'un autre type non ; filtre sur [du ; au]. */
    @Test
    void r25_parMois_prospectsEtClientsSeulement() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction)), PAR_MOIS);

        assertThat(valeur(s, COMMERCIAL, jeton("a"), TOTAL)).isEqualByComparingTo("3");
    }

    /** Cinq années annee-4…annee, sans filtre de date : 2025 (4) + 2022 (1) ; 2020 hors fenêtre. */
    @Test
    void r25_parAnnee_cinqAnnees_sansFiltreDeDate() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction)), PAR_ANNEE);

        List<Object> periodes = colonnes(s).stream().map(c -> c.get("nom"))
                .filter(n -> !COMMERCIAL.equals(n) && !TOTAL.equals(n)).toList();
        assertThat(periodes).hasSize(5);
        assertThat(valeur(s, COMMERCIAL, jeton("a"), TOTAL)).isEqualByComparingTo("5");
    }

    @Test
    void r25_troisSectionsGlobales_unGraphiqueColonnesChacune() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction));

        SoftAssertions soft = new SoftAssertions();
        for (String id : List.of("global-semaine", "global-mois", "global-annee")) {
            soft.assertThat(typesGraphiques(section(sections, id))).as(id).containsExactly("COLONNES");
        }
        soft.assertAll();
    }

    @Test
    void r25_totaux_sommeServeurDesLignes() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction));

        assertThat(sections).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        totauxEgauxSommeDesLignes(soft, sections);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ RV-R3b : commerciaux visibles

    @Test
    void rv_nonAffecte_reserveAuxExemptes() throws Exception {
        Map<String, Object> exempte = section(appels.sections(rapport(direction)), PAR_SEMAINE);
        Map<String, Object> commercial = section(appels.sections(rapport(commercialA)), PAR_SEMAINE);

        assertThat(lignesContenant(exempte, COMMERCIAL, NON_AFFECTE)).hasSize(1);
        assertThat(lignesContenant(commercial, COMMERCIAL, NON_AFFECTE)).isEmpty();
    }

    @Test
    void rv_nonExempte_neVoitQueSesCommerciaux() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(commercialA)), PAR_SEMAINE);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("a"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("b"))).isEmpty();
    }

    @Test
    void rv_exempte_voitTousLesCommerciaux() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction)), PAR_SEMAINE);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("a"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("b"))).hasSize(1);
    }
}
