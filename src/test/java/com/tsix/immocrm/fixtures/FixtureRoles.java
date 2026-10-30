package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Rôles et droits (FEATURE-admin-roles.md), en SQL natif validé : les entités du lot n'existent pas encore et un refus
 * se juge sur un vrai rollback. Chaque test appelle {@link #preparer()} puis {@link #nettoyer()} en {@code @AfterEach}.
 */
@Component
public class FixtureRoles {

    public static final String RACINE = "/api/administration/roles";
    public static final String FICHE = RACINE + "/{id}";
    public static final String DROITS = FICHE + "/droits";
    public static final String EXPORT = RACINE + "/export";
    public static final String ARBRE = RACINE + "/arbre-droits";

    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String RESTRICTIONS = "restrictions";
    public static final String CODES = "codes";
    public static final String TEXTE = "texte";
    public static final String ADMINISTRATEUR = "Administrateur";
    public static final String TYPE_AUDIT = "Yes.Adm.AdmRole";
    public static final String KIND_ACTION = "Action";

    private static final String P_ROLE = "AdmRoleId";

    // Absentes de yesRemsTest ; la CI les reçoit de ci/schema/156_roles_tables.sql. Définitions de yesRemsProd.
    private static final String TABLE_ARBRE = """
            IF OBJECT_ID('dbo.AdmRightEntity', 'U') IS NULL
            CREATE TABLE dbo.AdmRightEntity (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
                Sorting int NULL, Depth int NULL, Kind nvarchar(255) NULL)""";
    private static final String TABLE_WORKFLOW_ACHAT = """
            IF OBJECT_ID('dbo.BuyDocumentWorkFlowAdmRole', 'U') IS NULL
            CREATE TABLE dbo.BuyDocumentWorkFlowAdmRole (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, WorkFlowId uniqueidentifier NULL, AdmRoleId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""";

    /** Champ du fil et colonne de {@code AdmRole} des 10 restrictions. */
    public enum Restriction {
        AJOUT_PJ("ajoutPiecesJointes", "WithAddAttachementRestriction"),
        MODIFICATION_PJ("modificationPiecesJointes", "WithEditAttachementRestriction"),
        SUPPRESSION_PJ("suppressionPiecesJointes", "WithDeleteAttachementRestriction"),
        AJOUT_ACTIONS("ajoutActions", "WithAddActionMessageRestriction"),
        MODIFICATION_ACTIONS("modificationActions", "WithEditActionMessageRestriction"),
        SUPPRESSION_ACTIONS("suppressionActions", "WithDeleteActionMessageRestriction"),
        RAPPORTS("rapports", "WithReportRestriction"),
        AUDITS("audits", "WithAuditRestriction"),
        CHAMPS_SPECIFIQUES("champsSpecifiques", "WithSpecificFieldsRestriction"),
        SEQUENCES("sequences", "WithSequenceFreeRestriction");

        private final String champ;
        private final String colonne;

        Restriction(String champ, String colonne) {
            this.champ = champ;
            this.colonne = colonne;
        }

        public String champ() {
            return champ;
        }

        public String colonne() {
            return colonne;
        }
    }

    /** Les liens de la règle 12 : le mot que le 409 doit contenir, la table et la colonne qui portent le lien. */
    public enum Lien {
        UTILISATEUR("utilisateur", "AdmUserRole", P_ROLE),
        UTILISATEUR_WEB("utilisateur", "AdmUser", "WebRoleId"),
        INTERVENANT("intervenant", "StrIntervener", P_ROLE),
        RAPPORT("rapport", "AdmReportDataRole", P_ROLE),
        WORKFLOW_ACHAT("workflow", "BuyDocumentWorkFlowAdmRole", P_ROLE),
        WORKFLOW_ETUDE("workflow", "StkFeasibilityStudyWorkFlowAdmRole", P_ROLE);

        private final String cause;
        private final String table;
        private final String colonne;

        Lien(String cause, String table, String colonne) {
            this.cause = cause;
            this.table = table;
            this.colonne = colonne;
        }

        public String cause() {
            return cause;
        }
    }

    /** Colonnes du rôle et lignes {@code AdmRight}, PKey comprises : deux états égaux = rien n'a bougé. */
    public record Etat(List<Map<String, Object>> role, List<Map<String, Object>> droits) {
    }

    private final JdbcTemplate jdbc;
    private final FixtureRapports rapports;

    private final Deque<Runnable> liens = new ArrayDeque<>();
    private final List<UUID> roles = new ArrayList<>();
    private final List<UUID> comptes = new ArrayList<>();
    private final List<UUID> droitsPoses = new ArrayList<>();
    private String jeton;

    public FixtureRoles(JdbcTemplate jdbc, FixtureRapports rapports) {
        this.jdbc = jdbc;
        this.rapports = rapports;
    }

    /** Tables absentes créées pour de bon, hors transaction ; rend le jeton que porte tout code du test. */
    public String preparer() {
        jdbc.execute(TABLE_ARBRE);
        jdbc.execute(TABLE_WORKFLOW_ACHAT);
        rapports.tables();
        jeton = "ROL" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return jeton;
    }

    public void nettoyer() {
        liens.forEach(Runnable::run);
        liens.clear();
        droitsPoses.forEach(id -> jdbc.update("DELETE FROM AdmRight WHERE PKey = ?", id.toString()));
        droitsPoses.clear();
        jdbc.queryForList("SELECT PKey FROM AdmRole WHERE Code LIKE ?", UUID.class, "%" + jeton + "%").forEach(this::suivre);
        roles.forEach(id -> {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString());
            jdbc.update("DELETE FROM AdmRight WHERE AdmRoleId = ?", id.toString());
            jdbc.update("DELETE FROM AdmRole WHERE PKey = ?", id.toString());
        });
        roles.clear();
        jdbc.update("DELETE FROM AdmRightEntity WHERE Code LIKE ?", "%" + jeton + "%");
        comptes.forEach(id -> {
            jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", id.toString());
            jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", id.toString());
        });
        comptes.clear();
    }

    private void suivre(UUID role) {
        if (!roles.contains(role)) {
            roles.add(role);
        }
    }

    // ------------------------------------------------------------------ comptes

    /** Compte réel, validé : auteur des écritures et porteur des liens. */
    public AdmUser compte() {
        UUID id = UUID.randomUUID();
        String code = "role." + jeton + "." + id;
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)",
                id.toString(), code, "Auteur " + jeton);
        comptes.add(id);
        AdmUser compte = new AdmUser();
        compte.setId(id);
        compte.setCode(code);
        compte.setDescription("Auteur " + jeton);
        compte.setDisabled(false);
        return compte;
    }

    // ------------------------------------------------------------------ rôles

    /** Rôle sans description ni restriction : les 10 colonnes restent {@code NULL}. */
    public UUID role(String code) {
        return role(code, null);
    }

    public UUID role(String code, String description) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmRole (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, description);
        roles.add(id);
        return id;
    }

    /** Le rôle Administrateur de la base, créé s'il manque (jamais en double : le code ne doit désigner qu'une ligne). */
    public UUID administrateur() {
        List<UUID> existants = jdbc.queryForList("SELECT TOP 1 PKey FROM AdmRole WHERE Code = ? ORDER BY PKey ASC", UUID.class,
                ADMINISTRATEUR);
        return existants.isEmpty() ? role(ADMINISTRATEUR, ADMINISTRATEUR) : existants.getFirst();
    }

    public void restriction(UUID role, Restriction restriction, Boolean valeur) {
        jdbc.update("UPDATE AdmRole SET " + restriction.colonne() + " = ? WHERE PKey = ?", valeur, role.toString());
    }

    /** Ligne {@code AdmRole} ; clés insensibles à la casse. */
    public Map<String, Object> ligne(UUID role) {
        return jdbc.queryForMap("SELECT * FROM AdmRole WHERE PKey = ?", role.toString());
    }

    /** Id du rôle créé par l'API ; suivi pour le nettoyage. */
    public UUID idDe(String code) {
        UUID id = jdbc.queryForObject("SELECT PKey FROM AdmRole WHERE Code = ?", UUID.class, code);
        suivre(id);
        return id;
    }

    public int nombreRoles(String code) {
        return compter("SELECT COUNT(*) FROM AdmRole WHERE LOWER(Code) = LOWER(?)", code);
    }

    public int rolesPresents(UUID role) {
        return compter("SELECT COUNT(*) FROM AdmRole WHERE PKey = ?", role.toString());
    }

    /** Rôles du test dont le code garde une espace de bord ({@code LEN} ignore celles de fin). */
    public int codesAvecEspacesDeBord() {
        return compter("""
                SELECT COUNT(*) FROM AdmRole
                WHERE Code LIKE ? AND (LEFT(Code, 1) = ' ' OR DATALENGTH(Code) <> 2 * LEN(Code))""", "%" + jeton + "%");
    }

    public Etat etat(UUID role) {
        return new Etat(jdbc.queryForList("SELECT * FROM AdmRole WHERE PKey = ?", role.toString()),
                jdbc.queryForList("SELECT PKey, Code, Description, AdmRoleId FROM AdmRight WHERE AdmRoleId = ? ORDER BY PKey ASC",
                        role.toString()));
    }

    // ------------------------------------------------------------------ arbre et droits

    public void noeud(String code, String description, int tri, int profondeur, String kind) {
        jdbc.update("INSERT INTO AdmRightEntity (PKey, Code, Description, Sorting, Depth, Kind) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), code, description, tri, profondeur, kind);
    }

    /** Nœud Action à la fin de l'arbre. */
    public void noeud(String code) {
        noeud(code, "Libelle " + code, 1, 2, KIND_ACTION);
    }

    /** Ligne {@code AdmRight} ; hors arbre si {@code code} n'a pas de {@link #noeud}. */
    public UUID droit(UUID role, String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                id.toString(), code, "Ancien " + code, role.toString());
        droitsPoses.add(id);
        return id;
    }

    public List<String> codesDroits(UUID role) {
        return jdbc.queryForList("SELECT Code FROM AdmRight WHERE AdmRoleId = ? ORDER BY Code ASC", String.class, role.toString());
    }

    /** PKey des lignes de ce rôle pour ce code. */
    public List<UUID> lignesDroit(UUID role, String code) {
        return jdbc.queryForList("SELECT PKey FROM AdmRight WHERE AdmRoleId = ? AND Code = ?", UUID.class, role.toString(), code);
    }

    public String descriptionDroit(UUID role, String code) {
        return jdbc.queryForObject("SELECT Description FROM AdmRight WHERE AdmRoleId = ? AND Code = ?", String.class,
                role.toString(), code);
    }

    /** Nombre de lignes et somme de contrôle de {@code AdmRightEntity}. */
    public Map<String, Object> empreinteArbre() {
        return jdbc.queryForMap("SELECT COUNT(*) AS Lignes, CHECKSUM_AGG(BINARY_CHECKSUM(*)) AS Somme FROM AdmRightEntity");
    }

    // ------------------------------------------------------------------ liens de la règle 12

    public void lier(Lien lien, UUID role) {
        UUID id = UUID.randomUUID();
        String r = role.toString();
        switch (lien) {
            case UTILISATEUR -> inserer("INSERT INTO AdmUserRole (PKey, AdmRoleId, AdmUserId) VALUES (?, ?, ?)",
                    lien, id, r, compte().getId().toString());
            case UTILISATEUR_WEB -> {
                UUID compte = compte().getId();
                jdbc.update("UPDATE AdmUser SET WebRoleId = ? WHERE PKey = ?", r, compte.toString());
                liens.push(() -> jdbc.update("UPDATE AdmUser SET WebRoleId = NULL WHERE PKey = ?", compte.toString()));
            }
            case INTERVENANT -> inserer("INSERT INTO StrIntervener (PKey, AdmRoleId, Code) VALUES (?, ?, ?)",
                    lien, id, r, "INT-" + jeton);
            case RAPPORT -> {
                UUID rapport = rapports.rapportVente("RAP-" + jeton + "-" + id, "Rapport " + jeton, null, null);
                liens.push(() -> rapports.supprimerRapport(rapport));
                jdbc.update("INSERT INTO AdmReportDataRole (PKey, AdmRoleId, AdmReportDataId) VALUES (?, ?, ?)",
                        id.toString(), r, rapport.toString());
            }
            case WORKFLOW_ACHAT -> workflow(lien, "BuyDocumentWorkFlow", id, r);
            case WORKFLOW_ETUDE -> workflow(lien, "StkFeasibilityStudyWorkFlow", id, r);
        }
    }

    private void workflow(Lien lien, String parent, UUID id, String role) {
        UUID transition = UUID.randomUUID();
        jdbc.update("INSERT INTO " + parent + " (PKey) VALUES (?)", transition.toString());
        liens.push(() -> jdbc.update("DELETE FROM " + parent + " WHERE PKey = ?", transition.toString()));
        inserer("INSERT INTO " + lien.table + " (PKey, AdmRoleId, WorkFlowId) VALUES (?, ?, ?)",
                lien, id, role, transition.toString());
    }

    private void inserer(String sql, Lien lien, UUID id, String role, String troisieme) {
        jdbc.update(sql, id.toString(), role, troisieme);
        liens.push(() -> jdbc.update("DELETE FROM " + lien.table + " WHERE PKey = ?", id.toString()));
    }

    public int lignesLien(Lien lien, UUID role) {
        return compter("SELECT COUNT(*) FROM " + lien.table + " WHERE " + lien.colonne + " = ?", role.toString());
    }

    // ------------------------------------------------------------------ audit

    /** Texte ({@code Description} + {@code Details}) des traces du rôle pour cette action. */
    public List<String> traces(UUID role, String action) {
        return jdbc.queryForList("""
                SELECT CONCAT(Description, ' ', CONVERT(nvarchar(max), Details)) FROM AdmWebHistorics
                WHERE ObjectId = ? AND Action = ? AND ObjectType = ?""", String.class, role.toString(), action, TYPE_AUDIT);
    }

    /** Toute trace, quel que soit le type, portant sur cet objet. */
    public int tracesObjet(UUID objet) {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?", objet.toString());
    }

    /** Traces de rôle dont le libellé porte le jeton du test. */
    public int tracesDuJeton() {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectType = ? AND ObjectLabel LIKE ?", TYPE_AUDIT, "%" + jeton + "%");
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
