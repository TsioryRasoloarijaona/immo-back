package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Refus d'OMRAN à l'annulation ou au désistement d'un dossier ({@code Sale.cs:BeforeUpdate} et
 * {@code Verif}) : dossier de syndic encore en cours, ou facture de chiffre d'affaires sans avoir.
 */
@Component
public class ControleSortieDossier {

    static final String SYNDIC_EN_COURS = "Pour annuler ou résilier le dossier en cours, veuillez annuler le "
            + "dossier de syndic qui lui est attaché !";
    static final String FACTURE_SANS_AVOIR = "Il faut générer un avoir sur la facture du chiffre d'affaire "
            + "pour l'annuler !";

    private final ComFolderRepository folderRepository;

    public ControleSortieDossier(ComFolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    /** 409 avant toute écriture. */
    public void verifier(ComFolder folder) {
        if (folderRepository.compterSyndicsEnCours(folder.getId()) > 0) {
            throw new ConflictException(SYNDIC_EN_COURS);
        }
        BigDecimal solde = folderRepository.soldeFacturesNetDesAvoirs(folder.getId());
        if (solde != null && solde.signum() != 0) {
            throw new ConflictException(FACTURE_SANS_AVOIR);
        }
    }
}
