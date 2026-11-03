package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.stereotype.Component;

/** Aiguille l'écriture d'une fiche selon sa forme : l'API et l'import passent ici. */
@Component
record SaisieTiers(EcritureTiers complete, FicheReduiteTiers reduite, GardesPersonnel gardes) {

    void appliquer(CfgTier t, FamilleTiers famille, EcritureTiersRequest c) {
        switch (famille.fiche()) {
            case COMPLETE, PHYSIQUE -> complete.appliquer(t, famille, c);
            case REDUITE -> reduite.appliquer(t, famille, c);
        }
    }
}
