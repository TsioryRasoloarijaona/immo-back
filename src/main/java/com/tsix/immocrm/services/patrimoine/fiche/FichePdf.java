package com.tsix.immocrm.services.patrimoine.fiche;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.web.export.MiseEnPagePdf;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.tsix.immocrm.web.export.MiseEnPagePdf.ajouter;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.paragraphe;

/**
 * Fiche PDF (GEN0011) : en-tête société, titre, sections clé / valeur et tableaux, A4 portrait ; le contenu vient du
 * DTO du détail, déjà lu sous la visibilité de {@code GET /{id}} (FEATURE-patrimoine-import-export.md §3.2).
 */
@Component
public class FichePdf {

    private final CfgCompanyRepository societes;

    public FichePdf(CfgCompanyRepository societes) {
        this.societes = societes;
    }

    public record Champ(String libelle, Object valeur) {
    }

    /** Section clé / valeur ({@code champs}) ou tableau ({@code entetes} + {@code lignes}). */
    public record Section(String titre, List<Champ> champs, List<String> entetes, List<List<Object>> lignes) {

        public static Section champs(String titre, List<Champ> champs) {
            return new Section(titre, champs, List.of(), List.of());
        }

        /** {@code lignes} nul = tableau vide. */
        public static <T> Section tableau(String titre, List<String> entetes, List<T> lignes,
                                          Function<T, Object[]> valeurs) {
            List<T> source = lignes == null ? List.of() : lignes;
            return new Section(titre, List.of(), entetes,
                    source.stream().map(l -> Arrays.asList(valeurs.apply(l))).toList());
        }
    }

    /** @param nom préfixe du nom de fichier ; {@code societeId} nul = pas d'en-tête société */
    public record Fiche(String nom, String titre, UUID societeId, List<Section> sections) {
    }

    public ResponseEntity<byte[]> reponse(Fiche fiche) {
        List<String> societe = enTeteSociete(fiche.societeId());
        byte[] pdf = MiseEnPagePdf.rendre(PageSize.A4, document -> {
            societe.forEach(l -> ajouter(document, paragraphe(l, MiseEnPagePdf.NORMAL)));
            ajouter(document, paragraphe(fiche.titre(), MiseEnPagePdf.TITRE));
            fiche.sections().forEach(s -> section(document, s));
        });
        String nomFichier = fiche.nom().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    /** Raison sociale ({@code CfgCompany.Description}) puis adresse, lignes vides omises. */
    private List<String> enTeteSociete(UUID societeId) {
        if (societeId == null) {
            return List.of();
        }
        return societes.findById(societeId).map(FichePdf::lignesSociete).orElse(List.of());
    }

    private static List<String> lignesSociete(CfgCompany s) {
        String ville = Stream.of(s.getZipCode(), s.getCity()).filter(FichePdf::renseigne).reduce((a, b) -> a + " " + b)
                .orElse(null);
        return Stream.of(s.getDescription(), s.getAddress(), s.getAddress2(), s.getAddress3(), ville)
                .filter(FichePdf::renseigne).toList();
    }

    private static boolean renseigne(String texte) {
        return texte != null && !texte.isBlank();
    }

    private static void section(Document document, Section section) {
        PdfPTable table;
        if (section.entetes().isEmpty()) {
            table = new PdfPTable(4);
            section.champs().forEach(c -> {
                table.addCell(MiseEnPagePdf.entete(c.libelle()));
                table.addCell(MiseEnPagePdf.nouvelleCellule(MiseEnPagePdf.texte(c.valeur()), false, false));
            });
            table.completeRow();
        } else {
            table = new PdfPTable(section.entetes().size());
            table.setHeaderRows(section.lignes().isEmpty() ? 0 : 1);
            section.entetes().forEach(e -> table.addCell(MiseEnPagePdf.entete(e)));
            section.lignes().forEach(l -> l.forEach(v -> table.addCell(MiseEnPagePdf.nouvelleCellule(
                    MiseEnPagePdf.texte(v), v instanceof Number, false))));
        }
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        ajouter(document, paragraphe(section.titre(), MiseEnPagePdf.SECTION));
        if (table.size() > 0) {
            ajouter(document, table);
        }
    }
}
