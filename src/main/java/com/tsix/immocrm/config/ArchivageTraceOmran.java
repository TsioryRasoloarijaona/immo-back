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
 * Archive périodiquement la trace d'audit SQL Server des requêtes d'<b>OMRAN Estate</b>.
 *
 * <p>L'audit serveur ({@code Audit_Global} → {@code C:\backup\*.sqlaudit}) est la source de
 * vérité sur ce que le légataire lit et écrit réellement — c'est lui qui a cadré les champs
 * obligatoires de la prospection et les lots de fiches. Mais il tourne sur <b>20 fichiers de
 * 100 Mo</b> : au-delà, SQL Server supprime les plus anciens, et le bruit (les lectures de la
 * synchronisation, l'outillage) consomme la fenêtre. Ce cycle extrait la part OMRAN
 * ({@code application_name = 'OMRAN estate'}) vers la table {@code AppTraceOmran} de la copie
 * ({@code db/migration/010_AppTraceOmran.sql}) avant que la rotation ne la détruise.
 *
 * <h2>Pourquoi rejouer la borne au lieu de dédupliquer</h2>
 *
 * <p>{@code sys.fn_get_audit_file} n'offre <b>aucune clé naturelle</b>, et c'est mesuré : une
 * requête sur une vue produit un enregistrement par objet référencé (même offset, même
 * instant, même statement), et 18 848 lignes du fichier brut sont strictement identiques sur
 * toutes les colonnes (relevé du 22/09/2026). Dédupliquer par clé perdrait donc de vrais
 * enregistrements. L'exactement-une-fois vient de la discipline de borne :
 *
 * <ul>
 *   <li><b>Purge puis rejeu de la borne</b> — la passe supprime de l'archive les lignes du
 *       tick {@code MAX(EventTimeUtc)} puis relit l'audit en {@code >=} : le tick frontière
 *       est réarchivé entier à chaque passe. C'est ce qui rend la passe idempotente face à un
 *       échec à mi-lot ({@code batchUpdate} commite instruction par instruction — un échec
 *       laisse un préfixe, et le filigrane peut retomber au milieu d'un tick).</li>
 *   <li><b>Lecture ordonnée par horodatage</b> — un préfixe partiellement archivé est alors
 *       toujours un préfixe <i>temporel</i> : la passe suivante repart de son {@code MAX} sans
 *       laisser de trou derrière elle. Sans cet ordre, un échec laisserait des trous que
 *       {@code >=} ne comblerait jamais.</li>
 *   <li><b>Borne haute de sécurité</b> — {@code event_time <= SYSUTCDATETIME() − 5 min},
 *       évaluée sur l'horloge de la source (celle qui horodate l'audit) : on ne lit jamais un
 *       tick encore en cours d'écriture ({@code QUEUE_DELAY} de l'audit : 1 s), la marge
 *       couvre large.</li>
 *   <li><b>Échec</b> : absorbé et journalisé, jamais propagé — le cycle suivant converge. La
 *       rotation laisse des semaines de marge à la cadence par défaut.</li>
 * </ul>
 *
 * <p>La lecture se fait <b>sur la source</b> ({@code sys.fn_get_audit_file} répond pour
 * l'instance où OMRAN écrit ; la fonction exige {@code CONTROL SERVER}) ; l'archive s'écrit
 * <b>sur la copie</b> via {@code jdbcLecture}, comme l'état de la synchronisation — l'archive
 * appartient au consommateur, pas à la vérité. L'audit horodate en <b>UTC</b> (décalage d'une
 * heure constaté avec l'heure locale), d'où {@code EventTimeUtc}.
 *
 * <p>Deux conditions d'activation : le mode deux-bases (même raison que
 * {@link SynchronisationDescendante} — sans lui, ni scheduler ni accès nommé à la copie) et un
 * chemin d'audit renseigné ({@code AUDIT_TRACE_PATH}, vide par défaut) — la suite de tests et
 * un déploiement dont la source n'a pas d'audit configuré n'instancient rien.
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''"
        + " && '${app.trace-omran.chemin:}'.trim() != ''")
public class ArchivageTraceOmran {

    private static final Logger log = LoggerFactory.getLogger(ArchivageTraceOmran.class);

    /** Le nom sous lequel le légataire se présente au serveur — le filtre de tout le cycle. */
    static final String APPLICATION_OMRAN = "OMRAN estate";

    /** Taille des lots d'insertion : borne la mémoire, la première passe lit ~43 000 lignes. */
    static final int TAILLE_LOT = 500;

    /** Marge de la borne haute : un tick plus vieux que ça est intégralement sur disque. */
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
        // Sans fetch size, le pilote mssql-jdbc matérialise tout le résultat : la première
        // passe (dizaines de milliers de lignes de 4 000 caractères) doit rester en flux.
        this.surSource.setFetchSize(TAILLE_LOT);
        this.surCopie = surCopie;
        this.chemin = chemin.trim();
    }

    /**
     * Une ligne d'audit telle que rendue par {@code sys.fn_get_audit_file}, réduite aux
     * colonnes archivées. {@code actionId} arrive complété d'espaces ({@code 'SL  '}) : le
     * constructeur normalise, pour que {@code WHERE ActionId = 'SL'} fonctionne à la requête.
     */
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

    /**
     * Le cycle planifié : 60 minutes par défaut ({@code AUDIT_TRACE_MINUTES}). La rotation de
     * l'audit se compte en semaines, l'heure est une marge large ; {@code fixedDelay} pour
     * qu'une première passe longue ne se fasse pas doubler.
     */
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

    /**
     * Exécute une passe d'archivage et rend le nombre de lignes archivées (rejeu de la borne
     * compris). Relancer sans nouvelle activité OMRAN rend le seul tick frontière, réarchivé
     * à l'identique — jamais de doublon, jamais de perte.
     */
    public int executer() {
        LocalDateTime filigrane = surCopie.queryForObject(
                "SELECT MAX(EventTimeUtc) FROM dbo.AppTraceOmran", LocalDateTime.class);
        if (filigrane != null) {
            // Le tick frontière peut être incomplet (échec à mi-lot d'une passe précédente) :
            // on le purge et on le relit entier plutôt que de dédupliquer — l'audit n'offre
            // aucune clé pour ça (voir le javadoc de classe).
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

    /**
     * La requête sur les fichiers d'audit, <b>ordonnée par horodatage</b> — la condition pour
     * qu'un échec à mi-passe laisse un préfixe temporel, que {@code >=} prolonge sans trou.
     * Sans filigrane (première passe), tout est lu jusqu'à la borne haute. Le filtre
     * {@code application_name} s'applique côté serveur : seules les lignes OMRAN traversent.
     */
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

    /** Les paramètres de {@link #requeteLecture} — alignés sur sa forme à borne optionnelle. */
    static Object[] parametresLecture(String chemin, LocalDateTime filigrane) {
        return filigrane == null
                ? new Object[]{chemin, APPLICATION_OMRAN}
                : new Object[]{chemin, APPLICATION_OMRAN, filigrane};
    }

    /** Les douze paramètres de {@code INSERTION}, dans l'ordre de ses colonnes. */
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
