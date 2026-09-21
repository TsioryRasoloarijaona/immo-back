package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Recopie dans la base de lecture une ligne qui vient d'être écrite sur la source de vérité.
 *
 * <p>Sans elle, ce qu'un utilisateur crée n'apparaîtrait qu'au cycle de synchronisation suivant :
 * il validerait un formulaire et ne verrait pas sa fiche. Cette classe ne traite donc que
 * <b>l'immédiat</b> — le rattrapage de masse, y compris de ce qu'OMRAN écrit de son côté, reste
 * le travail de la synchronisation périodique.
 *
 * <p><b>Elle relit la ligne depuis la source</b> au lieu de recopier l'entité en mémoire : c'est
 * la seule façon d'emporter ce que la base a réellement enregistré — valeurs par défaut,
 * colonnes calculées, et les déclencheurs que porte {@code ComFolder}.
 *
 * <p>Les écritures passent par {@code jdbcLecture}, donc en JDBC direct et non par JPA :
 * elles ne repassent pas par {@link CollecteurLignesEcrites}, et aucune boucle ne se forme.
 *
 * <p>Même condition d'activation que {@link DataSourceConfig}, et ce n'est pas une précaution
 * de confort : ce service dépend de {@code jdbcLecture} et de la source {@code ecriture}, qui
 * n'existent qu'en mode deux bases. Sans la condition, le contexte échouerait au démarrage en
 * mono-base — donc sur toute la suite de tests.
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class RemonteeLecture {

    private static final Logger log = LoggerFactory.getLogger(RemonteeLecture.class);

    /**
     * Colonne d'identité par table, quand ce n'est pas {@code PKey}. {@code AppFileObject} est en
     * relation 1:1 avec sa ligne {@code AdmAttachedFile} et en partage la clé sous un autre nom.
     */
    private static final Map<String, String> CLES_PRIMAIRES = Map.of("AppFileObject", "AdmAttachedFileId");

    /**
     * Colonnes que la source possède mais que la copie n'a <b>volontairement</b> pas — à ne pas
     * confondre avec une dérive de schéma, qui doit continuer d'échouer bruyamment.
     *
     * <p>{@code AttachedFile} est le binaire FILESTREAM que lit le logiciel de bureau ; la copie
     * l'a physiquement supprimé ({@code 004_drop_attachedfile_filestream.sql} — FILESTREAM
     * n'existe pas sur SQL Server Linux) et son binaire vit sur S3, pointé par
     * {@code AppFileObject}. Exclure la colonne du {@code SELECT} — et non du seul {@code MERGE} —
     * évite aussi de faire transiter le fichier une seconde fois sur le réseau à chaque dépôt.
     */
    private static final Map<String, Set<String>> COLONNES_ABSENTES_DE_LA_COPIE =
            Map.of("AdmAttachedFile", Set.of("AttachedFile"));

    private final JdbcTemplate versLecture;
    private final JdbcTemplate depuisEcriture;

    /** Instruction {@code MERGE} par table — construite une fois, réutilisée ensuite. */
    private final Map<String, String> fusions = new ConcurrentHashMap<>();

    /** Instruction {@code SELECT} par table à colonnes exclues — même régime que {@link #fusions}. */
    private final Map<String, String> selections = new ConcurrentHashMap<>();

    public RemonteeLecture(@Qualifier("jdbcLecture") JdbcTemplate versLecture,
                           @Qualifier("ecriture") DataSource ecriture) {
        this.versLecture = versLecture;
        this.depuisEcriture = new JdbcTemplate(ecriture);
    }

    /**
     * Rapatrie les lignes indiquées, dans l'ordre reçu — qui est déjà celui des clés étrangères.
     *
     * <p>Chaque ligne est traitée indépendamment : l'échec de l'une n'empêche pas les suivantes.
     * Un échec est journalisé et <b>jamais propagé</b> — la ligne existe sur la source de vérité,
     * qui fait foi, et la synchronisation périodique la reprendra. Faire échouer la requête de
     * l'utilisateur pour un problème de copie serait signaler une perte qui n'a pas eu lieu.
     *
     * <p><b>Une ligne absente de la source est supprimée de la copie.</b> C'est ainsi que les
     * {@code DELETE} voyagent : la suppression est collectée comme une écriture, la relecture ne
     * trouve rien, et l'absence — la source faisant foi — se propage. Les suppressions passent
     * <b>après</b> les fusions et en <b>ordre inverse</b> : la copie porte les clés étrangères du
     * schéma complet, un parent supprimé avant ses enfants serait refusé.
     */
    public void remonter(Collection<LigneEcrite> lignes) {
        List<LigneEcrite> absentes = new ArrayList<>();
        for (LigneEcrite ligne : lignes) {
            try {
                if (!remonterUne(ligne)) {
                    absentes.add(ligne);
                }
            } catch (RuntimeException e) {
                log.error("Ligne non rapatriee dans la base de lecture : {} {}. "
                                + "Elle est bien ecrite sur la source ; la synchronisation periodique la reprendra.",
                        ligne.table(), ligne.pkey(), e);
            }
        }
        for (LigneEcrite ligne : absentes.reversed()) {
            try {
                supprimerUne(ligne);
            } catch (RuntimeException e) {
                log.error("Suppression non repercutee dans la base de lecture : {} {}. "
                                + "La synchronisation periodique la reprendra.",
                        ligne.table(), ligne.pkey(), e);
            }
        }
    }

    /** {@code true} si la ligne existe sur la source et a été fusionnée dans la copie. */
    private boolean remonterUne(LigneEcrite ligne) {
        String cle = CLES_PRIMAIRES.getOrDefault(ligne.table(), "PKey");
        List<Map<String, Object>> trouvees = depuisEcriture.queryForList(
                selections.computeIfAbsent(ligne.table(), t -> selection(t, cle)), ligne.pkey());

        if (trouvees.isEmpty()) {
            return false;
        }

        Map<String, Object> valeurs = trouvees.get(0);
        List<String> colonnes = new ArrayList<>(valeurs.keySet());

        Object[] parametres = parametres(cle, colonnes, valeurs, ligne.pkey());
        versLecture.update(fusions.computeIfAbsent(ligne.table(), t -> fusion(t, cle, colonnes)), parametres);
        return true;
    }

    /**
     * Répercute une suppression. Idempotente par nature ({@code DELETE} d'une ligne déjà
     * absente : zéro ligne, zéro erreur) — le cas d'une ligne jamais rapatriée est donc gratuit.
     */
    private void supprimerUne(LigneEcrite ligne) {
        String cle = CLES_PRIMAIRES.getOrDefault(ligne.table(), "PKey");
        int lignes = versLecture.update(
                "DELETE FROM dbo." + crochets(ligne.table()) + " WHERE " + crochets(cle) + " = ?",
                ligne.pkey());
        log.debug("Suppression repercutee dans la copie : {} {} ({} ligne).",
                ligne.table(), ligne.pkey(), lignes);
    }

    /**
     * {@code SELECT *} pour le cas courant ; une liste explicite de colonnes, lue sur la source,
     * pour les tables dont la copie ne possède pas tout (cf.
     * {@link #COLONNES_ABSENTES_DE_LA_COPIE}) — la colonne écartée ne quitte alors jamais la
     * source.
     */
    private String selection(String table, String cle) {
        Set<String> exclues = COLONNES_ABSENTES_DE_LA_COPIE.get(table);
        String colonnes = "*";
        if (exclues != null) {
            colonnes = depuisEcriture.queryForList(
                            "SELECT name FROM sys.columns WHERE object_id = OBJECT_ID(?) ORDER BY column_id",
                            String.class, "dbo." + table).stream()
                    .filter(c -> exclues.stream().noneMatch(c::equalsIgnoreCase))
                    .map(RemonteeLecture::crochets)
                    .collect(Collectors.joining(", "));
        }
        return "SELECT " + colonnes + " FROM dbo." + crochets(table)
                + " WHERE " + crochets(cle) + " = ?";
    }

    /**
     * {@code MERGE} plutôt que « tenter un {@code UPDATE}, insérer s'il n'a rien touché » : une
     * seule instruction, donc idempotente et rejouable sans condition de course.
     *
     * <p>{@code WITH (HOLDLOCK)} n'est pas optionnel — sans lui, deux fusions simultanées sur la
     * même clé peuvent toutes deux conclure « pas trouvé » et insérer.
     *
     * <p>Les colonnes sont celles que la <b>source</b> renvoie. Si la copie n'en possède pas
     * autant, l'instruction échoue et la trace le dit : une dérive de schéma entre les deux
     * bases doit se voir, pas se faire absorber en silence.
     */
    static String fusion(String table, String cle, List<String> colonnes) {
        String affectations = colonnes.stream()
                .filter(c -> !cle.equalsIgnoreCase(c))
                .map(c -> "c." + crochets(c) + " = ?")
                .collect(Collectors.joining(", "));

        String liste = colonnes.stream().map(RemonteeLecture::crochets).collect(Collectors.joining(", "));
        String substituts = colonnes.stream().map(c -> "?").collect(Collectors.joining(", "));

        return "MERGE INTO dbo." + crochets(table) + " WITH (HOLDLOCK) AS c"
                + " USING (SELECT ? AS Cle) AS s ON c." + crochets(cle) + " = s.Cle"
                + " WHEN MATCHED THEN UPDATE SET " + affectations
                + " WHEN NOT MATCHED THEN INSERT (" + liste + ") VALUES (" + substituts + ");";
    }

    /** Dans l'ordre du {@code MERGE} : la clé de jointure, puis le {@code SET}, puis l'{@code INSERT}. */
    static Object[] parametres(String cle, List<String> colonnes, Map<String, Object> valeurs, Object pkey) {
        List<Object> parametres = new ArrayList<>();
        parametres.add(pkey);
        colonnes.stream().filter(c -> !cle.equalsIgnoreCase(c)).forEach(c -> parametres.add(valeurs.get(c)));
        colonnes.forEach(c -> parametres.add(valeurs.get(c)));
        return parametres.toArray();
    }

    /**
     * Les noms de ce schéma viennent de tables héritées et comptent des mots réservés
     * ({@code Usage}). Les crocheter systématiquement évite d'avoir à savoir lesquels.
     */
    private static String crochets(String identifiant) {
        return "[" + identifiant.replace("]", "]]") + "]";
    }
}
