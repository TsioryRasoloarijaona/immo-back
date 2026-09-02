package com.tsix.immocrm.utils;

import com.tsix.immocrm.exceptions.InvalidRequestException;

/**
 * Contrôle de champ obligatoire sur un corps de requête, en une ligne au lieu de trois.
 *
 * <p>Le dépôt valide à la main plutôt qu'avec {@code @Valid} — décision explicite, rappelée dans
 * {@code FEATURE-reglement-statut-preuve.md} §4.4 : aucun handler
 * {@code MethodArgumentNotValidException} n'existe dans {@link
 * com.tsix.immocrm.web.GlobalExceptionHandler}, donc une violation Bean Validation sortirait hors
 * du format {@link org.springframework.http.ProblemDetail} que tout le reste de l'API respecte.
 * Ce helper garde cette validation manuelle, il en supprime seulement le passe-partout : les six
 * contrôles consécutifs de {@code EcheancierService.addReglement} occupaient quinze lignes pour
 * six décisions.
 *
 * <p>Le <b>message complet</b> est passé par l'appelant, jamais dérivé du nom du champ : l'accord
 * en français dépend du genre du mot ({@code montant est requis} mais {@code dateEcheance est
 * requise}), qu'aucune signature générique ne peut deviner. Les messages existants restent donc
 * identiques au caractère près.
 */
public final class Requis {

    private Requis() {
    }

    /**
     * Renvoie {@code valeur}, ou lève un {@link InvalidRequestException} (⟶ 400) portant
     * {@code message} si elle est {@code null}. La valeur est renvoyée pour permettre
     * {@code var montant = Requis.champ(request.montant(), "montant est requis");}.
     */
    public static <T> T champ(T valeur, String message) {
        if (valeur == null) {
            throw new InvalidRequestException(message);
        }
        return valeur;
    }
}
