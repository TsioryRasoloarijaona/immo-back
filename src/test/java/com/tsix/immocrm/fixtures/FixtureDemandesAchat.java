package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * N6a (FEATURE-n6-demandes-achat.md) : demandes internes d'achat ({@code BuySelection}) sur une entité {@code StrEntity} propre au
 * test, ses 4 statuts (Préparation / Dépouillement / Clôturé / Annulé, types de prod), sa procédure, ses transitions et intervenants.
 * SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction du test.
 */
@Component
public class FixtureDemandesAchat {

    public static final String DROIT_LECTURE = "Yes.Buy.Front.CallOfTender.BuySelectionList";
    public static final String DROIT_NOUVEAU = DROIT_LECTURE + ".toolNew";
    public static final String DROIT_SUPPRIMER = DROIT_LECTURE + ".toolDelete";
    public static final String DROIT_EXPORTER = DROIT_LECTURE + ".toolExport";
    public static final String DROIT_CONFIDENTIEL = DROIT_LECTURE + ".IsConfidential";
    public static final String DROIT_PROCEDURES = "Yes.Buy.Front.Config.BuySelectionKind";
    public static final List<String> TOUS_LES_DROITS = List.of(DROIT_LECTURE, DROIT_NOUVEAU, DROIT_SUPPRIMER, DROIT_EXPORTER,
            DROIT_CONFIDENTIEL);

    public static final UUID TYPE_NOUVEAU = FixtureAchat.ETAT_NOUVEAU;
    public static final UUID TYPE_INTERMEDIAIRE = FixtureAchat.ETAT_INTERMEDIAIRE;
    public static final UUID TYPE_SUCCES = FixtureAchat.ETAT_TERMINE_SUCCES;
    public static final UUID TYPE_ECHEC = FixtureAchat.ETAT_TERMINE_ECHEC;
    public static final UUID TYPE_ANNULATION = FixtureAchat.ETAT_ANNULATION;

    public static final String DEMANDE = "BuySelection";
    public static final String PROCEDURE = "BuySelectionKind";
    public static final String STATUT = "StrStatus";
    public static final String TRANSITION = "StrWorkFlow";

    private static final String HISTORIQUE = "SELECT * FROM StrHistory WHERE ObjectId = ? ORDER BY DocDate ASC";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encodeur;
    private final FixtureAchat achat;
    private final FixtureLotFiche lotFiche;
    private final FixtureLotsParite parite;
    private final FixtureProjetLots lots;

    public FixtureDemandesAchat(EntityManager em, JdbcTemplate jdbc, PasswordEncoder encodeur, FixtureAchat achat,
                                FixtureLotFiche lotFiche, FixtureLotsParite parite, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.encodeur = encodeur;
        this.achat = achat;
        this.lotFiche = lotFiche;
        this.parite = parite;
        this.lots = lots;
    }

    /** Entité de test et ses statuts : {@code prep} (Nouveau), {@code depouil} (Intermédiaire), {@code clot} (Fin succès), {@code annul}. */
    public record Cadre(String jeton, UUID societe, UUID projet, UUID entite, String entiteLibelle, UUID prep, UUID depouil,
                        UUID clot, UUID annul, UUID procedure, UUID moduleAchat) {

        public List<UUID> statuts() {
            return List.of(prep, depouil, clot, annul);
        }

        public String code(String suffixe) {
            return suffixe + "-" + jeton;
        }
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    // ------------------------------------------------------------------ schéma et référentiels

    /** Colonnes de la CI sur {@code BuySelection} / {@code BuySelectionKind}, types de statut de prod, module BUY. */
    public void preparer() {
        lotFiche.tablesDemandesAchat();
        typeStatut(TYPE_NOUVEAU, "Nouveau", 1);
        typeStatut(TYPE_INTERMEDIAIRE, "Intermédiaire", 2);
        typeStatut(TYPE_SUCCES, "Terminé avec succès", 3);
        typeStatut(TYPE_ECHEC, "Terminé avec échec", 4);
        typeStatut(TYPE_ANNULATION, "Annulation", 5);
        em.flush();
        achat.module("BUY");
    }

    private void typeStatut(UUID id, String libelle, int ordre) {
        maj("""
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = ?)
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, ?, ?, ?)""",
                s(id), s(id), libelle, libelle, ordre);
    }

    /** Cadre complet : société, projet, entité propre, 4 statuts et une procédure « sans vérification » de cette entité. */
    public Cadre cadre(String prefixe) {
        preparer();
        String jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique(prefixe + "-SOC-" + jeton);
        UUID projet = parite.projet(prefixe + "-PRJ-" + jeton, societe);
        String libelle = "BuyCallOfTenderT-" + jeton;
        UUID entite = entite(libelle);
        UUID prep = statut(entite, "PREP-" + jeton, TYPE_NOUVEAU, 1, true);
        UUID depouil = statut(entite, "DEP-" + jeton, TYPE_INTERMEDIAIRE, 2, true);
        UUID clot = statut(entite, "CLO-" + jeton, TYPE_SUCCES, 3, false);
        UUID annul = statut(entite, "ANN-" + jeton, TYPE_ANNULATION, 4, true);
        UUID procedure = procedure("PROC-" + jeton, entite);
        return new Cadre(jeton, societe, projet, entite, libelle, prep, depouil, clot, annul, procedure, achat.module("BUY"));
    }

    /** Autre projet de la société du cadre, sur lequel aucun compte n'est habilité. */
    public UUID projetDe(Cadre c, String code) {
        return parite.projet(code, c.societe());
    }

    /** {@code StrEntity} propre au test ; code = libellé. */
    public UUID entite(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)", s(id), code, code);
        return id;
    }

    /** Statut typé de l'entité ; code = libellé ; {@code CanDelete} donné, {@code BoolField001..004} à 0. */
    public UUID statut(UUID entite, String code, UUID type, int ordre, boolean peutSupprimer) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName, CanDelete,
                    IsReadOnly, BoolField001, BoolField002, BoolField003, BoolField004)
                VALUES (?, ?, ?, ?, ?, ?, N'LimeGreen', ?, 0, 0, 0, 0, 0)""",
                s(id), code, code, ordre, s(entite), s(type), peutSupprimer);
        return id;
    }

    /** {@code BoolField001..004} (bordereau, présélection, offres, sélection) du statut. */
    public void phases(UUID statut, boolean bordereau, boolean preselection, boolean offres, boolean selection) {
        maj("UPDATE StrStatus SET BoolField001 = ?, BoolField002 = ?, BoolField003 = ?, BoolField004 = ? WHERE PKey = ?",
                bordereau, preselection, offres, selection, s(statut));
    }

    /** Procédure d'achat de l'entité, « sans vérification » du montant, ni confidentielle ni multi-fournisseur ; code = libellé. */
    public UUID procedure(String code, UUID entite) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuySelectionKind (PKey, Code, Description, StrEntityManagmentId, MaxAmount, IsConfidential,
                    IsMultiSupplier, IsWithoutVerification)
                VALUES (?, ?, ?, ?, 0, 0, 0, 1)""", s(id), code, code, s(entite));
        return id;
    }

    /** Procédure soumise au contrôle du montant (R6) : {@code IsWithoutVerification = 0}, {@code MaxAmount} donné. */
    public UUID procedureVerifiee(String code, UUID entite, BigDecimal montantMax) {
        UUID id = procedure(code, entite);
        maj("UPDATE BuySelectionKind SET IsWithoutVerification = 0, MaxAmount = ? WHERE PKey = ?", montantMax, s(id));
        return id;
    }

    public void confidentielle(UUID procedure) {
        maj("UPDATE BuySelectionKind SET IsConfidential = 1 WHERE PKey = ?", s(procedure));
    }

    /** Procédure sans entité fonctionnelle (R3). */
    public UUID procedureSansEntite(String code) {
        return procedure(code, null);
    }

    // ------------------------------------------------------------------ transitions et intervenants

    /** {@code StrWorkFlow} de l'entité, sans exigence ; code = libellé. */
    public UUID transition(UUID entite, UUID depuis, UUID vers, String libelle, int ordre) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                    WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, 0, ?)""",
                s(id), s(depuis), s(vers), ordre, libelle, libelle, s(entite));
        return id;
    }

    /** {@code WithComment}, {@code WithPassword}, {@code WithUserDate} de la transition. */
    public void exigences(UUID transition, boolean commentaire, boolean motDePasse, boolean dateUtilisateur) {
        maj("UPDATE StrWorkFlow SET WithComment = ?, WithPassword = ?, WithUserDate = ? WHERE PKey = ?",
                commentaire, motDePasse, dateUtilisateur, s(transition));
    }

    /** Intervenant « rôle » sur la transition ; {@code motDePasse} = {@code StrWorkFlowIntervener.WithPassword}. */
    public void intervenantRole(UUID entite, UUID role, UUID transition, boolean motDePasse) {
        UUID id = intervenant(entite, role, null);
        lierTransition(id, transition, motDePasse);
    }

    /** Intervenant « compte » ({@code StrIntervener.AdmUserId}) sur la transition. */
    public void intervenantCompte(UUID entite, AdmUser compte, UUID transition, boolean motDePasse) {
        UUID id = intervenant(entite, null, compte.getId());
        lierTransition(id, transition, motDePasse);
    }

    /** Statuts visibles du rôle ({@code StrStatusIntervener}), comme {@code IntervenantCourant.statutsDeLotsEtProjets} les lit. */
    public void voitStatuts(UUID entite, UUID role, List<UUID> statuts) {
        UUID id = intervenant(entite, role, null);
        for (UUID statut : statuts) {
            maj("INSERT INTO StrStatusIntervener (PKey, StrIntervenerId, StrStatusId) VALUES (?, ?, ?)",
                    s(UUID.randomUUID()), s(id), s(statut));
        }
    }

    private UUID intervenant(UUID entite, UUID role, UUID compte) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, AdmUserId, StrConfigEntityId) VALUES (?, ?, ?, ?, ?, ?)""",
                s(id), "N6-" + id.toString().substring(0, 8), "Intervenant N6", s(role), s(compte), s(entite));
        return id;
    }

    private void lierTransition(UUID intervenant, UUID transition, boolean motDePasse) {
        maj("INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId, WithPassword) VALUES (?, ?, ?, ?)",
                s(UUID.randomUUID()), s(intervenant), s(transition), motDePasse);
    }

    // ------------------------------------------------------------------ demandes

    /** DIA du cadre (société, projet, entité, procédure du cadre), dernière de son groupe, module BUY, datée du 10/03/2026. */
    public UUID demande(Cadre c, String code, UUID statut) {
        return demande(c, code, statut, c.procedure(), LocalDate.of(2026, 3, 10));
    }

    public UUID demande(Cadre c, String code, UUID statut, UUID procedure, LocalDate date) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuySelection (PKey, Code, Description, DocDate, CfgCompanyId, PrjProjectId, StrEntityId, StrStatusId,
                    BuySelectionKindId, IsTheLast, GroupId, AdmModuleId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)""",
                s(id), code, "DIA " + code, Timestamp.valueOf(date.atStartOfDay()), s(c.societe()), s(c.projet()),
                s(c.entite()), s(statut), s(procedure), s(id), s(c.moduleAchat()));
        return id;
    }

    /** Colonne d'une ligne écrite par le test ({@code table} et {@code colonne} fixes, jamais une saisie). */
    public void poser(String table, UUID id, String colonne, Object valeur) {
        Object v = valeur instanceof UUID u ? s(u) : valeur;
        maj("UPDATE " + table + " SET " + colonne + " = ? WHERE PKey = ?", v, s(id));
    }

    public void sansProjet(UUID demande) {
        maj("UPDATE BuySelection SET PrjProjectId = NULL WHERE PKey = ?", s(demande));
    }

    /** Lot de marché du projet du cadre ; {@code multi} = genre Fournisseurs ({@code IsMultiSupplier = 1}), sinon Entrepreneurs. */
    public UUID lot(Cadre c, String code, boolean multi) {
        parite.genresDeLot();
        String genre = multi ? FixtureLotsParite.FOURNISSEURS : FixtureLotsParite.ENTREPRENEURS;
        UUID statutLot = parite.statut(genre, "SL-" + code);
        return parite.lot(c.projet(), code, genre, statutLot);
    }

    /** Fournisseur ({@code CfgTier}, raison sociale {@code nom}) retenu sur la DIA ({@code BuySelection.CfgTierId}). */
    public UUID fournisseur(UUID demande, String code, String nom) {
        UUID id = achat.fournisseur(code, nom);
        poser(DEMANDE, demande, "CfgTierId", id);
        return id;
    }

    /** Pièce d'achat qui cite la DIA ({@code BuyDocument.BuySelectionId}, FK {@code NO_ACTION}). */
    public UUID pieceLiee(UUID demande, String code) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, AmountHT, AmountTTC, AmountToPay, BuySelectionId)
                VALUES (?, ?, N'QUOTE', ?, 0, 0, 0, ?)""",
                s(id), code, Timestamp.valueOf(LocalDateTime.of(2026, 3, 12, 0, 0)), s(demande));
        return id;
    }

    // ------------------------------------------------------------------ comptes

    /** Compte libre (un rôle à {@code WithSequenceFreeRestriction = 0}), habilité sur la société et le projet du cadre. */
    public AdmUser libre(Cadre c, String code, List<String> droits) {
        AdmUser compte = lots.compte(code);
        habiliter(c, compte, droits);
        return compte;
    }

    /** Compte restreint (rôle propre à 1), habilité sur la société et le projet, voyant les 4 statuts du cadre. */
    public AdmUser restreint(Cadre c, String code, List<String> droits) {
        AdmUser compte = restreintSansStatut(c, code, droits);
        voitStatuts(c.entite(), parite.roleDe(compte), c.statuts());
        return compte;
    }

    /** Compte restreint habilité (société, projet), sans aucun statut visible. */
    public AdmUser restreintSansStatut(Cadre c, String code, List<String> droits) {
        AdmUser compte = parite.restreint(code);
        habiliter(c, compte, droits);
        return compte;
    }

    private void habiliter(Cadre c, AdmUser compte, List<String> droits) {
        achat.habiliterSociete(compte, c.societe());
        achat.habiliter(compte, c.projet());
        for (String droit : droits) {
            achat.droitOmran(compte, droit);
        }
    }

    public UUID roleDe(AdmUser compte) {
        return parite.roleDe(compte);
    }

    /** Principal du compte sans rôle ajouté : les rôles sont lus en base ({@code AdmUserRole}). */
    public static RequestPostProcessor qui(AdmUser compte) {
        return FixtureDossierVente.connecte(compte);
    }

    /** Identifiants Arpent ({@code AppUserCredential}, BCrypt) du compte. */
    public void identifiants(AdmUser compte, String motDePasse) {
        maj("""
                INSERT INTO AppUserCredential (PKey, AdmUserId, PasswordHash, MustChangePassword, ProfileCompleted, TokenVersion,
                    UserCreateDateTime) VALUES (?, ?, ?, 0, 1, 0, ?)""",
                s(UUID.randomUUID()), s(compte.getId()), encodeur.encode(motDePasse),
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0)));
    }

    public void desactiver(AdmUser compte) {
        maj("UPDATE AdmUser SET Disabled = 1 WHERE PKey = ?", s(compte.getId()));
    }

    // ------------------------------------------------------------------ lecture

    public Map<String, Object> demandeEnBase(UUID id) {
        em.flush();
        return jdbc.queryForMap("SELECT * FROM BuySelection WHERE PKey = ?", s(id));
    }

    public List<Map<String, Object>> historique(UUID objet) {
        em.flush();
        return jdbc.queryForList(HISTORIQUE, s(objet));
    }

    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, parametres);
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
