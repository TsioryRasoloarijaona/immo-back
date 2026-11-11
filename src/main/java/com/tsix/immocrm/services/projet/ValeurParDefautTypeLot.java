package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.TypeLotParDefautDto;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * {@code GlobalParams.DefaultMarketType} d'OMRAN ({@code MarketTypeList.cs:btnSaveDefaults_Click}). SQL natif : l'entité
 * {@code AdmParamsGuid} est {@code @Immutable} et resterait en cache dans le contexte de persistance.
 */
@Service
@Transactional(readOnly = true)
public class ValeurParDefautTypeLot {

    private static final String CODE = "DefaultMarketType";

    private final EntityManager em;

    public ValeurParDefautTypeLot(EntityManager em) {
        this.em = em;
    }

    public TypeLotParDefautDto lire() {
        List<?> valeurs = em.createNativeQuery("SELECT CONVERT(varchar(36), CodeValue) FROM AdmParamsGuid WHERE Code = :code")
                .setParameter("code", CODE).getResultList();
        Object valeur = valeurs.isEmpty() ? null : valeurs.getFirst();
        return new TypeLotParDefautDto(valeur == null ? null : UUID.fromString(valeur.toString()));
    }

    @Transactional
    public TypeLotParDefautDto enregistrer(UUID typeLotId) {
        if (typeLotId != null && em.find(PrjMarketType.class, typeLotId) == null) {
            throw new InvalidRequestException("typeLotId : type de lot inconnu.");
        }
        int lignes = em.createNativeQuery("UPDATE AdmParamsGuid SET CodeValue = :valeur WHERE Code = :code")
                .setParameter("valeur", typeLotId).setParameter("code", CODE).executeUpdate();
        if (lignes == 0) {
            em.createNativeQuery("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (:code, :valeur, :id)")
                    .setParameter("code", CODE).setParameter("valeur", typeLotId).setParameter("id", UUID.randomUUID())
                    .executeUpdate();
        }
        return new TypeLotParDefautDto(typeLotId);
    }
}
