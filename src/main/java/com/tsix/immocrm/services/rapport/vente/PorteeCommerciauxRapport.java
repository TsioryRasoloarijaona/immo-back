package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Couche « commerciaux » des rapports chiffrés par commercial (FEATURE-tdb-vente-rapports-commercial.md §2.3) : un
 * exempté voit tout, ligne sans commercial comprise ; les autres, les seuls {@link ContexteRapport#commerciauxVisibles()}.
 */
@Component
class PorteeCommerciauxRapport {

    private final PerimetreCommercialService perimetre;

    PorteeCommerciauxRapport(PerimetreCommercialService perimetre) {
        this.perimetre = perimetre;
    }

    /** Teste la clé du commercial d'une ligne ({@code CfgCommercialId}, {@code null} = sans commercial). */
    Predicate<UUID> retenus(ContexteRapport contexte) {
        if (contexte.admUserId() != null && perimetre.estExempte(contexte.admUserId())) {
            return id -> true;
        }
        Set<UUID> visibles = Set.copyOf(contexte.commerciauxVisibles());
        return id -> id != null && visibles.contains(id);
    }
}
