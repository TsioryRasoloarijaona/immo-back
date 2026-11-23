package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.RetenuesLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import org.springframework.stereotype.Component;

/** R3 {@code PRJInsuranceTRCStatus} « ETAT DES ASSURANCES TOUS RISQUES CHANTIER ». */
@Component
public class EtatAssurancesTrcRapport extends EtatAssurancesRapport {

    public EtatAssurancesTrcRapport(SourcesRapportsProjet sources) {
        super(sources, "PRJInsuranceTRCStatus", "État des assurances tous risques chantier", PrjMarket::getInsuranceTRCRatio,
                RetenuesLotDto::assuranceTrc);
    }
}
