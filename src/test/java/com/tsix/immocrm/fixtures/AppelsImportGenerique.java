package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.FICHIER;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.IMPORT;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.XLSX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Appels de l'import générique P9e (FEATURE-patrimoine-import-generique.md §2 et décision du manager du 26/10) : mêmes
 * routes que P9a / P9b ({@code /api/import-patrimoine/{type}}), plus {@code GET /colonnes}, {@code POST /analyse} et le
 * champ multipart {@code mapping} (JSON {@code MappingImportDto}) facultatif sur l'import.
 */
public final class AppelsImportGenerique {

    public static final String COLONNES = "/api/import-patrimoine/{type}/colonnes";
    public static final String ANALYSE = "/api/import-patrimoine/{type}/analyse";
    public static final String MAPPING = "mapping";
    public static final String FEUILLE = "feuille";
    public static final String DETAIL = "$.detail";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ENTETE = "entete";
    private static final String INDEX = "indexColonne";
    private static final String PROPRIETE = "propriete";
    private static final String COLONNES_DU_MAPPING = "colonnes";

    private AppelsImportGenerique() {
    }

    /**
     * Import ; {@code simulation} nul = paramètre absent, {@code mapping} nul = champ absent. Le mapping part en partie
     * JSON sans nom de fichier : MockMvc la rend aussi comme paramètre texte (lisible par {@code @RequestParam} ou
     * {@code @RequestPart}).
     */
    public static MockMultipartHttpServletRequestBuilder importer(String type, Boolean simulation, byte[] fichier,
                                                                  String mapping, RequestPostProcessor qui) {
        MockMultipartHttpServletRequestBuilder requete = multipart(IMPORT, type);
        requete.file(new MockMultipartFile(FICHIER, "import.xlsx", XLSX, fichier));
        if (simulation != null) {
            requete.param("simulation", simulation.toString());
        }
        if (mapping != null) {
            MockPart part = new MockPart(MAPPING, mapping.getBytes(StandardCharsets.UTF_8));
            part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            requete.part(part);
        }
        requete.with(qui);
        return requete;
    }

    /** Analyse ; {@code feuille} nulle = paramètre absent. */
    public static MockMultipartHttpServletRequestBuilder analyse(String type, byte[] fichier, String feuille,
                                                                 RequestPostProcessor qui) {
        MockMultipartHttpServletRequestBuilder requete = multipart(ANALYSE, type);
        requete.file(new MockMultipartFile(FICHIER, "import.xlsx", XLSX, fichier));
        if (feuille != null) {
            requete.param(FEUILLE, feuille);
        }
        requete.with(qui);
        return requete;
    }

    public static MockHttpServletRequestBuilder lireColonnes(String type, RequestPostProcessor qui) {
        return get(COLONNES, type).with(qui);
    }

    // ------------------------------------------------------------------ catalogue (réponse de GET /colonnes)

    public static List<Map<String, Object>> catalogue(String json) {
        return JsonPath.read(json, "$");
    }

    /** Propriété cible de la colonne d'en-tête {@code entete} ; {@code defaut} si le catalogue ne la porte pas. */
    public static String propriete(String catalogue, String entete, String defaut) {
        return catalogue(catalogue).stream()
                .filter(c -> entete.equals(c.get(ENTETE)))
                .map(c -> String.valueOf(c.get(PROPRIETE)))
                .findFirst()
                .orElse(defaut);
    }

    // ------------------------------------------------------------------ mapping (MappingImportDto)

    /** {@code mappingPropose} d'une réponse d'analyse, en JSON prêt à renvoyer. */
    public static String mappingPropose(String analyse) {
        Map<String, Object> mapping = JsonPath.read(analyse, "$.mappingPropose");
        return JSON.writeValueAsString(mapping);
    }

    /** Index de colonne proposé pour la cible {@code entete} (-1 = non mappée ou absente). */
    public static int indexPropose(String analyse, String entete) {
        List<Map<String, Object>> colonnes = JsonPath.read(analyse, "$.mappingPropose.colonnes");
        return colonnes.stream()
                .filter(c -> entete.equals(c.get(ENTETE)))
                .map(c -> ((Number) c.get(INDEX)).intValue())
                .filter(i -> i >= 0)
                .findFirst()
                .orElse(-1);
    }

    /** Remplace toute ligne de la cible {@code entete} par une seule, mappée sur {@code index}. */
    public static String avec(String mapping, String entete, String propriete, int index) {
        Map<String, Object> m = lire(mapping);
        List<Map<String, Object>> colonnes = colonnesDe(m).stream()
                .filter(c -> !entete.equals(c.get(ENTETE)))
                .collect(Collectors.toCollection(ArrayList::new));
        colonnes.add(colonne(entete, propriete, index));
        m.put(COLONNES_DU_MAPPING, colonnes);
        return JSON.writeValueAsString(m);
    }

    /** Ajoute une seconde ligne pour la cible {@code entete} (même propriété), sur une autre colonne du fichier. */
    public static String doubler(String mapping, String entete, int autreIndex) {
        Map<String, Object> m = lire(mapping);
        List<Map<String, Object>> colonnes = new ArrayList<>(colonnesDe(m));
        Object propriete = colonnes.stream().filter(c -> entete.equals(c.get(ENTETE)))
                .map(c -> c.get(PROPRIETE)).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new AssertionError("pas de cible " + entete + " dans " + mapping));
        colonnes.add(colonne(entete, String.valueOf(propriete), autreIndex));
        m.put(COLONNES_DU_MAPPING, colonnes);
        return JSON.writeValueAsString(m);
    }

    /** Ajoute {@code n} lignes de mapping fictives (cibles C0…Cn-1 sur les colonnes 0…n-1). */
    public static String gonfler(String mapping, int n) {
        Map<String, Object> m = lire(mapping);
        List<Map<String, Object>> colonnes = new ArrayList<>(colonnesDe(m));
        for (int k = 0; k < n; k++) {
            colonnes.add(colonne("C" + k, "c" + k, k));
        }
        m.put(COLONNES_DU_MAPPING, colonnes);
        return JSON.writeValueAsString(m);
    }

    public static Map<String, Object> colonne(String entete, String propriete, int index) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put(ENTETE, entete);
        c.put(INDEX, index);
        c.put(PROPRIETE, propriete);
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> lire(String mapping) {
        return new LinkedHashMap<>(JSON.readValue(mapping, Map.class));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> colonnesDe(Map<String, Object> mapping) {
        Object colonnes = mapping.get(COLONNES_DU_MAPPING);
        return colonnes == null ? List.of() : (List<Map<String, Object>>) colonnes;
    }
}
