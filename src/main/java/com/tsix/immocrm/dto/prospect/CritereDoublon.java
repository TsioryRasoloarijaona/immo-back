package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

/** Voir instructions/FEATURE-doublons-prospect-projet.md §2. */
// Texte laissé dans l'annotation : un enum inline n'est pas un schéma de components/ (pas de .md possible).
@Schema(description = "Critère sur lequel une fiche existante correspond à l'identité saisie. "
        + "`NOM` : prénom + nom (ou raison sociale) sans casse ni espaces, dans les deux ordres ; "
        + "`TELEPHONE` : 9 derniers chiffres d'un des numéros de la fiche ; `EMAIL` : sans casse.")
public enum CritereDoublon {
    NOM,
    TELEPHONE,
    EMAIL
}
