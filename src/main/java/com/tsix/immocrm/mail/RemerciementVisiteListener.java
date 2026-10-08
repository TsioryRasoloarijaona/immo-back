package com.tsix.immocrm.mail;

import com.tsix.immocrm.services.prospection.RemerciementVisiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Remerciement après commit du passage d'étape, en {@code @Async} ; aucune règle ici. Jamais déclenché sous test. */
@Component
public class RemerciementVisiteListener {

    private static final Logger log = LoggerFactory.getLogger(RemerciementVisiteListener.class);

    private final RemerciementVisiteService remerciementVisiteService;

    public RemerciementVisiteListener(RemerciementVisiteService remerciementVisiteService) {
        this.remerciementVisiteService = remerciementVisiteService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surVisiteEffectuee(VisiteEffectuee evenement) {
        try {
            remerciementVisiteService.envoyer(evenement.prospectionId());
        } catch (RuntimeException e) {
            // Hors du fil de la requête : une exception laissée filer se perdrait dans l'exécuteur.
            log.error("Remerciement non envoyé pour la prospection {}.", evenement.prospectionId(), e);
        }
    }
}
