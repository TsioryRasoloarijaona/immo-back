package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Valide les paramètres reçus contre ceux d'un rapport (FEATURE-tdb-vente-rapports-socle.md §3.2, B4, B7) : chaque
 * refus est un 400. Rend les valeurs appliquées, défauts compris, dans l'ordre des paramètres.
 */
public final class ParametresRapport {

    static final int ANNEE_MIN = 1900;
    static final int ANNEE_MAX = 2200;
    private static final String DU = "du";
    private static final String AU = "au";

    private ParametresRapport() {
    }

    public static Map<String, List<String>> valider(List<ParametreRapport> parametres,
                                                    Map<String, List<String>> recus) {
        List<String> noms = parametres.stream().map(ParametreRapport::nom).toList();
        recus.keySet().stream().filter(n -> !noms.contains(n)).findFirst().ifPresent(n -> {
            throw new InvalidRequestException("Paramètre inconnu : %s. Paramètres acceptés : %s"
                    .formatted(n, String.join(", ", noms)));
        });
        Map<String, List<String>> appliques = new LinkedHashMap<>();
        for (ParametreRapport p : parametres) {
            List<String> valeurs = valeurs(p, recus.getOrDefault(p.nom(), List.of()));
            valeurs.forEach(v -> controler(p, v));
            if (!valeurs.isEmpty()) {
                appliques.put(p.nom(), valeurs);
            }
        }
        List<String> du = appliques.getOrDefault(DU, List.of());
        List<String> au = appliques.getOrDefault(AU, List.of());
        if (!du.isEmpty() && !au.isEmpty() && LocalDate.parse(au.get(0)).isBefore(LocalDate.parse(du.get(0)))) {
            throw new InvalidRequestException("« au » (%s) est avant « du » (%s).".formatted(au.get(0), du.get(0)));
        }
        return appliques;
    }

    private static List<String> valeurs(ParametreRapport p, List<String> recues) {
        List<String> valeurs = recues.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
        if (valeurs.size() > 1 && !p.multiple()) {
            throw new InvalidRequestException("Le paramètre « %s » n'accepte qu'une valeur.".formatted(p.nom()));
        }
        if (valeurs.isEmpty() && p.valeurParDefaut() != null) {
            return List.of(p.valeurParDefaut());
        }
        if (valeurs.isEmpty() && p.obligatoire()) {
            throw new InvalidRequestException("Le paramètre « %s » est obligatoire.".formatted(p.nom()));
        }
        return valeurs;
    }

    private static void controler(ParametreRapport p, String valeur) {
        switch (p.type()) {
            case DATE -> date(p, valeur);
            case ANNEE -> entier(p, valeur, ANNEE_MIN, ANNEE_MAX);
            case ENTIER -> entier(p, valeur, p.min(), p.max());
            case TRANCHES, COMMERCIAUX -> uuid(p, valeur);
            case CHOIX -> choix(p, valeur);
            case BOOLEEN -> booleen(p, valeur);
            case TEXTE -> {
                // texte libre
            }
        }
    }

    private static void booleen(ParametreRapport p, String valeur) {
        if (!"true".equalsIgnoreCase(valeur) && !"false".equalsIgnoreCase(valeur)) {
            throw invalide(p, valeur, "true ou false attendu");
        }
    }

    private static void date(ParametreRapport p, String valeur) {
        try {
            LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            throw invalide(p, valeur, "date aaaa-MM-jj attendue");
        }
    }

    private static void entier(ParametreRapport p, String valeur, Integer min, Integer max) {
        int n;
        try {
            n = Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw invalide(p, valeur, "entier attendu");
        }
        if ((min != null && n < min) || (max != null && n > max)) {
            throw invalide(p, valeur, "hors de [%s ; %s]".formatted(min, max));
        }
    }

    private static void uuid(ParametreRapport p, String valeur) {
        try {
            UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            throw invalide(p, valeur, "identifiant UUID attendu");
        }
    }

    private static void choix(ParametreRapport p, String valeur) {
        List<String> permis = p.options().stream().map(ParametreRapport.Option::valeur).toList();
        if (!permis.contains(valeur)) {
            throw invalide(p, valeur, "valeurs acceptées : " + String.join(", ", permis));
        }
    }

    private static InvalidRequestException invalide(ParametreRapport p, String valeur, String attendu) {
        return new InvalidRequestException("Valeur invalide pour « %s » : %s (%s).".formatted(p.nom(), valeur, attendu));
    }
}
