package com.tsix.immocrm.dto.prospection;

import com.fasterxml.jackson.annotation.JsonFilter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/prospections} and {@code GET /api/pre-prospections}.
 *
 * <p><strong>Toutes les clés ne sont pas toujours présentes.</strong> Le paramètre
 * {@code ?colonnes=} sélectionne celles qui sont sérialisées ; les autres sont absentes de la
 * réponse — voir {@link com.tsix.immocrm.domain.ColonneProspection}, qui fait aussi office de
 * liste blanche et alimente {@code GET /api/referentiels/colonnes/prospections}.
 *
 * <p>Sans ce paramètre, la réponse est <em>exactement</em> celle d'avant son introduction : les
 * champs du groupe activité valent {@code null} et le serveur n'interroge pas la vue légataire.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComProspection}. Adjust once a spec is available.
 */
@JsonFilter(ProspectionListItemDto.FILTRE_COLONNES)
public record ProspectionListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        UUID prospectId,
        String prospectNom,
        UUID commercialId,
        String commercialNom,
        String origine,
        String etape,
        Boolean fermee,

        @Schema(description = "Budget déclaré du prospect. **Mesuré mort : renseigné sur 4 lignes "
                + "sur 14 114**, et valant `0` — jamais `null` — partout ailleurs. Retiré de la "
                + "sélection par défaut pour cette raison ; reste demandable via `?colonnes=prix`. "
                + "Ne pas afficher comme un budget : ce serait annoncer « 0,00 DH » sur tout le "
                + "portefeuille.")
        BigDecimal prix,

        @Schema(description = "Surface recherchée. **Mesurée morte : 0 ligne renseignée** sur "
                + "14 114. Même traitement que `prix`.")
        BigDecimal surface,

        UUID trancheId,
        // -- Libellé de la tranche principale (CfgTranche.Code/Description), pour affichage sans
        // second appel à GET /api/tranches/{trancheId}.
        String trancheCode,
        String trancheDescription,

        // -- Contact et relance, pour qu'un commercial puisse appeler depuis la liste sans
        // ouvrir chaque fiche. Voir instructions/FEATURE-prospection-contact-alerte.md.
        @Schema(description = "Téléphone du prospect : celui de sa fiche (mobile, à défaut fixe) "
                + "et, à défaut, le numéro saisi lors du premier contact. Le repli n'est pas "
                + "décoratif — mesuré sur les 8 613 prospections ouvertes : 5 034 joignables par "
                + "la fiche, 7 596 par le champ de premier contact, **8 591 par l'une ou "
                + "l'autre**.",
                example = "0612345678")
        String telephone,

        @Schema(description = "Email du prospect, même règle de repli que `telephone`. Renseigné "
                + "sur 4 752 des 8 613 prospections ouvertes (55 %) — `null` est donc un cas "
                + "courant, pas une anomalie.",
                example = "client@exemple.ma")
        String email,

        @Schema(description = "Date de validité de la dernière alerte de relance posée sur cette "
                + "prospection, `null` s'il n'y en a aucune (3 236 des 8 613 ouvertes en ont "
                + "une). L'alerte provient toujours d'une **interaction** : la colonne d'alerte "
                + "portée par la prospection elle-même est morte en base — 0 ligne sur 8 613.")
        LocalDateTime derniereAlerteDate,

        @Schema(description = "Message de cette dernière alerte — en pratique la note du "
                + "commercial sur l'interaction qui l'a posée. `null` si aucune alerte.",
                example = "rappeler la cliente lundi matin")
        String derniereAlerteMessage,

        // ------------------------------------------------------------------
        // Groupe « activité » — calculé par la base légataire (ComSaleProspectionView).
        // Hors sélection par défaut : ces champs valent null tant que ?colonnes= ne les
        // demande pas, et la requête de vue n'est alors même pas émise.
        // ------------------------------------------------------------------

        @Schema(description = "Jours écoulés depuis la dernière action — le tri de relance du "
                + "logiciel de bureau. Renseigné sur 100 % des prospections de vente.",
                example = "1688")
        Integer joursInactivite,

        @Schema(description = "Nombre d'interactions journalisées. 97 %.", example = "12")
        Integer nbInteractions,

        @Schema(description = "Date de la dernière action, tous types confondus. 97 %.")
        LocalDateTime derniereActionDate,

        @Schema(description = "Date du dernier échange avec le prospect. 90 %.")
        LocalDateTime derniereCommunicationDate,

        @Schema(description = "Résultat du dernier échange. 90 %.",
                example = "Prospect n'est plus intéressé par un appartement")
        String derniereCommunicationResultat,

        @Schema(description = "Nombre de visites effectuées. 20 %.", example = "2")
        Integer nbVisites,

        @Schema(description = "Date de la dernière visite. `null` si aucune visite.")
        LocalDateTime derniereVisiteDate,

        @Schema(description = "Identifiant du projet — la liste n'exposait que la tranche. 99 %.")
        UUID projetId,

        @Schema(description = "Code du projet. 99 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 99 %.")
        String projetLibelle,

        @Schema(description = "CIN du prospect. 99 %.")
        String cin,

        @Schema(description = "Identifiant du dossier issu de la prospection. `null` sur 98 % des "
                + "lignes : c'est le taux de conversion réel, pas une donnée manquante.")
        UUID dossierId,

        @Schema(description = "Code du dossier issu de la prospection. `null` si non transformée.",
                example = "26/00314")
        String dossierCode,

        @Schema(description = "Statut du dossier issu de la prospection. `null` si non "
                + "transformée.", example = "CONCRETISATION")
        String statutDossier
) {

    /**
     * Identifiant du filtre Jackson qui porte la sélection de colonnes. Déclaré ici pour que la
     * constante suive le DTO qu'elle filtre.
     */
    public static final String FILTRE_COLONNES = "colonnesProspection";
}
