package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.AppelsImportGenerique;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.lignes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import {@code fournisseurs} et {@code contacts} — FEATURE-admin-tiers.md, règles 13 et 14. L'import ouvre sa transaction :
 * lignes validées, nettoyées après le test. En-têtes = DisplayName d'OMRAN ({@code Yes.Cfg.Import/TierPM.cs}), une ligne
 * de personne morale ; « Devise » n'existe pas chez OMRAN, une colonne inconnue n'est qu'un avertissement du moteur.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TiersImportTest {

    private static final String FOU = FOURNISSEURS.famille();
    private static final String CNT = CONTACTS.famille();
    private static final String PIVOT = "Pivot";
    private static final String RAISON = "Raison sociale";
    private static final String FORME = "Forme sociale";
    private static final String PAYS_RESIDENCE = "Pays de résidence";
    private static final String MOBILE = "Mobile";
    private static final String EMAIL = "EMail";
    private static final String DEVISE = "Devise";
    private static final String NATURE = "Nature";
    private static final String CODE_SQL = "Code";

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

    private String raison(String suffixe) {
        return "Import " + suffixe + " " + j.jeton();
    }

    private Map<String, Object> ligneValide(String pivot, String raisonSociale) {
        return colonnes(PIVOT, pivot, FORME, j.codeDe(j.formeMorale()), RAISON, raisonSociale,
                PAYS_RESIDENCE, j.codeDe(j.pays()), MOBILE, "0612345678", EMAIL, "import@test.ma",
                DEVISE, j.codeDe(j.devise()));
    }

    private String executer(String type, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(type, simulation, fichier(lignes), null, connecte(j.admin())))
                .andExpect(status().isOk()).andReturn());
    }

    private UUID seul(String raisonSociale) {
        List<UUID> trouves = fixture.parRaisonSociale(raisonSociale);
        assertThat(trouves).as(raisonSociale).hasSize(1);
        return trouves.getFirst();
    }

    private static int numero(String code) {
        return Integer.parseInt(code.substring(code.indexOf('/') + 1));
    }

    // ------------------------------------------------------------------ règle 13 : pivot

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void r13_pivotSansCorrespondance_creation_codeDuCompteur_jamaisCeluiDuFichier(TypeTiers type) throws Exception {
        int avant = fixture.compteurTiers();
        String pivot = j.code("PIV");
        String nom = raison("A");

        String bilan = executer(type.famille(), false, List.of(ligneValide(pivot, nom)));

        Map<String, Object> ligne = fixture.ligne(seul(nom));
        String code = (String) ligne.get(CODE_SQL);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(code).isNotEqualTo(pivot).matches("\\d+/\\d+"),
                () -> assertThat(numero(code)).isGreaterThan(avant),
                () -> assertThat(ligne).containsEntry("CfgTierTypeId", j.typeId(type).toString().toLowerCase())
                        .containsEntry("Description", nom).containsEntry("IsProspect", 0).containsEntry("IsOldCustomer", 0)
                        .containsEntry("IsRntCustomer", 0).containsEntry("IsVisibleInList", 0)
                        .containsEntry("IsFundReader", 0).containsEntry("IsExtern", 0)
                        .containsEntry("UserCreate", j.admin().getDescription()));
    }

    @Test
    void r13_pivotVide_creation_deuxLignesDeuxCodes() throws Exception {
        String a = raison("B");
        String b = raison("C");

        String bilan = executer(FOU, false, List.of(ligneValide(null, a), ligneValide(null, b)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(CREATION),
                () -> assertThat(fixture.ligne(seul(a)).get(CODE_SQL)).isNotEqualTo(fixture.ligne(seul(b)).get(CODE_SQL)));
    }

    @Test
    void r13_uneCorrespondanceDansLeType_miseAJour_codeInchange() throws Exception {
        UUID existant = j.tiers(FOURNISSEURS, "F1").inserer();
        String code = j.code("F1");
        String nom = raison("D");

        String bilan = executer(FOU, false, List.of(ligneValide(code, nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(fixture.ligne(existant)).containsEntry(CODE_SQL, code).containsEntry("SocialReason", nom)
                        .containsEntry("UserUpdate", j.admin().getDescription()));
    }

    @Test
    void r13_codeDUnAutreType_neCompteQuePas_creation_lAutreIntact() throws Exception {
        UUID contact = j.tiers(CONTACTS, "C1").inserer();
        Map<String, Object> avant = fixture.ligne(contact);
        String nom = raison("E");

        String bilan = executer(FOU, false, List.of(ligneValide(j.code("C1"), nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.ligne(contact)).isEqualTo(avant));
    }

    @Test
    void r13_plusieursCorrespondances_erreurDeLigne_rienNeChange() throws Exception {
        String code = j.code("DBL");
        UUID premier = j.tiers(FOURNISSEURS, "F2").avec(CODE_SQL, code).inserer();
        UUID second = j.tiers(FOURNISSEURS, "F3").avec(CODE_SQL, code).inserer();
        Map<String, Object> avant = fixture.ligne(premier);

        String bilan = executer(FOU, false, List.of(ligneValide(code, raison("F"))));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.ligne(premier)).isEqualTo(avant),
                () -> assertThat(fixture.existe(second)).isTrue());
    }

    @Test
    void r13_ligneEnErreur_nArretePasLesAutres_laSuivanteEstTraitee() throws Exception {
        Map<String, Object> fautive = ligneValide(null, raison("G"));
        fautive.put(RAISON, null);

        String bilan = executer(FOU, false, List.of(fautive, ligneValide(null, raison("H"))));

        assertAll(
                () -> assertThat(lignes(bilan)).as(bilan).hasSize(2),
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(CREATION));
    }

    // ------------------------------------------------------------------ règle 13 : règles 6 à 9 par ligne

    @ParameterizedTest
    @ValueSource(strings = {RAISON, PAYS_RESIDENCE, FORME})
    void r13_celluleRequiseVide_erreurDeLigne_rienCree(String entete) throws Exception {
        String nom = raison("I");
        Map<String, Object> ligne = ligneValide(null, nom);
        ligne.put(entete, null);

        String bilan = executer(FOU, false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.parRaisonSociale(nom)).isEmpty());
    }

    @Test
    void r13_r8_mobileEtEmailVides_erreurDeLigne() throws Exception {
        String nom = raison("J");
        Map<String, Object> ligne = ligneValide(null, nom);
        ligne.put(MOBILE, null);
        ligne.put(EMAIL, null);

        String bilan = executer(CNT, false, List.of(ligne));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.parRaisonSociale(nom)).isEmpty());
    }

    @Test
    void r13_r9_referenceInconnue_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(null, raison("K"));
        ligne.put(PAYS_RESIDENCE, "PAYS-INCONNU-" + j.jeton());

        String bilan = executer(FOU, true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    @Test
    void r13_r9_natureDUnAutreType_erreurDeLigne() throws Exception {
        Map<String, Object> ligne = ligneValide(null, raison("L"));
        ligne.put(NATURE, j.codeDe(j.nature(CONTACTS)));

        String bilan = executer(FOU, true, List.of(ligne));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    // ------------------------------------------------------------------ règle 13 : rien écrit

    @Test
    void r13_simulation_nEcritRien_niTiersNiCompteur() throws Exception {
        int avant = fixture.compteurTiers();
        String nom = raison("M");

        String bilan = executer(FOU, true, List.of(ligneValide(null, nom)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.parRaisonSociale(nom)).isEmpty(),
                () -> assertThat(fixture.compteurTiers()).isEqualTo(avant));
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void r13_analyse_nEcritRien(TypeTiers type) throws Exception {
        String nom = raison("N");
        byte[] classeur = fichier(List.of(ligneValide(null, nom)));

        mockMvc.perform(analyse(type.famille(), classeur, null, connecte(j.admin()))).andExpect(status().isOk());

        assertThat(fixture.parRaisonSociale(nom)).isEmpty();
    }

    // ------------------------------------------------------------------ routes du moteur

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void routes_colonnes_portentLePivot(TypeTiers type) throws Exception {
        String corps = corps(mockMvc.perform(lireColonnes(type.famille(), connecte(j.admin())))
                .andExpect(status().isOk()).andReturn());

        List<String> entetes = JsonPath.read(corps, "$[*].entete");
        assertThat(entetes).contains(PIVOT, RAISON);
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void routes_modele_200(TypeTiers type) throws Exception {
        mockMvc.perform(get(MODELE, type.famille()).with(connecte(j.admin()))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 14

    private RequestPostProcessor commercial() {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void r14_import_sansRoleAdministrateur_403_surLesQuatreRoutes(TypeTiers type) throws Exception {
        byte[] classeur = fichier(List.of(ligneValide(null, raison("O"))));
        // Témoin : la route existe pour un Administrateur.
        mockMvc.perform(lireColonnes(type.famille(), connecte(j.admin()))).andExpect(status().isOk());

        mockMvc.perform(get(MODELE, type.famille()).with(commercial())).andExpect(status().isForbidden());
        mockMvc.perform(lireColonnes(type.famille(), commercial())).andExpect(status().isForbidden());
        mockMvc.perform(AppelsImportGenerique.analyse(type.famille(), classeur, null, commercial()))
                .andExpect(status().isForbidden());
        mockMvc.perform(importer(type.famille(), false, classeur, null, commercial())).andExpect(status().isForbidden());
    }
}
