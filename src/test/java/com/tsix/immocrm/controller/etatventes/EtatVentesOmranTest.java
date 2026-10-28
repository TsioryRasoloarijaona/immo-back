package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * État des ventes en disposition OMRAN (FEATURE-etat-ventes-omran.md §1, §4) : racines {@code IsUnderItem = 1} libres
 * retirées (1.1), frais syndic séparés (1.2), alerte du dossier (1.3), visibilité inchangée (1.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesOmranTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String URL_DOSSIERS = "/api/dossiers";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String ARTICLE_ID = "articleId";
    private static final String DOSSIER_ID = "dossierId";
    private static final String CONTENT = "$.content";
    private static final String POINTE = "POINTED";
    private static final String FRAIS = "frais";
    private static final String FRAIS_REGLES = "fraisRegles";
    private static final String FRAIS_RESTE = "fraisReste";
    private static final String FRAIS_SYNDIC = "fraisSyndic";
    private static final String FRAIS_SYNDIC_REGLES = "fraisSyndicRegles";
    private static final String FRAIS_SYNDIC_RESTE = "fraisSyndicReste";
    private static final String ALERTE = "alerte";
    private static final String ROLE_COMMERCIAL = "Commercial";
    private static final String SOUS_ARTICLES = "sousArticles";
    private static final String ENTITE_ID = "entiteId";
    private static final List<String> SYNDIC = List.of(FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES, FRAIS_SYNDIC_RESTE);
    private static final List<String> NOUVELLES = List.of(FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES, FRAIS_SYNDIC_RESTE,
            ALERTE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureEtatVentes fixture;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private FixtureDossierVente fixtureDossier;
    @Autowired private FixtureInteraction fixtureInteraction;
    @Autowired private EntityManager em;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemCategory parking;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVO");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        appartement = fixture.categorie("APP-" + prefixe);
        parking = fixture.categorie("PAR-" + prefixe);
        type = fixture.type("T-" + prefixe);
    }

    // ------------------------------------------------------------------ jeu

    private StkItem appart(String suffixe) {
        return fixture.article(prefixe + "-" + suffixe, tranche, appartement, type);
    }

    private StkItem parkingRacine(String suffixe) {
        return fixture.parkingAutonome(prefixe + "-" + suffixe, tranche, parking, type);
    }

    private ComFolder vendu(StkItem a) {
        ComFolder d = fixture.vendre(a, "DOS-" + a.getCode(), fixture.statut(StatutDossier.RESERVATION), null,
                LocalDateTime.of(2025, 3, 15, 10, 0));
        return fixture.prix(d, new BigDecimal("1000000"));
    }

    private void annule(StkItem a) {
        fixture.vendre(a, "DOS-" + a.getCode(), fixture.statutSortie("ANN-" + a.getCode(), true), null, null);
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param(TRANCHE_ID, tranche.getId().toString()).param(SIZE, "100");
    }

    private List<String> articlesListes(MockHttpServletRequestBuilder requete) throws Exception {
        return JsonPath.read(corps(requete.param(COLONNES, ARTICLE_ID)), "$.content[*].articleId");
    }

    private Map<String, Object> ligneEtat(StkItem a, List<String> colonnes) throws Exception {
        List<Map<String, Object>> lignes = JsonPath.read(corps(liste()
                .param(COLONNES, ARTICLE_ID + "," + String.join(",", colonnes))), CONTENT);
        List<Map<String, Object>> trouvees = lignes.stream()
                .filter(l -> a.getId().toString().equals(l.get(ARTICLE_ID))).toList();
        assertThat(trouvees).as("ligne de %s", a.getCode()).hasSize(1);
        return trouvees.getFirst();
    }

    private Map<String, Object> ligneDossier(ComFolder d, List<String> colonnes) throws Exception {
        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL_DOSSIERS).param(SEARCH, prefixe)
                .param(SIZE, "100").param(COLONNES, "id," + String.join(",", colonnes))), CONTENT);
        List<Map<String, Object>> trouvees = lignes.stream()
                .filter(l -> d.getId().toString().equals(l.get("id"))).toList();
        assertThat(trouvees).as("dossier %s", d.getCode()).hasSize(1);
        return trouvees.getFirst();
    }

    private static BigDecimal montant(Map<String, Object> ligne, String nom) {
        assertThat(ligne).containsKey(nom);
        Object valeur = ligne.get(nom);
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    // ------------------------------------------------------------------ 1.1 racines IsUnderItem = 1 libres retirées

    /** 1.1 : parking racine sans dossier absent, quel que soit le filtre (« Libre » compris). */
    @ParameterizedTest
    @ValueSource(strings = {"", "statut=LIBRE", "nature=SOUS_ARTICLES", "nature=TOUS"})
    void r11_parkingRacineLibre_absent_quelQueSoitLeFiltre(String filtre) throws Exception {
        StkItem parkingLibre = parkingRacine("PL");
        StkItem appartLibre = appart("AL");
        fixture.relire();
        MockHttpServletRequestBuilder requete = liste();
        if (!filtre.isEmpty()) {
            String[] cleValeur = filtre.split("=");
            requete.param(cleValeur[0], cleValeur[1]);
        }

        List<String> ids = articlesListes(requete);

        assertThat(ids).doesNotContain(parkingLibre.getId().toString());
        if (!filtre.startsWith("nature=SOUS")) {
            assertThat(ids).contains(appartLibre.getId().toString());
        }
    }

    /** 1.1 : « sans dossier non annulé » — un parking racine dont le seul dossier est annulé reste absent. */
    @Test
    void r11_parkingRacineDossierAnnule_absent() throws Exception {
        StkItem parkingAnnule = parkingRacine("PA");
        annule(parkingAnnule);
        fixture.relire();

        assertThat(articlesListes(liste())).doesNotContain(parkingAnnule.getId().toString());
    }

    /** 1.1 : parking racine vendu présent (c'est une vente), sans filtre et filtré sur son statut. */
    @Test
    void r11_parkingRacineVendu_present() throws Exception {
        StkItem parkingVendu = parkingRacine("PV");
        vendu(parkingVendu);
        fixture.relire();

        assertThat(List.of(articlesListes(liste()), articlesListes(liste().param("statut", "RESERVATION"))))
                .hasSize(2).allSatisfy(ids -> assertThat(ids).contains(parkingVendu.getId().toString()));
    }

    /** 1.1 : un appartement libre reste listé, avec le statut « Libre ». */
    @Test
    void r11_appartementLibre_present() throws Exception {
        StkItem appartLibre = appart("AL");
        fixture.relire();

        assertThat(ligneEtat(appartLibre, List.of("statutCode"))).containsEntry("statutCode", "LIBRE");
    }

    /** 1.1 : le total des colonnes compte les mêmes lignes que la liste (parking libre exclu des deux). */
    @Test
    void r11_totauxColonnes_parkingRacineLibreNonCompte() throws Exception {
        parkingRacine("PL");
        appart("AL");
        vendu(parkingRacine("PV"));
        fixture.relire();

        Number nombreTotal = JsonPath.read(corps(get(URL + "/totaux-colonnes")
                .param(TRANCHE_ID, tranche.getId().toString()).param("agregats", "surface")), "$.nombreTotal");
        Number totalElements = JsonPath.read(corps(liste()), "$.totalElements");

        assertThat(List.of(nombreTotal.longValue(), totalElements.longValue())).containsExactly(2L, 2L);
    }

    // ------------------------------------------------------------------ 1.2 frais syndic

    private record JeuSyndic(StkItem article, ComFolder dossier) {
    }

    /** Échéance FRAIS SYNDIC 16 800 réglée 5 000 (+ 900 annulé), autre frais 3 000 réglé 1 000. */
    private JeuSyndic jeuSyndic() {
        ComSettlementCategory syndic = rd.categorieSyndic();
        StkItem a = appart("SY");
        ComFolder d = vendu(a);
        ComDocument e = rd.echeance(d, syndic, new BigDecimal("16800"));
        fixture.regler(e, new BigDecimal("5000"), true, POINTE);
        fixture.regler(e, new BigDecimal("900"), true, "CANCELED");
        fixture.regler(fixture.echeance(d, Nature.FRAIS, true, new BigDecimal("3000")), new BigDecimal("1000"), true,
                POINTE);
        fixture.relire();
        return new JeuSyndic(a, d);
    }

    /** 1.2 : syndic = l'échéance syndic seule ; réglé = Σ de ses lignes hors annulées ; frais = toutes natures. */
    @Test
    void r12_fraisSyndic_echeanceSyndicSeule_fraisToutesNatures() throws Exception {
        JeuSyndic jeu = jeuSyndic();

        Map<String, Object> ligne = ligneEtat(jeu.article(), List.of(FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES,
                FRAIS_SYNDIC_RESTE, FRAIS, FRAIS_REGLES, FRAIS_RESTE));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(montant(ligne, FRAIS_SYNDIC)).isEqualByComparingTo("16800");
        soft.assertThat(montant(ligne, FRAIS_SYNDIC_REGLES)).isEqualByComparingTo("5000");
        soft.assertThat(montant(ligne, FRAIS_SYNDIC_RESTE)).isEqualByComparingTo("11800");
        soft.assertThat(montant(ligne, FRAIS)).isEqualByComparingTo("19800");
        soft.assertThat(montant(ligne, FRAIS_REGLES)).isEqualByComparingTo("6000");
        soft.assertThat(montant(ligne, FRAIS_RESTE)).isEqualByComparingTo("13800");
        soft.assertAll();
    }

    /** 1.2 : mêmes valeurs que les colonnes FRAIS_SYNDIC* et FRAIS* de la liste des dossiers pour le même dossier. */
    @Test
    void r12_fraisSyndicEtFrais_egauxALaLigneDuDossier() throws Exception {
        JeuSyndic jeu = jeuSyndic();
        List<String> colonnes = List.of(FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES, FRAIS_SYNDIC_RESTE, FRAIS, FRAIS_REGLES,
                FRAIS_RESTE);

        Map<String, Object> etat = ligneEtat(jeu.article(), colonnes);
        Map<String, Object> dossier = ligneDossier(jeu.dossier(), colonnes);

        SoftAssertions soft = new SoftAssertions();
        for (String c : colonnes) {
            soft.assertThat(montant(etat, c)).as(c).isNotNull().isEqualByComparingTo(montant(dossier, c));
        }
        soft.assertAll();
    }

    /** 1.2 : dossier sans échéance syndic → 0 (pas null) ; ses autres frais restent dans {@code frais}. */
    @Test
    void r12_dossierSansEcheanceSyndic_zero() throws Exception {
        rd.categorieSyndic();
        StkItem a = appart("SZ");
        fixture.echeance(vendu(a), Nature.FRAIS, true, new BigDecimal("3000"));
        fixture.relire();

        Map<String, Object> ligne = ligneEtat(a, List.of(FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES, FRAIS_SYNDIC_RESTE,
                FRAIS));

        SoftAssertions soft = new SoftAssertions();
        for (String c : SYNDIC) {
            soft.assertThat(montant(ligne, c)).as(c).isNotNull().isEqualByComparingTo("0");
        }
        soft.assertThat(montant(ligne, FRAIS)).isEqualByComparingTo("3000");
        soft.assertAll();
    }

    /** 1.2 / 1.3 : article sans dossier → colonnes syndic et alerte présentes et nulles. */
    @Test
    void r12r13_articleSansDossier_syndicEtAlerteNulls() throws Exception {
        rd.categorieSyndic();
        StkItem libre = appart("LB");
        fixture.relire();

        Map<String, Object> ligne = ligneEtat(libre, NOUVELLES);

        assertThat(ligne).containsKeys(NOUVELLES.toArray(String[]::new))
                .containsEntry(FRAIS_SYNDIC, null)
                .containsEntry(FRAIS_SYNDIC_REGLES, null)
                .containsEntry(FRAIS_SYNDIC_RESTE, null)
                .containsEntry(ALERTE, null);
    }

    /** 1.2 / 1.3 : colonnes au catalogue, hors défaut serveur ; montants pour le syndic, date pour l'alerte. */
    @Test
    void r12r13_catalogue_nouvellesColonnesHorsDefaut() throws Exception {
        List<Map<String, Object>> catalogue = JsonPath.read(corps(get("/api/referentiels/colonnes/etat-des-ventes")),
                "$");
        Map<String, Map<String, Object>> parNom = new HashMap<>();
        catalogue.forEach(c -> parNom.put((String) c.get("nom"), c));

        SoftAssertions soft = new SoftAssertions();
        for (String c : SYNDIC) {
            soft.assertThat(parNom.getOrDefault(c, Map.of())).as(c)
                    .containsEntry("parDefaut", false).containsEntry("type", "MONTANT");
        }
        soft.assertThat(parNom.getOrDefault(ALERTE, Map.of())).as(ALERTE)
                .containsEntry("parDefaut", false).containsEntry("type", "DATE");
        soft.assertAll();
    }

    /** 1.2 : sans {@code ?colonnes=}, la réponse ne porte pas les nouvelles colonnes (hors défaut serveur). */
    @Test
    void r12r13_sansColonnes_nouvellesColonnesAbsentes() throws Exception {
        jeuSyndic();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param(TRANCHE_ID, tranche.getId().toString())),
                CONTENT);

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l)
                .containsKeys("articleCode", "montantRegle")
                .doesNotContainKeys(NOUVELLES.toArray(String[]::new)));
    }

    // ------------------------------------------------------------------ 1.3 alerte

    private ComProspection prospection(ComFolder d) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-" + d.getCode());
        p.setDocDate(LocalDateTime.of(2025, 1, 10, 9, 0));
        em.persist(p);
        d.setComProspection(p);
        fixture.enregistrer(d);
        return p;
    }

    /** Alerte {@code AdmAlertData} sur un message de la prospection du dossier (lien par {@code ComProspectionId}). */
    private void alerte(ComProspection p, LocalDateTime date, boolean active) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(date.minusDays(2));
        m.setComProspection(p);
        m.setComActionMessageCategory(fixtureInteraction.categorieProspection());
        m.setIsSystem(false);
        m.setIsUnpaid(false);
        m.setIsNotRead(false);
        em.persist(m);
        AdmAlertData a = new AdmAlertData();
        a.setId(UUID.randomUUID());
        a.setRelatedKey(m.getId());
        a.setActive(active);
        a.setAlertDate(date);
        em.persist(a);
    }

    /** 1.3 : alerte active → même valeur dans {@code /api/etat-des-ventes} et {@code /api/dossiers}. */
    @Test
    void r13_alerteActive_memeValeurQueLaListeDesDossiers() throws Exception {
        StkItem a = appart("AA");
        ComFolder d = vendu(a);
        alerte(prospection(d), LocalDateTime.of(2025, 6, 1, 10, 0), true);
        fixture.relire();

        Object etat = ligneEtat(a, List.of(ALERTE)).get(ALERTE);
        Object dossier = ligneDossier(d, List.of(ALERTE)).get(ALERTE);

        assertThat(etat).isNotNull().isEqualTo(dossier);
    }

    /** 1.3 : alerte inactive seule → nulle dans les deux listes. */
    @Test
    void r13_alerteInactiveSeule_absenteDesDeuxListes() throws Exception {
        StkItem a = appart("AI");
        ComFolder d = vendu(a);
        alerte(prospection(d), LocalDateTime.of(2025, 6, 1, 10, 0), false);
        fixture.relire();

        Map<String, Object> etat = ligneEtat(a, List.of(ALERTE));
        Map<String, Object> dossier = ligneDossier(d, List.of(ALERTE));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(etat).containsEntry(ALERTE, null);
        soft.assertThat(dossier.get(ALERTE)).isNull();
        soft.assertAll();
    }

    /** 1.3 : une alerte inactive plus récente ne masque pas l'active ; les deux listes rendent la même date. */
    @Test
    void r13_alerteInactivePlusRecente_ignoree_memeDateQueLesDossiers() throws Exception {
        StkItem a = appart("AM");
        ComFolder d = vendu(a);
        ComProspection p = prospection(d);
        alerte(p, LocalDateTime.of(2025, 6, 1, 10, 0), true);
        alerte(p, LocalDateTime.of(2025, 9, 1, 10, 0), false);
        fixture.relire();

        Object etat = ligneEtat(a, List.of(ALERTE)).get(ALERTE);
        Object dossier = ligneDossier(d, List.of(ALERTE)).get(ALERTE);

        assertThat(etat).isNotNull().isEqualTo(dossier)
                .satisfies(v -> assertThat(String.valueOf(v)).startsWith("2025-06-01"));
    }

    // ------------------------------------------------------------------ §5 sous-articles

    private void itemDuDossier(ComFolder d, String code) {
        ComFolderItem item = new ComFolderItem();
        item.setId(UUID.randomUUID());
        item.setComFolder(d);
        item.setCode(code);
        item.setDescription("Item " + code);
        item.setArea(BigDecimal.ONE);
        item.setIsUnderItem(true);
        item.setIsUnderItemOrigin(false);
        em.persist(item);
    }

    /** §5 / décision Q2 : {@code sousArticles} = {@code sousArticlesCodes} de {@code /api/dossiers} pour le même dossier. */
    @Test
    void s5_sousArticles_egalAuxCodesDeLaListeDesDossiers() throws Exception {
        StkItem a = appart("SA");
        ComFolder d = vendu(a);
        itemDuDossier(d, "P-07-" + prefixe);
        itemDuDossier(d, "C-02-" + prefixe);
        fixture.relire();

        Object etat = ligneEtat(a, List.of(SOUS_ARTICLES)).get(SOUS_ARTICLES);
        Object dossier = ligneDossier(d, List.of("sousArticlesCodes")).get("sousArticlesCodes");

        assertThat(etat).isNotNull().isEqualTo(dossier)
                .satisfies(v -> assertThat(String.valueOf(v)).contains("P-07-" + prefixe, "C-02-" + prefixe));
    }

    /** §5 : ligne libre → {@code sousArticles} nul ; colonne texte au catalogue. */
    @Test
    void s5_sousArticles_ligneLibreNulle_colonneTexteAuCatalogue() throws Exception {
        StkItem libre = appart("SL");
        fixture.relire();

        Map<String, Object> ligne = ligneEtat(libre, List.of(SOUS_ARTICLES));
        List<String> types = JsonPath.read(corps(get("/api/referentiels/colonnes/etat-des-ventes")),
                "$[?(@.nom=='sousArticles')].type");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ligne).containsEntry(SOUS_ARTICLES, null);
        soft.assertThat(types).containsExactly("TEXTE");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ §5 filtre entiteId

    /** Deux entités : A (projet du jeu) et B (autre société), un article vendu dans chacune. */
    private record DeuxEntites(StkItem dansA, StkItem dansB, CfgTranche trancheB, UUID entiteA, UUID entiteB) {
    }

    private DeuxEntites deuxEntites() {
        StkItem a = appart("EA");
        vendu(a);
        CfgProject projetB = fixture.projet("PRJE-" + prefixe, true);
        CfgTranche trancheB = fixture.tranche("TRE-" + prefixe, projetB, true);
        StkItem b = fixture.article(prefixe + "-EB", trancheB, appartement, type);
        vendu(b);
        fixture.relire();
        return new DeuxEntites(a, b, trancheB, projet.getCfgCompany().getId(), projetB.getCfgCompany().getId());
    }

    /** §5 : {@code entiteId} restreint la liste aux articles des tranches de cette entité. */
    @Test
    void s5_entiteId_liste_seulementLesLignesDeLEntite() throws Exception {
        DeuxEntites jeu = deuxEntites();

        List<String> ids = articlesListes(get(URL).param(SEARCH, prefixe).param(ENTITE_ID, jeu.entiteA().toString()));

        assertThat(ids).containsExactly(jeu.dansA().getId().toString());
    }

    /** §5 : {@code entiteId} restreint aussi les totaux de colonnes. */
    @Test
    void s5_entiteId_totaux_seulementLEntite() throws Exception {
        DeuxEntites jeu = deuxEntites();

        Number nombre = JsonPath.read(corps(get(URL + "/totaux-colonnes").param(SEARCH, prefixe)
                .param(ENTITE_ID, jeu.entiteB().toString()).param("agregats", "surface")), "$.nombreTotal");

        assertThat(nombre.longValue()).isEqualTo(1L);
    }

    /** §5 : la synthèse suit l'entité cochée (aucune ligne de la tranche de l'autre entité). */
    @Test
    void s5_entiteId_synthese_sansLaTrancheDeLAutreEntite() throws Exception {
        DeuxEntites jeu = deuxEntites();

        String synthese = corps(get(URL + "/synthese")
                .param(TRANCHE_ID, tranche.getId().toString(), jeu.trancheB().getId().toString())
                .param(ENTITE_ID, jeu.entiteA().toString()));
        List<String> tranches = JsonPath.read(synthese, "$.lignes[*].trancheId");

        assertThat(tranches).isNotEmpty().containsOnly(tranche.getId().toString());
    }

    /** §5 / règle 6 : {@code entiteId} d'une entité hors périmètre ne montre aucune ligne au commercial restreint. */
    @Test
    void s5_entiteId_horsPerimetre_aucuneLigne() throws Exception {
        DeuxEntites jeu = deuxEntites();
        AdmUser commercial = fixtureDossier.compte("edvo.ent." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();

        List<String> horsPerimetre = articlesListes(get(URL).param(SEARCH, prefixe)
                .param(ENTITE_ID, jeu.entiteB().toString()).with(connecte(commercial, ROLE_COMMERCIAL)));
        List<String> lesDeux = articlesListes(get(URL).param(SEARCH, prefixe)
                .param(ENTITE_ID, jeu.entiteA().toString(), jeu.entiteB().toString())
                .with(connecte(commercial, ROLE_COMMERCIAL)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(horsPerimetre).isEmpty();
        soft.assertThat(lesDeux).containsExactly(jeu.dansA().getId().toString());
        soft.assertAll();
    }

    // ------------------------------------------------------------------ 1.1 synthèse

    /** 1.1 : la synthèse ne compte plus le parking racine libre (même source que la liste). */
    @Test
    void r11_synthese_parkingRacineLibreNonCompte() throws Exception {
        parkingRacine("PL");
        appart("AL");
        fixture.relire();

        String synthese = corps(get(URL + "/synthese").param(TRANCHE_ID, tranche.getId().toString()));
        List<Object> lignesParking = JsonPath.read(synthese,
                "$.lignes[?(@.trancheId=='" + tranche.getId() + "' && @.categorieId=='" + parking.getId() + "')]");
        List<Object> lignesAppart = JsonPath.read(synthese,
                "$.lignes[?(@.trancheId=='" + tranche.getId() + "' && @.categorieId=='" + appartement.getId() + "')]");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignesParking).isEmpty();
        soft.assertThat(lignesAppart).hasSize(1);
        soft.assertAll();
    }

    // ------------------------------------------------------------------ 1.4 visibilité

    /** A (projet du commercial) et B (autre projet), chacun vendu avec syndic et alerte actifs. */
    private record Perimetre(StkItem visible, StkItem horsPerimetre, AdmUser commercial) {
    }

    private Perimetre perimetre() {
        ComSettlementCategory syndic = rd.categorieSyndic();
        StkItem a = appart("VA");
        ComFolder da = vendu(a);
        rd.echeance(da, syndic, new BigDecimal("16800"));
        alerte(prospection(da), LocalDateTime.of(2025, 6, 1, 10, 0), true);
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, fixture.projet("PRJB-" + prefixe, true), true);
        StkItem b = fixture.article(prefixe + "-VB", trancheB, appartement, type);
        ComFolder db = vendu(b);
        rd.echeance(db, syndic, new BigDecimal("7000"));
        alerte(prospection(db), LocalDateTime.of(2025, 6, 2, 10, 0), true);
        AdmUser commercial = fixtureDossier.compte("edvo.com." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();
        return new Perimetre(a, b, commercial);
    }

    /** 1.4 : un commercial restreint voit les mêmes lignes avec ou sans les nouvelles colonnes, son périmètre seul. */
    @Test
    void r14_commercialRestreint_nouvellesColonnes_neRendentPasPlusDeLignes() throws Exception {
        Perimetre p = perimetre();

        List<String> sans = JsonPath.read(corps(get(URL).param(SEARCH, prefixe).param(COLONNES, ARTICLE_ID)
                .with(connecte(p.commercial(), ROLE_COMMERCIAL))), "$.content[*].articleId");
        List<String> avec = JsonPath.read(corps(get(URL).param(SEARCH, prefixe)
                .param(COLONNES, ARTICLE_ID + "," + String.join(",", NOUVELLES))
                .with(connecte(p.commercial(), ROLE_COMMERCIAL))), "$.content[*].articleId");

        assertThat(avec).containsExactly(p.visible().getId().toString())
                .containsExactlyElementsOf(sans)
                .doesNotContain(p.horsPerimetre().getId().toString());
    }

    /** 1.4 : la ligne visible porte les montants de son propre dossier, pas ceux d'un dossier hors périmètre. */
    @Test
    void r14_commercialRestreint_syndicDeSaSeuleLigne() throws Exception {
        Perimetre p = perimetre();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param(SEARCH, prefixe)
                .param(COLONNES, ARTICLE_ID + "," + DOSSIER_ID + "," + FRAIS_SYNDIC)
                .with(connecte(p.commercial(), ROLE_COMMERCIAL))), CONTENT);

        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l).containsEntry(ARTICLE_ID, p.visible().getId().toString());
            assertThat(montant(l, FRAIS_SYNDIC)).isEqualByComparingTo("16800");
        });
    }
}
