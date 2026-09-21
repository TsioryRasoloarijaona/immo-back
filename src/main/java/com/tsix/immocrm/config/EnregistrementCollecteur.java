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

/**
 * Branche {@link CollecteurLignesEcrites} sur les événements Hibernate, au démarrage.
 *
 * <p>Enregistrement <b>après</b> construction de la {@code SessionFactory} plutôt que par un
 * {@code Integrator} au démarrage d'Hibernate : {@code SessionFactoryImplementor} expose
 * directement {@code getEventListenerRegistry()}, ce qui évite de passer par les propriétés de
 * bootstrap et la chaîne {@code IntegratorProvider}. Moins de surface, et une seule API à
 * suivre lors d'une montée de version.
 *
 * <p>Même condition d'activation que {@link DataSourceConfig} : sans seconde base, il n'y a
 * rien à rapatrier, donc rien à collecter. Le coût en mode mono-base est exactement nul —
 * l'écouteur n'est pas même enregistré.
 */
@Component
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class EnregistrementCollecteur {

    private static final Logger log = LoggerFactory.getLogger(EnregistrementCollecteur.class);

    public EnregistrementCollecteur(EntityManagerFactory fabrique, ApplicationEventPublisher evenements) {
        CollecteurLignesEcrites collecteur = new CollecteurLignesEcrites(evenements);
        EventListenerRegistry registre = fabrique.unwrap(SessionFactoryImplementor.class)
                .getEventListenerRegistry();
        registre.appendListeners(EventType.POST_INSERT, collecteur);
        registre.appendListeners(EventType.POST_UPDATE, collecteur);
        registre.appendListeners(EventType.POST_DELETE, collecteur);
        log.info("Collecte des lignes ecrites activee (POST_INSERT, POST_UPDATE, POST_DELETE).");
    }
}
