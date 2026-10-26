package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Données des tests de l'import générique P9e (FEATURE-patrimoine-import-generique.md) : entités juridiques, programmes,
 * tranches. Validées pour de bon comme {@link FixtureImportPatrimoine} (l'import ouvre sa propre transaction) ; tout
 * objet dont le code a été tiré par {@link Generique#code(String)} est effacé à la fin, qu'il vienne de la fixture ou
 * de l'import (tranches, puis programmes, puis entités, avec audit, habilitations et compteurs).
 */
@Component
public class FixtureImportGenerique {

    private static final Logger log = LoggerFactory.getLogger(FixtureImportGenerique.class);

    public static final String ENTITES = "entites-juridiques";
    public static final String PROJETS = "projets";
    public static final String TRANCHES = "tranches";
    public static final String PROSPECTIONS = "prospections-terrain";

    public static final String TYPE_ENTITE = "Yes.Cfg.CfgCompany";
    public static final String TYPE_PROGRAMME = "Yes.Cfg.CfgProject";
    public static final String TYPE_TRANCHE = "Yes.Cfg.CfgTranche";
    public static final String ADDED = "Added";
    public static final String MODIFIED = "Modified";

    // En-têtes = DisplayName OMRAN (CfgCompany.cs, CfgProject.cs, CfgTranche.cs, Yes.Lnd/Prospection.cs).
    public static final String CODE = "Code";
    public static final String DESCRIPTION = "Description";
    public static final String FORME_JURIDIQUE = "Forme juridique";
    public static final String VILLE = "Ville";
    public static final String PAYS = "Pays";
    public static final String ACTIVE = "Active";
    public static final String ENTITE = "Entité";
    public static final String PROGRAMME = "Programme";
    public static final String ADRESSE_1 = "Adresse 1";

    private static final String PKEY = "PKey";

    private final FixtureImportPatrimoine base;
    private final FixturePatrimoine patrimoine;
    private final FixtureEntites entites;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbc;

    public FixtureImportGenerique(FixtureImportPatrimoine base, FixturePatrimoine patrimoine, FixtureEntites entites,
                                  PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.base = base;
        this.patrimoine = patrimoine;
        this.entites = entites;
        this.transactionManager = transactionManager;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Contexte P9a (admin, entité active, programme, tranche) + codes tirés par le test. */
    public record Generique(Import i, List<String> codes) {

        public String jeton() {
            return i.jeton();
        }

        public AdmUser admin() {
            return i.admin();
        }

        public CfgCompany entite() {
            return i.c().entite();
        }

        public CfgProject projet() {
            return i.c().projet();
        }

        /** Code propre au test ({@code suffixe-jeton}), effacé à la fin quelle que soit la table qui le porte. */
        public String code(String suffixe) {
            String code = suffixe + "-" + jeton();
            codes.add(code);
            return code;
        }
    }

    /** Contexte prêt ; la devise MAD (défaut de création d'une entité) existe. */
    public Generique preparer(String prefixe) {
        Import i = base.preparer(prefixe);
        Generique g = new Generique(i, new ArrayList<>());
        dans(g, entites::deviseMad);
        return g;
    }

    // ------------------------------------------------------------------ objets existants

    public CfgCompany entite(Generique g, String code, boolean active) {
        return dans(g, () -> patrimoine.entite(code, active));
    }

    public CfgProject projet(Generique g, String code, CfgCompany entite) {
        return dans(g, () -> patrimoine.projet(code, entite));
    }

    public CfgTranche tranche(Generique g, String code, CfgProject projet) {
        return dans(g, () -> patrimoine.tranche(code, projet));
    }

    /** Pays « Pays &lt;code&gt; », effacé à la fin. */
    public AdmCountry pays(Generique g, String code) {
        AdmCountry pays = dans(g, () -> patrimoine.pays(code));
        g.i().suivis().add(new String[]{"AdmCountry", pays.getId().toString()});
        return pays;
    }

    /** {@code colonne} : nom de colonne écrit par le test, jamais une saisie. */
    public void definir(String table, UUID id, String colonne, Object valeur) {
        jdbc.update("UPDATE " + table + " SET " + colonne + " = ? WHERE PKey = ?", valeur, id.toString());
    }

    // ------------------------------------------------------------------ lecture (hors transaction)

    public List<Map<String, Object>> entites(String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description, LegalForm, City,
                       CAST(IsActive AS int) AS IsActive, CONVERT(varchar(36), AdmCountryId) AS AdmCountryId
                FROM CfgCompany WHERE Code = ?""", code);
    }

    public List<Map<String, Object>> projets(String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description, City, Address,
                       CONVERT(varchar(36), CfgCompanyId) AS CfgCompanyId
                FROM CfgProject WHERE Code = ?""", code);
    }

    public List<Map<String, Object>> tranches(String code) {
        return jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) AS PKey, Code, Description, Address,
                       CONVERT(varchar(36), CfgProjectId) AS CfgProjectId,
                       CAST(IsActiveAdvancePercentDiscount AS int) AS IsActiveAdvancePercentDiscount
                FROM CfgTranche WHERE Code = ?""", code);
    }

    /** La seule ligne de la liste ; échec explicite sinon. */
    public static Map<String, Object> seule(List<Map<String, Object>> lignes, String quoi) {
        if (lignes.size() != 1) {
            throw new AssertionError(quoi + " : " + lignes.size() + " ligne(s) au lieu d'une : " + lignes);
        }
        return lignes.get(0);
    }

    public static String id(Map<String, Object> ligne) {
        return String.valueOf(ligne.get(PKEY));
    }

    /** Lignes d'audit de l'objet pour l'action et le type OMRAN donnés. */
    public int audit(String objetId, String action, String type) {
        return base.compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ? AND ObjectType = ?",
                objetId, action, type);
    }

    /** Lignes d'audit du type dont le libellé porte le jeton du test (objets créés puis annulés compris). */
    public int auditDuTest(Generique g, String action, String type) {
        return base.compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE Action = ? AND ObjectType = ? AND ObjectLabel LIKE ?",
                action, type, "%" + g.jeton() + "%");
    }

    // ------------------------------------------------------------------ nettoyage

    public void nettoyer(Generique g) {
        SecurityContextHolder.clearContext();
        if (g == null) {
            return;
        }
        effacerParCode(g, "CfgTranche", "CfgTrancheId");
        effacerParCode(g, "CfgProject", "CfgProjectId");
        for (String code : g.codes()) {
            effacer("DELETE FROM AdmSequence WHERE CfgCompanyId IN (SELECT PKey FROM CfgCompany WHERE Code = ?)", code);
        }
        effacerParCode(g, "CfgCompany", "CfgCompanyId");
        base.nettoyer(g.i());
    }

    private void effacerParCode(Generique g, String table, String colonneHabilitation) {
        String ids = "(SELECT PKey FROM " + table + " WHERE Code = ?)";
        for (String code : g.codes()) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN " + ids, code);
            effacer("DELETE FROM AdmWebHistorics WHERE ParentObjectId IN " + ids, code);
            effacer("DELETE FROM AdmUserHabilitation WHERE " + colonneHabilitation + " IN " + ids, code);
            effacer("DELETE FROM " + table + " WHERE Code = ?", code);
        }
    }

    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private <T> T dans(Generique g, Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(g.admin().getId(), g.admin().getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        try {
            return new TransactionTemplate(transactionManager).execute(statut -> ecriture.get());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
