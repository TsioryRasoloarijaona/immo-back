package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Déclenche le rapatriement une fois l'écriture validée sur la source de vérité.
 *
 * <p>{@code @Async} pour la même raison que {@code mail/EmailVisiteListener} : la latence de la
 * copie n'a pas à compter dans le temps de réponse de l'utilisateur.
 *
 * <p><b>Mais un {@code @EventListener} ordinaire, et non un
 * {@code @TransactionalEventListener(AFTER_COMMIT)}</b> comme ailleurs dans le dépôt. Ce n'est
 * pas une inattention : {@link CollecteurLignesEcrites} publie déjà depuis le rappel
 * {@code afterCommit} d'une synchronisation de transaction, parce que les lignes écrites ne sont
 * connues qu'après le vidage de session d'Hibernate — lequel a lieu <i>dans</i>
 * {@code doCommit()}, donc après que Spring a déroulé ses {@code beforeCommit}. Un écouteur
 * transactionnel enregistré à ce moment-là ne serait jamais appelé.
 *
 * <p>La garantie « seulement si la transaction a été validée » est donc portée par le point de
 * publication, pas par l'annotation. Une transaction annulée ne publie rien.
 *
 * <p>Cet écouteur ne contient <b>aucune règle</b> : il passe la main à {@link RemonteeLecture},
 * qu'un futur travail de rattrapage pourra appeler directement.
 *
 * <p><b>À savoir pour les tests</b> : ce rappel ne se déclenche <b>jamais</b> sous la suite de ce
 * dépôt, qui est {@code @Transactional} donc annulée — même limite que les écouteurs de
 * {@code mail/}. Le rapatriement s'éprouve en appelant {@link RemonteeLecture} directement.
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
    @EventListener
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
