package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.EtatSession;
import com.tsix.immocrm.entities.adm.AdmWebSessions;
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

/** Filtres de la liste des sessions ; tous facultatifs et cumulés. */
public record FiltresSessions(
        @Parameter(description = "Premier jour d'ouverture inclus, ISO ; absent = pas de borne basse.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @Parameter(description = "Dernier jour d'ouverture inclus, ISO ; absent = pas de borne haute.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @Parameter(description = "Utilisateur exact, valeur de GET /api/administration/sessions/utilisateurs ; vide = tous.")
        String utilisateur,
        @Parameter(description = "Sous-chaîne de l'adresse IP, prise à la lettre ; vide ou blanc = pas de filtre.")
        String ip,
        @Parameter(description = "Vrai = sessions ouvertes, faux = toutes les autres ; absent = toutes.")
        Boolean ouverte) {

    private static final String OUVERTURE = "startDate";
    private static final String ETAT = "state";

    public Specification<AdmWebSessions> specification() {
        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            throw new InvalidRequestException("dateDebut doit précéder ou égaler dateFin.");
        }
        return (session, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (dateDebut != null) {
                conditions.add(cb.greaterThanOrEqualTo(session.<LocalDateTime>get(OUVERTURE), dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                conditions.add(cb.lessThan(session.<LocalDateTime>get(OUVERTURE), dateFin.plusDays(1).atStartOfDay()));
            }
            if (StringUtils.hasText(utilisateur)) {
                conditions.add(cb.equal(session.get("userDesc"), utilisateur));
            }
            if (StringUtils.hasText(ip)) {
                conditions.add(cb.like(session.get("ipAddress"), HistoriqueService.motif(ip)));
            }
            if (ouverte != null) {
                int etatOuvert = EtatSession.OUVERTE.state();
                conditions.add(ouverte ? cb.equal(session.get(ETAT), etatOuvert) : cb.notEqual(session.get(ETAT), etatOuvert));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
}
