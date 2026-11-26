package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.FactureGenereeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Facture et avoir fournisseur d'un décompte (FEATURE-lc8-facture-decompte.md, OMRAN {@code Invoice.cs:689-832}). */
@Service
@Transactional
public class FacturationDecompteService {

    public FactureGenereeDto facturer(UUID id) {
        throw new UnsupportedOperationException(id.toString());
    }

    public FactureGenereeDto avoir(UUID id) {
        throw new UnsupportedOperationException(id.toString());
    }
}
