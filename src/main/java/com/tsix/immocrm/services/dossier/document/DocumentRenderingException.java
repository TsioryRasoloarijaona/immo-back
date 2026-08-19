package com.tsix.immocrm.services.dossier.document;

/** Wraps checked rendering exceptions from OpenPDF/Apache POI as an unchecked exception. */
public class DocumentRenderingException extends RuntimeException {
    public DocumentRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
