package com.tsix.immocrm.storage;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code {prefix}/pieces-jointes/{porteur}/{porteurId}/{fileId}{ext}} : finit par l'id immuable, jamais le nom
 * d'origine (accents, données personnelles). La clé est stockée entière et jamais recalculée pour un fichier existant.
 */
public final class CleStockage {

    /** Hors de ce format, l'extension est écartée plutôt que reprise telle quelle. */
    private static final Pattern EXTENSION_VALIDE = Pattern.compile("^\\.[a-z0-9]{1,10}$");

    private CleStockage() {
    }

    public static String construire(String prefix, PorteurPieceJointe porteur, UUID porteurId,
                                    UUID pieceId, String extension) {
        return normaliserPrefixe(prefix) + "pieces-jointes/" + porteur.segmentCle() + "/"
                + porteurId.toString().toLowerCase(Locale.ROOT) + "/"
                + pieceId.toString().toLowerCase(Locale.ROOT)
                + normaliserExtension(extension);
    }

    /**
     * {@code "/prod/"} → {@code "prod/"} ; vide → {@code ""}. Facultatif avec un bucket par environnement ; vide, il ne
     * doit pas laisser de {@code /} en tête (dossier sans nom dans la console MinIO).
     */
    private static String normaliserPrefixe(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String propre = prefix.trim();
        while (propre.startsWith("/")) {
            propre = propre.substring(1);
        }
        while (propre.endsWith("/")) {
            propre = propre.substring(0, propre.length() - 1);
        }
        return propre.isEmpty() ? "" : propre + "/";
    }

    /** {@code .PDF} / {@code pdf} / {@code null} → {@code .pdf} / {@code .pdf} / {@code ""}. */
    public static String normaliserExtension(String brute) {
        if (brute == null || brute.isBlank()) {
            return "";
        }
        String extension = brute.trim().toLowerCase(Locale.ROOT);
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return EXTENSION_VALIDE.matcher(extension).matches() ? extension : "";
    }
}
