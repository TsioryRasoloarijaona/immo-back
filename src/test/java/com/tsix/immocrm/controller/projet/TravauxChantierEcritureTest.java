package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TRAVAUX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/** Saisie d'un jour travaillé (FEATURE-n2-travaux-chantier.md D1, D7, D8, R1, R3, R4, R5). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierEcritureTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String PAR_CODE = "SELECT COUNT(*) FROM GrhWorkedDay WHERE Code = ?";
    private static final String COLONNE_DU_CODE = "SELECT %s FROM GrhWorkedDay WHERE Code = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID typeA;
    private UUID typeB;
    private RequestPostProcessor editeur;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GRE-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        typeA = fixture.type("TA-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        typeB = fixture.type("TB-" + jeton, new ValeursType("M2", "80", "0.14", "U", "5", "0.07"));
        editeur = fixture.compte(jeton, false, List.of(chantier.projet()), DROIT_LIRE, DROIT_MODIFIER, DROIT_SUPPRIMER)
                .connexion();
    }

    private Map<String, Object> corps(String code) {
        return AppelsTravauxChantier.ecriture(chantier.societe(), chantier.projet(), typeA, zone, DATE, code);
    }

    private String colonne(String nom, String code) {
        return fixture.lire(COLONNE_DU_CODE.formatted(nom), code);
    }

    private BigDecimal decimal(String nom, String code) {
        return fixture.montant(COLONNE_DU_CODE.formatted(nom), code);
    }

    private UUID jourSeme(String code, UUID employe) {
        return fixture.jour(new Jour(chantier.societe(), null, typeA, zone, EN_COURS, DATE.atStartOfDay(), code, employe));
    }

    // ------------------------------------------------------------------ D1 / R1 création

    /** D1 : statut de départ = statut « Nouveau » de l'entité (En cours) ; aussi témoin des 400 de R1. */
    @Test
    void d1_creation_201_statutDeDepart() throws Exception {
        String code = "C-" + jeton;

        Reponse r = appels.creer(corps(code), editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertAll(
                () -> assertThat(fixture.compter(PAR_CODE, code)).isEqualTo(1),
                () -> assertThat(colonne("CAST(StrStatusId AS varchar(36))", code)).isEqualToIgnoringCase(EN_COURS.toString()),
                () -> assertThat(colonne("CAST(GrhWorkedDayTypeId AS varchar(36))", code))
                        .isEqualToIgnoringCase(typeA.toString()),
                () -> assertThat(colonne("CAST(CfgProjectZoneId AS varchar(36))", code)).isEqualToIgnoringCase(zone.toString()));
    }

    /** R1 : société, type, zone, date, quantité, quantité 1 obligatoires ; 400 qui nomme le champ, rien d'écrit. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"societeId, société|entité", "typeId, type", "zoneId, zone", "date, date", "quantite, quantité",
            "quantite1, quantité 1"})
    void r1_obligatoireManquant_400(String champ, String libelles) throws Exception {
        String code = "O-" + jeton;
        Map<String, Object> incomplet = corps(code);
        incomplet.remove(champ);

        Reponse r = appels.creer(incomplet, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        String detail = String.valueOf(r.<Object>lire("$.detail")).toLowerCase();
        assertThat(detail).as("le détail nomme le champ (nom du fil ou libellé)")
                .containsAnyOf(java.util.stream.Stream.concat(java.util.stream.Stream.of(champ.toLowerCase()),
                        java.util.Arrays.stream(libelles.split("\\|"))).toArray(String[]::new));
        assertThat(fixture.compter(PAR_CODE, code)).isZero();
    }

    /** D7 : {@code Code} libre, non unique (aucune séquence). */
    @Test
    void d7_code_nonUnique_deuxCreations201() throws Exception {
        String code = "DBL-" + jeton;

        Reponse premier = appels.creer(corps(code), editeur);
        Reponse second = appels.creer(corps(code), editeur);

        assertThat(premier.statut()).as(premier.corps()).isEqualTo(201);
        assertThat(second.statut()).as(second.corps()).isEqualTo(201);
        assertThat(fixture.compter(PAR_CODE, code)).isEqualTo(2);
    }

    // ------------------------------------------------------------------ D8 / R3 valeurs du type

    @Test
    void d8_creation_typeCopieUnitesPrixTva() throws Exception {
        String code = "T-" + jeton;

        Reponse r = appels.creer(corps(code), editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertAll(
                () -> assertThat(colonne("Unity", code)).isEqualTo("J"),
                () -> assertThat(decimal("UnitPriceHT", code)).isEqualByComparingTo("150"),
                () -> assertThat(decimal("VatRatio", code)).isEqualByComparingTo("0.2"),
                () -> assertThat(colonne("Unity1", code)).isEqualTo("H"),
                () -> assertThat(decimal("UnitPriceHT1", code)).isEqualByComparingTo("20"),
                () -> assertThat(decimal("VatRatio1", code)).isEqualByComparingTo("0.1"));
    }

    @Test
    void r3_creation_valeursDuCorpsPriment() throws Exception {
        String code = "P-" + jeton;
        Map<String, Object> saisie = corps(code);
        saisie.put("unite", "SAC");
        saisie.put("prixUnitaireHT", 99);
        saisie.put("tva", 0.1);
        saisie.put("unite1", "KG");
        saisie.put("prixUnitaireHT1", 7);
        saisie.put("tva1", 0.2);

        Reponse r = appels.creer(saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertAll(
                () -> assertThat(colonne("Unity", code)).isEqualTo("SAC"),
                () -> assertThat(decimal("UnitPriceHT", code)).isEqualByComparingTo("99"),
                () -> assertThat(decimal("VatRatio", code)).isEqualByComparingTo("0.1"),
                () -> assertThat(colonne("Unity1", code)).isEqualTo("KG"),
                () -> assertThat(decimal("UnitPriceHT1", code)).isEqualByComparingTo("7"),
                () -> assertThat(decimal("VatRatio1", code)).isEqualByComparingTo("0.2"));
    }

    /** D8 : changer le type au PUT recopie les valeurs du nouveau type (le corps ne prime que sans changement de type). */
    @Test
    void d8_put_changementDeType_copieLeNouveauType() throws Exception {
        String code = "CT-" + jeton;
        UUID jour = jourSeme(code, null);
        Map<String, Object> saisie = AppelsTravauxChantier.ecriture(chantier.societe(), null, typeB, zone, DATE, code);
        saisie.put("unite", "J");
        saisie.put("prixUnitaireHT", 150.5);
        saisie.put("tva", 0.2);

        Reponse r = appels.modifier(jour, saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(colonne("Unity", code)).isEqualTo("M2"),
                () -> assertThat(decimal("UnitPriceHT", code)).isEqualByComparingTo("80"),
                () -> assertThat(decimal("VatRatio", code)).isEqualByComparingTo("0.14"),
                () -> assertThat(colonne("Unity1", code)).isEqualTo("U"),
                () -> assertThat(decimal("UnitPriceHT1", code)).isEqualByComparingTo("5"),
                () -> assertThat(decimal("VatRatio1", code)).isEqualByComparingTo("0.07"));
    }

    @Test
    void r3_put_memeType_valeursDuCorpsPriment() throws Exception {
        String code = "MT-" + jeton;
        UUID jour = jourSeme(code, null);
        Map<String, Object> saisie = AppelsTravauxChantier.ecriture(chantier.societe(), null, typeA, zone, DATE, code);
        saisie.put("unite", "SAC");
        saisie.put("prixUnitaireHT", 42);
        saisie.put("tva", 0.1);

        Reponse r = appels.modifier(jour, saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(colonne("Unity", code)).isEqualTo("SAC"),
                () -> assertThat(decimal("UnitPriceHT", code)).isEqualByComparingTo("42"),
                () -> assertThat(decimal("VatRatio", code)).isEqualByComparingTo("0.1"));
    }

    // ------------------------------------------------------------------ R4 société → personnel

    @Test
    void r4_put_changementDeSociete_videLePersonnel() throws Exception {
        String code = "R4-" + jeton;
        UUID employe = fixture.tiers("EMP-" + jeton);
        UUID jour = jourSeme(code, employe);
        UUID autreSociete = fixture.chantier("GRS-" + jeton).societe();
        Map<String, Object> saisie = AppelsTravauxChantier.ecriture(autreSociete, null, typeA, zone, DATE, code);
        saisie.put("employeId", employe.toString());

        Reponse r = appels.modifier(jour, saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(colonne("CAST(CfgCompanyId AS varchar(36))", code))
                        .isEqualToIgnoringCase(autreSociete.toString()),
                () -> assertThat(colonne("CAST(CfgEmployeeId AS varchar(36))", code)).isNull());
    }

    @Test
    void r4_put_memeSociete_gardeLePersonnel() throws Exception {
        String code = "R4M-" + jeton;
        UUID employe = fixture.tiers("EMP-" + jeton);
        UUID jour = jourSeme(code, employe);
        Map<String, Object> saisie = AppelsTravauxChantier.ecriture(chantier.societe(), null, typeA, zone, DATE, code);
        saisie.put("employeId", employe.toString());

        Reponse r = appels.modifier(jour, saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(colonne("CAST(CfgEmployeeId AS varchar(36))", code)).isEqualToIgnoringCase(employe.toString());
    }

    // ------------------------------------------------------------------ R5 suppression

    @Test
    void r5_suppression_204() throws Exception {
        String code = "SUP-" + jeton;
        UUID jour = jourSeme(code, null);

        Reponse r = appels.appeler(delete(TRAVAUX + "/" + jour), editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(fixture.compter(PAR_CODE, code)).isZero();
    }

    @Test
    void r5_suppression_inconnue_404() throws Exception {
        UUID jour = jourSeme("SUI-" + jeton, null);

        Reponse temoin = appels.appeler(delete(TRAVAUX + "/" + jour), editeur);
        Reponse r = appels.appeler(delete(TRAVAUX + "/" + UUID.randomUUID()), editeur);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(204);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void put_inconnu_404() throws Exception {
        String code = "PI-" + jeton;
        UUID jour = jourSeme(code, null);

        Reponse temoin = appels.modifier(jour, corps(code), editeur);
        Reponse r = appels.modifier(UUID.randomUUID(), corps(code), editeur);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void put_datesEtQuantitesEcrites() throws Exception {
        String code = "PQ-" + jeton;
        UUID jour = jourSeme(code, null);
        Map<String, Object> saisie = corps(code);
        saisie.put("date", "2077-03-20");
        saisie.put("quantite", 5);
        saisie.put("quantite1", 4);

        Reponse r = appels.modifier(jour, saisie, editeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(decimal("Quantity", code)).isEqualByComparingTo("5"),
                () -> assertThat(decimal("Quantity1", code)).isEqualByComparingTo("4"),
                () -> assertThat(colonne("CONVERT(varchar(10), DocDate, 23)", code)).isEqualTo("2077-03-20"));
    }
}
