package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class LignesCommission {

    public LigneCommissionCalculeeDto modifierMontant(UUID ligneId, BigDecimal montant) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(List<UUID> ligneIds) {
        throw new UnsupportedOperationException();
    }
}
