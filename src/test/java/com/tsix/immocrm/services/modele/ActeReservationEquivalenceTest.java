package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureActeReservation;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critère de remplacement (FEATURE-modeles-documents.md §8) : le texte du PDF et du Word générés par le modèle livré
 * égale celui de l'actuel {@link ActeReservationPdfRenderer}, espaces normalisés, pour un appartement et un terrain.
 */
@SpringBootTest
@Transactional
@WithMockUser
class ActeReservationEquivalenceTest {

    @Autowired
    private FixtureActeReservation fixture;
    @Autowired
    private ActeReservationService acteReservationService;
    @Autowired
    private ActeReservationPdfRenderer pdfRenderer;
    @Autowired
    private InitialiseurModeles initialiseur;
    @Autowired
    private GenerationModeleService generation;

    /** Sans aucun espace : les retours à la ligne diffèrent d'un moteur à l'autre (« non-respect » coupé après le tiret). */
    static String normaliser(String texte) {
        return texte.replace(" ", "").replaceAll("\\s+", "");
    }

    /** Message d'échec centré sur le premier écart, lisible malgré la longueur de l'acte. */
    static String premierEcart(String attendu, String obtenu) {
        int i = 0;
        while (i < attendu.length() && i < obtenu.length() && attendu.charAt(i) == obtenu.charAt(i)) {
            i++;
        }
        int debut = Math.max(0, i - 60);
        return "écart à %d : attendu « %s » / obtenu « %s »".formatted(i,
                attendu.substring(debut, Math.min(attendu.length(), i + 60)),
                obtenu.substring(debut, Math.min(obtenu.length(), i + 60)));
    }

    static String textePdf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // openhtmltopdf écrit le gras après le reste de la ligne : ordre de lecture, pas ordre du flux.
            PDFTextStripper extracteur = new PDFTextStripper();
            extracteur.setSortByPosition(true);
            return normaliser(extracteur.getText(document));
        }
    }

    static String texteDocx(byte[] docx) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extracteur = new XWPFWordExtractor(document)) {
            return normaliser(extracteur.getText());
        }
    }

    private byte[] parModele(ComFolder dossier, FormatDocument format) {
        initialiseur.initialiserActeReservation();
        return generation.produireParCode(CategorieModele.DOSSIER_VENTE, ActeReservationModele.CODE, dossier.getId(), format)
                .orElseThrow()
                .contenu();
    }

    @ParameterizedTest(name = "terrain = {0}")
    @ValueSource(booleans = {false, true})
    void pdfDuModele_memeTexteQueLActeCodeEnDur(boolean terrain) throws IOException {
        ComFolder dossier = fixture.dossier("EQ-PDF", terrain, true);
        ActeReservationData data = acteReservationService.collect(dossier.getId());
        String attendu = textePdf(pdfRenderer.render(data));

        String obtenu = textePdf(parModele(dossier, FormatDocument.PDF));

        assertThat(obtenu).as(premierEcart(attendu, obtenu)).isEqualTo(attendu).contains("Rabat");
    }

    @ParameterizedTest(name = "terrain = {0}")
    @ValueSource(booleans = {false, true})
    void wordDuModele_memeTexteQueLActeCodeEnDur(boolean terrain) throws IOException {
        ComFolder dossier = fixture.dossier("EQ-DOCX", terrain, true);
        ActeReservationData data = acteReservationService.collect(dossier.getId());
        String attendu = textePdf(pdfRenderer.render(data));

        String obtenu = texteDocx(parModele(dossier, FormatDocument.DOCX));

        assertThat(obtenu).as(premierEcart(attendu, obtenu)).isEqualTo(attendu);
    }
}
