package com.tsix.immocrm.dto.encaissement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Shape de {@code GET /api/encaissements/{id}} — la fiche d'un règlement.
 *
 * <h2>Pourquoi une forme distincte de celle de la liste</h2>
 *
 * <p>La fiche portait jusqu'ici le même {@code EncaissementDto} qu'une ligne de liste. Deux raisons
 * l'en séparent.
 *
 * <p><strong>1. Les affectations.</strong> L'écran légataire liste, dans le détail d'un règlement,
 * les échéances qu'il solde — c'est le menu qu'il nomme « Affectations ». La liste d'`EncaissementDto`
 * n'expose qu'un {@code dossierCode}, celui de la <em>première</em> ligne : sur les 8 règlements
 * réels qui couvrent plusieurs dossiers, ce champ en cache donc un ou deux. Le tableau ci-dessous
 * est la réponse complète.
 *
 * <p><strong>2. Le filtre de colonnes n'a pas sa place sur une fiche.</strong>
 * {@code EncaissementDto} porte un {@code @JsonFilter} : sans {@code ?colonnes=}, seules les
 * colonnes déclarées par défaut sont sérialisées. C'est le bon comportement pour une grille, dont
 * l'utilisateur choisit les colonnes ; c'en est un mauvais pour une fiche, qui doit tout dire.
 * Ce record n'est donc <strong>pas filtré</strong> : il sérialise l'intégralité de ses champs.
 *
 * <h2>Ce que cela change pour un client existant</h2>
 *
 * <p>La fiche renvoie désormais <em>plus</em> de clés qu'avant — les colonnes hors sélection par
 * défaut, qui étaient jusqu'ici retirées par le filtre, et le tableau {@code affectations}. Aucune
 * clé ne disparaît et aucune ne change de type. Un client qui refuse les champs inconnus à la
 * désérialisation doit être vérifié ; les autres ne voient qu'un enrichissement.
 *
 * <p>Corollaire : {@code ?colonnes=} n'a plus d'effet sur cette route. Il n'en a jamais eu d'usage
 * documenté — le sélecteur de colonnes est une fonction de grille — mais le paramètre était
 * techniquement accepté.
 *
 * <h2>La duplication des champs est gardée par un test</h2>
 *
 * <p>Les composants repris de {@link EncaissementDto} sont recopiés ici plutôt qu'imbriqués : un
 * {@code @JsonUnwrapped} aurait réintroduit le filtre de la liste, et une imbrication nue aurait
 * changé la forme de la réponse. Le risque est qu'un champ ajouté à la liste soit oublié ici.
 * C'est pourquoi {@code ficheEtListe_exposentLesMemesChamps} compare les deux jeux de composants
 * par réflexion : l'oubli casse un test, il ne dérive pas en silence.
 */
@Schema(description = "Fiche complète d'un règlement : ses champs, plus les échéances qu'il solde. "
        + "Contrairement aux lignes de la liste, cette réponse n'est pas filtrée par `?colonnes=`.")
public record EncaissementDetailDto(

        UUID id,
        /** {@code StlSettlement.Code} — "Référence" dans l'écran legacy (ex. n° de chèque). */
        String reference,
        String etat,
        /**
         * Dossier de la <strong>première</strong> ligne d'affectation, conservé pour que la fiche
         * garde la forme de la liste. Préférer {@link #affectations} dès qu'un règlement peut
         * couvrir plusieurs dossiers.
         */
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

        @Schema(description = """
                Les échéances que ce règlement solde, **montant affecté croissant** — l'ordre de \
                l'écran légataire, qui trie ses lignes par `Amount ASC`.

                Jamais `null` : un tableau vide signifie un règlement encaissé mais pas encore \
                imputé. Sur les données réelles, un règlement du périmètre Vente en porte de une \
                à trois.

                La somme des `montantAffecte` vaut `montantBrut - montantBrutRestant`.""")
        List<AffectationDto> affectations
) {

    /**
     * Compose la fiche depuis la forme de liste et ses affectations.
     *
     * <p>Recopie positionnelle, gardée par {@code ficheEtListe_exposentLesMemesChamps} — voir
     * l'entête.
     */
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
