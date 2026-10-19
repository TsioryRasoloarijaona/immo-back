package com.tsix.immocrm.services.note;

import java.util.Locale;
import java.util.Map;

/**
 * Passage texte ↔ {@code AdmMessage.Notes} (HTML lu par OMRAN), FEATURE-patrimoine-audit-actions.md §11.8. Parcours
 * caractère par caractère, sans regex (ReDoS, S8786) : chaque position n'est lue qu'une fois.
 */
final class TexteNote {

    private static final Map<String, String> ENTITES = Map.of(
            "lt", "<", "gt", ">", "amp", "&", "quot", "\"", "apos", "'", "nbsp", " ");
    private static final int ENTITE_MAX = 10;

    private TexteNote() {
    }

    /** {@code & < > " '} échappés, sauts de ligne en {@code <br>}. */
    static String versHtml(String texte) {
        StringBuilder html = new StringBuilder(texte.length() + 16);
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            switch (c) {
                case '&' -> html.append("&amp;");
                case '<' -> html.append("&lt;");
                case '>' -> html.append("&gt;");
                case '"' -> html.append("&quot;");
                case '\'' -> html.append("&#39;");
                case '\n' -> html.append("<br>");
                case '\r' -> { }
                default -> html.append(c);
            }
        }
        return html.toString();
    }

    /** Texte sans aucune balise, entités décodées, {@code <br>} et {@code </p>} en saut de ligne. */
    static String versTexte(String html) {
        if (html == null) {
            return "";
        }
        StringBuilder texte = new StringBuilder(html.length());
        boolean fermantRestant = true;
        int i = 0;
        while (i < html.length()) {
            char c = html.charAt(i);
            int fin = c == '<' && fermantRestant ? html.indexOf('>', i) : -1;
            if (c == '<' && fin < 0) {
                // Plus aucun '>' : ne pas relancer la recherche à chaque '<' suivant (parcours quadratique).
                fermantRestant = false;
            }
            if (fin > 0) {
                texte.append(sautDeLigne(html.substring(i + 1, fin)));
                i = fin + 1;
            } else if (c == '&') {
                i = entite(html, i, texte);
            } else {
                texte.append(c);
                i++;
            }
        }
        return retirerSautsFinaux(texte);
    }

    private static String sautDeLigne(String balise) {
        String nom = balise.strip().toLowerCase(Locale.ROOT);
        return nom.equals("br") || nom.startsWith("br ") || nom.startsWith("br/") || nom.equals("/p") ? "\n" : "";
    }

    /** Ajoute l'entité décodée (ou le {@code &} tel quel) et rend la position suivante. */
    private static int entite(String html, int debut, StringBuilder texte) {
        int fin = pointVirgule(html, debut);
        String nom = fin > 0 ? html.substring(debut + 1, fin) : null;
        String decode = nom == null ? null : decoder(nom);
        if (decode == null) {
            texte.append('&');
            return debut + 1;
        }
        texte.append(decode);
        return fin + 1;
    }

    /** Recherche bornée à {@link #ENTITE_MAX} caractères : un {@code &} sans {@code ;} ne relit pas tout le texte. */
    private static int pointVirgule(String html, int debut) {
        int limite = Math.min(html.length(), debut + ENTITE_MAX + 1);
        for (int j = debut + 1; j < limite; j++) {
            if (html.charAt(j) == ';') {
                return j;
            }
        }
        return -1;
    }

    private static String decoder(String nom) {
        if (nom.startsWith("#")) {
            try {
                boolean hexa = nom.startsWith("#x") || nom.startsWith("#X");
                int code = Integer.parseInt(nom.substring(hexa ? 2 : 1), hexa ? 16 : 10);
                return Character.isValidCodePoint(code) ? Character.toString(code) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return ENTITES.get(nom.toLowerCase(Locale.ROOT));
    }

    private static String retirerSautsFinaux(StringBuilder texte) {
        int fin = texte.length();
        while (fin > 0 && texte.charAt(fin - 1) == '\n') {
            fin--;
        }
        return texte.substring(0, fin);
    }
}
