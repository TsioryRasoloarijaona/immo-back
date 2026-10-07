package com.tsix.immocrm.dto.prospection;

import com.fasterxml.jackson.annotation.JsonFilter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clés sérialisées selon {@code ?colonnes=} (liste blanche :
 * {@link com.tsix.immocrm.domain.ColonneProspection}) ; sans ce paramètre, le groupe activité vaut
 * {@code null} et la vue légataire n'est pas interrogée.
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

        BigDecimal prix,

        @Schema(description = "Surface recherchée. **Mesurée morte : 0 ligne renseignée** sur "
                + "14 114. Même traitement que `prix`.")
        BigDecimal surface,

        UUID trancheId,
        // -- Libellé de la tranche principale (CfgTranche.Code/Description).
        String trancheCode,
        String trancheDescription,

        // -- Contact et relance : voir instructions/FEATURE-prospection-contact-alerte.md.
        @Schema(example = "0612345678")
        String telephone,

        @Schema(example = "client@exemple.ma")
        String email,

        LocalDateTime derniereAlerteDate,

        @Schema(example = "rappeler la cliente lundi matin")
        String derniereAlerteMessage,

        // -- Groupe « activité » (ComSaleProspectionView) : null tant que ?colonnes= ne le
        //    demande pas, et la requête de vue n'est alors pas émise.

        @Schema(example = "1688")
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

        UUID dossierId,

        @Schema(description = "Code du dossier issu de la prospection. `null` si non transformée.",
                example = "26/00314")
        String dossierCode,

        @Schema(description = "Statut du dossier issu de la prospection. `null` si non "
                + "transformée.", example = "CONCRETISATION")
        String statutDossier,

        // -- Colonnes du logiciel de bureau servies par les entités, hors sélection par défaut.

        @Schema(description = "Code du prospect (`CfgTier.Code`). 100 %.")
        String prospectCode,

        String mobileProspect,

        @Schema(description = "Indicatif pays du mobile. 100 %.", example = "212")
        String indicatifMobile,

        @Schema(description = "Email **brut de la fiche**, sans le repli appliqué par `email`. "
                + "59,1 %.")
        String emailFiche,

        @Schema(description = "Code du commercial (`CfgTier.Code`). 99,3 % — la liste n'exposait "
                + "que son nom.")
        String commercialCode,

        @Schema(description = "Le prospect est un ancien client. 100 %. Déjà en fiche, absent de "
                + "la liste jusqu'ici.")
        Boolean ancienClient,

        @Schema(description = "Date du dernier archivage. 62,9 %. **Conservée après désarchivage** "
                + "— lire `fermee` pour l'état.")
        LocalDateTime dateFermeture,

        @Schema(description = "Motif de clôture. 60,5 %.", example = "Hors budget")
        String raisonFermeture,

        @Schema(description = "Type du prospect (`CfgTierType`). 100 %.", example = "PRO")
        String typeProspect,

        @Schema(description = "Support de l'origine. 50,2 %.", example = "FACEBOOK")
        String origineSupport,

        @Schema(description = "Action commerciale (campagne) rattachée. 40,2 %.")
        String actionCommerciale,

        @Schema(description = "Pays du prospect. 27,3 %.")
        String pays,

        UUID societeId,

        @Schema(description = "Code de la société. 99,8 %.", example = "ADMOD")
        String societeCode,

        @Schema(description = "Libellé de la société. 99,8 %.")
        String societeLibelle,

        @Schema(description = "Auteur de la création. **34,3 % seulement** — les prospections "
                + "créées par notre API n'en portent pas (bug #41).")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 100 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 100 %.")
        LocalDateTime modifieLe,

        // -- Type de document du légataire (StrEntity), hors sélection par défaut.
        //    Voir instructions/FEATURE-strentity-prospection.md.

        @Schema(description = "Identifiant du type de document légataire (`StrEntity`). 100 % — "
                + "sauf 4 lignes en base réelle.")
        UUID typeProspectionId,

        @Schema(example = "ComProspection")
        String typeProspectionCode,

        @Schema(description = "Libellé d'affichage du type, traduit. Ne pas s'en servir pour "
                + "tester le type.",
                example = "Prospection vente")
        String typeProspection
) {

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes. */
    public static final String FILTRE_COLONNES = "colonnesProspection";
}
