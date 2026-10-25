package com.tsix.immocrm.services.patrimoine.champs;

import com.tsix.immocrm.entities.adm.AdmSpecificField;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldDataType;
import com.tsix.immocrm.entities.adm.AdmSpecificFieldValue;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.utils.Champs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Saisie d'un champ spécifique contrôlée selon le {@code Code} de son type ; refus = 400 nommant le champ. */
record ValeurTypee(String texte, LocalDateTime date, Boolean booleen, BigDecimal decimal, Integer entier) {

    static final String LISTE = "Drop down";
    static final String SEPARATEUR = ";";
    private static final int TEXTE_MAX = 1000;
    private static final int CHIFFRES_ENTIERS_MAX = 20;
    private static final int ANNEE_MIN = 1753;

    static List<String> choix(String comboValue) {
        return comboValue == null ? List.of()
                : Arrays.stream(comboValue.split(SEPARATEUR)).map(String::trim).filter(c -> !c.isEmpty()).toList();
    }

    static ValeurTypee lire(AdmSpecificField champ, String saisie) {
        String brut = saisie == null ? "" : saisie.trim();
        if (brut.isEmpty()) {
            if (Boolean.TRUE.equals(champ.getRequired())) {
                throw new InvalidRequestException("La valeur du champ [%s] est obligatoire !!".formatted(champ.getCaption()));
            }
            return new ValeurTypee(null, null, null, null, null);
        }
        String type = Champs.de(champ.getDataType(), AdmSpecificFieldDataType::getCode);
        try {
            return switch (type == null ? "" : type) {
                case "Date" -> date(champ, brut);
                case "Decimal", "Money", "Percentage" -> decimal(champ, brut);
                case "Int" -> new ValeurTypee(brut, null, null, null, Integer.valueOf(brut));
                case "Bit" -> booleen(champ, brut);
                case LISTE -> dansLaListe(champ, brut);
                default -> texte(champ, brut);
            };
        } catch (DateTimeParseException | NumberFormatException e) {
            throw refus(champ, brut);
        }
    }

    void appliquer(AdmSpecificFieldValue ligne) {
        ligne.setValue(texte);
        ligne.setDateValue(date);
        ligne.setBoolValue(booleen);
        ligne.setDecimalValue(decimal);
        ligne.setIntValue(entier);
    }

    private static ValeurTypee date(AdmSpecificField champ, String brut) {
        LocalDate jour = LocalDate.parse(brut);
        if (jour.getYear() < ANNEE_MIN) {
            throw refus(champ, brut);
        }
        return new ValeurTypee(brut, jour.atStartOfDay(), null, null, null);
    }

    private static ValeurTypee decimal(AdmSpecificField champ, String brut) {
        BigDecimal nombre = new BigDecimal(brut).setScale(6, RoundingMode.HALF_UP);
        if (nombre.precision() - nombre.scale() > CHIFFRES_ENTIERS_MAX) {
            throw refus(champ, brut);
        }
        return new ValeurTypee(brut, null, null, nombre, null);
    }

    private static ValeurTypee booleen(AdmSpecificField champ, String brut) {
        String b = brut.toLowerCase(Locale.ROOT);
        if (!b.equals("true") && !b.equals("false")) {
            throw refus(champ, brut);
        }
        return new ValeurTypee(b, null, Boolean.valueOf(b), null, null);
    }

    private static ValeurTypee dansLaListe(AdmSpecificField champ, String brut) {
        if (!choix(champ.getComboValue()).contains(brut)) {
            throw refus(champ, brut);
        }
        return texte(champ, brut);
    }

    private static ValeurTypee texte(AdmSpecificField champ, String brut) {
        if (brut.length() > TEXTE_MAX) {
            throw new InvalidRequestException("La valeur du champ [%s] dépasse %d caractères."
                    .formatted(champ.getCaption(), TEXTE_MAX));
        }
        return new ValeurTypee(brut, null, null, null, null);
    }

    private static InvalidRequestException refus(AdmSpecificField champ, String brut) {
        String type = Champs.de(champ.getDataType(), AdmSpecificFieldDataType::getCode);
        return new InvalidRequestException("Valeur « %s » refusée pour le champ [%s] (type %s)."
                .formatted(brut, champ.getCaption(), type));
    }
}
