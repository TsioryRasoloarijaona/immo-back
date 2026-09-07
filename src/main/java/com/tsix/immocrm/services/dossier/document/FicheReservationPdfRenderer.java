package com.tsix.immocrm.services.dossier.document;

import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.Alignement;
import com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.Zone;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.ARTICLE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.ADRESSE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.CIN;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.CM;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.DATE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.DOSSIER;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.ETAGE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.IMMEUBLE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.MODE_PAIEMENT;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.MONTANT;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.NOM;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.PAGE_H;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.PAGE_L;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.PORTE;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.PRIX;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.REGLE_PAR;
import static com.tsix.immocrm.services.dossier.document.GabaritFicheReservation.SUPERFICIE;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.date;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.montant;
import static com.tsix.immocrm.services.dossier.document.MontantFormatter.vide;

/**
 * Rend la « Fiche de réservation » en PDF, à l'identique du document légataire.
 *
 * <p>Le rapport d'origine ne dessine rien : chaque marque a un <b>fond de page pré-imprimé</b>
 * (cadre, logo, libellés, cases, paragraphes fixes, bandeau de pied) posé en filigrane, sur
 * lequel une quinzaine de valeurs sont incrustées. Ce générateur fait la même chose, et c'est
 * ce qui rend bordures, espacements et polices exacts : ils ne sont pas reproduits, ils sont
 * <i>les mêmes</i>. La version précédente redessinait tout en primitives, ce qui ne pouvait pas
 * coïncider — la police du fond n'est pas Helvetica.
 *
 * <p><b>La marque vient du préfixe du code dossier</b>, comme dans le rapport légataire, dont les
 * pages s'appellent APF / ARS / ART / MBR. C'est aussi ce qui décide de la société mentionnée,
 * du projet cité et des clauses imprimées : un dossier ADMOD et un dossier GHRIZA n'ont ni le
 * même en-tête, ni les mêmes conditions.
 */
@Component
public class FicheReservationPdfRenderer {

    /** Une fiche de réservation est un document contractuel : la police reste noire et nette. */
    private static final Color NOIR = Color.BLACK;

    /** Arial dans le rapport d'origine ; Helvetica en est l'équivalent métrique en PDF. */
    private static final String POLICE = FontFactory.HELVETICA_BOLD;

    /** Les fonds pèsent ~1 Mo : lus une fois, gardés en mémoire. */
    private static final Map<String, byte[]> FONDS = new ConcurrentHashMap<>();

    public byte[] render(FicheReservationData data) {
        String marque = marque(data.dossierCode());
        verifierCoherenceSociete(marque, data.entrepriseNom());
        Map<String, Zone> gabarit = gabarit(marque);

        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            poserFond(writer, marque);
            PdfContentByte canevas = writer.getDirectContent();

            ecrire(canevas, gabarit.get(DOSSIER), vide(data.dossierCode()));
            ecrire(canevas, gabarit.get(NOM), vide(data.clientNom()));
            ecrire(canevas, gabarit.get(ADRESSE), vide(data.clientAdresse()));
            ecrire(canevas, gabarit.get(CIN), vide(data.clientCin()));
            ecrire(canevas, gabarit.get(MODE_PAIEMENT), vide(data.modePaiement()));
            ecrire(canevas, gabarit.get(MONTANT), montant(data.montant()));
            ecrire(canevas, gabarit.get(REGLE_PAR), vide(data.reglePar()));
            ecrire(canevas, gabarit.get(PRIX), montant(data.prixArticle()));
            ecrire(canevas, gabarit.get(ARTICLE), vide(data.articleCode()));
            ecrire(canevas, gabarit.get(IMMEUBLE), vide(data.immeubleCode()));
            ecrire(canevas, gabarit.get(PORTE), vide(data.porte()));
            ecrire(canevas, gabarit.get(ETAGE), vide(data.etage()));
            ecrire(canevas, gabarit.get(SUPERFICIE), superficie(data));
            ecrire(canevas, gabarit.get(DATE), date(data.dateEdition()));

            if (MARQUE_ARS.equals(marque)) {
                ecrire(canevas, GabaritFicheReservation.ZONE_MENTION_ARS,
                        GabaritFicheReservation.MENTION_ARS);
            }

            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new DocumentRenderingException("Échec du rendu de la fiche de réservation", e);
        }
    }

    static final String MARQUE_APF = "APF";
    static final String MARQUE_ARS = "ARS";

    /**
     * Marque déduite du préfixe du code dossier (« ARS 2026/00370 » → {@code ARS}).
     *
     * <p>Un préfixe non couvert est un <b>refus explicite</b>, pas un repli. Émettre un reçu de
     * réservation sous l'en-tête et les conditions d'une autre société serait pire qu'une erreur
     * visible : c'est une pièce remise au client. Les marques MBR (Mansouria Beach Resort) et ART
     * (Residence Tribeca) existent dans le rapport légataire et représentent 221 dossiers réels —
     * les ajouter demande leur fond et le relevé de leurs cases.
     */
    private static String marque(String codeDossier) {
        String prefixe = codeDossier == null || codeDossier.length() < 3
                ? ""
                : codeDossier.substring(0, 3).toUpperCase();
        if (!MARQUE_APF.equals(prefixe) && !MARQUE_ARS.equals(prefixe)) {
            throw new ConflictException(
                    "Fiche de réservation non disponible pour la marque « " + prefixe + " » : "
                            + "seules APF et ARS ont un fond de page. Émettre le document sous "
                            + "l'en-tête d'une autre société n'est pas acceptable pour une pièce "
                            + "remise au client.");
        }
        return prefixe;
    }

    /** Société attendue pour chaque marque, telle qu'imprimée dans le fond de page. */
    private static String societeAttendue(String marque) {
        return MARQUE_ARS.equals(marque) ? "GHRIZA" : "ADMOD";
    }

    /**
     * Refuse d'émettre si la société du projet ne correspond pas à la marque déduite du code.
     *
     * <p>Le nom de la société n'est plus une valeur incrustée : il fait partie du fond pré-imprimé,
     * comme dans le rapport légataire. Cette garantie-là ne peut donc plus se vérifier en lisant le
     * PDF — elle doit être <b>appliquée</b> ici. Sans ce contrôle, un dossier codé {@code APF…} mais
     * rattaché à une autre entité sortirait sous l'en-tête et les clauses d'ADMOD, en silence.
     */
    private static void verifierCoherenceSociete(String marque, String entreprise) {
        String attendue = societeAttendue(marque);
        if (entreprise == null || !entreprise.toUpperCase().contains(attendue)) {
            throw new ConflictException(
                    "Incohérence entre le code dossier (marque « " + marque + " », en-tête "
                            + attendue + ") et l'entité juridique du projet « " + entreprise
                            + " ». La fiche n'est pas émise : elle porterait l'en-tête et les "
                            + "clauses d'une société qui n'est pas le vendeur.");
        }
    }

    private static Map<String, Zone> gabarit(String marque) {
        return MARQUE_ARS.equals(marque) ? GabaritFicheReservation.ARS : GabaritFicheReservation.APF;
    }

    /** Le fond couvre la page entière : les deux images font exactement 21 × 29,7 cm à 300 dpi. */
    private void poserFond(PdfWriter writer, String marque) throws IOException {
        Image fond = Image.getInstance(fond(marque));
        fond.setAbsolutePosition(0, 0);
        fond.scaleAbsolute(PAGE_L * CM, PAGE_H * CM);
        try {
            writer.getDirectContentUnder().addImage(fond);
        } catch (org.openpdf.text.DocumentException e) {
            throw new DocumentRenderingException("Fond de page inutilisable pour la marque " + marque, e);
        }
    }

    private static byte[] fond(String marque) {
        return FONDS.computeIfAbsent(marque, m -> {
            ClassPathResource resource = new ClassPathResource("documents/fiche-reservation/fond-" + m + ".jpg");
            try (InputStream in = resource.getInputStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new DocumentRenderingException("Fond de page introuvable pour la marque " + m, e);
            }
        });
    }

    /**
     * Écrit une valeur dans sa boîte, centrée verticalement.
     *
     * <p>{@code showTextAligned} placerait le texte sur une ligne de base, ce qui obligerait à
     * réajuster à chaque changement de taille. Une colonne de la hauteur de la case, avec un
     * décalage d'un tiers de cadratin, garde l'alignement optique sur les filets du fond.
     */
    private static void ecrire(PdfContentByte canevas, Zone zone, String valeur) {
        if (zone == null || valeur == null || valeur.isBlank()) {
            return;
        }
        Font police = FontFactory.getFont(POLICE, zone.taille(), NOIR);
        float x = zone.xCm() * CM;
        float largeur = zone.largeurCm() * CM;
        float interligne = zone.taille() * 1.15f;

        // La colonne est construite AUTOUR de la ligne de base voulue, pas autour de la case.
        // Sinon une case plus basse que l'interligne — les valeurs incrustées dans le paragraphe
        // font 0,4 cm, soit 11,3 pt, pour un interligne de 11,5 pt à 10 pt de corps — ne peut
        // accueillir aucune ligne : OpenPDF n'écrit rien, sans erreur ni trace.
        float centre = (PAGE_H - zone.yCm() - zone.hauteurCm() / 2f) * CM;
        float ligneDeBase = centre - zone.taille() * 0.35f;

        ColumnText colonne = new ColumnText(canevas);
        colonne.setLeading(interligne);
        colonne.setAlignment(alignement(zone.alignement()));
        // Hauteur volontairement limitée à une seule ligne : une valeur trop longue est tronquée,
        // comme dans le document légataire (le mode de paiement l'est déjà sur la référence APF).
        // Sans ça elle passerait à la ligne et déborderait de la case imprimée sur le fond.
        colonne.setSimpleColumn(x, ligneDeBase - 0.2f, x + largeur, ligneDeBase + interligne);
        colonne.addText(new Phrase(valeur, police));
        try {
            colonne.go();
        } catch (org.openpdf.text.DocumentException e) {
            throw new DocumentRenderingException("Placement impossible pour la valeur « " + valeur + " »", e);
        }
    }

    private static int alignement(Alignement a) {
        return switch (a) {
            case CENTRE -> Element.ALIGN_CENTER;
            case DROITE -> Element.ALIGN_RIGHT;
            case GAUCHE -> Element.ALIGN_LEFT;
        };
    }

    /** La superficie s'imprime à deux décimales, sans unité : le « m² » est dans le fond. */
    private static String superficie(FicheReservationData data) {
        return data.superficie() == null ? "" : String.format("%.2f", data.superficie());
    }
}
