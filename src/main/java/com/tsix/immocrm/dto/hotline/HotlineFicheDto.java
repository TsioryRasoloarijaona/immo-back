package com.tsix.immocrm.dto.hotline;

import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Everything known about one {@code CfgTier} matched by phone number, for
 * {@code GET /api/hotline/recherche}. Bundles the same information a hotline agent would
 * otherwise have to gather by opening the prospect fiche and its four separate tabs
 * ({@code /api/prospects/{id}/prospections}, {@code .../dossiers}, {@code .../reglements},
 * {@code .../historique}) plus the tier-level interaction journal — in one response.
 *
 * <p>Each history list is capped to the {@value com.tsix.immocrm.services.hotline.HotlineService#HISTORIQUE_LIMIT}
 * most recent entries. If a caller has more than that, use {@code tier.id} to page through the
 * full history via the dedicated {@code /api/prospects/{id}/...} sub-resource endpoints.
 */
@Schema(description = "Fiche complète d'une personne déjà connue (CfgTier) trouvée par numéro de "
        + "téléphone : informations générales + tout son historique d'activité, limité aux "
        + "entrées les plus récentes de chaque catégorie. Pour l'historique complet d'une "
        + "catégorie, utiliser tier.id avec les endpoints /api/prospects/{id}/... correspondants.")
public record HotlineFicheDto(

        @Schema(description = "Informations générales de la personne (identité, coordonnées, "
                + "type/kind, commercial référent...) — identique au détail retourné par "
                + "GET /api/prospects/{id}.")
        ProspectDetailDto tier,

        @Schema(description = "Prospections (démarches d'achat/vente en cours ou closes) où cette "
                + "personne est le prospect, les plus récentes en premier.")
        List<ProspectionListItemDto> prospections,

        @Schema(description = "Dossiers de vente où cette personne est le client, les plus "
                + "récents en premier.")
        List<DossierListItemDto> dossiers,

        @Schema(description = "Règlements (paiements) de cette personne, tous dossiers confondus, "
                + "les plus récents en premier.")
        List<ReglementProspectDto> reglements,

        @Schema(description = "Journal des interactions (appels, messages, notes) enregistrées "
                + "directement sur sa fiche — hors interactions déjà rattachées à une "
                + "prospection ou un dossier précis, les plus récentes en premier.")
        List<InteractionDto> interactions,

        @Schema(description = "Historique des changements de statut/workflow de cette personne "
                + "(alimente l'onglet \"Historique\" de la fiche prospect), le plus récent en "
                + "premier.")
        List<HistoriqueDto> historique
) {
}
