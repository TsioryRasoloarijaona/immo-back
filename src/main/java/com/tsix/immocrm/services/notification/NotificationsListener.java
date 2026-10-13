package com.tsix.immocrm.services.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifications d'attribution après commit, sur leur propre fil et leur propre transaction : un échec n'annule
 * jamais l'attribution. Jamais déclenché sous la suite de tests (transactions annulées).
 */
@Component
public class NotificationsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationsListener.class);

    private final NotificationService notificationService;
    private final FluxNotifications flux;

    public NotificationsListener(NotificationService notificationService, FluxNotifications flux) {
        this.notificationService = notificationService;
        this.flux = flux;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surLeadsAttribues(LeadsAttribues evenement) {
        try {
            // Poussée après le retour : la transaction des notifications est validée (et recopiée) à ce stade.
            flux.diffuser(notificationService.notifierAttributions(evenement));
        } catch (RuntimeException e) {
            log.error("Notifications d'attribution non créées ({} lead(s)).", evenement.attribues().size(), e);
        }
    }
}
