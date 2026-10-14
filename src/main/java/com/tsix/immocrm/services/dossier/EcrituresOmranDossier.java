package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.services.dossier.workflow.WorkflowDossier;
import org.springframework.stereotype.Component;

/** Composants des écritures compatibles OMRAN d'un dossier, regroupés pour l'injection dans {@link DossierService}. */
@Component
public record EcrituresOmranDossier(CreationDossierOmran creation, HistoriqueDossier historique,
                                    LigneEcheancierOmran ligneEcheancier, ControleSortieDossier controleSortie,
                                    MontantsDossier montants, WorkflowDossier workflow) {
}
