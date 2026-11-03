package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureBanques;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.analyse;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.lireColonnes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.BANQUES;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.ORGANISMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import {@code banques} et {@code organismes} — FEATURE-admin-banques-organismes.md, section Back et règle 10. L'import
 * ouvre sa transaction : lignes validées, nettoyées après le test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BanquesImportTest {

    private static final String BAN = BANQUES.famille();
    private static final String ORG = ORGANISMES.famille();
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String PAYS = "Pays";
    private static final String NATURE = "Nature";
    private static final String MATRICULE = "Matricule fiscal";
    private static final String REGISTRE = "Registre de commerce";
    private static final String ICE = "ICE";
    private static final String MOBILE = "Mobile";
    private static final String EMAIL = "EMail";
    private static final String ADRESSE = "Adresse";
    private static final String CODE_POSTAL = "Code postal";
    private static final String RAISON_SQL = "SocialReason";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBanques banques;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = banques.jeu();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private String libelle(String suffixe) {
        return "IMPORT " + suffixe + " " + j.jeton();
    }

    private Map<String, Object> ligneValide(String code, String description) {
        return colonnes(CODE, code, DESCRIPTION, description, PAYS, j.codeDe(j.pays()));
    }

    private String executer(String type, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(type, simulation, fichier(lignes), null, connecte(j.admin())))
                .andExpect(status().isOk()).andReturn());
    }

    private UUID seul(String description) {
        List<UUID> trouves = fixture.parRaisonSociale(description);
        assertThat(trouves).as(description).hasSize(1);
        return trouves.getFirst();
    }

    // ------------------------------------------------------------------ règle 10 : pivot

    @Test
    void r10_pivotVide_erreurDeLigne_rienEcrit() throws Exception {
        String nom = libelle("A");

        String bilan = executer(BAN, false, List.of(ligneValide(null, nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale(nom)).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r10_zeroCorrespondance_creationAvecCeCode_reglesDeLaFiche(TypeTiers type) throws Exception {
        String code = j.code("PIV");
        String nom = libelle("B");

        String bilan = executer(type.famille(), false, List.of(ligneValide(code, nom.toLowerCase(Locale.ROOT))));

        Map<String, Object> ligne = fixture.ligne(seul(nom));
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(ligne).containsEntry(CODE, code).containsEntry(DESCRIPTION, nom).containsEntry(RAISON_SQL, nom)
                        .containsEntry("CfgTierTypeId", j.typeId(type).toString().toLowerCase(Locale.ROOT))
                        .containsEntry("AdmCountryResidenceId", j.pays().toString().toLowerCase(Locale.ROOT))
                        .containsEntry("IsProspect", 0).containsEntry("IsOldCustomer", 0).containsEntry("IsRntCustomer", 0)
                        .containsEntry("IsVisibleInList", 0).containsEntry("IsFundReader", 0));
    }

    @Test
    void r10_touteLesColonnes_ecrites_matriculeDansIdNumber() throws Exception {
        UUID nature = j.nature(BANQUES);
        String nom = libelle("C");
        Map<String, Object> ligne = colonnes(CODE, j.code("PIV"), DESCRIPTION, nom, PAYS, j.codeDe(j.pays()),
                NATURE, j.codeDe(nature), MATRICULE, "MAT1", REGISTRE, "RC1", ICE, "ICE1", MOBILE, "0661000001",
                EMAIL, "b@test.ma", ADRESSE, "1 rue A", CODE_POSTAL, "20000");

        String bilan = executer(BAN, false, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION);
        assertThat(banques.colonnes(seul(nom))).containsEntry("CfgTierKindId", nature.toString().toLowerCase(Locale.ROOT))
                .containsEntry("FiscalCode", "MAT1").containsEntry("IDNumber", "MAT1").containsEntry("FiscalRegistry", "RC1")
                .containsEntry(ICE, "ICE1").containsEntry(MOBILE, "0661000001").containsEntry(EMAIL, "b@test.ma")
                .containsEntry("Address", "1 rue A").containsEntry("ZipCode", "20000");
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r10_uneCorrespondanceDansLeType_miseAJour(TypeTiers type) throws Exception {
        UUID existant = j.tiers(type, "T1").inserer();
        String code = j.code("T1");
        String nom = libelle("D");

        String bilan = executer(type.famille(), false, List.of(ligneValide(code, nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(fixture.ligne(existant)).containsEntry(CODE, code).containsEntry(DESCRIPTION, nom)
                        .containsEntry(RAISON_SQL, nom).containsEntry("UserUpdate", j.admin().getDescription()));
    }

    @Test
    void r10_codeDUnAutreType_neCorrespondPas_creation_lAutreIntact() throws Exception {
        UUID banque = j.tiers(BANQUES, "B1").inserer();
        Map<String, Object> avant = fixture.ligne(banque);
        String nom = libelle("E");

        String bilan = executer(ORG, false, List.of(ligneValide(j.code("B1"), nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.ligne(banque)).isEqualTo(avant));
    }

    @Test
    void r10_plusieursCorrespondances_erreurDeLigne_rienNeChange() throws Exception {
        String code = j.code("DBL");
        UUID premier = j.tiers(BANQUES, "B2").avec(CODE, code).inserer();
        j.tiers(BANQUES, "B3").avec(CODE, code).inserer();
        Map<String, Object> avant = fixture.ligne(premier);

        String bilan = executer(BAN, false, List.of(ligneValide(code, libelle("F"))));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.ligne(premier)).isEqualTo(avant));
    }

    // ------------------------------------------------------------------ règle 10 : tout ou rien

    @Test
    void r10_toutOuRien_uneLigneEnErreur_rienEcrit_toutesTraitees() throws Exception {
        String valide = libelle("G");

        String bilan = executer(BAN, false, List.of(ligneValide(j.code("V1"), valide), ligneValide(null, libelle("H"))));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale(valide)).isEmpty());
    }

    // ------------------------------------------------------------------ règle 10 : règles de la fiche par ligne

    @Test
    void r10_r3_descriptionVide_erreurDeLigne() throws Exception {
        String bilan = executer(BAN, true, List.of(ligneValide(j.code("V2"), null)));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void r10_r4_paysVide_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(j.code("V3"), libelle("I"));
        ligne.put(PAYS, null);

        String bilan = executer(BAN, true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void r10_r4_paysInconnu_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(j.code("V4"), libelle("J"));
        ligne.put(PAYS, "PAYS-INCONNU-" + j.jeton());

        String bilan = executer(BAN, true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void r10_natureDUnAutreType_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(j.code("V5"), libelle("K"));
        ligne.put(NATURE, j.codeDe(j.nature(ORGANISMES)));

        String bilan = executer(BAN, true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    // ------------------------------------------------------------------ simulation et analyse

    @Test
    void r10_simulation_nEcritRien() throws Exception {
        String nom = libelle("L");

        String bilan = executer(BAN, true, List.of(ligneValide(j.code("V6"), nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale(nom)).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r10_analyse_nEcritRien(TypeTiers type) throws Exception {
        String nom = libelle("M");
        byte[] classeur = fichier(List.of(ligneValide(j.code("V7"), nom)));

        mockMvc.perform(analyse(type.famille(), classeur, null, connecte(j.admin()))).andExpect(status().isOk());

        assertThat(fixture.parRaisonSociale(nom)).isEmpty();
    }

    // ------------------------------------------------------------------ routes du moteur

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void back_colonnes_enTetesDuSpec_pivotCode(TypeTiers type) throws Exception {
        String corps = corps(mockMvc.perform(lireColonnes(type.famille(), connecte(j.admin())))
                .andExpect(status().isOk()).andReturn());

        List<String> entetes = JsonPath.read(corps, "$[*].entete");
        assertThat(entetes).contains(CODE, DESCRIPTION, PAYS, NATURE, MATRICULE, REGISTRE, ICE, MOBILE, EMAIL, ADRESSE,
                CODE_POSTAL).doesNotContain("Pivot");
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void back_modele_200(TypeTiers type) throws Exception {
        mockMvc.perform(get(MODELE, type.famille()).with(connecte(j.admin()))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 11

    private RequestPostProcessor commercial() {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r11_import_sansRoleAdministrateur_403_surLesQuatreRoutes(TypeTiers type) throws Exception {
        byte[] classeur = fichier(List.of(ligneValide(j.code("V8"), libelle("N"))));
        // Témoin : la route existe pour un Administrateur.
        mockMvc.perform(lireColonnes(type.famille(), connecte(j.admin()))).andExpect(status().isOk());

        mockMvc.perform(get(MODELE, type.famille()).with(commercial())).andExpect(status().isForbidden());
        mockMvc.perform(lireColonnes(type.famille(), commercial())).andExpect(status().isForbidden());
        mockMvc.perform(analyse(type.famille(), classeur, null, commercial())).andExpect(status().isForbidden());
        mockMvc.perform(importer(type.famille(), false, classeur, null, commercial())).andExpect(status().isForbidden());
    }
}
