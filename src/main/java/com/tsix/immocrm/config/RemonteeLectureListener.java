package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * {@code @EventListener} et non {@code AFTER_COMMIT} : {@link CollecteurLignesEcrites} publie déjà depuis
 * {@code afterCommit}, où un écouteur transactionnel ne serait jamais appelé. Une transaction annulée ne publie rien ;
 * la suite de tests (annulée) ne le déclenche donc jamais : éprouver {@link RemonteeLecture} directement.
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
            // Hors du fil de la requête : une exception laissée filer se perdrait dans l'exécuteur.
            log.error("Rapatriement interrompu pour {} ligne(s).", evenement.lignes().size(), e);
        }
    }
}
