package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.graphiques;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.lignesContenant;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.totauxEgauxSommeDesLignes;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typeParametre;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.valeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 17 SUIVI DES COMMERCIAUX (FEATURE-tdb-vente-rapports-commercial.md §4, §2 couche commerciaux, §10). Année 2025,
 * tranche T (projet P) ; A, B et le chef affectés à P, C affecté à P2 seulement (hors équipe du chef) :
 * <pre>
 *   A : prospections 05/03, 06/03, 02/06 ; Visite 12/03 (sa prospection), Visite 10/06 sur la prospection de B
 *       (auteur A) ; visite de vente 03/07 ; Téléphone Sortant 13/03, 14/03 + Mail Sortant 15/03 ;
 *       Téléphone entrant 11/06 + Mail entrant 12/06
 *   B : prospection 07/03 ; Téléphone Sortant 02/04          C : prospection 08/03 sur T ; prospection 09/03 sur T2
 *   sans auteur : Téléphone Sortant 16/03 sur la prospection de A
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxRapportTest {

    private static final String CODE = "ComTdbCommercialState";
    private static final int ANNEE = 2025;
    private static final String TRANCHE_ID = "trancheId";
    private static final String COMMERCIAL_ID = "commercialId";
    private static final String COMMERCIAL = "commercial";
    private static final String TOTAL = "total";
    private static final String PROSPECTIONS = "prospections";
    private static final String VISITES = "visites";
    private static final String ENTRANTES = "communications-entrantes";
    private static final String SORTANTES = "communications-sortantes";
    private static final String EVOLUTION = "evolution";
    private static final String MARS = "m03";
    private static final String JUIN = "m06";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;
    @Autowired private FixtureSuiviCommerciaux suivi;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole libre;
    private CfgTranche tranche;
    private CfgTranche trancheP2;
    private CfgTranche trancheInactive;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private AdmUser commercialC;
    private AdmUser chef;
    private AdmUser direction;

    private static LocalDateTime le(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour).atTime(10, 0);
    }

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("R17");
        fixture.tables();
        fixture.rapportVente(CODE, "SUIVI DES COMMERCIAUX", "G4", 1500);
        libre = fixture.role("Libre " + prefixe, false);

        CfgProject p = rc.projet("P-" + prefixe, true);
        CfgProject p2 = rc.projet("P2-" + prefixe, true);
        tranche = rc.tranche("T-" + prefixe, p);
        trancheP2 = rc.tranche("T2-" + prefixe, p2);
        trancheInactive = rc.tranche("TI-" + prefixe, rc.projet("PI-" + prefixe, false));

        commercialA = rc.compte(jeton("a"), new CfgProject[] {p}, libre, fixture.role(RolesEquipe.COMMERCIAL, true));
        commercialB = rc.compte(jeton("b"), new CfgProject[] {p}, fixture.role(RolesEquipe.COMMERCIAL, true));
        commercialC = rc.compte(jeton("c"), new CfgProject[] {p2}, fixture.role(RolesEquipe.COMMERCIAL, true));
        chef = rc.compte(jeton("chef"), new CfgProject[] {p}, libre, fixture.role(RolesEquipe.CHEF, true));
        direction = rc.compte(jeton("dg"), new CfgProject[0], fixture.role("Direction Générale", false));

        ComActionMessageType visite = suivi.visite();
        ComActionMessageType sortant = suivi.appelSortant();
        ComActionMessageType entrant = suivi.appelEntrant();
        ComActionMessageType mailSortant = suivi.type("Mail Sortant");
        ComActionMessageType mailEntrant = suivi.type("Mail entrant");

        ComProspection pa1 = rc.prospection(tranche, commercialA, le(3, 5));
        ComProspection pa2 = rc.prospection(tranche, commercialA, le(3, 6));
        rc.prospection(tranche, commercialA, le(6, 2));
        ComProspection pb = rc.prospection(tranche, commercialB, le(3, 7));
        ComProspection pc = rc.prospection(tranche, commercialC, le(3, 8));
        rc.prospection(trancheP2, commercialC, le(3, 9));
        rc.prospection(trancheInactive, commercialA, le(3, 9));

        rc.interaction(commercialA, pa1, visite, null, le(3, 12));
        rc.interaction(commercialA, pb, visite, null, le(6, 10));
        ComFolder vente = rc.vente(prefixe + "-V1", commercialA, tranche, le(1, 15));
        rc.visiteVente(commercialA, vente, visite, le(7, 3));
        rc.interaction(commercialA, pa1, sortant, null, le(3, 13));
        rc.interaction(commercialA, pa1, sortant, null, le(3, 14));
        rc.interaction(commercialA, pa1, mailSortant, null, le(3, 15));
        rc.interaction(commercialA, pa2, entrant, null, le(6, 11));
        rc.interaction(commercialA, pa2, mailEntrant, null, le(6, 12));
        rc.interaction(commercialB, pb, sortant, null, le(4, 2));
        rc.interaction(commercialC, pc, sortant, null, le(3, 20));
        rc.interactionSansAuteur(pa1, sortant, le(3, 16));
        rc.relire();
    }

    private String jeton(String qui) {
        return "r17." + qui + "." + prefixe;
    }

    private MockHttpServletRequestBuilder rapport(AdmUser qui, CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, CODE)).param("annee", String.valueOf(ANNEE))
                .with(connecte(qui));
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private BigDecimal cellule(Map<String, Object> section, String qui, String colonne) {
        return valeur(section, COMMERCIAL, jeton(qui), colonne);
    }

    private static String mois(int m) {
        return "m%02d".formatted(m);
    }

    // ------------------------------------------------------------------ descripteur, droits

    @Test
    void r17_descripteur_annee_tranches_commerciaux() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(direction)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(typeParametre(corps, "annee")).isEqualTo("ANNEE");
        soft.assertThat(typeParametre(corps, TRANCHE_ID)).isEqualTo("TRANCHES");
        soft.assertThat(typeParametre(corps, COMMERCIAL_ID)).isEqualTo("COMMERCIAUX");
        soft.assertAll();
    }

    @Test
    void transversal_roleNonLie_403() throws Exception {
        AdmUser restreint = rc.compte(jeton("restreint"), new CfgProject[0], fixture.role("Restreint " + prefixe, true));
        fixture.relire();

        assertThat(appels.statut(rapport(restreint, tranche))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ nominal chiffré

    @Test
    void r17_prospections_parCommercialAssigneEtMois() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, tranche)), PROSPECTIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, "a", MARS)).isEqualByComparingTo("2");
        soft.assertThat(cellule(s, "a", JUIN)).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, "a", TOTAL)).isEqualByComparingTo("3");
        soft.assertThat(cellule(s, "b", MARS)).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, "b", TOTAL)).isEqualByComparingTo("1");
        soft.assertAll();
    }

    /** Q2 : la visite suit l'auteur de l'interaction (visite de A sur la prospection de B), visites de vente comprises. */
    @Test
    void r17_visites_suiventLAuteur_venteComprise() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, tranche)), VISITES);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, "a", MARS)).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, "a", JUIN)).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, "a", "m07")).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, "a", TOTAL)).isEqualByComparingTo("3");
        soft.assertThat(cellule(s, "b", TOTAL)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r17_communications_telephoneEtMail_entrantesEtSortantes() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction, tranche));
        Map<String, Object> entrantes = section(sections, ENTRANTES);
        Map<String, Object> sortantes = section(sections, SORTANTES);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(entrantes, "a", JUIN)).isEqualByComparingTo("2");
        soft.assertThat(cellule(entrantes, "a", TOTAL)).isEqualByComparingTo("2");
        soft.assertThat(cellule(sortantes, "a", MARS)).isEqualByComparingTo("3");
        soft.assertThat(cellule(sortantes, "b", "m04")).isEqualByComparingTo("1");
        soft.assertAll();
    }

    /** §4 : interaction sans compte auteur absente des chiffres, signalée en avertissement. */
    @Test
    void r17_interactionSansAuteur_absente_avertissement() throws Exception {
        String corps = appels.corps(rapport(direction, tranche));
        Map<String, Object> sortantes = section(JsonPath.read(corps, "$.sections"), SORTANTES);
        List<Object> avertissements = JsonPath.read(corps, "$.avertissements");

        assertThat(cellule(sortantes, "a", MARS)).isEqualByComparingTo("3");
        assertThat(avertissements).isNotEmpty();
    }

    @Test
    void r17_colonnes_commercial_douzeMois_total() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, tranche)), PROSPECTIONS);

        List<Object> noms = AppelsRapports.colonnes(s).stream().map(c -> c.get("nom")).toList();
        assertThat(noms).containsExactly(COMMERCIAL, "m01", "m02", MARS, "m04", "m05", JUIN, "m07", "m08", "m09",
                "m10", "m11", "m12", TOTAL);
    }

    @Test
    void r17_totaux_sommeServeurDesLignes() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction, tranche));

        assertThat(sections).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        totauxEgauxSommeDesLignes(soft, sections);
        soft.assertAll();
    }

    /** Graphiques : section « evolution », 12 mois, deux graphiques en colonnes par mois. */
    @Test
    void r17_evolution_douzeMois_deuxGraphiquesColonnes() throws Exception {
        Map<String, Object> evolution = section(appels.sections(rapport(direction, tranche)), EVOLUTION);

        List<Map<String, Object>> lignes = lignes(evolution);
        assertThat(lignes).hasSize(12);
        // Mars : A 2 + B 1 + C 1 (exempté : tous les commerciaux de la tranche).
        assertThat(nombre(lignes.get(2).get(PROSPECTIONS))).isEqualByComparingTo("4");
        assertThat(nombre(lignes.get(5).get(VISITES))).isEqualByComparingTo("1");
        assertThat(graphiques(evolution)).hasSize(2)
                .allSatisfy(g -> assertThat(g).containsEntry("type", "COLONNES").containsEntry("colonneCategorie", "mois"));
    }

    // ------------------------------------------------------------------ égalité avec /suivi-commerciaux (décision 2)

    /** Un seul calcul : prospections et visites = /api/suivi-commerciaux/mensuel, pour chaque commercial et mois. */
    @Test
    void re_prospectionsEtVisites_egalesAuMensuelDeLEcran() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction, tranche));
        Map<String, Object> prospections = section(sections, PROSPECTIONS);
        Map<String, Object> visites = section(sections, VISITES);

        SoftAssertions soft = new SoftAssertions();
        for (int m : new int[] {3, 4, 6, 7}) {
            String corps = mensuel(m);
            for (AdmUser c : List.of(commercialA, commercialB)) {
                String qui = c == commercialA ? "a" : "b";
                soft.assertThat(cellule(prospections, qui, mois(m))).as("prospections %s m%d", qui, m)
                        .isEqualByComparingTo(champMensuel(corps, c, "nbProspections"));
                soft.assertThat(cellule(visites, qui, mois(m))).as("visites %s m%d", qui, m)
                        .isEqualByComparingTo(champMensuel(corps, c, "nbVisitesProspection")
                                .add(champMensuel(corps, c, "nbVisitesVente")));
            }
        }
        soft.assertAll();
    }

    /** Téléphones = appelsPasses / appelsRecus de /synthese sur le mois ; un mail s'y ajoute. */
    @Test
    void re_communications_egalesAuxAppelsDeLaSynthese_plusLesMails() throws Exception {
        List<Map<String, Object>> sections = appels.sections(rapport(direction, tranche));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(section(sections, SORTANTES), "a", MARS))
                .isEqualByComparingTo(champSynthese(3, commercialA, "appelsPasses").add(BigDecimal.ONE));
        soft.assertThat(cellule(section(sections, ENTRANTES), "a", JUIN))
                .isEqualByComparingTo(champSynthese(6, commercialA, "appelsRecus").add(BigDecimal.ONE));
        soft.assertThat(cellule(section(sections, SORTANTES), "b", "m04"))
                .isEqualByComparingTo(champSynthese(4, commercialB, "appelsPasses"));
        soft.assertAll();
    }

    private String mensuel(int m) throws Exception {
        return appels.corps(get("/api/suivi-commerciaux/mensuel").param("annee", String.valueOf(ANNEE))
                .param("mois", String.valueOf(m)).param("trancheIds", tranche.getId().toString())
                .param("commercialIds", cle(commercialA), cle(commercialB))
                .with(connecte(direction, "Direction Générale")));
    }

    private static BigDecimal champMensuel(String corps, AdmUser c, String champ) {
        List<Object> valeurs = JsonPath.read(corps, "$.mois.lignes[?(@.commercialCle=='" + cle(c) + "')]." + champ);
        return valeurs.isEmpty() || valeurs.get(0) == null ? BigDecimal.ZERO : nombre(valeurs.get(0));
    }

    private BigDecimal champSynthese(int m, AdmUser c, String champ) throws Exception {
        YearMonth ym = YearMonth.of(ANNEE, m);
        String corps = appels.corps(get("/api/suivi-commerciaux/synthese")
                .param("du", ym.atDay(1).toString()).param("au", ym.atEndOfMonth().toString())
                .param("commercialIds", cle(c)).with(connecte(direction, "Direction Générale")));
        List<Object> valeurs = JsonPath.read(corps,
                "$.totaux.parCommercial[?(@.commercialCle=='" + cle(c) + "')]." + champ);
        return valeurs.isEmpty() ? BigDecimal.ZERO : nombre(valeurs.get(0));
    }

    // ------------------------------------------------------------------ RV-R3b : commerciaux visibles

    @Test
    void rv_commercial_neVoitQueSaLigne() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(commercialA)), PROSPECTIONS);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("a"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("b"))).isEmpty();
        assertThat(lignesContenant(s, COMMERCIAL, jeton("c"))).isEmpty();
    }

    @Test
    void rv_chef_luiEtSonEquipe_pasLesAutres() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(chef, tranche)), PROSPECTIONS);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("a"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("b"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("c"))).isEmpty();
    }

    @Test
    void rv_exempte_tousLesCommerciaux() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, tranche)), PROSPECTIONS);

        SoftAssertions soft = new SoftAssertions();
        for (String qui : List.of("a", "b", "c")) {
            soft.assertThat(lignesContenant(s, COMMERCIAL, jeton(qui))).as(qui).hasSize(1);
        }
        soft.assertAll();
    }

    @Test
    void rv_commercialIdDemande_restreintLesLignes() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, tranche)
                .param(COMMERCIAL_ID, cle(commercialB))), PROSPECTIONS);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("b"))).hasSize(1);
        assertThat(lignesContenant(s, COMMERCIAL, jeton("a"))).isEmpty();
    }

    /** Décision 22/10 : un commercial demandé hors portée est ignoré par le socle (200), jamais montré. */
    @Test
    void rv_commercialHorsPortee_ignore_absentDesDonnees() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(commercialA, tranche)
                .param(COMMERCIAL_ID, cle(commercialC))), PROSPECTIONS);

        assertThat(lignesContenant(s, COMMERCIAL, jeton("c"))).isEmpty();
    }

    @Test
    void rv_trancheSocieteInactive_aucunChiffre() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(direction, trancheInactive)), PROSPECTIONS);

        assertThat(somme(lignes(s), TOTAL)).isEqualByComparingTo("0");
    }

    @Test
    void rv_trancheHorsPerimetre_aucunChiffre() throws Exception {
        Map<String, Object> s = section(appels.sections(rapport(commercialA, trancheP2)), PROSPECTIONS);

        assertThat(somme(lignes(s), TOTAL)).isEqualByComparingTo("0");
    }
}
