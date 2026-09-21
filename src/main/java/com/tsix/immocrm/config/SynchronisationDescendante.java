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
 * Synchronisation <b>descendante</b> périodique : rapatrie dans la copie de lecture ce que
 * d'autres que ce backend — le logiciel de bureau, en pratique — écrivent sur la source de
 * vérité. Le pendant de {@link RemonteeLecture}, qui ne couvre que l'immédiat de nos propres
 * écritures ; sans ce cycle, un dossier créé dans le logiciel de bureau n'apparaîtrait jamais
 * dans le CRM.
 *
 * <p>Portée par le <b>Change Tracking</b> de SQL Server, activé sur la source
 * ({@code prototype/ct-activer-yesRems.sql}, rétention 7 jours) : disponible sur Express,
 * aucune colonne ni déclencheur ajouté — le légataire ne voit rien. Les décisions reprennent
 * {@code prototype/sync-ct-descendant.sql}, la version T-SQL de ce même cycle (qui exige un
 * nommage à trois parties, donc un seul serveur — ici les deux connexions du backend suffisent).
 *
 * <h2>Deux régimes, choisis table par table</h2>
 *
 * <ul>
 *   <li><b>BASELINE</b> — aucun filigrane, ou filigrane plus vieux que
 *       {@code CHANGE_TRACKING_MIN_VALID_VERSION} (le job est resté arrêté au-delà de la
 *       rétention). Toutes les lignes de la source sont fusionnées, par lots.
 *       <b>Aucune suppression</b> : la copie porte des lignes que la source n'a jamais eues
 *       (héritées d'avant la bascule), les effacer détruirait des données. Seule une
 *       suppression explicitement rapportée par le Change Tracking est propagée.</li>
 *   <li><b>DELTA</b> — le cas courant : {@code CHANGETABLE(CHANGES …)} donne les clés touchées
 *       et l'opération. Insertions et mises à jour passent par le même {@code MERGE} que le
 *       rapatriement immédiat ; une clé dont la ligne a disparu de la source entre-temps est
 *       traitée comme une suppression — la relecture fait foi, pas le code opération.</li>
 * </ul>
 *
 * <h2>Ce qui rend le cycle sûr</h2>
 *
 * <ul>
 *   <li><b>Le filigrane est lu une fois, en début de cycle.</b> Ce qui change pendant
 *       l'exécution porte une version supérieure et sera repris au tour suivant — au pire
 *       traité deux fois, et tout est idempotent ({@code MERGE}, {@code DELETE}).</li>
 *   <li><b>Le filigrane d'une table n'avance que si sa passe a entièrement réussi.</b> Un
 *       échec (FK, indisponibilité) est journalisé et la table sera rejouée depuis le même
 *       point au cycle suivant.</li>
 *   <li><b>Fusions dans l'ordre des clés étrangères, suppressions après et en ordre
 *       inverse</b> — la copie porte le schéma complet et ses 1 219 FK. Un ordre inter-tables
 *       encore insatisfait (parent supprimé avant son enfant d'une autre table) échoue
 *       proprement et converge au cycle suivant.</li>
 *   <li><b>Nos propres écritures reviennent par ce canal</b> (elles touchent la source, donc
 *       le Change Tracking les voit) : les refusionner est un no-op idempotent, pas une
 *       boucle.</li>
 * </ul>
 *
 * <p>L'état vit côté <b>copie</b>, table {@code AppSyncEtat}
 * ({@code db/migration/009_AppSyncEtat.sql}) : un filigrane par table suivie — jamais sur la
 * source, l'état appartient au consommateur. Démarrage sur une copie fraîchement restaurée
 * depuis la source : pré-remplir {@code AppSyncEtat} à la version courante (voir le script)
 * évite une baseline intégrale qui ne changerait rien.
 *
 * <p>La liste des tables est lue sur la source ({@code sys.change_tracking_tables}) à chaque
 * cycle — activer le suivi d'une table de plus ne demande aucun redéploiement. {@code
 * AdmSequence} resterait exclue même si on l'y ajoutait ({@code JAMAIS_RAPATRIEES}).
 *
 * <p>Même condition d'activation que {@link DataSourceConfig} ; en mono-base, ni bean ni
 * planification. {@code fixedDelay} (pas {@code fixedRate}) : un cycle lent ne se fait pas
 * doubler par le suivant.
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class SynchronisationDescendante {

    private static final Logger log = LoggerFactory.getLogger(SynchronisationDescendante.class);

    /** Les deux régimes du cycle — voir le javadoc de classe. */
    enum Regime { BASELINE, DELTA }

    private final JdbcTemplate surSource;
    private final JdbcTemplate surCopie;
    private final RemonteeLecture remontee;

    public SynchronisationDescendante(@Qualifier("ecriture") DataSource source,
                                      @Qualifier("jdbcLecture") JdbcTemplate surCopie,
                                      RemonteeLecture remontee) {
        this.surSource = new JdbcTemplate(source);
        this.surCopie = surCopie;
        this.remontee = remontee;
    }

    /**
     * Le cycle. 5 minutes par défaut ({@code SOURCE_DB_SYNC_MINUTES}) : le Change Tracking rend
     * un cycle vide quasi gratuit, et c'est le retard maximal qu'un utilisateur du CRM voit sur
     * ce que le bureau vient de saisir.
     */
    @Scheduled(fixedDelayString = "${app.source-de-verite.sync-minutes}",
            initialDelayString = "1", timeUnit = TimeUnit.MINUTES)
    public void cycle() {
        long debut = System.nanoTime();
        Long versionCourante = surSource.queryForObject(
                "SELECT CHANGE_TRACKING_CURRENT_VERSION()", Long.class);
        if (versionCourante == null) {
            log.error("Change Tracking inactif sur la source : la synchronisation descendante "
                    + "ne peut rien faire. Voir prototype/ct-activer-yesRems.sql.");
            return;
        }

        List<LigneEcrite> suppressions = new ArrayList<>();
        List<String> tablesReussies = new ArrayList<>();
        int fusionnees = 0;

        for (String table : tablesSuivies()) {
            try {
                fusionnees += passeDeFusion(table, versionCourante, suppressions);
                tablesReussies.add(table);
            } catch (RuntimeException e) {
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
                // Le filigrane de sa table avance quand même : la clé reste dans CHANGETABLE
                // tant que la rétention court, mais pas au-delà. Se voir dans les journaux
                // suffit — l'ordre inter-tables converge normalement au cycle suivant.
                log.error("Suppression non repercutee dans la copie : {} {}.",
                        ligne.table(), ligne.pkey(), e);
                tablesReussies.remove(ligne.table());
            }
        }

        for (String table : tablesReussies) {
            enregistrerFiligrane(table, versionCourante);
        }
        if (fusionnees > 0 || supprimees > 0) {
            log.info("Synchronisation descendante : {} fusion(s), {} suppression(s), "
                            + "version {}, en {} ms.", fusionnees, supprimees, versionCourante,
                    (System.nanoTime() - debut) / 1_000_000);
        }
    }

    /** Fusionne ce que la table doit à la copie ; collecte les suppressions pour la fin de cycle. */
    private int passeDeFusion(String table, long versionCourante, List<LigneEcrite> suppressions) {
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
            // L'opération I/U/D rapportée n'est pas consultée : la relecture décide. Une ligne
            // présente sur la source se fusionne, une ligne absente se supprime — y compris une
            // insertion déjà resupprimée depuis.
            if (remontee.fusionner(ligne)) {
                fusionnees++;
            } else {
                suppressions.add(ligne);
            }
        }
        return fusionnees;
    }

    /** La règle de choix du régime, extraite pour être éprouvée sans base. */
    static Regime regime(Long filigrane, Long minValide) {
        if (filigrane == null || minValide == null || filigrane < minValide) {
            return Regime.BASELINE;
        }
        return Regime.DELTA;
    }

    /**
     * Les tables suivies par le Change Tracking de la source, dans l'ordre des clés étrangères
     * — les inconnues de la liste passent en dernier, comme au rapatriement immédiat.
     */
    private List<String> tablesSuivies() {
        List<String> tables = surSource.queryForList(
                "SELECT t.name FROM sys.change_tracking_tables ctt "
                        + "JOIN sys.tables t ON t.object_id = ctt.object_id", String.class);
        return tables.stream()
                .filter(t -> !CollecteurLignesEcrites.JAMAIS_RAPATRIEES.contains(t))
                .sorted(Comparator.comparingInt(t -> {
                    int rang = CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.indexOf(t);
                    return rang < 0 ? CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size() : rang;
                }))
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
