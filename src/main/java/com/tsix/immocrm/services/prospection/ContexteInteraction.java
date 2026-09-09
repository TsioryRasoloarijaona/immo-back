package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;

import java.util.UUID;

/**
 * Résout le couple projet / tranche d'une interaction.
 *
 * <p>Cette information n'est portée par aucune colonne de {@code ComActionMessage} : le logiciel
 * de bureau la déduit, et la vue {@code ComActionMessageView} l'expose sous {@code CfgTrancheId} /
 * {@code CfgProjectId} — renseignés sur <strong>96 %</strong> des 48 160 interactions réelles.
 *
 * <p>La règle reproduite ici est <strong>celle de la vue</strong>, relevée mot pour mot dans sa
 * définition ({@code sys.sql_modules}) :
 *
 * <pre>
 * IIF(ComActionMessage.ComActionMessageCategoryId = '69f8e7d6-1111-1111-1111-1aa8b1f02008',
 *     ComProspection.StkHierarchyId,
 *     StkItem.StkHierarchyId) AS CfgTrancheId
 * </pre>
 *
 * <p>Autrement dit : une interaction de <em>prospection</em> hérite de la tranche de sa
 * prospection ; toute autre (dossier de vente, réclamation, transfert…) hérite de celle de son
 * lot. Le projet se déduit ensuite de la tranche.
 *
 * <p><strong>Pourquoi ne pas lire la vue.</strong> Elle n'a aucune sélectivité : un
 * {@code SELECT *} sur une seule ligne par sa clé primaire prend 30 secondes, à cause du
 * {@code COUNT} corrélé qui calcule {@code ActionRank}. Recopier la règle coûte deux jointures
 * déjà chargées. Voir {@code instructions/FEATURE-vues-legacy-prospection.md} §4.
 *
 * <p>Corollaire : la concordance avec la vue n'a pu être sondée que sur un échantillon (la
 * comparaison exhaustive ne termine pas, pour la raison ci-dessus). La source de vérité retenue
 * est la définition de la vue, pas le sondage.
 */
public final class ContexteInteraction {

    /** {@code ComActionMessageCategory.PKey} de la catégorie « Prospection » (46 666 lignes). */
    private static final UUID CATEGORIE_PROSPECTION =
            UUID.fromString("69f8e7d6-1111-1111-1111-1aa8b1f02008");

    private ContexteInteraction() {
    }

    /**
     * Tranche de rattachement de l'interaction, ou {@code null} quand ni la prospection ni le lot
     * n'en portent (4 % des lignes réelles).
     */
    public static CfgTranche tranche(ComActionMessage m) {
        boolean prospection = m.getComActionMessageCategory() != null
                && CATEGORIE_PROSPECTION.equals(m.getComActionMessageCategory().getId());
        if (prospection) {
            return m.getComProspection() != null ? m.getComProspection().getStkHierarchy() : null;
        }
        return m.getStkItem() != null ? m.getStkItem().getStkHierarchy() : null;
    }
}
