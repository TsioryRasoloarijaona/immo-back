package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rapports du stock (FEATURE-patrimoine-rapports.md §1–§6) : lignes {@code AdmReportData} du Type patrimoine, seuils
 * {@code AdmParamsGuid}, transferts de produit, pièces d'article, actionnaires. Tout s'annule avec la transaction du test.
 */
@Component
public class FixtureRapportsPatrimoine {

    public static final String VALORISATION = "StkTdbValorisation";
    public static final String MOUVEMENTS = "StkTdbMotion";
    public static final String SUPERFICIES = "StkTdbAreaForSale";
    public static final String ACTIONNAIRES = "StkTdbCompanyShareholder";
    public static final String VERIFICATION = "StkVerifAttachment";
    public static final List<String> CODES_P10A = List.of(VALORISATION, MOUVEMENTS, SUPERFICIES, ACTIONNAIRES,
            VERIFICATION);
    public static final List<String> CODES_HORS_LOT = List.of("StkTdbComparatifAuMettreCarre",
            "StkCostPriceByCategories", "StkCostPriceByCategoriesByUnit", "StkTdbSuiviProjet");

    public static final String SEUIL_CONCRETISATION = "DefaultComFolderStatusConcretisation";
    public static final String SEUIL_RESILIATION = "DefaultComFolderStatusResiliation";
    public static final String SEUIL_ANNULATION = "DefaultComFolderStatusCancellation";

    /** PKey de production de la vocation LOCATION (absente de yesRemsTest). */
    public static final UUID VOCATION_LOCATION = UUID.fromString("39659C7B-96DF-4B3B-ADD5-64B06A60086F");
    public static final String CATEGORIE_PIECE_ARTICLE = "Yes.Stk.Building.Item";

    // yesRemsTest n'a que 11 colonnes de ComSaleWithdrawalProduct (ci/schema/112) ; la CI a la table complète (010).
    private static final String COLONNE_DOSSIER = """
            IF COL_LENGTH('dbo.ComSaleWithdrawalProduct', 'ComSaleId') IS NULL
            ALTER TABLE dbo.ComSaleWithdrawalProduct ADD ComSaleId uniqueidentifier NULL""";
    private static final String COLONNE_VALIDITE = """
            IF COL_LENGTH('dbo.ComSaleWithdrawalProduct', 'ValidityDate') IS NULL
            ALTER TABLE dbo.ComSaleWithdrawalProduct ADD ValidityDate datetime NULL""";

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final FixtureRapports rapports;
    private final StkVocationRepository vocations;
    private final StkItemRepository articles;
    private final StrStatusRepository statuts;

    public FixtureRapportsPatrimoine(JdbcTemplate jdbc, EntityManager entityManager, FixtureRapports rapports,
                                     StkVocationRepository vocations, StkItemRepository articles,
                                     StrStatusRepository statuts) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.rapports = rapports;
        this.vocations = vocations;
        this.articles = articles;
        this.statuts = statuts;
    }

    // ------------------------------------------------------------------ AdmReportData (§1)

    /** Les 5 rapports de P10a avec libellé, groupe et ordre de la prod ; code → PKey. */
    public Map<String, UUID> rapportsP10a() {
        rapports.tables();
        Map<String, UUID> ids = new LinkedHashMap<>();
        ids.put(VALORISATION, patrimoine(VALORISATION, "VALORISATION DU STOCK", "VALORISATION", 10));
        ids.put(MOUVEMENTS, patrimoine(MOUVEMENTS, "MOUVEMENTS DU STOCK", "MOUVEMENTS", 20));
        ids.put(SUPERFICIES, patrimoine(SUPERFICIES, "ETAT DES SUPERFICIES VENDABLE", "STOCK", 10));
        ids.put(ACTIONNAIRES, patrimoine(ACTIONNAIRES, "ACTIONNAIRES - ENTITE JURIDIQUES", "STOCK", 4000));
        ids.put(VERIFICATION, patrimoine(VERIFICATION, "VERIFICATION DES ATTACHEMENTS", "ARTICLES", 10000));
        return ids;
    }

    /** Les 4 rapports patrimoine hors lot (P10b et Suivi des projets), sans classe : masqués par le socle. */
    public void rapportsHorsLot() {
        rapports.tables();
        int ordre = 30;
        for (String code : CODES_HORS_LOT) {
            patrimoine(code, "HORS LOT " + code, "COUT", ordre++);
        }
    }

    private UUID patrimoine(String code, String libelle, String groupe, int ordre) {
        return rapports.rapport(FixtureRapports.TYPE_PATRIMOINE, code, libelle, groupe, ordre);
    }

    // ------------------------------------------------------------------ articles

    public StkVocation location() {
        return vocations.findById(VOCATION_LOCATION).orElseGet(() -> {
            StkVocation v = new StkVocation();
            v.setId(VOCATION_LOCATION);
            v.setCode("LOCATION");
            v.setDescription("LOCATION");
            return vocations.save(v);
        });
    }

    /** {@code AreaFloor}, {@code SalePriceFloor}, {@code Area}, {@code SalePrice} (valeurs stockées, nulles permises). */
    public StkItem mesures(StkItem article, String surfaceBase, String valeurBase, String surfaceFinale,
                           String valeurFinale) {
        article.setAreaFloor(decimal(surfaceBase));
        article.setSalePriceFloor(decimal(valeurBase));
        article.setArea(decimal(surfaceFinale));
        article.setSalePrice(decimal(valeurFinale));
        return articles.save(article);
    }

    public StkItem enregistrer(StkItem article) {
        return articles.save(article);
    }

    private static BigDecimal decimal(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    // ------------------------------------------------------------------ statuts et seuils (§3)

    public StrStatus statut(String code, int ordre, boolean annulation, boolean resiliation) {
        StrStatus s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(code);
        s.setDescription(code);
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        s.setAnnulation(annulation);
        s.setResiliation(resiliation);
        s.setSorting(ordre);
        return statuts.save(s);
    }

    /** {@code AdmParamsGuid} (clé = Code) posé ou remplacé. */
    public void seuil(String code, StrStatus statut) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
        jdbc.update("INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, NEWID())", code,
                statut.getId().toString());
    }

    public void sansSeuil(String code) {
        entityManager.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
    }

    // ------------------------------------------------------------------ transfert de produit (§3)

    /**
     * Transfert du dossier de {@code ancien} vers {@code nouveau} ({@code ComSaleWithdrawalProduct}), avec la ligne
     * {@code StrFolderStatusHistory} du transfert au statut {@code statutHistorique}.
     */
    public void transfert(ComFolder dossier, StkItem ancien, StkItem nouveau, LocalDateTime validite,
                          StrStatus statutHistorique) {
        entityManager.flush();
        jdbc.execute(COLONNE_DOSSIER);
        jdbc.execute(COLONNE_VALIDITE);
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO ComSaleWithdrawalProduct (PKey, Code, ComSaleId, StkOldItemId, StkItemId, ValidityDate) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", id, "TRF-" + id.substring(0, 8), dossier.getId().toString(),
                ancien.getId().toString(), nouveau.getId().toString(), validite);
        jdbc.update("INSERT INTO StrFolderStatusHistory (PKey, DocDate, ComFolderId, StrFolderStatusId, "
                        + "ComSaleWithdrawalProductId, Description) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), validite, dossier.getId().toString(),
                statutHistorique.getId().toString(), id, "TRANSFERT");
    }

    // ------------------------------------------------------------------ pièces d'article (§6)

    public UUID typePiece(String code) {
        entityManager.flush();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmAttachedFileType (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(),
                code, code);
        return id;
    }

    /** Pièce jointe à l'article ; {@code type} nul = sans type ; {@code cochee} = vérifiée non jointe. */
    public void piece(StkItem article, UUID type, boolean cochee) {
        entityManager.flush();
        jdbc.update("INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, AdmAttachedFileTypeId, Description, "
                        + "IsCheckedButNotAttached) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID().toString(),
                CATEGORIE_PIECE_ARTICLE, article.getId().toString(), type == null ? null : type.toString(),
                "Piece " + article.getCode(), cochee);
    }

    // ------------------------------------------------------------------ actionnaires (§5)

    public UUID actionnaire(String libelle) {
        entityManager.flush();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgShareholder (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(),
                libelle, libelle);
        return id;
    }

    /** Ligne {@code CfgCompanyShareholder} ; {@code parts} peut être nul. */
    public void part(UUID actionnaire, CfgCompany entite, Integer parts) {
        entityManager.flush();
        jdbc.update("INSERT INTO CfgCompanyShareholder (PKey, CfgCompanyId, CfgShareholderId, SharingNumber) "
                + "VALUES (?, ?, ?, ?)", UUID.randomUUID().toString(), entite.getId().toString(),
                actionnaire.toString(), parts);
    }
}
