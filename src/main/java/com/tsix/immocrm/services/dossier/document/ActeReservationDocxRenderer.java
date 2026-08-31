package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.pourcentage;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Renders the "Acte de réservation" as a Word (.docx) document — same content as
 * {@link ActeReservationPdfRenderer}, built with Apache POI (XWPF).
 */
@Component
public class ActeReservationDocxRenderer {

    public byte[] render(ActeReservationData data) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addTitle(document, "ACTE DE RÉSERVATION");
            blank(document);

            addHeader(document, data);
            blank(document);

            String entreprise = vide(data.entrepriseNom());

            addSection(document, "1-");
            addBody(document, "Le réservataire s'engage par la présente à acquérir auprès de la société " + entreprise
                    + " le bien ci-après désigné, il s'engage ainsi à payer le prix selon les modalités ci-dessous. Le "
                    + "présent contrat de réservation ne peut en aucun cas valoir contrat de vente, ce dernier est à "
                    + "signer par-devant notaire après " + (data.estTerrain() ? "" : "achèvement des travaux et ")
                    + "éclatement des titres fonciers.");
            blank(document);

            addSection(document, "2- DÉSIGNATION");
            addBody(document, designationText(data));
            blank(document);

            addSection(document, "3- PRIX DE VENTE");
            addBody(document, "La présente réservation est consentie et acceptée moyennant le prix de vente de "
                    + montant(data.prixVente()) + " TTC. Il est à préciser que la TVA applicable à la présente "
                    + "vente sera celle imposée par la loi au moment de la signature du contrat de vente, toute "
                    + "augmentation du taux de la dite TVA demeure supportée par le réservataire.");
            blank(document);

            addSection(document, "4- MODALITÉS DE PAIEMENT");
            addBody(document, paiementText(data));
            blank(document);

            addSection(document, "5- PROPRIÉTÉ ET JOUISSANCE");
            addBody(document, "La présente réservation ne donne aucun droit d'occupation ou de jouissance du bien "
                    + "objet de la dite réservation, le réservataire n'aura la pleine propriété et jouissance "
                    + "qu'après la signature du contrat définitif de vente et son inscription sur les livres "
                    + "fonciers tel que prévu par les articles 66 et 67 du Dahir du 12 Août 1913 sur "
                    + "l'immatriculation foncière tel que modifié et complété par la loi 14-07.");
            blank(document);

            addSection(document, "6- CONDITIONS SUSPENSIVES");
            addBody(document, "En cas de fausse déclaration ou de non-respect des conditions du présent acte, la "
                    + "société " + entreprise + " se réserve le droit de considérer la présente réservation comme "
                    + "résiliée et retiendra sur le montant de l'acquisition une indemnité contractuelle de "
                    + pourcentage(data.penaliteResiliationPct()) + " du prix de vente global TTC.");
            blank(document);

            addSection(document, "7- PENALITES");
            addBody(document, "En cas de désistement : Pénalité de " + pourcentage(data.penaliteResiliationPct())
                    + " du Prix de Vente. En cas de Transferts : Produit : " + montant(data.transfertProduitMontant())
                    + " - Tranche/Projet : " + montant(data.transfertTrancheMontant())
                    + " - Client* : " + pourcentage(data.transfertClientPct()) + " du prix de vente.");
            blank(document);

            if (!data.estTerrain()) {
                addSection(document, "8- SERVICE APRES VENTE - ASSURANCE - SYNDIC");
                addBody(document, savText(data));
                blank(document);
            }
            int n = data.estTerrain() ? 8 : 9;

            addSection(document, n + "- PROTECTION DES DONNEES PERSONNELLES");
            addBody(document, entreprise + " (Groupe AYAD Developpement) s'engage à ce que la collecte et le "
                    + "traitement d'informations personnelles soient effectués conformément à la loi n°09-08 du 18 "
                    + "février 2009 relative à la protection des personnes physiques.");
            blank(document);

            addSection(document, (n + 1) + "- ELECTION DE DOMICILE");
            addBody(document, "Le réservataire fait élection de domicile en son lieu respectif sus indiqué et doit "
                    + "aviser la société " + entreprise + " de tout changement d'adresse postale.");
            blank(document);

            addSection(document, (n + 2) + "- ATTRIBUTION DE COMPÉTENCE");
            addBody(document, "Toute contestation doit être réglée à l'amiable, à défaut compétence est donnée aux "
                    + "Tribunaux de " + vide(data.tribunalCompetent()) + ".");
            blank(document);
            blank(document);

            XWPFParagraph fait = document.createParagraph();
            fait.createRun().setText("Fait à " + vide(data.tribunalCompetent()) + " le : " + date(data.dateEdition()));
            blank(document);

            XWPFParagraph signataire = document.createParagraph();
            XWPFRun signataireLabel = signataire.createRun();
            signataireLabel.setText("Le réservataire");
            signataireLabel.setBold(true);
            signataireLabel.addBreak();
            signataireLabel.setText(vide(data.clientTitre()) + " " + vide(data.clientNom()));
            blank(document);

            XWPFParagraph societe = document.createParagraph();
            XWPFRun societeLabel = societe.createRun();
            societeLabel.setText("Pour la société " + entreprise);
            societeLabel.setBold(true);
            societeLabel.addBreak();
            societeLabel.setText(vide(data.signataireNom()));
            societeLabel.addBreak();
            societeLabel.setText("Directeur Général");

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DocumentRenderingException("Erreur lors de la génération du Word de l'acte de réservation", e);
        }
    }

    private static void blank(XWPFDocument document) {
        document.createParagraph();
    }

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
    }

    private static void addSection(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
    }

    private static void addBody(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.createRun().setText(text);
    }

    private static void addHeader(XWPFDocument document, ActeReservationData data) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun r = p.createRun();
        r.setText(vide(data.clientTitre()) + " : " + vide(data.clientNom()) + " Titulaire de la Pièce d'identité "
                + "(CIN) n° " + vide(data.clientCin()) + " demeurant à : " + vide(data.clientAdresse()));
        if (data.clientTelephone() != null) {
            r.addBreak();
            r.setText("Tél : " + data.clientTelephone());
        }
        r.addBreak();
        r.setText("Ci-après, dénommé(e) «Réservataire»");
    }

    private static String designationText(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("La totalité de la propriété sise au projet ").append(vide(data.projetNom()));
        if (data.projetAdresse() != null) {
            sb.append(", ").append(data.projetAdresse());
        }
        sb.append(", consistant en un ").append(vide(data.articleCategorie()))
                .append(" portant le numéro ").append(vide(data.articleCode()));
        if (!data.estTerrain()) {
            sb.append(", situé au ").append(vide(data.etage())).append(" de l'Immeuble ").append(vide(data.immeubleCode()));
        }
        sb.append(", d'une superficie de ").append(surface(data.superficie())).append(" m²");
        for (SousArticle sous : data.sousArticles()) {
            sb.append(" et un ").append(vide(sous.typeLabel()));
            if (sous.superficie() != null) {
                sb.append(" d'une superficie de ").append(surface(sous.superficie())).append(" m²");
            }
        }
        sb.append(". Le réservataire déclare avoir pris parfaitement connaissance et accepte le plan du dit bien "
                + "et des parties indivises sans aucune observation ou restriction.");
        return sb.toString();
    }

    private static String surface(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private static String paiementText(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Le réservataire s'engage à régulariser le prix de vente comme suit : à titre d'avance sur "
                + "réservation, la somme de ").append(montant(data.avanceMontant()))
                .append(" soit (").append(pourcentage(data.avancePct())).append("). Le solde du prix de la vente "
                + "sera réglé ainsi : ");
        for (LignePaiement ligne : data.lignesSolde()) {
            sb.append("- ").append(montant(ligne.montant())).append(" soit (").append(pourcentage(ligne.pct())).append(")");
            if (ligne.date() != null) {
                sb.append(" en date du ").append(date(ligne.date()));
            }
            sb.append("; ");
        }
        return sb.toString();
    }

    private static String savText(ActeReservationData data) {
        StringBuilder sb = new StringBuilder();
        if (data.dureeSavMois() != null) {
            sb.append("La durée du Service Après-Vente est de ").append(data.dureeSavMois())
                    .append(" mois à partir de la date de signature du contrat chez le notaire. ");
        }
        sb.append("Avant la récupération des clés, il faut souscrire une assurance multirisques habitation d'une "
                + "durée minimum d'une année. ");
        if (data.syndicMontantMensuel() != null && data.syndicDureeMois() != null) {
            BigDecimal total = data.syndicMontantMensuel().multiply(BigDecimal.valueOf(data.syndicDureeMois()));
            sb.append("Le réservataire versera un montant fixé à ").append(montant(total))
                    .append(", à raison de ").append(montant(data.syndicMontantMensuel()))
                    .append("/Mois pendant ").append(data.syndicDureeMois())
                    .append(" mois à compter de la signature du contrat chez le notaire.");
        }
        return sb.toString();
    }
}
