package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.web.export.MiseEnPagePdf.ajouter;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.paragraphe;

/**
 * Export d'une liste en PDF (OpenPDF), mêmes entrées qu'{@link ExportTableur} : A4 paysage, un tableau par
 * feuille, en-tête de colonnes répété à chaque page, ligne « Total » si {@code totaux}, pied « Page n / m ».
 */
@Component
public class ExportPdf {

    /** Au-delà, un PDF n'est plus lisible (≈ 60 pages) : l'export se fait en {@code .xlsx}. */
    public static final int LIGNES_PDF_MAX = 2_000;
    public static final String TROP_DE_LIGNES = "Trop de lignes pour un PDF : exportez en .xlsx";

    private static final String TOTAL = "Total";
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Clock clock;
    private final UtilisateurAffiche utilisateurAffiche;

    public ExportPdf(Clock clock, UtilisateurAffiche utilisateurAffiche) {
        this.clock = clock;
        this.utilisateurAffiche = utilisateurAffiche;
    }

    /**
     * @param filtres filtres appliqués, une ligne « libellé : valeur » chacun, écrits sous le titre
     * @throws InvalidRequestException (400) au-delà de {@link #LIGNES_PDF_MAX} lignes
     */
    public ResponseEntity<byte[]> reponse(String liste, String titre, List<String> filtres, List<FeuilleExport> feuilles) {
        if (feuilles.stream().mapToInt(f -> f.lignes().size()).sum() > LIGNES_PDF_MAX) {
            throw new InvalidRequestException(TROP_DE_LIGNES);
        }
        byte[] pdf = ecrire(titre, utilisateurAffiche.courant().nom(), filtres, feuilles);
        String nomFichier = liste + "_" + LocalDateTime.now(clock).format(HORODATAGE) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(pdf);
    }

    public byte[] ecrire(String titre, String auteur, List<String> filtres, List<FeuilleExport> feuilles) {
        String edition = "Édité le " + MiseEnPagePdf.DATE.format(LocalDate.now(clock)) + " par " + auteur;
        return MiseEnPagePdf.rendre(PageSize.A4.rotate(), document -> {
            ajouter(document, paragraphe(titre, MiseEnPagePdf.TITRE));
            ajouter(document, paragraphe(edition, MiseEnPagePdf.NORMAL));
            filtres.forEach(f -> ajouter(document, paragraphe(f, MiseEnPagePdf.NORMAL)));
            feuilles.forEach(f -> feuille(document, f, feuilles.size() > 1));
        });
    }

    private static void feuille(Document document, FeuilleExport feuille, boolean nommer) {
        if (nommer) {
            ajouter(document, paragraphe(feuille.nom(), MiseEnPagePdf.SECTION));
        }
        List<? extends Colonne> colonnes = feuille.colonnes();
        PdfPTable table = new PdfPTable(Math.max(colonnes.size(), 1));
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        // OpenPDF n'imprime pas une table réduite à sa ligne d'en-tête : liste vide = en-tête ordinaire.
        table.setHeaderRows(feuille.lignes().isEmpty() && feuille.totaux() == null ? 0 : 1);
        colonnes.forEach(c -> table.addCell(MiseEnPagePdf.entete(c.libelle())));
        feuille.lignes().forEach(l -> ligne(table, colonnes, LignesAplaties.de(l), false));
        if (feuille.totaux() != null) {
            ligne(table, colonnes, feuille.totaux(), true);
        }
        ajouter(document, table);
    }

    private static void ligne(PdfPTable table, List<? extends Colonne> colonnes, Map<String, ?> valeurs, boolean total) {
        for (int i = 0; i < colonnes.size(); i++) {
            Colonne colonne = colonnes.get(i);
            Object valeur = valeurs.get(colonne.nom());
            String texte = total && i == 0 && valeur == null ? TOTAL : MiseEnPagePdf.texte(valeur, colonne.typeExport());
            boolean chiffre = colonne.typeExport() == TypeExport.MONTANT || colonne.typeExport() == TypeExport.NOMBRE;
            table.addCell(MiseEnPagePdf.nouvelleCellule(texte, chiffre, total));
        }
    }
}
