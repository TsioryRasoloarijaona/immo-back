package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Statuts et transitions de lot éditables (FEATURE-restes-projet-achat.md §6) : les 5 genres de lot, types de statut, champs
 * du genre, statuts / lots / transitions / historiques qui les citent. PKeys de yesRemsProd, lignes créées si absentes
 * (yesRemsTest n'a aucun genre de lot) ; parents insérés d'abord (FK de la CI).
 */
@Component
public class FixtureStatutsLot {

    public static final String DROIT_STATUTS = "Yes.Prj.Front.Config.MarketStatusList";
    public static final String DROIT_TRANSITIONS = "Yes.Prj.Front.Config.MarketWorkFlowList";

    // Genres de lot de prod (StrEntity) : code, PKey, libellé.
    public static final UUID ENTREPRENEURS = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B613");
    public static final UUID FOURNISSEURS = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B614");
    public static final UUID ADMINISTRATION = UUID.fromString("E7999F71-7507-40D9-AA78-A2825C42B614");
    public static final UUID MAITRES_OUVRAGE = UUID.fromString("E7999F72-7507-40D9-AA78-A2825C42B614");
    public static final UUID CONCESSIONNAIRES = UUID.fromString("E7999F74-7507-40D9-AA78-A2825C42B614");

    // StrStatusType « Intermédiaire » et champs du genre Entrepreneurs (StrField) de prod.
    public static final UUID TYPE_INTERMEDIAIRE = UUID.fromString("B53E0A4F-3D67-4BF9-8C0E-B24155865FD6");
    public static final UUID CHAMP_CAUSE_RESILIATION = UUID.fromString("274BD727-6CA2-4B7A-B918-0833D68EC392");
    public static final UUID CHAMP_SIGNATURE_MARCHE = UUID.fromString("F93CF6FE-5573-485D-A3D8-832FBE5FDBEB");
    public static final UUID CHAMP_LANCEMENT_TRAVAUX = UUID.fromString("40E00EAB-870B-45A8-91DD-EB6DAD09E273");

    private static final String SI_ABSENT = "IF NOT EXISTS (SELECT 1 FROM ";

    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public FixtureStatutsLot(EntityManager em, JdbcTemplate jdbc) {
        this.em = em;
        this.jdbc = jdbc;
    }

    /** Les 5 genres de lot, le type « Intermédiaire » et 3 champs du genre Entrepreneurs, créés s'ils manquent. */
    public void genres() {
        em.flush();
        genre(ENTREPRENEURS, "PrjMarket", "Lot Entrepreneurs");
        genre(FOURNISSEURS, "PrjMarketMultiSupplier", "Lot Fournisseurs");
        genre(ADMINISTRATION, "PrjMarketAdministrative", "Lot Administration");
        genre(MAITRES_OUVRAGE, "PrjMarketStudies", "Lot Maitres d'oeuvre");
        genre(CONCESSIONNAIRES, "PrjMarketDealers", "Lot Concessionnaires");
        jdbc.update(SI_ABSENT + "StrStatusType WHERE PKey = ?) "
                        + "INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, N'Intermédiaire', N'Intermédiaire', 1)",
                TYPE_INTERMEDIAIRE.toString(), TYPE_INTERMEDIAIRE.toString());
        champ(CHAMP_CAUSE_RESILIATION, "CancellationCause", "cause de résiliation", 250, ENTREPRENEURS);
        champ(CHAMP_SIGNATURE_MARCHE, "ContractSignatureDate", "Date signature du marché", 190, ENTREPRENEURS);
        champ(CHAMP_LANCEMENT_TRAVAUX, "WorkLaunchDate", "Date de lancement des travaux", 210, ENTREPRENEURS);
    }

    /** Libellés des cases du genre Entrepreneurs comme en prod : 001-005 et 007-010 renseignés, 006 sans libellé. */
    public void libellesEntrepreneurs() {
        em.flush();
        jdbc.update("""
                        UPDATE StrEntity SET BoolField001Label = N'Est un marché effectif', BoolField002Label = N'Est une annulation',
                            BoolField003Label = N'Avec prévision', BoolField004Label = N'Avec bordereau estimatif',
                            BoolField005Label = N'Avec demande interne d''achat', BoolField006Label = NULL,
                            BoolField007Label = N'Avec décomposition', BoolField008Label = N'Avec avances sur décompte',
                            BoolField009Label = N'Avec décomptes', BoolField010Label = N'Avec avances et retenues',
                            BoolField011Label = NULL, BoolField012Label = NULL, BoolField013Label = NULL, BoolField014Label = NULL,
                            BoolField015Label = NULL, BoolField016Label = NULL, BoolField017Label = NULL, BoolField018Label = NULL,
                            BoolField019Label = NULL, BoolField020Label = NULL
                        WHERE PKey = ?""", ENTREPRENEURS.toString());
    }

    private void genre(UUID id, String code, String description) {
        jdbc.update(SI_ABSENT + "StrEntity WHERE PKey = ?) INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)",
                id.toString(), id.toString(), code, description);
    }

    private void champ(UUID id, String code, String description, int ordre, UUID genre) {
        jdbc.update(SI_ABSENT + "StrField WHERE PKey = ?) "
                        + "INSERT INTO StrField (PKey, Code, Description, Sorting, StrConfigEntityId) VALUES (?, ?, ?, ?, ?)",
                id.toString(), id.toString(), code, description, ordre, genre.toString());
    }

    /** Champ ({@code StrField}) propre au test, rattaché à un genre. */
    public UUID champDuGenre(String code, UUID genre) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StrField (PKey, Code, Description, Sorting, StrConfigEntityId) VALUES (?, ?, ?, 1, ?)",
                id.toString(), code, code, genre.toString());
        return id;
    }

    /** Statut de lot posé en SQL (code = description), type « Intermédiaire ». */
    public UUID statut(UUID genre, String code, int ordre) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("""
                        INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName, Delay)
                        VALUES (?, ?, ?, ?, ?, ?, N'LimeGreen', 0)""",
                id.toString(), code, code, ordre, genre.toString(), TYPE_INTERMEDIAIRE.toString());
        return id;
    }

    /** Champ obligatoire du statut ({@code StrStatusMandatoryField}). */
    public void champObligatoire(UUID statut, UUID champ) {
        em.flush();
        jdbc.update("INSERT INTO StrStatusMandatoryField (PKey, StrFieldId, StrStatusId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), champ.toString(), statut.toString());
    }

    /** Lot ({@code PrjMarket}) du genre, sur ce statut. */
    public UUID lot(String code, UUID genre, UUID statut) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO PrjMarket (PKey, Code, Description, StrEntityId, StrStatusId) VALUES (?, ?, ?, ?, ?)",
                id.toString(), code, code, genre.toString(), statut.toString());
        return id;
    }

    /** Transition de lot ({@code StrWorkFlow}) du genre, sans option. */
    public UUID transition(UUID genre, String libelle, UUID depart, UUID arrivee) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("""
                        INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment, WithPassword,
                            WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                        VALUES (?, ?, ?, 10, ?, ?, 0, 0, 0, 0, ?)""",
                id.toString(), depart.toString(), arrivee.toString(), libelle, libelle, genre.toString());
        return id;
    }

    /** Intervenant ({@code StrIntervener}) d'un rôle sur la transition ({@code StrWorkFlowIntervener}). */
    public UUID intervenant(UUID transition, UUID genre, UUID roleId, String code) {
        UUID intervenant = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, StrConfigEntityId) VALUES (?, ?, ?, ?, ?)",
                intervenant.toString(), code, code, roleId.toString(), genre.toString());
        jdbc.update("INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), intervenant.toString(), transition.toString());
        return intervenant;
    }

    /** Ligne d'historique ({@code StrHistory}) citant le statut. */
    public void historique(UUID statut, UUID genre, LocalDateTime quand) {
        em.flush();
        jdbc.update("""
                        INSERT INTO StrHistory (PKey, DocDate, Description, StrStatusId, StrConfigEntityId)
                        VALUES (?, ?, N'Passage de test', ?, ?)""",
                UUID.randomUUID().toString(), quand, statut.toString(), genre.toString());
    }

    // ------------------------------------------------------------------ lectures en base

    /** Ligne {@code StrStatus} (ids en minuscules), {@code null} si absente. */
    public Map<String, Object> ligne(UUID statut) {
        em.flush();
        List<Map<String, Object>> l = jdbc.queryForList("""
                SELECT Code, Description, Sorting, Notes, ColorName, Delay,
                    LOWER(CONVERT(varchar(36), StrStatusTypeId)) AS StrStatusTypeId,
                    LOWER(CONVERT(varchar(36), CfgImageId)) AS CfgImageId,
                    LOWER(CONVERT(varchar(36), StrConfigEntityId)) AS StrConfigEntityId,
                    CAST(ISNULL(BoolField001, 0) AS int) AS B1, CAST(ISNULL(BoolField002, 0) AS int) AS B2,
                    CAST(ISNULL(BoolField003, 0) AS int) AS B3, CAST(ISNULL(BoolField004, 0) AS int) AS B4,
                    CAST(ISNULL(BoolField005, 0) AS int) AS B5, CAST(ISNULL(BoolField007, 0) AS int) AS B7,
                    CAST(ISNULL(BoolField010, 0) AS int) AS B10
                FROM StrStatus WHERE PKey = ?""", statut.toString());
        return l.isEmpty() ? null : l.get(0);
    }

    /** Champs obligatoires du statut (ids {@code StrField} en minuscules). */
    public List<String> champsObligatoires(UUID statut) {
        em.flush();
        return jdbc.queryForList("""
                SELECT LOWER(CONVERT(varchar(36), StrFieldId)) FROM StrStatusMandatoryField WHERE StrStatusId = ?""",
                String.class, statut.toString());
    }

    /** Statuts du genre portant ce code exact (comparaison de la base, sans casse). */
    public int statutsDuCode(UUID genre, String code) {
        return compter("SELECT COUNT(*) FROM StrStatus WHERE StrConfigEntityId = ? AND Code = ?", genre.toString(), code);
    }

    /** Statuts portant cette description. */
    public int statutsDecrits(String description) {
        return compter("SELECT COUNT(*) FROM StrStatus WHERE Description = ?", description);
    }

    public int statutsParId(UUID statut) {
        return compter("SELECT COUNT(*) FROM StrStatus WHERE PKey = ?", statut.toString());
    }

    public int transitionsParId(UUID transition) {
        return compter("SELECT COUNT(*) FROM StrWorkFlow WHERE PKey = ?", transition.toString());
    }

    /** Transitions décrites ainsi ({@code Description}). */
    public int transitionsDecrites(String description) {
        return compter("SELECT COUNT(*) FROM StrWorkFlow WHERE Description = ?", description);
    }

    /** Transition décrite ainsi, entre ces deux statuts, dans ce genre. */
    public int transitionsEntre(String description, UUID genre, UUID depart, UUID arrivee) {
        return compter("""
                SELECT COUNT(*) FROM StrWorkFlow WHERE Description = ? AND StrConfigEntityId = ? AND StatusFromId = ?
                    AND StatusToId = ?""", description, genre.toString(), depart.toString(), arrivee.toString());
    }

    /** Id de l'unique transition décrite ainsi dans le genre ({@code null} si aucune). */
    public UUID transitionDecrite(String description, UUID genre) {
        em.flush();
        List<String> ids = jdbc.queryForList("""
                SELECT CONVERT(varchar(36), PKey) FROM StrWorkFlow WHERE Description = ? AND StrConfigEntityId = ?""",
                String.class, description, genre.toString());
        return ids.size() == 1 ? UUID.fromString(ids.get(0)) : null;
    }

    /** Rôles ({@code AdmRoleId}, minuscules) atteints par les liens {@code StrWorkFlowIntervener → StrIntervener} de la transition. */
    public List<String> rolesDeTransition(UUID transition) {
        em.flush();
        return jdbc.queryForList("""
                SELECT LOWER(CONVERT(varchar(36), i.AdmRoleId)) FROM StrWorkFlowIntervener l
                JOIN StrIntervener i ON i.PKey = l.StrIntervenerId WHERE l.StrWorkFlowId = ?""",
                String.class, transition.toString());
    }

    /** Intervenants ({@code StrIntervener}) du rôle dans le genre. */
    public int intervenantsDuRole(UUID roleId, UUID genre) {
        return compter("SELECT COUNT(*) FROM StrIntervener WHERE AdmRoleId = ? AND StrConfigEntityId = ?",
                roleId.toString(), genre.toString());
    }

    /** Liens de la transition vers cet intervenant précis. */
    public int liensVers(UUID transition, UUID intervenant) {
        return compter("SELECT COUNT(*) FROM StrWorkFlowIntervener WHERE StrWorkFlowId = ? AND StrIntervenerId = ?",
                transition.toString(), intervenant.toString());
    }

    public int liensIntervenants(UUID transition) {
        return compter("SELECT COUNT(*) FROM StrWorkFlowIntervener WHERE StrWorkFlowId = ?", transition.toString());
    }

    private int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
