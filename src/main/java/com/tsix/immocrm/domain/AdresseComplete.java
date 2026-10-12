package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Colonne « Adresse » des listes programme et tranche : {@code MainAddress} de {@code CfgProjectView} /
 * {@code CfgTrancheView}, soit ville, adresses 1 à 3, code postal, pays, rognés et séparés d'un espace, vides omis.
 */
public final class AdresseComplete {

    private AdresseComplete() {
    }

    public static String de(String ville, String adresse1, String adresse2, String adresse3, String codePostal,
                            String pays) {
        return Arrays.stream(new String[]{ville, adresse1, adresse2, adresse3, codePostal, pays})
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(partie -> !partie.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
