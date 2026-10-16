package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** {@code etapesAtteignables} : calculées depuis {@link com.tsix.immocrm.domain.TransitionsValidees}, jamais stockées. */
public record DossierDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        List<String> etapesAtteignables,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        UUID avocatId,
        String avocatNom,
        List<ArticleSummaryDto> articles,
        BigDecimal prixAvantRemise,
        BigDecimal remise,
        BigDecimal remiseMontant,
        BigDecimal prixApresRemise,
        BigDecimal prix,
        BigDecimal montantAvance,
        BigDecimal totalAPayer,
        String notes,

        BigDecimal probabiliteFinalisation,

        String modePaie,

        // En-tête de l'écran « Dossier de vente », lu sur ComFolder — voir FEATURE-fiche-dossier-alignement-ecran.md §4.

        @Schema(description = "Prix unitaire au m² (`ComFolder.UnitCost`) — la case « Prix du m² ». "
                + "**98,3 %** des 358 dossiers réels.", example = "13000.00")
        BigDecimal prixM2,

        @Schema(description = "Prix de vente **hors taxes** (`ComFolder.PriceHT`), après remise. "
                + "Le pendant HT de `prix`. **98,3 %**.", example = "776250.00")
        BigDecimal prixHT,

        @Schema(example = "747500.00")
        BigDecimal prixAvantRemiseHT,

        BigDecimal tauxTVA,

        LocalDateTime dateValiditeStatut,

        BigDecimal quotePart,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        String prospectionCode,

        // Contexte patrimonial (article → tranche → projet → société) — voir FEATURE-contexte-dossier.md §2.1.

        UUID projetId,

        @Schema(description = "Code du projet. 100 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 100 %.")
        String projetLibelle,

        @Schema(description = "Identifiant de la tranche de l'article principal. 100 %.")
        UUID trancheId,

        @Schema(description = "Code de la tranche. 100 %.")
        String trancheCode,

        @Schema(description = "Libellé de la tranche. 100 %.")
        String trancheLibelle,

        @Schema(description = "Identifiant de la société / entité juridique. 100 %.")
        UUID societeId,

        @Schema(description = "Code de la société. 100 %.", example = "ADMOD")
        String societeCode,

        @Schema(description = "Libellé de la société. 100 %.")
        String societeLibelle,

        @Schema(description = "Identifiant de la vocation du dossier. 100 %.")
        UUID vocationId,

        @Schema(description = "Code de la vocation. 100 %.")
        String vocationCode,

        @Schema(description = "Libellé de la vocation. 100 %.")
        String vocationLibelle,

        // Piste d'audit — FEATURE-contexte-dossier.md §2.3.

        @Schema(description = "Auteur de la création du dossier. 100 % des 358 dossiers réels.")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 100 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 100 %.")
        LocalDateTime modifieLe,

        // Origine — FEATURE-contexte-dossier.md §2.4.

        String etapeProspection,

        @Schema(description = "Origine de la prospection. 99,2 %.", example = "FACEBOOK")
        String origineProspection,

        @Schema(description = "Date de la prospection d'origine. 99,2 %.")
        LocalDateTime dateProspection,

        // Responsable de vente et délais — voir FEATURE-colonnes-dossier-ecran-legataire.md.

        @Schema(description = "Id du responsable de vente (ComFolder.CfgSaleManagerId).")
        UUID responsableVenteId,

        @Schema(example = "Hamza ATCHANY")
        String responsableVenteNom,

        @Schema(description = "Date du compromis (ComFolder.AgreementDate). 73 / 358.")
        LocalDateTime dateCompromis,

        @Schema(description = "Date de livraison réelle (ComFolder.DeliveryDateReal). 63 / 358.")
        LocalDateTime dateLivraisonReelle,

        @Schema(example = "47")
        Integer delaiConcretisation,

        @Schema(description = "Jours entre la concrétisation et le compromis. 73 / 358.",
                example = "112")
        Integer delaiNotaire,

        @Schema(example = "365")
        Integer delaiLivraison,

        @Schema(description = "Création seulement : codes des autres prospections ouvertes du client, archivées par "
                + "elle (« Création dossier de vente »). Vide en lecture.")
        List<String> prospectionsArchivees
) {
}
