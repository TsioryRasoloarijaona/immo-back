package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Une ligne de l'onglet Dossiers d'un article (vente, location ou syndic).")
public record ArticleDossierDto(
        OrigineDossier origine,
        @Schema(description = "PKey du dossier (ComFolder, RntFolder ou SynFolder selon origine).")
        UUID id,
        @Schema(description = "N° du dossier.")
        String code,
        @Schema(description = "Libellé du statut du dossier (StrStatus).")
        String statut,
        @Schema(description = "Nom du client du dossier.")
        String client,
        @Schema(description = "Code de l'article du dossier.")
        String articleCode,
        @Schema(description = "Entité juridique du programme de l'article (libellé).")
        String societe,
        @Schema(description = "Code de la tranche de l'article.")
        String tranche,
        @Schema(description = "Code du programme de l'article.")
        String programme,
        @Schema(description = "Catégorie de l'article (libellé).")
        String categorie,
        @Schema(description = "Type de l'article (libellé).")
        String type,
        @Schema(description = "Type de présentation de l'article (libellé).")
        String presentation,
        BigDecimal total,
        BigDecimal regle,
        BigDecimal reste
) {
}
