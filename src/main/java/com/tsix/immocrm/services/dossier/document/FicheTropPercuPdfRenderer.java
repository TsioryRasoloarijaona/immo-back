package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheTropPercuData;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.LigneReglement;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData.SousArticle;
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
import java.math.BigDecimal;
import java.util.List;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Fiche Trop Perçu" as a PDF, modeled band-for-band on the legacy Stimulsoft report
 * source {@code instructions/templates/ComSaleFicheTropPercu.yes} — same reading order, section
 * texts, table columns (N° Vers / Date / Montant / Mode) and column proportions (1.4 / 2.2 / 2.6 /
 * 10.4 cm in the original, kept as relative widths here). Uses OpenPDF ({@code org.openpdf.text}).
 * Font family (Century Gothic / Times New Roman in the original) is substituted with Helvetica,
 * same precedent as every other renderer in this package — sizes and weights (bold/regular) are
 * kept exact.
 */
@Component
public class FicheTropPercuPdfRenderer {

    /** {@code AdmParamsString} rows confirmed on real data ({@code CurrencyLabel}/{@code CurrencyFraction}). */
    private static final String DEVISE = "dirham";
    private static final String DEVISE_FRACTION = "centime";

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font TABLE_CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

    /** Relative widths of the "N° Vers" / "Date" / "Montant" / "Mode" columns — 1.4 / 2.2 / 2.6 / 10.4 cm in the original. */
    private static final float[] COLONNES_VERSEMENTS = {1.4f, 2.2f, 2.6f, 10.4f};

    public byte[] render(FicheTropPercuData data) {
        Document document = new Document(PageSize.A4, 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph titre = new Paragraph("TROP PERÇU N° " + vide(data.dossierCode()), TITLE);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(spacer());

            document.add(receuParagraph(data));
            document.add(spacer());

            document.add(new Paragraph(designationBien(data), BOLD));
            document.add(new Paragraph("Type : " + vide(data.typeLibelle()), BOLD));
            document.add(new Paragraph(superficieLigne(data), BOLD));
            for (SousArticle sousArticle : data.sousArticles()) {
                document.add(new Paragraph(sousArticleLigne(sousArticle, data.superficieApproximative()), BOLD));
            }
            document.add(spacer());

            document.add(new Paragraph("En foi de quoi la présente est délivrée pour servir et valoir ce que de droit.", NORMAL));
            Paragraph villeDate = new Paragraph(vide(data.villeSignature()) + " le, " + date(data.dateEdition()), NORMAL);
            villeDate.setAlignment(Element.ALIGN_RIGHT);
            document.add(villeDate);
            document.add(new Paragraph(prixLigne(data), BOLD));
            document.add(new Paragraph("Totals des frais : " + montant(data.fraisTotal()) + ".", BOLD));
            document.add(new Paragraph("Nous vous rappelons ci-après la situation de vos versements :", BOLD));
            document.add(spacer());

            document.add(versementsTable(data));
            document.add(spacer());

            document.add(new Paragraph("Reste à payer : " + montant(data.resteAPayer()), BOLD));

            if (data.superficieApproximative()) {
                document.add(spacer());
                document.add(new Paragraph(
                        "* La superficie étant approximative, le prix définitif sera arrêté dès la création des titres fonciers.",
                        FOOTNOTE));
            }
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche trop perçu", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static Paragraph receuParagraph(FicheTropPercuData data) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.add(new Chunk("Nous soussignés société " + vide(data.entrepriseNom())
                + ", dont le siège social est à " + vide(data.entrepriseAdresse())
                + ", reconnaissons avoir reçu de " + acquereursTexte(data.acquereurs())
                + ", un trop perçu de " + montant(data.montantTropPercu())
                + " (" + NombreEnLettresFr.montant(data.montantTropPercu(), DEVISE, DEVISE_FRACTION)
                + ") sur l'acquisition de : ", NORMAL));
        return p;
    }

    private static String acquereursTexte(List<FicheTropPercuData.Acquereur> acquereurs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < acquereurs.size(); i++) {
            FicheTropPercuData.Acquereur a = acquereurs.get(i);
            if (i > 0) {
                sb.append(" et de ");
            }
            sb.append(vide(a.titre())).append(' ').append(vide(a.nom()));
            if (a.pieceIdentite() != null && !a.pieceIdentite().isBlank()) {
                sb.append(", ").append(a.pieceIdentite());
            }
        }
        return sb.toString();
    }

    private static String designationBien(FicheTropPercuData data) {
        return vide(data.articleCategorieLibelle()) + " n° " + vide(data.articleCode())
                + ", situé dans le projet " + vide(data.projetDescription()) + ", comprenant :";
    }

    private static String superficieLigne(FicheTropPercuData data) {
        String label = "Superficie" + (data.superficieApproximative() ? " approximative" : "");
        String valeur = data.superficie() != null ? data.superficie().toPlainString() : "";
        return label + " : " + valeur + " m²" + (data.superficieApproximative() ? " *" : "");
    }

    private static String sousArticleLigne(SousArticle sousArticle, boolean superficieApproximative) {
        boolean parking = "PAR".equals(sousArticle.categorieCode());
        String label = vide(sousArticle.categorieLibelle())
                + (parking ? " N°" : (superficieApproximative ? " approximative" : ""));
        String valeur = parking ? vide(sousArticle.code())
                : (sousArticle.area() != null ? sousArticle.area().toPlainString() + " m²" : "");
        return label + " : " + valeur;
    }

    private static String prixLigne(FicheTropPercuData data) {
        return "Prix " + vide(data.articleCategorieLibelle()) + " : " + montant(data.prixApresRemise())
                + (data.superficieApproximative() ? "*" : "") + " , soit " + montant(data.unitCost()) + " le m².";
    }

    private static PdfPTable versementsTable(FicheTropPercuData data) {
        PdfPTable table = new PdfPTable(COLONNES_VERSEMENTS);
        table.setWidthPercentage(87); // 16.6cm de tableau sur ~19cm de zone de contenu, comme l'original
        table.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(headerCell("N° Vers"));
        table.addCell(headerCell("Date"));
        table.addCell(headerCell("Montant"));
        table.addCell(headerCell("Mode"));

        int ligne = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (LigneReglement versement : data.lignesReglement()) {
            table.addCell(bodyCell(String.valueOf(ligne++), Element.ALIGN_CENTER, TABLE_CELL));
            table.addCell(bodyCell(date(versement.date()), Element.ALIGN_LEFT, TABLE_CELL));
            table.addCell(bodyCell(montant(versement.montant()), Element.ALIGN_RIGHT, TABLE_CELL));
            table.addCell(bodyCell(vide(versement.mode()), Element.ALIGN_LEFT, TABLE_CELL));
            total = total.add(versement.montant() != null ? versement.montant().abs() : BigDecimal.ZERO);
        }

        table.addCell(bodyCell("", Element.ALIGN_CENTER, TABLE_CELL_BOLD));
        table.addCell(bodyCell("Total", Element.ALIGN_CENTER, TABLE_CELL_BOLD));
        table.addCell(bodyCell(montant(data.totalRegle() != null ? data.totalRegle() : total), Element.ALIGN_RIGHT, TABLE_CELL_BOLD));
        table.addCell(bodyCell("", Element.ALIGN_LEFT, TABLE_CELL_BOLD));

        return table;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell bodyCell(String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
