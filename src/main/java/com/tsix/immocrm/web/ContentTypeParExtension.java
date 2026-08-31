package com.tsix.immocrm.web;

import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves a stored {@code AdmAttachedFile.Extention} (e.g. {@code ".pdf"}) to the
 * {@link MediaType} to serve it as — shared by every controller that streams a raw pièce
 * jointe (Dossier, Article...), previously duplicated per-controller.
 */
public final class ContentTypeParExtension {

    private static final Map<String, MediaType> PAR_EXTENSION = Map.ofEntries(
            Map.entry(".pdf", MediaType.APPLICATION_PDF),
            Map.entry(".jpg", MediaType.IMAGE_JPEG),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG),
            Map.entry(".png", MediaType.IMAGE_PNG),
            Map.entry(".gif", MediaType.IMAGE_GIF),
            Map.entry(".doc", MediaType.valueOf("application/msword")),
            Map.entry(".docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".xls", MediaType.valueOf("application/vnd.ms-excel")),
            Map.entry(".xlsx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".txt", MediaType.TEXT_PLAIN)
    );

    private ContentTypeParExtension() {
    }

    /** {@code extension} may be {@code null}/empty/any case — falls back to {@code application/octet-stream}. */
    public static MediaType resolve(String extension) {
        String cle = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        return PAR_EXTENSION.getOrDefault(cle, MediaType.APPLICATION_OCTET_STREAM);
    }
}
