package com.tsix.immocrm.services.modele;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Au démarrage : modèles livrés avec l'application. Désactivé sous la suite de tests ({@code app.modeles.initialiser}). */
@Component
@ConditionalOnProperty(name = "app.modeles.initialiser", havingValue = "true", matchIfMissing = true)
public class DemarrageModeles implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemarrageModeles.class);

    private final InitialiseurModeles initialiseur;

    public DemarrageModeles(InitialiseurModeles initialiseur) {
        this.initialiseur = initialiseur;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initialiseur.initialiserActeReservation();
            initialiseur.initialiserPremiereRelance();
        } catch (RuntimeException e) {
            // Table absente (script 017 non appliqué) : l'acte reste servi par le rendu codé en dur.
            log.error("Initialisation des modèles de documents impossible.", e);
        }
    }
}
