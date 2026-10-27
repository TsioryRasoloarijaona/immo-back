package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.DATE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rapport n° 4 {@code ComTdbSuiviHebdoPrevision} « SUIVI HEBDOMADAIRE DES PRÉVISIONS » (FEATURE-tdb-vente-previsions.md §6,
 * §9.3 R-P12, §9.4 R-V1, R-V2, R-V4, R-V5 ; décisions Q6–Q8). Année 2065 (6 avril = lundi, ≤ 2078 pour {@code smalldatetime}) :
 * <pre>
 *   T  (projet P, lancée 01/03, 10 racines VENTE) : d1 avancé 06/04 (J) · d2 avancé 12/04 · d3 avancé 13/04, annulé 30/04
 *      · 7 libres ; règlements de d1 : 250 000 le 08/04, 100 000 le 20/04 ; visites le 06/04 et le 20/04
 *   T2 (projet P, en vente, SANS MarketingStartDate) : un dossier avancé 01/03     ⇒ stock −1
 *   T3 (projet P2, en vente, sans lancement ni mouvement)                          ⇒ une ligne à 0
 * </pre>
 * J = 06/04 ⇒ T : stock 10, réalisation semaine 2 (06/04 et 12/04), mois 3 − 1 = 2. Avant le code, le code de rapport sans
 * classe répond 404 : tous les tests sont rouges.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviHebdoPrevisionsRapportTest {

    private static final String CODE = "ComTdbSuiviHebdoPrevision";
    private static final int Y = 2065;
    private static final LocalDate J = LocalDate.of(Y, 4, 6);
    private static final String SUIVI = "suivi";
    private static final String PROGRAMME = "programme";
    private static final String TRANCHE = "tranche";
    private static final String STOCK = "stock";
    private static final String CUMUL_LANCEMENT = "cumulLancement";
    private static final String PREVU_SEMAINE = "prevuSemaine";
    private static final String REALISATION_SEMAINE = "realisationSemaine";
    private static final String ECART_SEMAINE = "ecartSemaine";
    private static final String VISITES_SEMAINE = "visitesSemaine";
    private static final String VERSEMENTS_SEMAINE = "versementsSemaine";
    private static final String PREVU_MOIS = "prevuMois";
    private static final String REALISATION_MOIS = "realisationMois";
    private static final String ECART_MOIS = "ecartMois";
    private static final String VISITES_MOIS = "visitesMois";
    private static final String VERSEMENTS_MOIS = "versementsMois";
    private static final String TAUX_SEMAINE = "tauxSemaine";
    private static final String TAUX_MOIS = "tauxMois";
    private static final String GROUPE = "groupe";
    private static final String SOMME = "SOMME";
    private static final String NOMBRE = "NOMBRE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private ComProspectionRepository prospections;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private UUID rapportId;
    private Jeu jeu;
    private AdmRole roleLibre;
    private RequestPostProcessor libre;
    private ComActionMessageType visite;
    private CfgProject p;
    private CfgProject p2;
    private CfgTranche t;
    private CfgTranche t2;
    private CfgTranche t3;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        rapports.tables();
        rapportId = rapports.rapportVente(CODE, "SUIVI HEBDOMADAIRE DES PREVISIONS*", "Suivi HEBDOMADAIRE ", 110);
        jeu = fixture.jeu("N4");
        Statuts st = jeu.statuts();
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_RESILIATION, st.desistement().getId());
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_ANNULATION, st.annulation().getId());
        roleLibre = rapports.role("Libre " + jeu.prefixe(), false);
        libre = connecte(rapports.compte("n4." + jeu.prefixe(), roleLibre));
        visite = fx.type(InteractionKpi.TYPE_VISITE);

        p = jeu.projet("P", true);
        t = jeu.tranche("T", p);
        ComFolder d1 = avancer(t, st.concretisation(), j(4, 6), null);
        avancer(t, st.concretisation(), j(4, 12), null);
        avancer(t, st.annulation(), j(4, 13), j(4, 30));
        libres(t, 7);
        t = jeu.lancer(t, j(3, 1));
        jeu.encaisser(d1, "250000", j(4, 8));
        jeu.encaisser(d1, "100000", j(4, 20));
        visiter(t, j(4, 6));
        visiter(t, j(4, 20));

        t2 = jeu.tranche("T2", p);
        avancer(t2, st.concretisation(), j(3, 1), null);

        p2 = jeu.projet("P2", true);
        t3 = jeu.tranche("T3", p2);
        rapports.relire();
    }

    private static LocalDate j(int mois, int jour) {
        return LocalDate.of(Y, mois, jour);
    }

    private ComFolder avancer(CfgTranche tranche, StrStatus statut, LocalDate avance, LocalDate sortie) {
        return jeu.dater(jeu.dossier(tranche, statut), avance, null, null, sortie);
    }

    private void libres(CfgTranche tranche, int nombre) {
        for (int i = 0; i < nombre; i++) {
            jeu.articleLibre(tranche, "1000");
        }
    }

    private ComProspection visiter(CfgTranche tranche, LocalDate jour) {
        return fx.prospection(tranche, null, visite, jour, null);
    }

    private String corps(RequestPostProcessor qui, LocalDate date, CfgTranche... tranches) throws Exception {
        if (date == null) {
            return appels.corps(demande(CODE, qui, List.of(tranches)));
        }
        return appels.corps(demande(CODE, qui, List.of(tranches), DATE, date.toString()));
    }

    private Map<String, Object> suivi(RequestPostProcessor qui, LocalDate date, CfgTranche... tranches)
            throws Exception {
        List<Map<String, Object>> sections = JsonPath.read(corps(qui, date, tranches), "$.sections");
        return section(sections, SUIVI);
    }

    private Map<String, Object> ligneDe(Map<String, Object> s, CfgTranche tranche) {
        return ligneContenant(s, TRANCHE, tranche.getCode());
    }

    private static BigDecimal valeur(Map<String, Object> ligne, String colonne) {
        return nombre(ligne.get(colonne));
    }

    private static List<String> avertissements(String corps) {
        return JsonPath.read(corps, "$.avertissements");
    }

    // ------------------------------------------------------------------ R-P12 : stock, semaine, mois

    @Test
    void rP12_stockAuJourJ_mouvementsAvantJ_lancementCompte_cumulLancementZero() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, STOCK)).as("lancement 10, aucune sortie avant le 06/04").isEqualByComparingTo("10");
        soft.assertThat(valeur(l, CUMUL_LANCEMENT)).as("10 articles − stock 10").isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void rP12_realisationSemaine_venteDuJourJComptee_venteDeJPlus7Exclue() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        assertThat(valeur(l, REALISATION_SEMAINE)).as("06/04 et 12/04 ; le 13/04 est hors semaine")
                .isEqualByComparingTo("2");
    }

    @Test
    void rP12_realisationMois_sortiesDuMoisMoinsRetoursDuMois() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        assertThat(valeur(l, REALISATION_MOIS)).as("3 concrétisations d'avril − 1 annulation du 30/04")
                .isEqualByComparingTo("2");
    }

    @Test
    void rP12_visitesSemaineEtMois_prospectionsDeTypeVisiteDeLaTranche() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, VISITES_SEMAINE)).as("06/04 seulement").isEqualByComparingTo("1");
        soft.assertThat(valeur(l, VISITES_MOIS)).as("06/04 et 20/04").isEqualByComparingTo("2");
        soft.assertAll();
    }

    @Test
    void rP12_versementsSemaineEtMois_encaissementsDeLaPeriode() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, VERSEMENTS_SEMAINE)).as("règlement du 08/04").isEqualByComparingTo("250000");
        soft.assertThat(valeur(l, VERSEMENTS_MOIS)).as("08/04 + 20/04").isEqualByComparingTo("350000");
        soft.assertAll();
    }

    @Test
    void rP12_taux_realisationSurVisitesFois100() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, TAUX_MOIS)).as("2 ÷ 2 × 100").isEqualByComparingTo("100");
        soft.assertThat(valeur(l, TAUX_SEMAINE)).as("2 ÷ 1 × 100").isEqualByComparingTo("200");
        soft.assertAll();
    }

    @Test
    void rP12_tauxNull_siAucuneVisite() throws Exception {
        Map<String, Object> l = ligneDe(suivi(libre, J, t2), t2);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, VISITES_SEMAINE)).as("précondition").isEqualByComparingTo("0");
        soft.assertThat(l).extractingByKey(TAUX_SEMAINE).isNull();
        soft.assertThat(l).extractingByKey(TAUX_MOIS).isNull();
        soft.assertAll();
    }

    @Test
    void rP12_prevuNonSaisi_prevusAZero_ecartEgalRealisation_avertissement() throws Exception {
        String corps = corps(libre, J, t);
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        Map<String, Object> l = ligneDe(section(sections, SUIVI), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, PREVU_SEMAINE)).isEqualByComparingTo("0");
        soft.assertThat(valeur(l, PREVU_MOIS)).isEqualByComparingTo("0");
        soft.assertThat(valeur(l, ECART_SEMAINE)).isEqualByComparingTo("2");
        soft.assertThat(valeur(l, ECART_MOIS)).isEqualByComparingTo("2");
        soft.assertThat(avertissements(corps)).anyMatch(a -> a.contains("Prévu non saisi"));
        soft.assertAll();
    }

    // Q6 : pré-prospections exclues
    @Test
    void rP12_preProspectionDeTypeVisite_nonComptee() throws Exception {
        ComProspection pre = visiter(t, j(4, 7));
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospections.save(pre);
        rapports.relire();

        Map<String, Object> l = ligneDe(suivi(libre, J, t), t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, VISITES_SEMAINE)).isEqualByComparingTo("1");
        soft.assertThat(valeur(l, VISITES_MOIS)).isEqualByComparingTo("2");
        soft.assertAll();
    }

    // Q8 : stock négatif reproduit + avertissement
    @Test
    void rP12_trancheSansDateDeCommercialisation_stockNegatif_avertissement() throws Exception {
        String corps = corps(libre, J, t, t2);
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        String seuleT = corps(libre, J, t);

        assertThat(valeur(ligneDe(section(sections, SUIVI), t2), STOCK)).as("lancement jamais compté")
                .isEqualByComparingTo("-1");
        assertThat(avertissements(corps)).anyMatch(a -> a.contains("sans date de commercialisation"));
        assertThat(avertissements(seuleT)).filteredOn(a -> a.contains("sans date de commercialisation")).isEmpty();
    }

    @Test
    void rP12_dateAbsente_lundiDeLaSemaineDeLHorloge() throws Exception {
        String corps = corps(libre, null, t);

        List<String> date = JsonPath.read(corps, "$.parametres.date");
        assertThat(date).containsExactly(
                LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString());
    }

    @Test
    void rP12_jourQuiNEstPasUnLundi_accepteTelQuel_semaineDeJAJPlus6() throws Exception {
        LocalDate mercredi = j(4, 8);
        String corps = corps(libre, mercredi, t);
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        Map<String, Object> l = ligneDe(section(sections, SUIVI), t);

        List<String> date = JsonPath.read(corps, "$.parametres.date");
        assertThat(date).containsExactly(mercredi.toString());
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(l, STOCK)).as("10 − vente du 06/04").isEqualByComparingTo("9");
        soft.assertThat(valeur(l, REALISATION_SEMAINE)).as("12/04 et 13/04").isEqualByComparingTo("2");
        soft.assertThat(valeur(l, CUMUL_LANCEMENT)).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void rP12_descripteur_dateSemaineDuDefautLundi_etTrancheId() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(libre));
        List<String> nomsParametres = JsonPath.read(corps, "$.parametres[*].nom");
        List<Map<String, Object>> date = JsonPath.read(corps, "$.parametres[?(@.nom == 'date')]");

        assertThat(nomsParametres).containsExactlyInAnyOrder(DATE, TRANCHE_ID);
        assertThat(date).singleElement().satisfies(x -> assertThat(x).containsEntry("type", "DATE")
                .containsEntry("libelle", "Semaine du")
                .containsEntry("valeurParDefaut",
                        LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()));
    }

    // ------------------------------------------------------------------ R-P12 : forme de la section

    @Test
    void rP12_uneSectionSuivi_colonnesTypees_sansGraphique() throws Exception {
        List<Map<String, Object>> sections = JsonPath.read(corps(libre, J, t), "$.sections");
        Map<String, Object> s = section(sections, SUIVI);

        assertThat(sections).extracting(x -> x.get("id")).containsExactly(SUIVI);
        assertThat(noms(s)).containsExactlyInAnyOrder(PROGRAMME, TRANCHE, STOCK, CUMUL_LANCEMENT, PREVU_SEMAINE,
                REALISATION_SEMAINE, ECART_SEMAINE, VISITES_SEMAINE, VERSEMENTS_SEMAINE, PREVU_MOIS, REALISATION_MOIS,
                ECART_MOIS, VISITES_MOIS, VERSEMENTS_MOIS, TAUX_SEMAINE, TAUX_MOIS);
        SoftAssertions soft = new SoftAssertions();
        for (String nom : List.of(PROGRAMME, TRANCHE)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("type", "TEXTE");
        }
        for (String nom : List.of(STOCK, CUMUL_LANCEMENT, PREVU_SEMAINE, REALISATION_SEMAINE, ECART_SEMAINE,
                VISITES_SEMAINE, PREVU_MOIS, REALISATION_MOIS, ECART_MOIS, VISITES_MOIS)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("type", NOMBRE).containsEntry("fonctionTotal", SOMME);
        }
        for (String nom : List.of(VERSEMENTS_SEMAINE, VERSEMENTS_MOIS)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("type", "MONTANT")
                    .containsEntry("fonctionTotal", SOMME);
        }
        for (String nom : List.of(TAUX_SEMAINE, TAUX_MOIS)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("type", "POURCENTAGE");
        }
        soft.assertThat((List<?>) s.get("graphiques")).as("le modèle n'a pas de graphique").isNullOrEmpty();
        soft.assertAll();
    }

    @Test
    void rP12_groupesDEnTete_stock_semaine_mois_pourcentage() throws Exception {
        Map<String, Object> s = suivi(libre, J, t);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(String.valueOf(colonne(s, STOCK).get(GROUPE))).startsWith("Stock au");
        for (String nom : List.of(CUMUL_LANCEMENT, PREVU_SEMAINE, REALISATION_SEMAINE, ECART_SEMAINE, VISITES_SEMAINE,
                VERSEMENTS_SEMAINE)) {
            soft.assertThat(String.valueOf(colonne(s, nom).get(GROUPE))).as(nom)
                    .startsWith("Analyse de la semaine du");
        }
        for (String nom : List.of(PREVU_MOIS, REALISATION_MOIS, ECART_MOIS, VISITES_MOIS, VERSEMENTS_MOIS)) {
            soft.assertThat(String.valueOf(colonne(s, nom).get(GROUPE))).as(nom).startsWith("Analyse mois");
        }
        for (String nom : List.of(TAUX_SEMAINE, TAUX_MOIS)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry(GROUPE, "% réalisation");
        }
        soft.assertAll();
    }

    @Test
    void rP12_uneLigneParTrancheVisible_memeSansMouvement() throws Exception {
        Map<String, Object> s = suivi(libre, J, t, t2, t3);

        assertThat(lignes(s)).hasSize(3);
        Map<String, Object> vide = ligneDe(s, t3);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(vide).containsEntry(PROGRAMME, p2.getDescription());
        soft.assertThat(valeur(vide, STOCK)).isEqualByComparingTo("0");
        soft.assertThat(valeur(vide, REALISATION_SEMAINE)).isEqualByComparingTo("0");
        soft.assertThat(valeur(vide, VERSEMENTS_MOIS)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void rP12_groupeParProgramme_sousTotalEgalSommeDeSesTranches_sansTaux() throws Exception {
        Map<String, Object> s = suivi(libre, J, t, t2, t3);
        Map<String, Map<String, Object>> parProgramme = sousTotaux(s);

        assertThat(s).containsEntry("groupePar", PROGRAMME);
        assertThat(parProgramme).isNotNull().containsKeys(p.getDescription(), p2.getDescription());
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(parProgramme.get(p.getDescription()).get(STOCK))).as("10 + (−1)")
                .isEqualByComparingTo("9");
        soft.assertThat(nombre(parProgramme.get(p.getDescription()).get(VERSEMENTS_MOIS)))
                .isEqualByComparingTo("350000");
        soft.assertThat(nombre(parProgramme.get(p2.getDescription()).get(STOCK))).isEqualByComparingTo("0");
        soft.assertThat(parProgramme.get(p.getDescription())).as("sous-totaux de taux absents (décision 27/10)")
                .extractingByKey(TAUX_SEMAINE).isNull();
        soft.assertAll();
    }

    @Test
    void rP12_totalGeneral_sommes_etTauxImposesSommeRealisationsSurSommeVisites() throws Exception {
        visiter(t2, j(4, 7));
        rapports.relire();

        Map<String, Object> s = suivi(libre, J, t, t2, t3);
        Map<String, Object> total = totaux(s);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(total.get(STOCK))).isEqualByComparingTo("9");
        soft.assertThat(nombre(total.get(REALISATION_SEMAINE))).isEqualByComparingTo("2");
        soft.assertThat(nombre(total.get(VISITES_SEMAINE))).isEqualByComparingTo("2");
        soft.assertThat(nombre(total.get(VERSEMENTS_SEMAINE))).isEqualByComparingTo("250000");
        soft.assertThat(nombre(total.get(TAUX_SEMAINE))).as("2 ÷ 2 × 100, pas 200 + 0")
                .isEqualByComparingTo("100");
        soft.assertThat(nombre(total.get(TAUX_MOIS))).as("2 ÷ 3 × 100").isEqualByComparingTo("66.67");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-V1 périmètre, R-V2 société inactive

    @Test
    void rV1_commercialAffecteAuProjetP_aucuneLigneNiValeurDuProjetB() throws Exception {
        CfgTranche tb = jeu.tranche("TB", jeu.projet("PB", true));
        libres(tb, 2);
        tb = jeu.lancer(tb, j(3, 1));
        visiter(tb, j(4, 7));
        AdmUser a = rapports.compte("n4a." + jeu.prefixe(), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        ev.affecter(a, p);
        rapports.relire();

        Map<String, Object> s = suivi(connecte(a), J);

        assertThat(lignes(s)).isNotEmpty()
                .allSatisfy(l -> assertThat(l).containsEntry(PROGRAMME, p.getDescription()));
        assertThat(lignesContenant(s, TRANCHE, tb.getCode())).isEmpty();
        assertThat(nombre(totaux(s).get(VISITES_SEMAINE))).as("la seule visite de T").isEqualByComparingTo("1");
    }

    @Test
    void rV1_trancheIdDuProjetB_demandeeParLeCommercialDeP_200_aucuneLigne() throws Exception {
        CfgTranche tb = jeu.tranche("TB", jeu.projet("PB", true));
        libres(tb, 2);
        tb = jeu.lancer(tb, j(3, 1));
        AdmUser a = rapports.compte("n4a." + jeu.prefixe(), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        ev.affecter(a, p);
        rapports.relire();

        Map<String, Object> s = suivi(connecte(a), J, tb);

        assertThat(lignes(s)).isEmpty();
    }

    @Test
    void rV2_trancheDUneSocieteInactive_absenteDesLignesEtDesTotaux() throws Exception {
        CfgTranche ti = jeu.tranche("TI", jeu.projet("PI", false));
        libres(ti, 3);
        ti = jeu.lancer(ti, j(3, 1));
        rapports.relire();

        Map<String, Object> s = suivi(libre, J, t, ti);

        assertThat(lignesContenant(s, TRANCHE, ti.getCode())).isEmpty();
        assertThat(nombre(totaux(s).get(STOCK))).isEqualByComparingTo("10");
    }

    // ------------------------------------------------------------------ R-V4 droits par le socle

    @Test
    void rV4_roleRestreintSansLigne_403SurDescripteurResultatExport() throws Exception {
        RequestPostProcessor restreint = connecte(rapports.compte("n4r." + jeu.prefixe(),
                rapports.role("Restreint " + jeu.prefixe(), true)));
        rapports.relire();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(restreint))).as("descripteur").isEqualTo(403);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString())
                .with(restreint))).as("resultat").isEqualTo(403);
        soft.assertThat(appels.exporter(get(export(VENTE, CODE)).param("format", "xlsx")
                .param(TRANCHE_ID, t.getId().toString()).with(restreint)).getResponse().getStatus()).as("export")
                .isEqualTo(403);
        soft.assertAll();
    }

    @Test
    void rV4_commercialChefLie_200SurDescripteurEtResultat() throws Exception {
        AdmRole chef = rapports.role("Commercial Chef " + jeu.prefixe(), true);
        rapports.lier(rapportId, chef);
        RequestPostProcessor lie = connecte(rapports.compte("n4c." + jeu.prefixe(), chef));
        rapports.relire();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, CODE)).with(lie))).as("descripteur").isEqualTo(200);
        soft.assertThat(appels.statut(get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString()).with(lie)))
                .as("resultat").isEqualTo(200);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ R-V5 clé d'API

    @Test
    void rV5_cleDApiAvecToutesLesPortees_403HorsPortee_surLesRoutesDuN4() throws Exception {
        AdmUser compte = dossiers.compte("N4-" + jeu.prefixe(), "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n4-" + jeu.prefixe());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        for (String url : List.of(descripteur(VENTE, CODE), resultat(VENTE, CODE), export(VENTE, CODE))) {
            mockMvc.perform(get(url).param(TRANCHE_ID, t.getId().toString())
                            .header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
        }
    }
}
