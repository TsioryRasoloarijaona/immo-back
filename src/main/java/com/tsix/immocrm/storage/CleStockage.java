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
        return normaliserPrefixe(prefix) + "pieces-jointes/" + porteur.segmentCle() + "/"
                + porteurId.toString().toLowerCase(Locale.ROOT) + "/"
                + pieceId.toString().toLowerCase(Locale.ROOT)
                + normaliserExtension(extension);
    }

    /**
     * {@code "prod"} / {@code "prod/"} / {@code "/prod/"} → {@code "prod/"} ; vide ou
     * {@code null} → {@code ""}.
     *
     * <p>Le préfixe devient facultatif dès lors que chaque environnement a <b>son propre
     * bucket</b> : le risque qu'il écartait — deux environnements écrivant la même clé, les
     * bases étant des restaurations du même jeu de données donc porteuses des mêmes
     * {@code PKey} — n'existe plus entre buckets distincts.
     *
     * <p>Un préfixe vide ne doit pour autant pas produire une clé commençant par {@code /} :
     * S3 l'accepterait, mais elle s'afficherait comme un dossier sans nom dans la console MinIO
     * et compliquerait toute inspection manuelle.
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
