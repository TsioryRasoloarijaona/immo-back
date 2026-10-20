package com.tsix.immocrm.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/** Colonnes de l'export Libres / Réservés / Vendus (FEATURE-tdb-vente-vendus-libres.md §4.3), dans l'ordre du classeur. */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ColonneVendusLibres implements Colonne {

    TRANCHE("tranche", "Tranche", TypeExport.TEXTE),
    FAMILLE("famille", "Famille", TypeExport.TEXTE),
    LIBRES("libres", "Libres", TypeExport.NOMBRE),
    RESERVES("reserves", "Réservés", TypeExport.NOMBRE),
    VENDUS("vendus", "Vendus", TypeExport.NOMBRE),
    TOTAL("total", "Total", TypeExport.NOMBRE),
    PCT_LIBRES("pctLibres", "% Libres", TypeExport.NOMBRE),
    PCT_RESERVES("pctReserves", "% Réservés", TypeExport.NOMBRE),
    PCT_VENDUS("pctVendus", "% Vendus", TypeExport.NOMBRE);

    private final String nom;
    private final String libelle;
    private final TypeExport typeExport;

    @Override
    public boolean parDefaut() {
        return true;
    }
}
