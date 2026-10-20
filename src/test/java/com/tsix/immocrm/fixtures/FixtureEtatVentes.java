package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jeu de l'état des ventes (FEATURE-etat-des-ventes.md §6) : articles racines, parkings autonomes, sous-articles
 * rattachés, dossiers par statut. Parents insérés avant les FK ; tout est annulé avec la transaction du test.
 */
@Component
public class FixtureEtatVentes {

    public static final BigDecimal SURFACE = new BigDecimal("100");
    public static final BigDecimal PRIX_CATALOGUE = new BigDecimal("950000");

    private final FixturePatrimoine patrimoine;
    private final FixtureReferentielStk referentielStk;
    private final FixtureStatutsDossier statutsDossier;
    private final Depots depots;
    private final DepotsReglement depotsReglement;
    private final EntityManager entityManager;

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(StkItemRepository articles, ComFolderRepository dossiers, CfgTrancheRepository tranches,
                         CfgTierRepository tiers, StrStatusRepository statuts,
                         AdmUserHabilitationRepository habilitations) {
    }

    /** Dépôts des échéances et des règlements (FEATURE-etat-des-ventes-parite.md §5). */
    @Component
    public record DepotsReglement(ComSettlementCategoryRepository categories, ComDocumentRepository echeances,
                                  StlSettlementRepository reglements, StlSettlementLineRepository lignes) {
    }

    /** Nature d'une catégorie d'échéance, drapeau {@code IsSale*} de {@code ComSettlementCategory}. */
    public enum Nature { AVANCE, AUTOFINANCEMENT, CREDIT, FRAIS, MODIFICATION, PENALITE }

    public FixtureEtatVentes(FixturePatrimoine patrimoine, FixtureReferentielStk referentielStk,
                             FixtureStatutsDossier statutsDossier, Depots depots, DepotsReglement depotsReglement,
                             EntityManager entityManager) {
        this.patrimoine = patrimoine;
        this.referentielStk = referentielStk;
        this.statutsDossier = statutsDossier;
        this.depots = depots;
        this.depotsReglement = depotsReglement;
        this.entityManager = entityManager;
    }

    public static String prefixe(String racine) {
        return racine + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ------------------------------------------------------------------ patrimoine

    public CfgProject projet(String code, boolean entiteActive) {
        CfgCompany entite = patrimoine.entite("SOC-" + code, entiteActive);
        return patrimoine.projet(code, entite);
    }

    public CfgTranche tranche(String code, CfgProject projet, boolean enVente) {
        CfgTranche tranche = patrimoine.tranche(code, projet);
        tranche.setIsForSale(enVente);
        return depots.tranches().save(tranche);
    }

    public StkItemCategory categorie(String code) {
        return patrimoine.categorie(code, referentielStk.natureParDefaut(), false);
    }

    public StkItemType type(String code) {
        return patrimoine.type(code, referentielStk.natureParDefaut(), false);
    }

    public StkVocation vente() {
        return patrimoine.vocationVente();
    }

    public StkVocation projetVocation() {
        return patrimoine.vocationProjet();
    }

    /** Article racine ({@code ParentId} NULL, {@code IsUnderItem = 0}), vocation VENTE. */
    public StkItem article(String code, CfgTranche tranche, StkItemCategory categorie, StkItemType type) {
        return depots.articles().save(nouvelArticle(code, tranche, categorie, type, vente(), false, null));
    }

    public StkItem article(String code, CfgTranche tranche, StkItemCategory categorie, StkItemType type,
                           StkVocation vocation) {
        return depots.articles().save(nouvelArticle(code, tranche, categorie, type, vocation, false, null));
    }

    /** Parking autonome : {@code IsUnderItem = 1} sans parent — compté (R1). */
    public StkItem parkingAutonome(String code, CfgTranche tranche, StkItemCategory categorie, StkItemType type) {
        return depots.articles().save(nouvelArticle(code, tranche, categorie, type, vente(), true, null));
    }

    /** Sous-article rattaché : {@code ParentId} renseigné — exclu (R1). */
    public StkItem sousArticle(String code, StkItem parent, StkItemCategory categorie, StkItemType type) {
        return depots.articles().save(nouvelArticle(code, parent.getStkHierarchy(), categorie, type, vente(), true,
                parent));
    }

    private StkItem nouvelArticle(String code, CfgTranche tranche, StkItemCategory categorie, StkItemType type,
                                  StkVocation vocation, boolean sousArticle, StkItem parent) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setDescription("Article " + code);
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(referentielStk.natureParDefaut());
        article.setIsUnderItem(sousArticle);
        article.setParent(parent);
        article.setArea(SURFACE);
        article.setComSalePrice(PRIX_CATALOGUE);
        return article;
    }

    // ------------------------------------------------------------------ dossiers

    public StrStatus statut(StatutDossier statut) {
        return statutsDossier.statut(statut);
    }

    /** Statut de sortie propre au test : {@code BoolField002} (annulation) ou {@code BoolField003} (désistement). */
    public StrStatus statutSortie(String code, boolean annulation) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setDescription(code);
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        s.setAnnulation(annulation);
        s.setResiliation(!annulation);
        return depots.statuts().save(s);
    }

    /** Statut propre au test, hors {@link StatutDossier} et hors sortie (ni annulation ni désistement). */
    public StrStatus statutEnCours(String code) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setDescription(code);
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        s.setAnnulation(false);
        s.setResiliation(false);
        return depots.statuts().save(s);
    }

    public CfgTier client(String nom) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("CLI-" + nom);
        tier.setFirstName("Prenom");
        tier.setLastName(nom);
        return depots.tiers().save(tier);
    }

    /** Dossier de vente qui retient {@code article} ({@code StkItem.ComFolderId}) ; {@code statut} peut être nul. */
    public ComFolder vendre(StkItem article, String codeDossier, StrStatus statut, CfgTier client,
                            LocalDateTime dateReservation) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(codeDossier);
        folder.setStkVocation(vente());
        folder.setStrStatus(statut);
        folder.setCfgCustomer(client);
        folder.setDocDate(dateReservation);
        folder.setArea(article.getArea());
        folder.setPrice(new BigDecimal("1200000"));
        ComFolder sauve = depots.dossiers().save(folder);

        article.setComFolderId(sauve.getId());
        sauve.setStkItem(depots.articles().save(article));
        return depots.dossiers().save(sauve);
    }

    /** Prix du dossier : {@code Price}, {@code PriceAfterDiscount} et {@code AgreementPrice} au même montant. */
    public ComFolder prix(ComFolder dossier, BigDecimal prix) {
        dossier.setPrice(prix);
        dossier.setPriceAfterDiscount(prix);
        dossier.setAgreementPrice(prix);
        return depots.dossiers().save(dossier);
    }

    /** Enregistre un dossier dont le test a posé des colonnes de prix à la main. */
    public ComFolder enregistrer(ComFolder dossier) {
        return depots.dossiers().save(dossier);
    }

    /** Prix catalogue d'un article : {@code SalePrice}, {@code ComSalePrice}, {@code Vat} (taux), {@code ComSalePriceHT}. */
    public StkItem catalogue(StkItem article, BigDecimal prixArticle, BigDecimal prixTotal, BigDecimal tva,
                             BigDecimal prixTotalHt) {
        article.setSalePrice(prixArticle);
        article.setComSalePrice(prixTotal);
        article.setVat(tva);
        article.setComSalePriceHT(prixTotalHt);
        return depots.articles().save(article);
    }

    /** Second article racine retenu par le même dossier ({@code StkItem.ComFolderId}), sans nouveau dossier. */
    public StkItem rattacher(StkItem article, ComFolder dossier) {
        article.setComFolderId(dossier.getId());
        return depots.articles().save(article);
    }

    // ------------------------------------------------------------------ échéances et règlements

    /** Catégorie propre au test ; {@code auCredit} = {@code ComSettlementCategory.IsCredit}. */
    public ComSettlementCategory categorieEcheance(Nature nature, boolean auCredit) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(nature.name().substring(0, 3) + "-" + UUID.randomUUID().toString().substring(0, 8));
        c.setIsCredit(auCredit);
        c.setIsSaleAdvance(nature == Nature.AVANCE);
        c.setIsSaleAutoFinancment(nature == Nature.AUTOFINANCEMENT);
        c.setIsSaleCredit(nature == Nature.CREDIT);
        c.setIsSaleExpense(nature == Nature.FRAIS);
        c.setIsSaleModification(nature == Nature.MODIFICATION);
        c.setIsSalePenality(nature == Nature.PENALITE);
        return depotsReglement.categories().save(c);
    }

    /** Échéance d'une nature ; les frais sont hors prix ({@code ExcludeFromPrice}), comme dans la liste des dossiers. */
    public ComDocument echeance(ComFolder dossier, Nature nature, boolean auCredit, BigDecimal montant) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorieEcheance(nature, auCredit));
        d.setAmount(montant);
        d.setDocDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        d.setExcludeFromPrice(nature == Nature.FRAIS);
        return depotsReglement.echeances().save(d);
    }

    /** Règlement d'une échéance ; {@code encaissement} = {@code StlSettlement.IsCredit}, {@code statut} peut être nul. */
    public void regler(ComDocument echeance, BigDecimal montant, boolean encaissement, String statut) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("RG-" + UUID.randomUUID().toString().substring(0, 8));
        s.setDocDate(LocalDateTime.of(2025, 2, 1, 0, 0));
        s.setAmount(montant);
        s.setIsCredit(encaissement);
        s.setStatus(statut);
        depotsReglement.reglements().save(s);
        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(echeance);
        l.setAmount(montant);
        depotsReglement.lignes().save(l);
    }

    // ------------------------------------------------------------------ périmètre

    public void affecter(AdmUser compte, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(projet);
        depots.habilitations().save(habilitation);
    }

    public void relire() {
        entityManager.flush();
        entityManager.clear();
    }
}
