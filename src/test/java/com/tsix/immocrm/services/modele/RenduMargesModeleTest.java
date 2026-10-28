package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.utils.LecturePdf;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Rendu des extensions du moteur (FEATURE-vente-formulaire-barid.md §3) avec des marges non nulles : la taille d'un bloc
 * positionné est celle demandée (openhtmltopdf tronque au px entier), son origine et celle du fond sont le coin de la page.
 */
class RenduMargesModeleTest {

    private static final MiseEnPageDto MARGES_10 = new MiseEnPageDto("A4", 10, 10, 10, 10, null, null);

    @ParameterizedTest
    @ValueSource(strings = {"6", "9", "15.75", "20", "72"})
    void blocPositionne_tailleEnPointsEtOrigineAuCoinDeLaPage(String taille) throws Exception {
        String html = "<div><div data-bloc data-x=\"3\" data-y=\"4\" data-l=\"15\" data-h=\"4\" data-taille=\"" + taille
                + "\">MOT</div></div>";
        byte[] pdf = new RenduModele().pdf(Jsoup.parseBodyFragment(html).body(), null, null, MARGES_10);

        LecturePdf.Glyphe mot = LecturePdf.glyphe(pdf, 1, "MOT");
        assertThat(mot).isNotNull();
        // PDFBox lit la taille tronquée à l'entier (15,75 → 15).
        assertThat(mot.taille()).isCloseTo(Float.parseFloat(taille), within(0.8f));
        assertThat(mot.x()).isCloseTo(3 * LecturePdf.CM, within(3f));
    }

    @Test
    void fondPleinePage_auCoinDeLaPageMalgreLesMarges() throws Exception {
        UUID fond = UUID.randomUUID();
        MiseEnPageDto miseEnPage = new MiseEnPageDto("A4", 10, 10, 10, 10, null, null, fond, true);
        ModelePrepare prepare = ModelePrepare.preparer(CatalogueDossierVente.CATALOGUE, "<p>Texte</p>", miseEnPage)
                .avecImages(Map.of(fond, png()));

        byte[] pdf = new RenduModele().pdf(prepare.corps(), null, null, miseEnPage);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage page = document.getPage(0);
            Positions positions = new Positions(page);
            positions.processPage(page);
            assertThat(positions.images).hasSize(1).first().satisfies(ctm -> {
                assertThat(ctm.getTranslateX()).isCloseTo(0f, within(2f));
                assertThat(ctm.getTranslateY()).isCloseTo(0f, within(2f));
                assertThat(Math.abs(ctm.getScalingFactorX())).isCloseTo(page.getMediaBox().getWidth(), within(2f));
            });
        }
    }

    private static String png() throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** Matrice de chaque image dessinée. */
    private static final class Positions extends PDFGraphicsStreamEngine {
        private final List<Matrix> images = new ArrayList<>();

        Positions(PDPage page) {
            super(page);
        }

        @Override
        public void drawImage(PDImage image) {
            images.add(getGraphicsState().getCurrentTransformationMatrix().clone());
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            // tracés ignorés
        }

        @Override
        public void clip(int regle) {
            // découpe ignorée
        }

        @Override
        public void moveTo(float x, float y) {
            // tracés ignorés
        }

        @Override
        public void lineTo(float x, float y) {
            // tracés ignorés
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            // tracés ignorés
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float();
        }

        @Override
        public void closePath() {
            // tracés ignorés
        }

        @Override
        public void endPath() {
            // tracés ignorés
        }

        @Override
        public void strokePath() {
            // tracés ignorés
        }

        @Override
        public void fillPath(int regle) {
            // tracés ignorés
        }

        @Override
        public void fillAndStrokePath(int regle) {
            // tracés ignorés
        }

        @Override
        public void shadingFill(COSName nom) {
            // dégradés ignorés
        }
    }
}
