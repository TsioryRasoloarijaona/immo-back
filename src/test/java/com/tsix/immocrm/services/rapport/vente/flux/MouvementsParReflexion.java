package com.tsix.immocrm.services.rapport.vente.flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Appelle {@code FluxVenteMensuel.mouvements(DemandeFlux)} par réflexion sur les noms du §3.1 de
 * FEATURE-tdb-vente-rapports-rythme.md (records {@code MouvementFlux}, {@code MouvementsFlux}) : les tests compilent avant
 * le code. Contrat absent ⇒ {@link IllegalStateException}, jamais {@link IllegalArgumentException} (réservée au service).
 */
final class MouvementsParReflexion {

    /** Composantes de {@code MouvementFlux} : §3.1 + {@code ligneId} (décision du manager 25/10). */
    static final List<String> COMPOSANTES = List.of("mesure", "trancheId", "date", "valeur", "dossierId",
            "commercialId", "categorieArticleId", "sousArticle", "ligneId");

    /** Une ligne de {@code MouvementsFlux.lignes()}, mesure par son nom. */
    record Mouvement(String mesure, UUID trancheId, LocalDate date, BigDecimal valeur, UUID dossierId,
                     UUID commercialId, UUID categorieArticleId, boolean sousArticle, UUID ligneId) {
    }

    record Resultat(List<Mouvement> lignes, AnomaliesFlux anomalies) {

        List<Mouvement> de(String mesure) {
            return lignes.stream().filter(l -> l.mesure().equals(mesure)).toList();
        }
    }

    private final FluxVenteMensuel service;

    MouvementsParReflexion(FluxVenteMensuel service) {
        this.service = service;
    }

    /** Constante de {@link MesureFlux} ; absente ⇒ « contrat absent ». */
    static MesureFlux mesure(String nom) {
        try {
            return MesureFlux.valueOf(nom);
        } catch (IllegalArgumentException absente) {
            throw new IllegalStateException("contrat absent : MesureFlux." + nom, absente);
        }
    }

    Resultat mouvements(DemandeFlux demande) {
        Method methode;
        try {
            methode = FluxVenteMensuel.class.getMethod("mouvements", DemandeFlux.class);
        } catch (NoSuchMethodException absente) {
            throw new IllegalStateException("contrat absent : FluxVenteMensuel.mouvements(DemandeFlux)", absente);
        }
        Object resultat;
        try {
            resultat = methode.invoke(service, demande);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("mouvements", e);
        }
        List<Mouvement> lignes = new ArrayList<>();
        for (Object ligne : (List<?>) composante(resultat, "lignes")) {
            lignes.add(new Mouvement(((Enum<?>) composante(ligne, "mesure")).name(),
                    (UUID) composante(ligne, "trancheId"), (LocalDate) composante(ligne, "date"),
                    (BigDecimal) composante(ligne, "valeur"), (UUID) composante(ligne, "dossierId"),
                    (UUID) composante(ligne, "commercialId"), (UUID) composante(ligne, "categorieArticleId"),
                    Boolean.TRUE.equals(composante(ligne, "sousArticle")), (UUID) composante(ligne, "ligneId")));
        }
        return new Resultat(lignes, (AnomaliesFlux) composante(resultat, "anomalies"));
    }

    static Object composante(Object enregistrement, String nom) {
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
