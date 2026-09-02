package com.tsix.immocrm.services.prospection.document;

import com.tsix.immocrm.dto.prospection.ProspectionFicheData;
import com.tsix.immocrm.dto.prospection.ProspectionFicheData.Action;
import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.tsix.immocrm.services.prospection.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.vide;

/**
 * Renders the "Fiche Prospection" as a PDF, modeled on the legacy "FICHE"
 * (Yes.Com.Front.Prospection.Prospection) template. Uses OpenPDF ({@code org.openpdf.text}).
 */
@Component
public class ProspectionFichePdfRenderer {

    private static final Color GRAY_HEADER = new Color(0xD9, 0xD9, 0xD9);
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    private static final Font WARNING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.RED);

    public byte[] render(ProspectionFicheData data) {
        Document document = new Document(PageSize.A4, 40, 40, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("FICHE PROSPECTION : " + vide(data.prospectNom()), TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            if (Boolean.TRUE.equals(data.fermee())) {
                Paragraph archivee = new Paragraph("ARCHIVÉE", WARNING);
                archivee.setAlignment(Element.ALIGN_CENTER);
                document.add(archivee);
            }
            document.add(spacer());

            document.add(sectionLabel("ENTETE"));
            document.add(entradeTable(data));
            document.add(spacer());

            document.add(sectionLabel("PROJET"));
            document.add(projetTable(data));
            document.add(spacer());

            if (data.dateAlerte() != null || data.messageAlerte() != null) {
                document.add(sectionLabel("ALERTE"));
                Paragraph alerte = new Paragraph();
                if (data.dateAlerte() != null) {
                    alerte.add(new org.openpdf.text.Chunk("Date alerte : " + data.dateAlerte().format(DATE_HEURE) + "\n", NORMAL));
                }
                alerte.add(new org.openpdf.text.Chunk(vide(data.messageAlerte()), NORMAL));
                document.add(alerte);
                document.add(spacer());
            }

            if (data.notes() != null && !data.notes().isBlank()) {
                document.add(sectionLabel("NOTES"));
                document.add(new Paragraph(data.notes(), NORMAL));
                document.add(spacer());
            }

            document.add(sectionLabel("ACTIONS"));
            document.add(actionsTable(data.actions()));
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche prospection", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static Paragraph sectionLabel(String text) {
        return new Paragraph(text, SECTION);
    }

    private static PdfPTable entradeTable(ProspectionFicheData data) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        row(table, "N° prospection", vide(data.code()), "Prospect", vide(data.prospectNom()));
        row(table, "Commercial", vide(data.commercialNom()), "Action Commercial", vide(data.actionCommerciale()));
        row(table, "Source de contact", vide(data.origine()), "Support source", vide(data.origineSupport()));
        row(table, "Vocation", vide(data.vocation()), "Projet", vide(data.projetNom()));
        row(table, "Article", vide(data.articleCode()), "", "");
        return table;
    }

    private static PdfPTable projetTable(ProspectionFicheData data) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        row(table, "Superficie (m²) de", surface(data.superficieDe()), "à", surface(data.superficieJusqua()));
        row(table, "Budget de", montant(data.prixDe()), "à", montant(data.prixJusqua()));
        row(table, "Avance", montant(data.avance()), "Fond propre", montant(data.fondsPropres()));
        row(table, "Crédit", montant(data.credit()), "", "");
        return table;
    }

    private static String surface(BigDecimal value) {
        return value != null ? value.toPlainString() + " m²" : "";
    }

    private static void row(PdfPTable table, String label1, String value1, String label2, String value2) {
        table.addCell(labelCell(label1));
        table.addCell(tableCell(value1));
        table.addCell(labelCell(label2));
        table.addCell(tableCell(value2));
    }

    private static PdfPTable actionsTable(List<Action> actions) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18, 20, 32, 30});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        table.addCell(headerCell("Date"));
        table.addCell(headerCell("Action"));
        table.addCell(headerCell("Description"));
        table.addCell(headerCell("Résultat"));

        for (Action action : actions) {
            table.addCell(tableCell(action.date() != null ? action.date().format(DATE_HEURE) : ""));
            table.addCell(tableCell(vide(action.actionType())));
            table.addCell(tableCell(vide(action.description())));
            table.addCell(tableCell(vide(action.resultat())));
        }
        if (actions.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Paragraph("Aucune action", TABLE_CELL));
            empty.setColspan(4);
            empty.setPadding(4);
            table.addCell(empty);
        }
        return table;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER));
        cell.setBackgroundColor(GRAY_HEADER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, BOLD));
        cell.setBackgroundColor(GRAY_HEADER);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell tableCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_CELL));
        cell.setPadding(4);
        return cell;
    }
}
