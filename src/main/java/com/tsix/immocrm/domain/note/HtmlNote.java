package com.tsix.immocrm.domain.note;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assainit le HTML d'une note par liste blanche : seules les balises de mise en forme simple survivent, <b>sans aucun
 * attribut</b> ; tout autre texte est échappé ; scripts, styles et commentaires disparaissent avec leur contenu.
 */
public final class HtmlNote {

    private static final Set<String> BALISES_PERMISES =
            Set.of("p", "br", "b", "strong", "i", "em", "u", "ul", "ol", "li", "div", "span");
    private static final Set<String> BALISES_VIDES = Set.of("br");

    private static final Pattern BLOCS_DANGEREUX = Pattern.compile(
            "<(script|style)\\b[^>]*>.*?</\\1\\s*>|<!--.*?-->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BALISE = Pattern.compile("<\\s*(/?)\\s*([a-zA-Z][a-zA-Z0-9]*)[^<>]*>");
    private static final Pattern TOUTE_BALISE = Pattern.compile("<[^<>]*>");

    private HtmlNote() {
    }

    public static String assainir(String html) {
        if (html == null) {
            return "";
        }
        String sansBlocs = BLOCS_DANGEREUX.matcher(html).replaceAll("");
        Matcher balise = BALISE.matcher(sansBlocs);
        StringBuilder sortie = new StringBuilder();
        int position = 0;
        while (balise.find()) {
            sortie.append(echapper(sansBlocs.substring(position, balise.start())));
            String nom = balise.group(2).toLowerCase(Locale.ROOT);
            if (BALISES_PERMISES.contains(nom)) {
                boolean fermante = !balise.group(1).isEmpty();
                if (!fermante || !BALISES_VIDES.contains(nom)) {
                    sortie.append('<').append(fermante ? "/" : "").append(nom).append('>');
                }
            }
            position = balise.end();
        }
        sortie.append(echapper(sansBlocs.substring(position)));
        return sortie.toString();
    }

    /** Vrai si la note assainie ne contient aucun texte visible (balises et espaces seulement). */
    public static boolean estVide(String htmlAssaini) {
        String texte = TOUTE_BALISE.matcher(htmlAssaini).replaceAll("").replace("&nbsp;", " ");
        return texte.isBlank();
    }

    private static String echapper(String texte) {
        StringBuilder sortie = new StringBuilder(texte.length());
        for (char c : texte.toCharArray()) {
            switch (c) {
                case '<' -> sortie.append("&lt;");
                case '>' -> sortie.append("&gt;");
                case '"' -> sortie.append("&quot;");
                case '\'' -> sortie.append("&#39;");
                default -> sortie.append(c);
            }
        }
        return sortie.toString();
    }
}
