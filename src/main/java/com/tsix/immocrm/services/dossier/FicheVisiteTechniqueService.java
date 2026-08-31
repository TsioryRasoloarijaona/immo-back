package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.FicheVisiteTechniqueData;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Visite Technique" document (see
 * {@code FicheVisiteTechniquePdfRenderer} for the actual PDF rendering) — the pre-delivery
 * inspection checklist plus the "Service Après-Vente" contact clause.
 */
@Service
@Transactional(readOnly = true)
public class FicheVisiteTechniqueService {

    /**
     * Confirmed against the real template: tranche code "FAN" gets a project-manager-by-email-only
     * contact line, every other tranche gets the full tranche technical manager (title/name/mobile/
     * email) — same per-tranche branching pattern as {@code ActeReservationService}'s TER handling.
     */
    private static final String CODE_TRANCHE_CONTACT_EMAIL_SEUL = "FAN";

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;

    public FicheVisiteTechniqueService(ComFolderRepository folderRepository,
                                        ComFolderCustomerRepository folderCustomerRepository) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
    }

    public FicheVisiteTechniqueData collect(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        StkItem article = folder.getStkItem();
        CfgTranche tranche = article != null ? article.getStkHierarchy() : null;

        List<String> clientNoms = new ArrayList<>();
        if (folder.getCfgCustomer() != null) {
            clientNoms.add(nomAvecTitre(folder.getCfgCustomer()));
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            clientNoms.add(nomAvecTitre(coAcquereur.getCfgTier()));
        }

        return new FicheVisiteTechniqueData(
                folder.getCode(),
                folder.getReceptionDate(),
                clientNoms,
                article != null ? article.getCode() : null,
                tranche != null ? tranche.getSavAddress() : null,
                tranche != null ? contactSav(tranche) : null,
                tranche != null ? tranche.getWarrantyDelay() : null
        );
    }

    private static String contactSav(CfgTranche tranche) {
        CfgTier responsable = tranche.getCfgProject() != null ? tranche.getCfgProject().getCfgTechnicalManager() : null;
        if (responsable == null) {
            return null;
        }
        if (CODE_TRANCHE_CONTACT_EMAIL_SEUL.equalsIgnoreCase(tranche.getCode())) {
            return "merci de contacter le responsable de projet par mail : " + vide(responsable.getEMail());
        }
        String titre = responsable.getAdmTierTitle() != null ? responsable.getAdmTierTitle().getDescription() : "";
        String nom = responsable.getDescription() != null ? responsable.getDescription() : "";
        StringBuilder sb = new StringBuilder("merci de contacter le responsable de tranche ")
                .append(vide(titre)).append(' ').append(vide(nom));
        if (responsable.getMobile() != null) {
            sb.append(" au : 0").append(responsable.getMobile());
        }
        if (responsable.getEMail() != null) {
            sb.append(" ou par mail : ").append(responsable.getEMail());
        }
        return sb.toString();
    }

    private static String nomAvecTitre(CfgTier tier) {
        String titre = tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getCode() : null;
        String nom = tier.getDescription() != null && !tier.getDescription().isBlank() ? tier.getDescription() : null;
        if (nom == null) {
            String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
            String lastName = tier.getLastName() != null ? tier.getLastName() : "";
            nom = (prenom + " " + lastName).trim();
        }
        return titre != null ? titre + " " + nom : nom;
    }

    private static String vide(String value) {
        return value != null ? value : "";
    }
}
