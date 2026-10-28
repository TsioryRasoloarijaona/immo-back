package com.tsix.immocrm.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Lecture d'un PDF rendu (texte par page, position et police d'un mot, images dessinées) pour les tests de modèles. */
public final class LecturePdf {

    /** 1 cm en points PDF. */
    public static final float CM = 72f / 2.54f;

    private LecturePdf() {
    }

    /** Premier glyphe d'un mot : {@code x} et {@code ligneDeBase} en points depuis le coin haut-gauche de la page. */
    public record Glyphe(float x, float ligneDeBase, float taille, String police) {
    }

    /** Événement de dessin d'une page, dans l'ordre du flux : image (avec sa taille dessinée) ou texte. */
    public record Dessin(boolean image, float largeur, float hauteur) {
    }

    public static int pages(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    /** Texte de la page {@code page} (1 = première), blancs réduits à une espace. */
    public static String textePage(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper extracteur = new PDFTextStripper();
            extracteur.setSortByPosition(true);
            extracteur.setStartPage(page);
            extracteur.setEndPage(page);
            return extracteur.getText(document).replaceAll("\\s+", " ");
        }
    }

    /** Premier glyphe du mot {@code mot} sur la page {@code page} ; {@code null} si absent. */
    public static Glyphe glyphe(byte[] pdf, int page, String mot) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            ChercheurDeMot chercheur = new ChercheurDeMot(mot);
            chercheur.setSortByPosition(true);
            chercheur.setStartPage(page);
            chercheur.setEndPage(page);
            chercheur.getText(document);
            return chercheur.trouve;
        }
    }

    /** Images et textes dessinés sur la page {@code page}, dans l'ordre du flux (formulaires compris). */
    public static List<Dessin> dessins(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage p = document.getPage(page - 1);
            Enregistreur enregistreur = new Enregistreur(p);
            enregistreur.processPage(p);
            return List.copyOf(enregistreur.dessins);
        }
    }

    /** Largeur et hauteur de la page {@code page}, en points. */
    public static float[] format(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage p = document.getPage(page - 1);
            return new float[]{p.getMediaBox().getWidth(), p.getMediaBox().getHeight()};
        }
    }

    private static final class ChercheurDeMot extends PDFTextStripper {
        private final String mot;
        private Glyphe trouve;

        ChercheurDeMot(String mot) {
            this.mot = mot;
        }

        @Override
        protected void writeString(String texte, List<TextPosition> positions) throws IOException {
            int indice = texte.indexOf(mot);
            if (trouve == null && indice >= 0 && indice < positions.size()) {
                TextPosition p = positions.get(indice);
                trouve = new Glyphe(p.getXDirAdj(), p.getYDirAdj(), p.getFontSizeInPt(), p.getFont().getName());
            }
            super.writeString(texte, positions);
        }
    }

    private static final class Enregistreur extends PDFGraphicsStreamEngine {
        private final List<Dessin> dessins = new ArrayList<>();

        Enregistreur(PDPage page) {
            super(page);
        }

        @Override
        public void drawImage(PDImage image) {
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            dessins.add(new Dessin(true, Math.abs(ctm.getScalingFactorX()), Math.abs(ctm.getScalingFactorY())));
        }

        @Override
        public void showTextString(byte[] texte) {
            dessins.add(new Dessin(false, 0, 0));
        }

        @Override
        public void showTextStrings(COSArray textes) {
            dessins.add(new Dessin(false, 0, 0));
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
