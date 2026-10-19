package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Données du lot P1c (FEATURE-patrimoine-tranche-regles.md §1-§4, §9) : lignes des tables que le lot crée, en SQL
 * natif (les tests ne dépendent pas de ses entités), échéances et règlements des dossiers d'une tranche.
 * Flush avant chaque ordre : la CI porte les FK, le parent doit être écrit.
 */
@Component
public class FixtureTrancheRegles {

    /** {@code StrEntity} « ComFolder » de production : entité des statuts de dossier de vente. */
    public static final UUID ENTITE_DOSSIER_VENTE = StatutDossier.CONFIG_ENTITY_ID;

    private static final String ID = "id";
    private static final String TRANCHE = "tranche";

    private final EntityManager entityManager;
    private final FixtureDossierVente vente;
    private final CfgTrancheRepository trancheRepository;
    private final ComFolderRepository folderRepository;
    private final ComSettlementCategoryRepository categoryRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    @SuppressWarnings("java:S107")
    public FixtureTrancheRegles(EntityManager entityManager, FixtureDossierVente vente,
                                CfgTrancheRepository trancheRepository, ComFolderRepository folderRepository,
                                ComSettlementCategoryRepository categoryRepository,
                                StlSettlementRepository settlementRepository,
                                StlSettlementLineRepository settlementLineRepository) {
        this.entityManager = entityManager;
        this.vente = vente;
        this.trancheRepository = trancheRepository;
        this.folderRepository = folderRepository;
        this.categoryRepository = categoryRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    // ------------------------------------------------------------------ finalisations (§1)

    /** StrEntity est @Immutable côté JPA : insertion native, idempotente. */
    public UUID entiteDossierVente() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'ComFolder', N'Dossier de vente')""")
                .setParameter(ID, ENTITE_DOSSIER_VENTE));
        return ENTITE_DOSSIER_VENTE;
    }

    /** Statut de l'entité dossier de vente ({@code StrConfigEntityId}). */
    public UUID statutDossier(StatutDossier statut) {
        return vente.statut(statut).getId();
    }

    public UUID ligneFinalisation(UUID trancheId, UUID entiteId, UUID statut1, UUID statut2) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTrancheFolderEntity (PKey, CfgTrancheId, StrFolderEntityId, StrStatusDeliveryId1,
                            StrStatusDeliveryId2)
                        VALUES (:id, :tranche, :entite, :s1, :s2)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("entite", entiteId)
                .setParameter("s1", statut1).setParameter("s2", statut2));
        return id;
    }

    // ------------------------------------------------------------------ commissions (§2)

    /** Ligne de commission sans clé (personnel, action, commercial, catégorie nuls). */
    public UUID ligneCommission(UUID trancheId, LocalDateTime debut, LocalDateTime fin) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO ComSalesCommission (PKey, CfgTrancheId, StartDate, EndDate)
                        VALUES (:id, :tranche, :debut, :fin)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId).setParameter("debut", debut)
                .setParameter("fin", fin));
        return id;
    }

    // ------------------------------------------------------------------ prévisions de base (§3)

    public UUID lignePrevision(UUID trancheId, int annee, int mois, int quantite, int quantiteFinalisation) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgPrevision (PKey, StkHierarchyId, DocDate, [Year], [Month], Quantity, TurnoverQuantity)
                        VALUES (:id, :tranche, :date, :annee, :mois, :quantite, :finalisation)""")
                .setParameter(ID, id).setParameter(TRANCHE, trancheId)
                .setParameter("date", LocalDateTime.of(annee, mois, 1, 0, 0))
                .setParameter("annee", annee).setParameter("mois", mois).setParameter("quantite", quantite)
                .setParameter("finalisation", quantiteFinalisation));
        return id;
    }

    /** Prix et sous-article d'un article déjà créé ({@code FixtureTrancheOnglets.article}). */
    public void prixArticle(UUID articleId, BigDecimal prixVente, boolean sousArticle) {
        executer(entityManager.createNativeQuery("""
                        UPDATE StkItem SET SalePrice = :prix, IsUnderItem = :sous WHERE PKey = :id""")
                .setParameter(ID, articleId).setParameter("prix", prixVente).setParameter("sous", sousArticle));
    }

    // ------------------------------------------------------------------ mise à jour des schémas (§4)

    public CfgTranche debutFinalisations(CfgTranche tranche, LocalDateTime date) {
        tranche.setStartDateFinalisation(date);
        return trancheRepository.saveAndFlush(tranche);
    }

    /** Catégorie d'échéance ; {@code conferment} = {@code IsConfermentBasedDate}. */
    public ComSettlementCategory categorie(String code, Boolean conferment) {
        ComSettlementCategory categorie = vente.categorieEcheance(code, false, false);
        categorie.setIsConfermentBasedDate(conferment);
        return categoryRepository.saveAndFlush(categorie);
    }

    /** Dossier courant : l'article de la tranche le pointe ({@code StkItem.ComFolderId}) et réciproquement. */
    public ComFolder dossierCourant(String code, CfgTranche tranche) {
        return vente.dossier(code, tranche, StatutDossier.FORMALISE);
    }

    /**
     * Dossier sorti ({@code ANNULATION} / {@code DESISTEMENT}) dont {@code ComFolder.StkItemId} vise encore
     * l'article, alors que l'article ne le pointe plus (comme les 31 dossiers sortis de production).
     */
    public ComFolder dossierSorti(String code, StkItem article, StatutDossier statut) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(article.getStkVocation());
        folder.setStrStatus(vente.statut(statut));
        folder.setCfgCustomer(vente.tier("CLI-" + code));
        folder.setStkItem(article);
        return folderRepository.saveAndFlush(folder);
    }

    /** Article libéré : plus aucun dossier ne l'occupe ({@code StkItem.ComFolderId} nul). */
    public void libererArticle(UUID articleId) {
        executer(entityManager.createNativeQuery("UPDATE StkItem SET ComFolderId = NULL WHERE PKey = :id")
                .setParameter(ID, articleId));
    }

    public ComDocument echeance(ComFolder folder, ComSettlementCategory categorie, BigDecimal montant,
                                LocalDateTime date) {
        ComDocument document = vente.echeance(folder, categorie, montant, date);
        document.setAmountHT(montant);
        document.setAmountTTC(montant);
        return document;
    }

    /** Règlement encaissé ({@code IsCredit}) de {@code montant} sur l'échéance, au statut donné. */
    public void reglement(ComDocument echeance, BigDecimal montant, String statut) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("REG-P1C-" + UUID.randomUUID());
        s.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        s.setAmount(montant);
        s.setStatus(statut);
        s.setIsCredit(true);
        s = settlementRepository.save(s);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(s);
        ligne.setComDocument(echeance);
        ligne.setAmount(montant);
        settlementLineRepository.saveAndFlush(ligne);
    }

    // ------------------------------------------------------------------ lecture

    /** Ligne {@code ComDocument} relue en base : DocDate, Amount, AmountHT, AmountTTC, UserUpdate, UserUpdateDateTime. */
    public Tuple ligneEcheance(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return (Tuple) entityManager.createNativeQuery("""
                        SELECT DocDate, Amount, AmountHT, AmountTTC, UserUpdate, UserUpdateDateTime
                        FROM ComDocument WHERE PKey = :id""", Tuple.class)
                .setParameter(ID, id)
                .getSingleResult();
    }

    public LocalDateTime dateEcheance(UUID id) {
        Object date = ligneEcheance(id).get("DocDate");
        return date instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) date;
    }

    /** Lignes d'une requête native à paramètre {@code :id}. */
    @SuppressWarnings("unchecked")
    public List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(ID, id).getResultList();
    }

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
