package com.tsix.immocrm.dto.hotline;

import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Top-level response for {@code GET /api/hotline/recherche}: everything the system knows about a
 * phone number, gathered in a single round trip so a hotline agent doesn't have to chase
 * separate prospect/prospection/dossier lookups while a caller is on the line.
 *
 * <p>Two independent sources are combined:
 * <ul>
 *   <li>{@link #fiches()} — one entry per existing {@code CfgTier} whose Phone/Phone1/Phone2/
 *       Mobile matches (normally 0 or 1, but several tiers can legitimately share a landline,
 *       e.g. a couple);</li>
 *   <li>{@link #appelsSansFiche()} — pré-prospections (a first phone contact captured before
 *       anyone created a formal {@code CfgTier} record) whose free-text phone field matches, and
 *       that aren't already linked to one of the tiers in {@link #fiches()}.</li>
 * </ul>
 */
@Schema(description = "Résultat de la recherche hotline par numéro de téléphone : la ou les "
        + "fiches correspondantes (s'il en existe) avec tout leur historique, plus les éventuels "
        + "appels antérieurs non encore rattachés à une fiche formelle.")
public record HotlineResultDto(

        @Schema(description = "Numéro de téléphone tel que saisi par l'agent, non modifié.",
                example = "06 12 34 56 78")
        String telephoneRecherche,

        @Schema(description = "Numéro normalisé effectivement utilisé pour la recherche : "
                + "uniquement les chiffres, 9 derniers chiffres conservés (indicatif pays "
                + "ignoré). Utile pour comprendre pourquoi deux numéros écrits différemment se "
                + "sont (ou non) rejoints.",
                example = "612345678")
        String telephoneNormalise,

        @Schema(description = "true si au moins une fiche (CfgTier) existante correspond à ce "
                + "numéro — c'est-à-dire si cette personne est déjà connue du système, pas "
                + "seulement un appelant jamais recontacté.")
        boolean connu,

        @Schema(description = "Une entrée par CfgTier existant dont un des numéros de téléphone "
                + "correspond, chacune avec tout son historique. Vide si personne n'est trouvé.")
        List<HotlineFicheDto> fiches,

        @Schema(description = "Pré-prospections (premiers contacts / démarchages) dont le numéro "
                + "de téléphone renseigné à l'époque correspond, mais qui n'ont jamais été "
                + "rattachées à une fiche CfgTier — donc absentes de la liste \"fiches\". "
                + "Signale un appelant déjà contacté par le passé mais jamais formellement créé "
                + "comme prospect/client.")
        List<ProspectionListItemDto> appelsSansFiche
) {
}
