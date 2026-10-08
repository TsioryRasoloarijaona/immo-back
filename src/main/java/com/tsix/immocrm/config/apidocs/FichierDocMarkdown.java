package com.tsix.immocrm.config.apidocs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un fichier {@code api-docs/**.md} découpé en sections : {@code #} (titre et texte de tête),
 * {@code ##} (méthode ou champ) et {@code ###} (sous-section d'une méthode).
 */
final class FichierDocMarkdown {

    record Section(String titre, String texte, Map<String, String> sousSections) {
    }

    /** Une ligne de titre : {@code niveau} = nombre de {@code #}, {@code nom} sans blancs autour. */
    record Titre(int niveau, String nom) {
    }

    final String chemin;
    String titre;
    String texteTete;
    final Map<String, Section> sections = new LinkedHashMap<>();
    final List<String> erreurs = new ArrayList<>();

    private FichierDocMarkdown(String chemin) {
        this.chemin = chemin;
    }

    static FichierDocMarkdown lire(String chemin, String contenu) {
        FichierDocMarkdown f = new FichierDocMarkdown(chemin);
        f.analyser(contenu.replace("\r\n", "\n").replace('\r', '\n'));
        return f;
    }

    private void analyser(String contenu) {
        List<String> tampon = new ArrayList<>();
        String section = null;
        String sousSection = null;
        String sectionTexte = null;
        Map<String, String> sous = null;
        boolean dansBlocCode = false;

        String[] lignes = contenu.split("\n", -1);
        for (int i = 0; i <= lignes.length; i++) {
            boolean fin = i == lignes.length;
            String ligne = fin ? "" : lignes[i];
            String nette = ligne.strip();
            if (!fin && (nette.startsWith("```") || nette.startsWith("~~~"))) dansBlocCode = !dansBlocCode;
            Titre m = (dansBlocCode && !fin) ? null : titre(ligne);
            if (!fin && m == null) {
                tampon.add(ligne);
                continue;
            }
            String texte = texte(tampon);
            tampon.clear();
            if (titre == null) {
                if (texte != null) erreurs.add("texte avant le titre « # »");
            } else if (section == null) {
                texteTete = texte;
            } else if (sousSection == null) {
                sectionTexte = texte;
            } else {
                sous.put(sousSection, texte);
            }
            if (fin || (m.niveau() <= 2 && section != null)) {
                if (section != null) sections.put(section, new Section(section, sectionTexte, sous));
                if (fin) break;
                section = null;
            }
            int niveau = m.niveau();
            String nom = m.nom();
            switch (niveau) {
                case 1 -> {
                    if (titre != null) erreurs.add("second titre « # " + nom + " »");
                    else titre = nom;
                }
                case 2 -> {
                    if (titre == null) erreurs.add("« ## " + nom + " » avant le titre « # »");
                    if (sections.containsKey(nom)) erreurs.add("section « ## " + nom + " » en double");
                    section = nom;
                    sectionTexte = null;
                    sous = new LinkedHashMap<>();
                    sousSection = null;
                }
                default -> {
                    if (section == null) {
                        erreurs.add("« ### " + nom + " » hors d'une section « ## »");
                        sousSection = null;
                    } else {
                        if (sous.containsKey(nom)) erreurs.add("« ### " + nom + " » en double dans « ## " + section + " »");
                        sousSection = nom;
                    }
                }
            }
            if (niveau <= 2) sousSection = null;
        }
        if (titre == null) erreurs.add("titre « # » manquant");
    }

    /**
     * {@code #} à {@code ###}, au moins un blanc, puis un nom non vide ; null sinon. {@code ####} et au-delà
     * restent du texte. Blancs = ceux de {@code \s} en regex (ASCII), pas {@code String.strip()}.
     */
    static Titre titre(String ligne) {
        int n = 0;
        while (n < ligne.length() && ligne.charAt(n) == '#') n++;
        if (n == 0 || n > 3 || n == ligne.length() || !blanc(ligne.charAt(n))) return null;
        int debut = n;
        int fin = ligne.length();
        while (debut < fin && blanc(ligne.charAt(debut))) debut++;
        while (fin > debut && blanc(ligne.charAt(fin - 1))) fin--;
        if (debut == fin) return null;
        String nom = ligne.substring(debut, fin);
        if (nom.chars().anyMatch(c -> c == '\u0085' || c == ' ' || c == ' ')) return null;
        return new Titre(n, nom);
    }

    private static boolean blanc(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /** Le texte d'une section : ses lignes, sans lignes vides de tête ni de queue ; null si vide. */
    private static String texte(List<String> lignes) {
        int debut = 0;
        int fin = lignes.size();
        while (debut < fin && lignes.get(debut).isBlank()) debut++;
        while (fin > debut && lignes.get(fin - 1).isBlank()) fin--;
        return debut == fin ? null : String.join("\n", lignes.subList(debut, fin));
    }
}
