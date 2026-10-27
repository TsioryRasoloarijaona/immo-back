package com.tsix.immocrm.services.modele;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Impression du lot du rapport n° 35 (FEATURE-vente-formulaire-barid.md §4) : un PDF, une page par destinataire. */
@Service
@Transactional(readOnly = true)
public class FormulairesBaridLot {

    public GenerationModeleService.DocumentProduit imprimer(String module, String code, Map<String, List<String>> recus) {
        throw new UnsupportedOperationException();
    }
}
