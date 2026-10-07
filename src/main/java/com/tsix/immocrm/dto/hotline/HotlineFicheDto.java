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
 * Chaque liste est plafonnée à {@value com.tsix.immocrm.services.hotline.HotlineService#HISTORIQUE_LIMIT}
 * entrées ; l'historique complet passe par les sous-ressources {@code /api/prospects/{id}/...}.
 */
public record HotlineFicheDto(

        ProspectDetailDto tier,

        List<ProspectionListItemDto> prospections,

        @Schema(description = "Dossiers de vente où cette personne est le client, les plus "
                + "récents en premier.")
        List<DossierListItemDto> dossiers,

        @Schema(description = "Règlements (paiements) de cette personne, tous dossiers confondus, "
                + "les plus récents en premier.")
        List<ReglementProspectDto> reglements,

        List<InteractionDto> interactions,

        List<HistoriqueDto> historique
) {
}
