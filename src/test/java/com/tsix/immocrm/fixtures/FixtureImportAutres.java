package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine.Import;
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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Données des tests d'import P9b (FEATURE-patrimoine-import-autres.md) : présentations, terrains, prévisions de base.
 * Validées pour de bon comme {@link FixtureImportPatrimoine} (l'import ouvre sa propre transaction) ; tout ce qui est
 * créé ici, ou par l'import dans les tranches et entités du test, est effacé par {@link #nettoyer(Autres)}.
 */
@Component
public class FixtureImportAutres {

    private static final Logger log = LoggerFactory.getLogger(FixtureImportAutres.class);

    public static final String PRESENTATIONS = "presentations";
    public static final String TERRAINS = "terrains";
    public static final String PREVISIONS = "previsions";
    public static final String OBJET_PRESENTATION = "Yes.Stk.Config.PresentationType";

    // En-têtes (import-export §2.1-§2.3, P9b §3.1-§3.3).
    public static final String TRANCHE = "Tranche";
    public static final String CODE = "Code";
    public static final String DESCRIPTION = "Description";
    public static final String PIVOT = "Pivot";
    public static final String NATURE = "Nature";
    public static final String ENTITE = "Entité";
    public static final String PAYS = "Pays";
    public static final String VILLE = "Ville";
    public static final String VOCATION = "Vocation";
    public static final String CATEGORIE = "Catégorie";
    public static final String TYPE = "Type";
    public static final String SURFACE_PROV = "Superficie Prov. (m²)";
    public static final String SURFACE_DEFI = "Superficie Defi. (m²)";
    public static final String TVA = "TVA";
    public static final String NOTES = "Notes";
    public static final String ANNEE = "Année";
    public static final String MOIS = "Mois";
    public static final String QTE_CONCRETISATION = "Quantité concrétisation";
    public static final String QTE_FINALISATION = "Quantité finalisation";
    public static final String NOMBRE_ARTICLES = "Nombre d'articles";
    public static final String POURCENTAGE = "% concrétisation";

    private static final String P_TRANCHE = "SELECT PKey FROM StkPresentationType WHERE StkHierarchyId = ?";
    private static final String P_ENTITE = "SELECT PKey FROM StkItem WHERE CfgCompanyId = ?";

    private final FixtureImportPatrimoine base;
    private final FixturePatrimoine patrimoine;
    private final FixtureTerrains terrains;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbc;

    public FixtureImportAutres(FixtureImportPatrimoine base, FixturePatrimoine patrimoine, FixtureTerrains terrains,
                               PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.base = base;
        this.patrimoine = patrimoine;
        this.terrains = terrains;
        this.transactionManager = transactionManager;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Contexte P9a (admin, entité, tranche) + suivi des lignes propres à P9b. */
    public record Autres(Import i, List<String> tranches, List<String> entites, List<String[]> suivis) {

        public String jeton() {
            return i.jeton();
        }
    }

    public record Contenance(UUID id, String libelle) {
    }

    public Autres preparer(String prefixe) {
        Import i = base.preparer(prefixe);
        Autres a = new Autres(i, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        a.tranches().add(i.trancheId().toString());
        return a;
    }

    // ------------------------------------------------------------------ classeurs

    /** Classeur d'une feuille : en-têtes = clés de la première ligne ; valeur absente ou {@code null} = cellule vide. */
    public static byte[] fichier(List<Map<String, Object>> lignes) {
        return fichier(ClasseurDeTest.FEUILLE, lignes);
    }

    public static byte[] fichier(String feuille, List<Map<String, Object>> lignes) {
        List<String> entetes = new ArrayList<>(lignes.get(0).keySet());
        List<List<Object>> valeurs = lignes.stream().map(l -> entetes.stream().map(l::get).toList()).toList();
        Map<String, List<List<Object>>> feuilles = new LinkedHashMap<>();
        feuilles.put(feuille, ClasseurDeTest.avecEntetes(entetes, valeurs));
        return ClasseurDeTest.xlsx(feuilles);
    }

    /** Ligne ordonnée : clé, valeur, clé, valeur… */
    public static Map<String, Object> colonnes(Object... clesValeurs) {
        Map<String, Object> l = new LinkedHashMap<>();
        for (int k = 0; k < clesValeurs.length; k += 2) {
            l.put((String) clesValeurs[k], clesValeurs[k + 1]);
        }
        return l;
    }

    // ------------------------------------------------------------------ présentations

    /** Contenance de libellé unique « Contenance &lt;code&gt; » (en-tête de colonne du fichier). */
    public Contenance contenance(Autres a, String code) {
        UUID id = UUID.randomUUID();
        String libelle = "Contenance " + code;
        jdbc.update("INSERT INTO StkContenance (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, libelle);
        suivre(a, "StkContenance", id);
        return new Contenance(id, libelle);
    }

    public UUID presentation(UUID trancheId, String code, String description) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StkPresentationType (PKey, Code, Description, StkHierarchyId) VALUES (?, ?, ?, ?)",
                id.toString(), code, description, trancheId.toString());
        return id;
    }

    public void contenanceDePresentation(UUID presentationId, UUID contenanceId, int nombre) {
        jdbc.update("""
                INSERT INTO StkPresentationTypeContenance (PKey, StkPresentationTypeId, StkContenanceId, ItemsNumber)
                VALUES (?, ?, ?, ?)""", UUID.randomUUID().toString(), presentationId.toString(), contenanceId.toString(),
                nombre);
    }

    public List<Map<String, Object>> presentations(UUID trancheId, String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description FROM StkPresentationType
                WHERE StkHierarchyId = ? AND Code = ?""", trancheId.toString(), code);
    }

    /** ItemsNumber des lignes de cette contenance sur la présentation (vide = ligne absente). */
    public List<Integer> nombres(String presentationId, UUID contenanceId) {
        return jdbc.queryForList("""
                SELECT ItemsNumber FROM StkPresentationTypeContenance
                WHERE StkPresentationTypeId = ? AND StkContenanceId = ?""", Integer.class, presentationId,
                contenanceId.toString());
    }

    // ------------------------------------------------------------------ terrains

    /** Entité active, pays, ville, catégorie et type de nature Terrain (FixtureTerrains §6.1). */
    public FixtureTerrains.Contexte terrains(Autres a) {
        FixtureTerrains.Contexte c = dans(a, () -> terrains.contexte("P9B"));
        a.entites().add(c.entite().getId().toString());
        suivre(a, "CfgCompany", c.entite().getId());
        suivre(a, "AdmCountry", c.pays().getId());
        suivre(a, "AdmCity", c.ville());
        suivre(a, "StkItemCategory", c.categorie().getId());
        suivre(a, "StkItemType", c.type().getId());
        return c;
    }

    public UUID terrain(Autres a, FixtureTerrains.Contexte c, String code) {
        return dans(a, () -> terrains.terrain(c, code));
    }

    /** Terrain occupé par un dossier de vente réel (FK StkItem.ComFolderId en CI). */
    public void occuper(Autres a, UUID terrainId, String code) {
        ComFolder dossier = dans(a, () -> patrimoine.dossier("DOS-" + code));
        suivre(a, "ComFolder", dossier.getId());
        jdbc.update("UPDATE StkItem SET ComFolderId = ? WHERE PKey = ?", dossier.getId().toString(), terrainId.toString());
    }

    /** StkItem de nature Bâtiment portant la société du contexte (T4). */
    public UUID batiment(Autres a, FixtureTerrains.Contexte c, String code) {
        UUID id = terrain(a, c, code);
        UUID bat = dans(a, () -> patrimoine.natureBat().getId());
        jdbc.update("UPDATE StkItem SET StkKindId = ? WHERE PKey = ?", bat.toString(), id.toString());
        return id;
    }

    public void notes(UUID articleId, String notes) {
        jdbc.update("UPDATE StkItem SET Notes = ? WHERE PKey = ?", notes, articleId.toString());
    }

    public String codeVocationProjet(Autres a) {
        return dans(a, () -> patrimoine.vocationProjet().getCode());
    }

    public CfgCompany entiteInactive(Autres a) {
        CfgCompany e = dans(a, () -> patrimoine.entite("ENTX-" + a.jeton(), false));
        a.entites().add(e.getId().toString());
        suivre(a, "CfgCompany", e.getId());
        return e;
    }

    public List<Map<String, Object>> terrainsDe(UUID entiteId, String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), i.PKey) AS PKey, i.Code, i.Description, i.Notes, i.Vat, i.Area, i.AreaFloor,
                       k.Code AS Nature, CONVERT(varchar(36), i.StkItemCategoryId) AS StkItemCategoryId
                FROM StkItem i LEFT JOIN StkKind k ON k.PKey = i.StkKindId
                WHERE i.CfgCompanyId = ? AND i.Code = ?""", entiteId.toString(), code);
    }

    public Map<String, Object> article(UUID id) {
        return jdbc.queryForMap("SELECT Code, Description, Notes FROM StkItem WHERE PKey = ?", id.toString());
    }

    // ------------------------------------------------------------------ prévisions

    public record EnTete(Integer nombre, BigDecimal valorisation, BigDecimal pourcentage, LocalDateTime debutC,
                         LocalDateTime finC, LocalDateTime debutF, LocalDateTime finF, BigDecimal ecartType) {
    }

    public void enTete(UUID trancheId, EnTete e) {
        jdbc.update("""
                UPDATE CfgTranche SET CountItemBasePrevision = ?, TotalSaleBasePrevision = ?, BaseConcretisationPercent = ?,
                    BaseMarketingStartDate = ?, BaseEndDateConcretisation = ?, BaseStartDateFinalisation = ?,
                    BaseMarketingEndDate = ?, BaseEcarttypeConcretisation = ?, BaseEcarttypeFinalisation = ?
                WHERE PKey = ?""", e.nombre(), e.valorisation(), e.pourcentage(), horodatage(e.debutC()),
                horodatage(e.finC()), horodatage(e.debutF()), horodatage(e.finF()), e.ecartType(), e.ecartType(),
                trancheId.toString());
    }

    public Map<String, Object> enTete(UUID trancheId) {
        return jdbc.queryForMap("""
                SELECT CountItemBasePrevision, TotalSaleBasePrevision, BaseConcretisationPercent,
                       CONVERT(varchar(10), BaseMarketingStartDate, 23) AS BaseMarketingStartDate,
                       CONVERT(varchar(10), BaseEndDateConcretisation, 23) AS BaseEndDateConcretisation,
                       CONVERT(varchar(10), BaseStartDateFinalisation, 23) AS BaseStartDateFinalisation,
                       CONVERT(varchar(10), BaseMarketingEndDate, 23) AS BaseMarketingEndDate,
                       BaseEcarttypeConcretisation, BaseEcarttypeFinalisation
                FROM CfgTranche WHERE PKey = ?""", trancheId.toString());
    }

    public void lignePrevision(UUID trancheId, int annee, int mois, int quantite, int finalisation) {
        jdbc.update("""
                INSERT INTO CfgPrevision (PKey, StkHierarchyId, DocDate, [Year], [Month], Quantity, TurnoverQuantity, Notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, N'')""", UUID.randomUUID().toString(), trancheId.toString(),
                Timestamp.valueOf(LocalDateTime.of(annee, mois, 15, 0, 0)), annee, mois, quantite, finalisation);
    }

    /** Lignes CfgPrevision de la tranche, par année et mois. */
    public List<Map<String, Object>> lignesPrevision(UUID trancheId) {
        return jdbc.queryForList("""
                SELECT [Year] AS Annee, [Month] AS Mois, Quantity, TurnoverQuantity, Notes FROM CfgPrevision
                WHERE StkHierarchyId = ? ORDER BY [Year] ASC, [Month] ASC""", trancheId.toString());
    }

    // ------------------------------------------------------------------ audit

    /** Lignes AdmWebHistorics de l'objet ou de ses sous-lignes (contenances d'une présentation). */
    public int audit(String objetId) {
        return base.compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", objetId,
                objetId);
    }

    // ------------------------------------------------------------------ nettoyage

    public void nettoyer(Autres a) {
        if (a == null) {
            return;
        }
        for (String tranche : a.tranches()) {
            effacer("DELETE FROM CfgPrevision WHERE StkHierarchyId = ?", tranche);
            effacer("DELETE FROM AdmWebHistorics WHERE ParentObjectId IN (" + P_TRANCHE + ")", tranche);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM StkPresentationTypeContenance "
                    + "WHERE StkPresentationTypeId IN (" + P_TRANCHE + "))", tranche);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + P_TRANCHE + ")", tranche);
            effacer("DELETE FROM StkPresentationTypeContenance WHERE StkPresentationTypeId IN (" + P_TRANCHE + ")", tranche);
            effacer("DELETE FROM StkPresentationType WHERE StkHierarchyId = ?", tranche);
        }
        for (String entite : a.entites()) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + P_ENTITE + ")", entite);
            effacer("DELETE FROM StkItem WHERE CfgCompanyId = ?", entite);
        }
        List<String[]> suivis = a.suivis();
        for (int k = suivis.size() - 1; k >= 0; k--) {
            String[] ligne = suivis.get(k);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
        base.nettoyer(a.i());
    }

    /** Tranche créée par le test (hors {@code preparer}) : ses présentations et prévisions sont effacées aussi. */
    public void suivreTranche(Autres a, UUID trancheId) {
        a.tranches().add(trancheId.toString());
    }

    private static void suivre(Autres a, String table, UUID id) {
        a.suivis().add(new String[]{table, id.toString()});
    }

    private static Timestamp horodatage(LocalDateTime d) {
        return d == null ? null : Timestamp.valueOf(d);
    }

    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private <T> T dans(Autres a, Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(a.i().admin().getId(), a.i().admin().getCode(), null, false,
                true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        try {
            return new TransactionTemplate(transactionManager).execute(statut -> ecriture.get());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
