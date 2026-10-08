package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheBancaireData;
import com.tsix.immocrm.dto.dossier.FicheBancaireData.Acquereur;
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
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.pourcentageRatio;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/** « Fiche Bancaire » en PDF (template legacy « FICHE BANCAIRE », appui de demande de crédit). */
@Component
public class FicheBancairePdfRenderer {

    private static final Color GRAY_HEADER = new Color(0xD9, 0xD9, 0xD9);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    private static final Font WARNING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.RED);

    public byte[] render(FicheBancaireData data) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(headerParagraph(data));
            document.add(spacer());

            if (data.dateFiche() == null) {
                document.add(new Paragraph(
                        "Vous devez saisir la date de la fiche bancaire dans le dossier de vente !!!", WARNING));
                document.add(spacer());
            }

            Paragraph title = new Paragraph("FICHE BANCAIRE CLIENT - " + vide(data.dossierCode()), TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(spacer());

            document.add(sectionLabel("I - INFORMATIONS PERSONNELLES OU ETAT CIVIL :"));
            document.add(acquereursTable(data.acquereurs()));
            document.add(spacer());

            document.add(sectionLabel("II - DESIGNATION DU BIEN :"));
            document.add(designationTable(data));
            document.add(spacer());

            document.add(sectionLabel("III - PRIX DU BIEN / MODALITE DE PAIEMENT :"));
            document.add(prixTable(data));
            document.add(spacer());
            document.add(spacer());

            document.add(new Paragraph("Signature de(s) acquéreur(s)", SECTION));
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche bancaire", e);
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

    private static Paragraph headerParagraph(FicheBancaireData data) {
        Paragraph p = new Paragraph();
        p.add(new Chunk("SOCIETE : ", BOLD));
        p.add(new Chunk(vide(data.entrepriseNom()) + "   ", NORMAL));
        p.add(new Chunk("PROGRAMME : ", BOLD));
        p.add(new Chunk(vide(data.projetNom()) + "   ", NORMAL));
        p.add(new Chunk("ADRESSE : ", BOLD));
        p.add(new Chunk(vide(data.projetAdresse()), NORMAL));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(vide(data.villeSignature()) + " le " + date(data.dateFiche()), NORMAL));
        return p;
    }

    private static PdfPTable acquereursTable(List<Acquereur> acquereurs) {
        PdfPTable table = new PdfPTable(2 + Math.max(acquereurs.size(), 1));
        table.setWidthPercentage(100);

        table.addCell(headerCell("Libellé"));
        for (int i = 0; i < acquereurs.size(); i++) {
            table.addCell(headerCell("Acquéreur " + (i + 1)));
        }
        if (acquereurs.isEmpty()) {
            table.addCell(headerCell("Acquéreur 1"));
        }
        table.addCell(headerCell(""));

        addAcquereurRow(table, "Nom & Prénom", acquereurs, Acquereur::nom);
        addAcquereurRow(table, "Profession", acquereurs, Acquereur::profession);
        addAcquereurRow(table, "Adresse", acquereurs, Acquereur::adresse);
        addAcquereurRow(table, "Date Nai.", acquereurs, a -> date(a.dateNaissance()));
        addAcquereurRow(table, "Lieu Nai.", acquereurs, Acquereur::lieuNaissance);
        addAcquereurRow(table, "Tél", acquereurs, Acquereur::telephone);
        addAcquereurRow(table, "Pièce Ident.", acquereurs, Acquereur::pieceIdentite);
        addAcquereurRow(table, "Situa. Fam.", acquereurs, Acquereur::situationFamiliale);
        addAcquereurRow(table, "Part", acquereurs, a -> a.partPercent() != null ? pourcentageRatio(a.partPercent()) : "");
        return table;
    }

    private static void addAcquereurRow(PdfPTable table, String label, List<Acquereur> acquereurs,
                                         java.util.function.Function<Acquereur, String> extractor) {
        table.addCell(labelCell(label));
        int cols = Math.max(acquereurs.size(), 1);
        for (int i = 0; i < cols; i++) {
            String value = i < acquereurs.size() ? vide(extractor.apply(acquereurs.get(i))) : "";
            table.addCell(tableCell(value));
        }
    }

    private static PdfPTable designationTable(FicheBancaireData data) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        row(table, "CONSISTANCE :", vide(data.categorieLibelle()), "IMMEUBLE :", vide(data.immeubleCode()));
        row(table, "ETAGE :", vide(data.etage()), "PRODUIT :", vide(data.articleCode()));
        row(table, "FRACTION DIVISE N° :", vide(data.fractionDivisee()), "PROPRIETE DITE :", vide(data.proprieteDite()));
        row(table, "LOGEMENT A USAGE :", vide(data.logementUsage()), "SUPERFICIE DEFINITIVE :",
                data.superficie() != null ? data.superficie().toPlainString() + " m²" : "");
        row(table, "T.F FONCIER MERE IMMEUBLE :", vide(data.titreFoncierMereImmeuble()),
                "TITRE FONCIER PARCELAIRE N° :", vide(data.titreFoncierParcellaire()));
        row(table, "PARTIES DIVISES :", data.partiesDivisesSur10000() != null ? data.partiesDivisesSur10000() + " / 10000" : "",
                "PARTIES INDIVISES :", data.partiesIndivisesSur10000() != null ? data.partiesIndivisesSur10000() + " / 10000" : "");
        row(table, "FRACTION DIVISE TERRAIN :", vide(data.fractionDiviseeTerrain()),
                "SUPERFICIE DES SOUS ARTICLES :",
                data.superficieSousArticles() != null ? data.superficieSousArticles().toPlainString() + " m²" : "");
        return table;
    }

    private static void row(PdfPTable table, String label1, String value1, String label2, String value2) {
        table.addCell(labelCell(label1));
        table.addCell(tableCell(value1));
        table.addCell(labelCell(label2));
        table.addCell(tableCell(value2));
    }

    private static PdfPTable prixTable(FicheBancaireData data) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell(headerCell(""));
        table.addCell(headerCell("Prix"));
        table.addCell(headerCell("Versé"));
        table.addCell(headerCell("Reliquat"));

        montantRow(table, "PRIX DU BIEN", data.prixApresRemise(), data.montantVerse(), data.reliquat());
        montantRow(table, "FRAIS DE DOSSIER", data.fraisDossier(), data.fraisDossierVerse(), data.fraisDossierReliquat());
        montantRow(table, "FRAIS DE SYNDIC", data.fraisSyndic(), data.fraisSyndicVerse(), data.fraisSyndicReliquat());
        return table;
    }

    private static void montantRow(PdfPTable table, String label, BigDecimal prix, BigDecimal verse, BigDecimal reliquat) {
        table.addCell(labelCell(label));
        table.addCell(amountCell(montant(prix)));
        table.addCell(amountCell(montant(verse)));
        table.addCell(amountCell(montant(reliquat)));
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

    private static PdfPCell amountCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_CELL));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
