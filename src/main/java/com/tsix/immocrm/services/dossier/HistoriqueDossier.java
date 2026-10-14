package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Écritures d'historique d'un dossier, dans les deux tables qu'alimente OMRAN ({@code StrFolderStatusHistory}
 * via {@code FolderStatusHistory.Historize}, {@code StrHistory}) ; {@code StrWorkflowId} = la transition paramétrée.
 */
@Component
public class HistoriqueDossier {

    public static final String AFFECTATION = "AFFECTATION";
    public static final String ANNULATION_AFFECTATION = "ANNULATION AFFECTATION";
    static final String CREATION = "Création";

    private final StrFolderStatusHistoryRepository statusHistoryRepository;
    private final StrHistoryRepository strHistoryRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public HistoriqueDossier(StrFolderStatusHistoryRepository statusHistoryRepository,
                             StrHistoryRepository strHistoryRepository, UtilisateurAffiche utilisateurAffiche,
                             Clock clock) {
        this.statusHistoryRepository = statusHistoryRepository;
        this.strHistoryRepository = strHistoryRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /**
     * Ligne {@code StrFolderStatusHistory} : statut (celui que prend le dossier), client, auteur et article
     * ({@code Historize(sale, itemId, description)}) ; {@code description} vide plutôt que NULL.
     */
    public void ligneStatut(ComFolder folder, StrStatus statut, StkItem article, String description) {
        StrFolderStatusHistory historique = new StrFolderStatusHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(LocalDateTime.now(clock));
        historique.setComFolder(folder);
        historique.setStrFolderStatus(statut);
        historique.setDescription(description != null ? description : "");
        historique.setAdmUserId(utilisateurAffiche.courant().compteId());
        historique.setCfgTier(folder.getCfgCustomer());
        historique.setStkItem(article);
        statusHistoryRepository.save(historique);
    }

    /** {@code StrHistory} d'un statut : {@code StrStatusDescription} = la Description du statut, pas son Code. */
    public void statutGenerique(ComFolder folder, StrStatus statut, String description) {
        statutGenerique(folder, statut, description, null, null);
    }

    /** Idem, avec la transition {@code StrWorkFlow} exécutée ({@code RessourceWithWorkFlow.ExecuteWorkflow}). */
    public void statutGenerique(ComFolder folder, StrStatus statut, String description, UUID workflowId,
                                String workflowDescription) {
        StrHistory generique = nouvelleLigne(folder, description);
        generique.setStrStatusId(statut.getId());
        generique.setStrStatusDescription(statut.getDescription());
        generique.setStrWorkflowId(workflowId);
        generique.setStrWorkflowDescription(workflowDescription);
        strHistoryRepository.save(generique);
    }

    /** Ligne {@code StrHistory} sans statut : un événement du dossier qui n'est pas un changement de statut. */
    public void evenement(ComFolder folder, String description) {
        strHistoryRepository.save(nouvelleLigne(folder, description));
    }

    private StrHistory nouvelleLigne(ComFolder folder, String description) {
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        StrHistory ligne = new StrHistory();
        ligne.setId(UUID.randomUUID());
        ligne.setDocDate(maintenant);
        ligne.setUserDate(maintenant);
        ligne.setObjectId(folder.getId());
        ligne.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        ligne.setAdmUserId(auteur.compteId());
        ligne.setAdmUserDescription(auteur.nom());
        ligne.setDescription(description);
        return ligne;
    }
}
