package com.tsix.immocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Recopie dans la copie, avant la réponse HTTP, une ligne écrite sur la source, pour que l'utilisateur voie ce qu'il
 * vient de créer. Relit la ligne sur la source (défauts, déclencheurs de {@code ComFolder}) par le pool {@code remontee} ;
 * écrit en JDBC direct, donc sans repasser par {@link CollecteurLignesEcrites} (pas de boucle).
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class RemonteeLecture {

    private static final Logger log = LoggerFactory.getLogger(RemonteeLecture.class);

    /** Clé quand ce n'est pas {@code PKey} : {@code AppFileObject} partage celle de son {@code AdmAttachedFile}. */
    static final Map<String, String> CLES_PRIMAIRES = Map.of("AppFileObject", "AdmAttachedFileId");

    /**
     * Absentes de la copie <b>volontairement</b> (une dérive de schéma doit, elle, échouer) : {@code AttachedFile} est
     * le FILESTREAM d'OMRAN, inexistant sur SQL Server Linux, binaire sur S3. Exclue du SELECT : il ne transite pas.
     */
    private static final Map<String, Set<String>> COLONNES_ABSENTES_DE_LA_COPIE =
            Map.of("AdmAttachedFile", Set.of("AttachedFile"));

    /**
     * Non nulle = ligne d'un module non répliqué, dont le MERGE échouerait sur la clé étrangère et bloquerait la table :
     * la ligne entière est écartée (FIX-synchro-lignes-hors-perimetre.md).
     */
    static final Map<String, List<String>> COLONNES_HORS_PERIMETRE =
            Map.of("StlSettlementLine", List.of("BuyDocumentId", "RntDocumentId", "StlDocumentId"));

    /**
     * NULL typé par la colonne source : un NULL non typé part en nvarchar, que SQL Server refuse dans une colonne
     * {@code image} ({@code CfgProjectZone.ImageData}).
     */
    private static final RowMapper<Map<String, Object>> LIGNE_TYPEE = new ColumnMapRowMapper() {
        @Override
        protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
            Object valeur = super.getColumnValue(rs, index);
            return valeur == null ? new SqlParameterValue(rs.getMetaData().getColumnType(index), null) : valeur;
        }
    };

    private final JdbcTemplate versLecture;
    private final JdbcTemplate depuisEcriture;

    private final Map<String, String> fusions = new ConcurrentHashMap<>();

    private final Map<String, String> selections = new ConcurrentHashMap<>();

    private final Map<String, String> selectionsTout = new ConcurrentHashMap<>();

    public RemonteeLecture(@Qualifier("jdbcLecture") JdbcTemplate versLecture,
                           @Qualifier("remontee") DataSource source) {
        this.versLecture = versLecture;
        this.depuisEcriture = new JdbcTemplate(source);
    }

    /**
     * Dans l'ordre reçu (celui des clés étrangères). Un échec est journalisé, jamais propagé : la source fait foi.
     * Une ligne absente de la source est supprimée de la copie (c'est ainsi que voyagent les DELETE), après les
     * fusions et en ordre inverse, sinon un parent partirait avant ses enfants. Sans connexion, tout s'arrête : chaque
     * ligne suivante attendrait le timeout du pool, dans le temps de réponse de la requête.
     */
    public void remonter(Collection<LigneEcrite> lignes) {
        List<LigneEcrite> absentes = new ArrayList<>();
        int traitees = 0;
        for (LigneEcrite ligne : lignes) {
            try {
                if (!fusionner(ligne)) {
                    absentes.add(ligne);
                }
                traitees++;
            } catch (CannotGetJdbcConnectionException e) {
                log.error("Rapatriement abandonne, connexion indisponible : {} ligne(s) sur {} non rapatriee(s), "
                                + "dont {} {}. Elles sont bien ecrites sur la source ; la synchronisation periodique "
                                + "les reprendra.",
                        lignes.size() - traitees, lignes.size(), ligne.table(), ligne.pkey(), e);
                return;
            } catch (RuntimeException e) {
                traitees++;
                log.error("Ligne non rapatriee dans la base de lecture : {} {}. "
                                + "Elle est bien ecrite sur la source ; la synchronisation periodique la reprendra.",
                        ligne.table(), ligne.pkey(), e);
            }
        }
        for (LigneEcrite ligne : absentes.reversed()) {
            try {
                supprimer(ligne);
            } catch (CannotGetJdbcConnectionException e) {
                log.error("Suppressions abandonnees, connexion a la copie indisponible : {} ligne(s). "
                        + "La synchronisation periodique les reprendra.", absentes.size(), e);
                return;
            } catch (RuntimeException e) {
                log.error("Suppression non repercutee dans la base de lecture : {} {}. "
                                + "La synchronisation periodique la reprendra.",
                        ligne.table(), ligne.pkey(), e);
            }
        }
    }

    /** Vrai si la ligne existe sur la source. Réutilisé par {@link SynchronisationDescendante} : un seul MERGE. */
    boolean fusionner(LigneEcrite ligne) {
        String cle = CLES_PRIMAIRES.getOrDefault(ligne.table(), "PKey");
        List<Map<String, Object>> trouvees = depuisEcriture.query(
                selections.computeIfAbsent(ligne.table(), t -> selection(t, cle)), LIGNE_TYPEE, ligne.pkey());

        if (trouvees.isEmpty()) {
            return false;
        }

        Map<String, Object> valeurs = trouvees.get(0);
        List<String> colonnes = new ArrayList<>(valeurs.keySet());

        Object[] parametres = parametres(cle, colonnes, valeurs, ligne.pkey());
        versLecture.update(fusions.computeIfAbsent(ligne.table(), t -> fusion(t, cle, colonnes)), parametres);
        return true;
    }

    /** Idempotente : une ligne déjà absente ne coûte rien. */
    void supprimer(LigneEcrite ligne) {
        String cle = CLES_PRIMAIRES.getOrDefault(ligne.table(), "PKey");
        int lignes = versLecture.update(
                "DELETE FROM dbo." + crochets(ligne.table()) + " WHERE " + crochets(cle) + " = ?",
                ligne.pkey());
        log.debug("Suppression repercutee dans la copie : {} {} ({} ligne).",
                ligne.table(), ligne.pkey(), lignes);
    }

    /**
     * Baseline de {@link SynchronisationDescendante}. Ne supprime jamais : la copie peut porter des lignes que la
     * source n'a pas, et seule une suppression rapportée par le Change Tracking fait foi.
     */
    int fusionnerTout(String table) {
        String cle = CLES_PRIMAIRES.getOrDefault(table, "PKey");
        List<Map<String, Object>> lignes = depuisEcriture.query(
                selectionsTout.computeIfAbsent(table, this::selectionTout), LIGNE_TYPEE);
        if (lignes.isEmpty()) {
            return 0;
        }

        List<String> colonnes = new ArrayList<>(lignes.get(0).keySet());
        String fusion = fusions.computeIfAbsent(table, t -> fusion(t, cle, colonnes));
        List<Object[]> parametres = lignes.stream()
                .map(valeurs -> parametres(cle, colonnes, valeurs, valeurs.get(cle)))
                .toList();
        for (int depuis = 0; depuis < parametres.size(); depuis += TAILLE_LOT_BASELINE) {
            versLecture.batchUpdate(fusion,
                    parametres.subList(depuis, Math.min(depuis + TAILLE_LOT_BASELINE, parametres.size())));
        }
        return parametres.size();
    }

    private static final int TAILLE_LOT_BASELINE = 500;

    /** {@code SELECT *}, sauf pour une table de {@link #COLONNES_ABSENTES_DE_LA_COPIE} : liste lue sur la source. */
    private String projection(String table) {
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
        return "SELECT " + colonnes + " FROM dbo." + crochets(table);
    }

    /** Une ligne hors périmètre rend zéro tuple : {@link #fusionner} la voit absente et ne la copie pas. */
    private String selection(String table, String cle) {
        String sql = projection(table) + " WHERE " + crochets(cle) + " = ?";
        String filtre = predicatPerimetre(table);
        return filtre == null ? sql : sql + " AND " + filtre;
    }

    private String selectionTout(String table) {
        String filtre = predicatPerimetre(table);
        return filtre == null ? projection(table) : projection(table) + " WHERE " + filtre;
    }

    /** Chaque colonne de {@link #COLONNES_HORS_PERIMETRE} doit être {@code NULL} ; {@code null} si la table n'en a pas. */
    static String predicatPerimetre(String table) {
        List<String> colonnes = COLONNES_HORS_PERIMETRE.get(table);
        if (colonnes == null) {
            return null;
        }
        return colonnes.stream()
                .map(c -> crochets(c) + " IS NULL")
                .collect(Collectors.joining(" AND "));
    }

    /**
     * Une seule instruction, donc rejouable. {@code HOLDLOCK} obligatoire : sinon deux fusions simultanées concluent
     * « absent » et insèrent toutes deux. Colonnes de la source : une dérive de schéma doit échouer, pas s'absorber.
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

    /** Le schéma légataire compte des mots réservés ({@code Usage}) : tout crocheter. */
    private static String crochets(String identifiant) {
        return "[" + identifiant.replace("]", "]]") + "]";
    }
}
