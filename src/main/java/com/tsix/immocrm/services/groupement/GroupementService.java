package com.tsix.immocrm.services.groupement;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.FonctionAgregat;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégation des listes repliées ({@code FEATURE-groupement-listes.md}). La {@code spec} reçue doit
 * être celle de la liste, périmètre compris : sinon un total montrerait des lignes interdites.
 * Aucun littéral numérique en SQL ({@code coalesce(sum, 0)} arrondit à l'unité) : NULL, −, ÷ en Java.
 */
@Service
public class GroupementService {

    private static final int ECHELLE_MOYENNE = 2;

    private final EntityManager em;

    public GroupementService(EntityManager em) {
        this.em = em;
    }

    /** @param totaux les mêmes agrégats sur <strong>toutes</strong> les lignes filtrées, pas la page */
    public record ResultatGroupement(Page<GroupeDto> groupes, Map<String, Object> totaux,
                                     long nombreTotal) {

        /** Ici et non dans {@code PageGroupesDto} : {@code dto} important {@code services} ferait un cycle. */
        public PageGroupesDto versPage() {
            return PageGroupesDto.of(groupes, totaux, nombreTotal);
        }
    }

    /**
     * @param spec        la Specification de la liste, périmètre commercial inclus
     * @param granularite non nulle uniquement si la colonne groupée est une date
     */
    @Transactional(readOnly = true)
    public <E> ResultatGroupement grouper(Class<E> racine, Specification<E> spec, Chemin cle,
                                          Granularite granularite, List<AgregatDemande> agregats,
                                          TriGroupe tri, boolean inclureSansValeur,
                                          Pageable pageable) {
        Page<GroupeDto> page = new PageImpl<>(lireGroupes(racine, spec, cle, granularite, agregats,
                new Affichage(tri, inclureSansValeur, pageable)), pageable, compterGroupes(racine, spec, cle, granularite, inclureSansValeur));
        return totalGeneral(racine, spec, cle, granularite, agregats, inclureSansValeur, page);
    }

    /** Les groupes seuls (effectifs), sans le comptage des groupes ni le total général de {@link #grouper}. */
    @Transactional(readOnly = true)
    public <E> List<GroupeDto> groupes(Class<E> racine, Specification<E> spec, Chemin cle, Granularite granularite,
                                       TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        return lireGroupes(racine, spec, cle, granularite, List.of(), new Affichage(tri, inclureSansValeur, pageable));
    }

    private record Affichage(TriGroupe tri, boolean inclureSansValeur, Pageable pageable) { }

    private <E> List<GroupeDto> lireGroupes(Class<E> racine, Specification<E> spec, Chemin cle,
                                            Granularite granularite, List<AgregatDemande> agregats,
                                            Affichage affichage) {
        TriGroupe tri = affichage.tri();
        boolean inclureSansValeur = affichage.inclureSansValeur();
        Pageable pageable = affichage.pageable();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<E> root = cq.from(racine);
        Jointures jointures = new Jointures(root);

        Expression<?> exprCle = expressionCle(cb, jointures, cle, granularite);
        List<Expression<?>> exprLibelle = new ArrayList<>();
        if (cle.typeLibelle() == Chemin.TypeLibelle.TIER) {
            for (String cheminLibelle : cle.cheminsLibelle()) {
                exprLibelle.add(jointures.chemin(cheminLibelle));
            }
        }

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(exprCle);
        selections.addAll(exprLibelle);
        Expression<Long> exprNombre = cb.count(root);
        selections.add(exprNombre);
        List<Agregation> plans = planifier(cb, jointures, agregats, selections);

        List<Expression<?>> groupBy = new ArrayList<>();
        groupBy.add(exprCle);
        groupBy.addAll(exprLibelle);

        cq.multiselect(selections).groupBy(groupBy);
        cq.where(predicats(cb, spec.toPredicate(root, cq, cb), exprCle, inclureSansValeur));
        Expression<?> exprTri = expressionTri(cb, cle, exprCle, exprLibelle, exprNombre, tri);
        cq.orderBy(tri == TriGroupe.NOMBRE ? cb.desc(exprTri) : cb.asc(exprTri));

        TypedQuery<Tuple> requete = em.createQuery(cq);
        requete.setFirstResult((int) pageable.getOffset());
        requete.setMaxResults(pageable.getPageSize());

        int indexNombre = 1 + exprLibelle.size();
        return requete.getResultList().stream()
                .map(t -> versGroupe(t, cle, granularite, indexNombre, plans))
                .toList();
    }

    /** {@code IS NOT NULL} sur l'expression de clé, pas la colonne brute : la requête de comptage compte des clés. */
    private static Predicate predicats(CriteriaBuilder cb, Predicate filtres, Expression<?> exprCle,
                                       boolean inclureSansValeur) {
        if (inclureSansValeur) {
            return filtres != null ? filtres : cb.conjunction();
        }
        Predicate renseignee = cb.isNotNull(exprCle);
        return filtres != null ? cb.and(filtres, renseignee) : renseignee;
    }

    /** {@code COUNT(DISTINCT)} ignore NULL : le second terme rajoute le groupe sans valeur s'il est peuplé. */
    private <E> long compterGroupes(Class<E> racine, Specification<E> spec, Chemin cle,
                                    Granularite granularite, boolean inclureSansValeur) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<E> root = cq.from(racine);
        Expression<?> exprCle = expressionCle(cb, new Jointures(root), cle, granularite);

        cq.multiselect(
                cb.countDistinct(exprCle),
                cb.sum(cb.<Integer>selectCase().when(cb.isNull(exprCle), 1).otherwise(0)));
        cq.where(predicats(cb, spec.toPredicate(root, cq, cb), exprCle, inclureSansValeur));

        Tuple t = em.createQuery(cq).getSingleResult();
        long distincts = t.get(0, Long.class);
        Number nuls = t.get(1, Number.class);
        return distincts + (nuls != null && nuls.longValue() > 0 ? 1 : 0);
    }

    /**
     * Requête à part : la page ne porte qu'une partie des groupes. Rejoue {@code inclureSansValeur},
     * sinon total et somme des groupes porteraient sur deux populations.
     */
    private <E> ResultatGroupement totalGeneral(Class<E> racine, Specification<E> spec, Chemin cle,
                                                Granularite granularite, List<AgregatDemande> agregats,
                                                boolean inclureSansValeur, Page<GroupeDto> page) {
        Totaux totaux = agregerTout(racine, agregats, (cb, jointures, filtres) -> predicats(cb, filtres,
                expressionCle(cb, jointures, cle, granularite), inclureSansValeur), spec);
        return new ResultatGroupement(page, totaux.valeurs(), totaux.nombre());
    }

    /** Agrégats et effectif de toutes les lignes filtrées, sans groupe : le même calcul que la bande des groupes. */
    public record Totaux(Map<String, Object> valeurs, long nombre) {
    }

    /**
     * Totaux d'une liste plate : exactement les agrégats de {@link #grouper} sur tout le filtre (exclusions de
     * {@link Chemin#sauf} comprises). La {@code spec} est celle de la liste, périmètre inclus.
     */
    @Transactional(readOnly = true)
    public <E> Totaux totaux(Class<E> racine, Specification<E> spec, List<AgregatDemande> agregats) {
        return agregerTout(racine, agregats, (cb, jointures, filtres) -> filtres != null ? filtres : cb.conjunction(),
                spec);
    }

    private interface Restriction {
        Predicate appliquer(CriteriaBuilder cb, Jointures jointures, Predicate filtres);
    }

    private <E> Totaux agregerTout(Class<E> racine, List<AgregatDemande> agregats, Restriction restriction,
                                   Specification<E> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<E> root = cq.from(racine);
        Jointures jointures = new Jointures(root);
        Predicate filtres = spec.toPredicate(root, cq, cb);
        Predicate where = restriction.appliquer(cb, jointures, filtres);

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(cb.count(root));
        List<Agregation> plans = planifier(cb, jointures, agregats, selections);

        cq.multiselect(selections);
        cq.where(where);

        Tuple t = em.createQuery(cq).getSingleResult();
        return new Totaux(valeurs(t, plans), ((Number) t.get(0)).longValue());
    }

    // ------------------------------------------------------------------
    // Agrégats
    // ------------------------------------------------------------------

    /** Indices explicites : un agrégat occupe une à trois colonnes du tuple (moyenne, différence). */
    private static final class Agregation {
        private final AgregatDemande demande;
        private int indexPrincipal = -1;
        private int indexSoustrait = -1;
        private int indexEffectif = -1;

        private Agregation(AgregatDemande demande) {
            this.demande = demande;
        }
    }

    /**
     * Différence = {@code SUM(a)} et {@code SUM(b)} séparés, soustraits en Java : {@code SUM(a - COALESCE(b, 0))}
     * exigerait un littéral SQL, et {@code SUM} ignorant NULL traite déjà un {@code b} absent comme zéro.
     */
    private static List<Agregation> planifier(CriteriaBuilder cb, Jointures jointures,
                                              List<AgregatDemande> agregats,
                                              List<Selection<?>> selections) {
        List<Agregation> plans = new ArrayList<>();
        for (AgregatDemande demande : agregats) {
            Agregation plan = new Agregation(demande);
            Chemin chemin = demande.chemin();
            Expression<?> principal = expressionAgregee(cb, jointures, chemin);

            switch (demande.fonction()) {
                case SUM, AVG -> {
                    principal = horsExclusion(cb, jointures, chemin, principal.as(BigDecimal.class));
                    plan.indexPrincipal = selections.size();
                    selections.add(cb.sum(principal.as(BigDecimal.class)));
                    if (chemin.cheminSoustrait() != null) {
                        plan.indexSoustrait = selections.size();
                        selections.add(cb.sum(horsExclusion(cb, jointures, chemin,
                                jointures.chemin(chemin.cheminSoustrait()).as(BigDecimal.class))));
                    }
                    if (demande.fonction() == FonctionAgregat.AVG) {
                        // Effectif des lignes renseignées, pas du groupe : les cases vides
                        // tireraient la moyenne vers le bas.
                        plan.indexEffectif = selections.size();
                        selections.add(cb.count(principal));
                    }
                }
                case MIN, MAX -> {
                    plan.indexPrincipal = selections.size();
                    selections.add(extremum(cb, principal, demande.fonction() == FonctionAgregat.MAX));
                }
                case COUNT -> {
                    plan.indexPrincipal = selections.size();
                    selections.add(cb.count(principal));
                }
            }
            plans.add(plan);
        }
        return plans;
    }

    /**
     * {@code CASE WHEN <ligne non exclue> THEN valeur END}, sans {@code ELSE} : NULL hors de la somme et de
     * l'effectif d'une moyenne, sans littéral numérique ({@link Chemin#sauf}).
     */
    private static Expression<BigDecimal> horsExclusion(CriteriaBuilder cb, Jointures jointures, Chemin chemin,
                                                        Expression<BigDecimal> valeur) {
        Chemin.Exclusion exclusion = chemin.exclusion();
        if (exclusion == null) {
            return valeur;
        }
        Path<?> discriminant = jointures.chemin(exclusion.chemin());
        Predicate gardee = cb.or(cb.isNull(discriminant), cb.notEqual(discriminant, exclusion.valeur()));
        return cb.<BigDecimal>selectCase().when(gardee, valeur);
    }

    /**
     * Comme {@link #expressionCle} sans granularité (min/max réels d'une date). Un repli garde sa
     * cascade, sinon {@code COUNT} ne compterait que la première source.
     */
    private static Expression<?> expressionAgregee(CriteriaBuilder cb, Jointures jointures,
                                                   Chemin chemin) {
        if (chemin.typeLibelle() == Chemin.TypeLibelle.REPLI) {
            return expressionRepli(cb, jointures, chemin);
        }
        return jointures.chemin(chemin.cheminCle());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Expression<?> extremum(CriteriaBuilder cb, Expression<?> expression, boolean max) {
        return max ? cb.greatest((Expression) expression) : cb.least((Expression) expression);
    }

    private static Map<String, Object> valeurs(Tuple t, List<Agregation> plans) {
        Map<String, Object> resultats = new LinkedHashMap<>();
        for (Agregation plan : plans) {
            resultats.put(plan.demande.jeton(), valeur(t, plan));
        }
        return resultats;
    }

    private static Object valeur(Tuple t, Agregation plan) {
        Object brut = t.get(plan.indexPrincipal);
        return switch (plan.demande.fonction()) {
            // Extremum de rien = null, pas 0 ; un total vide, lui, vaut zéro.
            case MIN, MAX -> brut;
            case COUNT -> brut == null ? 0L : ((Number) brut).longValue();
            case SUM -> total(t, plan);
            case AVG -> moyenne(t, plan);
        };
    }

    /** {@code SUM} de lignes toutes nulles rend NULL : zéro substitué ici, pas en SQL. */
    private static BigDecimal total(Tuple t, Agregation plan) {
        BigDecimal somme = decimal(t.get(plan.indexPrincipal));
        if (plan.indexSoustrait >= 0) {
            somme = somme.subtract(decimal(t.get(plan.indexSoustrait)));
        }
        return somme;
    }

    /** Jamais {@code cb.avg()}, qui rend un {@code double} : deux agrégats exacts divisés en {@code BigDecimal}. */
    private static BigDecimal moyenne(Tuple t, Agregation plan) {
        Number effectif = (Number) t.get(plan.indexEffectif);
        if (effectif == null || effectif.longValue() == 0) {
            return null;
        }
        return total(t, plan)
                .divide(BigDecimal.valueOf(effectif.longValue()), ECHELLE_MOYENNE, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object valeur) {
        return switch (valeur) {
            case null -> BigDecimal.ZERO;
            case BigDecimal bd -> bd;
            case Number n -> new BigDecimal(n.toString());
            default -> BigDecimal.ZERO;
        };
    }

    // ------------------------------------------------------------------
    // Expression de la clé de groupe
    // ------------------------------------------------------------------

    /**
     * Une seule expression, pour que {@code COUNT(DISTINCT)} et {@code ORDER BY} restent exprimables
     * (les noms d'un tiers dépendent de son id). Date ⇒ entier de période ({@code 202507}) : pas de
     * {@code FORMAT()} propre à SQL Server, et l'ordre numérique est chronologique.
     */
    private static Expression<?> expressionCle(CriteriaBuilder cb, Jointures jointures,
                                               Chemin cle, Granularite granularite) {
        Objects.requireNonNull(cle, "cle");
        if (cle.typeLibelle() == Chemin.TypeLibelle.REPLI) {
            return expressionRepli(cb, jointures, cle);
        }
        Path<?> path = jointures.chemin(cle.cheminCle());
        if (granularite == null) {
            return path;
        }
        Expression<Integer> annee = cb.function("year", Integer.class, path);
        Expression<Integer> mois = cb.function("month", Integer.class, path);
        return switch (granularite) {
            case ANNEE -> annee;
            case MOIS -> cb.sum(cb.prod(annee, 100), mois);
            case JOUR -> cb.sum(
                    cb.sum(cb.prod(annee, 10000), cb.prod(mois, 100)),
                    cb.function("day", Integer.class, path));
        };
    }

    /**
     * {@code NULLIF(TRIM(x), '')} reproduit le {@code StringUtils.hasText} du mappeur : un {@code COALESCE}
     * nu garderait les blancs et l'en-tête divergerait de la ligne ({@link Chemin#repli}).
     */
    private static Expression<String> expressionRepli(CriteriaBuilder cb, Jointures jointures,
                                                      Chemin cle) {
        Expression<String> cascade = null;
        for (String chemin : cle.cheminsLibelle()) {
            Expression<String> terme = cb.nullif(
                    cb.trim(jointures.chemin(chemin).as(String.class)), "");
            cascade = cascade == null ? terme : cb.coalesce(cascade, terme);
        }
        return cascade;
    }

    private static Expression<?> expressionTri(CriteriaBuilder cb, Chemin cle, Expression<?> exprCle,
                                               List<Expression<?>> exprLibelle,
                                               Expression<Long> exprNombre, TriGroupe tri) {
        if (tri == TriGroupe.NOMBRE) {
            return exprNombre;
        }
        if (cle.typeLibelle() != Chemin.TypeLibelle.TIER) {
            return exprCle;
        }
        // Priorité de TierNomFormatter rejouée, pour trier sur le libellé affiché et non sur l'id.
        @SuppressWarnings("unchecked")
        Expression<String> raisonSociale = (Expression<String>) exprLibelle.get(0);
        @SuppressWarnings("unchecked")
        Expression<String> description = (Expression<String>) exprLibelle.get(1);
        @SuppressWarnings("unchecked")
        Expression<String> nomFamille = (Expression<String>) exprLibelle.get(3);
        return cb.coalesce(cb.coalesce(raisonSociale, description), nomFamille);
    }

    // ------------------------------------------------------------------
    // Restitution
    // ------------------------------------------------------------------

    private static GroupeDto versGroupe(Tuple t, Chemin cle, Granularite granularite,
                                        int indexNombre, List<Agregation> plans) {
        Object valeurCle = t.get(0);
        String libelle;
        if (granularite != null) {
            libelle = libellePeriode(valeurCle, granularite);
        } else if (cle.typeLibelle() == Chemin.TypeLibelle.TIER) {
            libelle = TierNomFormatter.nom(
                    (String) t.get(1), (String) t.get(2), (String) t.get(3), (String) t.get(4));
        } else {
            libelle = valeurCle != null ? String.valueOf(valeurCle) : null;
        }

        long nombre = ((Number) t.get(indexNombre)).longValue();
        return new GroupeDto(valeurCle, libelle, nombre, valeurs(t, plans));
    }

    /** {@code 202507} → « juillet 2025 ». */
    private static String libellePeriode(Object cle, Granularite granularite) {
        if (cle == null) {
            return null;
        }
        int valeur = ((Number) cle).intValue();
        return switch (granularite) {
            case ANNEE -> String.valueOf(valeur);
            case MOIS -> nomDuMois(valeur % 100) + " " + (valeur / 100);
            case JOUR -> String.format("%02d/%02d/%04d",
                    valeur % 100, (valeur / 100) % 100, valeur / 10000);
        };
    }

    private static String nomDuMois(int mois) {
        return Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }

    // ------------------------------------------------------------------
    // Dépliage d'un groupe
    // ------------------------------------------------------------------

    /**
     * {@code valeur} = la {@code cle} de {@code /groupes} réémise telle quelle, reconvertie d'après le
     * type de la colonne. Absente ou vide ⇒ le groupe sans valeur, pas une erreur.
     */
    public static Predicate predicatDeGroupe(Root<?> root, CriteriaBuilder cb, Chemin cle,
                                             Granularite granularite, String valeur) {
        Expression<?> exprCle = expressionCle(cb, new Jointures(root), cle, granularite);
        if (valeur == null || valeur.isBlank()) {
            return cb.isNull(exprCle);
        }
        return cb.equal(exprCle, convertir(exprCle.getJavaType(), valeur.trim()));
    }

    private static Object convertir(Class<?> type, String valeur) {
        if (type == UUID.class) {
            return UUID.fromString(valeur);
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(valeur);
        }
        if (type == Integer.class || type == int.class) {
            return Integer.valueOf(valeur);
        }
        if (type == Long.class || type == long.class) {
            return Long.valueOf(valeur);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(valeur);
        }
        return valeur;
    }

    // ------------------------------------------------------------------

    /**
     * Jointures LEFT seulement (une navigation implicite ferait des INNER JOIN qui effaceraient des
     * groupes) et mutualisées (sinon deux chemins au préfixe commun joindraient deux fois).
     */
    private static final class Jointures {

        private final Root<?> root;
        private final Map<String, From<?, ?>> cache = new HashMap<>();

        private Jointures(Root<?> root) {
            this.root = root;
        }

        private Path<?> chemin(String chemin) {
            String[] segments = chemin.split("\\.");
            From<?, ?> courant = root;
            StringBuilder prefixe = new StringBuilder();
            for (int i = 0; i < segments.length - 1; i++) {
                if (i > 0) {
                    prefixe.append('.');
                }
                prefixe.append(segments[i]);
                From<?, ?> deja = cache.get(prefixe.toString());
                if (deja == null) {
                    deja = courant.join(segments[i], JoinType.LEFT);
                    cache.put(prefixe.toString(), deja);
                }
                courant = deja;
            }
            return courant.get(segments[segments.length - 1]);
        }
    }
}
