package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Charge d'un commercial pour la répartition : ses leads « à trier » de la Boîte de tri et de Ma journée (même
 * prédicat), sans le filtre de périmètre de l'appelant (FEATURE-repartition-notifications.md §3.1).
 */
@Service
@Transactional(readOnly = true)
public class ChargeCommerciale {

    private final ComProspectionRepository prospectionRepository;
    private final Clock clock;

    public ChargeCommerciale(ComProspectionRepository prospectionRepository, Clock clock) {
        this.prospectionRepository = prospectionRepository;
        this.clock = clock;
    }

    public long leadsATrier(UUID commercialId) {
        LocalDate depuis = LocalDate.now(clock).minusMonths(LignesParcours.MOIS_RECENTS);
        return prospectionRepository.count(LignesParcours.aTrier(commercialId, depuis)
                .and(LignesParcours.sansInteraction()));
    }
}
