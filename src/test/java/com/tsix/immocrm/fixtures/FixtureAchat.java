package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
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

    public static final String FACTURE = "INVOICE";
    public static final String DOCUMENT_A_REGLER = "DOCUMENT_TO_PAY";
    public static final String AVOIR = "REFUND";

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

    /** Ligne {@code AdmUserHabilitation.CfgCompanyId} : sans elle, aucune pièce de la société (FEATURE-visibilite-omran.md R3). */
    public void habiliterSociete(AdmUser compte, UUID cfgCompanyId) {
        executer(em.createNativeQuery("""
                        INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgCompanyId) VALUES (:id, :user, :societe)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("user", compte.getId())
                .setParameter("societe", cfgCompanyId));
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

    /** Lot A3 : pièce de {@code nature} donnée, à payer signé (récupération d'avance négative possible). */
    public UUID piece(String code, String nature, UUID fournisseur, String fournisseurNom, UUID societe, UUID projet,
                      UUID statut, String aPayer) {
        return piece(Piece.facture(code, LocalDate.of(2026, 5, 4), fournisseur, fournisseurNom, societe, projet,
                new BigDecimal(aPayer)).nature(nature).statut(statut));
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

    // ------------------------------------------------------------------ lot C8 « liste des pièces » (FEATURE-achat-liste-parite.md)

    public static final String DROIT_BC_SANS_RESTRICTION = "Yes.Buy.Front.Order.OrderList.WithNoRestriction";

    /** Types d'état de workflow ({@code AdmWorkFlowStateType}) aux PKey de production. */
    public static final UUID ETAT_TERMINE_SUCCES = UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA");
    public static final UUID ETAT_TERMINE_ECHEC = UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97");
    public static final UUID ETAT_ANNULATION = UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0");
    public static final UUID ETAT_INTERMEDIAIRE = UUID.fromString("B53E0A4F-3D67-4BF9-8C0E-B24155865FD6");

    private static final Map<String, UUID> MODULES = Map.of(
            "BUY", UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00003"),
            "STL", UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00004"),
            "PRJ", UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00005"));
    private static final Map<String, String> LIBELLES_MODULES = Map.of(
            "BUY", "Achat", "STL", "Trésorerie", "PRJ", "Projet");

    /** Module {@code AdmModule} BUY / STL / PRJ à sa PKey de production, créé s'il manque ; rend la PKey du code. */
    public UUID module(String code) {
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmModule WHERE Code = :code)
                            INSERT INTO AdmModule (PKey, Code, Description) VALUES (:id, :code, :libelle)""")
                .setParameter(P_ID, MODULES.get(code)).setParameter(P_CODE, code)
                .setParameter("libelle", LIBELLES_MODULES.get(code)));
        Object id = em.createNativeQuery(
                        "SELECT TOP 1 CAST(PKey AS varchar(36)) FROM AdmModule WHERE Code = :code ORDER BY PKey ASC")
                .setParameter(P_CODE, code).getSingleResult();
        return UUID.fromString(id.toString());
    }

    /** Type de lot ({@code PrjMarketType}). */
    public UUID typeLot(String code, String libelle) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO PrjMarketType (PKey, Code, Description) VALUES (:id, :code, :libelle)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("libelle", libelle));
        return id;
    }

    /** Devise ({@code StlCurrency}). */
    public UUID devise(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    /** Dossier de groupement ({@code BuyFolder}, absente de yesRemsTest : créée dans la transaction si besoin). */
    public UUID dossierGroupement(String code, String description) {
        executer(em.createNativeQuery("""
                IF OBJECT_ID('dbo.BuyFolder', 'U') IS NULL
                    CREATE TABLE dbo.BuyFolder (Pkey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                        Description nvarchar(255) NULL, IsClosed bit NULL, UserCreate nvarchar(255) NULL,
                        UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                        Notes nvarchar(255) NULL)"""));
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO BuyFolder (Pkey, Code, Description) VALUES (:id, :code, :description)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", description));
        return id;
    }

    /** Statut de pièce rattaché au type d'état {@code etat} (créé à sa PKey de production s'il manque), couleur donnée. */
    public UUID statutDEtat(String code, UUID etat, String couleur) {
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmWorkFlowStateType WHERE PKey = :id)
                            INSERT INTO AdmWorkFlowStateType (PKey, Code, Description) VALUES (:id, :code, :code)""")
                .setParameter(P_ID, etat).setParameter(P_CODE, "ETAT-" + etat.toString().substring(0, 8)));
        UUID statut = statut(code, false, false, 50);
        executer(em.createNativeQuery("UPDATE BuyDocumentStatus SET AdmWorkFlowStateTypeId = :etat WHERE PKey = :id")
                .setParameter(P_ID, statut).setParameter("etat", etat));
        if (couleur != null) {
            executer(em.createNativeQuery("UPDATE BuyDocumentStatus SET ColorName = :couleur WHERE PKey = :id")
                    .setParameter(P_ID, statut).setParameter("couleur", couleur));
        }
        return statut;
    }

    /** Colonne de {@code BuyDocument} remise à NULL (nom fixe ; évite de lier un null sans type). */
    public void effacer(UUID pieceId, String colonne) {
        executer(em.createNativeQuery("UPDATE BuyDocument SET " + colonne + " = NULL WHERE PKey = :id")
                .setParameter(P_ID, pieceId));
    }

    /** Colonne d'une table de référence écrite par le test ({@code table} / {@code colonne} fixes, jamais une saisie). */
    public void poserSur(String table, UUID id, String colonne, Object valeur) {
        executer(em.createNativeQuery("UPDATE " + table + " SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter(P_ID, id).setParameter("valeur", valeur));
    }

    /** Droit OMRAN ({@code AdmRight.Code}) porté par le rôle du compte, tel que {@code DroitsOmran} le lit. */
    public void droitOmran(AdmUser compte, String code) {
        executer(em.createNativeQuery("""
                        INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId)
                        SELECT TOP (1) :id, :code, :code, ur.AdmRoleId FROM AdmUserRole ur WHERE ur.AdmUserId = :user
                        ORDER BY ur.AdmRoleId ASC""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter(P_CODE, code).setParameter("user", compte.getId()));
    }

    /** Compte sans personnel : {@code AdmUser.CfgEmployeeId} NULL en base et dans le principal ({@code connecte}). */
    public void sansPersonnel(AdmUser compte) {
        compte.setCfgEmployee(null);
        executer(em.createNativeQuery("UPDATE AdmUser SET CfgEmployeeId = NULL WHERE PKey = :id")
                .setParameter(P_ID, compte.getId()));
    }

    // ------------------------------------------------------------------ lot A2a « fiche pièce » (FEATURE-achat-fiche-parite.md)

    public static final String DEVIS = "QUOTE";
    public static final String BON_COMMANDE = "ORDER";
    public static final String BON_LIVRAISON = "DELIVRY";

    /** Ligne complète ; fractions comme OMRAN (remise, FODEC, TVA) ; {@code code} nul possible ; PU TTC stocké tel quel. */
    public record LigneAchat(int ordre, String code, String designation, String unite, BigDecimal quantite,
                             BigDecimal prixUnitaireHT, BigDecimal tauxTva, BigDecimal remise, BigDecimal fodec,
                             BigDecimal prixUnitaireTTC) {
    }

    public UUID ligne(UUID pieceId, LigneAchat l) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentLine (Pkey, BuyDocumentId, Sorting, Code, Description, Unity, Quantity,
                            UnitPriceHT, UnitPriceTTC, VatRatio, DiscountRatio, FODECRatio)
                        VALUES (:id, :piece, :ordre, :code, :designation, :unite, :qte, :pu, :puttc, :tva, :remise, :fodec)""")
                .setParameter(P_ID, id).setParameter("piece", pieceId).setParameter("ordre", l.ordre())
                .setParameter(P_CODE, l.code() == null ? "" : l.code()).setParameter("designation", l.designation())
                .setParameter("unite", l.unite()).setParameter("qte", l.quantite()).setParameter("pu", l.prixUnitaireHT())
                .setParameter("puttc", l.prixUnitaireTTC()).setParameter("tva", l.tauxTva())
                .setParameter("remise", l.remise()).setParameter("fodec", l.fodec()));
        if (l.code() == null) {
            executer(em.createNativeQuery("UPDATE BuyDocumentLine SET Code = NULL WHERE Pkey = :id").setParameter(P_ID, id));
        }
        return id;
    }

    /** Ventilation par projet ({@code BuyVentilationExpense}). */
    public void ventilationProjet(UUID pieceId, UUID projet, BigDecimal taux) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyVentilationExpense (Pkey, BuyDocumentId, PrjProjectId, VentilationRate)
                        VALUES (:id, :piece, :projet, :taux)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("piece", pieceId).setParameter("projet", projet)
                .setParameter("taux", taux));
    }

    /** Lien BC ↔ facture de la table {@code BuyOrderInvoice} (absente de yesRemsTest et de la CI au 12/11 : créée si besoin). */
    public void lierFactureACommande(UUID bonCommande, UUID facture) {
        executer(em.createNativeQuery("""
                IF OBJECT_ID('dbo.BuyOrderInvoice', 'U') IS NULL
                    CREATE TABLE dbo.BuyOrderInvoice (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                        BuyOrderId uniqueidentifier NULL, BuyInvoiceId uniqueidentifier NULL,
                        UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                        UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)"""));
        executer(em.createNativeQuery("""
                        INSERT INTO BuyOrderInvoice (PKey, BuyOrderId, BuyInvoiceId) VALUES (:id, :bc, :facture)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("bc", bonCommande).setParameter("facture", facture));
    }

    /** Transition de workflow ({@code BuyDocumentWorkFlow}) entre deux statuts existants. */
    public UUID transition(UUID depart, UUID arrivee, String description) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentWorkFlow (PKey, Sorting, StartStatusId, EndStatusId, Description)
                        VALUES (:id, 1, :depart, :arrivee, :description)""")
                .setParameter(P_ID, id).setParameter("depart", depart).setParameter("arrivee", arrivee)
                .setParameter("description", description));
        return id;
    }

    /** Passage d'historique avec sa transition ({@code buyDocumentWorkFlowId}, colonne en minuscule). */
    public void historique(UUID pieceId, UUID statut, LocalDateTime date, AdmUser auteur, String note, UUID transition) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentStatusHistory (PKey, DocDate, BuyDocumentStatusId, BuyDocumentId, AdmUserId, Notes,
                            buyDocumentWorkFlowId)
                        VALUES (:id, :date, :statut, :piece, :user, :note, :transition)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("date", date).setParameter("statut", statut)
                .setParameter("piece", pieceId).setParameter("user", auteur.getId()).setParameter("note", note)
                .setParameter("transition", transition));
    }

    // ------------------------------------------------------------------ lot A1 bis « agrégats des BC » (FEATURE-achat-bc-agregats.md)

    public static final String AVANCE_COMMANDE = "ORDER_ADVANCE";
    public static final String GARANTIE_COMMANDE = "ORDER_WARRANTY";

    /** Montants stockés de la pièce, posés tels quels (TVA = TTC − HT) : HT, TTC et à payer indépendants. */
    public void montants(UUID pieceId, String ht, String ttc, String aPayer) {
        BigDecimal h = new BigDecimal(ht);
        BigDecimal t = new BigDecimal(ttc);
        executer(em.createNativeQuery("""
                        UPDATE BuyDocument SET AmountHT = :ht, AmountVat = :tva, AmountTTC = :ttc, AmountToPay = :apayer
                        WHERE PKey = :id""")
                .setParameter(P_ID, pieceId).setParameter("ht", h).setParameter("tva", t.subtract(h))
                .setParameter("ttc", t).setParameter("apayer", new BigDecimal(aPayer)));
    }

    /** Ligne de plus sur un règlement existant ({@code reglement}). */
    public void ligneReglement(UUID reglement, UUID pieceId, BigDecimal montant) {
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :piece, :montant)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("reglement", reglement)
                .setParameter("piece", pieceId).setParameter("montant", montant));
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
