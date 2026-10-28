package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lignes du lot P7c (FEATURE-patrimoine-lotissements.md), <b>validées pour de bon</b> en JDBC (chaque ordre est sa propre
 * transaction : aucun verrou tenu sur le paramétrage partagé) ; tout est effacé par {@link #nettoyer(Jeu)}. Le jeu crée ses
 * propres entité, programme, tranches, vocation, catégories et types ; seules les lignes de référence de production
 * (natures, entités StrEntity, vocations VENTE / PROJET) sont posées si absentes, une fois par JVM.
 */
@Component
public class FixtureLotissements {

    private static final Logger log = LoggerFactory.getLogger(FixtureLotissements.class);

    // ------------------------------------------------------------------ routes et types du fil
    public static final String LOTISSEMENTS = "/api/lotissements";
    public static final String LOTISSEMENT = "/api/lotissements/{id}";
    public static final String FICHE_PDF = "/api/lotissements/{id}/fiche.pdf";
    public static final String TYPE_IMPORT = "lotissements";
    public static final String OBJET_LOT = "Yes.Stk.Land.Land";
    public static final String LIBELLE_OBJET_LOT = "Lot de terrain";
    public static final String ADDED = "Added";
    public static final String MODIFIED = "Modified";
    public static final String DELETED = "Deleted";

    // ------------------------------------------------------------------ champs de LotissementEcritureDto (§2)
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String TRANCHE_ID = "trancheId";
    public static final String VOCATION_ID = "vocationId";
    public static final String CATEGORIE_ID = "categorieId";
    public static final String TYPE_ID = "typeId";
    public static final String PRESENTATION_ID = "presentationId";
    public static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    public static final String SURFACE_DEFINITIVE = "surfaceDefinitive";
    public static final String PRIX_M2_DEFINITIF_HT = "prixM2DefinitifHT";
    public static final String PRIX_M2_DEFINITIF_TTC = "prixM2DefinitifTTC";
    public static final String TAUX_TVA = "tauxTva";
    public static final String TITRE_FONCIER = "titreFoncier";
    public static final String NOTES = "notes";

    // ------------------------------------------------------------------ PKey de production
    public static final UUID NATURE_TER = FixtureReferentielsAdmin.NATURE_TER;
    public static final UUID NATURE_LOT_DE_TERRAIN = UUID.fromString("4BDDEF6B-1111-2222-3333-1FB318D6C444");
    public static final UUID NATURE_BAT = FixturePatrimoine.NATURE_BAT;
    public static final UUID VOCATION_VENTE = FixturePatrimoine.VOCATION_VENTE;
    public static final UUID VOCATION_PROJET = FixturePatrimoine.VOCATION_PROJET;
    public static final UUID ENTITE_STK_ITEM = FixturePatrimoine.ENTITE_STK_ITEM;
    public static final UUID ENTITE_LAND_PROJECT = FixtureTerrains.ENTITE_LAND_PROJECT;

    private static final String ENTITE_ARTICLE = "SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'StkItem' ORDER BY PKey ASC";
    private static final String ENTITE_TERRAIN =
            "SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'LandProject' ORDER BY PKey ASC";
    private static final String ARTICLES_DE_LA_TRANCHE = "SELECT PKey FROM StkItem WHERE StkHierarchyId = ?";
    private static final String CODE_DESCRIPTION = "INSERT INTO %s (PKey, Code, Description) VALUES (?, ?, ?)";

    public static final BigDecimal MILLE = new BigDecimal("1000");

    private static volatile boolean referentielsPoses;

    private final JdbcTemplate jdbc;

    public FixtureLotissements(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * Jeu d'un test : {@code tranche} et {@code autreTranche} du même programme, entité active ; catégorie / type de nature
     * TER ({@code categorie}, {@code type}), LOT DE TERRAIN ({@code categorieLot}, {@code typeLot}) et BAT.
     */
    public record Jeu(String jeton, AdmUser admin, UUID entite, UUID projet, UUID tranche, UUID autreTranche,
                      UUID vocation, String codeVocation, Classement ter, Classement lotDeTerrain, Classement bat,
                      List<String[]> suivis, List<UUID> objets) {

        public String code(String suffixe) {
            return suffixe + "-" + jeton;
        }
    }

    public record Classement(UUID categorie, String codeCategorie, UUID type, String codeType) {
    }

    // ------------------------------------------------------------------ préparation

    public Jeu preparer(String prefixe) {
        referentiels();
        String jeton = prefixe + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Jeu j = new Jeu(jeton, admin(jeton), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "VOC-" + jeton, null, null, null, new ArrayList<>(), new ArrayList<>());
        suivre(j, "AdmUser", j.admin().getId());
        jdbc.update("INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (?, ?, ?, 1)",
                j.entite().toString(), "ENT-" + jeton, "Entite " + jeton);
        suivre(j, "CfgCompany", j.entite());
        jdbc.update("INSERT INTO CfgProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)",
                j.projet().toString(), "PRJ-" + jeton, "Programme " + jeton, j.entite().toString());
        suivre(j, "CfgProject", j.projet());
        tranche(j, j.tranche(), "TR-" + jeton);
        tranche(j, j.autreTranche(), "TR2-" + jeton);
        codeDescription(j, "StkVocation", j.vocation(), j.codeVocation());
        return new Jeu(jeton, j.admin(), j.entite(), j.projet(), j.tranche(), j.autreTranche(), j.vocation(),
                j.codeVocation(), classement(j, NATURE_TER, "T"), classement(j, NATURE_LOT_DE_TERRAIN, "L"),
                classement(j, NATURE_BAT, "B"), j.suivis(), j.objets());
    }

    /** Corps de création complet et valide : tranche du jeu, vocation du jeu, catégorie / type TER, 1 000 m². */
    public static CorpsJson corpsLot(Jeu j, String code) {
        return CorpsJson.corps().avec(CODE, code).avec(DESCRIPTION, description(code)).avec(TRANCHE_ID, j.tranche())
                .avec(VOCATION_ID, j.vocation()).avec(CATEGORIE_ID, j.ter().categorie()).avec(TYPE_ID, j.ter().type())
                .avec(SURFACE_PROVISOIRE, MILLE).avec(SURFACE_DEFINITIVE, MILLE);
    }

    /** Tranche d'une autre entité, inactive (R4 : invisible). */
    public UUID trancheInactive(Jeu j) {
        UUID entite = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (?, ?, ?, 0)", entite.toString(),
                "ENTI-" + j.jeton(), "Entite inactive " + j.jeton());
        suivre(j, "CfgCompany", entite);
        UUID projet = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)",
                projet.toString(), "PRJI-" + j.jeton(), "Programme inactif " + j.jeton(), entite.toString());
        suivre(j, "CfgProject", projet);
        UUID tranche = UUID.randomUUID();
        tranche(j, tranche, "TRI-" + j.jeton());
        jdbc.update("UPDATE CfgTranche SET CfgProjectId = ? WHERE PKey = ?", projet.toString(), tranche.toString());
        return tranche;
    }

    public void desactiverEntite(Jeu j) {
        jdbc.update("UPDATE CfgCompany SET IsActive = 0 WHERE PKey = ?", j.entite().toString());
    }

    // ------------------------------------------------------------------ lots et voisins

    /** Lot libre de la tranche du jeu : nature TER, entité « article », vocation du jeu, 1 000 m², TVA 0,2. */
    public UUID lot(Jeu j, String code) {
        return lot(j, code, j.tranche());
    }

    public UUID lot(Jeu j, String code, UUID trancheId) {
        return item(j, new Item(code, NATURE_TER, j.vocation(), j.ter(), trancheId, entite(ENTITE_ARTICLE)));
    }

    /** Terrain (LandProject) sans tranche, vocation PROJET. */
    public UUID terrainSansTranche(Jeu j, String code) {
        return item(j, new Item(code, NATURE_TER, VOCATION_PROJET, j.ter(), null, entite(ENTITE_TERRAIN)));
    }

    /** Terrain LandProject rattaché à la tranche (§1.1 : ne devient pas lot). */
    public UUID terrainLandProjectDeTranche(Jeu j, String code) {
        return item(j, new Item(code, NATURE_TER, j.vocation(), j.ter(), j.tranche(), entite(ENTITE_TERRAIN)));
    }

    /** Article bâti de la tranche. */
    public UUID article(Jeu j, String code) {
        return item(j, new Item(code, NATURE_BAT, VOCATION_VENTE, j.bat(), j.tranche(), entite(ENTITE_ARTICLE)));
    }

    private record Item(String code, UUID nature, UUID vocation, Classement classement, UUID tranche, UUID strEntite) {
    }

    private UUID item(Jeu j, Item i) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO StkItem (PKey, Code, Description, StkKindId, StkVocationId, StkItemCategoryId, StkItemTypeId,
                            StkHierarchyId, Area, AreaFloor, Vat, IsFinished, IsUnderItem, IsUnderItemOrigin, IsTitled,
                            AlwaysAffected, CfgCompanyId, StrEntityId)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1000, 1000, 0.2, 1, 0, 0, 1, 0, ?, ?)""",
                id.toString(), i.code(), description(i.code()), i.nature().toString(), i.vocation().toString(),
                i.classement().categorie().toString(), i.classement().type().toString(), texte(i.tranche()),
                j.entite().toString(), i.strEntite().toString());
        j.objets().add(id);
        return id;
    }

    /** Libellé écrit par la fixture : porte le code, donc le jeton (nettoyage de l'audit par libellé). */
    public static String description(String code) {
        return "Lot " + code;
    }

    /** Dossier de vente réel posé sur le lot (FK StkItem.ComFolderId en CI ; aucun article sur le dossier). */
    public UUID occuper(Jeu j, UUID lotId) {
        UUID dossier = UUID.randomUUID();
        jdbc.update("INSERT INTO ComFolder (PKey, Code, StkVocationId) VALUES (?, ?, ?)", dossier.toString(),
                "DOS-" + j.jeton() + "-" + dossier.toString().substring(0, 4), VOCATION_VENTE.toString());
        suivre(j, "ComFolder", dossier);
        jdbc.update("UPDATE StkItem SET ComFolderId = ? WHERE PKey = ?", dossier.toString(), lotId.toString());
        return dossier;
    }

    public UUID presentation(Jeu j, UUID trancheId, String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StkPresentationType (PKey, Code, Description, StkHierarchyId) VALUES (?, ?, ?, ?)",
                id.toString(), code, "Presentation " + code, trancheId.toString());
        suivre(j, "StkPresentationType", id);
        return id;
    }

    public void definir(UUID lotId, String colonne, Object valeur) {
        jdbc.update("UPDATE StkItem SET " + colonne + " = ? WHERE PKey = ?", valeur, lotId.toString());
    }

    /** Ligne de contenance du lot (StkItemContenance) sur une contenance propre au jeu. */
    public void contenance(Jeu j, UUID lotId) {
        UUID contenance = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO StkContenance (PKey, Code, Description, Sorting, Option1, IsOption1WithQuantity, Option2,
                            IsOption2WithQuantity, Option3, IsOption3WithQuantity)
                        VALUES (?, ?, ?, 1, NULL, 0, NULL, 0, NULL, 0)""",
                contenance.toString(), "CONT-" + j.jeton(), "Contenance " + j.jeton());
        suivre(j, "StkContenance", contenance);
        jdbc.update("INSERT INTO StkItemContenance (PKey, StkItemId, StkContenanceId, ItemsNumber) VALUES (?, ?, ?, 2)",
                UUID.randomUUID().toString(), lotId.toString(), contenance.toString());
    }

    public void tracePlan(UUID lotId) {
        jdbc.update("INSERT INTO StkHierarchyPath (PKey, SvgPath, StkItemId) VALUES (?, 'M0 0', ?)",
                UUID.randomUUID().toString(), lotId.toString());
    }

    public void note(UUID lotId) {
        jdbc.update("INSERT INTO AdmMessage (PKey, CategoryId, Description) VALUES (?, ?, N'Note P7c')",
                UUID.randomUUID().toString(), lotId.toString());
    }

    public void pieceJointe(UUID lotId) {
        jdbc.update("INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, Description) VALUES (?, N'StkItem', ?, N'Piece P7c')",
                UUID.randomUUID().toString(), lotId.toString());
    }

    /** Prospection qui cite le lot (ComProspection.StkItemId, FK NO ACTION en production). */
    public void prospection(Jeu j, UUID lotId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ComProspection (PKey, Code, StkItemId) VALUES (?, ?, ?)", id.toString(),
                "PROS-" + j.jeton(), lotId.toString());
        suivre(j, "ComProspection", id);
    }

    public UUID etage(Jeu j) {
        UUID id = UUID.randomUUID();
        codeDescription(j, "StkFloor", id, "ETG-" + j.jeton());
        return id;
    }

    // ------------------------------------------------------------------ lecture

    public Map<String, Object> ligne(UUID id) {
        return jdbc.queryForMap("""
                SELECT Code, Description, Notes, TitleNumber, Area, AreaFloor, Vat, ComSaleUnitCostHT, ComSaleUnitCost,
                       SalePriceHT, SalePrice, CAST(IsFinished AS int) AS IsFinished, CAST(IsUnderItem AS int) AS IsUnderItem,
                       CONVERT(varchar(36), StkKindId) AS StkKindId, CONVERT(varchar(36), StkHierarchyId) AS StkHierarchyId,
                       CONVERT(varchar(36), StkItemCategoryId) AS StkItemCategoryId,
                       CONVERT(varchar(36), StkPresentationTypeId) AS StkPresentationTypeId,
                       CONVERT(varchar(36), StrStatusId) AS StrStatusId
                FROM StkItem WHERE PKey = ?""", id.toString());
    }

    /** PKey des StkItem de ce code dans la tranche (une création par l'API se retrouve sans lire la réponse). */
    public List<UUID> ids(UUID trancheId, String code) {
        return jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM StkItem WHERE StkHierarchyId = ? AND Code = ?",
                String.class, trancheId.toString(), code).stream().map(UUID::fromString).toList();
    }

    public boolean existe(UUID id) {
        return compter("SELECT COUNT(*) FROM StkItem WHERE PKey = ?", id.toString()) == 1;
    }

    public boolean entiteArticle(UUID id) {
        return compter("""
                SELECT COUNT(*) FROM StkItem i JOIN StrEntity e ON e.PKey = i.StrEntityId
                WHERE i.PKey = ? AND e.Code = N'StkItem'""", id.toString()) == 1;
    }

    public int audit(UUID objetId, String action) {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ? AND ObjectType = ?",
                objetId.toString(), action, OBJET_LOT);
    }

    /** Lignes d'audit de type lot dont le libellé porte le jeton du jeu (objets créés par un import). */
    public int auditDuJeu(Jeu j, String action) {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE Action = ? AND ObjectType = ? AND ObjectLabel LIKE ?",
                action, OBJET_LOT, "%" + j.jeton() + "%");
    }

    public List<Map<String, Object>> lignesAudit(UUID objetId, String action) {
        return jdbc.queryForList("""
                SELECT ObjectType, ObjectTypeLabel, ObjectLabel FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""",
                objetId.toString(), action);
    }

    public int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------ nettoyage

    public void nettoyer(Jeu j) {
        if (j == null) {
            return;
        }
        List<String> tranches = j.suivis().stream().filter(s -> "CfgTranche".equals(s[0])).map(s -> s[1]).toList();
        List<String> objets = new ArrayList<>(j.objets().stream().map(UUID::toString).toList());
        for (String tranche : tranches) {
            objets.addAll(jdbc.queryForList(ARTICLES_DE_LA_TRANCHE, String.class, tranche));
        }
        for (String id : objets) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM StkItemContenance WHERE StkItemId = ?", id);
            effacer("DELETE FROM StkHierarchyPath WHERE StkItemId = ?", id);
            effacer("DELETE FROM AdmMessage WHERE CategoryId = ?", id);
            effacer("DELETE FROM AdmAttachedFile WHERE CategoryId = ?", id);
            effacer("DELETE FROM ComProspection WHERE StkItemId = ?", id);
            effacer("UPDATE StkItem SET ComFolderId = NULL WHERE PKey = ?", id);
        }
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectLabel LIKE ?", "%" + j.jeton() + "%");
        for (String tranche : tranches) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM StkHierarchyPlan WHERE StkHierarchyId = ?)",
                    tranche);
            effacer("DELETE FROM StkHierarchyPlan WHERE StkHierarchyId = ?", tranche);
        }
        for (String id : objets) {
            effacer("DELETE FROM StkItem WHERE PKey = ?", id);
        }
        List<String[]> suivis = j.suivis();
        for (int k = suivis.size() - 1; k >= 0; k--) {
            String[] ligne = suivis.get(k);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    // ------------------------------------------------------------------ outils

    /** Lignes de référence de production, posées si absentes ; une fois par JVM (les autres agents les lisent aussi). */
    private void referentiels() {
        if (referentielsPoses) {
            return;
        }
        siAbsent("StkKind", NATURE_TER, "TER", "TERRAIN");
        siAbsent("StkKind", NATURE_LOT_DE_TERRAIN, "LOT DE TERRAIN", "LOT DE TERRAIN");
        siAbsent("StkKind", NATURE_BAT, "BAT", "BATIMENT");
        siAbsent("StkVocation", VOCATION_VENTE, "VENTE", "VENTE");
        siAbsent("StkVocation", VOCATION_PROJET, "PROJET", "PROJET");
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'StkItem')
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'StkItem', N'Article')""",
                ENTITE_STK_ITEM.toString());
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'LandProject')
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'LandProject', N'Terrains')""",
                ENTITE_LAND_PROJECT.toString());
        referentielsPoses = true;
    }

    private void siAbsent(String table, UUID id, String code, String description) {
        jdbc.update("IF NOT EXISTS (SELECT 1 FROM " + table + " WHERE PKey = ?) " + CODE_DESCRIPTION.formatted(table),
                id.toString(), id.toString(), code, description);
    }

    private AdmUser admin(String jeton) {
        UUID id = UUID.randomUUID();
        String code = "admin.lotissements." + jeton.toLowerCase(Locale.ROOT);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)", id.toString(), code,
                "Admin Lotissements");
        AdmUser admin = new AdmUser();
        admin.setId(id);
        admin.setCode(code);
        admin.setDescription("Admin Lotissements");
        return admin;
    }

    private void tranche(Jeu j, UUID id, String code) {
        jdbc.update("INSERT INTO CfgTranche (PKey, Code, Description, CfgProjectId) VALUES (?, ?, ?, ?)", id.toString(),
                code, "Tranche " + code, j.projet().toString());
        suivre(j, "CfgTranche", id);
    }

    private Classement classement(Jeu j, UUID nature, String lettre) {
        UUID categorie = UUID.randomUUID();
        String codeCategorie = "CAT" + lettre + "-" + j.jeton();
        jdbc.update("INSERT INTO StkItemCategory (PKey, Code, Description, StkKindId, ForUnderItem) VALUES (?, ?, ?, ?, 0)",
                categorie.toString(), codeCategorie, "Categorie " + codeCategorie, nature.toString());
        suivre(j, "StkItemCategory", categorie);
        UUID type = UUID.randomUUID();
        String codeType = "TYP" + lettre + "-" + j.jeton();
        jdbc.update("INSERT INTO StkItemType (PKey, Code, Description, StkKindId, ForUnderItem) VALUES (?, ?, ?, ?, 0)",
                type.toString(), codeType, "Type " + codeType, nature.toString());
        suivre(j, "StkItemType", type);
        return new Classement(categorie, codeCategorie, type, codeType);
    }

    private void codeDescription(Jeu j, String table, UUID id, String code) {
        jdbc.update(CODE_DESCRIPTION.formatted(table), id.toString(), code, "Libelle " + code);
        suivre(j, table, id);
    }

    private UUID entite(String sql) {
        return UUID.fromString(jdbc.queryForObject(sql, String.class));
    }

    private static String texte(UUID id) {
        return id == null ? null : id.toString();
    }

    private static void suivre(Jeu j, String table, UUID id) {
        j.suivis().add(new String[]{table, id.toString()});
    }

    /** Nettoyage au mieux : une ligne annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }
}
