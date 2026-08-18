package com.tsix.immocrm.domain;

/**
 * Availability status of an {@code StkItem} (article/lot), computed — never stored.
 *
 * Confirmed by direct SQL check against the real data (0 rows for a
 * {@code StkItem.ComFolderId} pointing at a Dossier annulé/désistement):
 * <ul>
 *   <li>{@link #DISPONIBLE} — {@code StkItem.ComFolderId IS NULL}</li>
 *   <li>{@link #OCCUPE} — {@code StkItem.ComFolderId IS NOT NULL}</li>
 * </ul>
 * No join to {@code ComFolder} is needed for this distinction. The finer boundary between
 * "réservé" and "vendu" within OCCUPE is NOT confirmed yet (depends on the not-yet-defined
 * StatutDossier values) — do not split this enum further until that's resolved.
 */
public enum StatutArticle {
    DISPONIBLE,
    OCCUPE
}
