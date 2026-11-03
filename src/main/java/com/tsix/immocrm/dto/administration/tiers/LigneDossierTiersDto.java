package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dossier du tiers : vente (client), co-acquéreur (ComFolderCustomer) ou location (client).")
public record LigneDossierTiersDto(
        @Schema(description = "PKey du ComFolder ou du RntFolder.") UUID id,
        @Schema(description = "Rôle du tiers ; client et co-acquéreur du même dossier = Vente.",
                allowableValues = {"Vente", "Location", "Co-acquéreur"}) String nature,
        @Schema(description = "Code.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Date du dossier (DocDate), clé du tri.") LocalDateTime date,
        @Schema(description = "Libellé du statut.") String statut,
        @Schema(description = "Libellé de la tranche de l'article.") String tranche,
        @Schema(description = "Code de l'article.") String article,
        @Schema(description = "Libellé de la catégorie de l'article.") String categorie,
        @Schema(description = "Libellé du type de l'article.") String type,
        @Schema(description = "Total à payer (ComFolder.TotalToPay) ; null pour une location.") BigDecimal total,
        @Schema(description = "Fiche dossier pour une vente ou un co-acquéreur ; null pour une location.")
        LienOngletTiersDto lien) implements LigneOngletTiers {

    public static final String VENTE = "Vente";
    public static final String LOCATION = "Location";
    public static final String CO_ACQUEREUR = "Co-acquéreur";

    public LigneDossierTiersDto(UUID id, String nature, String code, String description, LocalDateTime date,
                                String statut, String tranche, String article, String categorie, String type,
                                BigDecimal total) {
        this(id, nature, code, description, date, statut, tranche, article, categorie, type, total,
                LOCATION.equals(nature) ? null : new LienOngletTiersDto("dossier", id));
    }
}
