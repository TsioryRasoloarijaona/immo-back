package com.tsix.immocrm.services.etatventes;

import com.tsix.immocrm.dto.etatventes.SyntheseVendusLibresDto.CompteVendusLibres;

/** Ligne du classeur Libres / Réservés / Vendus : composantes aux noms de {@code ColonneVendusLibres}. */
public record LigneTableurVendusLibres(String tranche, String famille, long libres, long reserves, long vendus,
                                       long total, double pctLibres, double pctReserves, double pctVendus) {

    static LigneTableurVendusLibres de(String tranche, String famille, CompteVendusLibres c) {
        return new LigneTableurVendusLibres(tranche, famille, c.libres(), c.reserves(), c.vendus(), c.total(),
                c.pctLibres(), c.pctReserves(), c.pctVendus());
    }
}
