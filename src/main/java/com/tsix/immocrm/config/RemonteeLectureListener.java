package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Déclenche le rapatriement une fois l'écriture réellement validée sur la source de vérité.
 *
 * <p>Mêmes deux annotations que {@code mail/EmailVisiteListener}, pour les mêmes raisons :
 * {@code AFTER_COMMIT} pour qu'une transaction annulée ne recopie rien — sa ligne n'existe
 * nulle part — et {@code @Async} pour que la latence de la copie sorte du temps de réponse.
 *
 * <p>Cet écouteur ne contient <b>aucune règle</b> : il ne fait que passer la main à
 * {@link RemonteeLecture}, qu'un futur travail de rattrapage pourra appeler directement. Toute
 * logique posée ici serait une logique que ce rattrapage n'appliquerait pas.
 *
 * <p><b>À savoir pour les tests</b> : un écouteur {@code AFTER_COMMIT} ne se déclenche
 * <b>jamais</b> sous la suite de ce dépôt, qui est {@code @Transactional} donc annulée. La
 * publication de l'événement se vérifie avec {@code @RecordApplicationEvents} ; le rapatriement
 * lui-même s'éprouve en appelant {@link RemonteeLecture} directement.
 *
 * <p>Conditionné comme {@link RemonteeLecture}, dont il dépend : en mode mono-base il n'existe
 * pas, et {@code EcrituresValidees} n'est de toute façon jamais publié.
 */
@Component
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class RemonteeLectureListener {

    private static final Logger log = LoggerFactory.getLogger(RemonteeLectureListener.class);

    private final RemonteeLecture remonteeLecture;

    public RemonteeLectureListener(RemonteeLecture remonteeLecture) {
        this.remonteeLecture = remonteeLecture;
    }

    @Async("remonteeExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surEcrituresValidees(EcrituresValidees evenement) {
        try {
            remonteeLecture.remonter(evenement.lignes());
        } catch (RuntimeException e) {
            // Rien à propager : nous sommes hors du fil de la requête et hors de sa transaction.
            // Une exception laissée filer se perdrait dans l'exécuteur, sans trace exploitable.
            log.error("Rapatriement interrompu pour {} ligne(s).", evenement.lignes().size(), e);
        }
    }
}
