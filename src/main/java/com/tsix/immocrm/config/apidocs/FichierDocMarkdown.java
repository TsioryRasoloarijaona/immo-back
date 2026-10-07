package com.tsix.immocrm.config.apidocs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un fichier {@code api-docs/**.md} découpé en sections : {@code #} (titre et texte de tête),
 * {@code ##} (méthode ou champ) et {@code ###} (sous-section d'une méthode).
 */
final class FichierDocMarkdown {

    record Section(String titre, String texte, Map<String, String> sousSections) {
    }

    // Seuls # à ### structurent le fichier ; #### et au-delà restent du texte.
    private static final Pattern TITRE = Pattern.compile("^(#{1,3})\\s+(.+?)\\s*$");

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
            Matcher m = (dansBlocCode && !fin) ? null : TITRE.matcher(ligne);
            if (!fin && (m == null || !m.matches())) {
                tampon.add(ligne);
                continue;
            }
            // Clôture de ce qui était en cours.
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
            if (fin || (m.group(1).length() <= 2 && section != null)) {
                if (section != null) sections.put(section, new Section(section, sectionTexte, sous));
                if (fin) break;
                section = null;
            }
            String niveau = m.group(1);
            String nom = m.group(2);
            switch (niveau.length()) {
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
            if (niveau.length() <= 2) sousSection = null;
        }
        if (titre == null) erreurs.add("titre « # » manquant");
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
