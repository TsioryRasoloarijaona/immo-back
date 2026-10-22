package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.adm.AdmProfessionRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.REPARTITION_CLIENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 13 RÉPARTITION DES CLIENTS (FEATURE-tdb-vente-rapports-socle.md §7 ; décision 3 du manager : naissance absente ⇒
 * « Non définie »). Période 2020, une tranche du test :
 * <pre>
 *   c1 : deux dossiers vendus (profession P1, né 1980)      c2 : un dossier vendu (né 1980), co-acquéreur c3
 *   c3 : co-acquéreur seulement (profession P3, né 1980)    c4 : un dossier vendu, SANS date de naissance
 *   c5 : un dossier vendu, né il y a 30 ans civils
 *   exclus : c6 (avance 2019), c7 (statut non vendu), c8 (tiers non CLIENT)
 * </pre>
 * Population attendue : 5 clients distincts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionClientsRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String DU = "du";
    private static final String AU = "au";
    private static final String NB_CLIENTS = "nbClients";
    private static final String LIBELLE = "libelle";
    private static final String NON_DEFINIE = "Non définie";
    private static final List<String> SECTIONS = List.of("professions", "salaires", "ages", "villes", "nationalites");
    private static final LocalDateTime AVANCE_2020 = LocalDateTime.of(2020, 6, 15, 0, 0);
    private static final LocalDateTime NE_EN_1980 = LocalDateTime.of(1980, 3, 1, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private StrStatusRepository statuts;
    @Autowired private CfgTierRepository tiers;
    @Autowired private CfgTierTypeRepository typesTiers;
    @Autowired private ComFolderCustomerRepository coAcquereurs;
    @Autowired private AdmProfessionRepository professions;
    @Autowired private AdmCityRepository villes;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private String prefixe;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus vendu;
    private CfgTierType typeClient;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RCL");
        fixture.tables();
        fixture.rapportVente(REPARTITION_CLIENTS, "REPARTITION DES CLIENTS", "", 1100);
        libre = connecte(fixture.compte("rcl." + prefixe, fixture.role("Libre " + prefixe, false)));
        tranche = ev.tranche("TR-" + prefixe, ev.projet("P-" + prefixe, true), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        vendu = ev.statutEnCours("VENDU-" + prefixe);
        vendu.setVenteEffective(true);
        vendu = statuts.save(vendu);
        typeClient = typesTiers.findByCode("CLI").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("CLI");
            t.setDescription("CLIENT");
            return typesTiers.save(t);
        });
    }

    private CfgTier client(String nom, LocalDateTime naissance, AdmProfession profession) {
        CfgTier tier = ev.client(nom + "-" + prefixe);
        tier.setCfgTierType(typeClient);
        tier.setBirthDate(naissance);
        tier.setAdmProfession(profession);
        return tiers.save(tier);
    }

    private ComFolder concretiser(CfgTier acquereur, StrStatus statut, LocalDateTime avance) {
        numero++;
        ComFolder dossier = ev.vendre(ev.article(prefixe + "-" + numero, tranche, categorie, type),
                "DOS-" + prefixe + "-" + numero, statut, acquereur, null);
        dossier.setAdvanceDate(avance);
        return ev.enregistrer(dossier);
    }

    private AdmProfession profession(String code) {
        AdmProfession p = new AdmProfession();
        p.setId(UUID.randomUUID());
        p.setCode(code + "-" + prefixe);
        p.setDescription(code + "-" + prefixe);
        return professions.save(p);
    }

    /** Jeu de la Javadoc de classe. */
    private void jeuDeBase() {
        CfgTier c1 = client("C1", NE_EN_1980, profession("P1"));
        concretiser(c1, vendu, AVANCE_2020);
        concretiser(c1, vendu, AVANCE_2020.plusMonths(1));
        ComFolder dossierC2 = concretiser(client("C2", NE_EN_1980, null), vendu, AVANCE_2020);
        ComFolderCustomer co = new ComFolderCustomer();
        co.setId(UUID.randomUUID());
        co.setComFolder(dossierC2);
        co.setCfgTier(client("C3", NE_EN_1980, profession("P3")));
        coAcquereurs.save(co);
        concretiser(client("C4", null, null), vendu, AVANCE_2020);
        int annee = LocalDate.now(clock).getYear();
        concretiser(client("C5", LocalDateTime.of(annee - 30, 1, 1, 0, 0), null), vendu, AVANCE_2020);

        concretiser(client("C6", NE_EN_1980, null), vendu, LocalDateTime.of(2019, 12, 31, 0, 0));
        concretiser(client("C7", NE_EN_1980, null), ev.statutEnCours("RES-" + prefixe), AVANCE_2020);
        concretiser(ev.client("C8-" + prefixe), vendu, AVANCE_2020);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder periode(String du, String au) {
        return get(resultat(VENTE, REPARTITION_CLIENTS)).param(DU, du).param(AU, au)
                .param(TRANCHE_ID, tranche.getId().toString()).with(libre);
    }

    private List<Map<String, Object>> sections2020() throws Exception {
        return appels.sections(periode("2020-01-01", "2020-12-31"));
    }

    private static BigDecimal effectif(Map<String, Object> section, String libelle) {
        List<Map<String, Object>> trouvees = lignesOu(section, LIBELLE, libelle);
        assertThat(trouvees).as("ligne « %s »", libelle).hasSize(1);
        return nombre(trouvees.get(0).get(NB_CLIENTS));
    }

    // ------------------------------------------------------------------ population et totaux

    @Test
    void cinqSections_totauxEgaux_auNombreDeClientsDistincts() throws Exception {
        jeuDeBase();
        List<Map<String, Object>> sections = sections2020();

        SoftAssertions soft = new SoftAssertions();
        for (String id : SECTIONS) {
            Map<String, Object> s = section(sections, id);
            soft.assertThat(nombre(totaux(s).get(NB_CLIENTS))).as("total %s", id).isEqualByComparingTo("5");
            soft.assertThat(somme(lignes(s), NB_CLIENTS)).as("lignes %s", id).isEqualByComparingTo("5");
        }
        soft.assertAll();
    }

    @Test
    void clientADeuxDossiers_compteUneFois() throws Exception {
        jeuDeBase();

        assertThat(effectif(section(sections2020(), "professions"), "P1-" + prefixe)).isEqualByComparingTo("1");
    }

    @Test
    void coAcquereur_compte() throws Exception {
        jeuDeBase();

        assertThat(effectif(section(sections2020(), "professions"), "P3-" + prefixe)).isEqualByComparingTo("1");
    }

    @Test
    void professionAbsente_nonDefinie() throws Exception {
        jeuDeBase();

        assertThat(effectif(section(sections2020(), "professions"), NON_DEFINIE)).isEqualByComparingTo("3");
    }

    @Test
    void ages_anneesCiviles_etNaissanceAbsente_nonDefinie() throws Exception {
        jeuDeBase();
        Map<String, Object> ages = section(sections2020(), "ages");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(effectif(ages, NON_DEFINIE)).isEqualByComparingTo("1");
        soft.assertThat(effectif(ages, "Entre 21 et 40 ans")).isEqualByComparingTo("1");
        soft.assertThat(effectif(ages, "Entre 41 et 60 ans")).isEqualByComparingTo("3");
        soft.assertThat(lignesOu(ages, LIBELLE, "Moins de 20 ans")).isEmpty();
        soft.assertAll();
    }

    @Test
    void part_sommeCent_parSection() throws Exception {
        jeuDeBase();
        List<Map<String, Object>> sections = sections2020();

        SoftAssertions soft = new SoftAssertions();
        for (String id : SECTIONS) {
            soft.assertThat(somme(lignes(section(sections, id)), "part").doubleValue()).as(id)
                    .isCloseTo(100.0, within(0.5));
        }
        soft.assertAll();
    }

    /** B1 (« Ajouts au socle ») : part en 0–100 (1 client sur 5 = 20) ; pas de moyenne pondérée déclarée ⇒ pas de total. */
    @Test
    void b1_part_echelle0a100_sansTotal() throws Exception {
        jeuDeBase();
        Map<String, Object> professionsSection = section(sections2020(), "professions");

        List<Map<String, Object>> p1 = lignesOu(professionsSection, LIBELLE, "P1-" + prefixe);
        assertThat(p1).hasSize(1);
        assertThat(nombre(p1.get(0).get("part")).doubleValue()).isCloseTo(20.0, within(0.05));
        assertThat(totaux(professionsSection)).doesNotContainKey("part");
    }

    @Test
    void periodeSansDossier_sectionsVides_totauxNull_avertissement() throws Exception {
        jeuDeBase();

        String corps = appels.corps(periode("1950-01-01", "1950-12-31"));

        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(sections).hasSize(SECTIONS.size())
                .allSatisfy(s -> assertThat(lignes(s)).isEmpty())
                .allSatisfy(s -> assertThat(totaux(s)).isNull());
        assertThat(avertissements).contains("Aucun dossier concrétisé sur la période");
    }

    @Test
    void graphiques_unCamembertParSection() throws Exception {
        jeuDeBase();
        List<Map<String, Object>> sections = sections2020();

        SoftAssertions soft = new SoftAssertions();
        for (String id : SECTIONS) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> graphiques = (List<Map<String, Object>>) section(sections, id).get("graphiques");
            soft.assertThat(graphiques).as(id).singleElement()
                    .satisfies(g -> assertThat(g).containsEntry("type", "SECTEURS")
                            .containsEntry("colonneCategorie", LIBELLE)
                            .containsEntry("colonnesValeurs", List.of(NB_CLIENTS)));
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ villes < 1 %

    @Test
    void villes_moinsDUnPourcent_regroupeesEnAutres() throws Exception {
        AdmCity grande = ville("GRANDE");
        AdmCity petite = ville("PETITE");
        for (int i = 0; i < 100; i++) {
            situer(concretiserClient("V" + i), grande);
        }
        situer(concretiserClient("VP"), petite);
        fixture.relire();

        Map<String, Object> villesSection = section(sections2020(), "villes");

        assertThat(lignesOu(villesSection, LIBELLE, petite.getDescription())).isEmpty();
        assertThat(effectif(villesSection, "AUTRES < 1%")).isEqualByComparingTo("1");
        assertThat(effectif(villesSection, grande.getDescription())).isEqualByComparingTo("100");
    }

    private CfgTier concretiserClient(String nom) {
        CfgTier c = client(nom, NE_EN_1980, null);
        concretiser(c, vendu, AVANCE_2020);
        return c;
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
}
