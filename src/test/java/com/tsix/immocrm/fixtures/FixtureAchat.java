package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lot A1 — Achat, pièces en lecture (FEATURE-achat-lecture-factures.md). Tout en SQL natif : aucune entité {@code Prj*}
 * (lot P1 en parallèle). Parents écrits avant les FK de la CI ; tout est annulé avec la transaction du test.
 */
@Component
public class FixtureAchat {

    public static final String EN_COURS = "IN_WORK";
    public static final String POINTE = "POINTED";
    public static final String ANNULE = "CANCELED";

    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    private final EntityManager em;
    private final FixtureDossierVente dossiers;

    public FixtureAchat(EntityManager em, FixtureDossierVente dossiers) {
        this.em = em;
        this.dossiers = dossiers;
    }

    /** Suffixe propre au test : yesRemsTest est partagée. */
    public static String jeton(String prefixe) {
        return prefixe + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ------------------------------------------------------------------ référentiels

    public UUID societe(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (:id, :code, :code, 1)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    /** Projet de chantier ({@code PrjProject}). */
    public UUID projet(String code, UUID societe) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId)
                        VALUES (:id, :code, :description, :societe)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Projet " + code)
                .setParameter("societe", societe));
        return id;
    }

    /** Lot de marché ({@code PrjMarket}) du projet. */
    public UUID lot(String code, UUID projet) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId) VALUES (:id, :code, :code, :projet)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("projet", projet));
        return id;
    }

    /** Fournisseur ({@code CfgTier}) : même nom en Description, raison sociale (et sur la pièce). */
    public UUID fournisseur(String code, String nom) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO CfgTier (PKey, Code, Description, SocialReason) VALUES (:id, :code, :nom, :nom)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("nom", nom));
        return id;
    }

    public UUID statut(String code, boolean okPaiement, boolean valideCompta, int ordre) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting, IsOkForPayement, IsValidatedOnAccounting)
                        VALUES (:id, :code, :description, :ordre, :ok, :compta)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Statut " + code)
                .setParameter("ordre", ordre).setParameter("ok", okPaiement).setParameter("compta", valideCompta));
        return id;
    }

    public UUID categorie(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (:id, :code, :description, 0)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Categorie " + code));
        return id;
    }

    public UUID typeReglement(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementType (PKey, Code, Description) VALUES (:id, :code, :description)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Type " + code));
        return id;
    }

    // ------------------------------------------------------------------ comptes et habilitations

    /** Compte actif, rôles {@code AdmUserRole} donnés, aucune habilitation. */
    public AdmUser compte(String code, String... roles) {
        AdmUser user = dossiers.compte(code, roles);
        em.flush();
        return user;
    }

    /** Ligne {@code AdmUserHabilitation.PrjProjectId} du compte. */
    public void habiliter(AdmUser compte, UUID prjProjectId) {
        executer(em.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (:id, :user, :projet)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("user", compte.getId())
                .setParameter("projet", prjProjectId));
    }

    // ------------------------------------------------------------------ pièces

    /** Pièce d'achat ; {@code projet}, {@code lot}, {@code statut} nuls possibles. */
    public record Piece(String code, String nature, LocalDate date, UUID fournisseur, String fournisseurNom,
                        UUID societe, UUID projet, UUID lot, UUID statut, BigDecimal aPayer,
                        String reference, String description) {

        public static Piece facture(String code, LocalDate date, UUID fournisseur, String fournisseurNom, UUID societe,
                                    UUID projet, BigDecimal aPayer) {
            return new Piece(code, "INVOICE", date, fournisseur, fournisseurNom, societe, projet, null, null, aPayer,
                    "REF-" + code, "Description " + code);
        }

        public Piece nature(String n) {
            return new Piece(code, n, date, fournisseur, fournisseurNom, societe, projet, lot, statut, aPayer,
                    reference, description);
        }

        public Piece lot(UUID l) {
            return new Piece(code, nature, date, fournisseur, fournisseurNom, societe, projet, l, statut, aPayer,
                    reference, description);
        }

        public Piece statut(UUID s) {
            return new Piece(code, nature, date, fournisseur, fournisseurNom, societe, projet, lot, s, aPayer,
                    reference, description);
        }

        public Piece textes(String ref, String desc) {
            return new Piece(code, nature, date, fournisseur, fournisseurNom, societe, projet, lot, statut, aPayer,
                    ref, desc);
        }
    }

    /** HT = à payer / 1,2, TVA = le reste, TTC = à payer ; échéance = date + 30 j. */
    public UUID piece(Piece p) {
        UUID id = UUID.randomUUID();
        BigDecimal ht = p.aPayer().divide(new BigDecimal("1.2"), 6, java.math.RoundingMode.HALF_UP);
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocument (PKey, Code, DocType, DocRef, Description, DocDate, MaturityDate, CfgTierId,
                            SupplierDescription, CfgCompanyId, PrjProjectId, PrjMarketId, BuyDocumentStatusId,
                            AmountHT, AmountVat, AmountTTC, AmountToPay, UserCreate, UserCreateDateTime)
                        VALUES (:id, :code, :nature, :ref, :description, :date, :echeance, :tiers, :nom, :societe, :projet,
                            :lot, :statut, :ht, :tva, :ttc, :ttc, N'TEST-ACHAT', :date)""")
                .setParameter(P_ID, id).setParameter(P_CODE, p.code()).setParameter("nature", p.nature())
                .setParameter("ref", p.reference()).setParameter("description", p.description())
                .setParameter("date", p.date().atStartOfDay())
                .setParameter("echeance", p.date().plusDays(30).atStartOfDay())
                .setParameter("tiers", p.fournisseur()).setParameter("nom", p.fournisseurNom())
                .setParameter("societe", p.societe()).setParameter("projet", p.projet()).setParameter("lot", p.lot())
                .setParameter("statut", p.statut()).setParameter("ht", ht).setParameter("tva", p.aPayer().subtract(ht))
                .setParameter("ttc", p.aPayer()));
        return id;
    }

    /** Colonne de {@code BuyDocument} écrite par le test (nom fixe, jamais une saisie). */
    public void poser(UUID pieceId, String colonne, Object valeur) {
        executer(em.createNativeQuery("UPDATE BuyDocument SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter(P_ID, pieceId).setParameter("valeur", valeur));
    }

    // ------------------------------------------------------------------ règlements, lignes, ventilation, historique

    /** Règlement fournisseur ({@code IsCredit = 0}) d'une ligne sur la pièce ; {@code statut} nul possible. */
    public UUID reglement(UUID pieceId, String code, BigDecimal montantLigne, String statut, LocalDate date,
                          UUID type) {
        UUID reglement = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, DocDate, Amount, Status, IsCredit, StlSettlementTypeId)
                        VALUES (:id, :code, :date, :montant, :statut, 0, :type)""")
                .setParameter(P_ID, reglement).setParameter(P_CODE, code).setParameter("date", date.atStartOfDay())
                .setParameter("montant", montantLigne).setParameter("statut", statut).setParameter("type", type));
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :piece, :montant)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("reglement", reglement)
                .setParameter("piece", pieceId).setParameter("montant", montantLigne));
        return reglement;
    }

    /** Ligne {@code BuyDocumentLine} ; {@code tauxTva} en fraction (0.2 = 20 %), comme OMRAN. */
    public UUID ligne(UUID pieceId, int ordre, String designation, String unite, BigDecimal quantite,
                      BigDecimal prixUnitaireHT, BigDecimal tauxTva) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentLine (Pkey, BuyDocumentId, Sorting, Code, Description, Unity, Quantity,
                            UnitPriceHT, UnitPriceTTC, VatRatio, DiscountRatio)
                        VALUES (:id, :piece, :ordre, :code, :designation, :unite, :qte, :pu, :puttc, :tva, 0)""")
                .setParameter(P_ID, id).setParameter("piece", pieceId).setParameter("ordre", ordre)
                .setParameter(P_CODE, "L" + ordre).setParameter("designation", designation).setParameter("unite", unite)
                .setParameter("qte", quantite).setParameter("pu", prixUnitaireHT)
                .setParameter("puttc", prixUnitaireHT.multiply(BigDecimal.ONE.add(tauxTva)))
                .setParameter("tva", tauxTva));
        return id;
    }

    public void ventilation(UUID pieceId, UUID categorie, BigDecimal taux) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyVentilationCategory (PKey, StlCategoryId, BuyDocumentId, VentilationRate)
                        VALUES (:id, :categorie, :piece, :taux)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("categorie", categorie)
                .setParameter("piece", pieceId).setParameter("taux", taux));
    }

    public void historique(UUID pieceId, UUID statut, LocalDateTime date, AdmUser auteur, String note) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentStatusHistory (PKey, DocDate, BuyDocumentStatusId, BuyDocumentId, AdmUserId, Notes)
                        VALUES (:id, :date, :statut, :piece, :user, :note)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("date", date).setParameter("statut", statut)
                .setParameter("piece", pieceId).setParameter("user", auteur.getId()).setParameter("note", note));
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
