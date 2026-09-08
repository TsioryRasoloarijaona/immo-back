package com.tsix.immocrm.mail;

import com.tsix.immocrm.services.prospection.RemerciementVisiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Remercie le prospect une fois le passage d'étape réellement écrit.
 *
 * <p>Mêmes deux annotations que {@link EmailVisiteListener}, pour les mêmes raisons :
 * {@code AFTER_COMMIT} pour qu'une étape annulée ne remercie personne, {@code @Async} pour que la
 * latence SMTP sorte du temps de réponse de {@code PATCH /etape}.
 *
 * <p>Aucune règle ici — l'écouteur ne fait que déclencher {@link RemerciementVisiteService}.
 *
 * <p><b>À savoir pour les tests</b> : un écouteur {@code AFTER_COMMIT} ne se déclenche jamais sous
 * la suite de ce dépôt, qui est {@code @Transactional} donc annulée. La publication se vérifie
 * avec {@code @RecordApplicationEvents} ; l'envoi s'éprouve en appelant le service directement.
 */
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
            // Hors du fil de la requête et de sa transaction : une exception laissée filer se
            // perdrait dans l'exécuteur, sans trace exploitable.
            log.error("Remerciement non envoyé pour la prospection {}.", evenement.prospectionId(), e);
        }
    }
}
