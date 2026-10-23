package com.tsix.immocrm.services.patrimoine;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ExportEtudeFaisabilite {

    public ResponseEntity<byte[]> exporter(UUID etudeId) {
        throw new UnsupportedOperationException();
    }
}
