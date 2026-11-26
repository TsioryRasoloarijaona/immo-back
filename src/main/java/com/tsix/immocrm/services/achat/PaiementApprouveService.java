package com.tsix.immocrm.services.achat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Impression « Paiement approuvé » d'une facture d'achat (FEATURE-lc8b-paiement-approuve.md). */
@Service
@Transactional(readOnly = true)
public class PaiementApprouveService {

    /** PDF rendu et code de la pièce (nom du fichier). */
    public record Impression(String code, byte[] pdf) {
    }

    public Impression imprimer(UUID id) {
        throw new UnsupportedOperationException(id.toString());
    }
}
