package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * R-C1 (FEATURE-agences-vente.md) : le contrat d'un dossier vendu par une agence n'est édité qu'après le pointage
 * d'un premier règlement, quel que soit le compte qui le demande.
 */
@Component
public record ContratApresPointage(ComFolderRepository dossiers, CfgTierContactRepository liens,
                                   StlSettlementLineRepository lignes) {

    public void verifier(UUID dossierId) {
        dossiers.findById(dossierId)
                .filter(d -> d.getCfgCommercial() != null
                        && !liens.tiersDuContact(d.getCfgCommercial().getId(), RoleAgence.RELATION_VENDEUR).isEmpty())
                .filter(d -> !lignes.existsReglementPointe(dossierId))
                .ifPresent(d -> {
                    throw new ConflictException(MESSAGE.formatted(d.getCode()));
                });
    }

    private static final String MESSAGE =
            "Dossier %s : le contrat d'une vente d'agence s'édite après le pointage d'un premier règlement";
}
