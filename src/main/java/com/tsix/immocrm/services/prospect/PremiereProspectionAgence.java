package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.prospect.ProspectionALaCreationRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.perimetre.AgenceDuCompte.Rattachement;
import com.tsix.immocrm.services.perimetre.ArticlesVisibles;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Un prospect créé par une agence naît avec sa prospection (FEATURE-agences-vente.md R-D7) ; le siège passe. */
// ObjectProvider : ProspectionService dépend déjà de ProspectService (cycle d'injection).
@Component
public record PremiereProspectionAgence(PerimetreCommercialService perimetre, ArticlesVisibles articlesVisibles,
                                        ObjectProvider<ProspectionService> prospections) {

    /** Contrôles avant toute écriture : 403 compte incomplet, 400 sans tranche, 404 tranche hors projets. */
    public Optional<Rattachement> verifier(ProspectionALaCreationRequest prospection) {
        Optional<Rattachement> agence = perimetre.rattachementAgenceComplet();
        if (agence.isPresent()) {
            if (prospection == null || prospection.trancheId() == null) {
                throw new InvalidRequestException("prospection.trancheId est requis pour un compte agence");
            }
            articlesVisibles.exigerTranche(prospection.trancheId());
        }
        return agence;
    }

    public void creer(CfgTier prospect, ProspectionALaCreationRequest prospection, Rattachement agence) {
        prospections.getObject().creerPremiereProspection(prospect, prospection, agence.vendeur());
    }
}
