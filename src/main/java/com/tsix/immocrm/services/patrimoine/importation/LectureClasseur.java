package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lecture durcie d'un classeur importé (FEATURE-patrimoine-import-export.md §1.2) : taille, octets magiques, bornes POI
 * ({@code config/ImportSecurise}), une feuille, 1 000 lignes de données au plus. Cellule vide = valeur absente.
 */
final class LectureClasseur {

    static final long TAILLE_MAX = 10L * 1024 * 1024;
    static final int LIGNES_MAX = 1000;
    private static final String SANS_DONNEES = "Votre fichier ne contient pas de données !";
    private static final byte[] ZIP = {'P', 'K', 3, 4};
    private static final byte[] OLE2 = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

    private LectureClasseur() {
    }

    /** Une ligne de données : n° de ligne Excel et valeurs non vides par en-tête (String, BigDecimal, LocalDateTime, Boolean). */
    record LigneLue(int numero, Map<String, Object> valeurs) {
    }

    /** {@code nom} = feuille lue, {@code feuilles} = toutes celles du classeur (analyse de l'import générique). */
    record Feuille(String nom, List<String> feuilles, Set<String> entetes, List<LigneLue> lignes) {
    }

    static Feuille lire(MultipartFile fichier, String nomFeuille) {
        if (fichier.getSize() > TAILLE_MAX) {
            throw new ClasseurTropGrosException("Le fichier dépasse la taille admise de 10 Mo.");
        }
        try (InputStream flux = fichier.getInputStream()) {
            exigerXlsx(flux.readNBytes(ZIP.length));
        } catch (IOException e) {
            throw illisible();
        }
        try (InputStream flux = fichier.getInputStream(); Workbook classeur = new XSSFWorkbook(flux)) {
            List<String> feuilles = new ArrayList<>();
            classeur.forEach(f -> feuilles.add(f.getSheetName()));
            return feuille(choisir(classeur, nomFeuille), List.copyOf(feuilles));
        } catch (InvalidRequestException refus) {
            throw refus;
        } catch (IOException | RuntimeException e) {
            throw illisible();
        }
    }

    private static InvalidRequestException illisible() {
        return new InvalidRequestException("Fichier illisible ou piégé : enregistrez un classeur .xlsx valide.");
    }

    private static void exigerXlsx(byte[] debut) {
        if (Arrays.equals(debut, OLE2)) {
            throw new InvalidRequestException("Format .xls non pris en charge : enregistrez le fichier au format .xlsx");
        }
        if (!Arrays.equals(debut, ZIP)) {
            throw new InvalidRequestException("Format non pris en charge : un classeur .xlsx est attendu.");
        }
    }

    private static Sheet choisir(Workbook classeur, String nomFeuille) {
        if (classeur.getNumberOfSheets() == 0) {
            throw new InvalidRequestException("Votre fichier ne contient pas de feuille de clacul !");
        }
        if (nomFeuille == null || nomFeuille.isBlank()) {
            return classeur.getSheetAt(0);
        }
        Sheet feuille = classeur.getSheet(nomFeuille.trim());
        if (feuille == null) {
            throw new InvalidRequestException("La feuille « " + nomFeuille.trim() + " » est absente du fichier.");
        }
        return feuille;
    }

    private static Feuille feuille(Sheet feuille, List<String> feuilles) {
        Row ligneEntetes = feuille.getRow(feuille.getFirstRowNum());
        if (ligneEntetes == null) {
            throw new InvalidRequestException(SANS_DONNEES);
        }
        List<String> entetes = entetes(ligneEntetes);
        List<LigneLue> lignes = new ArrayList<>();
        for (Row r : feuille) {
            if (r.getRowNum() > ligneEntetes.getRowNum()) {
                Map<String, Object> valeurs = valeurs(r, entetes);
                if (!valeurs.isEmpty()) {
                    lignes.add(new LigneLue(r.getRowNum() + 1, valeurs));
                }
            }
            if (lignes.size() > LIGNES_MAX) {
                throw new InvalidRequestException("Le fichier dépasse 1 000 lignes de données : découpez-le en plusieurs imports.");
            }
        }
        if (lignes.isEmpty()) {
            throw new InvalidRequestException(SANS_DONNEES);
        }
        return new Feuille(feuille.getSheetName(), feuilles,
                new LinkedHashSet<>(entetes.stream().filter(e -> !e.isEmpty()).toList()), lignes);
    }

    /** Index = n° de colonne ; en-tête vide gardé en place (colonne ignorée). */
    private static List<String> entetes(Row r) {
        List<String> entetes = new ArrayList<>();
        Set<String> vus = new LinkedHashSet<>();
        for (int i = 0; i < r.getLastCellNum(); i++) {
            Object valeur = valeur(r.getCell(i));
            String entete = valeur == null ? "" : valeur.toString();
            if (!entete.isEmpty() && !vus.add(entete)) {
                throw new InvalidRequestException("L'en-tête « " + entete + " » figure deux fois dans le fichier.");
            }
            entetes.add(entete);
        }
        return entetes;
    }

    private static Map<String, Object> valeurs(Row r, List<String> entetes) {
        Map<String, Object> valeurs = new HashMap<>();
        for (int i = 0; i < entetes.size(); i++) {
            Object valeur = valeur(r.getCell(i));
            if (valeur != null && !entetes.get(i).isEmpty()) {
                valeurs.put(entetes.get(i), valeur);
            }
        }
        return valeurs;
    }

    private static Object valeur(Cell c) {
        if (c == null) {
            return null;
        }
        CellType type = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
        return switch (type) {
            case NUMERIC -> DateUtil.isCellDateFormatted(c) ? c.getLocalDateTimeCellValue()
                    : BigDecimal.valueOf(c.getNumericCellValue());
            case BOOLEAN -> c.getBooleanCellValue();
            case STRING -> {
                String texte = c.getStringCellValue().strip();
                yield texte.isEmpty() ? null : texte;
            }
            default -> null;
        };
    }
}
