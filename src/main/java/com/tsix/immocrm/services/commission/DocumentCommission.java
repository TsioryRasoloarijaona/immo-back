package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.DocumentCommissionDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DocumentCommission {

    public List<DocumentCommissionDto> deLaTranche(UUID trancheId) {
        throw new UnsupportedOperationException();
    }

    public DocumentCommissionDto generer(List<UUID> ligneIds) {
        throw new UnsupportedOperationException();
    }

    public DocumentCommissionDto deLaLigne(UUID ligneId) {
        throw new UnsupportedOperationException();
    }
}
