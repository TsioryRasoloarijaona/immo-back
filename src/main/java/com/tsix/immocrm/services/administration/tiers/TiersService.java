package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.dto.administration.tiers.FicheTiersDto;
import com.tsix.immocrm.dto.administration.tiers.FiltresTiers;
import com.tsix.immocrm.dto.administration.tiers.LigneTiersDto;
import com.tsix.immocrm.dto.administration.tiers.NatureTiersDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TiersService {

    public PageDto<LigneTiersDto> lister(String famille, FiltresTiers filtres, int page, int size) {
        throw new UnsupportedOperationException();
    }

    public List<NatureTiersDto> natures(String famille) {
        throw new UnsupportedOperationException();
    }

    public List<ReferentielItemDto> referentiel(String liste) {
        throw new UnsupportedOperationException();
    }

    public FicheTiersDto lire(String famille, UUID id) {
        throw new UnsupportedOperationException();
    }

    public FicheTiersDto creer(String famille, EcritureTiersRequest corps) {
        throw new UnsupportedOperationException();
    }

    public FicheTiersDto modifier(String famille, UUID id, EcritureTiersRequest corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(String famille, UUID id) {
        throw new UnsupportedOperationException();
    }
}
