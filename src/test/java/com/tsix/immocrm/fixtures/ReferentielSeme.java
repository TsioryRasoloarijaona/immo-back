package com.tsix.immocrm.fixtures;

import java.util.UUID;

/**
 * Identifiants des lignes de référentiel semées par le bootstrap de la base de test.
 *
 * <p>La règle du projet est que les fixtures construisent ce dont elles ont besoin. Ces
 * identifiants sont l'exception assumée : ils désignent des tables du logiciel légataire
 * qu'<b>aucune entité JPA ne modélise</b>, donc qu'aucun code de test ne peut créer. Les lignes
 * correspondantes sont insérées par {@code ci/schema/040_seed_reference.sql}, qui documente
 * pourquoi chacune existe.
 *
 * <p>Les valeurs doivent rester alignées avec ce script — elles n'ont pas d'autre source.
 */
public final class ReferentielSeme {

    /**
     * Ligne {@code PrjProject} servant de cible à {@code AdmUserHabilitation.PrjProjectId}.
     *
     * <p>Un UUID tiré au hasard conviendrait contre {@code yesRemsTest}, où la contrainte
     * n'existe pas, mais pas contre le schéma de production reconstitué en CI, où
     * {@code AdmUserHabilitation__PrjProject_FK1} le rejette.
     */
    public static final UUID PRJ_PROJECT = UUID.fromString("B4E9D2C1-3A78-4F60-8D15-6E90C7B2A833");

    private ReferentielSeme() {
    }
}
