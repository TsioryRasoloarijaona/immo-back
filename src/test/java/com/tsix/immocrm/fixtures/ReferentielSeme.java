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

    /** {@code StrEntity} « StlSettlementIn », {@code StrEntityId} de tout encaissement. */
    public static final UUID STR_ENTITY_ENCAISSEMENT = UUID.fromString("D7061C59-A039-4EF8-A6ED-B0E886F8EDFF");

    /** {@code StrEntity} « StlSettlementOut ». */
    public static final UUID STR_ENTITY_DECAISSEMENT = UUID.fromString("8287D7A9-2D51-4F2E-8F47-0F1B738F1A11");

    /** {@code AdmModule} « COM » (vente). */
    public static final UUID ADM_MODULE_COM = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00001");

    /** {@code StlCurrency} « MAD », valeur du paramètre {@code AdmParamsGuid} « DefaultCurrency ». */
    public static final UUID STL_CURRENCY_MAD = UUID.fromString("24EB36A2-2E81-4A12-AF02-6259CFA25BD6");

    private ReferentielSeme() {
    }
}
