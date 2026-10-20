package com.tsix.immocrm.web;

import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Type MIME servi pour une pièce jointe, d'après {@code AdmAttachedFile.Extention} (ex. {@code ".pdf"}). Ses clés sont
 * aussi la liste blanche des dépôts : ni svg (script servi en ligne) ni dwg (FEATURE-patrimoine-article-onglets.md §11).
 */
public final class ContentTypeParExtension {

    private static final MediaType JPEG = MediaType.IMAGE_JPEG;

    private static final Map<String, MediaType> PAR_EXTENSION = Map.ofEntries(
            Map.entry(".pdf", MediaType.APPLICATION_PDF),
            Map.entry(".jpg", JPEG),
            Map.entry(".jpeg", JPEG),
            Map.entry(".jfif", JPEG),
            Map.entry(".png", MediaType.IMAGE_PNG),
            Map.entry(".gif", MediaType.IMAGE_GIF),
            Map.entry(".webp", MediaType.valueOf("image/webp")),
            Map.entry(".heic", MediaType.valueOf("image/heic")),
            Map.entry(".doc", MediaType.valueOf("application/msword")),
            Map.entry(".docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".xls", MediaType.valueOf("application/vnd.ms-excel")),
            Map.entry(".xlsx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".txt", MediaType.TEXT_PLAIN)
    );

    /** « doc, docx, gif… » : la liste citée dans le refus d'un dépôt. */
    public static final String ADMISES = PAR_EXTENSION.keySet().stream().map(e -> e.substring(1)).sorted()
            .collect(Collectors.joining(", "));

    private ContentTypeParExtension() {
    }

    /** Insensible à la casse ; nul, vide ou inconnu → {@code application/octet-stream}. */
    public static MediaType resolve(String extension) {
        return PAR_EXTENSION.getOrDefault(cle(extension), MediaType.APPLICATION_OCTET_STREAM);
    }

    public static boolean admise(String extension) {
        return PAR_EXTENSION.containsKey(cle(extension));
    }

    private static String cle(String extension) {
        return extension != null ? extension.toLowerCase(Locale.ROOT) : "";
    }
}
