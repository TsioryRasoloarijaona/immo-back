package com.tsix.immocrm.fixtures;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Classeurs Excel construits en mémoire pour les tests d'import (FEATURE-patrimoine-import-export.md §1.2) : ligne 0 =
 * en-têtes ; une valeur {@code null} laisse la cellule absente (vide), un {@link Number} donne une cellule numérique.
 */
public final class ClasseurDeTest {

    public static final String FEUILLE = "ARTICLES";
    public static final String ENTREE_FEUILLE_1 = "xl/worksheets/sheet1.xml";

    private ClasseurDeTest() {
    }

    /** Une feuille de données. */
    public static byte[] xlsx(List<String> entetes, List<List<Object>> lignes) {
        Map<String, List<List<Object>>> feuilles = new LinkedHashMap<>();
        feuilles.put(FEUILLE, avecEntetes(entetes, lignes));
        return xlsx(feuilles);
    }

    /** Plusieurs feuilles, dans l'ordre de la map ; chaque liste commence par sa ligne d'en-têtes. */
    public static byte[] xlsx(Map<String, List<List<Object>>> feuilles) {
        try (XSSFWorkbook classeur = new XSSFWorkbook()) {
            feuilles.forEach((nom, lignes) -> remplir(classeur.createSheet(nom), lignes));
            return octets(classeur);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<List<Object>> avecEntetes(List<String> entetes, List<List<Object>> lignes) {
        List<List<Object>> tout = new ArrayList<>();
        tout.add(new ArrayList<>(entetes));
        tout.addAll(lignes);
        return tout;
    }

    /** Ligne de valeurs ; {@code null} = cellule absente. */
    public static List<Object> ligne(Object... valeurs) {
        return Arrays.asList(valeurs);
    }

    /** Classeur .xlsx valide sans aucune feuille. */
    public static byte[] sansFeuille() {
        try (XSSFWorkbook classeur = new XSSFWorkbook()) {
            return octets(classeur);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Vrai classeur Excel 97-2003 (OLE2, octets D0 CF 11 E0). */
    public static byte[] xls(List<String> entetes) {
        try (HSSFWorkbook classeur = new HSSFWorkbook()) {
            remplir(classeur.createSheet(FEUILLE), List.of(new ArrayList<>(entetes)));
            return octets(classeur);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Recopie l'archive en remplaçant le contenu d'une entrée (bombe, DOCTYPE). */
    public static byte[] remplacerEntree(byte[] xlsx, String entree, byte[] contenu) {
        try (ZipInputStream lecture = new ZipInputStream(new ByteArrayInputStream(xlsx));
             ByteArrayOutputStream sortie = new ByteArrayOutputStream();
             ZipOutputStream ecriture = new ZipOutputStream(sortie)) {
            for (ZipEntry e = lecture.getNextEntry(); e != null; e = lecture.getNextEntry()) {
                ecriture.putNextEntry(new ZipEntry(e.getName()));
                ecriture.write(e.getName().equals(entree) ? contenu : lecture.readAllBytes());
                ecriture.closeEntry();
            }
            ecriture.finish();
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Toutes les feuilles d'un classeur reçu : nom → lignes de textes formatés. */
    public static Map<String, List<List<String>>> lire(byte[] xlsx) {
        Map<String, List<List<String>>> feuilles = new LinkedHashMap<>();
        DataFormatter format = new DataFormatter();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            for (Sheet feuille : classeur) {
                List<List<String>> lignes = new ArrayList<>();
                for (Row r : feuille) {
                    List<String> cellules = new ArrayList<>();
                    for (int i = 0; i < r.getLastCellNum(); i++) {
                        Cell c = r.getCell(i);
                        cellules.add(c == null ? "" : format.formatCellValue(c).trim());
                    }
                    lignes.add(cellules);
                }
                feuilles.put(feuille.getSheetName(), lignes);
            }
            return feuilles;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void remplir(Sheet feuille, List<List<Object>> lignes) {
        for (int i = 0; i < lignes.size(); i++) {
            Row r = feuille.createRow(i);
            List<Object> valeurs = lignes.get(i);
            for (int j = 0; j < valeurs.size(); j++) {
                ecrire(r, j, valeurs.get(j));
            }
        }
    }

    private static void ecrire(Row r, int colonne, Object valeur) {
        if (valeur == null) {
            return;
        }
        Cell c = r.createCell(colonne);
        if (valeur instanceof BigDecimal d) {
            c.setCellValue(d.doubleValue());
        } else if (valeur instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(valeur.toString());
        }
    }

    private static byte[] octets(Workbook classeur) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        classeur.write(sortie);
        return sortie.toByteArray();
    }
}
