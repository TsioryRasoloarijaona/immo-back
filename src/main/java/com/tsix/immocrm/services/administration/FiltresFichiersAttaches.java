package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.administration.CategorieFichierAttache;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Filtres de la liste des fichiers attachés ; tous facultatifs, cumulés ; liste vide = pas de filtre. */
public record FiltresFichiersAttaches(
        @Parameter(description = "Sous-chaîne de la description, de la catégorie, du groupe ou du type, sans casse.")
        String recherche,
        @Parameter(description = "Types cochés (AdmAttachedFileType.PKey, voir /filtres) ; vide = tous.")
        List<UUID> typeIds,
        @Parameter(description = "Catégories normalisées cochées (codes de /filtres) ; vide = toutes.")
        List<CategorieFichierAttache> categories,
        @Parameter(description = "Groupes cochés (valeurs de /filtres, ou NON_DEFINI = groupe vide) ; vide = tous.")
        List<String> groupes,
        @Parameter(description = "Vrai = avec fichier stocké vivant, faux = sans ; absent = toutes.")
        Boolean avecFichier,
        @Parameter(description = "Premier jour de création inclus, ISO ; absent = pas de borne.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Dernier jour de création inclus, ISO ; absent = pas de borne.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Premier jour de modification inclus, ISO ; absent = pas de borne.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modifieDu,
        @Parameter(description = "Dernier jour de modification inclus, ISO ; absent = pas de borne.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modifieAu) {

    public static final String NON_DEFINI = "NON_DEFINI";
    private static final String CATEGORIE = "category";
    private static final String GROUPE = "groupe";

    public FiltresFichiersAttaches {
        typeIds = typeIds == null ? List.of() : List.copyOf(typeIds);
        categories = categories == null ? List.of() : List.copyOf(categories);
        groupes = groupes == null ? List.of() : List.copyOf(groupes);
    }

    public Specification<AdmAttachedFileSummary> specification() {
        verifier(du, au, "du doit précéder ou égaler au.");
        verifier(modifieDu, modifieAu, "modifieDu doit précéder ou égaler modifieAu.");
        return (piece, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            periode(conditions, cb, piece.get("userCreateDateTime"), du, au);
            periode(conditions, cb, piece.get("userUpdateDateTime"), modifieDu, modifieAu);
            if (!typeIds.isEmpty()) {
                conditions.add(piece.get("type").get("id").in(typeIds));
            }
            if (!categories.isEmpty()) {
                conditions.add(cb.or(categories.stream().map(c -> categorie(piece, cb, c)).toArray(Predicate[]::new)));
            }
            if (!groupes.isEmpty()) {
                conditions.add(groupe(piece, cb));
            }
            if (avecFichier != null) {
                Predicate vivant = cb.exists(fichierVivant(piece, requete, cb));
                conditions.add(avecFichier ? vivant : cb.not(vivant));
            }
            if (StringUtils.hasText(recherche)) {
                conditions.add(recherche(piece, cb));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    private static void verifier(LocalDate debut, LocalDate fin, String message) {
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new InvalidRequestException(message);
        }
    }

    private static void periode(List<Predicate> conditions, CriteriaBuilder cb, Path<LocalDateTime> date,
                                LocalDate debut, LocalDate fin) {
        if (debut != null) {
            conditions.add(cb.greaterThanOrEqualTo(date, debut.atStartOfDay()));
        }
        if (fin != null) {
            conditions.add(cb.lessThan(date, fin.plusDays(1).atStartOfDay()));
        }
    }

    private static Predicate categorie(Root<AdmAttachedFileSummary> piece, CriteriaBuilder cb,
                                       CategorieFichierAttache categorie) {
        Path<String> category = piece.get(CATEGORIE);
        Predicate libelleDossier = cb.like(category, CategorieFichierAttache.PREFIXE_DOSSIER + "%");
        return switch (categorie) {
            case AUTRE -> cb.or(cb.isNull(category),
                    cb.not(cb.or(category.in(CategorieFichierAttache.toutesLesClasses()), libelleDossier)));
            case DOSSIER -> cb.or(category.in(categorie.classes()), libelleDossier);
            default -> category.in(categorie.classes());
        };
    }

    private Predicate groupe(Root<AdmAttachedFileSummary> piece, CriteriaBuilder cb) {
        Expression<String> groupe = cb.trim(piece.get(GROUPE));
        List<String> valeurs = groupes.stream().filter(g -> !NON_DEFINI.equals(g)).toList();
        List<Predicate> choix = new ArrayList<>();
        if (!valeurs.isEmpty()) {
            choix.add(groupe.in(valeurs));
        }
        if (groupes.contains(NON_DEFINI)) {
            choix.add(cb.or(cb.isNull(piece.get(GROUPE)), cb.equal(groupe, "")));
        }
        return cb.or(choix.toArray(Predicate[]::new));
    }

    private static Subquery<Integer> fichierVivant(Root<AdmAttachedFileSummary> piece, CommonAbstractCriteria requete,
                                                   CriteriaBuilder cb) {
        Subquery<Integer> sous = requete.subquery(Integer.class);
        Root<AppFileObject> objet = sous.from(AppFileObject.class);
        return sous.select(cb.literal(1)).where(cb.equal(objet.get("admAttachedFileId"), piece.get("id")),
                cb.isNull(objet.get("deletedAt")));
    }

    private Predicate recherche(Root<AdmAttachedFileSummary> piece, CriteriaBuilder cb) {
        String motif = Objects.requireNonNull(HistoriqueService.motif(recherche.strip())).toLowerCase(Locale.ROOT);
        return cb.or(cb.like(cb.lower(piece.get("description")), motif),
                cb.like(cb.lower(piece.get("categoryDescription")), motif),
                cb.like(cb.lower(piece.get(GROUPE)), motif),
                cb.like(cb.lower(piece.join("type", JoinType.LEFT).get("description")), motif));
    }
}
