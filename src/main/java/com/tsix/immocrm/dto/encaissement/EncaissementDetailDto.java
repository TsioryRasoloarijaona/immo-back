package com.tsix.immocrm.dto.encaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fiche d'un règlement, non filtrée par {@code ?colonnes=}. Champs de {@link EncaissementDto} recopiés
 * ({@code @JsonUnwrapped} réintroduirait son filtre) ; un oubli casse
 * {@code ficheEtListe_exposentLesMemesChamps}. Voir FEATURE-affectations-encaissement.md §4.
 */
public record EncaissementDetailDto(

        UUID id,
        /** {@code StlSettlement.Code} — "Référence" dans l'écran legacy (ex. n° de chèque). */
        String reference,
        String etat,
        /** Dossier de la <strong>première</strong> affectation seulement : préférer {@link #affectations}. */
        UUID dossierId,
        /** Voir {@link #dossierId}. */
        String dossierCode,
        LocalDateTime dateDocument,
        LocalDateTime dateEcheance,
        LocalDateTime dateReglement,
        UUID tierId,
        String tierNom,
        BigDecimal montantBrut,
        BigDecimal montantBrutRestant,
        BigDecimal montantNet,
        Boolean comptabilise,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        UUID compteId,
        String compteCode,
        UUID depotId,
        String depotCode,
        UUID banqueId,
        String banqueNom,
        String notes,
        String description,
        BigDecimal montantBase,
        UUID tierPayeurId,
        String tierPayeurNom,
        Boolean remisAuCentre,
        String recuCode,
        LocalDateTime dateRemiseAuCentre,
        String bordereauCode,
        LocalDateTime dateBordereau,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe,

        List<AffectationDto> affectations
) {

    public static EncaissementDetailDto de(EncaissementDto base, List<AffectationDto> affectations) {
        return new EncaissementDetailDto(
                base.id(),
                base.reference(),
                base.etat(),
                base.dossierId(),
                base.dossierCode(),
                base.dateDocument(),
                base.dateEcheance(),
                base.dateReglement(),
                base.tierId(),
                base.tierNom(),
                base.montantBrut(),
                base.montantBrutRestant(),
                base.montantNet(),
                base.comptabilise(),
                base.typeId(),
                base.typeCode(),
                base.typeLibelle(),
                base.entiteId(),
                base.entiteCode(),
                base.entiteLibelle(),
                base.compteId(),
                base.compteCode(),
                base.depotId(),
                base.depotCode(),
                base.banqueId(),
                base.banqueNom(),
                base.notes(),
                base.description(),
                base.montantBase(),
                base.tierPayeurId(),
                base.tierPayeurNom(),
                base.remisAuCentre(),
                base.recuCode(),
                base.dateRemiseAuCentre(),
                base.bordereauCode(),
                base.dateBordereau(),
                base.creePar(),
                base.creeLe(),
                base.modifiePar(),
                base.modifieLe(),
                affectations);
    }
}
