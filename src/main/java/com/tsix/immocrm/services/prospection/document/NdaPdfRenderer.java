package com.tsix.immocrm.services.prospection.document;

import com.tsix.immocrm.dto.prospection.NdaData;
import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.openpdf.text.Chunk;
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
import java.time.format.DateTimeFormatter;

/**
 * Renders the "NDA AYAD PRIVATE" PDF, modeled on {@code instructions/templates/NDA AYAD PRIVATE.pdf} —
 * in English, like the original form.
 */
@Component
public class NdaPdfRenderer {

    private static final Color ORANGE = new Color(0xE8, 0x7A, 0x1E);
    private static final Color PEACH = new Color(0xFC, 0xE4, 0xD6);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ORANGE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ORANGE);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] render(NdaData data) {
        Document document = new Document(PageSize.A4, 45, 45, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("Expression of Interest Form", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph subtitle = new Paragraph("Non-Disclosure Agreement", SUBTITLE_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(spacer());

            Paragraph intro = new Paragraph(
                    "Once completed this form should be sent to Ayadprivate@ayaddeveloppement.com. In case "
                            + "of a secondment being offered, you shall be required to provide all supporting "
                            + "documents and proceed with the contract.\n"
                            + "Please note that this is also a confidential agreement between you and our "
                            + "company. All the documents presented by our company can not be shared and are "
                            + "strictly confidential.",
                    NORMAL);
            intro.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(intro);
            document.add(spacer());

            document.add(new Paragraph("1. PERSONAL INFORMATION", SECTION));
            document.add(spacer());
            document.add(personalInfoTable(data));
            document.add(spacer());

            document.add(new Paragraph("2. ADDRESS AND CONTACT DETAILS", SECTION));
            document.add(spacer());
            document.add(addressTable(data));
            document.add(spacer());
            document.add(spacer());

            PdfPTable signatureRow = new PdfPTable(2);
            signatureRow.setWidthPercentage(100);
            PdfPCell signature = new PdfPCell(new Paragraph("Signature", VALUE));
            signature.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
            Paragraph datePara = new Paragraph();
            datePara.add(new Chunk("Date\n", VALUE));
            datePara.add(new Chunk(data.dateEdition() != null ? data.dateEdition().format(DATE_FORMAT) : "", NORMAL));
            dateCell.addElement(datePara);
            signatureRow.addCell(signature);
            signatureRow.addCell(dateCell);
            document.add(signatureRow);
            document.add(spacer());
            document.add(spacer());

            Paragraph footer = new Paragraph(
                    """
                    Ayad Developpement - 100 Boulevard Mohamed Zerktouni, Casablanca 20250, Morocco
                    Email: issam@ayaddeveloppement.com - Telephone: +212 5222-73496 or +212 6 49 044 133
                    © AYAD Private Limited""",
                    FOOTER);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la NDA", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static PdfPTable personalInfoTable(NdaData data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addField(table, "Last Name", data.nom());
        addField(table, "Nationality", data.nationalite());
        addField(table, "First Name", data.prenom());
        addField(table, "Occupation", data.profession());
        addField(table, "Date Of Birth", data.dateNaissance() != null ? data.dateNaissance().format(DATE_FORMAT) : null);
        addField(table, "Gender", genre(data.sexe()));
        addField(table, "Place Of Birth", data.lieuNaissance());
        table.addCell(emptyCell());
        return table;
    }

    private static PdfPTable addressTable(NdaData data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addField(table, "Address", data.adresse());
        addField(table, "Daytime Telephone Number", data.telephone());
        addField(table, "Town / Postal Code", joindre(data.ville(), data.codePostal()));
        addField(table, "Alternative Telephone Number", data.telephoneAlternatif());
        addField(table, "Country", data.pays());
        addField(table, "E-mail Address", data.email());
        return table;
    }

    private static String genre(String code) {
        if (code == null) return null;
        return switch (code.toUpperCase()) {
            case "M" -> "Male";
            case "F" -> "Female";
            default -> code;
        };
    }

    private static String joindre(String a, String b) {
        if (a == null && b == null) return null;
        return (a != null ? a : "") + (a != null && b != null ? " " : "") + (b != null ? b : "");
    }

    private static void addField(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(ORANGE);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", LABEL));
        p.add(new Chunk(value != null ? value : "", VALUE));
        cell.addElement(p);
        if (value != null && !value.isBlank()) {
            cell.setBackgroundColor(PEACH);
        }
        table.addCell(cell);
    }

    private static PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
        return cell;
    }
}
