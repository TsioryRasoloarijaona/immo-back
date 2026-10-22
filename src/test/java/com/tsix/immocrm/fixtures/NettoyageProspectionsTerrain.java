package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;

/**
 * Tests P7b non transactionnels (audit, rollback) : écritures de mise en place validées au nom d'un Administrateur, puis
 * tout ce qui a été validé est effacé — prospections et terrains issus, lignes suivies, configuration semée absente avant.
 */
public final class NettoyageProspectionsTerrain {

    private static final Logger log = LoggerFactory.getLogger(NettoyageProspectionsTerrain.class);
    private static final String PAR_ID = " WHERE PKey = ?";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Deque<String[]> suivies = new ArrayDeque<>();
    private final List<UUID> prospections = new ArrayList<>();
    private final List<String[]> semis = new ArrayList<>();

    public NettoyageProspectionsTerrain(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** À appeler avant toute mise en place : note les lignes de configuration absentes, à effacer à la fin. */
    public void noterConfigurationAbsente() {
        for (UUID t : List.of(T_EN_COURS_VALIDE, T_EN_COURS_NON_ABOUTIE, T_NON_ABOUTIE_EN_COURS, T_EN_COURS_REJETE,
                T_VALIDE_EN_COURS)) {
            noterSiAbsent("StrWorkFlow", t);
        }
        for (UUID s : List.of(STATUT_EN_COURS, STATUT_VALIDE, STATUT_NON_ABOUTIE, STATUT_REJETE,
                FixtureTerrains.STATUT_ACQUIS)) {
            noterSiAbsent("StrStatus", s);
        }
        for (UUID t : List.of(TYPE_NOUVEAU, TYPE_SUCCES, TYPE_ECHEC, TYPE_ANNULATION)) {
            noterSiAbsent("StrStatusType", t);
        }
        for (UUID e : List.of(ENTITE_LND_LAND, FixtureTerrains.ENTITE_LAND_PROJECT, FixturePatrimoine.ENTITE_STK_ITEM)) {
            noterSiAbsent("StrEntity", e);
        }
        noterSiAbsent("StkVocation", FixturePatrimoine.VOCATION_VENTE);
        noterSiAbsent("StkVocation", FixturePatrimoine.VOCATION_PROJET);
        for (UUID k : List.of(NATURE_PROS_TER, FixtureTerrains.NATURE_TER, FixturePatrimoine.NATURE_BAT)) {
            noterSiAbsent("StkKind", k);
        }
    }

    public <T> T commeAdmin(AdmUser admin, Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        try {
            return transactions.execute(statut -> ecriture.get());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public <T> T valider(Supplier<T> ecriture) {
        return transactions.execute(statut -> ecriture.get());
    }

    /** Ligne validée par la mise en place, effacée à la fin dans l'ordre inverse (enfant avant parent). */
    public void suivre(String table, UUID id) {
        suivies.push(new String[]{table, id.toString()});
    }

    /** Références d'un contexte (FixtureProspectionsTerrain.contexte), à effacer à la fin. */
    public void suivre(FixtureProspectionsTerrain.Contexte c) {
        suivre("AdmCountry", c.pays().getId());
        suivre("AdmCity", c.ville());
        suivre("StkItemCategory", c.categorie());
        suivre("StkItemType", c.type());
        suivre("LndLevel", c.niveau());
        suivre("LndVocation", c.vocation());
        suivre("StkOrientation", c.orientation());
        suivre("CfgTier", c.courtier());
        suivre("CfgTier", c.notaire());
    }

    /** Prospection (créée par le test ou par l'API) : effacée avec ses zones, son historique et ses terrains issus. */
    public void suivreProspection(UUID id) {
        prospections.add(id);
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public void nettoyer() {
        SecurityContextHolder.clearContext();
        prospections.forEach(this::effacerProspection);
        while (!suivies.isEmpty()) {
            String[] ligne = suivies.pop();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", ligne[1]);
            effacer("DELETE FROM " + ligne[0] + PAR_ID, ligne[1]);
        }
        semis.forEach(s -> effacer("DELETE FROM " + s[0] + PAR_ID, s[1]));
    }

    private void effacerProspection(UUID id) {
        String p = id.toString();
        String terrains = "(SELECT PKey FROM StkItem WHERE LndLandId = ?)";
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN " + terrains, p);
        effacer("DELETE FROM StrHistory WHERE ObjectId IN " + terrains, p);
        effacer("DELETE FROM StkItem WHERE LndLandId = ?", p);
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", p);
        effacer("DELETE FROM StrHistory WHERE ObjectId = ?", p);
        effacer("DELETE FROM LndLandZone WHERE ProspectionId = ?", p);
        effacer("DELETE FROM LndLand WHERE PKey = ?", p);
    }

    /** Nettoyage au mieux : une table absente (base sans schéma 138) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, String parametre) {
        try {
            jdbc.update(sql, parametre);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private void noterSiAbsent(String table, UUID id) {
        try {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + PAR_ID, Integer.class, id.toString());
            if (n != null && n == 0) {
                semis.add(new String[]{table, id.toString()});
            }
        } catch (DataAccessException e) {
            log.warn("Table de semis absente ({}) : {}", table, e.getMessage());
        }
    }
}
