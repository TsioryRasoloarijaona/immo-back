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

    // Équivaut à la regex <(script|style)\b[^>]*>.*?</\1\s*>|<!--.*?--> sans son retour arrière quadratique sur une
    // ouverture jamais fermée : chaque fin est cherchée une fois, puis réutilisée (voir Prochaine).
    private static final Pattern OUVERTURE = Pattern.compile("<(?:(script|style)\\b|!--)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHEVRON_FERMANT = Pattern.compile(">");
    private static final Pattern FIN_COMMENTAIRE = Pattern.compile("-->");
    private static final Pattern FIN_SCRIPT = Pattern.compile("</script\\s*+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIN_STYLE = Pattern.compile("</style\\s*+>", Pattern.CASE_INSENSITIVE);
    // Quantificateurs possessifs : sans eux, nom et attributs se disputent les mêmes caractères (retour arrière
    // quadratique sur une balise jamais fermée, entrée utilisateur).
    private static final Pattern BALISE = Pattern.compile("<\\s*+(/?)\\s*+([a-zA-Z][a-zA-Z0-9]*+)[^<>]*+>");
    private static final Pattern TOUTE_BALISE = Pattern.compile("<[^<>]*>");

    private HtmlNote() {
    }

    public static String assainir(String html) {
        if (html == null) {
            return "";
        }
        String sansBlocs = retirerBlocsDangereux(html);
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

    /** Retire scripts, styles (avec leur contenu) et commentaires fermés ; une ouverture sans fin reste du texte. */
    private static String retirerBlocsDangereux(String html) {
        Matcher ouverture = OUVERTURE.matcher(html);
        Prochaine chevron = new Prochaine(CHEVRON_FERMANT, html);
        Prochaine finCommentaire = new Prochaine(FIN_COMMENTAIRE, html);
        Prochaine finScript = new Prochaine(FIN_SCRIPT, html);
        Prochaine finStyle = new Prochaine(FIN_STYLE, html);
        StringBuilder sortie = new StringBuilder(html.length());
        int copie = 0;
        int depuis = 0;
        while (ouverture.find(depuis)) {
            int fin;
            String nom = ouverture.group(1);
            if (nom == null) {
                fin = finCommentaire.finDepuis(ouverture.end());
            } else {
                int finOuvrante = chevron.finDepuis(ouverture.end());
                Prochaine fermeture = "script".equalsIgnoreCase(nom) ? finScript : finStyle;
                fin = finOuvrante < 0 ? -1 : fermeture.finDepuis(finOuvrante);
            }
            if (fin < 0) {
                depuis = ouverture.end();
            } else {
                sortie.append(html, copie, ouverture.start());
                copie = fin;
                depuis = fin;
            }
        }
        return sortie.append(html, copie, html.length()).toString();
    }

    /**
     * Première occurrence d'un motif à partir d'une position qui ne décroît jamais : le résultat déjà trouvé (ou son
     * absence) resservant tant qu'il reste devant, le texte n'est parcouru qu'une fois au total.
     */
    private static final class Prochaine {
        private static final int NON_CHERCHE = -2;
        private final Matcher matcher;
        private int debut = NON_CHERCHE;
        private int fin;

        Prochaine(Pattern motif, String texte) {
            this.matcher = motif.matcher(texte);
        }

        /** Fin de la première occurrence commençant à {@code depuis} ou après, -1 s'il n'y en a pas. */
        int finDepuis(int depuis) {
            if (debut == -1) {
                return -1;
            }
            if (debut < depuis) {
                if (matcher.find(depuis)) {
                    debut = matcher.start();
                    fin = matcher.end();
                } else {
                    debut = -1;
                    return -1;
                }
            }
            return fin;
        }
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
