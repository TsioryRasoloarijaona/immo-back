package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.fixtures.FixtureTransformationVente.StatutsVente;
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
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * N° 2 SUIVI HEBDOMADAIRE (FEATURE-tdb-vente-rapports-commercial.md §3, §2, §10). Semaine du lundi 03/03/2025 au
 * dimanche 09/03/2025 ; projet P (actif) : tranche T ; projet PI (entité INACTIVE) : tranche TI.
 * <pre>
 *   commercial A (rôle Commercial, lié au rapport, affecté à P) ; commercial B (idem, affecté à P2) ; direction exemptée
 *   concrétisations : cA CONC avance 05/03, prix 1 500 000, article 100 m² + sous-article 100 m², avance 300 000 réglée
 *                     120 000 ; cRes RES avance 05/03 ; cHors CONC avance 12/03 ; cB (B) CONC avance 06/03 ;
 *                     cI CONC avance 05/03 sur TI
 *   désistements    : desA DES annulé le 04/03 ; annA ANN annulé le 04/03 ; desHors DES annulé le 15/03
 *   contrats        : minA MIN signé 07/03 ; annA (aussi signé 04/03, statut ≥ MINUTE non filtré) ; concAsd CONC signé
 *                     07/03 ; minHors MIN signé 15/03 ; minB (B) MIN signé 07/03
 *   livraisons      : livA CLES livré 08/03 ; minDdr MIN livré 08/03 ; livB (B) CLES livré 08/03
 *   versements (sur cA, catégorie autofinancement) : +100 000 encaissement ; −30 000 décaissement ; +5 000 IsCredit NULL ;
 *                     7 000 frais (exclu) ; 9 000 non comptabilisé (exclu) ; 11 000 le 15/03 (exclu) ;
 *                     sur cB : +40 000
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviHebdomadaireRapportTest {

    private static final String CODE = "ComTdbWeeklyMonitoring";
    private static final LocalDate DU = LocalDate.of(2025, 3, 3);
    private static final LocalDate AU = LocalDate.of(2025, 3, 9);
    private static final String CONCRETISATIONS = "concretisations";
    private static final String DESISTEMENTS = "desistements";
    private static final String VERSEMENTS = "versements";
    private static final String CONTRATS = "contrats";
    private static final String LIVRAISONS = "livraisons";
    private static final String PRODUIT = "produit";
    private static final String QUANTITE = "quantite";
    private static final String MONTANT = "montant";
    private static final String TRANCHE_ID = "trancheId";
    private static final String AVERTISSEMENTS = "$.avertissements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private String prefixe;
    private CfgTranche t;
    private CfgTranche ti;
    private StatutsVente statuts;
    private AdmUser commercialA;
    private AdmUser direction;
    private int numero;

    private String cA;
    private String cRes;
    private String cHors;
    private String cB;
    private String cI;
    private String desA;
    private String annA;
    private String desHors;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("SHB");
        fixture.tables();
        UUID rapport = fixture.rapportVente(CODE, "SUIVI HEBDOMADAIRE*", "Suivi HEBDOMADAIRE ", 100);
        AdmRole commercial = fixture.role(RolesEquipe.COMMERCIAL, true);
        fixture.lier(rapport, commercial);
        commercialA = fixture.compte("alpha." + prefixe, commercial);
        AdmRole commercialDeB = fixture.role(RolesEquipe.COMMERCIAL, true);
        fixture.lier(rapport, commercialDeB);
        AdmUser commercialB = fixture.compte("beta." + prefixe, commercialDeB);
        direction = fixture.compte("dg." + prefixe, fixture.role("Direction Générale", false));

        CfgProject projet = ev.projet("P-" + prefixe, true);
        t = ev.tranche("T-" + prefixe, projet, true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        ev.affecter(commercialA, projet);
        ev.affecter(commercialB, ev.projet("P2-" + prefixe, true));
        statuts = fx.statutsVente(prefixe);

        concretisations(commercialB);
        sorties();
        contratsEtLivraisons(commercialB);
        fixture.relire();
    }

    private void concretisations(AdmUser commercialB) {
        ComFolder dossierA = ev.prix(avance(dossier(t, commercialA, statuts.concretisation()), jour(5)),
                new BigDecimal("1500000"));
        ev.sousArticle(dossierA.getStkItem().getCode() + "-S", dossierA.getStkItem(), ev.categorie("CS-" + prefixe),
                ev.type("TS-" + prefixe));
        ComDocument avance = ev.echeance(dossierA, Nature.AVANCE, true, new BigDecimal("300000"));
        ev.regler(avance, new BigDecimal("120000"), true, null);
        cA = produit(dossierA);
        versements(dossierA);

        cRes = produit(avance(dossier(t, commercialA, statuts.reservation()), jour(5)));
        cHors = produit(avance(dossier(t, commercialA, statuts.concretisation()), LocalDate.of(2025, 3, 12)));
        ComFolder dossierB = avance(dossier(t, commercialB, statuts.concretisation()), jour(6));
        cB = produit(dossierB);
        fx.versement(dossierB, Nature.AUTOFINANCEMENT, true, true, DU.plusDays(2), new BigDecimal("40000"));
        cI = produit(avance(dossier(ti, commercialA, statuts.concretisation()), jour(5)));
    }

    private void versements(ComFolder dossier) {
        fx.versement(dossier, Nature.AUTOFINANCEMENT, true, true, DU.plusDays(1), new BigDecimal("100000"));
        fx.versement(dossier, Nature.AUTOFINANCEMENT, false, true, DU.plusDays(1), new BigDecimal("30000"));
        fx.versement(dossier, Nature.AUTOFINANCEMENT, null, true, DU.plusDays(2), new BigDecimal("5000"));
        fx.versement(dossier, Nature.FRAIS, true, true, DU.plusDays(2), new BigDecimal("7000"));
        fx.versement(dossier, Nature.AUTOFINANCEMENT, true, false, DU.plusDays(2), new BigDecimal("9000"));
        fx.versement(dossier, Nature.AUTOFINANCEMENT, true, true, LocalDate.of(2025, 3, 15), new BigDecimal("11000"));
    }

    private void sorties() {
        desA = produit(annulation(dossier(t, commercialA, statuts.desiste()), jour(4)));
        ComFolder annule = annulation(dossier(t, commercialA, statuts.annule()), jour(4));
        annule.setAgreementSavingDate(jour(4).atTime(10, 0));
        annA = produit(fx.enregistrer(annule));
        desHors = produit(annulation(dossier(t, commercialA, statuts.desiste()), LocalDate.of(2025, 3, 15)));
    }

    private void contratsEtLivraisons(AdmUser commercialB) {
        signe(dossier(t, commercialA, statuts.minute()), jour(7));
        signe(dossier(t, commercialA, statuts.concretisation()), jour(7));
        signe(dossier(t, commercialA, statuts.minute()), LocalDate.of(2025, 3, 15));
        signe(dossier(t, commercialB, statuts.minute()), jour(7));
        livre(dossier(t, commercialA, statuts.livre()), jour(8));
        livre(dossier(t, commercialA, statuts.minute()), jour(8));
        livre(dossier(t, commercialB, statuts.livre()), jour(8));
    }

    private static LocalDate jour(int jourDeMars) {
        return LocalDate.of(2025, 3, jourDeMars);
    }

    private ComFolder dossier(CfgTranche tranche, AdmUser commercial, StrStatus statut) {
        numero++;
        return fx.dossier("DSH-" + prefixe + "-" + numero, tranche, commercial, statut, null);
    }

    private ComFolder avance(ComFolder d, LocalDate date) {
        d.setAdvanceDate(date.atTime(10, 0));
        return fx.enregistrer(d);
    }

    private ComFolder annulation(ComFolder d, LocalDate date) {
        d.setCancelDate(date.atTime(10, 0));
        return fx.enregistrer(d);
    }

    private void signe(ComFolder d, LocalDate date) {
        d.setAgreementSavingDate(date.atTime(10, 0));
        fx.enregistrer(d);
    }

    private void livre(ComFolder d, LocalDate date) {
        d.setDeliveryDateReal(date.atTime(10, 0));
        fx.enregistrer(d);
    }

    private static String produit(ComFolder d) {
        return d.getStkItem().getCode();
    }

    private MockHttpServletRequestBuilder requete(AdmUser qui, CfgTranche... tranches) {
        MockHttpServletRequestBuilder r = get(resultat(VENTE, CODE)).param("du", DU.toString())
                .param("au", AU.toString()).with(connecte(qui));
        for (CfgTranche tr : tranches) {
            r.param(TRANCHE_ID, tr.getId().toString());
        }
        return r;
    }

    private static List<Object> produits(Map<String, Object> section) {
        return lignes(section).stream().map(l -> l.get(PRODUIT)).toList();
    }

    /** Valeur de {@code colonne} de la ligne unique de la section (filtrée sur la seule tranche T). */
    private static BigDecimal valeurUnique(Map<String, Object> section, String colonne) {
        assertThat(lignes(section)).as("lignes de %s", section.get("id")).hasSize(1);
        return nombre(lignes(section).get(0).get(colonne));
    }

    // ------------------------------------------------------------------ concrétisations (§3)

    @Test
    void r2_concretisations_avanceDansLaSemaine_statutConcretisationOuPlus() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), CONCRETISATIONS);

        assertThat(produits(s)).contains(cA, cB).doesNotContain(cRes, cHors);
    }

    @Test
    void r2_concretisations_prixM2_prixSurSurfacePlusMoitieDesSousArticles() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), CONCRETISATIONS);

        List<Map<String, Object>> ligne = lignesOu(s, PRODUIT, cA);
        assertThat(ligne).hasSize(1);
        assertThat(nombre(ligne.get(0).get("prixM2"))).as("1 500 000 / (100 + 100 / 2)").isEqualByComparingTo("10000");
    }

    /** Un seul calcul : réglé de la nature AVANCE de {@code MontantsParNatureDossier}, pas les autres versements. */
    @Test
    void r2_concretisations_avanceReglee_regleDeLaNatureAvance() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), CONCRETISATIONS);

        List<Map<String, Object>> ligne = lignesOu(s, PRODUIT, cA);
        assertThat(ligne).hasSize(1);
        assertThat(nombre(ligne.get(0).get("avanceReglee"))).isEqualByComparingTo("120000");
    }

    @Test
    void r2_concretisations_totauxSomme_egalSommeDesLignes() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), CONCRETISATIONS);

        List<String> sommes = colonnesAvecTotal(s, "SOMME");
        assertThat(sommes).contains("surface", "prixVenteTtc", "avanceReglee", "remise");
        SoftAssertions soft = new SoftAssertions();
        for (String nom : sommes) {
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ désistements (§3)

    @Test
    void r2_desistements_dateDAnnulationDansLaSemaine_statutDesistementSeulement() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), DESISTEMENTS);

        assertThat(produits(s)).contains(desA).doesNotContain(annA, desHors);
    }

    // ------------------------------------------------------------------ contrats, livraisons (§3)

    @Test
    void r2_contrats_signesDansLaSemaine_statutFinalisationOuPlus_annuleNonFiltre() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), CONTRATS);

        assertThat(valeurUnique(s, QUANTITE)).as("minA + annA + minB").isEqualByComparingTo("3");
    }

    @Test
    void r2_livraisons_livresDansLaSemaine_statutLivraisonOuPlus() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), LIVRAISONS);

        assertThat(valeurUnique(s, QUANTITE)).as("livA + livB").isEqualByComparingTo("2");
    }

    // ------------------------------------------------------------------ versements (§3, signe partagé)

    @Test
    void r2_versements_signePartage_fraisEtNonComptabiliseExclus_horsSemaineExclu() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), VERSEMENTS);

        assertThat(valeurUnique(s, MONTANT)).as("100 000 − 30 000 + 5 000 + 40 000").isEqualByComparingTo("115000");
    }

    @Test
    void r2_versements_totalEgalSommeDesLignes() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction, t)), VERSEMENTS);

        assertThat(nombre(totaux(s).get(MONTANT))).isEqualByComparingTo(somme(lignes(s), MONTANT))
                .isEqualByComparingTo("115000");
    }

    // ------------------------------------------------------------------ seuils AdmParamsGuid (§3 notes)

    @Test
    void r2_seuilAbsent_avertissement_jamaisDErreur() throws Exception {
        List<String> avant = JsonPath.read(appels.corps(requete(direction, t)), AVERTISSEMENTS);
        fx.retirerParametre(FixtureTransformationVente.SEUIL_LIVRAISON);

        List<String> apres = JsonPath.read(appels.corps(requete(direction, t)), AVERTISSEMENTS);

        assertThat(apres).hasSizeGreaterThan(avant.size());
    }

    // ------------------------------------------------------------------ paramètres (§3)

    @Test
    void descripteur_duLundiDeLaSemaine_auDimanche_trancheIdMultiple() throws Exception {
        LocalDate lundi = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        mockMvc.perform(get(descripteur(VENTE, CODE)).with(connecte(direction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].valeurParDefaut").value(lundi.toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'au')].valeurParDefaut")
                        .value(lundi.plusDays(6).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].multiple").value(true));
    }

    // ------------------------------------------------------------------ visibilité (RV-R3b, RV-R3a, règle 6)

    @Test
    void rvR3b_commercial_concretisations_seulementSesDossiers() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(commercialA, t)), CONCRETISATIONS);

        assertThat(produits(s)).contains(cA).doesNotContain(cB);
    }

    @Test
    void rvR3b_commercial_contratsEtLivraisons_seulementSesDossiers() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(commercialA, t));

        assertThat(valeurUnique(section(sections, CONTRATS), QUANTITE)).as("minA + annA").isEqualByComparingTo("2");
        assertThat(valeurUnique(section(sections, LIVRAISONS), QUANTITE)).as("livA").isEqualByComparingTo("1");
    }

    @Test
    void rvR3b_commercial_versements_seulementSesDossiers() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(commercialA, t)), VERSEMENTS);

        assertThat(valeurUnique(s, MONTANT)).isEqualByComparingTo("75000");
    }

    @Test
    void rvR3a_trancheDUneEntiteInactive_absente() throws Exception {
        Map<String, Object> s = section(appels.sections(requete(direction)), CONCRETISATIONS);

        assertThat(produits(s)).contains(cA).doesNotContain(cI);
    }

    @Test
    void rvR3a_trancheIdInactive_resultatVide() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(direction, ti));

        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(lignes(s)).as((String) s.get("id")).isEmpty());
    }

    // ------------------------------------------------------------------ droits (§1 : ouvert au rôle Commercial)

    @Test
    void droits_roleCommercialLie_200() throws Exception {
        mockMvc.perform(requete(commercialA, t)).andExpect(status().isOk());
    }

    @Test
    void droits_roleRestreintNonLie_403() throws Exception {
        AdmUser sansLien = fixture.compte("sanslien." + prefixe, fixture.role("Sans lien " + prefixe, true));
        fixture.relire();

        mockMvc.perform(requete(sansLien, t)).andExpect(status().isForbidden());
    }
}
