package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Lot A2b-2b « générer une pièce d'achat » (FEATURE-achat-generation.md) : paramètres {@code AdmParamsBoolean} (ligne absente
 * possible), gestions multi-fournisseur aux PKeys de prod, pièces sources typées, lignes complètes (catégorie, bordereau),
 * lignes de bordereau {@code PrjMarketLine}. Parents écrits avant les FK de la CI ; tout est annulé avec la transaction du test.
 */
@Component
public class FixtureGenerationAchat {

    public static final String FACTURE_DEPUIS_BC = "GenerateInvoiceFromOrder";
    public static final String FACTURE_PLUSIEURS_BC = "GenerateInvoiceFromManyOrders";

    public static final String GESTION_DEVIS_LOT = "PrjMarketQuote";
    public static final String GESTION_DEVIS_MULTI = "PrjMarketMultiSupplierQuote";
    public static final String GESTION_BC_MULTI = "PrjMarketMultiSupplierOrder";
    public static final String GESTION_BL_MULTI = "PrjMarketMultiSupplierDelivery";
    public static final String GESTION_FACTURE_MULTI = "PrjInvoiceDebitMultiSupplier";

    /** Gestions hors saisie libre, aux PKeys de yesRemsProd (mesuré le 17/11). */
    private static final Map<String, UUID> GESTIONS_PROD = Map.of(
            GESTION_DEVIS_LOT, UUID.fromString("15C10DF0-61D0-4A74-AE69-DDDCD4301118"),
            GESTION_DEVIS_MULTI, UUID.fromString("39E9AE05-D862-444A-BA77-73D57E09801F"),
            GESTION_BC_MULTI, UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42C000"),
            GESTION_BL_MULTI, UUID.fromString("E934A458-560E-43F0-82BC-EE940EA3DF16"),
            GESTION_FACTURE_MULTI, UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B612"));

    private static final String P_ID = "id";

    private final EntityManager em;
    private final FixtureAchat achat;

    public FixtureGenerationAchat(EntityManager em, FixtureAchat achat) {
        this.em = em;
        this.achat = achat;
    }

    // ------------------------------------------------------------------ paramètres

    /** Les trois paramètres de génération ; {@code null} = ligne absente. */
    public void parametres(Boolean factureDepuisBc, Boolean plusieursBc, Boolean limiteQuantites) {
        parametre(FACTURE_DEPUIS_BC, factureDepuisBc);
        parametre(FACTURE_PLUSIEURS_BC, plusieursBc);
        parametre(FixtureAchat.LIMITE_QUANTITES, limiteQuantites);
    }

    /** Paramètre {@code AdmParamsBoolean} posé, ou ligne supprimée si {@code valeur} est nulle (table créée si absente). */
    public void parametre(String code, Boolean valeur) {
        achat.parametreBooleen(code, Boolean.TRUE.equals(valeur));
        if (valeur == null) {
            executer(em.createNativeQuery("DELETE FROM AdmParamsBoolean WHERE Code = :code").setParameter("code", code));
        }
    }

    // ------------------------------------------------------------------ gestions et pièces sources

    /** Gestion hors saisie libre (multi-fournisseur, devis de lot) à sa PKey de prod, transitions détachées ; rend sa PKey. */
    public UUID gestion(String code) {
        return achat.gestionVide(code, Objects.requireNonNull(GESTIONS_PROD.get(code), code));
    }

    /** Pièce du cadre (à payer 100, sans lot) de gestion donnée ; {@code statut} nul possible. */
    public UUID source(Cadre c, String suffixe, String nature, UUID gestion, UUID statut) {
        UUID id = achat.piece(c, suffixe, nature);
        achat.poser(id, "StrEntityId", gestion);
        if (statut != null) {
            achat.poser(id, "BuyDocumentStatusId", statut);
        }
        return id;
    }

    /** Pièce sans gestion ({@code StrEntityId} NULL, réponse 10 du manager). */
    public void sansGestion(UUID piece) {
        executer(em.createNativeQuery("UPDATE BuyDocument SET StrEntityId = NULL WHERE PKey = :id").setParameter("id", piece));
    }

    /** Ligne de bordereau définitif ({@code PrjMarketLine}) du lot. */
    public UUID ligneBordereau(UUID lot, String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO PrjMarketLine (PKey, Code, Description, PrjMarketId, Sorting, Unity, Quantity, UnitPriceHT,
                            VatRatio, UnitPriceTTC, FODECRatio, IsStock)
                        VALUES (:id, :code, :code, :lot, 1, N'U', 100, 100, 0.2, 120, 0, 0)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("lot", lot));
        return id;
    }

    /** Ligne de pièce complète : taux en fraction, {@code bordereau} et {@code code} nuls possibles. */
    public record LigneSource(int ordre, String code, String designation, String unite, String quantite, String prixHT,
                              String remise, String fodec, String tva, UUID categorie, UUID bordereau) {

        /** Désignation « Article code », unité U, PU 100, sans remise ni FODEC, TVA 20 %, sans bordereau. */
        public static LigneSource simple(int ordre, String code, String quantite, UUID categorie) {
            return new LigneSource(ordre, code, "Article " + code, "U", quantite, "100", "0", "0", "0.2", categorie, null);
        }

        public LigneSource bordereau(UUID b) {
            return new LigneSource(ordre, code, designation, unite, quantite, prixHT, remise, fodec, tva, categorie, b);
        }

        public LigneSource unite(String u) {
            return new LigneSource(ordre, code, designation, u, quantite, prixHT, remise, fodec, tva, categorie, bordereau);
        }
    }

    public UUID ligne(UUID piece, LigneSource l) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = new BigDecimal(l.prixHT());
        BigDecimal puTtc = pu.multiply(BigDecimal.ONE.subtract(new BigDecimal(l.remise())))
                .multiply(BigDecimal.ONE.add(new BigDecimal(l.fodec()))).multiply(BigDecimal.ONE.add(new BigDecimal(l.tva())));
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentLine (Pkey, BuyDocumentId, Sorting, Code, Description, Unity, Quantity,
                            UnitPriceHT, UnitPriceTTC, VatRatio, DiscountRatio, FODECRatio, StlCategoryId, Notes)
                        VALUES (:id, :piece, :ordre, :code, :designation, :unite, :qte, :pu, :puttc, :tva, :remise, :fodec,
                            :categorie, N'')""")
                .setParameter(P_ID, id).setParameter("piece", piece).setParameter("ordre", l.ordre())
                .setParameter("code", l.code() == null ? "" : l.code()).setParameter("designation", l.designation())
                .setParameter("unite", l.unite()).setParameter("qte", new BigDecimal(l.quantite())).setParameter("pu", pu)
                .setParameter("puttc", puTtc).setParameter("tva", new BigDecimal(l.tva()))
                .setParameter("remise", new BigDecimal(l.remise())).setParameter("fodec", new BigDecimal(l.fodec()))
                .setParameter("categorie", l.categorie()));
        if (l.bordereau() != null) {
            executer(em.createNativeQuery("UPDATE BuyDocumentLine SET PrjMarketLineId = :b WHERE Pkey = :id")
                    .setParameter(P_ID, id).setParameter("b", l.bordereau()));
        }
        return id;
    }

    // ------------------------------------------------------------------ lectures

    /** {@code BuyOrderInvoice} créée si absente (définition de yesRemsProd) : les lectures et l'écriture du lot la supposent. */
    public void tableBuyOrderInvoice() {
        executer(em.createNativeQuery("""
                IF OBJECT_ID('dbo.BuyOrderInvoice', 'U') IS NULL
                    CREATE TABLE dbo.BuyOrderInvoice (PKey uniqueidentifier NOT NULL PRIMARY KEY,
                        BuyOrderId uniqueidentifier NULL, BuyInvoiceId uniqueidentifier NULL,
                        UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                        UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)"""));
    }

    /** {@code BuyOrderId} des lignes {@code BuyOrderInvoice} de la facture, en majuscules (doublons gardés). */
    public List<String> commandesDeFacture(UUID facture) {
        return guids("SELECT CAST(BuyOrderId AS varchar(36)) FROM BuyOrderInvoice WHERE BuyInvoiceId = :id", facture);
    }

    /** {@code PrjMarketLineId} des lignes de la pièce, en majuscules ({@code null} gardé). */
    public List<String> bordereauxDesLignes(UUID piece) {
        return guids("SELECT CAST(PrjMarketLineId AS varchar(36)) FROM BuyDocumentLine WHERE BuyDocumentId = :id", piece);
    }

    /** Ids des lignes de la pièce. */
    public List<UUID> lignesDe(UUID piece) {
        return guids("SELECT CAST(Pkey AS varchar(36)) FROM BuyDocumentLine WHERE BuyDocumentId = :id", piece).stream()
                .map(UUID::fromString).toList();
    }

    /** Pièces de {@code nature} du fournisseur. */
    public int nombrePieces(UUID fournisseur, String nature) {
        em.flush();
        Object n = em.createNativeQuery("SELECT COUNT(*) FROM BuyDocument WHERE CfgTierId = :id AND DocType = :nature")
                .setParameter(P_ID, fournisseur).setParameter("nature", nature).getSingleResult();
        return ((Number) n).intValue();
    }

    /** Pièces du fournisseur au code donné (numéro de séquence attendu). */
    public int nombreParCode(UUID fournisseur, String code) {
        em.flush();
        Object n = em.createNativeQuery("SELECT COUNT(*) FROM BuyDocument WHERE CfgTierId = :id AND Code = :code")
                .setParameter(P_ID, fournisseur).setParameter("code", code).getSingleResult();
        return ((Number) n).intValue();
    }

    private List<String> guids(String sql, UUID id) {
        em.flush();
        List<?> valeurs = em.createNativeQuery(sql).setParameter(P_ID, id).getResultList();
        return valeurs.stream().map(v -> v == null ? null : v.toString().toUpperCase()).toList();
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
