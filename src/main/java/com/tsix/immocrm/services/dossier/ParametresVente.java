package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Seul lecteur des paramètres OMRAN des impayés et pénalités ({@code FEATURE-impayes.md} P4). Table ou ligne
 * absente ⇒ défaut, sans erreur SQL : l'existence est testée par {@code OBJECT_ID} avant toute lecture, sinon
 * l'échec marquerait la transaction appelante (un règlement) en rollback.
 */
@Component
public class ParametresVente {

    static final int RECOUVREMENT_PAR_DEFAUT = 90;

    private static final String TAUX_PENALITE = "ComPenaltyRatio";
    private static final String CATEGORIE_PENALITE = "DefaultComPenaltyDocumentType";
    private static final String AGE_RECOUVREMENT = "RecoveryAge";

    private static final String TABLE_EXISTE = "SELECT CASE WHEN OBJECT_ID(:table, 'U') IS NULL THEN 0 ELSE 1 END";
    private static final String LIRE_DECIMAL = "SELECT CodeValue FROM AdmParamsDecimal WHERE Code = :code";
    private static final String LIRE_ENTIER = "SELECT CodeValue FROM AdmParamsInteger WHERE Code = :code";

    private final EntityManager em;
    private final AdmParamsGuidRepository parametresGuid;

    public ParametresVente(EntityManager em, AdmParamsGuidRepository parametresGuid) {
        this.em = em;
        this.parametresGuid = parametresGuid;
    }

    /** {@code ComPenaltyRatio}, en fraction ; zéro si absent. */
    public BigDecimal tauxPenalite() {
        return premiereValeur("AdmParamsDecimal", LIRE_DECIMAL, TAUX_PENALITE)
                .map(v -> v instanceof BigDecimal b ? b : new BigDecimal(v.toString()))
                .orElse(BigDecimal.ZERO);
    }

    /** {@code DefaultComPenaltyDocumentType} : la catégorie des échéances pénalités ; vide si non posé. */
    public Optional<UUID> categoriePenalite() {
        return guid(CATEGORIE_PENALITE);
    }

    /** Valeur d'un paramètre {@code AdmParamsGuid}, lu par son {@code Code} ; vide si non posé. */
    public Optional<UUID> guid(String code) {
        return parametresGuid.findById(code).map(AdmParamsGuid::getCodeValue);
    }

    /** {@code RecoveryAge}, en jours ; 90 si absent. */
    public int ageRecouvrement() {
        return premiereValeur("AdmParamsInteger", LIRE_ENTIER, AGE_RECOUVREMENT)
                .map(v -> ((Number) v).intValue())
                .orElse(RECOUVREMENT_PAR_DEFAUT);
    }

    private Optional<Object> premiereValeur(String table, String requete, String code) {
        Number existe = (Number) em.createNativeQuery(TABLE_EXISTE).setParameter("table", table).getSingleResult();
        if (existe.intValue() == 0) {
            return Optional.empty();
        }
        List<?> valeurs = em.createNativeQuery(requete).setParameter("code", code).getResultList();
        return valeurs.stream().filter(Objects::nonNull).map(Object.class::cast).findFirst();
    }
}
