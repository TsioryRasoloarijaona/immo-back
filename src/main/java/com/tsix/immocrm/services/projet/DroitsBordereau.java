package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.GenreLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.droits.DroitsOmran;

import java.util.Optional;
import java.util.Set;

/**
 * Seule règle d'écriture des bordereaux d'un lot (FEATURE-lot-ecritures.md R2, R3) : appelée par la lecture
 * ({@code verrouille}, {@code modifiable}) et par l'écriture.
 */
final class DroitsBordereau {

    static final String AJOUTER_LIGNE = "Yes.Prj.Front.Market.MarketMonitoring.toolAddMarketLine";
    static final String CADENAS = "Le bordereau estimatif est verrouillé par le statut du lot";
    static final String MULTI = "Bordereau estimatif multi-fournisseurs : non géré";

    private DroitsBordereau() {
    }

    /** Refus d'écriture du bordereau (403 droit, 409 cadenas ou multi-fournisseurs), vide si l'écriture est permise. */
    static Optional<RuntimeException> refus(DroitsOmran droits, PrjMarket lot, boolean estimatif) {
        Set<String> portes = droits.parmi(GenreLot.DROIT_NOUVEAU, AJOUTER_LIGNE);
        if (!portes.contains(GenreLot.DROIT_NOUVEAU) || !(estimatif || portes.contains(AJOUTER_LIGNE))) {
            return Optional.of(new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action."));
        }
        if (!estimatif) {
            return Optional.empty();
        }
        if (EcrituresLotService.genre(lot).multiFournisseur()) {
            return Optional.of(new ConflictException(MULTI));
        }
        // BoolField004 (« Avec bordereau estimatif ») est mappé sous le nom de son usage Vente.
        boolean ouvert = lot.getStrStatus() != null && Boolean.TRUE.equals(lot.getStrStatus().getVenteAuComptant());
        return ouvert ? Optional.empty() : Optional.of(new ConflictException(CADENAS));
    }
}
