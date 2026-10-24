package com.tsix.immocrm.web.export;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.util.Arrays;

/** Format d'un export de liste ({@code ?format=}) ; absent = {@code xlsx}. */
public enum FormatExport {
    XLSX, PDF;

    /** @throws InvalidRequestException (400) si {@code format} n'est ni {@code xlsx} ni {@code pdf}. */
    public static FormatExport lire(String format) {
        if (format == null || format.isBlank()) {
            return XLSX;
        }
        return Arrays.stream(values()).filter(f -> f.name().equalsIgnoreCase(format.trim())).findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "format doit être 'xlsx' ou 'pdf' (reçu : " + format + ")"));
    }
}
