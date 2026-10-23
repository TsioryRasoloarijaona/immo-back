package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Colonnes, paramètres et lignes communs aux rapports du stock (FEATURE-patrimoine-rapports.md). */
final class ColonnesStock {

    static final String TRANCHE = "tranche";
    static final String TRANCHES = "tranches";
    static final Comparator<String> TEXTE = Comparator.nullsFirst(Comparator.naturalOrder());
    static final ColonneRapport COLONNE_TRANCHE = ColonneRapport.texte(TRANCHE, "Tranche");

    private ColonnesStock() {
    }

    /** Surface : nombre à 2 décimales, sommé. */
    static ColonneRapport surface(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, null, FonctionTotal.SOMME, true, 2);
    }

    /** Nombre entier sans total (compteurs d'une synthèse, cumul). */
    static ColonneRapport compteur(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, true, 0);
    }

    /** Texte masqué par défaut. */
    static ColonneRapport texteMasque(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.TEXTE, null, FonctionTotal.AUCUNE, false, 0);
    }

    /** {@code CHOIX} multiple facultatif : absent = aucun filtre. */
    static ParametreRapport choixMultiple(String nom, String libelle, List<ParametreRapport.Option> options) {
        return new ParametreRapport(nom, libelle, TypeParametre.CHOIX, false, true, null, options, null, null);
    }

    /** Ligne de section à partir de paires clé, valeur (valeurs nulles permises). */
    static Map<String, Object> ligne(Object... clesValeurs) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        for (int i = 0; i + 1 < clesValeurs.length; i += 2) {
            ligne.put((String) clesValeurs[i], clesValeurs[i + 1]);
        }
        return ligne;
    }
}
