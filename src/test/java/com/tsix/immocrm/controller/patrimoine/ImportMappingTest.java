package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportGenerique;
import com.tsix.immocrm.fixtures.FixtureImportGenerique.Generique;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.DETAIL;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.analyse;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.avec;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.catalogue;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.doubler;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.gonfler;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.indexPropose;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.lireColonnes;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.mappingPropose;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.propriete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ClasseurDeTest.avecEntetes;
import static com.tsix.immocrm.fixtures.ClasseurDeTest.ligne;
import static com.tsix.immocrm.fixtures.ClasseurDeTest.xlsx;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ACTIVE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.FORME_JURIDIQUE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.seule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogue, analyse, mapping, modèle et lecture durcie de l'import générique — FEATURE-patrimoine-import-generique.md
 * §1.3 (mapping proposé sans casse, pivot obligatoire, 100 colonnes, cible dupliquée), §2 (DTO du fil), §4 V3, V14, V15 ;
 * décision du manager du 26/10 (routes {@code /colonnes}, {@code /analyse}, champ {@code mapping} facultatif, modèle =
 * en-têtes + feuilles d'aide sans pré-remplissage). Objet de référence : les entités juridiques.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportMappingTest {

    private static final long DIX_MO = 10L * 1024 * 1024;
    private static final String MESSAGE_PIVOT = "Il faut définir les pivots et effectuer leurs mapping !";
    private static final String SARL = "SARL";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportGenerique fixture;

    private Generique g;

    @BeforeEach
    void setUp() {
        g = fixture.preparer("P9EM");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(g);
    }

    // ------------------------------------------------------------------ outils

    private String analyser(byte[] classeur, String feuille) throws Exception {
        return corps(mockMvc.perform(analyse(ENTITES, classeur, feuille, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String catalogueEntites() throws Exception {
        return corps(mockMvc.perform(lireColonnes(ENTITES, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private ResultActions importerAvec(byte[] classeur, String mapping) throws Exception {
        return mockMvc.perform(importer(ENTITES, false, classeur, mapping, connecte(g.admin())));
    }

    private byte[] classeur(List<String> entetes, List<List<Object>> lignes) {
        return xlsx(entetes, lignes);
    }

    private void rienEcrit(String code) {
        assertThat(fixture.entites(code)).as("entité " + code).isEmpty();
    }

    // ------------------------------------------------------------------ §2 GET /colonnes

    @Test
    void r2_colonnes_formeDuFil_unSeulPivotCode() throws Exception {
        List<Map<String, Object>> colonnes = catalogue(catalogueEntites());

        List<Map<String, Object>> pivots = colonnes.stream().filter(c -> Boolean.TRUE.equals(c.get("pivot"))).toList();
        assertAll(
                () -> assertThat(colonnes).isNotEmpty().allSatisfy(c -> assertThat(c).containsKeys(
                        "propriete", "entete", "type", "pivot", "obligatoire", "ignoree", "reference")),
                () -> assertThat(pivots).hasSize(1),
                () -> assertThat(pivots.get(0)).containsEntry("propriete", "code").containsEntry("entete", CODE));
    }

    @Test
    void r1_1_objetInconnu_colonnes_404() throws Exception {
        mockMvc.perform(lireColonnes("inconnu", connecte(g.admin()))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §2 POST /analyse

    @Test
    void r2_analyse_feuilles_entetes_mappingPropose_apercuDeCinqLignes_aucuneEcriture() throws Exception {
        List<List<Object>> lignes = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (int k = 1; k <= 7; k++) {
            String code = g.code("EAN" + k);
            codes.add(code);
            lignes.add(ligne(code, "Entite " + k, SARL));
        }
        Map<String, List<List<Object>>> feuilles = new LinkedHashMap<>();
        feuilles.put("AUTRE", avecEntetes(List.of("X"), List.of(ligne("y"))));
        feuilles.put("DONNEES", avecEntetes(List.of(CODE, DESCRIPTION, FORME_JURIDIQUE), lignes));

        String reponse = analyser(xlsx(feuilles), "DONNEES");

        List<List<String>> apercu = JsonPath.read(reponse, "$.apercu");
        List<String> feuillesLues = JsonPath.read(reponse, "$.feuilles");
        List<String> entetes = JsonPath.read(reponse, "$.entetes");
        assertAll(
                () -> assertThat(feuillesLues).as(reponse).containsExactly("AUTRE", "DONNEES"),
                () -> assertThat((String) JsonPath.read(reponse, "$.feuille")).isEqualTo("DONNEES"),
                () -> assertThat(entetes).containsExactly(CODE, DESCRIPTION, FORME_JURIDIQUE),
                () -> assertThat(apercu).hasSize(5),
                () -> assertThat(apercu.get(0)).containsExactly(codes.get(0), "Entite 1", SARL),
                () -> assertThat(indexPropose(reponse, CODE)).isZero(),
                () -> assertThat(indexPropose(reponse, DESCRIPTION)).isEqualTo(1),
                () -> assertThat(indexPropose(reponse, FORME_JURIDIQUE)).isEqualTo(2),
                () -> assertThat(codes.stream().mapToInt(c -> fixture.entites(c).size()).sum()).isZero());
    }

    /** V15 : en-têtes en casse différente et entourés d'espaces → mapping proposé complet, importable tel quel. */
    @Test
    void v15_entetesEnCasseDifferente_mappingProposeComplet_importable() throws Exception {
        String code = g.code("ECAS");
        byte[] fichier = classeur(List.of("code", "DESCRIPTION", "  forme JURIDIQUE  "),
                List.of(ligne(code, "Entite casse", SARL)));

        String reponse = analyser(fichier, null);
        String bilan = corps(importerAvec(fichier, mappingPropose(reponse)).andExpect(status().isOk()).andReturn());

        assertAll(
                () -> assertThat(indexPropose(reponse, CODE)).as(reponse).isZero(),
                () -> assertThat(indexPropose(reponse, DESCRIPTION)).isEqualTo(1),
                () -> assertThat(indexPropose(reponse, FORME_JURIDIQUE)).isEqualTo(2),
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(seule(fixture.entites(code), code)).containsEntry("Description", "Entite casse")
                        .containsEntry("LegalForm", SARL));
    }

    // ------------------------------------------------------------------ mapping manuel

    @Test
    void r1_3_mappingManuel_colonnesAuxNomsLibres_importees() throws Exception {
        String code = g.code("EMAN");
        byte[] fichier = classeur(List.of("Identifiant", "Nom", "Statut legal"), List.of(ligne(code, "Entite libre", SARL)));
        String catalogue = catalogueEntites();
        String mapping = mappingPropose(analyser(fichier, null));
        mapping = avec(mapping, CODE, propriete(catalogue, CODE, "code"), 0);
        mapping = avec(mapping, DESCRIPTION, propriete(catalogue, DESCRIPTION, "description"), 1);
        mapping = avec(mapping, FORME_JURIDIQUE, propriete(catalogue, FORME_JURIDIQUE, "formeJuridique"), 2);

        String bilan = corps(importerAvec(fichier, mapping).andExpect(status().isOk()).andReturn());

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(seule(fixture.entites(code), code)).containsEntry("Description", "Entite libre")
                        .containsEntry("LegalForm", SARL));
    }

    // ------------------------------------------------------------------ V3 mapping invalide

    @Test
    void v3_pivotNonMappe_400_messageOmran_rienEcrit() throws Exception {
        String code = g.code("EPNM");
        byte[] fichier = classeur(List.of("Identifiant", DESCRIPTION), List.of(ligne(code, "Sans pivot")));
        String mapping = mappingPropose(analyser(fichier, null));

        importerAvec(fichier, mapping)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_PIVOT)));

        rienEcrit(code);
    }

    @Test
    void v3_pivotMappeDeuxFois_400() throws Exception {
        String code = g.code("EP2");
        byte[] fichier = classeur(List.of(CODE, DESCRIPTION, "Autre code"), List.of(ligne(code, "Double", code)));
        String mapping = doubler(mappingPropose(analyser(fichier, null)), CODE, 2);

        importerAvec(fichier, mapping).andExpect(status().isBadRequest());

        rienEcrit(code);
    }

    @Test
    void v3_colonneCibleDupliquee_400() throws Exception {
        String code = g.code("EC2");
        byte[] fichier = classeur(List.of(CODE, DESCRIPTION, "Libelle bis"), List.of(ligne(code, "Un", "Deux")));
        String mapping = doubler(mappingPropose(analyser(fichier, null)), DESCRIPTION, 2);

        importerAvec(fichier, mapping).andExpect(status().isBadRequest());

        rienEcrit(code);
    }

    @Test
    void v3_plusDe100ColonnesMappees_400() throws Exception {
        String code = g.code("E101");
        byte[] fichier = classeur(List.of(CODE, DESCRIPTION), List.of(ligne(code, "Trop de colonnes")));
        String mapping = gonfler(mappingPropose(analyser(fichier, null)), 101);

        importerAvec(fichier, mapping).andExpect(status().isBadRequest());

        rienEcrit(code);
    }

    @Test
    void r1_3_cellulePivotVide_erreurDeLigneManqueLePivot() throws Exception {
        byte[] fichier = classeur(List.of(CODE, DESCRIPTION), List.of(ligne(null, "Sans code " + g.jeton())));

        String bilan = corps(mockMvc.perform(importer(ENTITES, true, fichier, null, connecte(g.admin())))
                .andExpect(status().isOk()).andReturn());

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le pivot"));
    }

    // ------------------------------------------------------------------ modèle

    /** Décision du 26/10 (2) : en-têtes des colonnes non ignorées, feuilles d'aide, aucune ligne pré-remplie. */
    @Test
    void decision26_modeleDesEntites_entetesDuCatalogue_feuillesDAide_sansPreRemplissage() throws Exception {
        String existante = g.code("EMOD");
        fixture.entite(g, existante, true);
        List<String> attendus = catalogue(catalogueEntites()).stream()
                .filter(c -> !Boolean.TRUE.equals(c.get("ignoree")))
                .map(c -> String.valueOf(c.get("entete")))
                .filter(e -> !ACTIVE.equals(e))
                .toList();

        MvcResult resultat = mockMvc.perform(get(MODELE, ENTITES).with(connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, List<List<String>>> feuilles = ClasseurDeTest.lire(resultat.getResponse().getContentAsByteArray());
        List<List<String>> donnees = feuilles.values().iterator().next();
        List<List<String>> remplies = donnees.stream().skip(1)
                .filter(r -> r.stream().anyMatch(s -> !s.isBlank())).toList();
        assertAll(
                () -> assertThat(donnees).isNotEmpty(),
                () -> assertThat(donnees.get(0).stream().filter(e -> !ACTIVE.equals(e)).toList())
                        .as("« Active » hors contrat : voir Questions sur le spec").containsExactlyInAnyOrderElementsOf(attendus),
                () -> assertThat(remplies).as("lignes pré-remplies").isEmpty(),
                () -> assertThat(feuilles).as("feuilles d'aide").hasSizeGreaterThan(1));
    }

    // ------------------------------------------------------------------ V14 lecture durcie

    @Test
    void v14_fichierXls_400() throws Exception {
        importerAvec(ClasseurDeTest.xls(List.of(CODE, DESCRIPTION)), null).andExpect(status().isBadRequest());
    }

    @Test
    void v14_plusDe10Mo_413() throws Exception {
        byte[] gros = Arrays.copyOf(classeur(List.of(CODE, DESCRIPTION), List.of(ligne(g.code("EGROS"), "Gros"))),
                (int) DIX_MO + 1);

        importerAvec(gros, null).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void v14_bombeZip_400() throws Exception {
        String feuille = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                %s<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData></sheetData></worksheet>"""
                .formatted(" ".repeat(30_000_000));
        byte[] bombe = ClasseurDeTest.remplacerEntree(classeur(List.of(CODE, DESCRIPTION), List.of(ligne("B", "B"))),
                ClasseurDeTest.ENTREE_FEUILLE_1, feuille.getBytes(StandardCharsets.UTF_8));

        importerAvec(bombe, null).andExpect(status().isBadRequest());
    }

    @Test
    void v14_plusDe1000Lignes_400_rienEcrit() throws Exception {
        String premier = g.code("E1000");
        List<List<Object>> lignes = new ArrayList<>();
        lignes.add(ligne(premier, "Ligne 0"));
        for (int n = 1; n <= 1000; n++) {
            lignes.add(ligne("L" + n + "-" + g.jeton(), "Ligne " + n));
        }

        importerAvec(classeur(List.of(CODE, DESCRIPTION), lignes), null).andExpect(status().isBadRequest());

        rienEcrit(premier);
    }

    @Test
    void v14_analyseDUnXls_400() throws Exception {
        mockMvc.perform(analyse(ENTITES, ClasseurDeTest.xls(List.of(CODE, DESCRIPTION)), null, connecte(g.admin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void v14_analyseDePlusDe10Mo_413() throws Exception {
        byte[] gros = Arrays.copyOf(classeur(List.of(CODE, DESCRIPTION), List.of(ligne("G", "G"))), (int) DIX_MO + 1);

        mockMvc.perform(analyse(ENTITES, gros, null, connecte(g.admin()))).andExpect(status().isPayloadTooLarge());
    }
}
