package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.TypeExport;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Consumer;

/** Polices, cellules, formats français et pied « Page n / m » communs aux PDF de liste et aux fiches. */
public final class MiseEnPagePdf {

    public static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    public static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);
    public static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    public static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    public static final Font CELLULE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color GRIS = new Color(0xE4, 0xE4, 0xE7);
    private static final String ECHEC = "Écriture du PDF échouée";

    private MiseEnPagePdf() {
    }

    /** Écrit le document (marges de 28 pt) puis numérote chaque page. */
    public static byte[] rendre(Rectangle page, Consumer<Document> contenu) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (Document document = new Document(page, 28, 28, 28, 36)) {
            PdfWriter.getInstance(document, sortie);
            document.open();
            contenu.accept(document);
        } catch (DocumentException e) {
            throw new IllegalStateException(ECHEC, e);
        }
        try {
            return numeroter(sortie.toByteArray());
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException(ECHEC, e);
        }
    }

    private static byte[] numeroter(byte[] pdf) throws IOException, DocumentException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (PdfReader lecteur = new PdfReader(pdf); PdfStamper tampon = new PdfStamper(lecteur, sortie)) {
            int total = lecteur.getNumberOfPages();
            for (int n = 1; n <= total; n++) {
                Rectangle page = lecteur.getPageSizeWithRotation(n);
                ColumnText.showTextAligned(tampon.getOverContent(n), Element.ALIGN_RIGHT,
                        new Phrase("Page " + n + " / " + total, NORMAL), page.getRight() - 28, page.getBottom() + 16, 0);
            }
        }
        return sortie.toByteArray();
    }

    /** Ajoute un élément ; l'erreur d'OpenPDF devient non vérifiée (dans un {@link Consumer}). */
    public static void ajouter(Document document, Element element) {
        try {
            document.add(element);
        } catch (DocumentException e) {
            throw new IllegalStateException(ECHEC, e);
        }
    }

    public static Paragraph paragraphe(String texte, Font police) {
        return new Paragraph(texte, police);
    }

    public static PdfPCell entete(String texte) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, GRAS));
        cellule.setBackgroundColor(GRIS);
        cellule.setPadding(3);
        return cellule;
    }

    public static PdfPCell cellule(String texte, boolean aDroite, boolean gras) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, gras ? GRAS : CELLULE));
        cellule.setPadding(3);
        if (aDroite) {
            cellule.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        return cellule;
    }

    /** Valeur affichée, format déduit du type Java (montants {@code 1 350 000,00}, dates {@code dd/MM/yyyy}). */
    public static String texte(Object valeur) {
        return switch (valeur) {
            case null -> "";
            case BigDecimal b -> montant(b);
            case Boolean b -> oui(b);
            case LocalDate d -> DATE.format(d);
            case LocalDateTime d -> DATE.format(d);
            default -> valeur.toString();
        };
    }

    /** Valeur d'une colonne d'export, au format de son type ({@link TypeExport}). */
    public static String texte(Object valeur, TypeExport type) {
        if (valeur == null) {
            return "";
        }
        return switch (type) {
            case MONTANT -> valeur instanceof Number n ? montant(new BigDecimal(n.toString())) : valeur.toString();
            case NOMBRE -> valeur instanceof Number n ? format("#,##0.##").format(new BigDecimal(n.toString())) : valeur.toString();
            case BOOLEEN -> oui(Boolean.TRUE.equals(valeur));
            case DATE -> valeur instanceof TemporalAccessor t ? DATE.format(t) : valeur.toString();
            case TEXTE -> texte(valeur);
        };
    }

    private static String montant(BigDecimal valeur) {
        return format("#,##0.00").format(valeur);
    }

    private static String oui(boolean valeur) {
        return valeur ? "Oui" : "Non";
    }

    private static DecimalFormat format(String motif) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols();
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        symboles.setMinusSign('-');
        return new DecimalFormat(motif, symboles);
    }
}
