package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Une ligne de la liste des dossiers, remplie colonne par colonne puis convertie en {@link DossierListItemDto}.
 * Le DTO a une composante par {@link ColonneDossier}, de même nom : une colonne non posée vaut {@code null}.
 */
public final class LigneDossier {

    private static final ColonneDossier[] ORDRE;
    private static final Constructor<DossierListItemDto> CONSTRUCTEUR;

    static {
        Map<String, ColonneDossier> parNom = Arrays.stream(ColonneDossier.values())
                .collect(Collectors.toMap(c -> c.nom().toLowerCase(Locale.ROOT), Function.identity()));
        RecordComponent[] composantes = DossierListItemDto.class.getRecordComponents();
        ORDRE = new ColonneDossier[composantes.length];
        Class<?>[] types = new Class<?>[composantes.length];
        for (int i = 0; i < composantes.length; i++) {
            ColonneDossier colonne = parNom.get(composantes[i].getName().toLowerCase(Locale.ROOT));
            if (colonne == null) {
                throw new IllegalStateException("Composante sans colonne au catalogue : " + composantes[i].getName());
            }
            ORDRE[i] = colonne;
            types[i] = composantes[i].getType();
        }
        try {
            CONSTRUCTEUR = DossierListItemDto.class.getDeclaredConstructor(types);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private final EnumMap<ColonneDossier, Object> valeurs = new EnumMap<>(ColonneDossier.class);

    public LigneDossier met(ColonneDossier colonne, Object valeur) {
        valeurs.put(colonne, valeur);
        return this;
    }

    public Object valeur(ColonneDossier colonne) {
        return valeurs.get(colonne);
    }

    public DossierListItemDto versDto() {
        Object[] arguments = new Object[ORDRE.length];
        for (int i = 0; i < ORDRE.length; i++) {
            arguments[i] = valeurs.get(ORDRE[i]);
        }
        try {
            return CONSTRUCTEUR.newInstance(arguments);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Ligne de dossier inconstructible", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Type de valeur incompatible avec DossierListItemDto", e);
        }
    }

    /** Nombre de composantes du DTO ; le catalogue doit en avoir autant (test de cohérence). */
    public static int nombreDeComposantes() {
        return ORDRE.length;
    }
}
