package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlAccountRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Affectations de règlements de vente (FEATURE-suivi-affectations.md §2) : échéances, pièces, lignes. Le patrimoine
 * et les dossiers viennent de {@link FixtureEtatVentes} (un dossier par article : trigger {@code trgAfterComFolderInsert}).
 */
@Component
public class FixtureSuiviAffectations {

    public static final String IN_WORK = "IN_WORK";
    public static final String POINTED = "POINTED";
    public static final String CANCELED = "CANCELED";

    private final FixtureEtatVentes etatVentes;
    private final Depots depots;

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(ComDocumentRepository echeances, ComSettlementCategoryRepository categories,
                         StlSettlementRepository pieces, StlSettlementLineRepository lignes,
                         StlSettlementTypeRepository types, StlAccountRepository comptes, CfgTierRepository tiers,
                         ComFolderRepository dossiers) {
    }

    public FixtureSuiviAffectations(FixtureEtatVentes etatVentes, Depots depots) {
        this.etatVentes = etatVentes;
        this.depots = depots;
    }

    public FixtureEtatVentes etatVentes() {
        return etatVentes;
    }

    // ------------------------------------------------------------------ référentiels

    public ComSettlementCategory categorie(String code) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Catégorie " + code);
        categorie.setStkVocation(etatVentes.vente());
        return depots.categories().save(categorie);
    }

    public StlSettlementType typePiece(String code) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription("Type " + code);
        return depots.types().save(type);
    }

    public StlAccount compte(String code) {
        StlAccount compte = new StlAccount();
        compte.setId(UUID.randomUUID());
        compte.setCode(code);
        compte.setDescription("Compte " + code);
        return depots.comptes().save(compte);
    }

    public CfgTier banque(String nom) {
        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode("BQ-" + nom);
        banque.setDescription(nom);
        return depots.tiers().save(banque);
    }

    /** Dossier de vente sans article : sa tranche n'est pas résolue (§6.4 du périmètre). */
    public ComFolder dossierSansArticle(String code) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(etatVentes.vente());
        return depots.dossiers().save(folder);
    }

    // ------------------------------------------------------------------ échéances, pièces, lignes

    /** Échéance de vente ({@code ComDocument}) ; {@code categorie} peut être nulle. */
    public ComDocument echeance(ComFolder dossier, ComSettlementCategory categorie, BigDecimal montant) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setComSettlementCategory(categorie);
        echeance.setDocDate(LocalDateTime.of(2025, 1, 15, 0, 0));
        echeance.setAmount(montant);
        return depots.echeances().save(echeance);
    }

    /**
     * Pièce de règlement ; {@code credit} et {@code etat} peuvent être nuls (IsCredit NULL, Status NULL).
     * {@code echeance} et {@code operation} alimentent {@code ISNULL(SettlementDate, EcheanceDate)} (R4).
     */
    public StlSettlement piece(String libelle, Boolean credit, String etat, LocalDateTime echeance,
                               LocalDateTime operation, BigDecimal montant) {
        StlSettlement piece = new StlSettlement();
        piece.setId(UUID.randomUUID());
        piece.setDescription(libelle);
        piece.setIsCredit(credit);
        piece.setStatus(etat);
        piece.setDocDate(LocalDateTime.of(2025, 1, 2, 0, 0));
        piece.setEcheanceDate(echeance);
        piece.setSettlementDate(operation);
        piece.setAmount(montant);
        return depots.pieces().save(piece);
    }

    public StlSettlement enregistrer(StlSettlement piece) {
        return depots.pieces().save(piece);
    }

    /** Ligne d'affectation vers une échéance de vente ({@code ComDocumentId} renseigné). */
    public StlSettlementLine ligne(StlSettlement piece, ComDocument echeance, BigDecimal montant) {
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(piece);
        ligne.setComDocument(echeance);
        ligne.setAmount(montant);
        return depots.lignes().save(ligne);
    }

    /** Ligne sans échéance de vente ({@code ComDocumentId} NULL) — une ligne d'achat pour l'écran (R1). */
    public StlSettlementLine ligneHorsVente(StlSettlement piece, BigDecimal montant) {
        return ligne(piece, null, montant);
    }

    public void relire() {
        etatVentes.relire();
    }
}
