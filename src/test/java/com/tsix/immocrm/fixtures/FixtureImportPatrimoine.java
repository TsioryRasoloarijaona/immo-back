package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Données des tests d'import P9a (FEATURE-patrimoine-import-export.md §1), <b>validées pour de bon</b> : l'import
 * ouvre sa propre transaction (simulation = rollback réel, audit écrit au commit), un test {@code @Transactional} ne
 * verrait ni l'un ni l'autre. Tout ce qui est créé est effacé par {@link #nettoyer(Import)}.
 */
@Component
public class FixtureImportPatrimoine {

    private static final Logger log = LoggerFactory.getLogger(FixtureImportPatrimoine.class);

    public static final String OBJET_ARTICLE = "Yes.Stk.Building.Item";
    public static final String OBJET_SOUS_ARTICLE = "Yes.Stk.Building.UnderItem";

    // En-têtes OMRAN exacts (omran-src/Yes.Rems/Yes.Stk.Front.Import/ImportItems.cs, ImportUnderItems.cs).
    public static final String PIVOT = "Pivot";
    public static final String TRANCHE = "Tranche";
    public static final String NATURE = "Nature";
    public static final String VOCATION = "Vocation";
    public static final String CODE = "Code";
    public static final String DESCRIPTION = "Description";
    public static final String IMMEUBLE = "Immeuble";
    public static final String ETAGE = "Etage";
    public static final String CATEGORIE = "Catégorie";
    public static final String TYPE = "Type";
    public static final String NOTES = "Notes";
    public static final String PRIX_PROV_HT = "Prix m² Prov. HT";
    public static final String PRIX_DEFI_HT = "Prix m² Defi. HT";
    public static final String SURFACE_PROV = "Superficie Prov. (m²)";
    public static final String SURFACE_DEFI = "Superficie Defi. (m²)";
    public static final String TVA = "TVA";
    public static final String ARTICLE_PARENT = "Article parent";
    public static final String TOUJOURS_AFFECTE = "Toujours affecté";

    private static final String EFFACER_AUDIT = "DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?";
    private static final String ARTICLES_DE_LA_TRANCHE = "SELECT PKey FROM StkItem WHERE StkHierarchyId = ?";

    private final FixturePatrimoine fixture;
    private final FixtureSousArticles sousArticles;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbc;

    public FixtureImportPatrimoine(FixturePatrimoine fixture, FixtureSousArticles sousArticles,
                                   PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.fixture = fixture;
        this.sousArticles = sousArticles;
        this.transactionManager = transactionManager;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Tranche de test et ses référentiels ; catégorie et type ont un libellé distinct du code (I14). */
    public record Import(AdmUser admin, Contexte c, List<String[]> suivis) {

        public String jeton() {
            return c.jeton();
        }

        public CfgTranche tranche() {
            return c.tranche();
        }

        public UUID trancheId() {
            return c.tranche().getId();
        }
    }

    public Import preparer(String prefixe) {
        AdmUser admin = new TransactionTemplate(transactionManager).execute(statut -> fixture.administrateur());
        Import i = new Import(admin, null, new ArrayList<>());
        Contexte c = dans(i, () -> {
            Contexte cree = sousArticles.contexte(prefixe);
            cree.categorie().setDescription("Libelle categorie " + cree.jeton());
            cree.type().setDescription("Libelle type " + cree.jeton());
            return cree;
        });
        Import pret = new Import(admin, c, i.suivis());
        suivre(pret, "AdmUser", admin.getId());
        suivre(pret, "CfgCompany", c.entite().getId());
        suivre(pret, "CfgProject", c.projet().getId());
        suivre(pret, "CfgTranche", c.tranche().getId());
        suivre(pret, "StkBuilding", c.immeuble().getId());
        suivre(pret, "StkFloor", c.etage().getId());
        suivre(pret, "StkItemCategory", c.categorie().getId());
        suivre(pret, "StkItemType", c.type().getId());
        suivre(pret, "StkItemCategory", c.categorieSous().getId());
        suivre(pret, "StkItemType", c.typeSous().getId());
        return pret;
    }

    /** Article principal existant (63 m², 819 000 TTC, TVA 0,2), validé. */
    public StkItem principal(Import i, String code, Consumer<StkItem> reglage) {
        return dans(i, () -> {
            StkItem item = sousArticles.principal(i.c(), code, i.tranche());
            reglage.accept(item);
            return sousArticles.enregistrer(item);
        });
    }

    public StkItem principal(Import i, String code) {
        return principal(i, code, item -> { });
    }

    /** Sous-article existant de la tranche ; {@code parent} nul = libre. */
    public StkItem sousArticle(Import i, String code, StkItem parent) {
        return dans(i, () -> sousArticles.sousArticle(i.c(), code, parent));
    }

    /** Article principal occupé par un dossier de vente réel (FK StkItem.ComFolderId en CI). */
    public StkItem occupe(Import i, String code) {
        return dans(i, () -> {
            ComFolder dossier = fixture.dossier("DOS-" + code);
            i.suivis().add(new String[]{"ComFolder", dossier.getId().toString()});
            StkItem item = sousArticles.principal(i.c(), code, i.tranche());
            item.setComFolderId(dossier.getId());
            return sousArticles.enregistrer(item);
        });
    }

    /** Nature autre que Bâtiment (ligne propre au test). */
    public StkKind natureAutre(Import i) {
        StkKind nature = dans(i, () -> fixture.nature(UUID.randomUUID(), "NAT-" + i.jeton()));
        suivre(i, "StkKind", nature.getId());
        return nature;
    }

    /** Tranche d'un programme dont l'entité juridique est inactive (R4 → 404). */
    public CfgTranche trancheInvisible(Import i) {
        return dans(i, () -> {
            CfgCompany entite = fixture.entite("ENTI-" + i.jeton(), false);
            CfgProject projet = fixture.projet("PRJI-" + i.jeton(), entite);
            CfgTranche tranche = fixture.tranche("TRI-" + i.jeton(), projet);
            suivre(i, "CfgCompany", entite.getId());
            suivre(i, "CfgProject", projet.getId());
            suivre(i, "CfgTranche", tranche.getId());
            return tranche;
        });
    }

    /** Seconde tranche visible du même programme. */
    public CfgTranche autreTranche(Import i) {
        CfgTranche tranche = dans(i, () -> sousArticles.autreTranche(i.c(), "AUTRE"));
        suivre(i, "CfgTranche", tranche.getId());
        return tranche;
    }

    public String codeNatureBatiment(Import i) {
        return dans(i, () -> fixture.natureBat().getCode());
    }

    public String codeVocationVente(Import i) {
        return dans(i, () -> fixture.vocationVente().getCode());
    }

    // ------------------------------------------------------------------ lecture (hors transaction)

    public List<Map<String, Object>> articles(Import i, String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description, Notes, Vat, AreaFloor, Area,
                       CONVERT(varchar(10), TitleDate, 23) AS TitleDate,
                       CAST(IsUnderItem AS int) AS IsUnderItem, CAST(AlwaysAffected AS int) AS AlwaysAffected,
                       CAST(IsAvailableForSaleInWebSite AS int) AS IsAvailableForSaleInWebSite,
                       CONVERT(varchar(36), ParentId) AS ParentId, CONVERT(varchar(36), StkBuildingId) AS StkBuildingId,
                       CONVERT(varchar(36), StkFloorId) AS StkFloorId,
                       CONVERT(varchar(36), StkItemCategoryId) AS StkItemCategoryId,
                       CONVERT(varchar(36), StkItemTypeId) AS StkItemTypeId, ComSaleUnitCostHT
                FROM StkItem WHERE StkHierarchyId = ? AND Code = ?""", i.trancheId().toString(), code);
    }

    public int nombreArticles(Import i) {
        return compter("SELECT COUNT(*) FROM StkItem WHERE StkHierarchyId = ?", i.trancheId().toString());
    }

    /** Lignes d'audit article / sous-article dont le libellé porte le jeton du test. */
    public int auditDuTest(Import i, String action) {
        return compter("""
                SELECT COUNT(*) FROM AdmWebHistorics WHERE Action = ? AND ObjectType IN (?, ?)
                AND ObjectLabel LIKE ?""", action, OBJET_ARTICLE, OBJET_SOUS_ARTICLE, "%" + i.jeton() + "%");
    }

    public int audit(UUID objetId, String action, String objectType) {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ? AND ObjectType = ?",
                objetId.toString(), action, objectType);
    }

    public int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------------ nettoyage

    public void nettoyer(Import i) {
        SecurityContextHolder.clearContext();
        if (i == null || i.c() == null) {
            return;
        }
        String tranche = i.trancheId().toString();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + ARTICLES_DE_LA_TRANCHE + ")", tranche);
        effacer("DELETE FROM AdmWebHistorics WHERE ParentObjectId IN (" + ARTICLES_DE_LA_TRANCHE + ")", tranche);
        effacer("DELETE FROM StkItem WHERE StkHierarchyId = ? AND ParentId IS NOT NULL", tranche);
        effacer("DELETE FROM StkItem WHERE StkHierarchyId = ?", tranche);
        String admin = i.admin().getId().toString();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM AdmUserHabilitation WHERE AdmUserId = ?)",
                admin);
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin);
        List<String[]> suivis = i.suivis();
        for (int k = suivis.size() - 1; k >= 0; k--) {
            String[] ligne = suivis.get(k);
            effacer(EFFACER_AUDIT, ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    private static void suivre(Import i, String table, UUID id) {
        i.suivis().add(new String[]{table, id.toString()});
    }

    /** Nettoyage au mieux : une ligne annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    /** Écriture de fixture validée, au nom de l'administrateur du test (l'audit P0 lit l'utilisateur courant). */
    private <T> T dans(Import i, Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(i.admin().getId(), i.admin().getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        try {
            return new TransactionTemplate(transactionManager).execute(statut -> ecriture.get());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
