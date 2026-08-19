package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.pourcentage;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Acte de réservation" as a PDF, modeled on the legacy template (see
 * {@code instructions/templates/ACTE RESERVATION.pdf}). Uses OpenPDF ({@code org.openpdf.text}).
 */
@Component
public class ActeReservationPdfRenderer {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);

    public byte[] render(ActeReservationData data) {
        Document document = new Document(PageSize.A4, 45, 45, 35, 35);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("ACTE DE RÉSERVATION", TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(spacer());

            document.add(headerParagraph(data));
            document.add(spacer());

            document.add(section("1-"));
            document.add(bodyParagraph(
                    "Le réservataire s'engage par la présente à acquérir auprès de la société ADMOD le bien "
                            + "ci-après désigné, il s'engage ainsi à payer le prix selon les modalités ci-dessous.\n"
                            + "Le présent contrat de réservation ne peut en aucun cas valoir contrat de vente, ce "
                            + "dernier est à signer pardevant notaire."));
            document.add(spacer());

            document.add(section("2- DÉSIGNATION"));
            document.add(bodyParagraph(designationText(data)));
            document.add(spacer());

            document.add(section("3- PRIX DE VENTE"));
            document.add(bodyParagraph(
                    "La présente réservation est consentie et acceptée moyennant le prix de vente de "
                            + montant(data.prixVente()) + " TTC.\n"
                            + "Il est à préciser que la TVA applicable à la présente vente sera celle imposée par la "
                            + "loi au moment de la signature du contrat de vente, toute augmentation du taux de la "
                            + "dite TVA demeure supportée par le réservataire."));
            document.add(spacer());

            document.add(section("4- MODALITÉS DE PAIEMENT"));
            document.add(paiementParagraph(data));
            document.add(spacer());

            document.add(section("5- PROPRIÉTÉ ET JOUISSANCE"));
            document.add(bodyParagraph(
                    "La présente réservation ne donne aucun droit d'occupation ou de jouissance du bien objet de "
                            + "la dite réservation, le réservataire n'aura la pleine propriété et jouissance qu'après "
                            + "la signature du contrat définitif de vente et son inscription sur les livres fonciers "
                            + "tel que prévu par les articles 66 et 67 du Dahir du 12 Août 1913 sur l'immatriculation "
                            + "foncière tel que modifié et complété par la loi 14-07.\n"
                            + "Il est bien entendu que le contrat définitif de vente et le compromis de vente devront "
                            + "être signés par devant notaire, au plus tard dans un délai d'un mois suivant la "
                            + "légalisation du contrat de réservation par le réservataire. Tout retard dépassant un "
                            + "mois après notification donnera à la société ADMOD tout le droit d'annuler la présente "
                            + "réservation et de disposer du bien réservé sans avoir recours à la justice."));
            document.add(spacer());

            document.add(section("6- CONDITIONS SUSPENSIVES"));
            document.add(bodyParagraph(
                    "En cas de fausse déclaration ayant une incidence sur le bon aboutissement de la vente, de "
                            + "même qu'en cas de non-respect des conditions du présent acte et notamment des délais "
                            + "de paiement, ou en cas de non réalisation de l'acte définitif de vente du fait du "
                            + "réservataire et à défaut d'une régularisation de cette situation après quinze jours à "
                            + "compter de la notification d'une mise en demeure, la société ADMOD se réserve le droit "
                            + "de considérer la présente réservation comme résiliée et sans effet tel que prévu par "
                            + "l'article 260 du DOC. En conséquence, la société ADMOD retiendra sur le montant de "
                            + "l'acquisition une indemnité contractuelle de " + pourcentage(data.penaliteResiliationPct())
                            + " du prix de vente global TTC et restituera le solde au réservataire."));
            document.add(spacer());

            document.add(section("7- PENALITES"));
            document.add(bodyParagraph(
                    "En cas de désistement : Pénalité de " + pourcentage(data.penaliteResiliationPct()) + " du Prix de Vente.\n"
                            + "En cas de Transferts : Produit : " + montant(data.transfertProduitMontant())
                            + " - Tranche/Projet : " + montant(data.transfertTrancheMontant())
                            + " - Client* : " + pourcentage(data.transfertClientPct()) + " du prix de vente.\n"
                            + "(*) Le transfert client se fait gratuitement au profit d'un ascendant, d'un "
                            + "descendant, du conjoint ou d'un membre de la famille jusqu'au 3ème degré."));
            document.add(spacer());

            document.add(section("8- SERVICE APRES VENTE - ASSURANCE - SYNDIC"));
            document.add(bodyParagraph(savParagraph(data)));
            document.add(spacer());

            document.add(section("9- PROTECTION DES DONNEES PERSONNELLES"));
            document.add(bodyParagraph(
                    "ADMOD (Groupe AYAD Developpement) s'engage à ce que la collecte et le traitement "
                            + "d'informations personnelles soient effectués conformément à la loi n°09-08 du 18 "
                            + "février 2009 relative à la protection des personnes physiques à l'égard du traitement "
                            + "des données à caractère personnel. Aucune information personnelle n'est cédée à des "
                            + "tiers."));
            document.add(spacer());

            document.add(section("10- ELECTION DE DOMICILE"));
            document.add(bodyParagraph(
                    "Pour l'exécution des présentes et leurs suites, le réservataire fait élection de domicile en "
                            + "son lieu respectif sus indiqué : le réservataire doit sous peine d'inopposabilité, "
                            + "aviser la société ADMOD de tout changement d'adresse postale."));
            document.add(spacer());

            document.add(section("11- ATTRIBUTION DE COMPÉTENCE"));
            document.add(bodyParagraph(
                    "Toute contestation liée à l'interprétation ou à l'exécution des présentes doit être réglée à "
                            + "l'amiable, à défaut compétence est donnée aux Tribunaux de " + vide(data.tribunalCompetent()) + "."));
            document.add(spacer());
            document.add(spacer());

            Paragraph fait = new Paragraph("Fait à " + vide(data.tribunalCompetent()) + " le : " + date(data.dateEdition()), NORMAL);
            document.add(fait);
            document.add(spacer());
            document.add(spacer());

            Paragraph signatures = new Paragraph();
            signatures.add(new Chunk("Le réservataire", BOLD));
            signatures.add(Chunk.NEWLINE);
            signatures.add(new Chunk(vide(data.clientTitre()) + " " + vide(data.clientNom()), NORMAL));
            document.add(signatures);
            document.add(spacer());

            Paragraph signatureSociete = new Paragraph();
            signatureSociete.add(new Chunk("Pour la société ADMOD", BOLD));
            signatureSociete.add(Chunk.NEWLINE);
            signatureSociete.add(new Chunk(vide(data.signataireNom()), NORMAL));
            signatureSociete.add(Chunk.NEWLINE);
            signatureSociete.add(new Chunk("Directeur Général", NORMAL));
            document.add(signatureSociete);
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la génération du PDF de l'acte de réservation", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", NORMAL);
    }

    private static Paragraph section(String text) {
        return new Paragraph(text, SECTION);
    }

    private static Paragraph bodyParagraph(String text) {
        Paragraph p = new Paragraph(text, NORMAL);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static Paragraph headerParagraph(ActeReservationData data) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(vide(data.clientTitre()) + " : ", NORMAL));
        p.add(new Chunk(vide(data.clientNom()) + " ", BOLD));
        p.add(new Chunk("Titulaire de la Pièce d'identité (CIN) n° ", NORMAL));
        p.add(new Chunk(vide(data.clientCin()) + " ", BOLD));
        p.add(new Chunk("demeurant à : " + vide(data.clientAdresse()), NORMAL));
        if (data.clientTelephone() != null) {
            p.add(Chunk.NEWLINE);
            p.add(new Chunk("Tél : " + data.clientTelephone(), NORMAL));
        }
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("Ci-après, dénommé(e) «Réservataire»", NORMAL));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private static String designationText(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("La totalité de la propriété sise au projet ").append(vide(data.projetNom()));
        if (data.projetAdresse() != null) {
            sb.append(", ").append(data.projetAdresse());
        }
        sb.append(", consistant en un ").append(vide(data.articleCategorie()))
                .append(" portant le numéro ").append(vide(data.articleCode()))
                .append(", situé au ").append(vide(data.etage())).append(" de l'Immeuble ").append(vide(data.immeubleCode()))
                .append(", d'une superficie de ").append(surface(data.superficie())).append(" m²");
        for (SousArticle sous : data.sousArticles()) {
            sb.append(" et un ").append(vide(sous.typeLabel()));
            if (sous.superficie() != null) {
                sb.append(" d'une superficie de ").append(surface(sous.superficie())).append(" m²");
            }
        }
        sb.append(".\nLe réservataire déclare avoir pris parfaitement connaissance et accepte le plan du dit bien "
                + "et des parties indivises sans aucune observation ou restriction.");
        return sb.toString();
    }

    private static String surface(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private static Paragraph paiementParagraph(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Le réservataire s'engage en outre à régulariser le prix de vente comme suit :\n");
        sb.append("A titre d'avance sur réservation, la somme de ").append(montant(data.avanceMontant()))
                .append(" soit (").append(pourcentage(data.avancePct())).append(").\n");
        sb.append("Le solde du prix de la vente sera réglé ainsi :\n");
        for (LignePaiement ligne : data.lignesSolde()) {
            sb.append("- ").append(montant(ligne.montant())).append(" soit (").append(pourcentage(ligne.pct())).append(")");
            if (ligne.date() != null) {
                sb.append(" en date du ").append(date(ligne.date()));
            }
            sb.append("\n");
        }
        return bodyParagraph(sb.toString());
    }

    private static String savParagraph(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        if (data.dureeSavMois() != null) {
            sb.append("La durée du Service Après-Vente est de ").append(data.dureeSavMois())
                    .append(" mois à partir de la date de signature du contrat chez le notaire.\n");
        }
        sb.append("Avant la récupération des clés, il faut souscrire une assurance multirisques habitation d'une "
                + "durée minimum d'une année et remettre la fiche de visite technique et la synthèse des "
                + "obligations légalisées.\n");
        if (data.syndicDureeMois() != null) {
            sb.append("Pour la gestion des parties communes, la société ADMOD s'est chargée d'organiser la gestion "
                    + "de la copropriété pendant ").append(data.syndicDureeMois())
                    .append(" mois à partir de la création des titres fonciers.\n");
        }
        if (data.syndicMontantMensuel() != null && data.syndicDureeMois() != null) {
            BigDecimal total = data.syndicMontantMensuel().multiply(BigDecimal.valueOf(data.syndicDureeMois()));
            sb.append("Le réservataire versera un montant fixé à ").append(montant(total))
                    .append(", à raison de ").append(montant(data.syndicMontantMensuel()))
                    .append("/Mois à payer au syndicat des copropriétaires du projet pour une durée de ")
                    .append(data.syndicDureeMois()).append(" mois à compter de la signature du contrat chez le notaire.");
        }
        return sb.toString();
    }
}
