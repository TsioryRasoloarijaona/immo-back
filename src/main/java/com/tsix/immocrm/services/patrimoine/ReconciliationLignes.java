package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.ChampInvalideException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Réconciliation d'un onglet de tranche envoyé en entier (FEATURE-patrimoine-tranche-onglets.md §1) : ligne avec id
 * connu = modifiée, sans id = créée, absente = supprimée ; une ligne dont la clé est déjà prise plus haut est ignorée
 * (doublon, comme OMRAN). Les refus sont des 400 sur {@code lignes[i].<champ>}.
 */
final class ReconciliationLignes {

    static final String CHAMP_LIGNES = "lignes";

    /** Ce que chaque onglet sait de ses lignes. */
    interface Onglet<L, E> {
        UUID idLigne(L ligne);

        /** Clé de doublon ; lève le 400 du champ obligatoire si elle manque. */
        Object cle(L ligne, String prefixe);

        E nouvelle();

        /** Valide et pose les valeurs de la ligne ; {@code rang} = position parmi les lignes gardées. */
        void appliquer(E entite, L ligne, int rang, String prefixe);
    }

    record Resultat<E>(List<E> gardees, List<E> supprimees) {
    }

    private ReconciliationLignes() {
    }

    static String prefixe(int index) {
        return CHAMP_LIGNES + "[" + index + "].";
    }

    static <L, E> Resultat<E> reconcilier(List<E> existantes, Function<E, UUID> idDe, List<L> lignes,
                                          Onglet<L, E> onglet) {
        if (lignes == null) {
            throw new ChampInvalideException(CHAMP_LIGNES, "La liste des lignes est obligatoire (vide pour tout supprimer).");
        }
        Map<UUID, E> parId = new LinkedHashMap<>();
        existantes.forEach(e -> parId.put(idDe.apply(e), e));
        Set<Object> cles = new HashSet<>();
        Set<UUID> idsVus = new HashSet<>();
        List<E> gardees = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            L ligne = lignes.get(i);
            String prefixe = prefixe(i);
            if (ligne == null) {
                throw new ChampInvalideException(CHAMP_LIGNES + "[" + i + "]", "Ligne vide.");
            }
            UUID id = onglet.idLigne(ligne);
            if (id != null && !parId.containsKey(id)) {
                throw new ChampInvalideException(prefixe + "id", "Cette ligne n'appartient pas à la tranche : " + id);
            }
            if (!cles.add(onglet.cle(ligne, prefixe)) || (id != null && !idsVus.add(id))) {
                continue;
            }
            E entite = id == null ? onglet.nouvelle() : parId.get(id);
            onglet.appliquer(entite, ligne, gardees.size(), prefixe);
            gardees.add(entite);
        }
        List<E> supprimees = existantes.stream().filter(e -> !gardees.contains(e)).toList();
        return new Resultat<>(gardees, supprimees);
    }
}
