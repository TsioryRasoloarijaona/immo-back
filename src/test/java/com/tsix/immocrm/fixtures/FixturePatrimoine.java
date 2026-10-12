package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chaîne société → projet → tranche → article, celle qu'OMRAN suit pour la société d'une échéance ou
 * d'un règlement ({@code StkItem.StkHierarchyId → CfgTranche → CfgProject.CfgCompanyId}).
 */
@Component
public class FixturePatrimoine {

    /** Référentiel d'une chaîne : la société et tout ce qu'il faut pour y créer des articles. */
    public record Chaine(CfgCompany societe, CfgTranche tranche, StkVocation vocation,
                         StkItemType type, StkItemCategory categorie) {
    }

    private final CfgCompanyRepository companyRepository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;
    private final StkItemTypeRepository itemTypeRepository;
    private final StkItemCategoryRepository itemCategoryRepository;
    private final StkVocationRepository vocationRepository;
    private final FixtureReferentielStk referentielStk;

    public FixturePatrimoine(CfgCompanyRepository companyRepository, CfgProjectRepository projectRepository,
                             CfgTrancheRepository trancheRepository, StkItemRepository stkItemRepository,
                             StkItemTypeRepository itemTypeRepository, StkItemCategoryRepository itemCategoryRepository,
                             StkVocationRepository vocationRepository, FixtureReferentielStk referentielStk) {
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.vocationRepository = vocationRepository;
        this.referentielStk = referentielStk;
    }

    public Chaine chaine(String suffixe) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-" + suffixe);
        societe.setDescription("Société " + suffixe);
        societe.setIsActive(true);
        societe = companyRepository.save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + suffixe);
        projet.setCfgCompany(societe);
        projet = projectRepository.save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-" + suffixe);
        tranche.setCfgProject(projet);
        tranche = trancheRepository.save(tranche);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-" + suffixe);
        vocation = vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-" + suffixe);
        type.setStkKind(referentielStk.natureParDefaut());
        type = itemTypeRepository.save(type);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-" + suffixe);
        categorie.setStkKind(referentielStk.natureParDefaut());
        categorie = itemCategoryRepository.save(categorie);

        return new Chaine(societe, tranche, vocation, type, categorie);
    }

    /** Article disponible de la chaîne ({@code ComFolderId} NULL). */
    public StkItem article(Chaine chaine, String code) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(chaine.type());
        article.setStkItemCategory(chaine.categorie());
        article.setStkVocation(chaine.vocation());
        article.setStkHierarchy(chaine.tranche());
        article.setStkKind(referentielStk.natureParDefaut());
        return stkItemRepository.save(article);
    }
}
