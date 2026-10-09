package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.TypeTiersProspect;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bascule prospect → client d'OMRAN, déclenchée par l'indicateur {@code StrStatus.BoolField011} du
 * statut cible. Seul {@code CfgTierTypeId} change ; jamais de retour client → prospect.
 */
@Component
public class TransformationProspectClient {

    public static final String MENTION_HISTORIQUE = "Prospect transformé en client";

    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierRepository tierRepository;

    public TransformationProspectClient(CfgTierTypeRepository tierTypeRepository, CfgTierRepository tierRepository) {
        this.tierTypeRepository = tierTypeRepository;
        this.tierRepository = tierRepository;
    }

    /** Vrai si le client du dossier vient de passer de prospect à client. */
    public boolean appliquer(ComFolder folder, StrStatus statutCible) {
        if (statutCible == null || !Boolean.TRUE.equals(statutCible.getTransformeProspectEnClient())) {
            return false;
        }
        CfgTier tiers = folder.getCfgCustomer();
        if (tiers == null || tiers.getCfgTierType() == null) {
            return false;
        }
        Optional<CfgTierType> typeClient = TypeTiersProspect
                .codeClientCorrespondant(tiers.getCfgTierType().getCode())
                .flatMap(tierTypeRepository::findByCode);
        if (typeClient.isEmpty()) {
            return false;
        }
        tiers.setCfgTierType(typeClient.get());
        tierRepository.save(tiers);
        return true;
    }
}
