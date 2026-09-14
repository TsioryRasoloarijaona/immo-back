package com.tsix.immocrm.domain;

/**
 * Un agrégat demandé par le client dans {@code ?agregats=}, résolu contre son catalogue.
 *
 * <p>Remplace le {@code Map<String, Chemin>} que les services construisaient : il fallait y porter
 * une fonction en plus du chemin, et une seconde carte parallèle aurait fini par diverger de la
 * première.
 *
 * @param jeton    le texte <strong>exact</strong> envoyé par le client ({@code "prix"} ou
 *                 {@code "prix:AVG"}). C'est la clé sous laquelle le résultat est renvoyé, et c'est
 *                 ce qui rend l'ouverture du menu Σ non cassante : un appelant qui écrit un nom nu
 *                 retrouve ce nom nu dans la réponse, exactement comme avant.
 * @param colonne  nom de colonne du catalogue, sans la fonction
 * @param chemin   chemin JPA résolu — jamais {@code null}, une colonne sans chemin n'est pas
 *                 agrégeable et aurait été refusée en amont
 * @param fonction fonction à appliquer ; {@link FonctionAgregat#DEFAUT} quand le jeton est nu
 */
public record AgregatDemande(String jeton, String colonne, Chemin chemin, FonctionAgregat fonction) {
}
