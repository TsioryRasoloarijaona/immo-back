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

    private final JdbcTemplate versLecture;
    private final JdbcTemplate depuisEcriture;

    /** Instruction {@code MERGE} par table — construite une fois, réutilisée ensuite. */
    private final Map<String, String> fusions = new ConcurrentHashMap<>();

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
     */
    public void remonter(Collection<LigneEcrite> lignes) {
        for (LigneEcrite ligne : lignes) {
            try {
                remonterUne(ligne);
            } catch (RuntimeException e) {
                log.error("Ligne non rapatriee dans la base de lecture : {} {}. "
                                + "Elle est bien ecrite sur la source ; la synchronisation periodique la reprendra.",
                        ligne.table(), ligne.pkey(), e);
            }
        }
    }

    private void remonterUne(LigneEcrite ligne) {
        List<Map<String, Object>> trouvees = depuisEcriture.queryForList(
                "SELECT * FROM dbo." + crochets(ligne.table()) + " WHERE PKey = ?", ligne.pkey());

        if (trouvees.isEmpty()) {
            // Supprimée entre la validation et le rapatriement. Rien à copier, rien d'anormal.
            log.debug("Ligne {} {} absente de la source au moment du rapatriement.",
                    ligne.table(), ligne.pkey());
            return;
        }

        Map<String, Object> valeurs = trouvees.get(0);
        List<String> colonnes = new ArrayList<>(valeurs.keySet());

        Object[] parametres = parametres(colonnes, valeurs, ligne.pkey());
        versLecture.update(fusions.computeIfAbsent(ligne.table(), t -> fusion(t, colonnes)), parametres);
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
    static String fusion(String table, List<String> colonnes) {
        String affectations = colonnes.stream()
                .filter(c -> !"PKey".equalsIgnoreCase(c))
                .map(c -> "c." + crochets(c) + " = ?")
                .collect(Collectors.joining(", "));

        String liste = colonnes.stream().map(RemonteeLecture::crochets).collect(Collectors.joining(", "));
        String substituts = colonnes.stream().map(c -> "?").collect(Collectors.joining(", "));

        return "MERGE INTO dbo." + crochets(table) + " WITH (HOLDLOCK) AS c"
                + " USING (SELECT ? AS PKey) AS s ON c.PKey = s.PKey"
                + " WHEN MATCHED THEN UPDATE SET " + affectations
                + " WHEN NOT MATCHED THEN INSERT (" + liste + ") VALUES (" + substituts + ");";
    }

    /** Dans l'ordre du {@code MERGE} : la clé de jointure, puis le {@code SET}, puis l'{@code INSERT}. */
    static Object[] parametres(List<String> colonnes, Map<String, Object> valeurs, Object pkey) {
        List<Object> parametres = new ArrayList<>();
        parametres.add(pkey);
        colonnes.stream().filter(c -> !"PKey".equalsIgnoreCase(c)).forEach(c -> parametres.add(valeurs.get(c)));
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
