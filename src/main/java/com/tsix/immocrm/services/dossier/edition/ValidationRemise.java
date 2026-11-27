package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.AgenceDuCompte;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Qui décide d'une remise (FEATURE-agences-vente.md A3) : jamais un compte agence, sinon niveau 2 ou droit
 * {@code toolOverDiscount}. Rôles et droits lus en base, jamais dans le JWT.
 */
@Component
public class ValidationRemise {

    static final String SANS_PLAFOND = "Yes.Com.Front.Sales.Monitoring.toolOverDiscount";

    private final DroitsOmran droits;
    private final AgenceDuCompte agences;

    public ValidationRemise(DroitsOmran droits, AgenceDuCompte agences) {
        this.droits = droits;
        this.agences = agences;
    }

    /** Le compte courant peut dépasser les plafonds de remise de l'article. */
    public boolean sansPlafond() {
        return sansPlafond(PerimetreCommercialService.admUserIdCourant().orElse(null));
    }

    public boolean valideur(UUID compte, Collection<String> rolesEnBase) {
        return !RoleAgence.porte(rolesEnBase)
                && (PlafondRemise.niveauDe(rolesEnBase) == PlafondRemise.Niveau.DEUXIEME || sansPlafond(compte));
    }

    /** Codes de rôle du compte, lus dans {@code AdmUserRole}. */
    public List<String> roles(UUID compte) {
        return agences.roles(compte);
    }

    /** 403 si le compte courant n'est pas valideur de remise. */
    public void exigerValideur() {
        UUID compte = compteCourant();
        if (!valideur(compte, roles(compte))) {
            throw new ForbiddenException("Réservé aux valideurs de remise (niveau 2 ou dépassement de plafond).");
        }
    }

    /** Compte courant s'il porte le rôle Agence en base, 403 sinon. */
    public UUID exigerAgence() {
        UUID compte = compteCourant();
        if (!RoleAgence.porte(roles(compte))) {
            throw new ForbiddenException("Réservé aux comptes d'agence.");
        }
        return compte;
    }

    private boolean sansPlafond(UUID compte) {
        return droits.parmiDe(compte, SANS_PLAFOND).contains(SANS_PLAFOND);
    }

    private static UUID compteCourant() {
        return PerimetreCommercialService.admUserIdCourant()
                .orElseThrow(() -> new ForbiddenException("Compte requis."));
    }
}
