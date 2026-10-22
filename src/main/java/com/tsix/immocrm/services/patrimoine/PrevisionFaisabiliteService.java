package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.ApercuTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanFaisabilitePrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.CategorieBilanDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisiePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TresorerieFaisabiliteDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PrevisionFaisabiliteService {

    public BilanFaisabilitePrevisionsDto lire(UUID etudeId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public BilanFaisabilitePrevisionsDto modifierPrevisions(UUID etudeId, List<SaisiePrevisionDto> lignes) {
        throw new UnsupportedOperationException();
    }

    public BilanFaisabilitePrevisionsDto calculer(UUID etudeId, List<SaisiePrevisionDto> lignes) {
        throw new UnsupportedOperationException();
    }

    public List<CategorieBilanDto> categories() {
        throw new UnsupportedOperationException();
    }

    public TresorerieFaisabiliteDto tresorerie(UUID etudeId, boolean avecFinancement) {
        throw new UnsupportedOperationException();
    }

    public TresorerieFaisabiliteDto calculerTresorerie(UUID etudeId, ApercuTresorerieDto apercu) {
        throw new UnsupportedOperationException();
    }
}
