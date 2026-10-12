package com.tsix.immocrm.domain.note;

import com.tsix.immocrm.exceptions.ChampInvalideException;

import java.util.Arrays;
import java.util.Locale;

/** Fiche qui porte une note : son id va dans {@code AdmMessage.CategoryId}. */
public enum PorteurNote {
    PROJET("projet"),
    TRANCHE("tranche"),
    ARTICLE("article");

    public static final String CHAMP = "porteur";

    private final String code;

    PorteurNote(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** 400 {@code champ = porteur} hors des trois valeurs. */
    public static PorteurNote depuis(String valeur) {
        String cherche = valeur == null ? "" : valeur.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(p -> p.code.equals(cherche))
                .findFirst()
                .orElseThrow(() -> new ChampInvalideException(CHAMP,
                        "porteur doit valoir projet, tranche ou article : " + valeur));
    }
}
