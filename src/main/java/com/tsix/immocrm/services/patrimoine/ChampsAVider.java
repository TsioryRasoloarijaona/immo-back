package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@code champsAVider} d'un PATCH (FEATURE-patrimoine-tranche-fiche.md §1.1, §8.3) : un champ listé est remis à NULL
 * (date, clé étrangère), 0 (nombre), false (case) ou {@code ""} (texte, comme OMRAN) ; sinon règle « absent = inchangé ».
 */
public final class ChampsAVider {

    static final String CHAMP = "champsAVider";

    private final Set<String> noms;

    private ChampsAVider(Set<String> noms) {
        this.noms = noms;
    }

    /** Nom inconnu, obligatoire, ou aussi renseigné dans le corps → 400 nommant {@value #CHAMP}. */
    public static ChampsAVider de(Record saisie, List<String> demandes, Set<String> obligatoires) {
        if (demandes == null || demandes.isEmpty()) {
            return new ChampsAVider(Set.of());
        }
        for (String nom : demandes) {
            RecordComponent composant = Arrays.stream(saisie.getClass().getRecordComponents())
                    .filter(c -> c.getName().equals(nom) && !CHAMP.equals(nom) && !obligatoires.contains(nom))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException(
                            "%s : « %s » n'est pas un champ facultatif de la saisie.".formatted(CHAMP, nom)));
            if (valeur(composant, saisie) != null) {
                throw new InvalidRequestException(
                        "%s : « %s » est aussi renseigné dans le corps.".formatted(CHAMP, nom));
            }
        }
        return new ChampsAVider(Set.copyOf(demandes));
    }

    private static Object valeur(RecordComponent composant, Record saisie) {
        try {
            return composant.getAccessor().invoke(saisie);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    boolean contient(String nom) {
        return noms.contains(nom);
    }

    /** Date : NULL si vidée. */
    public <T> T ou(String nom, T saisie, T actuel) {
        return contient(nom) ? null : SaisiePatrimoine.ou(saisie, actuel);
    }

    Integer ou(String nom, Integer saisie, Integer actuel) {
        return contient(nom) ? Integer.valueOf(0) : SaisiePatrimoine.ou(saisie, actuel);
    }

    BigDecimal ou(String nom, BigDecimal saisie, BigDecimal actuel) {
        return contient(nom) ? BigDecimal.ZERO : SaisiePatrimoine.ou(saisie, actuel);
    }

    Boolean ou(String nom, Boolean saisie, Boolean actuel) {
        return contient(nom) ? Boolean.FALSE : SaisiePatrimoine.ou(saisie, actuel);
    }

    /** Texte : {@code ""} si vidé, à passer à {@link SaisiePatrimoine#texte}. */
    public String texte(String nom, String saisie) {
        return contient(nom) ? "" : saisie;
    }

    /** Clé étrangère : NULL si vidée, résolue si saisie, inchangée sinon. */
    public <T> T reference(String nom, UUID id, T actuel, Function<UUID, T> resoudre) {
        if (contient(nom)) {
            return null;
        }
        return id != null ? resoudre.apply(id) : actuel;
    }
}
