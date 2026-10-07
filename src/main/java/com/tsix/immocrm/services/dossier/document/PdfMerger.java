package com.tsix.immocrm.services.dossier.document;

import org.openpdf.text.Document;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Concatène des PDF par {@link PdfCopy}, sans re-rendu, dans l'ordre : la génération en lot. */
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
