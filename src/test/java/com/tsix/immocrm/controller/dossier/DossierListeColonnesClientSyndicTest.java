package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sept colonnes de la liste {@code /dossiers} (FEATURE-tdb-vente-rapports-dossiers.md §3.2, §3.3, §9 « Colonnes (n° 1) ») :
 * e-mail, adresse, ville, pays du client principal ; frais syndic (montant, réglé, reste) isolés dans
 * {@code MontantsParNatureDossier} par la catégorie {@code DefaultSyndicFeeDocumentType}.
 * <pre>
 *   d1 : client c1 (e-mail, adresse + complément 3, ville référentiel ET City, pays P1)
 *        syndic 12 000 réglé 5 000 (+ 900 CANCELED), autres frais 3 000
 *   d2 : client c2 (City seule, pays P2), aucune échéance syndic
 *   d3 : client c3 (ni ville ni pays), syndic 4 000 non réglé
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierListeColonnesClientSyndicTest {

    private static final String LISTE = "/api/dossiers";
    private static final String SEARCH = "search";
    private static final String COLONNES = "colonnes";
    private static final String SIZE = "size";
    private static final String CODE = "code";
    private static final String FRAIS_SYNDIC = "fraisSyndic";
    private static final String FRAIS_SYNDIC_REGLES = "fraisSyndicRegles";
    private static final String FRAIS_SYNDIC_RESTE = "fraisSyndicReste";
    private static final String CLIENT_VILLE = "clientVille";
    private static final String CLIENT_PAYS = "clientPays";
    private static final String CATALOGUE = "/api/referentiels/colonnes/dossiers";
    private static final List<String> SEPT = List.of("clientEmail", "clientAdresse", CLIENT_VILLE, CLIENT_PAYS,
            FRAIS_SYNDIC, FRAIS_SYNDIC_REGLES, FRAIS_SYNDIC_RESTE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private CfgTierRepository tiers;
    @Autowired private AdmCityRepository villes;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory emf;

    private String prefixe;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus enCours;
    private ComSettlementCategory syndic;
    private AdmCity casa;
    private AdmCountry pays1;
    private AdmCountry pays2;
    private int numero;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("CSY");
        tranche = ev.tranche("TR-" + prefixe, ev.projet("P-" + prefixe, true), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        enCours = ev.statutEnCours("ENC-" + prefixe);
        syndic = rd.categorieSyndic();
        casa = ville("CASA");
        pays1 = patrimoine.pays("P1-" + prefixe);
        pays2 = patrimoine.pays("P2-" + prefixe);
    }

    private AdmCity ville(String code) {
        AdmCity v = new AdmCity();
        v.setId(UUID.randomUUID());
        v.setCode(code + "-" + prefixe);
        v.setDescription(code + "-" + prefixe);
        return villes.save(v);
    }

    // AdmCityId n'est pas inscriptible par JPA (CfgTier.admCityId en lecture seule) : posé en SQL.
    private void situer(CfgTier client, AdmCity ville) {
        tiers.flush();
        jdbc.update("UPDATE CfgTier SET AdmCityId = ? WHERE PKey = ?", ville.getId().toString(),
                client.getId().toString());
    }

    private ComFolder dossier(String racine, CfgTier client) {
        numero++;
        return ev.vendre(ev.article(racine + "-A" + numero, tranche, categorie, type), racine + "-D" + numero,
                enCours, client, null);
    }

    private CfgTier clientComplet() {
        CfgTier c = ev.client("C1-" + prefixe);
        c.setEMail("c1." + prefixe + "@exemple.ma");
        c.setAddress("12 rue des Orangers");
        c.setAddress3("Bâtiment C");
        c.setCity("VILLE-LIBRE-" + prefixe);
        c.setAdmCountry(pays1);
        CfgTier sauve = tiers.save(c);
        situer(sauve, casa);
        return sauve;
    }

    private CfgTier clientVilleTexte() {
        CfgTier c = ev.client("C2-" + prefixe);
        c.setCity("TEXTE-" + prefixe);
        c.setAdmCountry(pays2);
        return tiers.save(c);
    }

    /** Jeu de la Javadoc de classe ; rend d1, d2, d3. */
    private List<ComFolder> jeuDeBase() {
        ComFolder d1 = dossier(prefixe, clientComplet());
        ComDocument e1 = rd.echeance(d1, syndic, new BigDecimal("12000"));
        ev.regler(e1, new BigDecimal("5000"), true, "POINTED");
        ev.regler(e1, new BigDecimal("900"), true, "CANCELED");
        ev.echeance(d1, Nature.FRAIS, true, new BigDecimal("3000"));
        ComFolder d2 = dossier(prefixe, clientVilleTexte());
        ComFolder d3 = dossier(prefixe, ev.client("C3-" + prefixe));
        rd.echeance(d3, syndic, new BigDecimal("4000"));
        ev.relire();
        return List.of(d1, d2, d3);
    }

    private List<Map<String, Object>> lignes(String colonnes) throws Exception {
        String corps = mockMvc.perform(get(LISTE).param(SEARCH, prefixe).param(SIZE, "100")
                        .param(COLONNES, CODE + "," + colonnes))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.content");
    }

    private static Map<String, Object> ligneDe(List<Map<String, Object>> lignes, ComFolder dossier) {
        return lignes.stream().filter(l -> dossier.getCode().equals(l.get(CODE))).findFirst()
                .orElseThrow(() -> new AssertionError("ligne absente : " + dossier.getCode()));
    }

    private static BigDecimal montant(Map<String, Object> ligne, String nom) {
        Object valeur = ligne.get(nom);
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void catalogue_septNouvellesColonnes_horsDefaut() throws Exception {
        String corps = mockMvc.perform(get(CATALOGUE)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Boolean> parDefaut = SEPT.stream()
                .<Boolean>flatMap(n -> JsonPath.<List<Boolean>>read(corps, "$[?(@.nom=='" + n + "')].parDefaut").stream())
                .toList();
        assertThat(parDefaut).hasSize(SEPT.size()).containsOnly(false);
    }

    @Test
    void catalogue_groupesTypesEtFonctions_dePlan32() throws Exception {
        mockMvc.perform(get(CATALOGUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='clientEmail')].groupe").value("CLIENT"))
                .andExpect(jsonPath("$[?(@.nom=='clientEmail')].type").value("TEXTE"))
                .andExpect(jsonPath("$[?(@.nom=='clientEmail')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='clientAdresse')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='clientPays')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='clientVille')].groupe").value("CLIENT"))
                .andExpect(jsonPath("$[?(@.nom=='clientVille')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='fraisSyndic')].groupe").value("FINANCIER"))
                .andExpect(jsonPath("$[?(@.nom=='fraisSyndic')].type").value("MONTANT"))
                .andExpect(jsonPath("$[?(@.nom=='fraisSyndic')].fonctions[0]").value("SUM"))
                .andExpect(jsonPath("$[?(@.nom=='fraisSyndicRegles')].fonctions[0]").value("SUM"))
                .andExpect(jsonPath("$[?(@.nom=='fraisSyndicReste')].fonctions[0]").value("SUM"));
    }

    // ------------------------------------------------------------------ colonnes client

    @Test
    void clientEmailEtPays_valeursDuClientPrincipal() throws Exception {
        List<ComFolder> d = jeuDeBase();

        List<Map<String, Object>> lignes = lignes("clientEmail,clientPays");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ligneDe(lignes, d.get(0))).containsEntry("clientEmail", "c1." + prefixe + "@exemple.ma")
                .containsEntry(CLIENT_PAYS, pays1.getDescription());
        soft.assertThat(ligneDe(lignes, d.get(1))).containsEntry(CLIENT_PAYS, pays2.getDescription());
        soft.assertThat(ligneDe(lignes, d.get(2)).get(CLIENT_PAYS)).isNull();
        soft.assertAll();
    }

    /** Adresse + compléments remplis ({@code address2} vide ici) ; le séparateur exact n'est pas figé. */
    @Test
    void clientAdresse_adressePuisComplementsRemplis() throws Exception {
        List<ComFolder> d = jeuDeBase();

        Object adresse = ligneDe(lignes("clientAdresse"), d.get(0)).get("clientAdresse");

        assertThat(String.valueOf(adresse)).startsWith("12 rue des Orangers").contains("Bâtiment C")
                .doesNotContain("null");
    }

    @Test
    void clientVille_referentielPrimeSurCity() throws Exception {
        List<ComFolder> d = jeuDeBase();

        assertThat(ligneDe(lignes(CLIENT_VILLE), d.get(0))).containsEntry(CLIENT_VILLE, casa.getDescription());
    }

    @Test
    void clientVille_sansReferentiel_city_sansVille_null() throws Exception {
        List<ComFolder> d = jeuDeBase();

        List<Map<String, Object>> lignes = lignes(CLIENT_VILLE);

        assertThat(ligneDe(lignes, d.get(1))).containsEntry(CLIENT_VILLE, "TEXTE-" + prefixe);
        assertThat(ligneDe(lignes, d.get(2)).get(CLIENT_VILLE)).isNull();
    }

    // ------------------------------------------------------------------ frais syndic (§3.3)

    /** Montant = échéances syndic au crédit ; réglé signé hors CANCELED ; reste = différence ; syndic ≤ frais. */
    @Test
    void fraisSyndic_montantRegleReste_reglementAnnuleExclu_inclusDansFrais() throws Exception {
        List<ComFolder> d = jeuDeBase();

        Map<String, Object> l1 = ligneDe(lignes("fraisSyndic,fraisSyndicRegles,fraisSyndicReste,frais"), d.get(0));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(montant(l1, FRAIS_SYNDIC)).isEqualByComparingTo("12000");
        soft.assertThat(montant(l1, FRAIS_SYNDIC_REGLES)).isEqualByComparingTo("5000");
        soft.assertThat(montant(l1, FRAIS_SYNDIC_RESTE)).isEqualByComparingTo("7000");
        soft.assertThat(montant(l1, "frais")).isEqualByComparingTo("15000");
        soft.assertAll();
    }

    @Test
    void fraisSyndic_dossierSansEcheanceSyndic_zeroPasNull() throws Exception {
        List<ComFolder> d = jeuDeBase();

        Map<String, Object> l2 = ligneDe(lignes("fraisSyndic,fraisSyndicRegles,fraisSyndicReste"), d.get(1));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(montant(l2, FRAIS_SYNDIC)).isNotNull().isEqualByComparingTo("0");
        soft.assertThat(montant(l2, FRAIS_SYNDIC_REGLES)).isNotNull().isEqualByComparingTo("0");
        soft.assertThat(montant(l2, FRAIS_SYNDIC_RESTE)).isNotNull().isEqualByComparingTo("0");
        soft.assertAll();
    }

    /** Invariant §3.3 sur chaque ligne : {@code fraisSyndic ≤ frais}. */
    @Test
    void fraisSyndic_jamaisSuperieurAuxFrais() throws Exception {
        jeuDeBase();

        List<Map<String, Object>> lignes = lignes("fraisSyndic,frais");

        assertThat(lignes).isNotEmpty().allSatisfy(l -> assertThat(montant(l, FRAIS_SYNDIC))
                .isLessThanOrEqualTo(montant(l, "frais")));
    }

    @Test
    void totaux_sommeLesTroisColonnesSyndic() throws Exception {
        jeuDeBase();

        mockMvc.perform(get(LISTE + "/totaux").param(SEARCH, prefixe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDossiers").value(3))
                .andExpect(jsonPath("$.sommes.fraisSyndic").value(16000.0))
                .andExpect(jsonPath("$.sommes.fraisSyndicRegles").value(5000.0))
                .andExpect(jsonPath("$.sommes.fraisSyndicReste").value(11000.0));
    }

    // ------------------------------------------------------------------ groupement

    @Test
    void groupBy_clientPays_unGroupeParPays() throws Exception {
        jeuDeBase();

        String corps = mockMvc.perform(get(LISTE + "/groupes").param("groupBy", CLIENT_PAYS)
                        .param(SEARCH, prefixe).param("inclureSansValeur", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> libelles = JsonPath.read(corps, "$.content[*].libelle");
        assertThat(libelles).contains(pays1.getDescription(), pays2.getDescription());
    }

    // ------------------------------------------------------------------ aucune requête par ligne

    /**
     * Coût des sept colonnes = (requêtes avec elles) − (requêtes avec {@code code} seul), mesuré sur 2 puis 50 dossiers :
     * il ne croît pas avec le nombre de lignes (ville lue par lot d'ids, syndic par le calcul de la page).
     */
    @Test
    void aucuneRequeteParLigne_coutDesColonnesIndependantDuNombreDeLignes() throws Exception {
        String petit = prefixe + "S";
        String grand = prefixe + "G";
        peupler(petit, 2);
        peupler(grand, 50);
        ev.relire();

        long coutPetit = requetes(petit, String.join(",", SEPT)) - requetes(petit, "");
        long coutGrand = requetes(grand, String.join(",", SEPT)) - requetes(grand, "");

        assertThat(coutGrand).isLessThanOrEqualTo(coutPetit + 2);
    }

    private void peupler(String racine, int nombre) {
        for (int i = 0; i < nombre; i++) {
            CfgTier client = ev.client(racine + "-C" + i);
            client.setAdmCountry(pays1);
            CfgTier sauve = tiers.save(client);
            situer(sauve, ville(racine + "V" + i));
            rd.echeance(dossier(racine, sauve), syndic, new BigDecimal("1000"));
        }
    }

    private long requetes(String recherche, String colonnes) throws Exception {
        Statistics statistiques = emf.unwrap(SessionFactory.class).getStatistics();
        boolean active = statistiques.isStatisticsEnabled();
        statistiques.setStatisticsEnabled(true);
        try {
            ev.relire();
            statistiques.clear();
            mockMvc.perform(get(LISTE).param(SEARCH, recherche).param(SIZE, "100")
                            .param(COLONNES, colonnes.isEmpty() ? CODE : CODE + "," + colonnes))
                    .andExpect(status().isOk());
            return statistiques.getPrepareStatementCount();
        } finally {
            statistiques.setStatisticsEnabled(active);
        }
    }
}
