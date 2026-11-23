package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.services.achat.RequetesRapportsAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/** Dépendances communes des rapports Projet (FEATURE-n5b-rapports-projet.md). */
@Component
record SourcesRapportsProjet(LotsProjetService lots, RequetesRapportsAchat requetes, PerimetreProjetService perimetre,
                             EntityManager em) {
}
