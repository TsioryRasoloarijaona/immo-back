package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/**
 * {@code AdmCountry} row. {@code indicatifTelephone} is parsed from the DB's text
 * {@code PhoneCode} column (e.g. "261") — null if the stored value isn't purely numeric.
 */
public record PaysDto(
        UUID id,
        String code,
        String isoAlpha3,
        String description,
        Integer indicatifTelephone
) {
}
