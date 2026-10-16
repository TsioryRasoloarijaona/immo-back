package com.tsix.immocrm.services.modele;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionSmokeTest {

    private static final String XHTML = "<html><head><meta charset=\"UTF-8\"/></head><body>"
            + "<h2>1- PRIX</h2><p style=\"text-align:justify\">Bonjour <strong>Réservataire</strong> 1 200 000.00 DH</p>"
            + "<table><tbody><tr><td>a</td><td>b</td></tr></tbody></table></body></html>";

    @Test
    void docx() throws Exception {
        WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
        XHTMLImporterImpl importer = new XHTMLImporterImpl(pkg);
        pkg.getMainDocumentPart().getContent().addAll(importer.convert(XHTML, null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pkg.save(out);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out.toByteArray()));
             XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
            assertThat(ex.getText()).contains("Réservataire").contains("1- PRIX");
        }
    }

    @Test
    void pdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(XHTML, null);
        builder.toStream(out);
        builder.run();
        try (PDDocument doc = Loader.loadPDF(out.toByteArray())) {
            assertThat(new PDFTextStripper().getText(doc)).contains("Réservataire");
        }
    }
}
