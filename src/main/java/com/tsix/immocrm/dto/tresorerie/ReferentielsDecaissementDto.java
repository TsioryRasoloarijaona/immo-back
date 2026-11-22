package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Listes de choix et droits de la fiche d'écriture d'un décaissement.")
public record ReferentielsDecaissementDto(
        @Schema(description = "Types de règlement et leurs contraintes, tri par code.") List<TypeDecaissementDto> types,
        @Schema(description = "Entités juridiques habilitées, tri par code.") List<SocieteDecaissementDto> societes,
        @Schema(description = "Comptes des sociétés habilitées, tri par code.") List<CompteDecaissementDto> comptes,
        @Schema(description = "Code de la devise par défaut (paramètre DefaultCurrency).", example = "MAD") String devise,
        @Schema(description = "Statuts proposés à la saisie : IN_WORK, POINTED, NOTPAYED.") List<String> statuts,
        @Schema(description = "Droits OMRAN du compte connecté sur l'écran.") DroitsDecaissementDto droits) {

    @Schema(description = "Type de règlement (StlSettlementType).")
    public record TypeDecaissementDto(
            UUID id,
            String code,
            @Schema(description = "Description du type.") String libelle,
            @Schema(description = "IsReferenceMandatory : référence obligatoire.") boolean referenceObligatoire,
            @Schema(description = "IsReferenceUnique : référence unique par type, sens et compte.") boolean referenceUnique,
            @Schema(description = "IsBankMandatory : compte obligatoire.") boolean banqueObligatoire,
            @Schema(description = "IsLawyerMandatory : notaire / avocat obligatoire.") boolean notaireObligatoire,
            @Schema(description = "IsTierUnique.") boolean tiersUnique,
            @Schema(description = "IsNotContabilized : type exclu de la comptabilité.") boolean exclu) {
    }

    @Schema(description = "Entité juridique (CfgCompany).")
    public record SocieteDecaissementDto(UUID id, String code, @Schema(description = "Raison sociale.") String libelle) {
    }

    @Schema(description = "Compte bancaire ou de caisse (StlAccount).")
    public record CompteDecaissementDto(
            UUID id,
            String code,
            @Schema(description = "Société du compte (CfgCompanyId).") UUID societeId,
            @Schema(description = "Code de la devise du compte.") String devise) {
    }

    @Schema(description = "Droits de la fiche (clés Yes.Stl.Front.Debit.SettlementList.*).")
    public record DroitsDecaissementDto(
            @Schema(description = "toolNew : créer et modifier.") boolean peutCreer,
            @Schema(description = "toolDelete : supprimer.") boolean peutSupprimer,
            @Schema(description = "CanSaveWithoutAffectation : enregistrer sans ligne.") boolean peutEnregistrerSansAffectation,
            @Schema(description = "CanModifyCompany : changer la société d'un règlement existant.") boolean peutChangerSociete) {
    }
}
