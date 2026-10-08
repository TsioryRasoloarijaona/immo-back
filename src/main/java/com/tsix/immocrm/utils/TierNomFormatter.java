package com.tsix.immocrm.utils;

import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.util.StringUtils;

/**
 * Nom affiché d'un tiers : {@code SocialReason} > {@code Description} > prénom + nom. {@code Description} seule ne
 * suffit pas : des fiches créées par l'API l'ont laissée vide. Règle écrite une seule fois, à réutiliser partout.
 */
public final class TierNomFormatter {

    private TierNomFormatter() {
    }

    public static String nom(CfgTier tier) {
        if (tier == null) {
            return null;
        }
        return nom(tier.getSocialReason(), tier.getDescription(), tier.getFirstName(), tier.getLastName());
    }

    /**
     * Même priorité sur des colonnes brutes, pour le groupement (voir {@link com.tsix.immocrm.domain.Chemin#tier(String)}) :
     * un en-tête de groupe doit porter le même nom que ses lignes.
     */
    public static String nom(String socialReason, String description, String firstName, String lastName) {
        if (StringUtils.hasText(socialReason)) {
            return socialReason;
        }
        if (StringUtils.hasText(description)) {
            return description;
        }
        String prenom = firstName != null ? firstName : "";
        String nom = lastName != null ? lastName : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }
}
