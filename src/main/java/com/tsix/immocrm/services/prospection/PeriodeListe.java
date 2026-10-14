package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.DateRenseignee;
import com.tsix.immocrm.domain.PeriodeSur;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Période d'une liste de prospections, bornes d'OMRAN ({@code ProspectionList.GetList}) : une date
 * sans heure vaut le jour entier ({@code >= début}, {@code < fin + 1 jour}) ; une date-heure garde
 * la borne haute incluse. {@code renseignee} ignore les bornes. Voir FEATURE-prospection-filtres.md §3.
 */
record PeriodeListe(LocalDateTime debut, LocalDateTime fin, boolean finExclue, PeriodeSur sur,
                    DateRenseignee renseignee) {

    static final PeriodeListe AUCUNE = new PeriodeListe(null, null, false, PeriodeSur.DATE, null);

    static PeriodeListe de(String debut, String fin, PeriodeSur sur, DateRenseignee renseignee) {
        LocalDateTime borneHaute = null;
        boolean exclue = false;
        if (StringUtils.hasText(fin)) {
            String f = fin.trim();
            exclue = estUneDate(f);
            borneHaute = exclue ? lireDate(f, "dateFin").plusDays(1).atStartOfDay() : lireDateHeure(f, "dateFin");
        }
        return new PeriodeListe(borneBasse(debut), borneHaute, exclue, sur != null ? sur : PeriodeSur.DATE,
                renseignee);
    }

    private static LocalDateTime borneBasse(String debut) {
        if (!StringUtils.hasText(debut)) {
            return null;
        }
        String d = debut.trim();
        return estUneDate(d) ? lireDate(d, "dateDebut").atStartOfDay() : lireDateHeure(d, "dateDebut");
    }

    private static boolean estUneDate(String valeur) {
        return valeur.indexOf('T') < 0;
    }

    private static LocalDate lireDate(String valeur, String parametre) {
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(formatInvalide(parametre, valeur));
        }
    }

    private static LocalDateTime lireDateHeure(String valeur, String parametre) {
        try {
            // ISO_DATE_TIME : millisecondes et décalage (« Z », « +01:00 ») acceptés, heure locale gardée.
            return LocalDateTime.parse(valeur, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(formatInvalide(parametre, valeur));
        }
    }

    private static String formatInvalide(String parametre, String valeur) {
        return "%s invalide : « %s » (attendu AAAA-MM-JJ ou AAAA-MM-JJTHH:MM:SS)".formatted(parametre, valeur);
    }

    /** Prédicats à ajouter ; vide si aucune borne ni condition de présence. */
    List<Predicate> predicats(Root<ComProspection> root, CriteriaBuilder cb) {
        Path<LocalDateTime> date = root.get(sur.attribut());
        if (renseignee != null) {
            return List.of(renseignee == DateRenseignee.OUI ? cb.isNotNull(date) : cb.isNull(date));
        }
        List<Predicate> predicats = new ArrayList<>();
        if (debut != null) {
            predicats.add(cb.greaterThanOrEqualTo(date, debut));
        }
        if (fin != null) {
            predicats.add(finExclue ? cb.lessThan(date, fin) : cb.lessThanOrEqualTo(date, fin));
        }
        return predicats;
    }
}
