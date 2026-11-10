package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.dto.projet.DepensesProjetDto;
import com.tsix.immocrm.dto.projet.FiltresReglementsProjet;
import com.tsix.immocrm.dto.projet.ReglementProjetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DepensesProjetService {

    public List<DepensesProjetDto> lireDepenses(List<UUID> projetIds) {
        throw new UnsupportedOperationException();
    }

    public Page<ReglementProjetDto> lireReglements(FiltresReglementsProjet filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }
}
