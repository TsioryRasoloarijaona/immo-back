package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchrone : publié depuis {@code afterCommit}, il recopie avant que la réponse parte, sinon le front relit l'ancien
 * état (FIX-rapatriement-synchrone.md). {@code @EventListener} et non {@code AFTER_COMMIT} : déjà en {@code afterCommit},
 * un écouteur transactionnel ne serait jamais appelé. La suite de tests (annulée) ne publie rien.
 */
@Component
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class RemonteeLectureListener {

    private static final Logger log = LoggerFactory.getLogger(RemonteeLectureListener.class);

    private final RemonteeLecture remonteeLecture;

    public RemonteeLectureListener(RemonteeLecture remonteeLecture) {
        this.remonteeLecture = remonteeLecture;
    }

    @EventListener
    public void surEcrituresValidees(EcrituresValidees evenement) {
        try {
            remonteeLecture.remonter(evenement.lignes());
        } catch (RuntimeException e) {
            // Levée en afterCommit, elle ferait échouer une requête dont l'écriture est pourtant validée sur la source.
            log.error("Rapatriement interrompu pour {} ligne(s).", evenement.lignes().size(), e);
        }
    }
}
