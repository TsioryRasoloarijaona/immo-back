package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielsTranche;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Référentiels des fiches programme et tranche (FEATURE-patrimoine-tranche-fiche.md §2) : une ligne par appel. */
@Component
public class FixtureFichePatrimoine {

    private final ReferentielsTranche referentiels;
    private final CfgTierRepository tierRepository;
    private final CfgTierTypeRepository tierTypeRepository;

    public FixtureFichePatrimoine(ReferentielsTranche referentiels, CfgTierRepository tierRepository,
                                  CfgTierTypeRepository tierTypeRepository) {
        this.referentiels = referentiels;
        this.tierRepository = tierRepository;
        this.tierTypeRepository = tierTypeRepository;
    }

    public CfgTier tiers(TypeTiersPatrimoine type, String code) {
        return tiersDuType(typeTiers(type.code()), code);
    }

    /** Tiers d'un autre type (ici PRO) : refusé par les sélecteurs personnels et fournisseurs. */
    public CfgTier prospect(String code) {
        return tiersDuType(typeTiers("PRO"), code);
    }

    private CfgTier tiersDuType(CfgTierType type, String code) {
        CfgTier tiers = new CfgTier();
        tiers.setId(UUID.randomUUID());
        tiers.setCode(code);
        tiers.setDescription("Tiers " + code);
        tiers.setCfgTierType(type);
        return tierRepository.save(tiers);
    }

    private CfgTierType typeTiers(String code) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType type = new CfgTierType();
            type.setId(UUID.randomUUID());
            type.setCode(code);
            type.setDescription("Type " + code);
            return tierTypeRepository.save(type);
        });
    }

    public CfgTrancheStanding standing(String code) {
        CfgTrancheStanding standing = new CfgTrancheStanding();
        standing.setId(UUID.randomUUID());
        standing.setCode(code);
        standing.setDescription("Standing " + code);
        return referentiels.standings().save(standing);
    }

    public LndLevel niveau(String code) {
        LndLevel niveau = new LndLevel();
        niveau.setId(UUID.randomUUID());
        niveau.setCode(code);
        niveau.setDescription("Niveau " + code);
        return referentiels.niveaux().save(niveau);
    }

    public StkTrancheHeading rubrique(String code, int ordre) {
        StkTrancheHeading rubrique = new StkTrancheHeading();
        rubrique.setId(UUID.randomUUID());
        rubrique.setCode(code);
        rubrique.setDescription("Rubrique " + code);
        rubrique.setSorting(ordre);
        return referentiels.rubriques().save(rubrique);
    }

    public ActAccount compte(String code) {
        ActAccount compte = new ActAccount();
        compte.setId(UUID.randomUUID());
        compte.setCode(code);
        compte.setDescription("Compte " + code);
        return referentiels.comptes().save(compte);
    }

    public PrjProject projetChantier(String code, CfgCompany entite) {
        PrjProject projet = new PrjProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(entite);
        return referentiels.projetsChantier().save(projet);
    }
}
