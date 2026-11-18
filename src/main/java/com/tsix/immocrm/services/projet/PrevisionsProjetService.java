package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.PrevisionsProjetDto;
import com.tsix.immocrm.dto.projet.ProjetPrevisionOptionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Prévisions des lots d'un projet (FEATURE-n4-previsions-chantier.md N4a). */
@Service
@Transactional(readOnly = true)
public class PrevisionsProjetService {

    public List<ProjetPrevisionOptionDto> projets() {
        throw new UnsupportedOperationException();
    }

    public PrevisionsProjetDto previsions(UUID projetId) {
        throw new UnsupportedOperationException();
    }

    public ResponseEntity<byte[]> exporter(UUID projetId) {
        throw new UnsupportedOperationException();
    }
}
