package com.tsix.immocrm.services.projet.travaux;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Conversion des colonnes d'une requête native SQL Server (uniqueidentifier, decimal, datetime). */
public final class ValeursNatives {

    private ValeursNatives() {
    }

    /** Libellé SQL d'un {@code CfgTier} d'alias donné : description, sinon nom prénom, sinon code. */
    public static String libelleTiers(String alias) {
        return "COALESCE(NULLIF(%1$s.Description, N''), NULLIF(LTRIM(RTRIM(CONCAT(%1$s.LastName, N' ', %1$s.FirstName))), N''), %1$s.Code)"
                .formatted(alias);
    }

    /** Condition « contient » sur le libellé d'un {@code CfgTier} d'alias donné. */
    public static String rechercheTiers(String alias) {
        return " OR %1$s.Description LIKE :recherche OR %1$s.LastName LIKE :recherche OR %1$s.FirstName LIKE :recherche"
                .formatted(alias);
    }

    public static UUID uuid(Object brut) {
        if (brut == null) {
            return null;
        }
        return brut instanceof UUID u ? u : UUID.fromString(String.valueOf(brut));
    }

    public static BigDecimal decimal(Object brut) {
        return brut instanceof Number n ? new BigDecimal(n.toString()) : null;
    }

    public static LocalDateTime moment(Object brut) {
        if (brut instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        return brut instanceof LocalDateTime d ? d : null;
    }

    public static LocalDate jour(Object brut) {
        LocalDateTime m = moment(brut);
        return m == null ? null : m.toLocalDate();
    }

    public static Integer entier(Object brut) {
        return brut instanceof Number n ? n.intValue() : null;
    }
}
