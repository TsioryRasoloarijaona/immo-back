package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;

import java.util.UUID;

/**
 * Tranche d'une interaction, qu'aucune colonne de {@code ComActionMessage} ne porte : la règle
 * de {@code ComActionMessageView}, recopiée parce que la vue est trop lente pour être lue.
 * Voir {@code instructions/FEATURE-vues-legacy-prospection.md} §4.
 */
public final class ContexteInteraction {

    /** {@code ComActionMessageCategory.PKey} de la catégorie « Prospection ». */
    private static final UUID CATEGORIE_PROSPECTION =
            UUID.fromString("69f8e7d6-1111-1111-1111-1aa8b1f02008");

    private ContexteInteraction() {
    }

    /** {@code null} quand ni la prospection ni le lot n'en portent. */
    public static CfgTranche tranche(ComActionMessage m) {
        // Vue (sys.sql_modules) : IIF(CategoryId = Prospection, ComProspection.StkHierarchyId,
        // StkItem.StkHierarchyId) AS CfgTrancheId.
        boolean prospection = m.getComActionMessageCategory() != null
                && CATEGORIE_PROSPECTION.equals(m.getComActionMessageCategory().getId());
        if (prospection) {
            return m.getComProspection() != null ? m.getComProspection().getStkHierarchy() : null;
        }
        return m.getStkItem() != null ? m.getStkItem().getStkHierarchy() : null;
    }
}
