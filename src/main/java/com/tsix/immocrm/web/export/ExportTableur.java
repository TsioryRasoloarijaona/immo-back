package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ooxml.POIXMLProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Export d'une liste en {@code .xlsx} (POI SXSSF, écrit en flux) : une feuille (« DONNEES » par défaut), une colonne par
 * {@link Colonne} demandée, dans l'ordre reçu. Chaque ligne est un {@code record} dont la composante porte le
 * nom de la colonne ({@link Colonne#nom()}) : aucune connaissance d'un écran particulier.
 */
@Component
public class ExportTableur {

    public static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    public static final String FEUILLE = "DONNEES";
    public static final String FORMAT_XLSX = "xlsx";

    private static final int LIGNES_EN_MEMOIRE = 200;
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Clock clock;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Map<Class<?>, Map<String, RecordComponent>> composantes = new ConcurrentHashMap<>();

    public ExportTableur(Clock clock, UtilisateurAffiche utilisateurAffiche) {
        this.clock = clock;
        this.utilisateurAffiche = utilisateurAffiche;
    }

    /** @throws InvalidRequestException (400) si {@code format} n'est pas {@code xlsx}. */
    public static void exigerXlsx(String format) {
        if (!FORMAT_XLSX.equalsIgnoreCase(format)) {
            throw new InvalidRequestException("format doit être 'xlsx' (reçu : " + format + ")");
        }
    }

    /**
     * @param liste  préfixe du nom de fichier ({@code <liste>_<horodatage>.xlsx})
     * @param titre  titre du classeur (propriétés du document)
     * @param lignes des {@code record} déjà chargés : la transaction est close quand le flux s'écrit
     */
    public ResponseEntity<StreamingResponseBody> reponse(String liste, String titre,
                                                         Collection<? extends Colonne> colonnes,
                                                         List<?> lignes) {
        return reponse(liste, titre, FEUILLE, colonnes, lignes);
    }

    /** Comme {@link #reponse(String, String, Collection, List)}, feuille nommée {@code feuille}. */
    public ResponseEntity<StreamingResponseBody> reponse(String liste, String titre, String feuille,
                                                         Collection<? extends Colonne> colonnes,
                                                         List<?> lignes) {
        String nomFichier = liste + "_" + LocalDateTime.now(clock).format(HORODATAGE) + ".xlsx";
        String auteur = utilisateurAffiche.courant().nom();
        List<Colonne> ordre = List.copyOf(colonnes);
        StreamingResponseBody corps = sortie -> ecrire(sortie, titre, feuille, auteur, ordre, lignes);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(corps);
    }

    /** Écrit le classeur dans {@code sortie} ; exposé pour les tests. */
    public void ecrire(OutputStream sortie, String titre, String nomFeuille, String auteur,
                       List<? extends Colonne> colonnes, List<?> lignes) throws IOException {
        try (SXSSFWorkbook classeur = new SXSSFWorkbook(LIGNES_EN_MEMOIRE)) {
            POIXMLProperties.CoreProperties proprietes = classeur.getXSSFWorkbook().getProperties()
                    .getCoreProperties();
            proprietes.setCreator(auteur);
            proprietes.setTitle(titre);

            Styles styles = new Styles(classeur);
            Sheet feuille = classeur.createSheet(nomFeuille);
            Row entete = feuille.createRow(0);
            for (int i = 0; i < colonnes.size(); i++) {
                Cell cellule = entete.createCell(i);
                cellule.setCellValue(colonnes.get(i).libelle());
                cellule.setCellStyle(styles.entete);
            }
            int numero = 1;
            for (Object ligne : lignes) {
                Row row = feuille.createRow(numero++);
                for (int i = 0; i < colonnes.size(); i++) {
                    Colonne colonne = colonnes.get(i);
                    remplir(row.createCell(i), colonne, valeur(ligne, colonne.nom()), styles);
                }
            }
            feuille.createFreezePane(0, 1);
            classeur.write(sortie);
        }
    }

    private static void remplir(Cell cellule, Colonne colonne, Object valeur, Styles styles) {
        if (valeur == null) {
            return;
        }
        switch (colonne.typeExport()) {
            case DATE -> remplirDate(cellule, valeur, styles);
            case MONTANT -> remplirNombre(cellule, valeur, styles.montant);
            case NOMBRE -> remplirNombre(cellule, valeur, styles.nombre);
            case BOOLEEN -> cellule.setCellValue(Boolean.TRUE.equals(valeur) ? "Oui" : "Non");
            case TEXTE -> cellule.setCellValue(valeur.toString());
        }
    }

    private static void remplirDate(Cell cellule, Object valeur, Styles styles) {
        if (valeur instanceof LocalDateTime d) {
            cellule.setCellValue(d);
            cellule.setCellStyle(styles.date);
        } else if (valeur instanceof LocalDate d) {
            cellule.setCellValue(d);
            cellule.setCellStyle(styles.date);
        } else {
            cellule.setCellValue(valeur.toString());
        }
    }

    private static void remplirNombre(Cell cellule, Object valeur, CellStyle style) {
        if (valeur instanceof BigDecimal b) {
            cellule.setCellValue(b.doubleValue());
        } else if (valeur instanceof Number n) {
            cellule.setCellValue(n.doubleValue());
        } else {
            cellule.setCellValue(valeur.toString());
            return;
        }
        cellule.setCellStyle(style);
    }

    /** Composante du {@code record} portant le nom de la colonne ; absente = cellule vide. */
    private Object valeur(Object ligne, String nom) {
        RecordComponent composante = composantes
                .computeIfAbsent(ligne.getClass(), ExportTableur::indexer)
                .get(nom);
        if (composante == null) {
            return null;
        }
        try {
            return composante.getAccessor().invoke(ligne);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Lecture impossible de la colonne " + nom, e);
        }
    }

    private static Map<String, RecordComponent> indexer(Class<?> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException("Ligne d'export non record : " + type.getName());
        }
        return Arrays.stream(type.getRecordComponents())
                .collect(Collectors.toMap(RecordComponent::getName, Function.identity()));
    }

    private static final class Styles {
        private final CellStyle entete;
        private final CellStyle date;
        private final CellStyle montant;
        private final CellStyle nombre;

        private Styles(SXSSFWorkbook classeur) {
            Font gras = classeur.createFont();
            gras.setBold(true);
            entete = classeur.createCellStyle();
            entete.setFont(gras);
            short formatDate = classeur.createDataFormat().getFormat("dd/mm/yyyy");
            date = classeur.createCellStyle();
            date.setDataFormat(formatDate);
            montant = classeur.createCellStyle();
            montant.setDataFormat(classeur.createDataFormat().getFormat("#,##0.00"));
            nombre = classeur.createCellStyle();
            nombre.setDataFormat(classeur.createDataFormat().getFormat("#,##0.##"));
        }
    }
}
