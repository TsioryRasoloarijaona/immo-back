package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.AutorisationVersementData;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.ClientAutorisation;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData.Payeur;
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
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * « Autorisation d'Encaissement », d'après {@code autorisation_virement/Report.pdf} : <b>une seule
 * page</b> (marges serrées pour cela), et une seule bordure, autour du « Cadre réservé à la banque ».
 */
@Component
public class AutorisationVersementPdfRenderer {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Font TITLE = FontFactory.getFont(FontFactory.TIMES_BOLD, 13, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.TIMES_ROMAN, 9, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.TIMES_BOLD, 9, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.TIMES_BOLD, 9, Color.BLACK);
    private static final Font FOOTNOTE = FontFactory.getFont(FontFactory.TIMES_ROMAN, 7, Color.BLACK);

    public byte[] render(AutorisationVersementData data) {
        Document document = new Document(PageSize.A4, 30, 30, 24, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph titre = new Paragraph("AUTORISATION D'ENCAISSEMENT", TITLE);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(10);
            document.add(titre);

            document.add(enteteTable(data));

            Paragraph montantEmission = montantEmissionParagraph(data);
            montantEmission.setSpacingBefore(6);
            document.add(montantEmission);

            PdfPTable coordonnees = coordonneesTable(data);
            coordonnees.setSpacingBefore(6);
            document.add(coordonnees);

            Paragraph conditions = conditionsGeneralesParagraph(data);
            conditions.setSpacingBefore(8);
            document.add(conditions);
            document.add(ligneSeparation());

            Paragraph note3 = new Paragraph(
                    "(3) Date de l'émission de l'autorisation de versement, cette dernière est à retourner "
                            + delai(data) + "h à compter de cette date.", FOOTNOTE);
            note3.setSpacingBefore(2);
            document.add(note3);
            document.add(new Paragraph(
                    "(4) La signature et le cachet de la société ne valent en aucun cas engagement de cette "
                            + "dernière à vendre le bien sus-indiqué, mais ils constituent simple autorisation "
                            + "à la banque d'accepter le versement du montant figurant sur la présente "
                            + "autorisation de versement.", FOOTNOTE));

            PdfPTable cadre = cadreBanque(data);
            cadre.setSpacingBefore(10);
            document.add(cadre);
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de l'autorisation d'encaissement", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph ligneSeparation() {
        Paragraph p = new Paragraph("_______________________________________________________________", NORMAL);
        p.setSpacingBefore(4);
        return p;
    }

    private static String delai(AutorisationVersementData data) {
        return data.delaiRetourHeures() != null ? String.valueOf(data.delaiRetourHeures()) : "";
    }

    // -- Bandeau : Opération / N° bien / Prix / Mode (gauche) — Clients (droite) --

    private static PdfPTable enteteTable(AutorisationVersementData data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        setWidths(table, 50, 50);

        Paragraph gauche = new Paragraph();
        ligne(gauche, "Opération : ", vide(data.operationLibelle()));
        ligne(gauche, "N° du bien : ", vide(data.articleCode()));
        ligne(gauche, "Prix total de vente (T.T.C) : ", montant(data.prixVenteTTC()));
        ligne(gauche, "Mode de versement : ", vide(data.modeVersement()));
        table.addCell(sansBordure(gauche));

        Paragraph droite = new Paragraph();
        droite.add(new Chunk("Nom et prénom des clients :\n", BOLD));
        droite.add(clientsChunk(data.clients()));
        table.addCell(sansBordure(droite));

        return table;
    }

    // -- Montant à verser + date d'émission --

    private static Paragraph montantEmissionParagraph(AutorisationVersementData data) {
        Paragraph p = new Paragraph();
        String montantTexte = montant(data.montantAVerser()) + " ("
                + NombreEnLettresFr.montant(data.montantAVerser(), "dirham", "centime") + ")";
        p.add(new Chunk("Montant à verser : ", NORMAL));
        p.add(new Chunk(montantTexte + "\n", BOLD));
        p.add(new Chunk("Emis le (3) : ", NORMAL));
        p.add(new Chunk(data.dateEmission() != null ? data.dateEmission().format(DATE_HEURE) : "", BOLD));
        return p;
    }

    // -- Coordonnées bancaires (gauche) — Responsable commercial (droite) --

    private static PdfPTable coordonneesTable(AutorisationVersementData data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        setWidths(table, 50, 50);

        Paragraph gauche = new Paragraph();
        gauche.add(new Chunk("COORDONNEES BANCAIRES :\n", BOLD));
        ligne(gauche, "RIB : ", vide(data.rib()));
        gauche.add(new Chunk(vide(data.banqueNom()), BOLD));
        table.addCell(sansBordure(gauche));

        Paragraph droite = new Paragraph();
        droite.add(new Chunk("Responsable Commercial : ", NORMAL));
        droite.add(new Chunk(responsable(data) + "\n", BOLD));
        droite.add(new Chunk("(Signature et cachet de la sté ", NORMAL));
        droite.add(new Chunk(vide(data.entrepriseNom()), BOLD));
        droite.add(new Chunk(") (4)", NORMAL));
        table.addCell(sansBordure(droite));

        return table;
    }

    private static String responsable(AutorisationVersementData data) {
        return (vide(data.responsableCommercialTitre()) + " " + vide(data.responsableCommercialNom())).trim();
    }

    // -- Conditions générales --

    private static Paragraph conditionsGeneralesParagraph(AutorisationVersementData data) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        String operation = vide(data.operationLibelle());
        String entreprise = vide(data.entrepriseNom());
        String delai = delai(data);
        p.add(new Chunk("CONDITIONS GENERALES :\n", BOLD));
        p.add(new Chunk("- Cette autorisation est à valoir sur l'achat d'un bien immobilier dans le cadre de "
                + "l'opération " + operation + ".\n", NORMAL));
        p.add(new Chunk("- Cette copie d'autorisation de versement doit être remplie par la banque et retournée "
                + "à la société " + entreprise + " dans le délai de " + delai + "h ouvrables après son émission.\n", NORMAL));
        p.add(new Chunk("- Le non respect par l'attributaire du délai de retour de l'autorisation de versement à "
                + "la société " + entreprise + " et des conditions entraîne la déchéance automatique de "
                + "l'attribution.\n", NORMAL));
        p.add(new Chunk("- La réservation n'est considérée valable et définitive qu'après réception du reçu de "
                + "versement délivré par la banque au client et en cas de paiement par chèque, qu'après qu'il "
                + "soit crédité sur le compte bancaire de la société " + entreprise + ".\n", NORMAL));
        p.add(new Chunk("- Tout litige pouvant naître entre les parties, sera réglé à l'amiable. En cas de "
                + "persistance du litige, compétence est donnée aux tribunaux de Casablanca.", NORMAL));
        return p;
    }

    // -- Cadre réservé à la banque : une seule PdfPCell bordée ; des lignes de table dessineraient
    // chacune leur bordure, or Report.pdf n'a aucune ligne interne.

    private static PdfPTable cadreBanque(AutorisationVersementData data) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(8);

        Paragraph titre = new Paragraph(
                "Cadre réservé à la banque (à retourner à la société " + vide(data.entrepriseNom()) + ")", SECTION);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(6);
        cell.addElement(titre);

        cell.addElement(cadreLigne("Versement effectué par : " + payeurTexte(data.payeur())));
        cell.addElement(cadreLigne("Nom et Prénom du client : " + clientsTexteSimple(data.clients())));
        cell.addElement(cadreLigne("Programme : " + vide(data.operationLibelle()) + "     N° du bien : " + vide(data.articleCode())));
        cell.addElement(cadreLigne("Prix total (T.T.C) : " + montant(data.prixVenteTTC())));
        cell.addElement(cadreLigne("Montant du versement : " + montant(data.montantAVerser())
                + "     - Versement N° : …………………………………………."));
        cell.addElement(cadreLigne("A la date du : …………………………………………. (date de versement)"));

        Paragraph mp = new Paragraph();
        mp.setSpacingBefore(3);
        mp.add(new Chunk("Mode de paiement :\n", NORMAL));
        mp.add(new Chunk("- Chèque certifié n° : ………………………………….     - Banque : ……………………….     - Réglé :  OUI     NON\n", NORMAL));
        mp.add(new Chunk("- Espèce : ………………………………….\n", NORMAL));
        mp.add(new Chunk("- Bordereau N° : ………………………….\n", NORMAL));
        mp.add(new Chunk("- Remise N° : ……………………………….", NORMAL));
        cell.addElement(mp);

        cell.addElement(signaturesTable());

        outer.addCell(cell);
        return outer;
    }

    private static String payeurTexte(Payeur payeur) {
        if (payeur == null) {
            return "";
        }
        String piece = payeur.pieceIdentite() != null ? payeur.pieceIdentite() : "";
        return "Nom : " + vide(payeur.nom()) + "   - Prénom : " + vide(payeur.prenom())
                + (piece.isBlank() ? "" : "   - N° " + piece);
    }

    private static String clientsTexteSimple(List<ClientAutorisation> clients) {
        StringBuilder sb = new StringBuilder();
        for (ClientAutorisation c : clients) {
            if (!sb.isEmpty()) {
                sb.append(" et ");
            }
            appendClient(sb, c);
        }
        return sb.toString();
    }

    private static Chunk clientsChunk(List<ClientAutorisation> clients) {
        StringBuilder sb = new StringBuilder();
        for (ClientAutorisation c : clients) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            appendClient(sb, c);
        }
        return new Chunk(sb.toString(), BOLD);
    }

    private static void appendClient(StringBuilder sb, ClientAutorisation c) {
        sb.append(vide(c.titre())).append(' ').append(vide(c.nom()));
        if (c.pieceIdentite() != null && !c.pieceIdentite().isBlank()) {
            sb.append(" N° ").append(c.pieceIdentite());
        }
    }

    private static Paragraph cadreLigne(String texte) {
        Paragraph p = new Paragraph(texte, NORMAL);
        p.setSpacingBefore(2);
        return p;
    }

    /** Sans bordure : la seule du document est la cellule englobante de {@link #cadreBanque}. */
    private static PdfPTable signaturesTable() {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        setWidths(table, 34, 33, 33);
        table.addCell(signatureCell("Signature Responsable Commercial :"));
        table.addCell(signatureCell("Signature et Cachet de la Banque :"));
        table.addCell(signatureCell("Signature Client : Lu et Approuvé"));
        return table;
    }

    private static PdfPCell signatureCell(String texte) {
        PdfPCell cell = new PdfPCell(new Paragraph(texte, NORMAL));
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setMinimumHeight(30);
        return cell;
    }

    // -- Utilitaires --

    private static void ligne(Paragraph p, String label, String valeur) {
        p.add(new Chunk(label, NORMAL));
        p.add(new Chunk(valeur + "\n", BOLD));
    }

    private static PdfPCell sansBordure(Paragraph contenu) {
        PdfPCell cell = new PdfPCell(contenu);
        cell.setBorder(0);
        cell.setPadding(3);
        return cell;
    }

    private static void setWidths(PdfPTable table, float... widths) {
        try {
            table.setWidths(widths);
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
    }
}
