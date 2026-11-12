package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.LotDetailDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

/** Enregistrer la fiche d'un lot (FEATURE-lot-fiche-parite.md E6). */
@Service
public class ModificationLot {

    @Transactional
    public LotDetailDto modifier(UUID id, JsonNode corps) {
        throw new UnsupportedOperationException();
    }
}
