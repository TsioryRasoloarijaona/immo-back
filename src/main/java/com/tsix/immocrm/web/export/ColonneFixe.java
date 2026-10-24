package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.ColonneLibre;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Colonne d'export d'une liste sans catalogue serveur : celles de son tableau front, dans le même ordre. */
public record ColonneFixe(String nom, String libelle, TypeExport typeExport, boolean parDefaut) implements ColonneLibre {

    /** Colonnes de contact masquées par défaut (programmes, entités). */
    public static final List<ColonneFixe> CONTACTS = List.of(cachee("telephone", "Tél."), cachee("mobile", "Mobile"),
            cachee("fax", "Fax"), cachee("email", "Email"), cachee("siteWeb", "Site web"));
    /** Piste d'audit masquée par défaut. */
    public static final List<ColonneFixe> AUDIT = List.of(cachee("creePar", "Créé par"),
            cachee("creeLe", "Créé le", TypeExport.DATE), cachee("modifiePar", "Modifié par"),
            cachee("modifieLe", "Modifié le", TypeExport.DATE));

    public static ColonneFixe texte(String nom, String libelle) {
        return de(nom, libelle, TypeExport.TEXTE);
    }

    public static ColonneFixe de(String nom, String libelle, TypeExport type) {
        return new ColonneFixe(nom, libelle, type, true);
    }

    /** Colonne masquée par défaut dans le tableau (sélecteur de colonnes). */
    public static ColonneFixe cachee(String nom, String libelle) {
        return cachee(nom, libelle, TypeExport.TEXTE);
    }

    public static ColonneFixe cachee(String nom, String libelle, TypeExport type) {
        return new ColonneFixe(nom, libelle, type, false);
    }

    @SafeVarargs
    public static List<ColonneFixe> suite(List<ColonneFixe>... parties) {
        return Arrays.stream(parties).flatMap(List::stream).toList();
    }

    /** Colonnes demandées dans l'ordre reçu ; absent = celles affichées par défaut ; nom inconnu = 400. */
    public static List<ColonneFixe> choisir(List<String> demandees, List<ColonneFixe> catalogue) {
        if (demandees == null || demandees.isEmpty()) {
            return catalogue.stream().filter(ColonneFixe::parDefaut).toList();
        }
        List<ColonneFixe> ordre = new ArrayList<>();
        for (String liste : demandees) {
            for (String nom : liste.split(",")) {
                ColonneFixe colonne = catalogue.stream().filter(c -> c.nom().equalsIgnoreCase(nom.trim())).findFirst()
                        .orElseThrow(() -> new InvalidRequestException("Colonne inconnue : " + nom.trim() + " (valides : "
                                + String.join(", ", catalogue.stream().map(ColonneFixe::nom).toList()) + ")"));
                if (!ordre.contains(colonne)) {
                    ordre.add(colonne);
                }
            }
        }
        return ordre;
    }
}
