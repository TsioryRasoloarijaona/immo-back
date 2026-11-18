package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsProjetSupport.detail;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * N4b — Prévisions chantier de la fiche projet (FEATURE-n4-previsions-chantier.md B1-B6). Chaque refus porte un appel témoin
 * 200 : une route absente répond déjà 404, et 403 au non-administrateur (règle d'URL de la config projet).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsChantierTest {

    private static final String ROUTE = FixtureProjetConfig.PROJET + "/previsions-chantier";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String COMMERCIAL = "Commercial";

    // Noms du fil (spec « Endpoints »)
    private static final String LIGNES = "lignes";
    private static final String TYPE_TRAVAUX_ID = "typeTravauxId";
    private static final String QUANTITE = "quantite";
    private static final String PRIX_UNITAIRE_HT = "prixUnitaireHT";
    private static final String TVA = "tva";
    private static final String NOTES = "notes";
    private static final String TOTAL_HT = "totalHT";
    private static final String TOTAL_TTC = "totalTTC";
    private static final String UNITE = "unite";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ACTIF = "$.actif";

    private static final String LIGNES_DU_PROJET = "SELECT COUNT(*) FROM PrjProjectBuildingPrevision WHERE PrjProjectId = ?";
    private static final String MESSAGE_SANS_NIVELLEMENT = "Le projet n'a pas de nivellement";

    // yesRemsTest : GrhWorkedDayType absente, PrjProjectBuildingPrevision réduite à 2 colonnes (types de prod, ci/schema/197)
    private static final String SCHEMA = """
            IF OBJECT_ID(N'dbo.GrhWorkedDayType', N'U') IS NULL
                CREATE TABLE dbo.GrhWorkedDayType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                    Description nvarchar(255) NULL, Notes nvarchar(500) NULL, UserCreate nvarchar(255) NULL,
                    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                    GroupDescription nvarchar(255) NULL, Unity nvarchar(255) NULL, UnitPriceHT decimal(26, 6) NULL,
                    VatRatio decimal(26, 6) NULL, Unity1 nvarchar(255) NULL, UnitPriceHT1 decimal(26, 6) NULL,
                    VatRatio1 decimal(26, 6) NULL);
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'GrhWorkedDayTypeId') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD GrhWorkedDayTypeId uniqueidentifier NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Quantity') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Quantity decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UnitPriceHT') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UnitPriceHT decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'VatRatio') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD VatRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Notes') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Notes nvarchar(1000) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreate') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreateDateTime') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreateDateTime datetime NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdate') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdate nvarchar(255) NULL;
            IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdateDateTime') IS NULL
                ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdateDateTime datetime NULL;""";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private FixtureProjetConfig config;
    @Autowired private FixtureLotsParite parite;

    private String jeton;
    private UUID societe;
    private int rang;

    /** DDL hors transaction du test (autocommit) : un DDL dans la transaction serait annulé avec elle. */
    @BeforeAll
    static void schema(@Autowired DataSource dataSource) throws SQLException {
        try (Connection connexion = dataSource.getConnection(); Statement ordre = connexion.createStatement()) {
            connexion.setAutoCommit(true);
            ordre.execute(SCHEMA);
        }
    }

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        config.parametrage();
        societe = parite.entiteJuridique("PCH-" + jeton);
    }

    // ------------------------------------------------------------------ fixtures

    private UUID projet(boolean avecNivellement) {
        rang++;
        UUID projet = config.projet("PCH" + rang + "-" + jeton, societe);
        if (avecNivellement) {
            config.liens(projet, config.nivellement("NIV" + rang + "-" + jeton), null, null);
        }
        return projet;
    }

    private RequestPostProcessor admin(UUID... projets) {
        rang++;
        return connecte(parite.administrateur("pch.adm" + rang + "." + jeton, projets), ADMINISTRATEUR);
    }

    private RequestPostProcessor sansDroit(UUID... projets) {
        rang++;
        return connecte(parite.avecDroits("pch.sd" + rang + "." + jeton, List.of(), projets), COMMERCIAL);
    }

    /** Type de travaux ({@code GrhWorkedDayType}) : unité, PU HT, TVA en ratio. */
    private UUID typeTravaux(String unite, String prixHT, String tvaRatio) {
        rang++;
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO GrhWorkedDayType (PKey, Code, Description, Unity, UnitPriceHT, VatRatio)
                VALUES (?, ?, ?, ?, ?, ?)""",
                id.toString(), "TT" + rang + "-" + jeton, "Type " + rang + " " + jeton, unite,
                new BigDecimal(prixHT), new BigDecimal(tvaRatio));
        return id;
    }

    /** Ligne de grille écrite en base (TVA en ratio, comme OMRAN). */
    private UUID ligneEnBase(UUID projet, UUID type, String quantite, String prixHT, String tvaRatio, String notes) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjProjectBuildingPrevision (PKey, PrjProjectId, GrhWorkedDayTypeId, Quantity, UnitPriceHT, VatRatio, Notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), projet.toString(), type.toString(), new BigDecimal(quantite), new BigDecimal(prixHT),
                new BigDecimal(tvaRatio), notes);
        return id;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    private static CorpsJson ligne(UUID type, String quantite, String prixHT, String tvaPourcent, String notes) {
        return corps().avec(TYPE_TRAVAUX_ID, type).avec(QUANTITE, new BigDecimal(quantite))
                .avec(PRIX_UNITAIRE_HT, new BigDecimal(prixHT)).avec(TVA, new BigDecimal(tvaPourcent)).avec(NOTES, notes);
    }

    // ------------------------------------------------------------------ appels

    private Reponse lire(RequestPostProcessor qui, UUID projet) throws Exception {
        return executer(get(ROUTE, projet).with(qui));
    }

    private Reponse enregistrer(RequestPostProcessor qui, UUID projet, CorpsJson... lignes) throws Exception {
        return enregistrerCorps(qui, projet, corps().avec(LIGNES, List.of(lignes)));
    }

    private Reponse enregistrerCorps(RequestPostProcessor qui, UUID projet, CorpsJson corps) throws Exception {
        return executer(put(ROUTE, projet).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private Reponse executer(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private static Reponse ok(Reponse r) {
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    /** Champ de la ligne du type {@code type} dans {@code lignes} (ou {@code typesTravaux}) ; une seule ligne attendue. */
    private static Object champ(Reponse r, String liste, String cle, UUID type, String champ) {
        List<?> valeurs = (List<?>) r.valeur("$." + liste + "[?(@." + cle + " == '" + type + "')]." + champ);
        assertThat(valeurs).as(r.corps()).hasSize(1);
        return valeurs.get(0);
    }

    private static BigDecimal nombre(Reponse r, UUID type, String champ) {
        return new BigDecimal(String.valueOf(champ(r, LIGNES, TYPE_TRAVAUX_ID, type, champ)));
    }

    // ------------------------------------------------------------------ B1 : droit BuildingPrevision

    @Test
    void b1_sansDroit_get_403() throws Exception {
        UUID projet = projet(true);
        ok(lire(admin(projet), projet));

        Reponse r = lire(sansDroit(projet), projet);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void b1_sansDroit_put_403() throws Exception {
        UUID projet = projet(true);
        UUID type = typeTravaux("H", "100", "0.2");
        ok(enregistrer(admin(projet), projet, ligne(type, "1", "100", "20", null)));

        Reponse r = enregistrer(sansDroit(projet), projet, ligne(type, "2", "100", "20", null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ B2 : projet inexistant

    @Test
    void b2_projetInconnu_get_404_memeMessageQueLaFicheProjet() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID inconnu = UUID.randomUUID();
        ok(lire(qui, projet));
        Reponse fiche = executer(get(FixtureProjetConfig.PROJET, inconnu).with(qui));

        Reponse r = lire(qui, inconnu);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        s.assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
        s.assertAll();
    }

    @Test
    void b2_projetInconnu_put_404_memeMessageQueLaFicheProjet() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "100", "0.2");
        UUID inconnu = UUID.randomUUID();
        ok(enregistrer(qui, projet, ligne(type, "1", "100", "20", null)));
        Reponse fiche = executer(get(FixtureProjetConfig.PROJET, inconnu).with(qui));

        Reponse r = enregistrer(qui, inconnu, ligne(type, "1", "100", "20", null));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        s.assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
        s.assertAll();
    }

    // ------------------------------------------------------------------ B3 : nivellement

    @Test
    void b3_sansNivellement_get_200_actifFaux_enTeteDuProjet() throws Exception {
        UUID projet = projet(false);
        String code = jdbc.queryForObject("SELECT Code FROM PrjProject WHERE PKey = ?", String.class,
                projet.toString());

        Reponse r = ok(lire(admin(projet), projet));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.valeur(ACTIF)).isEqualTo(false);
        s.assertThat(String.valueOf(r.valeur("$.projetId"))).isEqualToIgnoringCase(projet.toString());
        s.assertThat(r.valeur("$." + CODE)).isEqualTo(code);
        s.assertThat(r.valeur("$." + DESCRIPTION)).isEqualTo("Projet " + code);
        s.assertAll();
    }

    @Test
    void b3_avecNivellement_get_actifVrai() throws Exception {
        UUID projet = projet(true);

        Reponse r = ok(lire(admin(projet), projet));

        assertThat(r.valeur(ACTIF)).isEqualTo(true);
    }

    @Test
    void b3_sansNivellement_put_409() throws Exception {
        UUID avec = projet(true);
        UUID sans = projet(false);
        RequestPostProcessor qui = admin(avec, sans);
        UUID type = typeTravaux("H", "100", "0.2");
        ok(enregistrer(qui, avec, ligne(type, "1", "100", "20", null)));

        Reponse r = enregistrer(qui, sans, ligne(type, "1", "100", "20", null));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        s.assertThat(detail(r)).contains(MESSAGE_SANS_NIVELLEMENT);
        s.assertAll();
    }

    // ------------------------------------------------------------------ B4 : type de travaux

    @Test
    void b4_deuxLignesDuMemeType_400() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "100", "0.2");
        ok(enregistrer(qui, projet, ligne(type, "1", "100", "20", null)));

        Reponse r = enregistrer(qui, projet, ligne(type, "1", "100", "20", null), ligne(type, "2", "50", "20", null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void b4_typeInconnu_400() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "100", "0.2");
        ok(enregistrer(qui, projet, ligne(type, "1", "100", "20", null)));

        Reponse r = enregistrer(qui, projet, ligne(UUID.randomUUID(), "1", "100", "20", null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @ParameterizedTest(name = "champ omis = {0}")
    @ValueSource(booleans = {true, false})
    void b4_typeObligatoire_400(boolean omis) throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "100", "0.2");
        ok(enregistrer(qui, projet, ligne(type, "1", "100", "20", null)));
        CorpsJson sansType = omis ? ligne(type, "1", "100", "20", null).sans(TYPE_TRAVAUX_ID)
                : ligne(type, "1", "100", "20", null).avec(TYPE_TRAVAUX_ID, null);

        Reponse r = enregistrer(qui, projet, sansType);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ B5 : montants calculés, TVA en %

    @Test
    void b5_lecture_totauxDeLaLigne_tvaEnPourcent_libellesDuType() throws Exception {
        UUID projet = projet(true);
        UUID type = typeTravaux("Jour", "99", "0.1");
        ligneEnBase(projet, type, "3", "150.5", "0.2", "Equipe A");
        String codeType = jdbc.queryForObject("SELECT Code FROM GrhWorkedDayType WHERE PKey = ?", String.class,
                type.toString());

        Reponse r = ok(lire(admin(projet), projet));

        // HT = 150,5 × 3 = 451,5 ; TTC = 451,5 × (1 + 0,2) = 541,8 ; PU et TVA de la ligne, pas du type
        SoftAssertions s = new SoftAssertions();
        s.assertThat(nombre(r, type, QUANTITE)).isEqualByComparingTo("3");
        s.assertThat(nombre(r, type, PRIX_UNITAIRE_HT)).isEqualByComparingTo("150.5");
        s.assertThat(nombre(r, type, TVA)).isEqualByComparingTo("20");
        s.assertThat(nombre(r, type, TOTAL_HT)).isEqualByComparingTo("451.5");
        s.assertThat(nombre(r, type, TOTAL_TTC)).isEqualByComparingTo("541.8");
        s.assertThat(champ(r, LIGNES, TYPE_TRAVAUX_ID, type, NOTES)).isEqualTo("Equipe A");
        s.assertThat(champ(r, LIGNES, TYPE_TRAVAUX_ID, type, CODE)).isEqualTo(codeType);
        s.assertThat(champ(r, LIGNES, TYPE_TRAVAUX_ID, type, UNITE)).isEqualTo("Jour");
        s.assertAll();
    }

    @Test
    void b5_b7_typesTravaux_puHtEtTvaEnPourcent() throws Exception {
        UUID projet = projet(true);
        UUID type = typeTravaux("Jour", "99", "0.1");

        Reponse r = ok(lire(admin(projet), projet));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(new BigDecimal(String.valueOf(champ(r, "typesTravaux", "id", type, PRIX_UNITAIRE_HT))))
                .isEqualByComparingTo("99");
        s.assertThat(new BigDecimal(String.valueOf(champ(r, "typesTravaux", "id", type, TVA))))
                .isEqualByComparingTo("10");
        s.assertThat(champ(r, "typesTravaux", "id", type, UNITE)).isEqualTo("Jour");
        s.assertAll();
    }

    @Test
    void b5_ecriture_tvaPourcentStockeeEnRatio() throws Exception {
        UUID projet = projet(true);
        UUID type = typeTravaux("H", "100", "0.2");

        ok(enregistrer(admin(projet), projet, ligne(type, "2.5", "100", "20", null)));

        assertThat(compter(LIGNES_DU_PROJET + " AND GrhWorkedDayTypeId = ? AND VatRatio = 0.2 AND Quantity = 2.5"
                + " AND UnitPriceHT = 100", projet.toString(), type.toString())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ B6 : remplacement de la grille

    @Test
    void b6_allerRetour_putPuisGet() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "80", "0.1");

        ok(enregistrer(qui, projet, ligne(type, "2.5", "100", "20", "Equipe B")));
        Reponse r = ok(lire(qui, projet));

        // HT = 100 × 2,5 = 250 ; TTC = 250 × 1,2 = 300
        SoftAssertions s = new SoftAssertions();
        s.assertThat((List<?>) r.valeur("$." + LIGNES)).hasSize(1);
        s.assertThat(nombre(r, type, QUANTITE)).isEqualByComparingTo("2.5");
        s.assertThat(nombre(r, type, PRIX_UNITAIRE_HT)).isEqualByComparingTo("100");
        s.assertThat(nombre(r, type, TVA)).isEqualByComparingTo("20");
        s.assertThat(nombre(r, type, TOTAL_HT)).isEqualByComparingTo("250");
        s.assertThat(nombre(r, type, TOTAL_TTC)).isEqualByComparingTo("300");
        s.assertThat(champ(r, LIGNES, TYPE_TRAVAUX_ID, type, NOTES)).isEqualTo("Equipe B");
        s.assertAll();
    }

    @Test
    void b6_remplacement_deuxPuisUne_ligneConserveeMiseAJourEnPlace() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID gardee = typeTravaux("H", "100", "0.2");
        UUID retiree = typeTravaux("J", "500", "0.2");
        ok(enregistrer(qui, projet, ligne(gardee, "2", "100", "20", null), ligne(retiree, "5", "500", "20", null)));
        String idAvant = jdbc.queryForObject(
                "SELECT CAST(PKey AS varchar(36)) FROM PrjProjectBuildingPrevision WHERE PrjProjectId = ? AND GrhWorkedDayTypeId = ?",
                String.class, projet.toString(), gardee.toString());

        ok(enregistrer(qui, projet, ligne(gardee, "7", "110", "10", "maj")));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(compter(LIGNES_DU_PROJET, projet.toString())).isEqualTo(1);
        s.assertThat(compter(LIGNES_DU_PROJET + " AND PKey = ? AND GrhWorkedDayTypeId = ? AND Quantity = 7"
                        + " AND UnitPriceHT = 110 AND VatRatio = 0.1 AND Notes = N'maj'",
                projet.toString(), idAvant, gardee.toString())).isEqualTo(1);
        s.assertAll();
    }

    @Test
    void b6_grilleVide_supprimeToutesLesLignes_autreProjetIntact() throws Exception {
        UUID projet = projet(true);
        UUID autre = projet(true);
        UUID type = typeTravaux("H", "100", "0.2");
        ligneEnBase(projet, type, "1", "100", "0.2", null);
        ligneEnBase(autre, type, "4", "100", "0.2", null);

        ok(enregistrer(admin(projet, autre), projet));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(compter(LIGNES_DU_PROJET, projet.toString())).isZero();
        s.assertThat(compter(LIGNES_DU_PROJET + " AND Quantity = 4", autre.toString())).isEqualTo(1);
        s.assertAll();
    }

    @Test
    void b6_audit_creationPuisMiseAJour() throws Exception {
        UUID projet = projet(true);
        RequestPostProcessor qui = admin(projet);
        UUID type = typeTravaux("H", "100", "0.2");
        String filtre = LIGNES_DU_PROJET + " AND GrhWorkedDayTypeId = ?";

        ok(enregistrer(qui, projet, ligne(type, "1", "100", "20", null)));
        int creee = compter(filtre + " AND UserCreate IS NOT NULL AND UserCreate <> N'' AND UserCreateDateTime IS NOT NULL",
                projet.toString(), type.toString());
        ok(enregistrer(qui, projet, ligne(type, "3", "100", "20", null)));
        int modifiee = compter(filtre + " AND UserUpdate IS NOT NULL AND UserUpdate <> N'' AND UserUpdateDateTime IS NOT NULL",
                projet.toString(), type.toString());

        assertThat(new int[]{creee, modifiee}).containsExactly(1, 1);
    }
}
