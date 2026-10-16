package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue : un contenu accepté par la liste blanche doit aussi se rendre en Word (FEATURE-modeles-documents.md §5, §7). */
class RevueRenduModeleTest {

    private static final MiseEnPageDto A4 = new MiseEnPageDto("A4", 15, 15, 15, 15, null, null);

    private static Element corps(String html) {
        return ContenuModele.analyser(html, CatalogueDossierVente.CATALOGUE).corps().clone();
    }

    private static String texteDocx(byte[] docx) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extracteur = new XWPFWordExtractor(document)) {
            return extracteur.getText();
        }
    }

    @Test
    void commentaireAvecDoubleTiret_accepteALEnregistrement_seRendAussiEnWord() throws IOException {
        Element contenu = corps("<p>Avant</p><!-- note -- interne --><p>Après</p>");

        byte[] docx = new RenduModele().docx(contenu, null, null, A4);

        assertThat(texteDocx(docx)).contains("Avant").contains("Après");
    }

    @Test
    void caractereDeControleColleDepuisWord_accepteALEnregistrement_seRendAussiEnWord() throws IOException {
        Element contenu = corps("<p>Ligne\u000Bsuite</p>");

        byte[] docx = new RenduModele().docx(contenu, null, null, A4);

        assertThat(texteDocx(docx)).contains("Ligne").contains("suite");
    }
}
