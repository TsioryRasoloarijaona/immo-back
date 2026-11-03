package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.dto.administration.tiers.LienTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LienTiersRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LiensTiersService {

    public List<LienTiersDto> lister(UUID tiersId) {
        throw new UnsupportedOperationException();
    }

    public LienTiersDto creer(UUID tiersId, LienTiersRequest corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID tiersId, UUID lienId) {
        throw new UnsupportedOperationException();
    }
}
