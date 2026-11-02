package com.tsix.immocrm.dto.administration;

import com.tsix.immocrm.domain.EtatFicheVerrouillee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste des fiches verrouillées par OMRAN.")
public record LigneFicheVerrouilleeDto(
        @Schema(description = "Identifiant du verrou (AdmWebLockEntities.PKey).") UUID id,
        @Schema(description = "Entité, code OMRAN tel quel (Settlement, COMFOLDER…).") String entite,
        @Schema(description = "Libellé de la fiche, tel qu'enregistré par OMRAN (Description).") String libelle,
        @Schema(description = "Date de verrouillage, ISO locale.", example = "2026-03-10T09:15:30") LocalDateTime dateVerrouillage,
        @Schema(description = "Utilisateur de la session (UserDesc) ; null si la session n'existe plus.") String utilisateur,
        @Schema(description = "État : State 1 = VERROUILLEE, toute autre valeur = NON_VERROUILLEE.") EtatFicheVerrouillee etat) {

    public LigneFicheVerrouilleeDto(UUID id, String entite, String libelle, LocalDateTime dateVerrouillage,
                                    String utilisateur, int state) {
        this(id, entite, libelle, dateVerrouillage, utilisateur, EtatFicheVerrouillee.depuis(state));
    }
}
