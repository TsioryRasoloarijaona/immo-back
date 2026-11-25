package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.TypeBordereau;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExportBordereauLot {

    public ResponseEntity<byte[]> exporter(UUID id, TypeBordereau type) {
        throw new UnsupportedOperationException();
    }
}
