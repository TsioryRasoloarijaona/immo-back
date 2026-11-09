package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Projet de chantier (PrjProject) vu par l'écran de configuration, tous projets confondus.")
public record ProjetConfigDto(
        @Schema(description = "PrjProject.PKey.") UUID id,
        @Schema(description = "Code, unique (casse et espaces ignorés).", example = "ARS-PRJ") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Description détaillée (DescriptionLong), 500 caractères au plus.") String descriptionLongue,
        @Schema(description = "Adresse du chantier, 500 caractères au plus.") String adresse,
        @Schema(description = "Notes, 1000 caractères au plus.") String notes,
        @Schema(description = "Société (CfgCompany.PKey).") UUID societeId,
        @Schema(description = "Code de la société.") String societeCode,
        @Schema(description = "Type d'objet du workflow (StrEntity « PrjProject »), posé par le serveur.") UUID entiteId,
        @Schema(description = "Code du type d'objet (PrjProject).") String entiteCode,
        @Schema(description = "Statut (StrStatus.PKey), posé par le serveur : « Nouveau » à la création.") UUID statutId,
        @Schema(description = "Code du statut.") String statutCode,
        @Schema(description = "Gestionnaire (CfgTier du personnel), null si aucun.") UUID gestionnaireId,
        @Schema(description = "Nom du gestionnaire, null si aucun.") String gestionnaireNom,
        @Schema(description = "Projet ventilé (IsVentilated).") boolean ventile,
        @Schema(description = "Taux de ventilation par défaut (defaultVentilationRate), 0 par défaut.") BigDecimal tauxVentilationDefaut,
        @Schema(description = "Parties communes (IsACommon).") boolean partiesCommunes,
        @Schema(description = "Calculé : vrai si le code commence par une année suivie d'un tiret (projet comptable d'exercice).")
        boolean anneeSociete,
        @Schema(description = "Nombre de lots (PrjMarket) du projet.") long nbLots,
        @Schema(description = "Nombre de pièces d'achat (BuyDocument) du projet.") long nbPiecesAchat,
        @Schema(description = "Date de création (UserCreateDateTime), null si inconnue.") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création (UserCreate).") String creePar,
        @Schema(description = "Date de dernière modification (UserUpdateDateTime).") LocalDateTime dateModification,
        @Schema(description = "Auteur de la dernière modification (UserUpdate).") String modifiePar
) {
}
