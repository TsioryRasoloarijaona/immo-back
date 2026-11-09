package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.LotListItemDto;
import com.tsix.immocrm.dto.projet.ReferentielsProjetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LotsProjetService {

    public ReferentielsProjetDto referentiels() {
        throw new UnsupportedOperationException();
    }

    public Page<LotListItemDto> lots(FiltresLots filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public LotDetailDto lot(UUID id) {
        throw new UnsupportedOperationException();
    }

    public BordereauDto bordereau(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<DecompteListItemDto> decomptesDuLot(UUID id) {
        throw new UnsupportedOperationException();
    }
}
