package com.tsix.immocrm.utils;

import java.util.function.Function;

/**
 * Lecture d'un champ à travers une association {@code @ManyToOne} potentiellement absente.
 *
 * <p>Presque tous les mappings entité → DTO de ce projet traversent des FK optionnelles
 * ({@code StkItem.StkHierarchyId}, {@code ComFolder.StrStatusId}, {@code StlSettlement.TierId}…),
 * ce qui produisait la même expression répétée environ 220 fois :
 *
 * <pre>{@code
 * tranche != null ? tranche.getCode() : null
 * projet != null && projet.getCfgCompany() != null ? projet.getCfgCompany().getDescription() : null
 * }</pre>
 *
 * <p>La forme à deux niveaux est la plus fautive : elle nomme le getter intermédiaire deux fois,
 * donc l'évalue deux fois — sur une association {@code LAZY} c'est deux passages par le proxy
 * Hibernate là où un seul suffit, et une occasion de se tromper de variable dans une liste
 * d'arguments de record qui en compte parfois trente.
 *
 * <p>Même démarche que {@link TierNomFormatter}, extrait pour la même raison : une expression
 * recopiée dans une douzaine de services finit par diverger. Sont délibérément absents d'ici :
 * <ul>
 *   <li>le simple repli sur une valeur par défaut ({@code montant != null ? montant : ZERO}) —
 *       c'est {@link java.util.Objects#requireNonNullElse} du JDK, inutile de le réécrire ;</li>
 *   <li>toute variante renvoyant un {@link java.util.Optional} — un {@code Optional} sert à
 *       exprimer l'absence dans un <b>type de retour</b>, pas à emballer une valeur qu'on
 *       déballe au token suivant. {@code ArticleService} le faisait 49 fois
 *       ({@code orNull(x, X::getY).orElse(null)}), soit 49 allocations pour rien.</li>
 * </ul>
 */
public final class Champs {

    private Champs() {
    }

    /**
     * {@code de(tranche, CfgTranche::getCode)} — le champ, ou {@code null} si l'association l'est.
     * Remplace {@code tranche != null ? tranche.getCode() : null}.
     */
    public static <T, R> R de(T source, Function<T, R> lecteur) {
        return source == null ? null : lecteur.apply(source);
    }

    /**
     * Idem à travers deux associations : {@code de(dossier, ComFolder::getStrStatus,
     * StrStatus::getCode)} — {@code null} dès que l'un des deux maillons manque, et le maillon
     * intermédiaire n'est traversé qu'une fois.
     */
    public static <T, U, R> R de(T source, Function<T, U> premier, Function<U, R> second) {
        U intermediaire = de(source, premier);
        return intermediaire == null ? null : second.apply(intermediaire);
    }
}
