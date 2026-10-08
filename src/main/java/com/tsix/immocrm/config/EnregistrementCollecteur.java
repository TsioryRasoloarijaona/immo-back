package com.tsix.immocrm.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Branche {@link CollecteurLignesEcrites} sur la {@code SessionFactory} construite ; jamais en mono-base. */
@Component
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class EnregistrementCollecteur {

    private static final Logger log = LoggerFactory.getLogger(EnregistrementCollecteur.class);

    EnregistrementCollecteur(EntityManagerFactory fabrique, ApplicationEventPublisher evenements) {
        CollecteurLignesEcrites collecteur = new CollecteurLignesEcrites(evenements);
        EventListenerRegistry registre = fabrique.unwrap(SessionFactoryImplementor.class)
                .getEventListenerRegistry();
        registre.appendListeners(EventType.POST_INSERT, collecteur);
        registre.appendListeners(EventType.POST_UPDATE, collecteur);
        registre.appendListeners(EventType.POST_DELETE, collecteur);
        log.info("Collecte des lignes ecrites activee (POST_INSERT, POST_UPDATE, POST_DELETE).");
    }
}
