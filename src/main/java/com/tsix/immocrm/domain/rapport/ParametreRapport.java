package com.tsix.immocrm.domain.rapport;

import java.time.LocalDate;
import java.util.List;

/**
 * Paramètre accepté par un rapport (FEATURE-tdb-vente-rapports-socle.md §3.1, B4, B7). {@code nom} = clé de requête ;
 * {@code valeurParDefaut} en texte ({@code aaaa-MM-jj} pour une date) ; {@code min}/{@code max} : bornes d'un ENTIER.
 */
public record ParametreRapport(String nom, String libelle, TypeParametre type, boolean obligatoire, boolean multiple,
                               String valeurParDefaut, List<Option> options, Integer min, Integer max) {

    public static final String TRANCHE_ID = "trancheId";

    public ParametreRapport {
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** Valeur proposée d'un {@code CHOIX} (ou, remplie par le socle, d'un {@code TRANCHES}/{@code COMMERCIAUX}). */
    public record Option(String valeur, String libelle) {
    }

    /** {@code trancheId}, multiple, facultatif : absent = toutes les tranches visibles. */
    public static ParametreRapport tranches() {
        return new ParametreRapport(TRANCHE_ID, "Tranches", TypeParametre.TRANCHES, false, true, null, List.of(),
                null, null);
    }

    public static ParametreRapport date(String nom, String libelle, LocalDate parDefaut) {
        return new ParametreRapport(nom, libelle, TypeParametre.DATE, false, false,
                parDefaut == null ? null : parDefaut.toString(), List.of(), null, null);
    }
}
