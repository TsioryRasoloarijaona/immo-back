package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Clés étrangères saisies sur les fiches programme et tranche (FEATURE-patrimoine-tranche-fiche.md §1.2) : référence
 * inconnue → 400 sur le champ du corps, modèle {@code ProjetService.pays}. Absent = inchangé.
 */
@Component
class ReferencesFiche {

    private final ReferentielsTranche referentiels;

    ReferencesFiche(ReferentielsTranche referentiels) {
        this.referentiels = referentiels;
    }

    void pays(UUID id, String champ, Consumer<AdmCountry> setter) {
        si(id, champ, "Pays", referentiels.pays(), setter);
    }

    void standing(UUID id, String champ, Consumer<CfgTrancheStanding> setter) {
        si(id, champ, "Standing", referentiels.standings(), setter);
    }

    void niveau(UUID id, String champ, Consumer<LndLevel> setter) {
        si(id, champ, "Niveau", referentiels.niveaux(), setter);
    }

    void rubrique(UUID id, String champ, Consumer<StkTrancheHeading> setter) {
        si(id, champ, "Rubrique", referentiels.rubriques(), setter);
    }

    void compte(UUID id, String champ, Consumer<ActAccount> setter) {
        si(id, champ, "Compte", referentiels.comptes(), setter);
    }

    void projetChantier(UUID id, String champ, Consumer<PrjProject> setter) {
        si(id, champ, "Projet", referentiels.projetsChantier(), setter);
    }

    /** Même type que la liste du sélecteur ({@code /api/referentiels-patrimoine/personnels|tiers-autrui}). */
    void tiers(UUID id, String champ, TypeTiersPatrimoine type, Consumer<CfgTier> setter) {
        if (id == null) {
            return;
        }
        CfgTier tiers = trouver(id, champ, "Tiers", referentiels.tiers());
        String code = Champs.de(tiers, CfgTier::getCfgTierType, CfgTierType::getCode);
        if (!type.code().equalsIgnoreCase(code)) {
            throw new ChampInvalideException(champ,
                    "Le tiers « %s » n'est pas %s.".formatted(tiers.getDescription(), type.libelle()));
        }
        setter.accept(tiers);
    }

    private static <T> void si(UUID id, String champ, String libelle, JpaRepository<T, UUID> depot,
                               Consumer<T> setter) {
        if (id != null) {
            setter.accept(trouver(id, champ, libelle, depot));
        }
    }

    private static <T> T trouver(UUID id, String champ, String libelle, JpaRepository<T, UUID> depot) {
        return depot.findById(id)
                .orElseThrow(() -> new ChampInvalideException(champ, "%s introuvable : %s".formatted(libelle, id)));
    }
}
