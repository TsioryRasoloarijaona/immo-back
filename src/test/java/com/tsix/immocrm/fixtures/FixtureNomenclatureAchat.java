package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

/**
 * Lot N8 « nomenclature d'achat » (FEATURE-n8-nomenclature.md). Tables {@code BuyItem*} absentes de yesRemsTest : créées
 * ici si besoin (définition de {@code ci/schema/010_tables.sql}), annulées avec la transaction du test. Parents écrits
 * avant les FK de la CI. Les slugs, routes et noms de champs ci-dessous sont le contrat du fil.
 */
@Component
public class FixtureNomenclatureAchat {

    // Slugs du moteur (FEATURE-n8 « Brief front ») et route de l'écran Articles
    public static final String FAMILLES = "familles-achat";
    public static final String SOUS_FAMILLES = "sous-familles-achat";
    public static final String NATURES_FOURNISSEUR = "natures-fournisseur";
    public static final String ARTICLES = "/api/achat/config/articles";
    public static final String ARTICLE = ARTICLES + "/{id}";

    // Champs du fil
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String SORTING = "sorting";
    public static final String NOTES = "notes";
    public static final String CATEGORIE = "categorieComptableId";
    public static final String FAMILLE = "familleId";
    public static final String SOUS_FAMILLE = "sousFamilleId";
    public static final String UNITE = "unite";
    /** Décimaux de l'article (R4), dans l'ordre des colonnes {@code BuyItem}. */
    public static final List<String> DECIMAUX = List.of("prixUnitaireHT", "fodec", "coutLogistiqueHT", "coutLogistiqueTva",
            "coutIndirectHT", "coutIndirectTva", "coutFinancierHT", "coutFinancierTva", "prixVenteHT", "venteTva");

    // Droits OMRAN (AdmRight.Code, D6)
    public static final String DROIT_FAMILLES = "Yes.Buy.Front.Config.BuyItemFamily";
    public static final String DROIT_SOUS_FAMILLES = "Yes.Buy.Front.Config.BuyItemUnderFamily";
    public static final String DROIT_ARTICLES = "Yes.Buy.Front.Config.BuyItemList";
    public static final String DROIT_ARTICLES_NOUVEAU = DROIT_ARTICLES + ".toolNew";
    public static final String DROIT_ARTICLES_SUPPRIMER = DROIT_ARTICLES + ".toolDelete";
    public static final String DROIT_NATURES = "Yes.Buy.Front.Config.CfgTierKind";

    /** PKey de production de {@code CfgTierType} FOU (Supplier d'OMRAN) et CLI. */
    public static final UUID TYPE_FOURNISSEUR = UUID.fromString("00685105-DFD9-497B-8830-DBF0FBE91668");
    public static final UUID TYPE_CLIENT = UUID.fromString("07830036-EF72-492A-AC39-0A407392DA62");

    public static final String ADMINISTRATEUR = "Administrateur";
    private static final String ROLE_SANS_DROIT = "Acheteur";

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_CATEGORIE = "categorie";

    private static final String TABLE_FAMILLE = """
            IF OBJECT_ID(N'dbo.BuyItemFamily', N'U') IS NULL
            CREATE TABLE dbo.BuyItemFamily (Pkey uniqueidentifier NOT NULL CONSTRAINT BuyItemFamily_PK PRIMARY KEY,
                Code nvarchar(255) NULL, Description nvarchar(255) NULL, VatRatio decimal(26, 6) NULL,
                StlCategoryId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, Notes nvarchar(500) NULL, Sorting int NULL)""";
    private static final String TABLE_SOUS_FAMILLE = """
            IF OBJECT_ID(N'dbo.BuyItemUnderFamily', N'U') IS NULL
            CREATE TABLE dbo.BuyItemUnderFamily (Pkey uniqueidentifier NOT NULL CONSTRAINT BuyItemUnderFamily_PK PRIMARY KEY,
                Code nvarchar(255) NULL, Description nvarchar(255) NULL, VatRatio decimal(26, 6) NULL,
                BuyItemFamilyId uniqueidentifier NULL, StlCategoryId uniqueidentifier NULL, UserCreate nvarchar(255) NULL,
                UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
                Notes nvarchar(500) NULL, Sorting int NULL)""";
    private static final String TABLE_ARTICLE = """
            IF OBJECT_ID(N'dbo.BuyItem', N'U') IS NULL
            CREATE TABLE dbo.BuyItem (PKey uniqueidentifier NOT NULL CONSTRAINT BuyItem_PK PRIMARY KEY,
                Code nvarchar(255) NULL, Description nvarchar(255) NULL, VatRatio decimal(26, 6) NULL,
                StlCategoryId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, Notes nvarchar(500) NULL,
                BuyItemUnderFamilyId uniqueidentifier NULL, Unity nvarchar(255) NULL, UnitPriceHT decimal(26, 6) NULL,
                FODECRatio decimal(26, 6) NULL, CostLogisticHT decimal(26, 6) NULL, CostLogisticVatRatio decimal(26, 6) NULL,
                CostIndirectHT decimal(26, 6) NULL, CostIndirectVatRatio decimal(26, 6) NULL,
                CostFinacialHT decimal(26, 6) NULL, CostFinacialVatRatio decimal(26, 6) NULL,
                SaleUnitPriceHT decimal(26, 6) NULL, SaleVatRatio decimal(26, 6) NULL)""";

    /** Tables qui citent un article (D3) : nom de la table, colonne clé. */
    public enum TableCitante {
        BUY_DOCUMENT_LINE("BuyDocumentLine", "Pkey"),
        PRJ_WORKING_OUT_LINE("PrjWorkingOutLine", "Pkey"),
        PRJ_MARKET_LINE("PrjMarketLine", "PKey"),
        PRJ_MARKET_LINE_ESTIMATION("PrjMarketLineEstimation", "PKey");

        private final String table;
        private final String cle;

        TableCitante(String table, String cle) {
            this.table = table;
            this.cle = cle;
        }

        public String table() {
            return table;
        }
    }

    private final EntityManager em;
    private final FixtureDossierVente dossiers;
    private final FixtureAchat achat;

    public FixtureNomenclatureAchat(EntityManager em, FixtureDossierVente dossiers, FixtureAchat achat) {
        this.em = em;
        this.dossiers = dossiers;
        this.achat = achat;
    }

    public static String jeton(String prefixe) {
        return FixtureAchat.jeton(prefixe);
    }

    // ------------------------------------------------------------------ schéma et parents

    /** Crée les 3 tables {@code BuyItem*} si yesRemsTest ne les a pas, et les types de tiers FOU et CLI. */
    public void preparer() {
        executer(em.createNativeQuery(TABLE_FAMILLE));
        executer(em.createNativeQuery(TABLE_SOUS_FAMILLE));
        executer(em.createNativeQuery(TABLE_ARTICLE));
        typeTiers(TYPE_FOURNISSEUR, "FOU", "FOURNISSEUR");
        typeTiers(TYPE_CLIENT, "CLI", "CLIENT");
    }

    private void typeTiers(UUID id, String code, String libelle) {
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM CfgTierType WHERE PKey = :id)
                            INSERT INTO CfgTierType (PKey, Code, Description) VALUES (:id, :code, :libelle)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("libelle", libelle));
    }

    /** Catégorie de trésorerie ({@code StlCategory}). */
    public UUID categorie(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO StlCategory (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    // ------------------------------------------------------------------ nomenclature

    public UUID famille(String code, UUID categorie) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyItemFamily (Pkey, Code, Description, StlCategoryId, Sorting)
                        VALUES (:id, :code, :description, :categorie, 1)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Famille " + code)
                .setParameter(P_CATEGORIE, categorie));
        return id;
    }

    public UUID sousFamille(String code, UUID famille, UUID categorie) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyItemUnderFamily (Pkey, Code, Description, BuyItemFamilyId, StlCategoryId, Sorting)
                        VALUES (:id, :code, :description, :famille, :categorie, 1)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Sous-famille " + code)
                .setParameter("famille", famille).setParameter(P_CATEGORIE, categorie));
        return id;
    }

    public UUID article(String code, String description, String unite, UUID sousFamille, UUID categorie) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyItem (PKey, Code, Description, Unity, BuyItemUnderFamilyId, StlCategoryId)
                        VALUES (:id, :code, :description, :unite, :sousFamille, :categorie)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", description)
                .setParameter("unite", unite).setParameter("sousFamille", sousFamille).setParameter(P_CATEGORIE, categorie));
        return id;
    }

    /** Ligne de {@code table} qui cite l'article, sans autre parent (colonnes nullables partout). */
    public void citer(TableCitante citante, UUID article) {
        executer(em.createNativeQuery("INSERT INTO " + citante.table + " (" + citante.cle + ", BuyItemId) VALUES (:id, :article)")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("article", article));
    }

    // ------------------------------------------------------------------ natures de tiers

    public UUID nature(String code, UUID typeTiers, int tri) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO CfgTierKind (PKey, Sorting, Code, Description, CfgTierTypeId)
                        VALUES (:id, :tri, :code, :description, :type)""")
                .setParameter(P_ID, id).setParameter("tri", tri).setParameter(P_CODE, code)
                .setParameter("description", "Nature " + code).setParameter("type", typeTiers));
        return id;
    }

    /** Tiers fournisseur qui porte cette nature. */
    public UUID tiersDeNature(String code, UUID nature) {
        UUID id = achat.fournisseur(code, "Fournisseur " + code);
        achat.poserSur("CfgTier", id, "CfgTierTypeId", TYPE_FOURNISSEUR);
        achat.poserSur("CfgTier", id, "CfgTierKindId", nature);
        return id;
    }

    // ------------------------------------------------------------------ comptes

    /** Administrateur en base ({@code AdmUserRole}, lu par {@code DroitsOmran}) et dans le JWT. */
    public RequestPostProcessor administrateur() {
        AdmUser compte = dossiers.compte("n8-admin", ADMINISTRATEUR);
        em.flush();
        return FixtureDossierVente.connecte(compte, ADMINISTRATEUR);
    }

    /** Compte non administrateur portant exactement ces droits OMRAN (aucun si vide). */
    public RequestPostProcessor avecDroits(String... droits) {
        AdmUser compte = dossiers.compte("n8-acheteur", ROLE_SANS_DROIT);
        em.flush();
        for (String droit : droits) {
            achat.droitOmran(compte, droit);
        }
        return FixtureDossierVente.connecte(compte, ROLE_SANS_DROIT);
    }

    // ------------------------------------------------------------------ lecture

    public int compter(String sql, UUID id) {
        em.flush();
        Object n = em.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
