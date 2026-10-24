package com.tsix.immocrm.services.rapport.vente.commercial;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/** Paramètres et colonnes de période communs aux rapports commerciaux (FEATURE-tdb-vente-rapports-commercial.md §2). */
public final class PeriodesRapport {

    public static final String ANNEE = "annee";
    public static final String DU = "du";
    public static final String AU = "au";

    private PeriodesRapport() {
    }

    /** {@code annee}, défaut = année de l'horloge. */
    public static ParametreRapport parametreAnnee(Clock clock) {
        return new ParametreRapport(ANNEE, "Année", TypeParametre.ANNEE, false, false,
                String.valueOf(LocalDate.now(clock).getYear()), List.of(), null, null);
    }

    /** Libellé français du mois (« Janvier »), comme {@code AdmCalanderMonth}. */
    public static String mois(int mois) {
        String nom = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return nom.substring(0, 1).toUpperCase(Locale.FRENCH) + nom.substring(1);
    }

    /** Nom de colonne du mois : {@code m01}…{@code m12}. */
    public static String colonneMois(int mois) {
        return "m%02d".formatted(mois);
    }

    /** Les douze colonnes {@code m01}…{@code m12}, libellées « Janvier »… */
    public static List<ColonneRapport> colonnesMois() {
        return IntStream.rangeClosed(1, 12).mapToObj(m -> ColonneRapport.effectif(colonneMois(m), mois(m), null))
                .toList();
    }
}
