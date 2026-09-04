package com.tsix.immocrm.storage;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Construction des clés d'objet.
 *
 * <pre>{@code {prefix}/pieces-jointes/{porteur}/{porteurId}/{fileId}{ext}}</pre>
 *
 * <p>La clé se termine par l'identifiant <b>immuable</b> de la pièce jointe : modifier sa
 * description ne déplace jamais l'objet. Le nom de fichier d'origine n'y entre <b>jamais</b>
 * (accents, espaces, données personnelles) — il vit dans {@code AdmAttachedFile.Description}
 * et n'est servi qu'en {@code Content-Disposition}.
 *
 * <p>Format identique à celui produit par le script de reprise
 * ({@code migration/migrate_blobs_to_s3.py}), mais ce n'est qu'une commodité de lecture du
 * bucket : {@code AppFileObject.StorageKey} stocke la clé <b>en entier</b> et l'application ne
 * la recalcule jamais pour un fichier existant. Une divergence de forme entre les clés issues
 * de la migration et celles des nouveaux dépôts serait donc sans conséquence.
 */
public final class CleStockage {

    /** Toute extension hors de ce format est écartée plutôt que reprise telle quelle. */
    private static final Pattern EXTENSION_VALIDE = Pattern.compile("^\\.[a-z0-9]{1,10}$");

    private CleStockage() {
    }

    public static String construire(String prefix, PorteurPieceJointe porteur, UUID porteurId,
                                    UUID pieceId, String extension) {
        return prefix + "/pieces-jointes/" + porteur.segmentCle() + "/"
                + porteurId.toString().toLowerCase(Locale.ROOT) + "/"
                + pieceId.toString().toLowerCase(Locale.ROOT)
                + normaliserExtension(extension);
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
