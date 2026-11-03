package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import static com.tsix.immocrm.fixtures.FixturePersonnels.SALAIRE;
import static com.tsix.immocrm.fixtures.FixturePersonnels.SECU;
import static com.tsix.immocrm.fixtures.FixturePersonnels.assertSansPaie;
import static com.tsix.immocrm.fixtures.FixturePersonnels.employe;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import {@code personnels} — FEATURE-admin-personnels.md, règles 2, 10 et 11. L'import ouvre sa transaction : lignes
 * validées, effacées après le test (le jeton est dans le nom, donc dans la raison sociale). En-têtes : socle, question 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PersonnelsImportTest {

    private static final String EMP = PERSONNELS.famille();
    private static final String PIVOT = "Pivot";
    private static final String PRENOM = "Prénom";
    private static final String NOM = "Nom";
    private static final String RAISON = "Raison sociale";
    private static final String FORME = "Forme sociale";
    private static final String DEVISE = "Devise";
    private static final String PAYS_RESIDENCE = "Pays de résidence";
    private static final String MOBILE = "Mobile";
    private static final String CODE_SQL = "Code";
    private static final String SALAIRE_SQL = "SalaryBase";
    private static final String SECU_SQL = "SocialSecurityNumber";
    private static final String SALAIRE_FICHIER = "4242.42";
    private static final String SECU_FICHIER = "SECU-FICHIER-77";
    private static final List<String> ENTETES_SALAIRE = List.of("Salaire", "Salaire de base", "SalaryBase");
    private static final List<String> ENTETES_SECU = List.of("Sécurité sociale", "Numéro de sécurité sociale", "SocialSecurityNumber");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private String nom(String suffixe) {
        return "Import " + suffixe + " " + j.jeton();
    }

    private Map<String, Object> ligneValide(String pivot, String nomDeFamille) {
        return colonnes(PIVOT, pivot, FORME, j.codeDe(j.formePhysique()), PRENOM, "Amal", NOM, nomDeFamille,
                "Titre", j.codeDe(j.titre()), "Sexe", j.codeDe(j.sexe()), PAYS_RESIDENCE, j.codeDe(j.pays()),
                MOBILE, "0612345678", "EMail", "import@test.ma", DEVISE, j.codeDe(j.devise()));
    }

    private Map<String, Object> avecPaie(Map<String, Object> ligne) {
        ENTETES_SALAIRE.forEach(e -> ligne.put(e, SALAIRE_FICHIER));
        ENTETES_SECU.forEach(e -> ligne.put(e, SECU_FICHIER));
        ligne.put("Date d'embauche", "2024-02-01");
        return ligne;
    }

    private String executer(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(EMP, simulation, fichier(lignes), null, connecte(j.admin())))
                .andExpect(status().isOk()).andReturn());
    }

    private UUID seul(String nomDeFamille) {
        List<UUID> trouves = fixture.parRaisonSociale("Amal " + nomDeFamille);
        assertThat(trouves).as(nomDeFamille).hasSize(1);
        return trouves.getFirst();
    }

    // ------------------------------------------------------------------ règle 10

    @Test
    void r10_creation_typeEmp_codeDuCompteur() throws Exception {
        String nom = nom("A");
        String pivot = j.code("PIV");

        String bilan = executer(false, List.of(ligneValide(pivot, nom)));

        Map<String, Object> ligne = fixture.ligne(seul(nom));
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat((String) ligne.get(CODE_SQL)).isNotEqualTo(pivot).matches("\\d+/\\d+"),
                () -> assertThat(ligne).containsEntry("CfgTierTypeId", j.typeId(PERSONNELS).toString().toLowerCase())
                        .containsEntry("FirstName", "Amal").containsEntry("LastName", nom)
                        .containsEntry("Description", "Amal " + nom));
    }

    @Test
    void r10_personneMorale_erreurDeLigne_rienCree() throws Exception {
        String raison = "Societe " + j.jeton();
        Map<String, Object> ligne = ligneValide(null, null);
        ligne.put(PRENOM, null);
        ligne.put(RAISON, raison);

        String bilan = executer(false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.parRaisonSociale(raison)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {PRENOM, NOM})
    void r10_prenomOuNomVide_erreurDeLigne_rienEcrit(String entete) throws Exception {
        Map<String, Object> ligne = ligneValide(null, nom("B"));
        ligne.put(entete, null);

        String bilan = executer(false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse());
    }

    @ParameterizedTest
    @ValueSource(strings = {FORME, DEVISE})
    void q1_celluleFormeOuDeviseVide_creation(String entete) throws Exception {
        String nom = nom("M" + entete.charAt(0));
        Map<String, Object> ligne = ligneValide(null, nom);
        ligne.put(entete, null);

        String bilan = executer(false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.parRaisonSociale("Amal " + nom)).hasSize(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {FORME, DEVISE})
    void q1_formeOuDeviseInconnueFournie_erreurDeLigne(String entete) throws Exception {
        Map<String, Object> ligne = ligneValide(null, nom("N"));
        ligne.put(entete, "INCONNU-" + j.jeton());

        String bilan = executer(true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Titre", "Sexe", PAYS_RESIDENCE})
    void socle9_celluleRequiseVide_erreurDeLigne(String entete) throws Exception {
        Map<String, Object> ligne = ligneValide(null, nom("P"));
        ligne.put(entete, null);

        String bilan = executer(true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void socle8_mobileEtEmailVides_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(null, nom("Q"));
        ligne.put(MOBILE, null);
        ligne.put("EMail", null);

        String bilan = executer(true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void r10_colonnesSalaire_ignorees_enCreation() throws Exception {
        String nom = nom("C");

        String bilan = executer(false, List.of(avecPaie(ligneValide(null, nom))));

        Map<String, Object> ligne = fixture.ligne(seul(nom));
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ligne).containsEntry(SALAIRE_SQL, null).containsEntry(SECU_SQL, null)
                        .containsEntry("HireDate", null));
    }

    @Test
    void r10_colonnesSalaire_ignorees_enMiseAJour_paieDeLaBaseGardee() throws Exception {
        UUID existant = employe(j, "E1").inserer();
        Map<String, Object> avant = fixture.ligne(existant);

        String bilan = executer(false, List.of(avecPaie(ligneValide(j.code("E1"), nom("D")))));

        Map<String, Object> apres = fixture.ligne(existant);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(apres).containsEntry(CODE_SQL, j.code("E1")).containsEntry(SECU_SQL, SECU)
                        .containsEntry("HireDate", avant.get("HireDate")),
                () -> assertThat((BigDecimal) apres.get(SALAIRE_SQL)).isEqualByComparingTo(SALAIRE));
    }

    @Test
    void r10_toutOuRien_uneLigneEnErreur_rienEcrit() throws Exception {
        String valide = nom("E");
        Map<String, Object> fautive = ligneValide(null, nom("F"));
        fautive.put(NOM, null);

        String bilan = executer(false, List.of(ligneValide(null, valide), fautive));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale("Amal " + valide)).isEmpty());
    }

    @Test
    void r10_pivotDUnAutreType_creation_lAutreIntact() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C1").inserer();
        Map<String, Object> avant = fixture.ligne(contact);

        String bilan = executer(false, List.of(ligneValide(j.code("C1"), nom("G"))));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.ligne(contact)).isEqualTo(avant));
    }

    @Test
    void r10_simulation_nEcritRien() throws Exception {
        String nom = nom("H");

        String bilan = executer(true, List.of(ligneValide(null, nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale("Amal " + nom)).isEmpty());
    }

    // ------------------------------------------------------------------ règle 2

    @Test
    void r2_bilanDImport_sansPaie_niDeLaBaseNiDuFichier() throws Exception {
        employe(j, "E2").inserer();

        String bilan = executer(false, List.of(avecPaie(ligneValide(j.code("E2"), nom("I")))));

        assertSansPaie(bilan);
        assertThat(bilan).doesNotContain(SALAIRE_FICHIER, SECU_FICHIER);
    }

    @Test
    void r2_bilanEnErreur_sansPaie() throws Exception {
        employe(j, "E3").inserer();
        Map<String, Object> ligne = avecPaie(ligneValide(j.code("E3"), nom("J")));
        ligne.put("Titre", "INCONNU-" + j.jeton());

        String bilan = executer(false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertSansPaie(bilan));
    }

    @Test
    void r2_analyse_sansPaieDeLaBase() throws Exception {
        employe(j, "E4").inserer();
        byte[] classeur = fichier(List.of(ligneValide(j.code("E4"), nom("K"))));

        assertSansPaie(corps(mockMvc.perform(analyse(EMP, classeur, null, connecte(j.admin())))
                .andExpect(status().isOk()).andReturn()));
    }

    @Test
    void r2_colonnesDImport_sansPaie() throws Exception {
        String corps = corps(mockMvc.perform(lireColonnes(EMP, connecte(j.admin()))).andExpect(status().isOk()).andReturn());

        List<String> entetes = JsonPath.read(corps, "$[*].entete");
        assertThat(entetes).contains(PIVOT, PRENOM, NOM)
                .noneMatch(e -> e.toLowerCase().contains("salaire") || e.toLowerCase().contains("sécurité"));
    }

    @Test
    void r2_modeleDImport_sansPaie() throws Exception {
        byte[] modele = mockMvc.perform(get(MODELE, EMP).with(connecte(j.admin()))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsByteArray();

        assertThat(cellules(modele)).contains(PRENOM, NOM)
                .noneMatch(c -> c.toLowerCase().contains("salaire") || c.toLowerCase().contains("sécurité"));
    }

    private static List<String> cellules(byte[] classeur) throws Exception {
        List<String> textes = new ArrayList<>();
        DataFormatter format = new DataFormatter();
        try (Workbook livre = WorkbookFactory.create(new ByteArrayInputStream(classeur))) {
            for (Sheet feuille : livre) {
                for (Row ligne : feuille) {
                    ligne.forEach(cellule -> textes.add(format.formatCellValue(cellule)));
                }
            }
        }
        return textes;
    }

    // ------------------------------------------------------------------ règle 11

    private RequestPostProcessor commercial() {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @Test
    void r11_import_sansRoleAdministrateur_403_surLesQuatreRoutes() throws Exception {
        byte[] classeur = fichier(List.of(ligneValide(null, nom("L"))));
        // Témoin : la route existe pour un Administrateur.
        mockMvc.perform(lireColonnes(EMP, connecte(j.admin()))).andExpect(status().isOk());

        mockMvc.perform(get(MODELE, EMP).with(commercial())).andExpect(status().isForbidden());
        mockMvc.perform(lireColonnes(EMP, commercial())).andExpect(status().isForbidden());
        mockMvc.perform(analyse(EMP, classeur, null, commercial())).andExpect(status().isForbidden());
        mockMvc.perform(importer(EMP, false, classeur, null, commercial())).andExpect(status().isForbidden());
    }
}
