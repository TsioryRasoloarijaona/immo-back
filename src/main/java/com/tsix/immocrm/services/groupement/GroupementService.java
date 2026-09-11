package com.tsix.immocrm.services.groupement;

import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
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
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Moteur d'agrégation des listes repliées — voir {@code instructions/FEATURE-groupement-listes.md}.
 *
 * <h2>Pourquoi un moteur unique et générique</h2>
 *
 * <p>Les trois listes visées (dossiers, prospections, pré-prospections) posent exactement le même
 * problème : compter et sommer <em>par valeur d'une colonne</em>, sous les filtres déjà actifs.
 * Seules changent l'entité racine et la {@link Specification}. Trois implémentations divergeraient
 * — c'est déjà arrivé aux trois mappeurs de {@code ProspectionListItemDto}, comme le rappelle le
 * javadoc de {@code Colonnes}.
 *
 * <h2>La règle non négociable : réutiliser la Specification de la liste</h2>
 *
 * <p>La {@code spec} passée ici <strong>doit être celle qui construit la liste</strong>, périmètre
 * commercial compris. Reconstruire les filtres en parallèle les ferait diverger au premier ajout,
 * et pour le périmètre ferait apparaître dans un total des lignes que l'appelant n'a pas le droit
 * de lire. C'est pourquoi ce service ne connaît aucun filtre : il les reçoit.
 *
 * <h2>Jointures explicitement LEFT</h2>
 *
 * <p>{@link Jointures} n'émet que des {@code LEFT JOIN}, et les mutualise. Une navigation
 * implicite ({@code root.get("stkItem").get("stkHierarchy")}) produirait des {@code INNER JOIN}
 * qui feraient disparaître les lignes à association nulle — donc des groupes entiers, en silence.
 * C'est le piège déjà rencontré sur le filtre {@code trancheId} de {@code DossierService}, et
 * l'invariant testé du §5 du FEATURE est là pour le détecter.
 */
@Service
public class GroupementService {

    private final EntityManager em;

    public GroupementService(EntityManager em) {
        this.em = em;
    }

    /**
     * Replie une liste en groupes.
     *
     * @param racine      entité racine ({@code ComFolder.class}, {@code ComProspection.class})
     * @param spec        la Specification de la liste, périmètre commercial inclus
     * @param cle         chemin de la colonne groupée
     * @param granularite non nulle uniquement si la colonne groupée est une date
     * @param agregats    nom de colonne exposé → chemin à sommer ; l'ordre est conservé
     * @param tri         ordre de restitution des groupes
     * @param inclureSansValeur inclure le groupe des lignes dont la colonne groupée est nulle
     */
    @Transactional(readOnly = true)
    public <E> Page<GroupeDto> grouper(Class<E> racine, Specification<E> spec, Chemin cle,
                                       Granularite granularite, Map<String, Chemin> agregats,
                                       TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<E> root = cq.from(racine);
        Jointures jointures = new Jointures(root);

        Expression<?> exprCle = expressionCle(cb, jointures, cle, granularite);
        // Les chemins de libellé ne sont sélectionnés que pour un groupement par tiers : ailleurs
        // le libellé EST la clé (colonne directe) ou s'en déduit (période).
        List<Expression<?>> exprLibelle = new ArrayList<>();
        if (cle.typeLibelle() == Chemin.TypeLibelle.TIER) {
            for (String cheminLibelle : cle.cheminsLibelle()) {
                exprLibelle.add(jointures.chemin(cheminLibelle));
            }
        }

        List<String> nomsAgregats = new ArrayList<>(agregats.keySet());
        Expression<Long> exprNombre = cb.count(root);

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(exprCle);
        selections.addAll(exprLibelle);
        selections.add(exprNombre);
        for (String nom : nomsAgregats) {
            // SUM nu. La substitution du NULL (groupe entièrement vide → 0, pas « case vide »)
            // se fait en Java, dans versGroupe.
            //
            // NE PAS remettre de COALESCE SQL ici. `cb.coalesce(sum, BigDecimal.ZERO)` tire le
            // type du résultat vers l'échelle du littéral : mesuré, 100,50 + 90,25 revenait à
            // 191,0 — une somme d'argent arrondie à l'unité, silencieusement. C'est le scénario
            // de sommeDesMontants_conserveLesCentimes, écrit précisément pour ça.
            selections.add(cb.sum(jointures.chemin(agregats.get(nom)).as(BigDecimal.class)));
        }

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

        List<GroupeDto> contenu = requete.getResultList().stream()
                .map(t -> versGroupe(t, cle, granularite, exprLibelle.size(), nomsAgregats))
                .toList();

        return new PageImpl<>(contenu, pageable,
                compterGroupes(racine, spec, cle, granularite, inclureSansValeur));
    }

    /**
     * Compose les filtres de la liste et, si le groupe sans valeur est exclu, le
     * {@code <clé> IS NOT NULL} qui le retire.
     *
     * <p>Le prédicat porte sur l'<strong>expression de clé</strong>, pas sur la colonne brute :
     * pour une date groupée au mois, la clé est {@code YEAR()*100 + MONTH()}, nulle exactement
     * quand la date l'est. Tester la colonne séparément marcherait ici mais divergerait de la
     * requête de comptage, qui compte des clés.
     */
    private static Predicate predicats(CriteriaBuilder cb, Predicate filtres, Expression<?> exprCle,
                                       boolean inclureSansValeur) {
        if (inclureSansValeur) {
            return filtres != null ? filtres : cb.conjunction();
        }
        Predicate renseignee = cb.isNotNull(exprCle);
        return filtres != null ? cb.and(filtres, renseignee) : renseignee;
    }

    /**
     * Nombre total de groupes, pour l'enveloppe de pagination.
     *
     * <p><strong>{@code COUNT(DISTINCT x)} ignore les NULL en SQL</strong> — le groupe des lignes
     * sans valeur existe donc dans la requête d'agrégation mais échapperait au total, qui serait
     * systématiquement inférieur d'une unité dès qu'une ligne n'a pas de valeur. D'où le second
     * terme, qui ajoute ce groupe s'il est peuplé.
     */
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
        // Le second terme ne peut être atteint que si le groupe nul est inclus — le prédicat
        // l'élimine sinon, et la somme vaut alors zéro d'elle-même.
        return distincts + (nuls != null && nuls.longValue() > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // Expression de la clé de groupe
    // ------------------------------------------------------------------

    /**
     * Clé de groupe, toujours une expression <strong>unique</strong>.
     *
     * <p>C'est ce qui rend {@code COUNT(DISTINCT)} et l'{@code ORDER BY} exprimables. Pour un
     * groupement par tiers, les colonnes de nom s'ajoutent au {@code GROUP BY} mais pas à la clé :
     * elles sont fonctionnellement dépendantes de l'identifiant, donc le nombre de groupes est
     * bien le nombre d'identifiants distincts.
     *
     * <p>Une date est ramenée à un <strong>entier de période</strong> ({@code 202507}) plutôt qu'à
     * une chaîne formatée : un seul {@code YEAR()/MONTH()} par dialecte, pas de dépendance à
     * {@code FORMAT()} propre à SQL Server, et un tri chronologique gratuit puisque l'ordre
     * numérique est l'ordre du temps.
     */
    private static Expression<?> expressionCle(CriteriaBuilder cb, Jointures jointures,
                                               Chemin cle, Granularite granularite) {
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
     * Cascade {@code COALESCE(NULLIF(TRIM(a), ''), NULLIF(TRIM(b), ''), …)}.
     *
     * <p>{@code NULLIF(TRIM(...), '')} et non la colonne nue : c'est ce qui reproduit
     * {@code StringUtils.hasText} du mappeur, qui écarte aussi les chaînes blanches. Un
     * {@code COALESCE} nu ne neutralise que {@code NULL} et ferait diverger l'en-tête de groupe de
     * la valeur affichée sur la ligne — voir {@link Chemin#repli}.
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
        // Trier sur l'identifiant rendrait un ordre arbitraire. On rejoue la priorité de
        // TierNomFormatter en COALESCE pour que l'ordre alphabétique soit celui des libellés
        // effectivement affichés : raison sociale > description > nom de famille.
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
                                        int nbLibelles, List<String> nomsAgregats) {
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

        long nombre = ((Number) t.get(1 + nbLibelles)).longValue();
        Map<String, BigDecimal> agregats = new LinkedHashMap<>();
        for (int i = 0; i < nomsAgregats.size(); i++) {
            Object valeur = t.get(2 + nbLibelles + i);
            // SUM d'un groupe dont toutes les lignes sont nulles rend NULL ; un total vaut zéro.
            // Substitué ici plutôt qu'en SQL — voir le commentaire du COALESCE dans grouper().
            agregats.put(nomsAgregats.get(i), switch (valeur) {
                case null -> BigDecimal.ZERO;
                case BigDecimal bd -> bd;
                case Number n -> new BigDecimal(n.toString());
                default -> BigDecimal.ZERO;
            });
        }
        return new GroupeDto(valeurCle, libelle, nombre, agregats);
    }

    /** {@code 202507} → « juillet 2025 ». Le mois en toutes lettres, comme partout dans l'API. */
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
     * Prédicat restreignant une liste aux lignes d'un groupe — c'est ce qui rend un groupe
     * <em>ouvrable</em>.
     *
     * <p>{@code valeur} est la {@code cle} renvoyée par {@code /groupes}, réémise telle quelle par
     * le client. Elle est reconvertie d'après le type Java de la colonne visée, donc le front n'a
     * rien à typer : c'est la seule façon d'éviter un langage de filtre générique et sa coercition.
     *
     * <p>{@code valeur} absente ou vide → le groupe des lignes <strong>sans valeur</strong>. Ce
     * n'est pas un cas d'erreur : ce groupe est renvoyé par {@code /groupes} et doit pouvoir
     * s'ouvrir comme les autres.
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
        return valeur;
    }

    // ------------------------------------------------------------------

    /**
     * Résout un chemin pointé en {@code Path}, en mutualisant des jointures {@code LEFT}.
     *
     * <p>La mutualisation n'est pas une optimisation : sans elle,
     * {@code stkItem.stkHierarchy.cfgProject.code} et
     * {@code stkItem.stkHierarchy.cfgProject.cfgCompany.code} demandés ensemble produiraient deux
     * jointures distinctes vers les mêmes tables.
     */
    private static final class Jointures {

        private final Root<?> root;
        private final Map<String, From<?, ?>> cache = new HashMap<>();

        private Jointures(Root<?> root) {
            this.root = root;
        }

        private Path<?> chemin(Chemin chemin) {
            return chemin(chemin.cheminCle());
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
