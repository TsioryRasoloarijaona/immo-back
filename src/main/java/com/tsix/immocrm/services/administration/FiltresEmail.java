package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.entities.adm.AdmEMailHistory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtres de la liste des e-mails ; tous facultatifs et cumulés. */
public record FiltresEmail(
        @Parameter(description = "Premier jour inclus, ISO ; absent = pas de borne basse.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @Parameter(description = "Dernier jour inclus, ISO ; absent = pas de borne haute.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @Parameter(description = "Sous-chaîne littérale, sans tenir compte de la casse ; vide ou blanc = pas de filtre.")
        String texte,
        @Parameter(description = "Utilisateur exact, valeur de GET /api/administration/emails/utilisateurs.")
        String utilisateur,
        @Parameter(description = "Vrai = envoyés, faux = en échec ; absent = tous.")
        Boolean envoye) {

    private static final String DATE = "mailDate";
    private static final List<String> CHAMPS_CHERCHES = List.of("admUserDescription", "subject", "displayFromName",
            "emailsTo", "emailFrom", "objectLabel");

    public Specification<AdmEMailHistory> specification() {
        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            throw new InvalidRequestException("dateDebut doit précéder ou égaler dateFin.");
        }
        return (email, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (dateDebut != null) {
                conditions.add(cb.greaterThanOrEqualTo(email.<LocalDateTime>get(DATE), dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                conditions.add(cb.lessThan(email.<LocalDateTime>get(DATE), dateFin.plusDays(1).atStartOfDay()));
            }
            if (StringUtils.hasText(utilisateur)) {
                conditions.add(cb.equal(email.get("admUserDescription"), utilisateur));
            }
            if (envoye != null) {
                conditions.add(cb.equal(email.get("emailsSent"), envoye));
            }
            if (StringUtils.hasText(texte)) {
                String motif = motif();
                conditions.add(cb.or(CHAMPS_CHERCHES.stream()
                        .map(champ -> cb.like(email.get(champ), motif)).toArray(Predicate[]::new)));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** Texte saisi pris à la lettre dans le {@code LIKE} ; la casse suit la collation de la colonne. */
    private String motif() {
        return "%" + texte.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
    }
}
