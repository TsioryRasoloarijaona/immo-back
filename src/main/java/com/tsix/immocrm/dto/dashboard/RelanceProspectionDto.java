package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une relance commerciale servie par {@code GET /api/dashboard/relances}.
 *
 * <p><b>À ne pas confondre avec {@link RelanceDto}</b>, qui porte le même mot mais désigne autre
 * chose : un <em>dossier</em> dont la fenêtre {@code ComFolder.Revival1}/{@code Revival2} a
 * expiré. Ces deux colonnes sont mortes en base réelle (0 ligne sur 358), donc
 * {@code DashboardDto.relancesEnRetard} est structurellement vide. Les relances décrites ici
 * sont les seules réelles — voir {@code instructions/FEATURE-dashboard-relances.md} §7.
 */
@Schema(description = "Une relance commerciale à traiter : l'échéance posée par un commercial sur "
        + "une interaction, encore armée. Une relance désarmée (prospection vendue) ou rattachée à "
        + "une échéance de paiement n'apparaît jamais ici.")
public record RelanceProspectionDto(

        @Schema(description = "Interaction qui porte la relance (`ComActionMessage`). C'est elle "
                + "qu'il faut viser pour désarmer ou reporter l'échéance, pas la prospection.",
                example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
        UUID interactionId,

        @Schema(description = "Prospection ou pré-prospection concernée.")
        UUID prospectionId,

        @Schema(description = "`true` s'il s'agit d'une pré-prospection — un lead non encore "
                + "transformé, de type légataire `ComPreProspection`, même discriminant que "
                + "`GET /api/pre-prospections`. Une pré-prospection saisie dans OMRAN peut déjà "
                + "porter un commercial : `nonAffectee` n'est donc pas systématique.")
        boolean preProspection,

        @Schema(description = "Nom du prospect. `null` sur une pré-prospection dont le prospect "
                + "n'a pas encore été créé — le `telephone` reste alors le seul moyen de rappel.")
        String prospectNom,

        @Schema(description = "Numéro joignable : le mobile de la fiche, à défaut son fixe, à "
                + "défaut le numéro saisi au premier contact. Aucune de ces sources ne suffit "
                + "seule ; ensemble elles couvrent 8 591 des 8 613 prospections ouvertes.")
        String telephone,

        @Schema(description = "Jour où la relance était due. La colonne légataire est un "
                + "`datetime` mais son heure ne porte aucune information — les 3 765 relances "
                + "armées sont toutes à minuit —, elle est donc ramenée au jour.",
                example = "2026-09-15")
        LocalDate dateRelance,

        @Schema(description = "Jours écoulés depuis l'échéance : `0` pour `echeance=AUJOURDHUI`, "
                + "de 1 à 30 pour `echeance=RETARD`.", example = "3")
        long joursDeRetard,

        @Schema(description = "Motif saisi par le commercial (`AlertMessage`). Fréquemment `null` "
                + "— une relance sans motif reste une relance à passer.")
        String motif,

        @Schema(description = "Commercial à qui la relance revient. Résolu par l'affectation de la "
                + "prospection d'abord (97,1 % des cas), à défaut par l'auteur de l'interaction. "
                + "`null` quand aucune des deux sources ne renseigne personne.")
        UUID commercialId,

        @Schema(description = "Nom du commercial résolu. `null` en même temps que `commercialId`.")
        String commercialNom,

        @Schema(description = "`true` quand la relance n'appartient à personne — 2,9 % des cas en "
                + "base réelle. **Ces lignes sont servies quelle que soit la `portee` demandée, y "
                + "compris `MOI`** : n'étant à personne, les réserver à un périmètre les rendrait "
                + "invisibles. À présenter comme une file à récupérer.")
        boolean nonAffectee,

        @Schema(description = "Date de l'interaction qui porte la relance — le dernier échange "
                + "connu avec ce prospect au moment où l'échéance a été posée.")
        LocalDateTime derniereInteractionDate
) {
}
