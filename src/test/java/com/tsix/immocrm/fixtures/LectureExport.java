package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

/**
 * Lecture des exports de listes et des fiches PDF (FEATURE-patrimoine-import-export.md §3) : réponse rendue en flux
 * (StreamingResponseBody, asynchrone) ou d'un bloc, classeur relu par POI, PDF relu par OpenPDF.
 */
public final class LectureExport {

    public static final String SANS_CODE = "<ligne sans code>";

    private LectureExport() {
    }

    /** Réponse finale : suit la répartition asynchrone si la réponse est écrite en flux. */
    public static MvcResult executer(MockMvc mockMvc, RequestBuilder requete) throws Exception {
        MvcResult premier = mockMvc.perform(requete).andReturn();
        if (premier.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(premier)).andReturn();
        }
        return premier;
    }

    /** {@code code} de chaque ligne d'une liste du fil, paginée ({@code content}) ou non (tableau). */
    public static List<String> codes(String json) {
        String chemin = json.trim().startsWith("[") ? "$[*].code" : "$.content[*].code";
        return JsonPath.read(json, chemin);
    }

    /** Lignes de la feuille « DONNEES » (en-tête compris) : texte, nombre ({@code Double}) ou {@code null} si vide. */
    public static List<List<Object>> lignesXlsx(byte[] contenu) throws IOException {
        List<List<Object>> lignes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                List<Object> valeurs = new ArrayList<>();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    valeurs.add(valeur(row.getCell(i)));
                }
                lignes.add(valeurs);
            }
        }
        return lignes;
    }

    /** Pour chaque ligne de données (hors en-tête et ligne « Total »), la première cellule égale à un des candidats. */
    public static List<String> codesDesLignes(List<List<Object>> lignes, List<String> candidats) {
        return lignes.stream().skip(1)
                .filter(l -> !l.isEmpty() && !"Total".equals(l.get(0)))
                .map(l -> l.stream().filter(candidats::contains).map(String.class::cast).findFirst().orElse(SANS_CODE))
                .toList();
    }

    private static Object valeur(Cell cellule) {
        if (cellule == null || cellule.getCellType() == CellType.BLANK) {
            return null;
        }
        return cellule.getCellType() == CellType.NUMERIC ? cellule.getNumericCellValue() : cellule.getStringCellValue();
    }

    public static boolean estPdf(byte[] contenu) {
        return contenu.length > 4 && new String(contenu, 0, 4, StandardCharsets.ISO_8859_1).equals("%PDF");
    }

    /** Texte de chaque page, espaces insécables ramenés à l'espace simple. */
    public static List<String> pagesPdf(byte[] contenu) throws IOException {
        List<String> pages = new ArrayList<>();
        try (PdfReader reader = new PdfReader(contenu)) {
            PdfTextExtractor extracteur = new PdfTextExtractor(reader);
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                pages.add(normaliser(extracteur.getTextFromPage(i)));
            }
        }
        return pages;
    }

    public static String textePdf(byte[] contenu) throws IOException {
        return String.join("\n", pagesPdf(contenu));
    }

    /** Sans aucun blanc : un code coupé en fin de cellule par le retour à la ligne reste lisible. */
    public static String compact(String texte) {
        return texte.replaceAll("\\s+", "");
    }

    private static String normaliser(String texte) {
        return texte.replace(' ', ' ').replace(' ', ' ');
    }
}
