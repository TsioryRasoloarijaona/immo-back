package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Statut d'un règlement ({@code StlSettlement.Status}) et table des transitions autorisées.
 *
 * <p>Contrairement à {@link StatutDossier}, dont les valeurs viennent d'une table de référence
 * ({@code StrStatus}), ce statut est une simple chaîne libre dans {@code StlSettlement.Status}.
 * Les valeurs ci-dessous sont donc les seules **réellement observées** en base réelle
 * (3 244 lignes) :
 * <ul>
 *   <li>{@code IN_WORK} — 3 218 lignes : saisi, pas encore encaissé ;</li>
 *   <li>{@code POINTED} — 14 lignes : encaissement constaté ;</li>
 *   <li>{@code CANCELED} — 3 lignes ;</li>
 *   <li>{@code NULL} — 9 lignes legacy, tolérées en lecture, jamais écrites par l'API.</li>
 * </ul>
 * {@code UNPAID}/{@code LOST} apparaissent dans les filtres legacy documentés sur
 * {@code EncaissementDto} mais ne portent <b>aucune ligne réelle</b> : délibérément absents ici.
 *
 * <p><b>Invariant lié</b> — {@code POINTED} ⟺ {@code SettlementDate} renseignée : les 14 lignes
 * {@code POINTED} en ont une, aucune des 3 218 {@code IN_WORK} n'en a. Statut et date sont deux
 * encodages du même fait et n'ont jamais divergé ; le service de transition doit les écrire
 * ensemble (voir {@code FEATURE-reglement-statut-preuve.md} §2, invariant 1).
 *
 * <p>La table de transitions est embarquée ici plutôt que dans une classe séparée à la
 * {@link TransitionsValidees} : trois valeurs et deux arêtes ne justifient pas un second fichier.
 * Elle est déduite du cycle de vie métier (un règlement saisi peut être pointé ou annulé, et les
 * deux états sont terminaux), pas de l'historique — {@code StlSettlementHistory} ne trace ni
 * l'ancien ni le nouveau statut, seulement les dates et la référence.
 */
public enum StatutReglement {

    IN_WORK,
    POINTED,
    CANCELED;

    private static final Map<StatutReglement, Set<StatutReglement>> TRANSITIONS =
            new EnumMap<>(StatutReglement.class);

    static {
        TRANSITIONS.put(IN_WORK, EnumSet.of(POINTED, CANCELED));
        TRANSITIONS.put(POINTED, EnumSet.noneOf(StatutReglement.class));
        TRANSITIONS.put(CANCELED, EnumSet.noneOf(StatutReglement.class));
    }

    /** Statut d'un règlement qui vient d'être créé — aligné sur les 3 218 lignes legacy. */
    public static final StatutReglement INITIAL = IN_WORK;

    public static boolean estValide(StatutReglement depuis, StatutReglement vers) {
        return TRANSITIONS.getOrDefault(depuis, Set.of()).contains(vers);
    }

    public static Set<StatutReglement> transitionsAutorisees(StatutReglement depuis) {
        return Set.copyOf(TRANSITIONS.getOrDefault(depuis, Set.of()));
    }

    /**
     * Lecture tolérante de la colonne : {@link Optional#empty()} pour une valeur absente <b>ou
     * inconnue</b>, jamais d'exception. Indispensable parce que {@code Status} est du texte libre
     * alimenté par l'application desktop, qui continue d'écrire dans {@code yesRems} — une valeur
     * inattendue ne doit pas faire échouer {@code GET /api/encaissements}, qui lit ce champ tel quel.
     *
     * <p>L'absence est portée par le type et non par un {@code null} : l'appelant qui l'ignore ne
     * compile pas, là où il obtenait auparavant un {@code null} silencieux.
     */
    public static Optional<StatutReglement> lireCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        for (StatutReglement statut : values()) {
            if (statut.name().equalsIgnoreCase(code.trim())) {
                return Optional.of(statut);
            }
        }
        return Optional.empty();
    }

    /**
     * Conversion stricte, pour une valeur reçue d'un client (corps de {@code PATCH .../statut}) :
     * une valeur inconnue est une erreur de requête, pas une donnée legacy à tolérer.
     */
    public static StatutReglement depuisCode(String code) {
        return lireCode(code).orElseThrow(() ->
                new IllegalArgumentException("Statut de règlement inconnu: " + code));
    }
}
