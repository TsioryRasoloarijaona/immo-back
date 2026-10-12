package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.ChampInvalideException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code champsAVider} des DTO d'écriture programme et tranche (FEATURE-patrimoine-tranche-fiche.md §1.1) : un PATCH
 * laisse inchangé un champ absent ou {@code null}, d'où une liste explicite pour remettre une date, une clé étrangère,
 * un texte ou un nombre à sa valeur « vide » d'OMRAN (NULL, {@code ''}, 0, false). Catalogue construit une fois.
 */
final class ChampsAVider<E> {

    static final String CHAMP = "champsAVider";

    private final Set<String> obligatoires;
    private final Map<String, Consumer<E>> videurs = new LinkedHashMap<>();

    ChampsAVider(Set<String> obligatoires) {
        this.obligatoires = Set.copyOf(obligatoires);
    }

    /** Texte qu'OMRAN initialise à {@code string.Empty} : vidé en {@code ""}, jamais NULL. */
    ChampsAVider<E> texte(String nom, BiConsumer<E, String> setter) {
        videurs.put(nom, e -> setter.accept(e, ""));
        return this;
    }

    /** Date ou clé étrangère facultative : NULL, comme OMRAN. */
    <T> ChampsAVider<E> nul(String nom, BiConsumer<E, T> setter) {
        videurs.put(nom, e -> setter.accept(e, null));
        return this;
    }

    /** Numérique ou booléen non nullable chez OMRAN : 0 ou false. */
    <T> ChampsAVider<E> valeur(String nom, BiConsumer<E, T> setter, T vide) {
        videurs.put(nom, e -> setter.accept(e, vide));
        return this;
    }

    /**
     * Contrôle toute la liste avant de vider quoi que ce soit.
     *
     * @throws ChampInvalideException (400, {@code champ = champsAVider}) : nom inconnu, champ obligatoire, ou champ
     *         à la fois renseigné dans le corps et listé
     */
    void appliquer(List<String> noms, Record demande, E entite) {
        if (noms == null || noms.isEmpty()) {
            return;
        }
        List<Consumer<E>> aAppliquer = noms.stream().map(nom -> videurValide(nom, demande)).toList();
        aAppliquer.forEach(videur -> videur.accept(entite));
    }

    private Consumer<E> videurValide(String nom, Record demande) {
        if (obligatoires.contains(nom)) {
            throw new ChampInvalideException(CHAMP,
                    "Le champ « %s » est obligatoire : il ne peut pas être vidé.".formatted(nom));
        }
        Consumer<E> videur = videurs.get(nom);
        if (videur == null) {
            throw new ChampInvalideException(CHAMP, "Champ inconnu dans champsAVider : « %s ».".formatted(nom));
        }
        if (valeurDuCorps(demande, nom) != null) {
            throw new ChampInvalideException(CHAMP,
                    "Le champ « %s » est à la fois renseigné et à vider.".formatted(nom));
        }
        return videur;
    }

    private static Object valeurDuCorps(Record demande, String nom) {
        RecordComponent composant = Arrays.stream(demande.getClass().getRecordComponents())
                .filter(c -> c.getName().equals(nom))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Champ vidable absent du DTO : " + nom));
        try {
            return composant.getAccessor().invoke(demande);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Lecture impossible du champ " + nom, e);
        }
    }
}
