package com.tsix.immocrm.services.prospection.document;

import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import com.tsix.immocrm.dto.prospection.OffreDevisData;
import com.tsix.immocrm.dto.prospection.OffreDevisData.SousArticleLigne;
import com.tsix.immocrm.services.dossier.document.DocumentRenderingException;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static com.tsix.immocrm.services.prospection.document.MontantFormatter.date;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.pourcentage;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.pourcentageRatio;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.surface;
import static com.tsix.immocrm.services.prospection.document.MontantFormatter.vide;

/**
 * Renders the "Fiche Offre vente" as a PDF, modeled on the legacy template (see
 * {@code instructions/templates/priceOffer/OFFRE DE PRIX.pdf} and its
 * {@code logo (1).png}). Uses OpenPDF ({@code org.openpdf.text}) — pure Java, no external process.
 */
@Component
public class OffreDevisPdfRenderer {

    private static final String LOGO_CLASSPATH = "documents/ayad-logo.png";

    private static final Color PURPLE = new Color(0x5A, 0x2C, 0x6E);
    private static final Color GRAY_LABEL = new Color(0xE4, 0xE4, 0xE4);
    private static final Color GRAY_HEADER = new Color(0xD9, 0xD9, 0xD9);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font LABEL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);

    public byte[] render(OffreDevisData data) {
        Document document = new Document(PageSize.A4, 40, 40, 30, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addLogo(document);
            document.add(spacer());

            Paragraph title = new Paragraph(
                    "Fiche Offre vente N° " + vide(data.code()) + " du " + date(data.date()), TITLE_FONT);
            document.add(title);
            document.add(spacer());

            document.add(sectionLabel("ENTETE"));
            document.add(entradeTable(data));
            document.add(spacer());

            document.add(sectionLabel("MONTANTS"));
            document.add(montantsTable(data));
            document.add(spacer());

            if (data.schemaFinancement() != null && !data.schemaFinancement().isEmpty()) {
                document.add(sectionLabel("SCHEMA DE FINANCEMENT"));
                document.add(schemaFinancementTable(data.schemaFinancement()));
                document.add(spacer());
            }

            if (!data.sousArticles().isEmpty()) {
                document.add(sectionLabel("SOUS ARTICLES"));
                document.add(sousArticlesTable(data.sousArticles()));
                document.add(spacer());
            }

            document.add(spacer());
            document.add(footerBar());
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche offre vente", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static void addLogo(Document document) throws IOException {
        try (InputStream in = new ClassPathResource(LOGO_CLASSPATH).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            Image logo = Image.getInstance(bytes);
            logo.scaleToFit(160, 60);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        } catch (Exception e) {
            // Le logo est décoratif — un échec de chargement ne doit pas bloquer la génération du PDF.
        }
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static PdfPTable sectionLabel(String text) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Paragraph(text, SECTION_FONT));
        cell.setBorderColor(Color.BLACK);
        cell.setPadding(4);
        table.addCell(cell);
        return table;
    }

    // -- ENTETE -----------------------------------------------------------------------------

    private static PdfPTable entradeTable(OffreDevisData data) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18, 32, 18, 32});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        addRow(table, "Code", vide(data.code()), "Date", date(data.date()));
        addFullRow(table, "Client", vide(data.clientNom()));
        addFullRow(table, "Tranche", vide(data.trancheDescription()));
        addRow(table, "Article", vide(data.articleCode()), "Sup. (m²)", surface(data.superficie()));
        addRow(table, "Catégorie", vide(data.categorieLibelle()), "Type", vide(data.typeLibelle()));
        addFullRow(table, "Commercial", vide(data.commercialNom()));
        addFullRow(table, "Mode de paie.", vide(data.modePaiementLibelle()));
        return table;
    }

    private static void addRow(PdfPTable table, String label1, String value1, String label2, String value2) {
        table.addCell(labelCell(label1));
        table.addCell(valueCell(value1));
        table.addCell(labelCell(label2));
        table.addCell(valueCell(value2));
    }

    private static void addFullRow(PdfPTable table, String label, String value) {
        table.addCell(labelCell(label));
        PdfPCell cell = valueCell(value);
        cell.setColspan(3);
        table.addCell(cell);
    }

    private static PdfPCell labelCell(String label) {
        PdfPCell cell = new PdfPCell(new Paragraph(label, LABEL_BOLD));
        cell.setBackgroundColor(GRAY_LABEL);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell valueCell(String value) {
        PdfPCell cell = new PdfPCell(new Paragraph(value, NORMAL));
        cell.setPadding(4);
        return cell;
    }

    // -- MONTANTS ---------------------------------------------------------------------------

    private static PdfPTable montantsTable(OffreDevisData data) {
        var m = data.montants();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{28, 27, 27, 18});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        table.addCell(headerCell(""));
        table.addCell(headerCell("HT"));
        table.addCell(headerCell("TTC"));
        table.addCell(headerCell(""));

        montantRow(table, "Prix du m²", m.prixM2HT(), m.prixM2TTC(), "TVA " + pourcentageRatio(m.tvaRatio()));
        montantRow(table, "Prix article", m.prixArticleHT(), m.prixArticleTTC(), "");
        montantRow(table, "S. articles", m.prixSousArticlesHT(), m.prixSousArticlesTTC(), "");
        montantRow(table, "Total", m.totalHT(), m.totalTTC(), "");
        montantRow(table, "Remise", m.remiseHT(), m.remiseTTC(), pourcentage(m.remisePercent()));
        montantRow(table, "Prix du m²", m.prixM2ApresRemiseHT(), m.prixM2ApresRemiseTTC(), "");
        montantRow(table, "Prix article", m.prixArticleApresRemiseHT(), m.prixArticleApresRemiseTTC(), "");
        montantRowBold(table, "Prix de vente", m.prixVenteHT(), m.prixVenteTTC(), pourcentage(m.remisePercent()));
        if (m.commissionPercent() != null) {
            table.addCell(labelCell("Commission"));
            // CommissionRate is stored as a ratio (e.g. 0.03 = 3%), same as VATRatio — never
            // written by this app's own API yet, only ever read back (possibly legacy data).
            table.addCell(amountCell(pourcentageRatio(m.commissionPercent()), NORMAL));
            table.addCell(amountCell("", NORMAL));
            table.addCell(amountCell("", NORMAL));
        }
        return table;
    }

    private static void montantRow(PdfPTable table, String label, BigDecimal ht, BigDecimal ttc, String note) {
        table.addCell(labelCell(label));
        table.addCell(amountCell(montant(ht), NORMAL));
        table.addCell(amountCell(montant(ttc), NORMAL));
        table.addCell(amountCell(note, NORMAL));
    }

    private static void montantRowBold(PdfPTable table, String label, BigDecimal ht, BigDecimal ttc, String note) {
        table.addCell(labelCell(label));
        table.addCell(amountCell(montant(ht), BOLD));
        table.addCell(amountCell(montant(ttc), BOLD));
        table.addCell(amountCell(note, BOLD));
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER));
        cell.setBackgroundColor(GRAY_HEADER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell amountCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    // -- SCHEMA DE FINANCEMENT ----------------------------------------------------------------

    private static PdfPTable schemaFinancementTable(List<LigneSchemaFinancementDto> lignes) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{60, 40});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        table.addCell(headerCell("Catégorie"));
        table.addCell(headerCell("Montant"));

        BigDecimal total = BigDecimal.ZERO;
        for (LigneSchemaFinancementDto ligne : lignes) {
            table.addCell(tableCell(vide(ligne.categorie()), Element.ALIGN_LEFT));
            table.addCell(amountCell(montant(ligne.montant()), TABLE_CELL));
            if (ligne.montant() != null) {
                total = total.add(ligne.montant());
            }
        }
        table.addCell(labelCell("Total"));
        table.addCell(amountCell(montant(total), BOLD));
        return table;
    }

    // -- SOUS ARTICLES ------------------------------------------------------------------------

    private static PdfPTable sousArticlesTable(List<SousArticleLigne> lignes) {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{12, 16, 10, 8, 16, 8, 12, 18});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        for (String header : new String[]{"Code", "Catégorie", "Tjs affecté", "Sup. (m²)",
                "Prix TTC avant remise", "Remise", "Montant remise", "Prix TTC après remise"}) {
            table.addCell(headerCell(header));
        }

        for (SousArticleLigne ligne : lignes) {
            table.addCell(tableCell(vide(ligne.code()), Element.ALIGN_LEFT));
            table.addCell(tableCell(vide(ligne.categorieLibelle()), Element.ALIGN_LEFT));
            table.addCell(tableCell(ligne.toujoursAffecte() ? "✔" : "", Element.ALIGN_CENTER));
            table.addCell(tableCell(surface(ligne.superficie()), Element.ALIGN_RIGHT));
            table.addCell(tableCell(montant(ligne.prixTTCavantRemise()), Element.ALIGN_RIGHT));
            table.addCell(tableCell(pourcentage(ligne.remisePercent()), Element.ALIGN_RIGHT));
            table.addCell(tableCell(montant(ligne.montantRemise()), Element.ALIGN_RIGHT));
            table.addCell(tableCell(montant(ligne.prixTTCapresRemise()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private static PdfPCell tableCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_CELL));
        cell.setPadding(3);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    // -- FOOTER -------------------------------------------------------------------------------

    private static PdfPTable footerBar() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Paragraph(
                "AYAD DEVELOPPEMENT — 100 Boulvard Zerktouni, Casablanca, Maroc — "
                        + "tel: +212 5 22 27 34 96 / +212 6 35 70 68 10 / +212 6 01 01 01 03 — "
                        + "contact@ayaddeveloppement.com — www.mansouriabeach.com",
                FOOTER));
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(PURPLE);
        cell.setPadding(6);
        table.addCell(cell);
        return table;
    }
}
