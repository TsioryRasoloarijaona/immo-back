package com.tsix.immocrm.dto.equipe;

import com.tsix.immocrm.domain.MotifAttribution;
import com.tsix.immocrm.domain.RaisonNonAttribution;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat (ou aperçu) d'une répartition automatique.")
public record ResultatRepartitionDto(
        @Schema(description = "`true` : aperçu, rien n'a été écrit.")
        boolean simulation,
        @Schema(description = "Lignes attribuées (ou qui le seraient), dans l'ordre de la demande.")
        List<AttributionDto> attributions,
        @Schema(description = "Lignes laissées « à distribuer », avec la raison.")
        List<NonAttribueeDto> nonAttribuees,
        @Schema(description = "Total par commercial, trié par nom.")
        List<RepartitionCommercialDto> parCommercial
) {

    @Schema(description = "Une pré-prospection et le commercial choisi.")
    public record AttributionDto(
            UUID preProspectionId,
            @Schema(example = "26/01234")
            String code,
            @Schema(description = "Commercial choisi (employé, CfgTier).")
            UUID commercialId,
            String commercialNom,
            @Schema(description = "`CONNU` : contact déjà suivi par ce commercial ; `MOINS_CHARGE` : le moins de leads à trier.")
            MotifAttribution motif
    ) {
    }

    @Schema(description = "Une pré-prospection que la répartition laisse sans commercial.")
    public record NonAttribueeDto(
            UUID preProspectionId,
            @Schema(example = "26/01235")
            String code,
            @Schema(description = "`SANS_PROJET`, `AUCUN_ELIGIBLE` (aucun commercial habilité sur le projet) ou `DEJA_ATTRIBUEE`.")
            RaisonNonAttribution raison
    ) {
    }
}
