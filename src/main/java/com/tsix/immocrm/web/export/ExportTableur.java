package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Export d'une liste en {@code .xlsx} (POI SXSSF, écrit en flux) : une feuille « DONNEES », une colonne par
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
    private static final int NOM_FEUILLE_MAX = 31;
    private static final String TOTAL = "Total";
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
        return reponse(liste, titre, List.of(new FeuilleExport(FEUILLE, List.copyOf(colonnes), lignes, null)));
    }

    /** Une feuille par élément de {@code feuilles}, noms nettoyés et dédoublonnés ({@link #nomFeuille}). */
    public ResponseEntity<StreamingResponseBody> reponse(String liste, String titre, List<FeuilleExport> feuilles) {
        String nomFichier = liste + "_" + LocalDateTime.now(clock).format(HORODATAGE) + ".xlsx";
        String auteur = utilisateurAffiche.courant().nom();
        StreamingResponseBody corps = sortie -> ecrire(sortie, titre, auteur, feuilles);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(corps);
    }

    /** Écrit le classeur dans {@code sortie} ; exposé pour les tests. */
    public void ecrire(OutputStream sortie, String titre, String auteur, List<? extends Colonne> colonnes,
                       List<?> lignes) throws IOException {
        ecrire(sortie, titre, auteur, List.of(new FeuilleExport(FEUILLE, colonnes, lignes, null)));
    }

    public void ecrire(OutputStream sortie, String titre, String auteur, List<FeuilleExport> feuilles)
            throws IOException {
        try (SXSSFWorkbook classeur = new SXSSFWorkbook(LIGNES_EN_MEMOIRE)) {
            POIXMLProperties.CoreProperties proprietes = classeur.getXSSFWorkbook().getProperties()
                    .getCoreProperties();
            proprietes.setCreator(auteur);
            proprietes.setTitle(titre);

            Styles styles = new Styles(classeur);
            Set<String> pris = new HashSet<>();
            for (FeuilleExport feuille : feuilles) {
                ecrireFeuille(classeur.createSheet(nomFeuille(feuille.nom(), pris)), feuille, styles);
            }
            classeur.write(sortie);
        }
    }

    private void ecrireFeuille(Sheet feuille, FeuilleExport contenu, Styles styles) {
        List<? extends Colonne> colonnes = contenu.colonnes();
        Row entete = feuille.createRow(0);
        for (int i = 0; i < colonnes.size(); i++) {
            Cell cellule = entete.createCell(i);
            cellule.setCellValue(colonnes.get(i).libelle());
            cellule.setCellStyle(styles.entete);
        }
        int numero = 1;
        for (Object ligne : contenu.lignes()) {
            Row row = feuille.createRow(numero++);
            for (int i = 0; i < colonnes.size(); i++) {
                Colonne colonne = colonnes.get(i);
                remplir(row.createCell(i), colonne, valeur(ligne, colonne.nom()), styles, false);
            }
        }
        if (contenu.totaux() != null) {
            Row total = feuille.createRow(numero);
            for (int i = 0; i < colonnes.size(); i++) {
                Colonne colonne = colonnes.get(i);
                Object valeur = contenu.totaux().get(colonne.nom());
                if (i == 0 && valeur == null) {
                    valeur = TOTAL;
                }
                remplir(total.createCell(i), colonne, valeur, styles, true);
            }
        }
        feuille.createFreezePane(0, 1);
    }

    /** Sans {@code []:*?/\}, 31 caractères au plus (limites d'Excel), {@code x (2)} si déjà pris. */
    static String nomFeuille(String titre, Set<String> pris) {
        String base = titre == null ? "" : titre.replaceAll("[\\[\\]:*?/\\\\]", "");
        base = base.isBlank() ? "Feuille" : base;
        String nom = tronquer(base, NOM_FEUILLE_MAX);
        for (int n = 2; pris.contains(nom.toLowerCase(Locale.ROOT)); n++) {
            String suffixe = " (" + n + ")";
            nom = tronquer(base, NOM_FEUILLE_MAX - suffixe.length()) + suffixe;
        }
        pris.add(nom.toLowerCase(Locale.ROOT));
        return nom;
    }

    private static String tronquer(String texte, int longueur) {
        return texte.length() > longueur ? texte.substring(0, longueur) : texte;
    }

    private static void remplir(Cell cellule, Colonne colonne, Object valeur, Styles styles, boolean total) {
        if (valeur == null) {
            return;
        }
        switch (colonne.typeExport()) {
            case DATE -> remplirDate(cellule, valeur, styles);
            case MONTANT -> remplirNombre(cellule, valeur, styles.gras(styles.montant, total));
            case NOMBRE -> remplirNombre(cellule, valeur, styles.gras(styles.nombre(colonne), total));
            case BOOLEEN -> cellule.setCellValue(Boolean.TRUE.equals(valeur) ? "Oui" : "Non");
            case TEXTE -> {
                cellule.setCellValue(valeur.toString());
                if (total) {
                    cellule.setCellStyle(styles.entete);
                }
            }
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

    /** Clé d'une ligne {@code Map}, ou composante du {@code record}, portant le nom de la colonne ; absente = vide. */
    private Object valeur(Object ligne, String nom) {
        if (ligne instanceof Map<?, ?> map) {
            return map.get(nom);
        }
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
        private final SXSSFWorkbook classeur;
        private final Font police;
        private final CellStyle entete;
        private final CellStyle date;
        private final CellStyle montant;
        private final CellStyle nombre;
        private final Map<Integer, CellStyle> parDecimales = new HashMap<>();
        private final Map<CellStyle, CellStyle> enGras = new HashMap<>();

        private Styles(SXSSFWorkbook classeur) {
            this.classeur = classeur;
            police = classeur.createFont();
            police.setBold(true);
            entete = classeur.createCellStyle();
            entete.setFont(police);
            short formatDate = classeur.createDataFormat().getFormat("dd/mm/yyyy");
            date = classeur.createCellStyle();
            date.setDataFormat(formatDate);
            montant = format("#,##0.00");
            nombre = format("#,##0.##");
        }

        private CellStyle format(String motif) {
            CellStyle style = classeur.createCellStyle();
            style.setDataFormat(classeur.createDataFormat().getFormat(motif));
            return style;
        }

        /** Colonne de rapport {@code NOMBRE} : ses décimales (B6) ; autres catalogues : format historique. */
        private CellStyle nombre(Colonne colonne) {
            if (colonne instanceof ColonneRapport c && c.type() == TypeColonne.NOMBRE) {
                int decimales = c.decimales();
                return parDecimales.computeIfAbsent(decimales,
                        d -> format(d > 0 ? "#,##0." + "0".repeat(d) : "#,##0"));
            }
            return nombre;
        }

        private CellStyle gras(CellStyle base, boolean total) {
            if (!total) {
                return base;
            }
            return enGras.computeIfAbsent(base, b -> {
                CellStyle style = classeur.createCellStyle();
                style.cloneStyleFrom(b);
                style.setFont(police);
                return style;
            });
        }
    }
}
