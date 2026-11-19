package com.tsix.immocrm.services.projet.travaux;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Requête native à conditions facultatives, toutes en paramètres liés. */
public final class RequeteNative {

    private final StringBuilder sql;
    private final Map<String, Object> parametres = new HashMap<>();

    public RequeteNative(String select) {
        this.sql = new StringBuilder(select);
    }

    public RequeteNative condition(String condition) {
        sql.append(condition);
        return this;
    }

    /** Ajoute la condition seulement si la valeur est donnée. */
    public RequeteNative filtre(String condition, String nom, Object valeur) {
        if (valeur != null) {
            parametres.put(nom, valeur);
            sql.append(condition);
        }
        return this;
    }

    /** Restriction à un ensemble : vide = aucune ligne. */
    public RequeteNative parmi(String condition, String nom, Collection<?> valeurs) {
        return valeurs.isEmpty() ? condition(" AND 1 = 0") : filtre(condition, nom, valeurs);
    }

    /** « Contient » : terme vide ou absent = pas de filtre. */
    public RequeteNative contient(String condition, String terme) {
        String t = terme == null ? "" : terme.strip();
        return filtre(condition, "recherche", t.isEmpty() ? null : "%" + t + "%");
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> lignes(EntityManager em, String tri) {
        Query requete = em.createNativeQuery(sql.append(tri).toString());
        parametres.forEach(requete::setParameter);
        return requete.getResultList();
    }
}
