package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Règles de répartition du capital (FEATURE-entites-juridiques.md §2) : seul calcul des parts d'une entité juridique
 * depuis un groupe d'actionnaires, et seuls contrôles d'une répartition d'entité ou de groupe. Messages exacts d'OMRAN.
 */
public final class RepartitionActionnaires {

    public static final String DOUBLON = "L'actionnaire existe déjà dans la liste !!!";
    public static final String NOMBRE_ACTIONNAIRES =
            "Le nombre de répartitions doit être égal au nombre d'actionnaires !";
    public static final String NOMBRE_PARTS = "Le nombre de parts (actions) doit être égal au total des nombre de "
            + "parts / actions dans la répartition !";
    public static final String CENT_POUR_CENT = "La somme des pourcentages doit être égale à 100 % !";

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** Ligne d'un groupe : {@code pourcentage} en fraction (0,4 = 40 %). */
    public record Pourcentage(UUID actionnaireId, BigDecimal pourcentage) {
    }

    public record Parts(UUID actionnaireId, int nombre) {
    }

    private RepartitionActionnaires() {
    }

    /**
     * {@code CfgCompany.SetRepartitions} : chaque ligne sauf la dernière reçoit {@code (int)(pourcentage × 100) ×
     * nombreParts / 100} (troncatures entières), la dernière le reste — la somme vaut toujours {@code nombreParts}.
     * Calcul en {@code long} : OMRAN déborde en {@code int} au-delà de 21 474 836 parts, pas nous.
     */
    public static List<Parts> depuisGroupe(List<Pourcentage> lignes, int nombreParts) {
        List<Parts> resultat = new ArrayList<>();
        long distribuees = 0;
        for (int i = 0; i < lignes.size() - 1; i++) {
            Pourcentage ligne = lignes.get(i);
            long centiemes = ligne.pourcentage().multiply(CENT).longValue();
            int parts = (int) (centiemes * nombreParts / 100);
            resultat.add(new Parts(ligne.actionnaireId(), parts));
            distribuees += parts;
        }
        if (!lignes.isEmpty()) {
            resultat.add(new Parts(lignes.getLast().actionnaireId(), (int) (nombreParts - distribuees)));
        }
        return List.copyOf(resultat);
    }

    /** {@code CfgCompany.Verif}, sans condition : le nombre de lignes, puis la somme des parts. */
    public static Optional<String> erreurRepartition(int nombreActionnaires, int nombreParts, List<Integer> parts) {
        if (parts.size() != nombreActionnaires) {
            return Optional.of(NOMBRE_ACTIONNAIRES);
        }
        long somme = parts.stream().mapToLong(Integer::longValue).sum();
        return somme == nombreParts ? Optional.empty() : Optional.of(NOMBRE_PARTS);
    }

    /** {@code CfgGroupShareholder.Verif} : somme exactement égale à 1, un groupe vide est donc refusé. */
    public static boolean fontCentPourCent(List<BigDecimal> pourcentages) {
        return pourcentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ONE) == 0;
    }

    public static boolean contientUnDoublon(List<UUID> actionnaireIds) {
        Set<UUID> vus = new HashSet<>();
        return actionnaireIds.stream().anyMatch(id -> !vus.add(id));
    }
}
