package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Dossier de vente complet pour les tests de la fiche et de ses actions (FEATURE-vente-fiche.md) : société,
 * projet, tranche, article, statut, comptes. Tout est créé par le test et annulé avec sa transaction.
 */
@Component
public class FixtureDossierVente {

    private final Depots depots;
    private final FixtureReferentielStk referentielStk;

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(CfgCompanyRepository societes, CfgProjectRepository projets, CfgTrancheRepository tranches,
                         CfgTierRepository tiers, StkItemRepository articles, StkItemTypeRepository typesArticle,
                         StkItemCategoryRepository categoriesArticle, StkVocationRepository vocations,
                         ComFolderRepository dossiers, StrStatusRepository statuts, ComDocumentRepository echeances,
                         ComSettlementCategoryRepository categoriesEcheance, AdmUserRepository comptes,
                         AdmRoleRepository roles, AdmUserRoleRepository rolesComptes,
                         ComActionMessageTypeRepository typesAction,
                         ComActionMessageCategoryRepository categoriesAction) {
    }

    public FixtureDossierVente(Depots depots, FixtureReferentielStk referentielStk) {
        this.depots = depots;
        this.referentielStk = referentielStk;
    }

    public CfgTranche tranche(String code, boolean societeActive) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-" + code + "-" + UUID.randomUUID().toString().substring(0, 8));
        societe.setDescription("Société " + code);
        societe.setIsActive(societeActive);
        depots.societes().save(societe);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + code);
        projet.setDescription("Projet " + code);
        projet.setCfgCompany(societe);
        depots.projets().save(projet);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setDescription("Tranche " + code);
        tranche.setCfgProject(projet);
        return depots.tranches().save(tranche);
    }

    public CfgTier tier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        tier.setFirstName("Prénom");
        tier.setLastName("Nom " + code);
        return depots.tiers().save(tier);
    }

    public StrStatus statut(StatutDossier statut) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(statut.getCode());
        s.setSorting(statut.ordinal());
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        return depots.statuts().save(s);
    }

    /** Article de 100 m² à 10 000 HT le m², TVA 20 %, rattaché à un dossier figé à l'identique. */
    public ComFolder dossier(String code, CfgTranche tranche, StatutDossier statut) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-" + code);
        depots.vocations().save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(vocation);
        folder.setStrStatus(statut != null ? statut(statut) : null);
        folder.setCfgCustomer(tier("CLI-" + code));
        folder.setArea(new BigDecimal("100"));
        folder.setUnitCostHT(new BigDecimal("10000"));
        folder.setVATRatio(new BigDecimal("0.2"));

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-" + code);
        article.setStkItemType(typeArticle());
        article.setStkItemCategory(categorieArticle());
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setComFolderId(folder.getId());
        article.setStkKind(referentielStk.natureParDefaut());
        article.setArea(new BigDecimal("100"));
        article.setComSaleUnitCostHT(new BigDecimal("10000"));
        article.setVat(new BigDecimal("0.2"));
        folder.setStkItem(depots.articles().save(article));
        return depots.dossiers().save(folder);
    }

    private StkItemType typeArticle() {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-FICHE");
        type.setStkKind(referentielStk.natureParDefaut());
        return depots.typesArticle().save(type);
    }

    private StkItemCategory categorieArticle() {
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-FICHE");
        categorie.setStkKind(referentielStk.natureParDefaut());
        return depots.categoriesArticle().save(categorie);
    }

    public ComSettlementCategory categorieEcheance(String code, boolean autofinancement, boolean credit) {
        return categorieEcheance(code, false, autofinancement, credit, null);
    }

    /** Catégorie au crédit ; {@code vocation} nulle = commune à toutes. */
    public ComSettlementCategory categorieEcheance(String code, boolean avance, boolean autofinancement,
                                                   boolean credit, StkVocation vocation) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setIsCredit(true);
        c.setIsSaleAdvance(avance);
        c.setIsSaleAutoFinancment(autofinancement);
        c.setIsSaleCredit(credit);
        c.setStkVocation(vocation);
        return depots.categoriesEcheance().save(c);
    }

    public ComDocument echeance(ComFolder folder, ComSettlementCategory categorie, BigDecimal montant,
                                LocalDateTime date) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(folder);
        d.setComSettlementCategory(categorie);
        d.setAmount(montant);
        d.setDocDate(date);
        return depots.echeances().save(d);
    }

    /** Compte actif rattaché à un employé, avec ses rôles {@code AdmUserRole}. */
    public AdmUser compte(String code, String... roles) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "-" + UUID.randomUUID());
        user.setDescription("Compte " + code);
        user.setDisabled(false);
        user.setCfgEmployee(tier("EMP-" + code));
        depots.comptes().save(user);
        for (String codeRole : roles) {
            AdmRole role = new AdmRole();
            role.setId(UUID.randomUUID());
            role.setCode(codeRole);
            depots.roles().save(role);
            AdmUserRole userRole = new AdmUserRole();
            userRole.setId(UUID.randomUUID());
            userRole.setAdmUser(user);
            userRole.setAdmRole(role);
            depots.rolesComptes().save(userRole);
        }
        return user;
    }

    public ComActionMessageType typeInstruction() {
        return depots.typesAction().findById(ActionsDossier.TYPE_INSTRUCTION).orElseGet(() -> {
            ComActionMessageType type = new ComActionMessageType();
            type.setId(ActionsDossier.TYPE_INSTRUCTION);
            type.setCode("Instruction");
            type.setDescription("Instruction");
            type.setIsSystem(false);
            return depots.typesAction().save(type);
        });
    }

    /** Catégories d'action d'un dossier, à leur identifiant légataire, créées si absentes. */
    public void categoriesAction() {
        categorieAction(ActionsDossier.CATEGORIE_RESERVATION, "Dossier de vente (Réservation)");
        categorieAction(ActionsDossier.CATEGORIE_LIVRAISON, "Dossier de vente (Livraison)");
        categorieAction(ActionsDossier.CATEGORIE_ANNULATION, "Désistement et annulation");
    }

    private void categorieAction(UUID id, String code) {
        if (depots.categoriesAction().existsById(id)) {
            return;
        }
        ComActionMessageCategory c = new ComActionMessageCategory();
        c.setId(id);
        c.setCode(code);
        c.setDescription(code);
        depots.categoriesAction().save(c);
    }

    public static RequestPostProcessor connecte(AdmUser user, String... roles) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(),
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null, false, true, 0);
        List<GrantedAuthority> autorites = Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, autorites));
    }
}
