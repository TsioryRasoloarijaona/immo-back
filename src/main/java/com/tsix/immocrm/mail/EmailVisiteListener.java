package com.tsix.immocrm.mail;

import com.tsix.immocrm.services.prospection.ConfirmationVisiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Confirmation de visite après commit, en {@code @Async}. Aucune règle ici : tout est dans
 * {@link ConfirmationVisiteService}, que le renvoi manuel appelle aussi. Jamais déclenché sous la suite de tests.
 */
@Component
public class EmailVisiteListener {

    private static final Logger log = LoggerFactory.getLogger(EmailVisiteListener.class);

    private final ConfirmationVisiteService confirmationVisiteService;

    public EmailVisiteListener(ConfirmationVisiteService confirmationVisiteService) {
        this.confirmationVisiteService = confirmationVisiteService;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surVisitePlanifiee(VisitePlanifiee evenement) {
        try {
            confirmationVisiteService.envoyer(evenement.interactionId());
        } catch (RuntimeException e) {
            // Hors du fil de la requête : une exception laissée filer se perdrait dans l'exécuteur.
            log.error("Confirmation de visite non envoyée pour l'interaction {}.",
                    evenement.interactionId(), e);
        }
    }
}
