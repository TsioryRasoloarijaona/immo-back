package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.CommissionTrancheDto;
import com.tsix.immocrm.dto.commission.DemandesCommission.FiltresCommission;
import com.tsix.immocrm.dto.commission.DossierCommissionDto;
import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommissionService {

    public List<CommissionTrancheDto> tranches(String recherche) {
        throw new UnsupportedOperationException();
    }

    public CommissionTrancheDto tranche(UUID trancheId) {
        throw new UnsupportedOperationException();
    }

    public List<DossierCommissionDto> dossiers(UUID trancheId, FiltresCommission filtres) {
        throw new UnsupportedOperationException();
    }

    public List<LigneCommissionCalculeeDto> lignes(UUID trancheId, FiltresCommission filtres) {
        throw new UnsupportedOperationException();
    }

    public ResponseEntity<byte[]> exporterLignes(UUID trancheId, FiltresCommission filtres) {
        throw new UnsupportedOperationException();
    }

    public ResponseEntity<byte[]> exporterTranches(String recherche) {
        throw new UnsupportedOperationException();
    }
}
