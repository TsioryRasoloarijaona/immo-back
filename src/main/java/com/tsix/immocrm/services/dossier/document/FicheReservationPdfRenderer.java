package com.tsix.immocrm.services.dossier.document;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import com.tsix.immocrm.dto.dossier.FicheReservationData;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Fiche de réservation" as a PDF, modeled on the legacy "FICHE RESERVATION MBR"
 * template (see {@code instructions/templates/FICHE RESERVATION MBR.pdf}). Uses OpenPDF
 * ({@code org.openpdf.text}) — pure Java, no external process needed.
 */
@Component
public class FicheReservationPdfRenderer {

    private static final Color ORANGE = new Color(0xE8, 0x7A, 0x1E);
    private static final Color GRAY_LABEL = new Color(0xE4, 0xE4, 0xE4);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ORANGE);
    private static final Font LABEL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);

    public byte[] render(FicheReservationData data) {
        Document document = new Document(PageSize.A4, 45, 45, 30, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(titleBox(data.dossierCode()));
            document.add(spacer());
            document.add(introParagraph(data));
            document.add(spacer());
            document.add(fieldsTable(data));
            document.add(spacer());
            document.add(descriptionParagraph(data));
            document.add(spacer());
            document.add(fixedWarningParagraph());
            document.add(spacer());
            document.add(new Paragraph("CONDITION DE RESERVATION", SECTION));
            document.add(spacer());
            document.add(conditionsParagraph());
            document.add(spacer());
            document.add(spacer());

            Paragraph fait = new Paragraph();
            fait.add(new Chunk("Fait le ", NORMAL));
            fait.add(new Chunk(date(data.dateEdition()), BOLD));
            document.add(fait);
            document.add(spacer());
            document.add(spacer());
            document.add(signatureTable());
            document.add(spacer());
            document.add(spacer());
            document.add(footerBar());
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche de réservation", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static PdfPTable titleBox(String dossierCode) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(35);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(ORANGE);
        cell.setBorderWidth(1.5f);
        cell.setPadding(6);
        Paragraph p = new Paragraph();
        p.add(new Chunk("N°V: ", TITLE_FONT));
        p.add(new Chunk(vide(dossierCode), BOLD));
        cell.addElement(p);
        table.addCell(cell);
        return table;
    }

    private static Paragraph introParagraph(FicheReservationData data) {
        Paragraph p = new Paragraph(
                "Nous soussignés, la Société dite « SOCIETE " + vide(data.entrepriseNom()) + " (groupe AYAD "
                        + "DEVELOPPEMENT) », société à Responsabilité limitée, sise à Casablanca, 100 Boulvard "
                        + "Zerktouni, représentées par l'un des mandataires dûment habilité à cet effet.\n"
                        + "Agissant en notre qualité de promoteur immobilier, reconnaissons sous réserve des conditions "
                        + "ci-dessous avoir reçu de :",
                NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static PdfPTable fieldsTable(FicheReservationData data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{35, 65});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        addRow(table, "M", vide(data.clientNom()), false);
        addRow(table, "Demeurant à", vide(data.clientAdresse()), false);
        addRow(table, "Passeport ou CIN N°", vide(data.clientCin()), false);
        addRow(table, "Mode de paiement", vide(data.modePaiement()), false);
        addRow(table, "Montant de", montant(data.montant()), true);
        addRow(table, "Réglé par", vide(data.reglePar()), false);
        addRow(table, "Prix article", montant(data.prixArticle()), true);
        return table;
    }

    private static void addRow(PdfPTable table, String label, String value, boolean valueRightAligned) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, LABEL_BOLD));
        labelCell.setBackgroundColor(GRAY_LABEL);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, valueRightAligned ? BOLD : NORMAL));
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(valueRightAligned ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    private static Paragraph descriptionParagraph(FicheReservationData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("A titre de réservation en sa faveur d'un appartement portant le n° ")
                .append(vide(data.articleCode())).append(", imm ").append(vide(data.immeubleCode()))
                .append(", porte ").append(vide(data.porte())).append(", au ").append(vide(data.etage()))
                .append(", d'une superficie totale approximative de (")
                .append(data.superficie() != null ? data.superficie().toPlainString() : "")
                .append(" m²) lequel lot fera l'objet d'un titre foncier");
        if (data.projetNom() != null) {
            sb.append(", au sein du projet ").append(data.projetNom());
        }
        if (data.projetAdresse() != null) {
            sb.append(" à ").append(data.projetAdresse());
        }
        sb.append(".");

        Paragraph p = new Paragraph(sb.toString(), NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static Paragraph fixedWarningParagraph() {
        Paragraph p = new Paragraph(
                "A défaut de l'encaissement effectif du montant ci-dessus, la réservation est annulée sans "
                        + "formalités.\n"
                        + "Des frais d'immobilisation seront déduits de l'avance à hauteur de 10% de la valeur totale "
                        + "du bien en cas de non aboutissement de la transaction.",
                NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static Paragraph conditionsParagraph() {
        Paragraph p = new Paragraph(
                "- Le client s'engage à signer le contrat de réservation dans un délai maximum de 15 JOURS à "
                        + "compter de la date de la signature du présent reçu de réservation, et sous condition de "
                        + "l'encaissement effectif du montant de réservation.\n\n"
                        + "- Ce document ne constitue en aucun cas un acte de vente.",
                NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static PdfPTable signatureTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        PdfPCell promoteur = new PdfPCell(new Paragraph("LE PROMOTEUR", SECTION));
        promoteur.setBorder(Rectangle.NO_BORDER);
        PdfPCell client = new PdfPCell(new Paragraph("LE CLIENT", SECTION));
        client.setBorder(Rectangle.NO_BORDER);
        table.addCell(promoteur);
        table.addCell(client);
        return table;
    }

    private static PdfPTable footerBar() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Paragraph(
                "AYAD DEVELOPPEMENT — 100 Boulvard Zerktouni, Casablanca, Maroc — "
                        + "tel: +212 5 22 27 34 96 / +212 6 45 45 45 50 — contact@ayaddeveloppement.com",
                FOOTER));
        cell.setBackgroundColor(ORANGE);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        return table;
    }
}
