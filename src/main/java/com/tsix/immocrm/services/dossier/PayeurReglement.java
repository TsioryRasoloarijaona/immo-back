package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlSettlement;

/**
 * Qui a payé un règlement : {@code TierPayerId}, à défaut {@code TierId} (lignes anciennes du CRM, qui
 * y mettaient le payeur). {@code TierId} reste le client du dossier, comme chez OMRAN.
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
