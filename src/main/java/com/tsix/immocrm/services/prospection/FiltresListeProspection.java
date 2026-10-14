package com.tsix.immocrm.services.prospection;

import java.util.List;
import java.util.UUID;

/**
 * Filtres de la barre des listes de prospections et de pré-prospections, tels que reçus. Les dates
 * restent des chaînes : une date sans heure vaut le jour entier ({@link PeriodeListe}).
 *
 * @param inclureSansTranche « Non affectés » : tranches cochées ou sans tranche ; seules les lignes sans
 *                           tranche si aucune n'est cochée
 */
public record FiltresListeProspection(List<String> etapes, List<String> origines, List<UUID> commerciaux,
                                      List<UUID> trancheIds, boolean inclureSansTranche, String dateDebut,
                                      String dateFin, String search, Boolean ouvertes) {
}
