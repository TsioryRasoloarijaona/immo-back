package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.utils.Champs;

/**
 * Société d'un dossier, écrite sur ses échéances et règlements : article principal → tranche → projet
 * → société, jamais {@code StkItem.CfgProjectId} (colonne morte). {@code null} si un maillon manque.
 */
public final class SocieteDossier {

    private SocieteDossier() {
    }

    public static CfgCompany de(ComFolder folder) {
        CfgTranche tranche = Champs.de(folder, ComFolder::getStkItem, StkItem::getStkHierarchy);
        return Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getCfgCompany);
    }
}
