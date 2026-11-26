package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Garde unique des écrivains de {@code CfgTierContact} pour le lien « Vendeur d'agence » (FEATURE-agences-vente.md R-INS1b). */
@Component
public class GardeLienVendeurAgence {

    private final CfgTierContactRepository liens;
    private final AgenceDuCompte agenceDuCompte;

    public GardeLienVendeurAgence(CfgTierContactRepository liens, AgenceDuCompte agenceDuCompte) {
        this.liens = liens;
        this.agenceDuCompte = agenceDuCompte;
    }

    public void verifier(CfgRelationType relation, UUID tiersId, UUID contactId) {
        if (relation == null || !RoleAgence.RELATION_VENDEUR.equals(relation.getCode())) {
            return;
        }
        boolean compteAgence = PerimetreCommercialService.admUserIdCourant()
                .map(agenceDuCompte::roles).map(RoleAgence::porte).orElse(false);
        if (compteAgence) {
            throw new ForbiddenException("Un compte agence ne modifie pas sa liste de vendeurs.");
        }
        if (liens.existsByCfgContactIdAndCfgRelationTypeCodeAndCfgTierIdNot(contactId, RoleAgence.RELATION_VENDEUR, tiersId)) {
            throw new ConflictException("Ce vendeur est déjà lié à une autre agence.");
        }
    }
}
