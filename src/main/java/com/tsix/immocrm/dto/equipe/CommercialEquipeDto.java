package com.tsix.immocrm.dto.equipe;

import com.tsix.immocrm.dto.kpi.PerformanceCommercialDto;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Un commercial de l'équipe : état de son portefeuille à ce jour et performances de la période.")
public record CommercialEquipeDto(

        @Schema(description = "Compte (AdmUser.PKey).")
        UUID admUserId,

        @Schema(description = "Identifiant de connexion (AdmUser.Code).", example = "f.saoud")
        String code,

        @Schema(description = "Nom affiché : description du compte, à défaut son code.", example = "Faycal SAOUD")
        String nom,

        @Schema(description = "Employé (CfgTier) du compte : la valeur à envoyer comme `commercialId`. `null` = compte "
                + "sans employé, qui ne peut recevoir aucune prospection.")
        UUID commercialId,

        @Schema(description = "Ses projets dans le périmètre de l'appelant. Vide = aucun projet affecté (sans restriction).")
        List<ProjetPerimetreDto> projets,

        long portefeuilleOuvert,

        long sansActionDepuis7Jours,

        long relancesEnRetard,

        @Schema(description = "Performances de la période (`du`/`au`), calculées par `GET /api/kpi/commerciaux` sur le "
                + "même périmètre de projets : dossiers, taux prospection → dossier, appels…")
        PerformanceCommercialDto performance
) {
}
