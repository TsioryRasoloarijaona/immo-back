package com.tsix.immocrm.services.rapport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Lecture bornée par des ids, par lots : SQL Server refuse plus de 2 100 paramètres liés par requête (RV2). */
public final class LotsTranches {

    static final int TAILLE_LOT = 2_000;

    private LotsTranches() {
    }

    /** Aucune requête si {@code ids} est vide : rien à lire, jamais « tout ». */
    public static <T> List<T> lire(List<UUID> ids, Function<List<UUID>, List<T>> lecture) {
        List<T> lignes = new ArrayList<>();
        for (int debut = 0; debut < ids.size(); debut += TAILLE_LOT) {
            lignes.addAll(lecture.apply(ids.subList(debut, Math.min(debut + TAILLE_LOT, ids.size()))));
        }
        return lignes;
    }

    /** Ids en texte pour un {@code IN (:ids)} natif ({@code uniqueidentifier}). */
    public static List<String> textes(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }
}
