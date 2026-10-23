package com.tsix.immocrm.services.patrimoine.importation;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ModeleImport {

    public ResponseEntity<byte[]> classeur(String type, UUID trancheId) {
        throw new UnsupportedOperationException();
    }
}
