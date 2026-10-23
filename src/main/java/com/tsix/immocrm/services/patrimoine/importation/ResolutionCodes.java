package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Codes de référentiel d'un fichier → entités, chargés une fois par import. Casse ignorée ; par libellé seulement si la
 * colonne l'admet (OMRAN {@code ImportVerif}). Les types de présentation sont ceux de la tranche.
 */
final class ResolutionCodes {

    private final EntityManager em;
    private final UUID trancheId;
    private final Map<Class<?>, List<? extends CodeLibelleOmran>> charges = new HashMap<>();

    ResolutionCodes(EntityManager em, UUID trancheId) {
        this.em = em;
        this.trancheId = trancheId;
    }

    <T extends CodeLibelleOmran> Optional<T> trouver(Class<T> classe, String valeur, boolean parLibelle) {
        List<T> lignes = toutes(classe);
        Optional<T> parCode = lignes.stream().filter(l -> valeur.equalsIgnoreCase(l.getCode())).findFirst();
        if (parCode.isPresent() || !parLibelle) {
            return parCode;
        }
        return lignes.stream().filter(l -> valeur.equalsIgnoreCase(l.getDescription())).findFirst();
    }

    /** Lignes du référentiel triées par code (feuilles d'aide du modèle). */
    @SuppressWarnings("unchecked")
    <T extends CodeLibelleOmran> List<T> toutes(Class<T> classe) {
        return (List<T>) charges.computeIfAbsent(classe, c -> charger(classe));
    }

    private <T extends CodeLibelleOmran> List<T> charger(Class<T> classe) {
        CriteriaQuery<T> requete = em.getCriteriaBuilder().createQuery(classe);
        requete.select(requete.from(classe));
        return em.createQuery(requete).getResultList().stream()
                .filter(l -> !(l instanceof StkPresentationType p)
                        || Objects.equals(trancheId, Champs.de(p.getStkHierarchy(), CfgTranche::getId)))
                .sorted(Comparator.comparing(CodeLibelleOmran::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
