package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.droits.DroitsOmran;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seule règle « mes bons de commande » (liste, Documents Achats, totaux, export, fiche) : comme OMRAN
 * ({@code OrderView.cs:803}), un BC n'est visible que si son déclencheur est le personnel du compte.
 */
@Component
public class RestrictionBonsCommande {

    static final String DROIT_SANS_RESTRICTION = "Yes.Buy.Front.Order.OrderList.WithNoRestriction";

    private final DroitsOmran droits;

    public RestrictionBonsCommande(DroitsOmran droits) {
        this.droits = droits;
    }

    /** Déclencheur imposé aux BC du compte courant ; null (aucune restriction) sans personnel ou avec le droit. */
    public UUID declencheurImpose() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID personnel = auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user.employeeId() : null;
        if (personnel == null || !droits.parmi(DROIT_SANS_RESTRICTION).isEmpty()) {
            return null;
        }
        return personnel;
    }
}
