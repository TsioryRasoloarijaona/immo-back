package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * OMRAN {@code CfgCompany.SetRepartitions} : parts de chaque ligne d'un groupe, dans l'ordre donné. Chaque ligne sauf la
 * dernière reçoit {@code (int)(pourcentage × 100) × parts / 100} (troncatures entières), la dernière reçoit le reste.
 */
public final class RepartitionActionnaires {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private RepartitionActionnaires() {
    }

    public static List<Integer> repartir(List<BigDecimal> pourcentages, int nombreParts) {
        List<Integer> parts = new ArrayList<>();
        int distribue = 0;
        for (int i = 0; i < pourcentages.size() - 1; i++) {
            int part = pourcentages.get(i).multiply(CENT).intValue() * nombreParts / 100;
            parts.add(part);
            distribue += part;
        }
        if (!pourcentages.isEmpty()) {
            parts.add(nombreParts - distribue);
        }
        return parts;
    }
}
