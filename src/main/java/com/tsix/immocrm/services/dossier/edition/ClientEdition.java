package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.services.dossier.ArchivageProspectionsVente;
import com.tsix.immocrm.services.dossier.HistoriqueDossier;
import com.tsix.immocrm.services.dossier.TransformationProspectClient;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Changement du client principal ({@code toolModifyCustomer}) : ligne {@code StrFolderStatusHistory} comme
 * {@code Sale.cs:AfterUpdate} (client changé ⇒ Historize), puis type client / prospect du nouveau et de l'ancien
 * tiers par {@link TransformationProspectClient}.
 */
@Component
class ClientEdition {

    private final ComFolderCustomerRepository folderCustomerRepository;
    private final TransformationProspectClient transformation;
    private final HistoriqueDossier historique;
    private final EntityManager entityManager;
    private final ArchivageProspectionsVente archivage;

    ClientEdition(ComFolderCustomerRepository folderCustomerRepository, TransformationProspectClient transformation,
                  HistoriqueDossier historique, EntityManager entityManager, ArchivageProspectionsVente archivage) {
        this.folderCustomerRepository = folderCustomerRepository;
        this.transformation = transformation;
        this.historique = historique;
        this.entityManager = entityManager;
        this.archivage = archivage;
    }

    /**
     * {@code Sale.cs:AfterUpdate} → {@code ManageProspectionToArchive} quand le client a changé : ses prospections
     * ouvertes, hors celle du dossier, sont archivées ; rend leurs codes.
     */
    List<String> archiverProspectionsDuClient(ComFolder dossier) {
        return archivage.archiverCellesDe(List.of(dossier.getCfgCustomer().getId()),
                Champs.de(dossier.getComProspection(), ComProspection::getId));
    }

    /** Vrai si le client a changé. */
    boolean changer(ComFolder dossier, CfgTier nouveau) {
        CfgTier ancien = dossier.getCfgCustomer();
        if (ancien != null && ancien.getId().equals(nouveau.getId())) {
            return false;
        }
        boolean dejaAcquereur = folderCustomerRepository.findByComFolderId(dossier.getId()).stream()
                .map(ComFolderCustomer::getCfgTier)
                .filter(Objects::nonNull)
                .anyMatch(t -> t.getId().equals(nouveau.getId()));
        if (dejaAcquereur) {
            throw new InvalidRequestException("Ce tiers est déjà co-acquéreur du dossier : retirez-le d'abord.");
        }
        dossier.setCfgCustomer(nouveau);
        if (dossier.getStrStatus() != null) {
            historique.ligneStatut(dossier, dossier.getStrStatus(), dossier.getStkItem(), "");
        }
        // Les requêtes « dossier actif » de la synchronisation doivent voir le nouveau client.
        entityManager.flush();
        transformation.synchroniser(dossier).forEach(mention -> historique.evenement(dossier, mention));
        if (ancien != null) {
            synchroniserAncien(dossier, ancien);
        }
        return true;
    }

    /**
     * L'ancien client n'est plus sur le dossier : on le présente à la synchronisation par une copie détachée,
     * jamais enregistrée, qui porte l'id du dossier (ses co-acquéreurs, déjà synchronisés, ne changent plus).
     */
    private void synchroniserAncien(ComFolder dossier, CfgTier ancien) {
        ComFolder vue = new ComFolder();
        vue.setId(dossier.getId());
        vue.setCfgCustomer(ancien);
        transformation.synchroniser(vue)
                .forEach(mention -> historique.evenement(dossier, mention + " : " + TierNomFormatter.nom(ancien)));
    }
}
