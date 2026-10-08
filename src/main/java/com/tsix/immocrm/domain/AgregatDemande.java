package com.tsix.immocrm.domain;

/**
 * Un agrégat de {@code ?agregats=}, résolu contre son catalogue.
 *
 * @param jeton  texte exact envoyé ({@code "prix"} ou {@code "prix:AVG"}) : clé du résultat
 * @param chemin jamais {@code null}, une colonne sans chemin est refusée en amont
 */
public record AgregatDemande(String jeton, String colonne, Chemin chemin, FonctionAgregat fonction) {
}
