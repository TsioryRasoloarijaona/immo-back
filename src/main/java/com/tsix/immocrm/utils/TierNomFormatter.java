package com.tsix.immocrm.utils;

import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.util.StringUtils;

/**
 * Derives a display name for a {@code CfgTier} (client, prospect, commercial, banque, avocat...)
 * without relying solely on {@code CfgTier.Description} — that column is the canonical legacy
 * display field, but was found to be systematically left {@code NULL} by this app's own
 * prospect/client creation flow (see {@code ProspectService}, fixed to populate it going forward)
 * — plenty of rows created through the API before that fix (and, in principle, any future gap)
 * still have it blank, so every read path needs the same fallback rather than trusting the column.
 * <p>
 * Priority: {@code SocialReason} (the authoritative name for a legal entity, e.g. a company or a
 * bank) &gt; {@code Description} (legacy canonical field — also covers tiers with neither a
 * SocialReason nor a First/LastName, e.g. some organismes/banques) &gt; {@code FirstName +
 * LastName} (individuals).
 * <p>
 * Was previously duplicated ad hoc (with two slightly different, both incomplete, priority
 * orders) across a dozen services — {@code DossierService}, {@code ProspectSubResourcesService},
 * {@code FicheBancaireService}, {@code FicheTropPercuService}, {@code EcheancierService},
 * {@code ProspectionService}, {@code DevisService}, {@code ActeReservationService},
 * {@code FicheDossierVenteService}, {@code OffreDevisDocumentService}, {@code HotlineService},
 * {@code CalendrierService} — consolidated here so a fix only needs to happen once.
 */
public final class TierNomFormatter {

    private TierNomFormatter() {
    }

    public static String nom(CfgTier tier) {
        if (tier == null) {
            return null;
        }
        if (StringUtils.hasText(tier.getSocialReason())) {
            return tier.getSocialReason();
        }
        if (StringUtils.hasText(tier.getDescription())) {
            return tier.getDescription();
        }
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }
}
