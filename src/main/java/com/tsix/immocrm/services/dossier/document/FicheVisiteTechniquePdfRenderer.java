package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheVisiteTechniqueData;
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

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/** « Fiche Visite Technique » en PDF ; partie française seulement du template legacy bilingue français/arabe. */
@Component
public class FicheVisiteTechniquePdfRenderer {

    private static final Color GRAY_HEADER = new Color(0xD9, 0xD9, 0xD9);
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final DateTimeFormatter DATE_SEULE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);

    private static final String[] POINTS_REMISE_CLES = {
            "Fissures", "Décollement Peinture", "Remontée capillaire", "Carreaux creux/ébréchés",
            "Volet roulant", "Fermeture châssis", "Réglage portes", "Robinetterie rouillée",
            "Serrage interrupteur", "Tableau de Protection (Portillon)"
    };

    private static final String[] POINTS_POSE_COMPTEURS = {
            "Absence de courant", "Absence de l'eau potable", "Fuites Sanitaires",
            "Interrupteur hors service", "Tableau de protection (Equipement)", "Signal Parabole"
    };

    public byte[] render(FicheVisiteTechniqueData data) {
        Document document = new Document(PageSize.A4, 40, 40, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("FICHE D'INTERVENTION AVANT LIVRAISON", TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(spacer());

            document.add(headerParagraph(data));
            document.add(spacer());

            document.add(sectionLabel("A LA REMISE DES CLES"));
            document.add(checklistTable(POINTS_REMISE_CLES));
            document.add(spacer());

            document.add(sectionLabel("A LA POSE DES COMPTEURS"));
            document.add(checklistTable(POINTS_POSE_COMPTEURS));
            document.add(spacer());

            document.add(sectionLabel("A LA LEVEE DES RESERVES"));
            document.add(new Paragraph("Le ……………………………", NORMAL));
            document.add(spacer());
            document.add(spacer());

            document.add(signatureTable());
            document.newPage();

            document.add(new Paragraph("Synthèse des Obligations et Interdictions", SECTION));
            document.add(new Paragraph("Prévues par le règlement de copropriété", NORMAL));
            document.add(spacer());
            document.add(obligationsParagraph());
            document.add(spacer());

            document.add(new Paragraph("Service Après-vente :", SECTION));
            document.add(savParagraph(data));
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche de visite technique", e);
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

    private static Paragraph headerParagraph(FicheVisiteTechniqueData data) {
        Paragraph p = new Paragraph();
        p.add(new Chunk("Date RDV : ", BOLD));
        p.add(new Chunk(data.dateRdv() != null ? data.dateRdv().format(DATE_HEURE) : "", NORMAL));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("CLIENT : ", BOLD));
        p.add(new Chunk(String.join(", ", data.clientNoms()), NORMAL));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("Appartement : ", BOLD));
        p.add(new Chunk(vide(data.articleCode()), NORMAL));
        return p;
    }

    private static PdfPTable checklistTable(String[] points) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{34, 22, 22, 22});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }

        table.addCell(headerCell(""));
        table.addCell(headerCell("Localisation"));
        table.addCell(headerCell("Date Interv."));
        table.addCell(headerCell("Observations"));

        for (String point : points) {
            table.addCell(labelCell(point));
            table.addCell(blankCell());
            table.addCell(blankCell());
            table.addCell(blankCell());
        }
        return table;
    }

    private static PdfPTable signatureTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        PdfPCell acquereur = new PdfPCell(new Paragraph("VISA ACQUEREUR", SECTION));
        acquereur.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
        PdfPCell promoteur = new PdfPCell(new Paragraph("VISA PROMOTEUR", SECTION));
        promoteur.setBorder(org.openpdf.text.Rectangle.NO_BORDER);
        table.addCell(acquereur);
        table.addCell(promoteur);
        return table;
    }

    private static Paragraph obligationsParagraph() {
        Paragraph p = new Paragraph(
                """
                Chaque propriétaire dispose des parties privatives comprises dans son lot. Il use et jouit \
                librement des parties privatives et des parties communes sous la condition de ne pas \
                porter atteinte ni aux droits des autres copropriétaires ni à la destination de \
                l'immeuble. Chaque occupant se doit de :
                - S'abstenir de tous actes générateurs de troubles de voisinage : (bruits excessifs, \
                mauvaises odeurs, activités contraires à la décence, ne pas détenir des animaux \
                criards, etc.)
                - S'abstenir d'annexer, d'empiéter, de s'approprier, d'encombrer des parties communes \
                (escaliers, terrasses, passages, allées, halls...) par : des chaises, tables, \
                barbecues, échelles, vélos, motos, déblais, etc.
                - S'abstenir d'étendre le linge aux fenêtres des façades
                - S'abstenir de laver le linge à la terrasse
                - S'interdire d'installer toute antenne individuelle de télévision, de radio (sur \
                balcons et/ou sur façade, etc.)
                - S'interdire d'entreprendre des travaux sur les parties communes de l'immeuble, \
                notamment : ne pas toucher à la solidité de l'immeuble, ne pas effectuer de travaux \
                sur la façade, ne pas effectuer de percements de gros murs, ne pas effectuer de \
                travaux affectant l'harmonie de l'immeuble
                - Pour tous les travaux réalisés à l'intérieur de l'appartement par l'acquéreur, le \
                Promoteur dégage toute responsabilité sur les incidences techniques ou dégâts qui \
                pourraient en découler sur l'appartement de ce dernier ou sur celui d'un tiers.
                - S'interdire de modifier les parties privées visibles de l'extérieur (fenêtres, \
                portes, volets, balcons, etc.)
                - Souscrire une police d'assurance personnelle du lot qu'il occupe.
                - Laisser l'accès au syndic dans son appartement en cas de besoin pour des \
                réparations de toute nature qui le concernent ou qui concernent les appartements \
                voisins ou qui concernent un besoin particulier de curage ou de réfection du réseau \
                d'assainissement.
                - Soumettre au Syndic pour validation la liste des travaux qu'il devra réaliser dans \
                son appartement pour s'assurer qu'ils ne portent pas préjudice à l'immeuble.
                - Respecter le règlement et les horaires du Rooftop, salle de sport, laverie.""",
                NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static Paragraph savParagraph(FicheVisiteTechniqueData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Avant remise des clés :\n")
                .append("1. Prise de rendez-vous de la visite technique et récupération de la fiche de visite "
                        + "technique mentionnant la date et l'heure du RDV ;\n")
                .append("2. Lors du rendez-vous, présenter la fiche de visite technique à l'agent technique, "
                        + "signaler toutes les réserves existantes le cas échéant (hors vices cachés) et signer "
                        + "la case « à la visite », une fois les réserves levées, signer la case « Levée des "
                        + "réserves »\n")
                .append("3. Légaliser la fiche de visite technique et la remettre au service après-vente.\n\n")
                .append("Après remise des clés :\n")
                .append("Toute réclamation (technique — dans le cadre de la garantie SAV — ou de service) doit "
                        + "être adressée au service après-vente.");
        if (data.bureauSav() != null) {
            sb.append(" Bureau SAV : ").append(data.bureauSav());
        }
        sb.append("\nLe délai de levée des réserves est de 10 jours maximum (ouvrables, hors période de fêtes ou "
                + "de besoin d'importation)");
        if (data.contactSav() != null) {
            sb.append(", si ce délai est dépassé, ").append(data.contactSav());
        }
        sb.append(".\n\n");
        if (data.dureeSavMois() != null) {
            sb.append("Durée de validité du Service Après-Vente : ").append(data.dureeSavMois())
                    .append(" mois à partir de la création des titres fonciers.");
        }

        Paragraph p = new Paragraph(sb.toString(), NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER));
        cell.setBackgroundColor(GRAY_HEADER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_CELL));
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell blankCell() {
        PdfPCell cell = new PdfPCell(new Paragraph(" ", TABLE_CELL));
        cell.setPadding(4);
        cell.setMinimumHeight(16);
        return cell;
    }
}
