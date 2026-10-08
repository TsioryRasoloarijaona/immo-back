package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Archive la part OMRAN de l'audit SQL Server (lu sur la source) dans {@code AppTraceOmran} (sur la copie) avant sa
 * rotation. L'audit n'a aucune clé naturelle : exactement-une-fois par purge-et-rejeu du tick frontière, lecture
 * ordonnée et borne haute à 5 min (FEATURE-archivage-trace-omran.md, « Solution »).
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''"
        + " && '${app.trace-omran.chemin:}'.trim() != ''")
public class ArchivageTraceOmran {

    private static final Logger log = LoggerFactory.getLogger(ArchivageTraceOmran.class);

    /** {@code application_name} du légataire. */
    static final String APPLICATION_OMRAN = "OMRAN estate";

    /** Borne la mémoire : la première passe lit tout l'historique. */
    static final int TAILLE_LOT = 500;

    /** Un tick plus vieux que ça est intégralement sur disque. */
    static final int MARGE_MINUTES = 5;

    private static final String INSERTION = "INSERT INTO dbo.AppTraceOmran"
            + " (EventTimeUtc, ActionId, Succeeded, PrincipalNom, BaseNom, SchemaNom,"
            + " ObjetNom, Statement, ClientIp, FichierNom, FichierOffset, SequenceNo)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate surSource;
    private final JdbcTemplate surCopie;
    private final String chemin;

    public ArchivageTraceOmran(@Qualifier("ecriture") DataSource source,
                               @Qualifier("jdbcLecture") JdbcTemplate surCopie,
                               @Value("${app.trace-omran.chemin}") String chemin) {
        this.surSource = new JdbcTemplate(source);
        // Sans fetch size, mssql-jdbc matérialise tout le résultat : la première passe doit rester en flux.
        this.surSource.setFetchSize(TAILLE_LOT);
        this.surCopie = surCopie;
        this.chemin = chemin.trim();
    }

    /** {@code actionId} arrive complété d'espaces ({@code 'SL  '}) : normalisé pour {@code WHERE ActionId = 'SL'}. */
    record LigneAudit(LocalDateTime eventTimeUtc, String actionId, boolean succeeded,
                      String principalNom, String baseNom, String schemaNom, String objetNom,
                      String statement, String clientIp,
                      String fichierNom, long fichierOffset, int sequenceNo) {

        LigneAudit {
            actionId = actionId == null ? null : actionId.trim();
        }

        static LigneAudit depuis(ResultSet rs) throws SQLException {
            return new LigneAudit(
                    rs.getObject("event_time", LocalDateTime.class),
                    rs.getString("action_id"),
                    rs.getBoolean("succeeded"),
                    rs.getString("server_principal_name"),
                    rs.getString("database_name"),
                    rs.getString("schema_name"),
                    rs.getString("object_name"),
                    rs.getString("statement"),
                    rs.getString("client_ip"),
                    rs.getString("file_name"),
                    rs.getLong("audit_file_offset"),
                    rs.getInt("sequence_number"));
        }
    }

    /** {@code fixedDelay} : une première passe longue ne se fait pas doubler. */
    @Scheduled(fixedDelayString = "${app.trace-omran.minutes}",
            initialDelayString = "3", timeUnit = TimeUnit.MINUTES)
    public void cycle() {
        try {
            executer();
        } catch (RuntimeException e) {
            log.error("Archivage de la trace OMRAN en echec — la passe suivante rejouera la "
                    + "borne et convergera.", e);
        }
    }

    /** Rejeu de la borne compris : sans activité OMRAN, rend le seul tick frontière, réarchivé à l'identique. */
    public int executer() {
        LocalDateTime filigrane = surCopie.queryForObject(
                "SELECT MAX(EventTimeUtc) FROM dbo.AppTraceOmran", LocalDateTime.class);
        if (filigrane != null) {
            // Tick frontière peut-être incomplet (batchUpdate commite par instruction) : purgé et relu entier,
            // pas dédupliqué — l'audit n'a aucune clé.
            surCopie.update("DELETE FROM dbo.AppTraceOmran WHERE EventTimeUtc = ?", filigrane);
        }

        List<LigneAudit> lot = new ArrayList<>(TAILLE_LOT);
        int[] archivees = {0};
        surSource.query(requeteLecture(filigrane), rs -> {
            lot.add(LigneAudit.depuis(rs));
            if (lot.size() >= TAILLE_LOT) {
                archivees[0] += inserer(lot);
                lot.clear();
            }
        }, parametresLecture(chemin, filigrane));
        archivees[0] += inserer(lot);

        if (archivees[0] > 0) {
            log.info("Trace OMRAN : {} ligne(s) archivee(s) depuis {}.",
                    archivees[0], filigrane == null ? "l'origine" : filigrane);
        } else {
            log.debug("Trace OMRAN : rien de neuf depuis {}.", filigrane);
        }
        return archivees[0];
    }

    /** Ordonnée par horodatage : un échec à mi-passe laisse un préfixe temporel, que {@code >=} prolonge sans trou. */
    static String requeteLecture(LocalDateTime filigrane) {
        return "SELECT event_time, action_id, succeeded, server_principal_name,"
                + " database_name, schema_name, object_name, statement, client_ip,"
                + " file_name, audit_file_offset, sequence_number"
                + " FROM sys.fn_get_audit_file(?, DEFAULT, DEFAULT)"
                + " WHERE application_name = ?"
                + " AND event_time <= DATEADD(MINUTE, -" + MARGE_MINUTES + ", SYSUTCDATETIME())"
                + (filigrane == null ? "" : " AND event_time >= ?")
                + " ORDER BY event_time";
    }

    static Object[] parametresLecture(String chemin, LocalDateTime filigrane) {
        return filigrane == null
                ? new Object[]{chemin, APPLICATION_OMRAN}
                : new Object[]{chemin, APPLICATION_OMRAN, filigrane};
    }

    /** Dans l'ordre des colonnes de {@code INSERTION}. */
    static Object[] parametresInsertion(LigneAudit l) {
        return new Object[]{
                l.eventTimeUtc(), l.actionId(), l.succeeded(), l.principalNom(), l.baseNom(),
                l.schemaNom(), l.objetNom(), l.statement(), l.clientIp(),
                l.fichierNom(), l.fichierOffset(), l.sequenceNo()};
    }

    private int inserer(List<LigneAudit> lot) {
        if (lot.isEmpty()) {
            return 0;
        }
        surCopie.batchUpdate(INSERTION,
                lot.stream().map(ArchivageTraceOmran::parametresInsertion).toList());
        return lot.size();
    }
}
