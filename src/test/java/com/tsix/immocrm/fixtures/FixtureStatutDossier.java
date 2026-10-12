package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Statut initial RESERVATION des dossiers, sans lequel {@code POST /api/dossiers} refuse la création
 * (500, comme OMRAN). Créé-si-absent, jamais supposé : ni {@code yesRemsTest} ni la base de CI n'en ont.
 */
@Component
public class FixtureStatutDossier {

    private final StrStatusRepository strStatusRepository;

    public FixtureStatutDossier(StrStatusRepository strStatusRepository) {
        this.strStatusRepository = strStatusRepository;
    }

    /** RESERVATION de l'entité dossier, existant ou créé (Description « Réservation », sans délai). */
    public StrStatus reservation() {
        return reservation(null);
    }

    /** Idem, avec un délai de validité en jours appliqué s'il est créé ici. */
    public StrStatus reservation(Integer delai) {
        StrStatus existant = strStatusRepository.findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, StatutDossier.RESERVATION.getCode());
        if (existant != null) {
            return existant;
        }
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(StatutDossier.RESERVATION.getCode());
        statut.setDescription("Réservation");
        statut.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        statut.setDelay(delai);
        return strStatusRepository.save(statut);
    }
}
