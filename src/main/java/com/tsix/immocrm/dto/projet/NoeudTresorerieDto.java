package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Nœud de l'arbre mensuel de la Trésorerie projet (racine, projet ou fournisseur).")
public record NoeudTresorerieDto(
        @Schema(description = "Clé opaque du nœud, à renvoyer telle quelle dans `noeud` de /cellule.") String cle,
        @Schema(description = "Libellé : « Encaissement » / « Décaissement », projet ou fournisseur.") String libelle,
        @Schema(description = "true sous la racine Encaissement (avoirs), false sous Décaissement.") boolean estCredit,
        @Schema(description = "Profondeur dans l'arbre, 0 pour les racines.") int niveau,
        @Schema(description = "12 montants, janvier en premier, positifs quel que soit le sens.") List<BigDecimal> montants,
        @Schema(description = "Somme des 12 mois.") BigDecimal total,
        @Schema(description = "Réglé : Σ des lignes dont la date (reportée) est avant aujourd'hui.") BigDecimal regle,
        @Schema(description = "Nœuds enfants ; vide pour une feuille.") List<NoeudTresorerieDto> enfants) {
}
