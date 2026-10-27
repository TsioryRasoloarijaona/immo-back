package com.tsix.immocrm.services.patrimoine.impressions;

import com.tsix.immocrm.dto.patrimoine.impressions.ApercuRelance2Dto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ImpressionsTrancheService {

    public record Lettres(String nomFichier, byte[] pdf) {
    }

    public ApercuRelance2Dto apercu(UUID trancheId, List<String> statuts) {
        throw new UnsupportedOperationException();
    }

    public Lettres lettres(UUID trancheId, List<String> statuts) {
        throw new UnsupportedOperationException();
    }
}
