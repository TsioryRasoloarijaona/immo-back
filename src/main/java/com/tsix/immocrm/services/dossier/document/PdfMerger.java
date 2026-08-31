package com.tsix.immocrm.services.dossier.document;

import org.openpdf.text.Document;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Concatenates several single-document PDFs (each possibly already multi-page, e.g. "Fiche
 * Dossier de Vente") into one PDF, page order preserved — backs every
 * {@code POST /api/dossiers/documents/{type}} "génération en lot" endpoint (select several
 * dossiers, get one PDF with one page group per dossier). Pure page concatenation via
 * {@link PdfCopy}, no re-rendering: each input PDF is produced by the same renderer used for the
 * single-dossier endpoint, so a batch of 1 dossier is byte-for-byte content-identical to calling
 * the single-dossier endpoint directly (just re-encoded through PdfCopy).
 */
@Component
public class PdfMerger {

    public byte[] merge(List<byte[]> pdfs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (byte[] pdf : pdfs) {
                PdfReader reader = new PdfReader(pdf);
                try {
                    int pages = reader.getNumberOfPages();
                    for (int page = 1; page <= pages; page++) {
                        copy.addPage(copy.getImportedPage(reader, page));
                    }
                } finally {
                    reader.close();
                }
            }
        } catch (Exception e) {
            throw new DocumentRenderingException("Erreur lors de la fusion des PDF", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }
}
