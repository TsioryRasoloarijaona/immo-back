package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/**
 * {@code AdmCountry}. {@code indicatifTelephone} vient de la colonne texte {@code PhoneCode} :
 * null si la valeur stockée n'est pas purement numérique.
 */
public record PaysDto(
        UUID id,
        String code,
        String isoAlpha3,
        String description,
        Integer indicatifTelephone
) {
}
