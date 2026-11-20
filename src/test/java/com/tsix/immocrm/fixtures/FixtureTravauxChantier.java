package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Travaux chantier et Modifications (FEATURE-n2-travaux-chantier.md, N2a). {@code GrhWorkedDay} n'a que 3 colonnes dans
 * yesRemsTest (2 + {@code BuyItemId} en CI) : les colonnes de prod sont ajoutées hors transaction. Workflow de l'entité
 * « GrhWorkedDay » (5 statuts aux PKey de prod) amorcé dans la transaction du test ; parents avant enfants (FK de la CI).
 */
@Component
public class FixtureTravauxChantier {

    public static final String TRAVAUX = "/api/projet/travaux-chantier";
    public static final String MODIFICATIONS = "/api/projet/modifications";

    // Droits D4 (AdmRight.Code de prod, vérifiés le 19/11)
    public static final String DROIT_LIRE = "Yes.Prj.Front.Bul.GrhWorkedDayView";
    public static final String DROIT_MODIFIER = "Yes.Prj.Front.Bul.GrhWorkedDayView.toolNew";
    public static final String DROIT_SUPPRIMER = "Yes.Prj.Front.Bul.GrhWorkedDayView.toolDelete";
    public static final String DROIT_MODIFS_LIRE = "Yes.Prj.Front.Modification.ModificationList";
    public static final String DROIT_MODIFS_MODIFIER = "Yes.Prj.Front.Modification.ModificationList.toolNew";

    /** Message OMRAN repris tel quel (D1, {@code RessourceWithWorkFlow.cs:416}). */
    public static final String MESSAGE_DEPART =
            "Vous n'avez pas les droits sur les états de création de l'entité 'Jour travaillé' !";

    /** {@code StrEntity} « GrhWorkedDay » de prod. */
    public static final UUID ENTITE = UUID.fromString("4905A0F9-DEFD-432F-9DD3-68703281B8F0");
    /** Statut de type « Nouveau » = statut de départ. */
    public static final UUID EN_COURS = UUID.fromString("4FF71EA4-D96F-41A5-9D42-F4EB8E16926A");
    public static final UUID DEMANDE_VALIDATION = UUID.fromString("C0588C30-A603-4784-9CB2-47B641DAF1DB");
    public static final UUID VALIDEE = UUID.fromString("E1C7EF84-A951-4F22-AEB9-EDC143EA044D");
    public static final UUID ANNULEE = UUID.fromString("5EC87BB4-E549-4180-A2CF-CF74034A0875");
    public static final UUID REJETEE = UUID.fromString("DF8AFD3E-0A98-47D3-92FB-567DA2D343E5");

    // Transitions de prod (N2b, mesurées le 20/11) : aucune exigence, aucun intervenant
    public static final UUID T_DEMANDE = UUID.fromString("05BEB173-B516-47CE-BACF-9C1699C84549");
    public static final UUID T_VALIDATION = UUID.fromString("09DBFB4F-95A2-4783-911B-0D8718342398");
    public static final UUID T_ANNULATION = UUID.fromString("BD4630E8-08AC-4EF3-949F-99DCD41BA51E");
    public static final UUID T_REJET = UUID.fromString("23926D6E-2E29-41C7-8B8C-1F43A0277327");
    public static final List<UUID> TOUTES_LES_TRANSITIONS = List.of(T_DEMANDE, T_VALIDATION, T_ANNULATION, T_REJET);
    public static final List<UUID> TOUS_LES_STATUTS = List.of(EN_COURS, DEMANDE_VALIDATION, VALIDEE, ANNULEE, REJETEE);

    private static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    private static final UUID TYPE_INTERMEDIAIRE = UUID.fromString("B53E0A4F-3D67-4BF9-8C0E-B24155865FD6");
    private static final UUID TYPE_SUCCES = UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA");
    private static final UUID TYPE_ANNULATION = UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0");
    private static final UUID TYPE_ECHEC = UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97");

    private static final String UID = "uniqueidentifier";
    private static final String TEXTE = "nvarchar(255)";
    private static final String DECIMAL = "decimal(26, 6)";
    private static final String DATE = "datetime";

    /** Colonnes de {@code GrhWorkedDay} en prod (nom, type SQL) ; {@code Pkey}, {@code PrjProjectId}, {@code BuyItemId} existent. */
    private static final List<String[]> COLONNES = List.of(
            new String[]{"DocDate", DATE}, new String[]{"Code", TEXTE}, new String[]{"Description", TEXTE},
            new String[]{"Notes", "nvarchar(1000)"}, new String[]{"Unity", TEXTE}, new String[]{"Quantity", DECIMAL},
            new String[]{"CfgEmployeeId", UID}, new String[]{"CfgCustomerId", UID}, new String[]{"CfgCompanyId", UID},
            new String[]{"GrhWorkedDayTypeId", UID}, new String[]{"UserCreate", TEXTE},
            new String[]{"UserCreateDateTime", DATE}, new String[]{"UserUpdate", TEXTE},
            new String[]{"UserUpdateDateTime", DATE}, new String[]{"UnitPriceHT", DECIMAL},
            new String[]{"Quantity1", DECIMAL}, new String[]{"UnitPriceHT1", DECIMAL},
            new String[]{"StlInvoiceCode", TEXTE}, new String[]{"StlInvoiceProformaCode", TEXTE},
            new String[]{"StrEntityId", UID}, new String[]{"CfgTrancheLevelingDefinitionId", UID},
            new String[]{"TimeStart", DATE}, new String[]{"TimeEnd", DATE}, new String[]{"Unity1", TEXTE},
            new String[]{"VatRatio", DECIMAL}, new String[]{"VatRatio1", DECIMAL}, new String[]{"StrStatusId", UID},
            new String[]{"CfgProjectZoneId", UID}, new String[]{"CfgProjectZone_X", "float"},
            new String[]{"CfgProjectZone_Y", "float"});

    /** Valeurs par défaut d'un type de travaux ({@code GrhWorkedDayType}). */
    public record ValeursType(String unite, String prixHT, String tva, String unite1, String prixHT1, String tva1) {
    }

    /** Société, son projet commercial ({@code CfgProject}) et un projet de chantier ({@code PrjProject}) de la société. */
    public record Chantier(UUID societe, UUID cfgProjet, UUID projet) {
    }

    /** Jour travaillé posé en SQL ; {@code projet}, {@code employe} nuls = colonne NULL. Quantité 3 × 150,5 ; Q1 2 × 20. */
    public record Jour(UUID societe, UUID projet, UUID type, UUID zone, UUID statut, LocalDateTime date, String code,
                       UUID employe) {
    }

    public record Compte(AdmUser user, UUID role, RequestPostProcessor connexion) {
    }

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final FixtureDossierVente vente;
    private final FixtureProjetLots lots;
    private final FixtureVisibiliteOmran visibilite;
    private final PasswordEncoder encodeur;

    public FixtureTravauxChantier(EntityManager em, JdbcTemplate jdbc, DataSource dataSource, FixtureDossierVente vente,
                                  FixtureProjetLots lots, FixtureVisibiliteOmran visibilite, PasswordEncoder encodeur) {
        this.em = em;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.vente = vente;
        this.lots = lots;
        this.visibilite = visibilite;
        this.encodeur = encodeur;
    }

    /** Colonnes (autocommit, durables) puis workflow de l'entité (dans la transaction du test). */
    public void preparer() {
        colonnes();
        workflow();
    }

    private void colonnes() {
        StringBuilder sql = new StringBuilder();
        for (String[] c : COLONNES) {
            sql.append("IF COL_LENGTH(N'dbo.GrhWorkedDay', N'").append(c[0]).append("') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD ")
                    .append(c[0]).append(' ').append(c[1]).append(" NULL;\n");
        }
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(sql.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Colonnes de GrhWorkedDay", e);
        }
    }

    private void workflow() {
        em.flush();
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'GrhWorkedDay')
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'GrhWorkedDay', N'Jour travaillé')""",
                ENTITE.toString());
        typeStatut(TYPE_NOUVEAU, "Nouveau", 10);
        typeStatut(TYPE_INTERMEDIAIRE, "Intermédiaire", 15);
        typeStatut(TYPE_SUCCES, "Succès", 20);
        typeStatut(TYPE_ANNULATION, "Annulation", 40);
        typeStatut(TYPE_ECHEC, "Términé avec échec", 50);
        UUID entite = entite();
        statut(entite, EN_COURS, "En cours", 10, TYPE_NOUVEAU, "Yellow");
        statut(entite, DEMANDE_VALIDATION, "Demande de validation", 20, TYPE_INTERMEDIAIRE, "Crimson");
        statut(entite, VALIDEE, "Validée", 60, TYPE_SUCCES, "Green");
        statut(entite, ANNULEE, "Annulée", 100, TYPE_ANNULATION, "DimGray");
        statut(entite, REJETEE, "Rejetée", 110, TYPE_ECHEC, "DimGray");
        // Code = libellé et CanDelete comme en prod (seul « En cours » est supprimable par un restreint, R-N2b-7)
        for (UUID s : TOUS_LES_STATUTS) {
            jdbc.update("UPDATE StrStatus SET Code = Description, CanDelete = ? WHERE PKey = ?", EN_COURS.equals(s),
                    s.toString());
        }
        transition(entite, T_DEMANDE, "Demande de validation", EN_COURS, DEMANDE_VALIDATION, 10);
        transition(entite, T_VALIDATION, "Validation", DEMANDE_VALIDATION, VALIDEE, 20);
        transition(entite, T_ANNULATION, "Annulation", EN_COURS, ANNULEE, 100);
        transition(entite, T_REJET, "Rejet", DEMANDE_VALIDATION, REJETEE, 120);
    }

    private void transition(UUID entite, UUID id, String libelle, UUID depuis, UUID vers, int ordre) {
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrWorkFlow WHERE PKey = ?)
                    INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                        WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                    VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, 0, ?)""",
                id.toString(), id.toString(), depuis.toString(), vers.toString(), ordre, libelle, libelle, entite.toString());
    }

    /** {@code WithComment}, {@code WithPassword}, {@code WithUserDate} de la transition (0 / 4 en prod : posés par le test). */
    public void exigences(UUID transition, boolean commentaire, boolean motDePasse, boolean dateUtilisateur) {
        maj("UPDATE StrWorkFlow SET WithComment = ?, WithPassword = ?, WithUserDate = ? WHERE PKey = ?",
                commentaire, motDePasse, dateUtilisateur, transition.toString());
    }

    /** Intervenant « rôle » de la transition ; {@code motDePasse} = {@code StrWorkFlowIntervener.WithPassword} (second valideur). */
    public void intervenantTransition(UUID transition, UUID role, boolean motDePasse) {
        UUID intervenant = UUID.randomUUID();
        UUID entite = entite();
        maj("""
                INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, AdmUserId, StrConfigEntityId)
                VALUES (?, ?, N'Intervenant travaux', ?, NULL, ?)""",
                intervenant.toString(), "GRH-" + intervenant.toString().substring(0, 8), role.toString(), entite.toString());
        maj("INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId, WithPassword) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), intervenant.toString(), transition.toString(), motDePasse);
    }

    /** Transition d'une autre entité {@code StrEntity} (2 statuts propres) : jamais permise sur un jour travaillé. */
    public UUID transitionEtrangere(String code) {
        UUID entite = UUID.randomUUID();
        UUID depuis = UUID.randomUUID();
        UUID vers = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)", entite.toString(), "ETR-" + code, "ETR-" + code);
        for (UUID s : List.of(depuis, vers)) {
            maj("""
                    INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName, CanDelete)
                    VALUES (?, ?, ?, 10, ?, ?, N'Red', 1)""",
                    s.toString(), "S-" + s.toString().substring(0, 8), "S-" + code, entite.toString(), TYPE_NOUVEAU.toString());
        }
        transition(entite, id, "Etrangère " + code, depuis, vers, 10);
        return id;
    }

    /** Identifiants Arpent ({@code AppUserCredential}, BCrypt) du compte : second valideur. */
    public void identifiants(AdmUser compte, String motDePasse) {
        maj("""
                INSERT INTO AppUserCredential (PKey, AdmUserId, PasswordHash, MustChangePassword, ProfileCompleted, TokenVersion,
                    UserCreateDateTime) VALUES (?, ?, ?, 0, 1, 0, ?)""",
                UUID.randomUUID().toString(), compte.getId().toString(), encodeur.encode(motDePasse),
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0)));
    }

    /** Ligne {@code StrHistory} posée en SQL sur le jour, datée (ordre de {@code GET …/historique}). */
    public void ligneHistorique(UUID jour, UUID statut, String description, LocalDateTime date) {
        maj("""
                INSERT INTO StrHistory (PKey, DocDate, Description, Notes, ObjectId, AdmUserDescription, StrStatusId,
                    StrConfigEntityId, UserDate) VALUES (?, ?, ?, N'', ?, N'Compte historique', ?, ?, ?)""",
                UUID.randomUUID().toString(), Timestamp.valueOf(date), description, jour.toString(), statut.toString(),
                entite().toString(), Timestamp.valueOf(date));
    }

    /** Lignes {@code StrHistory} du jour, plus ancienne d'abord. */
    public List<Map<String, Object>> historique(UUID jour) {
        flush();
        return jdbc.queryForList("SELECT * FROM StrHistory WHERE ObjectId = ? ORDER BY DocDate ASC", jour.toString());
    }

    /** {@code GrhWorkedDay.StrStatusId} en majuscules. */
    public String statutDe(UUID jour) {
        String s = lire("SELECT CAST(StrStatusId AS varchar(36)) FROM GrhWorkedDay WHERE Pkey = ?", jour);
        return s == null ? null : s.toUpperCase();
    }

    private void typeStatut(UUID id, String libelle, int ordre) {
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = ?)
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, ?, ?, ?)""",
                id.toString(), id.toString(), libelle, libelle, ordre);
    }

    private void statut(UUID entite, UUID id, String libelle, int ordre, UUID type, String couleur) {
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE PKey = ?)
                    INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName)
                    VALUES (?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), id.toString(), libelle, libelle, ordre, entite.toString(), type.toString(), couleur);
    }

    /** PKey de l'entité « GrhWorkedDay » (celle de prod, ou celle déjà présente sous ce code). */
    public UUID entite() {
        em.flush();
        return UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(PKey AS varchar(36)) FROM StrEntity WHERE Code = N'GrhWorkedDay' ORDER BY PKey ASC",
                String.class));
    }

    // ------------------------------------------------------------------ comptes

    /**
     * Compte non administrateur, rôle propre portant exactement ces droits, habilité sur ces projets de chantier ;
     * {@code restreint} = {@code AdmRole.WithSequenceFreeRestriction} (faux = tous les statuts visibles).
     */
    public Compte compte(String jeton, boolean restreint, Collection<UUID> projets, String... droits) {
        String role = "Travaux " + jeton + "-" + UUID.randomUUID().toString().substring(0, 6);
        AdmUser user = vente.compte("grh." + jeton, role);
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = ? WHERE Code = ?", restreint, role);
        UUID roleId = UUID.fromString(jdbc.queryForObject("SELECT CAST(PKey AS varchar(36)) FROM AdmRole WHERE Code = ?",
                String.class, role));
        projets.forEach(projet -> lots.habiliter(user, projet));
        for (String droit : droits) {
            jdbc.update("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), droit, droit, roleId.toString());
        }
        flush();
        return new Compte(user, roleId, FixtureDossierVente.connecte(user, role));
    }

    /** N2b : compte restreint qui voit ces statuts et intervient (sans mot de passe) sur ces transitions, habilité sur le projet. */
    public Compte restreint(String jeton, UUID projet, List<UUID> statuts, List<UUID> transitions, String... droits) {
        Compte c = compte(jeton, true, List.of(projet), droits);
        statuts.forEach(s -> intervenant(s, c.role()));
        transitions.forEach(t -> intervenantTransition(t, c.role(), false));
        flush();
        return c;
    }

    /** Ids JSON (minuscules) mis en majuscules, pour comparer aux constantes. */
    public static List<String> majuscules(List<String> ids) {
        return ids.stream().map(String::toUpperCase).toList();
    }

    public static String majuscules(UUID id) {
        return id.toString().toUpperCase();
    }

    /** Intervenant « rôle » d'un statut de l'entité : le statut devient visible des porteurs du rôle. */
    public void intervenant(UUID statut, UUID role) {
        visibilite.intervenantRole(entite(), statut, role);
    }

    /** Ligne {@code AdmUserHabilitation.CfgTrancheId} (périmètre de la liste Modifications, R7). */
    public void habiliterTranche(AdmUser compte, UUID tranche) {
        em.flush();
        jdbc.update("INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgTrancheId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), compte.getId().toString(), tranche.toString());
    }

    /** Ligne {@code AdmUserHabilitation.CfgProjectId} : périmètre commercial des dossiers de vente. */
    public void habiliterProjetCommercial(AdmUser compte, UUID cfgProjet) {
        em.flush();
        jdbc.update("INSERT INTO AdmUserHabilitation (PKey, AdmUserId, CfgProjectId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), compte.getId().toString(), cfgProjet.toString());
    }

    // ------------------------------------------------------------------ parents des jours travaillés

    public Chantier chantier(String code) {
        CfgTranche tranche = vente.tranche("T" + code, true);
        UUID societe = tranche.getCfgProject().getCfgCompany().getId();
        return new Chantier(societe, tranche.getCfgProject().getId(), projet(societe, code));
    }

    /** Projet de chantier ({@code PrjProject}) de la société. */
    public UUID projet(UUID societe, String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)",
                id.toString(), code, "Projet " + code, societe.toString());
        return id;
    }

    /** Zone ({@code CfgProjectZone}) du projet commercial. */
    public UUID zone(UUID cfgProjet, String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgProjectZone (PKey, Code, Description, Sorting, CfgProjectId) VALUES (?, ?, ?, 1, ?)",
                id.toString(), code, "Zone " + code, cfgProjet.toString());
        return id;
    }

    public UUID type(String code, ValeursType v) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO GrhWorkedDayType (PKey, Code, Description, GroupDescription, Unity, UnitPriceHT, VatRatio, Unity1,
                    UnitPriceHT1, VatRatio1) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), code, "Type " + code, "Groupe " + code, v.unite(), new BigDecimal(v.prixHT()),
                new BigDecimal(v.tva()), v.unite1(), new BigDecimal(v.prixHT1()), new BigDecimal(v.tva1()));
        return id;
    }

    /** Tiers ({@code CfgTier}) : personnel ou client ; nom de famille « Nom &lt;code&gt; ». */
    public UUID tiers(String code) {
        UUID id = vente.tier(code).getId();
        em.flush();
        return id;
    }

    public UUID jour(Jour j) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO GrhWorkedDay (Pkey, DocDate, Code, Description, Quantity, UnitPriceHT, Unity, VatRatio, Quantity1,
                    UnitPriceHT1, Unity1, VatRatio1, CfgCompanyId, PrjProjectId, GrhWorkedDayTypeId, CfgProjectZoneId,
                    StrEntityId, StrStatusId, CfgEmployeeId)
                VALUES (?, ?, ?, ?, 3, 150.5, N'J', 0.2, 2, 20, N'H', 0.1, ?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), Timestamp.valueOf(j.date()), j.code(), "Jour " + j.code(), j.societe().toString(),
                texte(j.projet()), j.type().toString(), j.zone().toString(), entite().toString(), j.statut().toString(),
                texte(j.employe()));
        return id;
    }

    // ------------------------------------------------------------------ dossiers et modifications

    /** Dossier de la tranche, article racine ({@code IsUnderItem = 0} sauf {@code sousArticle}), prix 1 000 000 TTC. */
    public ComFolder dossier(String code, CfgTranche tranche, boolean sousArticle) {
        ComFolder dossier = vente.dossier(code, tranche, StatutDossier.RESERVATION);
        em.flush();
        jdbc.update("UPDATE StkItem SET IsUnderItem = ? WHERE ComFolderId = ?", sousArticle, dossier.getId().toString());
        jdbc.update("""
                UPDATE ComFolder SET PriceAfterDiscount = 1000000, PriceAfterDiscountHT = 833333.33, Price = 1000000
                WHERE PKey = ?""", dossier.getId().toString());
        return dossier;
    }

    /** Demande de modification ({@code ComModification}) ; {@code etat} = nom d'{@code EtatModification}. */
    public UUID modification(ComFolder dossier, String etat, String description, LocalDateTime demande) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO ComModification (PKey, Description, DemandDate, Cost, CommercialAmount, CommercialAmountHT, Status,
                    Notes, ComFolderId) VALUES (?, ?, ?, 8000, 12000, 10000, ?, N'', ?)""",
                id.toString(), description, Timestamp.valueOf(demande), etat, dossier.getId().toString());
        return id;
    }

    // ------------------------------------------------------------------ lecture

    /** Comptage natif ; {@code ?} lié aux {@code parametres} (UUID passés en texte). */
    public int compter(String sql, Object... parametres) {
        flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, texte(parametres));
        return n == null ? 0 : n;
    }

    /** Première colonne de la première ligne, en texte, nulle si aucune ligne ou valeur NULL. */
    public String lire(String sql, Object... parametres) {
        flush();
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, texte(parametres));
    }

    /** Première colonne décimale de la première ligne, nulle si aucune ligne ou valeur NULL. */
    public BigDecimal montant(String sql, Object... parametres) {
        flush();
        return jdbc.query(sql, rs -> rs.next() ? rs.getBigDecimal(1) : null, texte(parametres));
    }

    /** Écrit les changements en attente et vide le cache : l'appel HTTP suivant relit la base. */
    public void flush() {
        em.flush();
        em.clear();
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
    }

    private static String texte(UUID id) {
        return id == null ? null : id.toString();
    }

    private static Object[] texte(Object... parametres) {
        Object[] valeurs = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            valeurs[i] = parametres[i] instanceof UUID u ? u.toString() : parametres[i];
        }
        return valeurs;
    }
}
