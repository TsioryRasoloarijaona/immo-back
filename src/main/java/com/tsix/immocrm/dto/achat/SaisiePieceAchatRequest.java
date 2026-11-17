package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Saisie d'une pièce d'achat (création ou modification) ; taux en fraction (0,2 = 20 %).")
public record SaisiePieceAchatRequest(
        @Schema(description = "Nature : QUOTE, ORDER, DELIVRY ou INVOICE ; ignorée en modification.", example = "ORDER")
        String nature,
        @Schema(description = "Date de la pièce (DocDate), obligatoire.") LocalDate dateDocument,
        @Schema(description = "Échéance ; null = date + délai du lot, sinon du fournisseur.") LocalDate dateEcheance,
        @Schema(description = "Livraison souhaitée (PrevisionDeliveryDate) ; null = date de la pièce.")
        LocalDate dateLivraisonSouhaitee,
        @Schema(description = "Société habilitée (CfgCompanyId), obligatoire.") UUID societeId,
        @Schema(description = "Module (AdmModuleId, parmi referentiels.modules), obligatoire.") UUID moduleId,
        @Schema(description = "Fournisseur (CfgTierId), obligatoire.") UUID fournisseurId,
        @Schema(description = "Projet habilité (PrjProjectId) ; null = ventilation des projets obligatoire.") UUID projetId,
        @Schema(description = "Lot du projet (PrjMarketId) ou null.") UUID lotId,
        @Schema(description = "Type de marché (PrjMarketTypeId), obligatoire.") UUID typeMarcheId,
        @Schema(description = "Déclencheur (CfgTierTriggerId) ; absent en création = employé du compte, null en "
                + "modification = effacé.") UUID declencheurId,
        @Schema(description = "Personne à contacter ; '' si absente.") String personneAContacter,
        @Schema(description = "Référence interne (DocRef) ; '' si absente.") String referenceInterne,
        @Schema(description = "Référence bureau d'ordre (OfficeOrderRef) ; '' si absente.") String referenceBureauOrdre,
        @Schema(description = "Notes ; '' si absentes.") String notes,
        @Schema(description = "Notes 2 ; '' si absentes.") String notes2,
        @Schema(description = "Montant de régularisation (AmountRegul) ; 0 si absent.") BigDecimal montantRegul,
        @Schema(description = "Timbre fiscal (FiscalStamp) ; 0 si absent.") BigDecimal timbreFiscal,
        @Schema(description = "Catégorie d'en-tête (StlCategory), ventilée à 100 % si aucune ligne.") UUID categorieId,
        @Schema(description = "Lignes ; vide permis. En modification, la liste remplace les lignes existantes.")
        List<LigneSaisieAchat> lignes,
        @Schema(description = "Ventilation des projets (PrjProject), taux en fraction ; obligatoire sans projetId.")
        List<PartVentilationAchat> ventilationProjets,
        @Schema(description = "Pièces sources d'une génération, dans l'ordre ; null ou vide = création libre ; ignoré en "
                + "modification.") List<UUID> sources) {

    @Schema(description = "Ligne saisie d'une pièce d'achat (BuyDocumentLine).")
    public record LigneSaisieAchat(
            @Schema(description = "Id d'une ligne existante de la pièce ; null = nouvelle ligne.") UUID id,
            @Schema(description = "Code article.") String code,
            @Schema(description = "Désignation, obligatoire.") String description,
            @Schema(description = "Unité ; '' si absente.") String unite,
            @Schema(description = "Quantité, obligatoire.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT, obligatoire.") BigDecimal prixUnitaireHT,
            @Schema(description = "Remise en fraction [0;1] ; 0 si absente.") BigDecimal remise,
            @Schema(description = "FODEC en fraction [0;1] ; 0 si absent.") BigDecimal fodec,
            @Schema(description = "TVA en fraction [0;1] ; null = 0.") BigDecimal tva,
            @Schema(description = "Catégorie (StlCategory), obligatoire.") UUID categorieId,
            @Schema(description = "Notes de la ligne ; '' si absentes.") String notes,
            @Schema(description = "Ligne de bordereau du lot (PrjMarketLineId) recopiée d'une source ; null = ligne libre. "
                    + "Ignorée en modification.") UUID ligneBordereauId) {
    }

    @Schema(description = "Part d'une ventilation : clé et taux en fraction.")
    public record PartVentilationAchat(
            @Schema(description = "Id de la clé ventilée (projet PrjProject).") UUID cleId,
            @Schema(description = "Taux en fraction (0,25 = 25 %).") BigDecimal taux) {
    }
}
