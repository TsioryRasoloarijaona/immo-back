package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.exceptions.ConflictException;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Fiche de réservation" as a Word (.docx) document — same content/layout as
 * {@link FicheReservationPdfRenderer}, built with Apache POI (XWPF) so it stays a pure-Java,
 * no-external-process dependency.
 */
@Component
public class FicheReservationDocxRenderer {

    /** Couleurs relevees sur les fonds de page legataires, pas approximees. */
    private static final String APF_ORANGE = "F37123";
    private static final String ARS_VIOLET = "726789";
    private static final String GRAY_LABEL = "E4E4E4";

    public byte[] render(FicheReservationData data) {
        String marque = marque(data.dossierCode());
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addTitleBox(document, data.dossierCode(), couleur(marque));
            blank(document);
            addIntro(document, data, marque);
            blank(document);
            addFieldsTable(document, data);
            blank(document);
            addDescription(document, data);
            blank(document);
            addFixedWarning(document, marque);
            blank(document);
            addHeading(document, "CONDITION DE RESERVATION");
            blank(document);
            addConditions(document);
            blank(document);
            blank(document);

            XWPFParagraph fait = document.createParagraph();
            XWPFRun faitRun = fait.createRun();
            faitRun.setText("Fait le ");
            XWPFRun faitDate = fait.createRun();
            faitDate.setText(date(data.dateEdition()));
            faitDate.setBold(true);

            blank(document);
            blank(document);
            addSignatureTable(document);
            blank(document);
            blank(document);
            addFooterBar(document, couleur(marque));

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DocumentRenderingException("Erreur lors de la génération du Word de la fiche de réservation", e);
        }
    }

    private static void blank(XWPFDocument document) {
        document.createParagraph();
    }

    /**
     * Marque deduite du prefixe du code dossier, comme dans le rapport legataire dont les pages
     * s'appellent APF / ARS / ART / MBR. Elle decide de la couleur, de la societe citee et des
     * clauses imprimees. Un prefixe inconnu est refuse plutot que rendu sous une autre marque :
     * une fiche de reservation est remise au client.
     */
    static String marque(String dossierCode) {
        String prefixe = dossierCode == null || dossierCode.length() < 3
                ? "" : dossierCode.substring(0, 3).toUpperCase();
        if (!"APF".equals(prefixe) && !"ARS".equals(prefixe)) {
            throw new ConflictException(
                    "Fiche de reservation non disponible pour la marque « " + prefixe + " » : "
                            + "seules APF et ARS sont couvertes.");
        }
        return prefixe;
    }

    private static String couleur(String marque) {
        return "ARS".equals(marque) ? ARS_VIOLET : APF_ORANGE;
    }

    private static void addTitleBox(XWPFDocument document, String dossierCode, String couleur) {
        XWPFTable table = document.createTable(1, 1);
        table.setWidth("35%");
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun label = p.createRun();
        label.setText("N°V: ");
        label.setBold(true);
        label.setColor(couleur);
        XWPFRun value = p.createRun();
        value.setText(vide(dossierCode));
        value.setBold(true);
    }

    /**
     * Le paragraphe de societe differe par marque : ce n'est pas une mise en forme mais le texte
     * legal de l'entite qui vend. L'ancienne version composait « SOCIETE {nom} (groupe AYAD
     * DEVELOPPEMENT) » quelle que soit l'entite, ce qui produisait une mention fausse pour GHRIZA.
     */
    private static void addIntro(XWPFDocument document, FicheReservationData data, String marque) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun r = p.createRun();
        r.setText("ARS".equals(marque)
                ? "Nous soussignés, la Société dite « " + vide(data.entrepriseNom()) + " », société à "
                        + "responsabilité limitée au capital de 100 000,00 Dhs, sise à Casablanca, 20 rue Racine, "
                        + "Val Fleuri, représentées par l'un des mandataires dûment habilité à cet effet."
                : "Nous soussignés, la Société dite « SOCIETE " + vide(data.entrepriseNom()) + " (groupe AYAD "
                        + "DEVELOPPEMENT) », société à Responsabilité limitée, sise à Casablanca, 100 Boulvard "
                        + "Zerktouni, représentées par l'un des mandataires dûment habilité à cet effet.");
        r.addBreak();
        r.setText("Agissant en notre qualité de promoteur immobilier, reconnaissons sous réserve des conditions "
                + "ci-dessous avoir reçu de :");
    }

    private static void addFieldsTable(XWPFDocument document, FicheReservationData data) {
        XWPFTable table = document.createTable(7, 2);
        table.setWidth("100%");

        addRow(table, 0, "M", vide(data.clientNom()), false);
        addRow(table, 1, "Demeurant à", vide(data.clientAdresse()), false);
        addRow(table, 2, "Passeport ou CIN N°", vide(data.clientCin()), false);
        addRow(table, 3, "Mode de paiement", vide(data.modePaiement()), false);
        addRow(table, 4, "Montant de", montant(data.montant()), true);
        addRow(table, 5, "Réglé par", vide(data.reglePar()), false);
        addRow(table, 6, "Prix article", montant(data.prixArticle()), true);
    }

    private static void addRow(XWPFTable table, int rowIndex, String label, String value, boolean valueBold) {
        XWPFTableRow row = table.getRow(rowIndex);

        XWPFTableCell labelCell = row.getCell(0);
        shade(labelCell, GRAY_LABEL);
        labelCell.removeParagraph(0);
        XWPFParagraph labelParagraph = labelCell.addParagraph();
        labelParagraph.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun labelRun = labelParagraph.createRun();
        labelRun.setText(label);
        labelRun.setBold(true);

        XWPFTableCell valueCell = row.getCell(1);
        valueCell.removeParagraph(0);
        XWPFParagraph valueParagraph = valueCell.addParagraph();
        valueParagraph.setAlignment(valueBold ? ParagraphAlignment.RIGHT : ParagraphAlignment.LEFT);
        XWPFRun valueRun = valueParagraph.createRun();
        valueRun.setText(value);
        valueRun.setBold(valueBold);
    }

    private static void shade(XWPFTableCell cell, String hexColor) {
        CTShd shd = cell.getCTTc().addNewTcPr().addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hexColor);
    }

    private static void addDescription(XWPFDocument document, FicheReservationData data) {
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

        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.createRun().setText(sb.toString());
    }

    /**
     * La clause d'immobilisation de 10 % n'existe que sur APF, et la mention parking que sur ARS :
     * les deux sont imprimees dans le fond de page de leur marque respective. Les appliquer aux
     * deux, comme le faisait l'ancienne version, revenait a engager GHRIZA sur une penalite que
     * son document ne prevoit pas.
     */
    private static void addFixedWarning(XWPFDocument document, String marque) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun r = p.createRun();
        r.setText("A défaut de l'encaissement effectif du montant ci-dessus, la réservation est annulée sans "
                + "formalités.");
        if ("APF".equals(marque)) {
            r.addBreak();
            r.setText("Des frais d'immobilisation seront déduits de l'avance à hauteur de 10% de la valeur totale du "
                    + "bien en cas de non aboutissement de la transaction.");
        }
        if ("ARS".equals(marque)) {
            XWPFParagraph mention = document.createParagraph();
            XWPFRun m = mention.createRun();
            m.setText(GabaritFicheReservation.MENTION_ARS);
            m.setBold(true);
            m.setFontSize(14);
        }
    }

    private static void addHeading(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
    }

    private static void addConditions(XWPFDocument document) {
        XWPFParagraph p1 = document.createParagraph();
        p1.setAlignment(ParagraphAlignment.BOTH);
        p1.createRun().setText("- Le client s'engage à signer le contrat de réservation dans un délai maximum de "
                + "15 JOURS à compter de la date de la signature du présent reçu de réservation, et sous condition "
                + "de l'encaissement effectif du montant de réservation.");

        blank(document);

        XWPFParagraph p2 = document.createParagraph();
        p2.setAlignment(ParagraphAlignment.BOTH);
        p2.createRun().setText("- Ce document ne constitue en aucun cas un acte de vente.");
    }

    private static void addSignatureTable(XWPFDocument document) {
        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");
        removeBorders(table);

        XWPFTableCell promoteur = table.getRow(0).getCell(0);
        promoteur.removeParagraph(0);
        XWPFRun promoteurRun = promoteur.addParagraph().createRun();
        promoteurRun.setText("LE PROMOTEUR");
        promoteurRun.setBold(true);

        XWPFTableCell client = table.getRow(0).getCell(1);
        client.removeParagraph(0);
        XWPFRun clientRun = client.addParagraph().createRun();
        clientRun.setText("LE CLIENT");
        clientRun.setBold(true);
    }

    private static void removeBorders(XWPFTable table) {
        table.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setTopBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setBottomBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setLeftBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setRightBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
    }

    private static void addFooterBar(XWPFDocument document, String couleur) {
        XWPFTable table = document.createTable(1, 1);
        table.setWidth("100%");
        XWPFTableCell cell = table.getRow(0).getCell(0);
        shade(cell, couleur);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun r = p.createRun();
        r.setText("AYAD DEVELOPPEMENT — 100 Boulvard Zerktouni, Casablanca, Maroc — "
                + "tel: +212 5 22 27 34 96 / +212 6 45 45 45 50 — contact@ayaddeveloppement.com");
        r.setColor("FFFFFF");
        r.setFontSize(8);
    }
}
