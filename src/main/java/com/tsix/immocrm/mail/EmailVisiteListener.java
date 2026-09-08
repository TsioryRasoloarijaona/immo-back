package com.tsix.immocrm.mail;

import com.tsix.immocrm.services.prospection.ConfirmationVisiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envoie la confirmation de visite au prospect, une fois l'interaction réellement écrite.
 *
 * <p>Mêmes deux annotations que {@link EmailMotDePasseListener}, pour les mêmes raisons :
 * {@code AFTER_COMMIT} pour qu'un enregistrement annulé ne convoque personne, {@code @Async}
 * pour que la latence SMTP sorte du temps de réponse de {@code POST /interactions}.
 *
 * <p>Cet écouteur ne contient <b>aucune règle</b> : il ne fait que déclencher
 * {@link ConfirmationVisiteService}, que l'endpoint de renvoi appelle également. C'est
 * délibéré — la moindre logique ici serait une logique que le renvoi manuel n'appliquerait pas.
 *
 * <p><b>À savoir pour les tests</b> : un écouteur {@code AFTER_COMMIT} ne se déclenche
 * <b>jamais</b> sous la suite de ce dépôt, qui est {@code @Transactional} donc annulée. La
 * publication de l'événement se vérifie avec {@code @RecordApplicationEvents} ; l'envoi
 * lui-même s'éprouve en appelant le service directement.
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
            // Rien à propager : nous sommes hors du fil de la requête et hors de sa transaction.
            // Une exception laissée filer se perdrait dans l'exécuteur, sans trace exploitable.
            log.error("Confirmation de visite non envoyée pour l'interaction {}.",
                    evenement.interactionId(), e);
        }
    }
}
