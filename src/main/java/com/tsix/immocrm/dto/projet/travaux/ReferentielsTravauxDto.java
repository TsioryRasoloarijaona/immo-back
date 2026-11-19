package com.tsix.immocrm.dto.projet.travaux;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Listes de choix de Travaux chantier, lisibles avec le droit de la liste.")
public record ReferentielsTravauxDto(
        @Schema(description = "Types de travaux avec leurs valeurs par défaut, par code.") List<TypeTravauxDto> types,
        @Schema(description = "Statuts de l'entité « Jour travaillé », par ordre (libellés des cases à cocher).")
        List<StatutJourTravailleDto> statuts,
        @Schema(description = "Vrai si l'appelant voit le statut de départ : sinon la création répond 403.")
        boolean departAutorise,
        @Schema(description = "Zones (CfgProjectZone), par code.")
        List<ReferentielItemDto> zones,
        @Schema(description = "Niveaux de nivellement du projet demandé ; vide sans projet ou projet non habilité.")
        List<ReferentielItemDto> niveaux,
        @Schema(description = "Articles (BuyItem), par code.") List<ReferentielItemDto> articles,
        @Schema(description = "Personnel (CfgTier de type employé).") List<ReferentielItemDto> personnel) {

    @Schema(description = "Type de travaux (GrhWorkedDayType) : son choix recopie unités, prix et TVA dans le jour.")
    public record TypeTravauxDto(
            @Schema(description = "GrhWorkedDayType.PKey.") UUID id,
            @Schema(description = "Code.") String code,
            @Schema(description = "Libellé.") String libelle,
            @Schema(description = "Groupe (GroupDescription).") String groupe,
            @Schema(description = "Unité par défaut.") String unite,
            @Schema(description = "Prix unitaire HT par défaut.") BigDecimal prixUnitaireHT,
            @Schema(description = "TVA par défaut (fraction).") BigDecimal tva,
            @Schema(description = "Unité 1 par défaut.") String unite1,
            @Schema(description = "Prix unitaire HT 1 par défaut.") BigDecimal prixUnitaireHT1,
            @Schema(description = "TVA 1 par défaut (fraction).") BigDecimal tva1) {
    }
}
