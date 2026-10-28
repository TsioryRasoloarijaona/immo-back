package com.tsix.immocrm.services.patrimoine.impressions;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.modele.CreerModeleDocumentRequest;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.app.AppModeleDocumentRepository;
import com.tsix.immocrm.services.modele.InitialiseurModeles;
import com.tsix.immocrm.services.modele.ModeleDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Au démarrage : version 1 du modèle de lettre {@code RELANCE_2} si absent. Désactivé sous la suite de tests. */
@Component
@ConditionalOnProperty(name = "app.modeles.initialiser", havingValue = "true", matchIfMissing = true)
public class DemarrageRelance2 implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemarrageRelance2.class);

    private final AppModeleDocumentRepository modeles;
    private final ModeleDocumentService service;

    public DemarrageRelance2(AppModeleDocumentRepository modeles, ModeleDocumentService service) {
        this.modeles = modeles;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        String categorie = CategorieModele.RELANCE_TITRE_FONCIER.name();
        try {
            if (!modeles.existsByCategorieAndCode(categorie, ImpressionsTrancheService.CODE_MODELE)) {
                service.creer(new CreerModeleDocumentRequest(categorie, ImpressionsTrancheService.CODE_MODELE,
                        "Lettre de 2e relance", 20,
                        InitialiseurModeles.contenu(ImpressionsTrancheService.RESSOURCE_MODELE), null));
                log.info("Modèle {}/{} créé (version 1).", categorie, ImpressionsTrancheService.CODE_MODELE);
            }
        } catch (ConflictException e) {
            log.debug("Modèle {}/{} déjà présent.", categorie, ImpressionsTrancheService.CODE_MODELE);
        } catch (RuntimeException e) {
            // Table absente (script 017 non appliqué) : les lettres restent rendues par le texte livré.
            log.error("Initialisation du modèle de Relance 2 impossible.", e);
        }
    }
}
