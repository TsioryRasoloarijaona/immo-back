package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Corps de {@code POST /api/prospects/{id}/contacts} et
 * {@code POST /api/prospections/{id}/contacts} — ajoute un contact (conjoint, notaire,
 * apporteur…) à un tiers.
 *
 * <p>Un contact est un lien {@code CfgTierContact} vers un autre {@code CfgTier} (la personne),
 * porteur d'un {@code CfgRelationType}. Deux façons de désigner la personne, exactement comme
 * l'écran légataire :
 * <ul>
 *   <li>{@code contactProspectId} — lier un tiers <b>déjà existant</b> ;</li>
 *   <li>{@code nouveauContact} — <b>créer la personne à la volée</b> (nom, prénom, mobile,
 *       téléphone, e-mail…) puis la lier. C'est ce que fait OMRAN : il insère un nouveau
 *       {@code CfgTier} puis le lien {@code CfgTierContact}.</li>
 * </ul>
 * Fournir l'un <b>ou</b> l'autre (400 si les deux sont absents, ou les deux présents).
 * {@code relationTypeId} est toujours requis.
 */
public record CreerContactProspectRequest(
        @Schema(description = "Tiers (CfgTier) existant à lier comme contact. Alternative à "
                + "`nouveauContact`.")
        UUID contactProspectId,

        @Schema(description = "Personne-contact à créer à la volée puis lier (flux OMRAN). "
                + "Alternative à `contactProspectId`. Au moins `nom` ou `raisonSociale` requis.")
        CreerProspectRequest nouveauContact,

        @Schema(description = "Type de relation (CfgRelationType → `CfgRelationTypeId`), p.ex. "
                + "« Conjoint », « Notaire ». **Obligatoire.**",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID relationTypeId
) {
}
