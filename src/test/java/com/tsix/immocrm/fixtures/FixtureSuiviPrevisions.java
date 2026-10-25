package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Suivi des prévisions de vente (FEATURE-tdb-vente-previsions.md §9) : ventes concrétisées avec client nommé, échéances et
 * règlements datés, sur un {@link Jeu} de {@link FixtureFluxVente} (seuils, statuts, parents avant enfants).
 */
@Component
public class FixtureSuiviPrevisions {

    public static final String MOT_DE_PASSE = "Password123";

    private final FixtureEtatVentes ev;
    private final CfgTierRepository tiers;
    private final AppUserCredentialRepository identifiants;

    public FixtureSuiviPrevisions(FixtureEtatVentes ev, CfgTierRepository tiers,
                                  AppUserCredentialRepository identifiants) {
        this.ev = ev;
        this.tiers = tiers;
        this.identifiants = identifiants;
    }

    /** Client dont la raison sociale, la description et le nom portent {@code jeton}. */
    public CfgTier client(String jeton) {
        CfgTier client = ev.client(jeton);
        client.setSocialReason(jeton);
        client.setDescription(jeton);
        return tiers.save(client);
    }

    /** Dossier CONCRETISATION concrétisé le {@code avance} ; {@code jetonClient} nul = sans client. */
    public ComFolder vente(Jeu jeu, CfgTranche tranche, LocalDate avance, String jetonClient) {
        ComFolder d = jeu.dossier(tranche, jeu.statuts().concretisation());
        if (jetonClient != null) {
            d.setCfgCustomer(client(jetonClient));
        }
        return jeu.dater(d, avance, null, null, null);
    }

    /** Échéance d'une catégorie propre au test, au crédit. */
    public ComDocument echeance(Jeu jeu, ComFolder dossier, Genre genre, String montant, LocalDate date) {
        return jeu.echeance(dossier, jeu.categorie(genre, true), new BigDecimal(montant), date);
    }

    /** Ligne de règlement encaissée ({@code IsCredit = 1}), datée par l'échéance du règlement. */
    public StlSettlementLine regler(Jeu jeu, ComDocument echeance, String montant, String statut, LocalDate date) {
        return jeu.regler(echeance, new BigDecimal(montant), true, statut, null, date);
    }

    /** Identifiant de connexion du compte (mot de passe {@link #MOT_DE_PASSE}, ni temporaire ni profil à compléter). */
    public void motDePasse(AdmUser compte) {
        AppUserCredential c = new AppUserCredential();
        c.setId(UUID.randomUUID());
        c.setAdmUser(compte);
        c.setPasswordHash(new BCryptPasswordEncoder().encode(MOT_DE_PASSE));
        c.setMustChangePassword(false);
        c.setProfileCompleted(true);
        c.setUserCreateDateTime(LocalDateTime.now());
        identifiants.save(c);
    }
}
