package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.NettoyageProspectionsTerrain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PAYS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROSPECTIONS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.VILLE;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.STATUT_EN_COURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des prospections terrain ({@code prospections-terrain}) — FEATURE-patrimoine-import-generique.md §1.1, §1.3
 * (colonne OMRAN sans champ = avertissement « ignorée »), §1.4.2 (références des prospections, code ou description sans
 * casse), §1.4.6 (« Problèmes d'import de la prospection terrain : X »), §1.5 et §4 V11 (statut et archivage jamais
 * touchés) ; décision du manager 25/10 n° 5. En-têtes = DisplayName de {@code Yes.Lnd/Prospection.cs}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportProspectionsTerrainTest {

    private static final String CATEGORIE = "Catégorie";
    private static final String TYPE = "Type";
    private static final String SUPERFICIE = "Superficie(m²)";
    private static final String ARCHIVEE = "Archivée";
    private static final String VALIDITE_STATUT = "Validité du statut";
    private static final String AVERTISSEMENTS = "$.avertissements";
    private static final String LIBELLE = "Prospection importee ";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private NettoyageProspectionsTerrain nettoyage;
    private AdmUser admin;
    private Contexte c;
    private final List<String> codes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        nettoyage = new NettoyageProspectionsTerrain(dataSource, transactionManager);
        nettoyage.noterConfigurationAbsente();
        admin = nettoyage.valider(patrimoine::administrateur);
        nettoyage.suivre("AdmUser", admin.getId());
        c = nettoyage.commeAdmin(admin, () -> fixture.contexte("P9EL"));
        nettoyage.suivre(c);
    }

    @AfterEach
    void nettoyer() {
        for (String code : codes) {
            nettoyage.jdbc().queryForList("SELECT CONVERT(varchar(36), PKey) FROM LndLand WHERE Code = ?", String.class,
                    code).forEach(id -> nettoyage.suivreProspection(UUID.fromString(id)));
        }
        nettoyage.nettoyer();
    }

    // ------------------------------------------------------------------ outils

    private String code(String suffixe) {
        String code = suffixe + "-" + c.jeton();
        codes.add(code);
        return code;
    }

    /** Ligne de création : catégorie par code en minuscules, ville par description en minuscules (V9). */
    private Map<String, Object> valide(String code) {
        return colonnes(CODE, code, DESCRIPTION, LIBELLE + code,
                CATEGORIE, ("CPT-" + c.jeton()).toLowerCase(Locale.ROOT), TYPE, "TPT-" + c.jeton(),
                PAYS, c.pays().getCode(), VILLE, ("Ville VIL-" + c.jeton()).toLowerCase(Locale.ROOT),
                SUPERFICIE, new BigDecimal("1500"));
    }

    private String executer(List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(PROSPECTIONS, false, fichier(lignes), null, connecte(admin)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private List<Map<String, Object>> enBase(String code) {
        return nettoyage.jdbc().queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Description, CONVERT(varchar(36), StrStatusId) AS StrStatusId,
                       CAST(IsInArchive AS int) AS IsInArchive, StatusValidityDate, Area,
                       CONVERT(varchar(36), LndCategoryId) AS LndCategoryId, CONVERT(varchar(36), AdmCityId) AS AdmCityId
                FROM LndLand WHERE Code = ?""", code);
    }

    private Map<String, Object> seule(String code) {
        List<Map<String, Object>> lignes = enBase(code);
        assertThat(lignes).as("prospection " + code).hasSize(1);
        return lignes.get(0);
    }

    // ------------------------------------------------------------------ création, références

    @Test
    void r1_5_creation_statutInitialDuService_referencesResoluesSansCasse() throws Exception {
        String code = code("PTC");

        String bilan = executer(List.of(valide(code)));

        Map<String, Object> p = seule(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(p.get("StrStatusId")).as("statut initial posé par le service").isNotNull(),
                () -> assertThat(String.valueOf(p.get("LndCategoryId"))).isEqualToIgnoringCase(c.categorie().toString()),
                () -> assertThat(String.valueOf(p.get("AdmCityId"))).isEqualToIgnoringCase(c.ville().toString()),
                () -> assertThat((BigDecimal) p.get("Area")).isEqualByComparingTo("1500"),
                () -> assertThat(p).containsEntry("IsInArchive", 0));
    }

    @Test
    void r1_4_6_villeInconnue_erreurDeLigne_problemesDImport_inexistente() throws Exception {
        String code = code("PTV");
        Map<String, Object> l = valide(code);
        l.put(VILLE, "VILLE-INCONNUE-" + c.jeton());

        String bilan = executer(List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).startsWith("Problèmes d'import de la prospection terrain : ")
                        .contains("est inexistente dans notre référenciel"),
                () -> assertThat(enBase(code)).isEmpty());
    }

    // ------------------------------------------------------------------ V11 statut et archivage

    @Test
    void v11_colonnesArchiveeEtValiditeDuStatut_avertissements_statutEtArchivageInchanges() throws Exception {
        String code = code("PTA");
        nettoyage.commeAdmin(admin, () -> fixture.prospection(c, code));

        String bilan = executer(List.of(colonnes(CODE, code, DESCRIPTION, "Renommee", ARCHIVEE, "Oui",
                VALIDITE_STATUT, "01/01/2027")));

        List<String> avertissements = JsonPath.read(bilan, AVERTISSEMENTS);
        Map<String, Object> p = seule(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(avertissements).anySatisfy(a -> assertThat(a).contains(ARCHIVEE))
                        .anySatisfy(a -> assertThat(a).contains(VALIDITE_STATUT)),
                () -> assertThat(p).containsEntry("Description", "Renommee").containsEntry("IsInArchive", 0),
                () -> assertThat(p.get("StatusValidityDate")).isNull(),
                () -> assertThat(String.valueOf(p.get("StrStatusId"))).isEqualToIgnoringCase(STATUT_EN_COURS.toString()));
    }

    // ------------------------------------------------------------------ §1.3 colonnes OMRAN sans champ (V12)

    @Test
    void v12_colonnesOmranSansChamp_avertissementIgnoree_importPossible() throws Exception {
        String code = code("PTI");
        Map<String, Object> l = valide(code);
        l.put("Acquisition", "Oui");
        l.put("Superficie Nette (m²)", new BigDecimal("900"));

        String bilan = executer(List.of(l));

        List<String> avertissements = JsonPath.read(bilan, AVERTISSEMENTS);
        assertAll(
                () -> assertThat(avertissements)
                        .anySatisfy(a -> assertThat(a).contains("Acquisition").contains("ignorée"))
                        .anySatisfy(a -> assertThat(a).contains("Superficie Nette (m²)").contains("ignorée")),
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(enBase(code)).hasSize(1));
    }
}
