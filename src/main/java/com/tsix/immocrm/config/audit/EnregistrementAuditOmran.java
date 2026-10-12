package com.tsix.immocrm.config.audit;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

/** Branche {@link EcouteurAuditOmran} sur la {@code SessionFactory}, en mono-base comme en deux bases. */
@Component
public class EnregistrementAuditOmran {

    EnregistrementAuditOmran(EntityManagerFactory fabrique, RedacteurAuditOmran redacteur) {
        EcouteurAuditOmran ecouteur = new EcouteurAuditOmran(redacteur);
        EventListenerRegistry registre = fabrique.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
        registre.appendListeners(EventType.POST_INSERT, ecouteur);
        registre.appendListeners(EventType.POST_UPDATE, ecouteur);
        registre.appendListeners(EventType.POST_DELETE, ecouteur);
    }
}
