package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sur quoi une fiche existante correspond à une identité saisie — voir
 * {@code instructions/FEATURE-doublons-prospect-projet.md} §2. Une seule correspondance suffit à
 * faire apparaître la fiche ; la liste dit à l'écran <em>pourquoi</em> elle apparaît.
 */
@Schema(description = "Critère sur lequel une fiche existante correspond à l'identité saisie. "
        + "`NOM` : prénom + nom (ou raison sociale) sans casse ni espaces, dans les deux ordres ; "
        + "`TELEPHONE` : 9 derniers chiffres d'un des numéros de la fiche ; `EMAIL` : sans casse.")
public enum CritereDoublon {
    NOM,
    TELEPHONE,
    EMAIL
}
