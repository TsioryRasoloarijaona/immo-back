package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlSettlement;

/**
 * Seule règle du « payeur » d'un règlement, pour l'affichage et les documents imprimés :
 * {@code TierPayerId}, repli {@code TierId} (lignes anciennes sans payeur). {@code TierId} est le
 * client du dossier. Voir {@code FEATURE-interop-omran-ecritures.md} §5.
 */
public final class PayeurReglement {

    private PayeurReglement() {
    }

    public static CfgTier de(StlSettlement reglement) {
        if (reglement == null) {
            return null;
        }
        return reglement.getTierPayer() != null ? reglement.getTierPayer() : reglement.getTier();
    }
}
