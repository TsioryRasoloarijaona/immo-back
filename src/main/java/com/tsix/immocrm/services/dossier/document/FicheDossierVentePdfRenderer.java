package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheDossierVenteData;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.Acquereur;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.DetailReglement;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.LigneEcheance;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.MontantsFiche;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData.SousArticleDossier;
import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.utils.Champs;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.pourcentageRatio;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * « Fiche Dossier de Vente » d'après {@code ComSaleFiche.yes} : mêmes sections, grilles et sauts de
 * page. Champs legacy volontairement non repris : voir {@code FicheDossierVenteData}.
 */
@Component
public class FicheDossierVentePdfRenderer {

    private static final String CATEGORIE = "Catégorie";

    private static final Color BORDER = Color.BLACK;
    private static final Color HEADER_BG = new Color(0xE7, 0xE7, 0xE7);
    private static final Color SETTLED_GREEN = new Color(0x1E, 0x7A, 0x1E);
    private static final Color REST_RED = new Color(0xC0, 0x00, 0x00);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font VALUE = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    private static final Font TABLE_CELL_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.DARK_GRAY);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);

    public byte[] render(FicheDossierVenteData data) {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(
                    "Fiche dossier de vente N° " + vide(data.dossierCode()) + " du " + date(toLocalDate(data.dateDocument())),
                    TITLE);
            title.setSpacingAfter(10);
            document.add(title);

            document.add(box("ENTETE", entradeTable(data)));
            document.add(spacer());
            document.add(box("MONTANTS", montantsTable(data.montants())));
            document.add(spacer());
            document.add(box("DATE", datesTable(data)));

            if (data.dateAnnulation() != null || (data.motifAnnulation() != null && !data.motifAnnulation().isBlank())) {
                document.add(spacer());
                document.add(box("ANNULATION / RESILIATION", annulationTable(data)));
            }

            document.newPage();
            document.add(box("AUTRES", autresTable(data)));

            document.newPage();
            if (!data.echeancier().isEmpty()) {
                document.add(box("SCHEMA DE FINANCEMENT", echeancierTable(data.echeancier())));
                document.add(spacer());
            }
            if (!data.sousArticles().isEmpty()) {
                document.add(box("SOUS ARTICLES", sousArticlesTable(data.sousArticles())));
                document.add(spacer());
            }
            document.add(box("ACQUEREURS", acquereursTable(data.acquereurs())));
            document.add(spacer());
            document.add(box("HISTORIQUE", historiqueTable(data.historique())));
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de la fiche dossier de vente", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    // -- Boîte encadrée "TITRE" + contenu, comme dans le template legacy --

    private static PdfPTable box(String titre, PdfPTable contenu) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        PdfPCell header = new PdfPCell(new Phrase(titre, SECTION));
        header.setBackgroundColor(HEADER_BG);
        header.setBorderColor(BORDER);
        header.setPadding(4);
        outer.addCell(header);
        PdfPCell content = new PdfPCell(contenu);
        content.setBorderColor(BORDER);
        content.setPadding(0);
        outer.addCell(content);
        return outer;
    }

    private static Paragraph spacer() {
        Paragraph p = new Paragraph(" ", SMALL);
        p.setSpacingAfter(2);
        return p;
    }

    private static LocalDate toLocalDate(LocalDateTime dt) {
        return Champs.de(dt, LocalDateTime::toLocalDate);
    }

    private static String d(LocalDateTime dt) {
        return dt != null ? date(dt.toLocalDate()) : "";
    }

    // -- ENTETE : grille label/valeur, 2 paires par ligne --

    private static PdfPTable entradeTable(FicheDossierVenteData data) {
        PdfPTable table = grid4();
        row(table, "N° dossier", vide(data.dossierCode()), "Date", d(data.dateDocument()));
        row(table, "Statut", vide(data.statut()), "Client", vide(data.clientNom()));
        row(table, CATEGORIE,vide(data.categorieLibelle()), "Type", vide(data.typeLibelle()));
        row(table, "Article", vide(data.articleCode()), "Supperficie (m²)", surface(data.superficie()));
        row(table, "Commercial", vide(data.commercialNom()), "Prospection", vide(data.prospectionCode()));
        row(table, "Entité", vide(data.entrepriseNom()), "Programme", vide(data.projetNom()));
        row(table, "Tranche", vide(data.trancheDescription()), "Titre foncier", vide(data.titreFoncier()));
        row(table, "Prix du m²", montantOrBlank(data.prixDuM2()), "Prix article", montantOrBlank(data.prixArticle()));
        row(table, "S. articles", montantOrBlank(data.sousArticlesPrix()), "Total", montantOrBlank(data.totalAvantRemise()));
        row(table, "Remise", pourcentageRatio(data.remiseRatio()), "Remise (montant)", montantOrBlank(data.remiseMontant()));
        row(table, "Prix de vente", montantOrBlank(data.prixDeVente()), "Prix contractuel", montantOrBlank(data.prixContractuel()));
        row(table, "Validité", d(data.validite()), "Part", data.partPercent() != null ? pourcentageRatio(data.partPercent()) : "");
        return table;
    }

    private static String surface(BigDecimal area) {
        return area != null ? area.toPlainString() : "";
    }

    private static String montantOrBlank(BigDecimal value) {
        return value != null ? montant(value) : "";
    }

    private static PdfPTable grid4() {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{16, 34, 16, 34});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        return table;
    }

    private static void row(PdfPTable table, String label1, String value1, String label2, String value2) {
        table.addCell(labelCell(label1));
        table.addCell(valueCell(value1));
        table.addCell(labelCell(label2));
        table.addCell(valueCell(value2));
    }

    // -- MONTANTS : 9 lignes fixes, colonnes Montant/Réglé/Reste --

    private static PdfPTable montantsTable(MontantsFiche m) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{34, 22, 22, 22});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        table.addCell(headerCell(""));
        table.addCell(headerCell("Montant"));
        table.addCell(headerCell("Réglé"));
        table.addCell(headerCell("Reste"));

        montantRow(table, "Frais", m.montantFrais(), m.regleFrais(), m.resteFrais());
        montantRow(table, "Modification", m.montantModification(), m.regleModification(), m.resteModification());
        montantRow(table, "Prix de vente", m.montantPrixVente(), m.reglePrixVente(), m.restePrixVente());
        montantRowAvecRatio(table, "Avance", m.ratioAvance(), m.montantAvance(), m.regleAvance(), m.resteAvance());
        montantRow(table, "Crédit", m.montantCredit(), m.regleCredit(), m.resteCredit());
        montantRow(table, "Auto-financement", m.montantAutoFinancement(), m.regleAutoFinancement(), m.resteAutoFinancement());
        montantRow(table, "Pénalité", m.montantPenalite(), m.reglePenalite(), m.restePenalite());
        montantRow(table, "Résiliation", m.montantResiliation(), m.regleResiliation(), m.resteResiliation());
        montantRow(table, "Net à payer", m.montantNetAPayer(), m.regleNetAPayer(), m.resteNetAPayer(), true);
        return table;
    }

    private static void montantRow(PdfPTable table, String libelle, BigDecimal montantVal, BigDecimal regle, BigDecimal reste) {
        montantRow(table, libelle, montantVal, regle, reste, false);
    }

    private static void montantRow(PdfPTable table, String libelle, BigDecimal montantVal, BigDecimal regle, BigDecimal reste, boolean total) {
        Font font = total ? LABEL : VALUE;
        table.addCell(labelPlainCell(libelle, total));
        table.addCell(amountCell(montant(montantVal), font));
        table.addCell(amountCell(montant(regle), font));
        table.addCell(amountCell(montant(reste), font));
    }

    private static void montantRowAvecRatio(PdfPTable table, String libelle, BigDecimal ratio, BigDecimal montantVal,
                                             BigDecimal regle, BigDecimal reste) {
        PdfPCell label = new PdfPCell();
        label.setPadding(3);
        Paragraph p = new Paragraph();
        p.add(new Phrase(libelle + "  ", LABEL));
        p.add(new Phrase(pourcentageRatio(ratio), VALUE));
        label.addElement(p);
        table.addCell(label);
        table.addCell(amountCell(montant(montantVal), VALUE));
        table.addCell(amountCell(montant(regle), VALUE));
        table.addCell(amountCell(montant(reste), VALUE));
    }

    // -- DATE : grille label/valeur, 2 paires par ligne --

    private static PdfPTable datesTable(FicheDossierVenteData data) {
        PdfPTable table = grid4();
        row(table, "Promesse de vente", d(data.dateReservation()), "Fiche bancaire", d(data.dateFicheBancaire()));
        row(table, "Concrétisation", d(data.dateCompromis()), "Compromis/Approbation crédit", d(data.dateApprobationCredit()));
        row(table, "Visite technique / Réception", d(data.dateVisiteTechnique()), "Attes. de consignation / Octroi crédit", d(data.dateOctroiCredit()));
        row(table, "Date enregistrement contrat", d(data.dateEnregistrementContrat()), "Intervention", d(data.dateIntervention()));
        row(table, "Expedition / Certificat de propriété", d(data.dateCertificatPropriete()), "Assurance", d(data.dateAssurance()));
        row(table, "Remise des clés prévue", d(data.dateLivraisonPrevue()), "Effective", d(data.dateLivraisonEffective()));
        return table;
    }

    private static PdfPTable annulationTable(FicheDossierVenteData data) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Paragraph(
                "Date : " + d(data.dateAnnulation()) + "\nCauses : " + vide(data.motifAnnulation()), VALUE));
        cell.setPadding(4);
        cell.setBorder(0);
        table.addCell(cell);
        return table;
    }

    // -- AUTRES (page 2) --

    private static PdfPTable autresTable(FicheDossierVenteData data) {
        PdfPTable table = grid4();
        row(table, "TVA", data.tvaRatio() != null ? pourcentageRatio(data.tvaRatio()) : "",
                "Avec crédit", checkbox(data.avecCredit()));
        row(table, "Notaire/Avocat", vide(data.notaireAvocatNom()), "Banque/Leasing", vide(data.banqueLeasingNom()));
        row(table, "Agence", vide(data.agence()), "C'est un ancien client", checkbox(data.ancienClient()));
        row(table, "Client veut choisir le cadeau plus tard", checkbox(data.cadeauChoisirPlusTard()),
                "Intégrer les modifications dans le prix de vente", checkbox(data.integrerModifications()));
        row(table, "Date d'envoi MEILLEURTAUX", d(data.dateEnvoiMeilleurtaux()),
                "Date décision MEILLEURTAUX", d(data.dateDecisionMeilleurtaux()));

        PdfPCell notesLabel = labelCell("Notes");
        table.addCell(notesLabel);
        PdfPCell notesValue = valueCell(vide(data.notes()));
        notesValue.setColspan(3);
        table.addCell(notesValue);
        return table;
    }

    private static String checkbox(Boolean value) {
        return Boolean.TRUE.equals(value) ? "☑" : "☐";
    }

    // -- SCHEMA DE FINANCEMENT : échéancier + sous-lignes de règlement --

    private static PdfPTable echeancierTable(List<LigneEcheance> lignes) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{12, 24, 16, 16, 32});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        table.addCell(headerCell("Date"));
        table.addCell(headerCell(CATEGORIE));
        table.addCell(headerCell("Montant"));
        table.addCell(headerCell("Réglé"));
        table.addCell(headerCell("Reste / Notes"));
        for (LigneEcheance ligne : lignes) {
            table.addCell(tableCell(d(ligne.date())));
            table.addCell(tableCell(vide(ligne.categorie())));
            table.addCell(amountCell(montant(ligne.montant()), TABLE_CELL));
            table.addCell(amountCell(montant(ligne.regle()), TABLE_CELL));
            table.addCell(amountCell(montant(ligne.reste()) + (vide(ligne.notes()).isBlank() ? "" : " — " + ligne.notes()), TABLE_CELL));
            for (DetailReglement dr : ligne.reglements()) {
                PdfPCell libelle = new PdfPCell(new Paragraph(dr.libelle(), TABLE_CELL_ITALIC));
                libelle.setColspan(4);
                libelle.setPadding(3);
                table.addCell(libelle);
                table.addCell(amountCell(montant(dr.montant()), TABLE_CELL_ITALIC));
            }
        }
        return table;
    }

    // -- SOUS ARTICLES --

    private static PdfPTable sousArticlesTable(List<SousArticleDossier> sousArticles) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{20, 22, 14, 16, 12, 16});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        table.addCell(headerCell("Code"));
        table.addCell(headerCell(CATEGORIE));
        table.addCell(headerCell("Superficie (m²)"));
        table.addCell(headerCell("Prix"));
        table.addCell(headerCell("Remise"));
        table.addCell(headerCell("Tjs affecté"));
        for (SousArticleDossier sa : sousArticles) {
            table.addCell(tableCell(vide(sa.code())));
            table.addCell(tableCell(vide(sa.categorieLibelle())));
            table.addCell(amountCell(surface(sa.superficie()), TABLE_CELL));
            table.addCell(amountCell(montantOrBlank(sa.prix()), TABLE_CELL));
            table.addCell(amountCell(sa.remiseRatio() != null ? pourcentageRatio(sa.remiseRatio()) : "", TABLE_CELL));
            table.addCell(centeredCell(checkbox(sa.toujoursAffecte())));
        }
        return table;
    }

    // -- ACQUEREURS --

    private static PdfPTable acquereursTable(List<Acquereur> acquereurs) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{70, 30});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        table.addCell(headerCell("Nom"));
        table.addCell(headerCell("Part"));
        for (Acquereur a : acquereurs) {
            table.addCell(tableCell(vide(a.nom())));
            table.addCell(amountCell(a.partPercent() != null ? pourcentageRatio(a.partPercent()) : "", TABLE_CELL));
        }
        return table;
    }

    // -- HISTORIQUE --

    private static PdfPTable historiqueTable(List<HistoriqueEvenementDto> historique) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18, 15, 22, 45});
        } catch (Exception ignored) {
            // widths array length always matches column count here
        }
        table.addCell(headerCell("Date"));
        table.addCell(headerCell("Type"));
        table.addCell(headerCell("Utilisateur"));
        table.addCell(headerCell("Description"));
        for (HistoriqueEvenementDto h : historique) {
            table.addCell(tableCell(h.date() != null ? h.date().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
            table.addCell(tableCell(vide(h.type())));
            table.addCell(tableCell(vide(h.utilisateur())));
            table.addCell(tableCell(vide(h.description())));
        }
        return table;
    }

    // -- Cellules --

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_HEADER));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, LABEL));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(3);
        return cell;
    }

    private static PdfPCell labelPlainCell(String text, boolean bold) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, bold ? LABEL : VALUE));
        cell.setPadding(3);
        return cell;
    }

    private static PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, VALUE));
        cell.setPadding(3);
        return cell;
    }

    private static PdfPCell tableCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, TABLE_CELL));
        cell.setPadding(3);
        return cell;
    }

    private static PdfPCell centeredCell(String text) {
        PdfPCell cell = tableCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell amountCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
