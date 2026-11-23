package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.RetenuesLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import org.springframework.stereotype.Component;

/** R2 {@code PRJInsuranceDECStatus} « ETAT DES ASSURANCES DECENALES ». */
@Component
public class EtatAssurancesDecennalesRapport extends EtatAssurancesRapport {

    public EtatAssurancesDecennalesRapport(SourcesRapportsProjet sources) {
        super(sources, "PRJInsuranceDECStatus", "État des assurances décennales", PrjMarket::getInsuranceDECRatio,
                RetenuesLotDto::assuranceDecennale);
    }
}
