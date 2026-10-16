package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.http.MediaType;

/** Sortie d'une génération : {@code pdf} ou {@code docx}, comme les autres documents du dossier. */
public enum FormatDocument {

    PDF("pdf", MediaType.APPLICATION_PDF),
    DOCX("docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    private final String extension;
    private final MediaType type;

    FormatDocument(String extension, MediaType type) {
        this.extension = extension;
        this.type = type;
    }

    public String extension() {
        return extension;
    }

    public MediaType type() {
        return type;
    }

    public static FormatDocument depuis(String format) {
        for (FormatDocument f : values()) {
            if (f.extension.equalsIgnoreCase(format)) {
                return f;
            }
        }
        throw new InvalidRequestException("format doit être 'pdf' ou 'docx' (reçu: " + format + ")");
    }
}
