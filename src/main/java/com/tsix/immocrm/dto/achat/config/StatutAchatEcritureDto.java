package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Fiche complète d'un statut d'achat (POST et PUT) ; un PUT remplace la fiche entière, listes comprises.")
public record StatutAchatEcritureDto(
        @Schema(description = "Ordre (O.D.T.), obligatoire, non unique.", example = "12") Integer ordre,
        @Schema(description = "Code, obligatoire, ≤ 255, unique sans casse ni espaces de bord.", example = "TEST K2") String code,
        @Schema(description = "Libellé, obligatoire, ≤ 255.") String description,
        @Schema(description = "Type d'état (AdmWorkFlowStateType.PKey, liste /statuts/types), obligatoire.") UUID typeId,
        @Schema(description = "Durée de vie en jours, ≥ 0 ; absent = 0 (durée infinie).", example = "0") Integer dureeVieJours,
        @Schema(description = "Nom de couleur .NET (Color.FromName) ; vide = blanc ; un autre texte répond 400.", example = "Pink")
        String couleur,
        @Schema(description = "Validé(e) pour paiement ; absent = false.") Boolean okPourPaiement,
        @Schema(description = "Valider en comptabilité ; absent = false.") Boolean valideComptabilite,
        @Schema(description = "En lecture seule ; absent = false.") Boolean lectureSeule,
        @Schema(description = "Image de la bibliothèque (CfgImage.PKey, liste /images) ; absent = aucune.") UUID imageId,
        @Schema(description = "Notes, ≤ 1000.") String notes,
        @Schema(description = "Noms du catalogue piece (/statuts/champs-obligatoires) ; absent = aucun.")
        List<String> champsObligatoires,
        @Schema(description = "Noms du catalogue fournisseur (/statuts/champs-obligatoires) ; absent = aucun.")
        List<String> champsObligatoiresFournisseur,
        @Schema(description = "Documents à joindre, un type au plus une fois ; absent = aucun.")
        List<DocumentAJoindreAchatDto> documentsAJoindre
) {
    public StatutAchatEcritureDto {
        dureeVieJours = Objects.requireNonNullElse(dureeVieJours, 0);
        okPourPaiement = Boolean.TRUE.equals(okPourPaiement);
        valideComptabilite = Boolean.TRUE.equals(valideComptabilite);
        lectureSeule = Boolean.TRUE.equals(lectureSeule);
        champsObligatoires = Objects.requireNonNullElse(champsObligatoires, List.of());
        champsObligatoiresFournisseur = Objects.requireNonNullElse(champsObligatoiresFournisseur, List.of());
        documentsAJoindre = Objects.requireNonNullElse(documentsAJoindre, List.of());
    }
}
