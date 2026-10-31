package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureRoles.Restriction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Comptes, rôles, comptes de dépôt et habilitations (FEATURE-admin-utilisateurs.md), en SQL natif validé : un refus se juge
 * sur un vrai rollback et la concurrence exige des lignes visibles des autres connexions. Chaque test appelle
 * {@link #preparer()}, puis {@link #nettoyer()} en {@code @AfterEach}.
 */
@Component
public class FixtureUtilisateurs {

    public static final String RACINE = "/api/utilisateurs";
    public static final String FICHE = RACINE + "/{id}";
    public static final String EXPORT = RACINE + "/export";
    public static final String REFERENCES = RACINE + "/references";
    public static final String ROLES = FICHE + "/roles";
    public static final String COMPTES_DEPOT = FICHE + "/comptes-depot";
    public static final String HABILITATIONS = FICHE + "/habilitations";

    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String INACTIF = "inactif";
    public static final String ROLE_IDS = "roleIds";
    public static final String COMPTE_IDS = "compteIds";
    public static final String NOEUDS = "noeuds";
    public static final String SEARCH = "search";
    public static final String ADMINISTRATEUR = "Administrateur";
    public static final String TYPE_AUDIT = "Yes.Adm.AdmUser";
    public static final String TYPE_AUDIT_HABILITATION = "Yes.Adm.AdmUserHabilitation";
    private static final String HISTORIQUE_HABILITATIONS = """
            SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectType = ? AND CONVERT(nvarchar(max), ObjectLabel) = ?""";

    private static final UUID TYPE_EMPLOYE = UUID.fromString("EEB3FCFF-15BE-484D-B785-923D7587E649");
    private static final UUID TYPE_CLIENT = UUID.fromString("07830036-EF72-492A-AC39-0A407392DA62");
    private static final String CODE_EMPLOYE = "EMP";
    private static final String CODE_CLIENT = "CLI";

    // Absentes de yesRemsTest ; en CI, AdmWebCultures existe et StlDepositUser vient du script du lot. Définitions de yesRemsProd.
    private static final String TABLE_LANGUES = """
            IF OBJECT_ID('dbo.AdmWebLanguages', 'U') IS NULL
            CREATE TABLE dbo.AdmWebLanguages (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(max) NULL, Description nvarchar(max) NULL,
                UserCreate nvarchar(max) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(max) NULL, UserUpdateDateTime datetime NULL)""";
    private static final String TABLE_CULTURES = """
            IF OBJECT_ID('dbo.AdmWebCultures', 'U') IS NULL
            CREATE TABLE dbo.AdmWebCultures (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(max) NULL, LanguageId uniqueidentifier NOT NULL,
                UserCreate nvarchar(max) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(max) NULL, UserUpdateDateTime datetime NULL)""";
    private static final String TABLE_DEPOTS_UTILISATEUR = """
            IF OBJECT_ID('dbo.StlDepositUser', 'U') IS NULL
            CREATE TABLE dbo.StlDepositUser (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, AdmUserId uniqueidentifier NULL, StlDepositId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""";

    /** Tout ce qu'une écriture sur le compte peut toucher, PKey comprises : deux états égaux = rien n'a bougé. */
    public record Etat(Map<String, Object> compte, List<Map<String, Object>> roles, List<Map<String, Object>> comptesDepot,
                       List<Map<String, Object>> habilitations, Integer tokenVersion, int traces) {
    }

    /** Valeurs factices des 5 colonnes secrètes d'un compte. */
    public record Secrets(String pass, String webPass, String emailPassword, String openBeePass, String passwordHash) {

        public List<String> valeurs() {
            return List.of(pass, webPass, emailPassword, openBeePass, passwordHash);
        }
    }

    private final JdbcTemplate jdbc;

    private final Deque<Runnable> retraits = new ArrayDeque<>();
    private final List<UUID> comptes = new ArrayList<>();
    private final Map<UUID, Object> administrateursSuspendus = new LinkedHashMap<>();
    private String jeton;

    public FixtureUtilisateurs(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tables absentes créées pour de bon, hors transaction ; rend le jeton (majuscules) que porte tout code du test. */
    public String preparer() {
        jdbc.execute(TABLE_LANGUES);
        jdbc.execute(TABLE_CULTURES);
        jdbc.execute(TABLE_DEPOTS_UTILISATEUR);
        jeton = "UTI" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return jeton;
    }

    public void nettoyer() {
        administrateursSuspendus.forEach((id, valeur) ->
                jdbc.update("UPDATE AdmUser SET Disabled = ? WHERE PKey = ?", valeur, id.toString()));
        administrateursSuspendus.clear();
        jdbc.queryForList("SELECT PKey FROM AdmUser WHERE UPPER(Code) LIKE ?", UUID.class, "%" + jeton + "%")
                .forEach(this::suivreCompte);
        comptes.forEach(this::supprimerCompte);
        comptes.clear();
        retraits.forEach(Runnable::run);
        retraits.clear();
    }

    private void suivreCompte(UUID id) {
        if (!comptes.contains(id)) {
            comptes.add(id);
        }
    }

    private void supprimerCompte(UUID id) {
        String c = id.toString();
        jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", c);
        jdbc.update(HISTORIQUE_HABILITATIONS.replace("SELECT COUNT(*)", "DELETE"), TYPE_AUDIT_HABILITATION, c);
        jdbc.update("DELETE FROM StlDepositUser WHERE AdmUserId = ?", c);
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", c);
        jdbc.update("DELETE FROM AdmUserRole WHERE AdmUserId = ?", c);
        jdbc.update("DELETE FROM AppUserCredential WHERE AdmUserId = ?", c);
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", c);
    }

    private void retirerApres(String delete, UUID id) {
        retraits.push(() -> jdbc.update(delete, id.toString()));
    }

    // ------------------------------------------------------------------ comptes

    public AdmUser compte() {
        return compte(false);
    }

    /** Compte réel, validé ; {@code disabled} {@code null} = colonne NULL, un compte actif pour le décompte R8. */
    public AdmUser compte(Boolean disabled) {
        UUID id = UUID.randomUUID();
        return compte(id, "user." + jeton + "." + id, disabled);
    }

    public AdmUser compte(String code) {
        return compte(UUID.randomUUID(), code, false);
    }

    private AdmUser compte(UUID id, String code, Boolean disabled) {
        String description = "Compte " + jeton;
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, ?)",
                id.toString(), code, description, disabled);
        comptes.add(id);
        AdmUser compte = new AdmUser();
        compte.setId(id);
        compte.setCode(code);
        compte.setDescription(description);
        compte.setDisabled(disabled);
        return compte;
    }

    /** Colonnes {@code AdmUser} d'un compte ; clés insensibles à la casse. */
    public Map<String, Object> ligne(UUID compte) {
        return jdbc.queryForMap("SELECT * FROM AdmUser WHERE PKey = ?", compte.toString());
    }

    /** Pose une colonne d'{@code AdmUser} sans passer par l'API ; {@code colonne} est une constante du test. */
    public void poser(UUID compte, String colonne, Object valeur) {
        jdbc.update("UPDATE AdmUser SET " + colonne + " = ? WHERE PKey = ?", valeur, compte.toString());
    }

    public void desactiver(UUID compte) {
        jdbc.update("UPDATE AdmUser SET Disabled = 1 WHERE PKey = ?", compte.toString());
    }

    /** Id du compte de ce code, à la casse près. */
    public UUID idDe(String code) {
        return jdbc.queryForObject("SELECT PKey FROM AdmUser WHERE UPPER(Code) = UPPER(?)", UUID.class, code);
    }

    public int nombreComptes(String code) {
        return compter("SELECT COUNT(*) FROM AdmUser WHERE UPPER(Code) = UPPER(?)", code);
    }

    /** Codes exacts de tous les comptes du test. */
    public List<String> codesDuJeton() {
        return jdbc.queryForList("SELECT Code FROM AdmUser WHERE UPPER(Code) LIKE ? ORDER BY PKey ASC", String.class,
                "%" + jeton + "%");
    }

    public Etat etat(UUID compte) {
        String c = compte.toString();
        return new Etat(ligne(compte),
                jdbc.queryForList("SELECT PKey, AdmRoleId FROM AdmUserRole WHERE AdmUserId = ? ORDER BY PKey ASC", c),
                jdbc.queryForList("SELECT PKey, StlDepositId FROM StlDepositUser WHERE AdmUserId = ? ORDER BY PKey ASC", c),
                habilitations(compte), tokenVersion(compte),
                tracesObjet(compte) + compter(HISTORIQUE_HABILITATIONS, TYPE_AUDIT_HABILITATION, c));
    }

    // ------------------------------------------------------------------ session

    /** Identifiants Arpent validés. */
    public void credential(UUID compte, int tokenVersion) {
        credential(compte, tokenVersion, "$2a$10$abcdefghijklmnopqrstuv");
    }

    public void credential(UUID compte, int tokenVersion, String empreinte) {
        jdbc.update("""
                INSERT INTO AppUserCredential (PKey, AdmUserId, PasswordHash, MustChangePassword, ProfileCompleted, TokenVersion,
                    UserCreateDateTime) VALUES (?, ?, ?, 0, 1, ?, ?)""",
                UUID.randomUUID().toString(), compte.toString(), empreinte, tokenVersion,
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0)));
    }

    /** {@code null} si le compte n'a pas d'identifiants. */
    public Integer tokenVersion(UUID compte) {
        List<Integer> versions = jdbc.queryForList("SELECT TokenVersion FROM AppUserCredential WHERE AdmUserId = ?", Integer.class,
                compte.toString());
        return versions.isEmpty() ? null : versions.getFirst();
    }

    // ------------------------------------------------------------------ rôles

    public UUID role(String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmRole (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, "Role " + code);
        retirerApres("DELETE FROM AdmRole WHERE PKey = ?", id);
        return id;
    }

    /** Le rôle Administrateur de la base, créé s'il manque : le code ne doit désigner qu'une ligne. */
    public UUID administrateur() {
        List<UUID> existants = jdbc.queryForList("SELECT TOP 1 PKey FROM AdmRole WHERE Code = ? ORDER BY PKey ASC", UUID.class,
                ADMINISTRATEUR);
        return existants.isEmpty() ? role(ADMINISTRATEUR) : existants.getFirst();
    }

    /** Ligne {@code AdmUserRole}, rend sa PKey. */
    public UUID lierRole(UUID compte, UUID role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmUserRole (PKey, AdmRoleId, AdmUserId) VALUES (?, ?, ?)", id.toString(), role.toString(),
                compte.toString());
        return id;
    }

    /** Compte actif titulaire du rôle Administrateur en base. */
    public AdmUser administrateurActif() {
        AdmUser compte = compte();
        lierRole(compte.getId(), administrateur());
        return compte;
    }

    /**
     * Suspend les administrateurs actifs déjà en base (validé, rétabli par {@link #nettoyer()}), pour qu'un test décide seul
     * qui est le dernier. Rien d'autre ne lit {@code Disabled} sous MockMvc, les jetons de test portant leur rôle.
     */
    public void suspendreLesAdministrateurs() {
        jdbc.queryForList("""
                        SELECT u.PKey, u.Disabled FROM AdmUser u
                        JOIN AdmUserRole ur ON ur.AdmUserId = u.PKey JOIN AdmRole r ON r.PKey = ur.AdmRoleId
                        WHERE r.Code = ? AND ISNULL(u.Disabled, 0) = 0""", ADMINISTRATEUR)
                .forEach(ligne -> {
                    UUID id = UUID.fromString(ligne.get("PKey").toString());
                    administrateursSuspendus.put(id, ligne.get("Disabled"));
                    desactiver(id);
                });
    }

    public int administrateursActifs() {
        return compter("""
                SELECT COUNT(DISTINCT u.PKey) FROM AdmUser u
                JOIN AdmUserRole ur ON ur.AdmUserId = u.PKey JOIN AdmRole r ON r.PKey = ur.AdmRoleId
                WHERE r.Code = ? AND ISNULL(u.Disabled, 0) = 0""", ADMINISTRATEUR);
    }

    public void restriction(UUID role, Restriction restriction, Boolean valeur) {
        jdbc.update("UPDATE AdmRole SET " + restriction.colonne() + " = ? WHERE PKey = ?", valeur, role.toString());
    }

    public List<UUID> rolesDe(UUID compte) {
        return jdbc.queryForList("SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = ?", UUID.class, compte.toString());
    }

    public List<UUID> lignesRole(UUID compte, UUID role) {
        return jdbc.queryForList("SELECT PKey FROM AdmUserRole WHERE AdmUserId = ? AND AdmRoleId = ?", UUID.class,
                compte.toString(), role.toString());
    }

    // ------------------------------------------------------------------ référentiels de la fiche

    public UUID culture() {
        UUID langue = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmWebLanguages (PKey, Code, Description) VALUES (?, ?, ?)", langue.toString(), jeton, jeton);
        retirerApres("DELETE FROM AdmWebLanguages WHERE PKey = ?", langue);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmWebCultures (PKey, Code, LanguageId) VALUES (?, ?, ?)", id.toString(), "fr-" + jeton,
                langue.toString());
        retirerApres("DELETE FROM AdmWebCultures WHERE PKey = ?", id);
        return id;
    }

    public UUID depot(String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StlDeposit (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, "Depot " + code);
        retirerApres("DELETE FROM StlDeposit WHERE PKey = ?", id);
        return id;
    }

    /** Ligne {@code StlDepositUser}, rend sa PKey. */
    public UUID lierDepot(UUID compte, UUID depot) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StlDepositUser (PKey, AdmUserId, StlDepositId) VALUES (?, ?, ?)", id.toString(), compte.toString(),
                depot.toString());
        return id;
    }

    public List<UUID> depotsDe(UUID compte) {
        return jdbc.queryForList("SELECT StlDepositId FROM StlDepositUser WHERE AdmUserId = ?", UUID.class, compte.toString());
    }

    public List<UUID> lignesDepot(UUID compte, UUID depot) {
        return jdbc.queryForList("SELECT PKey FROM StlDepositUser WHERE AdmUserId = ? AND StlDepositId = ?", UUID.class,
                compte.toString(), depot.toString());
    }

    /** Tiers de type Employé. */
    public UUID personnel(String nom) {
        return tier(nom, type(TYPE_EMPLOYE, CODE_EMPLOYE));
    }

    /** Tiers de type Client : hors liste du champ Personnel. */
    public UUID client(String nom) {
        return tier(nom, type(TYPE_CLIENT, CODE_CLIENT));
    }

    private UUID type(UUID id, String code) {
        List<UUID> existants = jdbc.queryForList("SELECT PKey FROM CfgTierType WHERE Code = ?", UUID.class, code);
        if (!existants.isEmpty()) {
            return existants.getFirst();
        }
        jdbc.update("INSERT INTO CfgTierType (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, code);
        retirerApres("DELETE FROM CfgTierType WHERE PKey = ?", id);
        return id;
    }

    private UUID tier(String nom, UUID type) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgTier (PKey, Code, FirstName, LastName, Description, CfgTierTypeId) VALUES (?, ?, ?, ?, ?, ?)",
                id.toString(), "TIE-" + id, "Prenom", nom, nom, type.toString());
        retirerApres("DELETE FROM CfgTier WHERE PKey = ?", id);
        return id;
    }

    // ------------------------------------------------------------------ patrimoine et habilitations

    public UUID societe(String code, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (?, ?, ?, ?)", id.toString(), code,
                "Societe " + code, active);
        retirerApres("DELETE FROM CfgCompany WHERE PKey = ?", id);
        return id;
    }

    public UUID projet(String code, UUID societe) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)", id.toString(), code,
                "Projet " + code, societe.toString());
        retirerApres("DELETE FROM CfgProject WHERE PKey = ?", id);
        return id;
    }

    public UUID tranche(String code, UUID projet) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgTranche (PKey, Code, Description, CfgProjectId) VALUES (?, ?, ?, ?)", id.toString(), code,
                "Tranche " + code, projet.toString());
        retirerApres("DELETE FROM CfgTranche WHERE PKey = ?", id);
        return id;
    }

    public UUID projetTravaux(String code, UUID societe) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)", id.toString(), code,
                "Travaux " + code, societe.toString());
        retirerApres("DELETE FROM PrjProject WHERE PKey = ?", id);
        return id;
    }

    /** Ligne {@code AdmUserHabilitation} aux cibles données, {@code null} pour les autres ; rend sa PKey. */
    public UUID habiliter(UUID compte, UUID societe, UUID projet, UUID tranche, UUID projetTravaux) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgCompanyId, CfgProjectId, CfgTrancheId, PrjProjectId)
                VALUES (?, ?, ?, ?, ?, ?)""", id.toString(), compte.toString(), texte(societe), texte(projet), texte(tranche),
                texte(projetTravaux));
        return id;
    }

    private static String texte(UUID id) {
        return id == null ? null : id.toString();
    }

    /** Colonnes de toutes les lignes du compte, triées par PKey. */
    public List<Map<String, Object>> habilitations(UUID compte) {
        return jdbc.queryForList("""
                SELECT PKey, CfgCompanyId, CfgProjectId, CfgTrancheId, PrjProjectId FROM AdmUserHabilitation
                WHERE AdmUserId = ? ORDER BY PKey ASC""", compte.toString());
    }

    /** Lignes du compte qui visent {@code cible}, dans n'importe quelle colonne. */
    public List<UUID> lignesHabilitation(UUID compte, UUID cible) {
        String c = cible.toString();
        return jdbc.queryForList("""
                SELECT PKey FROM AdmUserHabilitation
                WHERE AdmUserId = ? AND (CfgCompanyId = ? OR CfgProjectId = ? OR CfgTrancheId = ? OR PrjProjectId = ?)""",
                UUID.class, compte.toString(), c, c, c, c);
    }

    /** Lignes du compte qui ne visent que {@code cible}. */
    public int lignesUneSeuleCible(UUID compte, UUID cible) {
        String c = cible.toString();
        return compter("""
                SELECT COUNT(*) FROM AdmUserHabilitation
                WHERE AdmUserId = ? AND ? IN (CfgCompanyId, CfgProjectId, CfgTrancheId, PrjProjectId)
                  AND IIF(CfgCompanyId IS NULL, 0, 1) + IIF(CfgProjectId IS NULL, 0, 1)
                    + IIF(CfgTrancheId IS NULL, 0, 1) + IIF(PrjProjectId IS NULL, 0, 1) = 1""", compte.toString(), c);
    }

    // ------------------------------------------------------------------ secrets et audit

    /** Remplit les 5 colonnes secrètes du compte, identifiants Arpent compris. */
    public Secrets secrets(UUID compte) {
        Secrets s = new Secrets("PASS" + jeton, "WEB" + jeton, "MAIL" + jeton, "BEE" + jeton, "HASH" + jeton);
        jdbc.update("UPDATE AdmUser SET Pass = ?, WebPass = ?, EmailPassword = ?, OpenBeePass = ? WHERE PKey = ?",
                s.pass(), s.webPass(), s.emailPassword(), s.openBeePass(), compte.toString());
        credential(compte, 0, s.passwordHash());
        return s;
    }

    /** Texte ({@code Description} + {@code Details}) des traces du compte pour cette action. */
    public List<String> traces(UUID compte, String action) {
        return jdbc.queryForList("""
                SELECT CONCAT(Description, ' ', CONVERT(nvarchar(max), Details)) FROM AdmWebHistorics
                WHERE ObjectId = ? AND Action = ? AND ObjectType = ?""", String.class, compte.toString(), action, TYPE_AUDIT);
    }

    /** Texte de toutes les traces portant sur cet objet, quels qu'en soient le type et l'action. */
    public List<String> textesTraces(UUID objet) {
        return jdbc.queryForList("""
                SELECT CONCAT(Description, ' ', ObjectLabel, ' ', CONVERT(nvarchar(max), Details)) FROM AdmWebHistorics
                WHERE ObjectId = ?""", String.class, objet.toString());
    }

    /** Traces d'habilitation du compte pour cette action : {@code ObjectId} y est la ligne, {@code ObjectLabel} le compte. */
    public int tracesHabilitation(UUID compte, String action) {
        return compter(HISTORIQUE_HABILITATIONS + " AND Action = ?", TYPE_AUDIT_HABILITATION, compte.toString(), action);
    }

    public int tracesObjet(UUID objet) {
        return compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?", objet.toString());
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
