package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.RecuReglementData;
import com.tsix.immocrm.dto.dossier.RecuReglementData.Beneficiaire;
import com.tsix.immocrm.dto.dossier.RecuReglementData.LigneReglement;
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
import java.util.List;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Reçu de règlement" as a PDF, modeled band-for-band on "Page1" of the legacy
 * Stimulsoft report source
 * {@code instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_20240301102347.yes} — same
 * reading order (attestation → désignation du bien → situation des chèques → disclaimer →
 * signature), table columns (Date / Montant / Mode / N° Vers) and column proportions (2.2 / 2.6 /
 * 10.4 / 1.4 cm in the original, kept as relative widths here). Uses OpenPDF
 * ({@code org.openpdf.text}). Font family (Century Gothic / Times New Roman in the original) is
 * substituted with Helvetica, same precedent as every other renderer in this package.
 */
@Component
public class RecuReglementPdfRenderer {

    /** {@code AdmParamsString} rows confirmed on real data ({@code CurrencyLabel}/{@code CurrencyFraction}), same as la Fiche Trop Perçu. */
    private static final String DEVISE = "dirham";
    private static final String DEVISE_FRACTION = "centime";

    private static final Font TITLE = FontFactory.getFont(FontFactory.TIMES_BOLD, 14, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font TABLE_CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

    /** Relative widths of the "Date" / "Montant" / "Mode" / "N° Vers" columns — 2.2 / 2.6 / 10.4 / 1.4 cm in the original. */
    private static final float[] COLONNES_VERSEMENTS = {2.2f, 2.6f, 10.4f, 1.4f};

    public byte[] render(RecuReglementData data) {
        Document document = new Document(PageSize.A4, 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph titre = new Paragraph(
                    "ATTESTATION DE RÉCEPTION CHEQUE N° " + vide(data.dossierCode()), TITLE);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            Paragraph recu = new Paragraph("RECU_" + vide(data.receiptCode()), TITLE);
            recu.setAlignment(Element.ALIGN_CENTER);
            document.add(recu);
            document.add(spacer());

            document.add(attestationParagraph(data));
            document.add(spacer());

            document.add(new Paragraph(designationBien(data), BOLD));
            document.add(new Paragraph("Type : " + vide(data.typeLibelle()), BOLD));
            document.add(new Paragraph(superficieLigne(data), BOLD));
            document.add(new Paragraph(prixLigne(data), BOLD));
            document.add(spacer());

            document.add(new Paragraph("Nous vous rappelons ci-après la situation des chèques reçus :", BOLD));
            document.add(spacer());
            document.add(versementsTable(data));
            document.add(spacer());

            document.add(new Paragraph("Reste à payer : " + montant(data.resteAPayer()), BOLD));
            document.add(spacer());

            document.add(new Paragraph(
                    "En foi de quoi la présente est délivrée pour servir et valoir ce que de droit. "
                            + "Cette attestation n'est valide qu'après l'encaissement du montant mentionné.",
                    NORMAL));
            document.add(spacer());

            Paragraph villeDate = new Paragraph(vide(data.villeSignature()) + " le, " + date(data.dateSignature()), NORMAL);
            villeDate.setAlignment(Element.ALIGN_RIGHT);
            document.add(villeDate);

            if (data.superficieApproximative()) {
                document.add(spacer());
                document.add(new Paragraph(
                        "* La superficie étant approximative, le prix définitif sera arrêté dès la création des titres fonciers.",
                        FOOTNOTE));
            }
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF du reçu de règlement", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    /**
     * Versement (non-IsCredit) : "Nous soussignés société {entreprise} reconnaissons avoir reçu
     * de {bénéficiaire}...". Remboursement (IsCredit) : ordre inversé, "Nous soussignés
     * {bénéficiaire} reconnaissons avoir reçu de la société {entreprise}..." — voir
     * {@code GroupHeaderBand2.Text3/Text4} du template legacy.
     */
    private static Paragraph attestationParagraph(RecuReglementData data) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        String beneficiaires = beneficiairesTexte(data);
        String montantTexte = montant(data.montantRegleDossier()) + " ("
                + NombreEnLettresFr.montant(data.montantRegleDossier(), DEVISE, DEVISE_FRACTION) + ")";
        if (data.remboursement()) {
            p.add(new Chunk("Nous soussignés " + beneficiaires
                    + ", reconnaissons avoir reçu de la société " + vide(data.entrepriseNom())
                    + " " + vide(data.entrepriseFormeJuridique())
                    + " dont le siège social est à " + vide(data.entrepriseAdresse())
                    + ", la somme de " + montantTexte
                    + " par " + vide(data.modePaiement())
                    + " représentant un remboursement sur l'acquisition de :", NORMAL));
        } else {
            p.add(new Chunk("Nous soussignés société " + vide(data.entrepriseNom())
                    + " " + vide(data.entrepriseFormeJuridique())
                    + " dont le siège social est à " + vide(data.entrepriseAdresse())
                    + ", reconnaissons avoir reçu de " + beneficiaires
                    + ", la somme de " + montantTexte
                    + " par " + vide(data.modePaiement())
                    + " représentant un versement sur l'acquisition de :", NORMAL));
        }
        return p;
    }

    private static String beneficiairesTexte(RecuReglementData data) {
        StringBuilder sb = new StringBuilder();
        appendBeneficiaire(sb, data.beneficiaire());
        String conjonction = data.remboursement() ? " et " : " et de ";
        for (Beneficiaire coAcquereur : data.coAcquereurs()) {
            sb.append(conjonction);
            appendBeneficiaire(sb, coAcquereur);
        }
        return sb.toString();
    }

    private static void appendBeneficiaire(StringBuilder sb, Beneficiaire beneficiaire) {
        if (beneficiaire == null) {
            return;
        }
        sb.append(vide(beneficiaire.titre())).append(' ').append(vide(beneficiaire.nom()));
        if (beneficiaire.pieceIdentite() != null && !beneficiaire.pieceIdentite().isBlank()) {
            sb.append(", ").append(beneficiaire.pieceIdentite());
        }
    }

    private static String designationBien(RecuReglementData data) {
        return vide(data.articleCategorieLibelle()) + " n° " + vide(data.articleCode())
                + ", situé dans le projet " + vide(data.projetDescription()) + ", comprenant :";
    }

    private static String superficieLigne(RecuReglementData data) {
        String label = "Superficie" + (data.superficieApproximative() ? " approximative" : "");
        String valeur = data.superficie() != null
                ? data.superficie().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "";
        return label + " : " + valeur + " m²" + (data.superficieApproximative() ? " *" : "");
    }

    private static String prixLigne(RecuReglementData data) {
        return "Prix " + vide(data.articleCategorieLibelle()) + " : " + montant(data.prixApresRemise())
                + (data.superficieApproximative() ? "*" : "");
    }

    private static PdfPTable versementsTable(RecuReglementData data) {
        PdfPTable table = new PdfPTable(COLONNES_VERSEMENTS);
        table.setWidthPercentage(87);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(headerCell("Date"));
        table.addCell(headerCell("Montant"));
        table.addCell(headerCell("Mode"));
        table.addCell(headerCell("N° Vers"));

        int ligne = 1;
        for (LigneReglement versement : data.lignesReglement()) {
            table.addCell(bodyCell(date(versement.date()), Element.ALIGN_LEFT, TABLE_CELL));
            table.addCell(bodyCell(montant(versement.montant()), Element.ALIGN_RIGHT, TABLE_CELL));
            table.addCell(bodyCell(vide(versement.mode()), Element.ALIGN_LEFT, TABLE_CELL));
            table.addCell(bodyCell(String.valueOf(ligne++), Element.ALIGN_CENTER, TABLE_CELL));
        }

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
