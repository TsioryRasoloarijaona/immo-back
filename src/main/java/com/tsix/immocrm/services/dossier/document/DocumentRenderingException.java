package com.tsix.immocrm.services.dossier.document;

/** Exceptions vérifiées d'OpenPDF / Apache POI, rendues non vérifiées. */
public class DocumentRenderingException extends RuntimeException {
    public DocumentRenderingException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Refus applicatif — le rendu est impossible en soi, sans exception sous-jacente à envelopper. */
    public DocumentRenderingException(String message) {
        super(message);
    }
}
