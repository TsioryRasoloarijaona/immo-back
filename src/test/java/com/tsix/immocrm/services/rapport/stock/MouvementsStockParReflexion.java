package com.tsix.immocrm.services.rapport.stock;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Appelle {@code services/rapport/stock/MouvementsStockTranche.lire(List<UUID>)} par réflexion sur les noms du §3.5 et de la
 * carte R2b de FEATURE-tdb-vente-rapports-rythme.md (record {@code MouvementStock} : tranche, article, dossier, libelle,
 * date, quantite) : les tests compilent avant le code. Contrat absent ⇒ {@link IllegalStateException}.
 */
public final class MouvementsStockParReflexion {

    public static final String COMPOSANT = "com.tsix.immocrm.services.rapport.stock.MouvementsStockTranche";
    public static final String LANCEMENT = "LANCEMENT DE LA COMMERCIALISATION";

    /** Une ligne de {@code MouvementStock} ; {@code tranche} tel que rendu (identifiant ou libellé). */
    public record Ligne(Object tranche, String article, String dossier, String libelle, LocalDate date, long quantite) {
    }

    private MouvementsStockParReflexion() {
    }

    public static List<Ligne> lire(ApplicationContext contexte, List<UUID> tranches) {
        Object composant;
        Method lire;
        try {
            Class<?> classe = Class.forName(COMPOSANT);
            composant = contexte.getBean(classe);
            lire = classe.getMethod("lire", List.class);
        } catch (ClassNotFoundException | NoSuchMethodException absent) {
            throw new IllegalStateException("contrat absent : MouvementsStockTranche.lire(List<UUID>)", absent);
        }
        Object resultat;
        try {
            resultat = lire.invoke(composant, tranches);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("lire", e);
        }
        List<Ligne> lignes = new ArrayList<>();
        for (Object m : (List<?>) resultat) {
            lignes.add(new Ligne(composante(m, "tranche"), (String) composante(m, "article"),
                    (String) composante(m, "dossier"), (String) composante(m, "libelle"),
                    (LocalDate) composante(m, "date"), Long.parseLong(String.valueOf(composante(m, "quantite")))));
        }
        return lignes;
    }

    private static Object composante(Object enregistrement, String nom) {
        if (!enregistrement.getClass().isRecord()) {
            throw new IllegalStateException("contrat absent : " + enregistrement.getClass().getSimpleName()
                    + " n'est pas un record");
        }
        for (RecordComponent c : enregistrement.getClass().getRecordComponents()) {
            if (c.getName().equals(nom)) {
                try {
                    Method accesseur = c.getAccessor();
                    accesseur.setAccessible(true);
                    return accesseur.invoke(enregistrement);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(nom, e);
                }
            }
        }
        throw new IllegalStateException("contrat absent : " + enregistrement.getClass().getSimpleName() + "." + nom);
    }
}
