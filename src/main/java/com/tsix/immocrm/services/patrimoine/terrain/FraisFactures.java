package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.buy.BuyDocumentRepository;
import com.tsix.immocrm.repositories.stk.StkExpenseRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seule définition d'un frais « validé » : {@code IsValidated}, ou une facture qui le porte déjà (facture orpheline
 * laissée par OMRAN, qui pose {@code IsValidated} hors de la transaction de la facture).
 */
@Component
record FraisFactures(BuyDocumentRepository factures, StkExpenseRepository depenses) {

    /** Facture de chaque frais facturé, par {@code StkExpense.Pkey}. */
    Map<UUID, BuyDocument> parFrais(Collection<StkExpense> frais) {
        if (frais.isEmpty()) {
            return Map.of();
        }
        return factures.findByStkExpenseIdIn(frais.stream().map(StkExpense::getId).toList()).stream()
                .collect(Collectors.toMap(BuyDocument::getStkExpenseId, Function.identity(), (a, b) -> a));
    }

    static boolean valide(StkExpense frais, Map<UUID, BuyDocument> factures) {
        return Boolean.TRUE.equals(frais.getIsValidated()) || factures.containsKey(frais.getId());
    }

    /** Frais du terrain verrouillé (PESSIMISTIC_WRITE) : il ne doit pas être déjà chargé dans la session. */
    StkExpense verrouiller(UUID fraisId, UUID terrainId) {
        return depenses.findPourValidation(fraisId, terrainId)
                .orElseThrow(() -> new ResourceNotFoundException("Frais introuvable sur ce terrain: " + fraisId));
    }
}
