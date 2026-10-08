package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Rapatrie dans la copie ce qu'OMRAN écrit sur la source, par le Change Tracking (FEATURE-source-de-verite-115.md,
 * « Synchronisation descendante »). Filigrane par table dans {@code AppSyncEtat} côté copie, lu une fois par cycle,
 * avancé seulement si la passe a réussi ; tout est idempotent. Baseline = fusion sans aucune suppression.
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class SynchronisationDescendante {

    private static final Logger log = LoggerFactory.getLogger(SynchronisationDescendante.class);

    enum Regime { BASELINE, DELTA }

    private final JdbcTemplate surSource;
    private final JdbcTemplate surCopie;
    private final RemonteeLecture remontee;
    private final MoniteurSynchronisation moniteur;

    /** Partagé avec {@code POST /api/synchronisation} : le second arrivant est refusé, pas mis en attente. */
    private final java.util.concurrent.locks.ReentrantLock verrou =
            new java.util.concurrent.locks.ReentrantLock();

    public SynchronisationDescendante(@Qualifier("ecriture") DataSource source,
                                      @Qualifier("jdbcLecture") JdbcTemplate surCopie,
                                      RemonteeLecture remontee,
                                      MoniteurSynchronisation moniteur) {
        this.surSource = new JdbcTemplate(source);
        this.surCopie = surCopie;
        this.remontee = remontee;
        this.moniteur = moniteur;
    }

    /**
     * @param declenchee faux si un autre cycle tournait déjà : rien n'a été fait
     * @param version    nul si le Change Tracking est inactif
     */
    public record ResultatSync(boolean declenchee, Long version, int fusions, int suppressions,
                               int tablesTraitees, int tablesEnEchec, long dureeMs) {

        static ResultatSync ignoree() {
            return new ResultatSync(false, null, 0, 0, 0, 0, 0);
        }
    }

    /** {@code fixedDelay} et non {@code fixedRate} : un cycle lent ne se fait pas doubler. */
    @Scheduled(fixedDelayString = "${app.source-de-verite.sync-minutes}",
            initialDelayString = "1", timeUnit = TimeUnit.MINUTES)
    public void cycle() {
        executer();
    }

    /** Les échecs par table sont comptés, jamais propagés : la table rejoue au cycle suivant. */
    public ResultatSync executer() {
        if (!verrou.tryLock()) {
            log.debug("Cycle de synchronisation deja en cours : declenchement ignore.");
            return ResultatSync.ignoree();
        }
        try {
            return executerSousVerrou();
        } finally {
            verrou.unlock();
        }
    }

    private ResultatSync executerSousVerrou() {
        long debut = System.nanoTime();
        Long versionCourante = surSource.queryForObject(
                "SELECT CHANGE_TRACKING_CURRENT_VERSION()", Long.class);
        if (versionCourante == null) {
            log.error("Change Tracking inactif sur la source : la synchronisation descendante "
                    + "ne peut rien faire. Voir prototype/ct-activer-yesRems.sql.");
            return new ResultatSync(true, null, 0, 0, 0, 0,
                    (System.nanoTime() - debut) / 1_000_000);
        }

        List<LigneEcrite> suppressions = new ArrayList<>();
        List<String> tablesReussies = new ArrayList<>();
        int fusionnees = 0;
        int enEchec = 0;

        for (String table : tablesSuivies()) {
            try {
                fusionnees += passeDeFusion(table, suppressions);
                tablesReussies.add(table);
            } catch (RuntimeException e) {
                enEchec++;
                log.error("Synchronisation de {} en echec — filigrane inchange, "
                        + "la table sera rejouee au prochain cycle.", table, e);
            }
        }

        int supprimees = 0;
        for (LigneEcrite ligne : suppressions.reversed()) {
            try {
                remontee.supprimer(ligne);
                supprimees++;
            } catch (RuntimeException e) {
                // Table retirée des réussies : son filigrane n'avance pas, la suppression rejouera au cycle suivant.
                log.error("Suppression non repercutee dans la copie : {} {}.",
                        ligne.table(), ligne.pkey(), e);
                if (tablesReussies.remove(ligne.table())) {
                    enEchec++;
                }
            }
        }

        for (String table : tablesReussies) {
            enregistrerFiligrane(table, versionCourante);
        }
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;
        if (fusionnees > 0 || supprimees > 0 || enEchec > 0) {
            log.info("Synchronisation descendante : {} fusion(s), {} suppression(s), "
                            + "{} table(s) en echec, version {}, en {} ms.",
                    fusionnees, supprimees, enEchec, versionCourante, dureeMs);
        }
        ResultatSync resultat = new ResultatSync(true, versionCourante, fusionnees, supprimees,
                tablesReussies.size(), enEchec, dureeMs);
        // Le moniteur ne doit jamais faire échouer un cycle.
        try {
            moniteur.enregistrerCycle(resultat, retardsParTable(versionCourante));
        } catch (RuntimeException e) {
            log.warn("Metriques de synchronisation non mises a jour pour ce cycle.", e);
        }
        return resultat;
    }

    /** Une table sans filigrane n'apparaît qu'après sa première baseline. */
    private Map<String, Long> retardsParTable(long versionCourante) {
        Map<String, Long> retards = new java.util.LinkedHashMap<>();
        surCopie.query("SELECT TableNom, DerniereVersion FROM dbo.AppSyncEtat", rs -> {
            long filigrane = rs.getLong("DerniereVersion");
            retards.put(rs.getString("TableNom"), Math.max(0L, versionCourante - filigrane));
        });
        return retards;
    }

    /** Les suppressions sont collectées pour la fin de cycle (en ordre inverse des clés étrangères). */
    private int passeDeFusion(String table, List<LigneEcrite> suppressions) {
        Long filigrane = surCopie.query(
                "SELECT DerniereVersion FROM dbo.AppSyncEtat WHERE TableNom = ?",
                rs -> rs.next() ? rs.getObject(1, Long.class) : null, table);
        Long minValide = surSource.queryForObject(
                "SELECT CHANGE_TRACKING_MIN_VALID_VERSION(OBJECT_ID('dbo.' + QUOTENAME(?)))",
                Long.class, table);

        if (regime(filigrane, minValide) == Regime.BASELINE) {
            int lignes = remontee.fusionnerTout(table);
            log.info("Baseline de {} : {} ligne(s) fusionnee(s) (filigrane {}).",
                    table, lignes, filigrane == null ? "absent" : "perime : " + filigrane);
            return lignes;
        }

        String cle = RemonteeLecture.CLES_PRIMAIRES.getOrDefault(table, "PKey");
        int fusionnees = 0;
        for (Map<String, Object> changement : surSource.queryForList(
                "SELECT ct.[" + cle + "] AS cle FROM CHANGETABLE(CHANGES dbo."
                        + "[" + table + "], ?) AS ct", filigrane)) {
            // Le pilote mssql-jdbc livre un uniqueidentifier en String, pas en UUID.
            Object brut = changement.get("cle");
            LigneEcrite ligne = new LigneEcrite(table,
                    brut instanceof UUID u ? u : UUID.fromString(brut.toString()));
            // L'opération I/U/D n'est pas consultée : la relecture décide (une insertion resupprimée depuis se supprime).
            if (remontee.fusionner(ligne)) {
                fusionnees++;
            } else {
                suppressions.add(ligne);
            }
        }
        return fusionnees;
    }

    /** Baseline si aucun filigrane ou s'il a dépassé la rétention du Change Tracking. */
    static Regime regime(Long filigrane, Long minValide) {
        if (filigrane == null || minValide == null || filigrane < minValide) {
            return Regime.BASELINE;
        }
        return Regime.DELTA;
    }

    /** Relue à chaque cycle : suivre une table de plus ne demande pas de redéploiement. */
    private List<String> tablesSuivies() {
        List<String> tables = surSource.queryForList(
                "SELECT t.name FROM sys.change_tracking_tables ctt "
                        + "JOIN sys.tables t ON t.object_id = ctt.object_id", String.class);
        return tables.stream()
                .filter(t -> !CollecteurLignesEcrites.JAMAIS_RAPATRIEES.contains(t))
                .sorted(Comparator.comparingInt(CollecteurLignesEcrites::rang))
                .toList();
    }

    private void enregistrerFiligrane(String table, long version) {
        surCopie.update("MERGE INTO dbo.AppSyncEtat WITH (HOLDLOCK) AS e"
                + " USING (SELECT ? AS TableNom) AS s ON e.TableNom = s.TableNom"
                + " WHEN MATCHED THEN UPDATE SET DerniereVersion = ?, DerniereExecution = SYSDATETIME()"
                + " WHEN NOT MATCHED THEN INSERT (TableNom, DerniereVersion, DerniereExecution)"
                + " VALUES (?, ?, SYSDATETIME());", table, version, table, version);
    }

}
