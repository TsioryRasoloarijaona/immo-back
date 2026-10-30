package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tâches, lot 1.2 (FEATURE-admin-taches.md), en SQL natif : les tests ne dépendent pas de l'entité que le lot crée.
 * Paramétrage de production à ses PKey : entités, six statuts, neuf transitions, séquence ; routes et champs = contrat du fil.
 */
@Component
public class FixtureTaches {

    // ------------------------------------------------------------------ routes
    public static final String RACINE = "/api/administration/taches";
    public static final String FICHE = RACINE + "/{id}";
    public static final String STATUT = FICHE + "/statut";
    public static final String STATUTS = RACINE + "/statuts";

    // ------------------------------------------------------------------ champs du fil
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String TYPE_ID = "typeId";
    public static final String DECLENCHEUR_ID = "declencheurId";
    public static final String DATE_PREVUE = "datePrevue";
    public static final String DUREE_PREVUE = "dureePrevue";
    public static final String DATE_FIN_PREVUE = "dateFinPrevue";
    public static final String DATE_DEBUT = "dateDebut";
    public static final String DUREE = "duree";
    public static final String DATE_FIN = "dateFin";
    public static final String MODULE_ID = "moduleId";
    public static final String PRIORITE_ID = "prioriteId";
    public static final String GESTIONNAIRE_ID = "gestionnaireId";
    public static final String ASSIGNE_ID = "assigneId";
    public static final String ENTITE_ID = "entiteId";
    public static final String PROJET_ID = "projetId";
    public static final String NOTES = "notes";
    public static final String STATUT_ID = "statutId";
    public static final String COMMENTAIRE = "commentaire";
    public static final String TEXTE = "texte";

    // ------------------------------------------------------------------ PKey de production
    public static final UUID ENTITE_TACHE = UUID.fromString("C174BD95-A268-47B0-8F5E-2ABE33A22082");
    public static final UUID ENTITE_RENDEZ_VOUS = UUID.fromString("613B1F65-8119-4E86-A826-68602388AE98");
    public static final String SEQUENCE = "AdmTask";
    public static final int VALEUR_SEQUENCE = 109;
    private static final UUID PKEY_SEQUENCE = UUID.fromString("75F54EEE-99B0-45C6-B64F-53FC93D936AC");
    private static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    private static final UUID TYPE_INTERMEDIAIRE = UUID.fromString("B53E0A4F-3D67-4BF9-8C0E-B24155865FD6");
    private static final UUID TYPE_SUCCES = UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA");
    private static final UUID TYPE_ANNULATION = UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0");

    /** {@code StrStatus} de l'entité tâche ; Code = Description = {@code libelle}. */
    public record Statut(UUID id, String libelle, int ordre, UUID type) {

        public String fil() {
            return id.toString();
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    /** {@code StrWorkFlow} de l'entité tâche. */
    public record Transition(UUID id, Statut depuis, Statut vers, int ordre, boolean commentaire) {

        public String code() {
            return depuis.libelle() + " => " + vers.libelle();
        }

        public String description() {
            return vers.libelle();
        }

        @Override
        public String toString() {
            return code();
        }
    }

    public static final Statut NON_COMMENCEE = new Statut(UUID.fromString("887351C2-D10B-43E4-B157-42346889B9D0"),
            "Non commencée", 10, TYPE_NOUVEAU);
    public static final Statut EN_COURS = new Statut(UUID.fromString("555EE389-F981-4B2C-9A86-706625DA5B16"),
            "En cours de réalisation", 20, TYPE_INTERMEDIAIRE);
    public static final Statut TERMINEE = new Statut(UUID.fromString("91AD2408-FBF0-487A-A1A0-D3E20DDD5F83"),
            "Terminée", 50, TYPE_SUCCES);
    public static final Statut ATTENTE = new Statut(UUID.fromString("339967AA-C617-4F6A-8AE3-F4DA9911B418"),
            "Attente de résolution de problème", 60, TYPE_INTERMEDIAIRE);
    public static final Statut ANNULEE = new Statut(UUID.fromString("D72A153D-5C6E-407E-9EB7-87AC522D57F9"),
            "Annulée", 100, TYPE_ANNULATION);
    public static final Statut DIFFEREE = new Statut(UUID.fromString("74D2B398-4A30-411A-9BD1-92A531604179"),
            "Différée", 110, TYPE_INTERMEDIAIRE);
    /** Ordre {@code Sorting}. */
    public static final List<Statut> STATUTS_TACHE = List.of(NON_COMMENCEE, EN_COURS, TERMINEE, ATTENTE, ANNULEE, DIFFEREE);

    public static final List<Transition> TRANSITIONS = List.of(
            new Transition(UUID.fromString("150E452F-6FA1-4889-8903-2A65FBE488B8"), NON_COMMENCEE, EN_COURS, 20, false),
            new Transition(UUID.fromString("A59BFD83-8C02-46EF-B3D3-E693B6A4E789"), NON_COMMENCEE, ATTENTE, 50, false),
            new Transition(UUID.fromString("A1AD0586-5810-4E2D-B384-95294A473B97"), NON_COMMENCEE, DIFFEREE, 60, false),
            new Transition(UUID.fromString("153DE611-E405-42BF-9B50-48DE7C862506"), EN_COURS, TERMINEE, 30, false),
            new Transition(UUID.fromString("4EA6C25A-C2EE-4E7B-B571-C80F87B48625"), EN_COURS, ANNULEE, 40, false),
            new Transition(UUID.fromString("62146EFE-6573-4267-86FA-E0B01C3BB710"), ATTENTE, EN_COURS, 90, false),
            new Transition(UUID.fromString("D90D2FCD-B5A8-49E8-AD27-1F19D5906B77"), ATTENTE, ANNULEE, 100, true),
            new Transition(UUID.fromString("3A0FF27B-4309-49CA-B2BC-0F6517ECB4CA"), DIFFEREE, EN_COURS, 70, false),
            new Transition(UUID.fromString("6D3E4F7E-DDAA-4F3E-8CDE-6E03EA728768"), DIFFEREE, ANNULEE, 80, false));

    /** Paramétrage idempotent, exécutable dans la transaction du test ou validé ({@link #NETTOYAGE} l'efface alors). */
    public static final List<String> SEMIS = semis();

    /** Enfants d'abord : les FK de la CI portent sur {@code AdmTask}, {@code StrHistory} et {@code StrWorkFlow}. */
    public static final List<String> NETTOYAGE = List.of(
            "DELETE FROM AdmTask WHERE StrEntityId IN ('%s', '%s')".formatted(ENTITE_TACHE, ENTITE_RENDEZ_VOUS),
            "DELETE FROM StrHistory WHERE StrConfigEntityId = '%s'".formatted(ENTITE_TACHE),
            "DELETE FROM StrWorkFlow WHERE StrConfigEntityId = '%s'".formatted(ENTITE_TACHE),
            "DELETE FROM StrStatus WHERE StrConfigEntityId = '%s'".formatted(ENTITE_TACHE),
            "DELETE FROM StrEntity WHERE PKey IN ('%s', '%s')".formatted(ENTITE_TACHE, ENTITE_RENDEZ_VOUS),
            "DELETE FROM AdmSequence WHERE PKey = '%s'".formatted(PKEY_SEQUENCE));

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_DESCRIPTION = "description";

    private final EntityManager entityManager;
    private final FixtureTachesAdmin tachesAdmin;

    public FixtureTaches(EntityManager entityManager, FixtureTachesAdmin tachesAdmin) {
        this.entityManager = entityManager;
        this.tachesAdmin = tachesAdmin;
    }

    private static List<String> semis() {
        List<String> sql = new ArrayList<>();
        sql.add(entite(ENTITE_TACHE, SEQUENCE, "Tâche"));
        sql.add(entite(ENTITE_RENDEZ_VOUS, "AdmAppointment", "Rendez-vous"));
        sql.add(typeStatut(TYPE_NOUVEAU, "Nouveau", 10));
        sql.add(typeStatut(TYPE_INTERMEDIAIRE, "Intermédiaire", 15));
        sql.add(typeStatut(TYPE_SUCCES, "Succès", 20));
        sql.add(typeStatut(TYPE_ANNULATION, "Annulation", 40));
        STATUTS_TACHE.forEach(s -> sql.add(statut(s)));
        TRANSITIONS.forEach(t -> sql.add(transition(t)));
        sql.add("""
                IF NOT EXISTS (SELECT 1 FROM AdmSequence WHERE Code = N'%s')
                    INSERT INTO AdmSequence (PKey, Code, SeqYear, SeqYearNumber, SeqValue, SeqValueNumber, Separator,
                        BasedOnSystemYear, SeqPrefix, SeqSuffix)
                    VALUES ('%s', N'%s', 2022, 2, %d, 5, N'/', 1, N'', N'')"""
                .formatted(SEQUENCE, PKEY_SEQUENCE, SEQUENCE, VALEUR_SEQUENCE));
        return List.copyOf(sql);
    }

    private static String entite(UUID id, String code, String libelle) {
        return """
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'%s')
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES ('%s', N'%s', N'%s')"""
                .formatted(code, id, code, libelle);
    }

    private static String typeStatut(UUID id, String libelle, int ordre) {
        return """
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = '%s')
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES ('%s', N'%s', N'%s', %d)"""
                .formatted(id, id, libelle, libelle, ordre);
    }

    private static String statut(Statut s) {
        return """
                IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE PKey = '%s')
                    INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, Delay,
                        IsReadOnly, CanDelete, BoolField001, ColorName)
                    VALUES ('%s', N'%s', N'%s', %d, '%s', '%s', 0, 0, 1, 0, N'ffff8000')"""
                .formatted(s.id(), s.id(), s.libelle(), s.libelle(), s.ordre(), ENTITE_TACHE, s.type());
    }

    private static String transition(Transition t) {
        return """
                IF NOT EXISTS (SELECT 1 FROM StrWorkFlow WHERE PKey = '%s')
                    INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                        WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                    VALUES ('%s', '%s', '%s', %d, N'%s', N'%s', %d, 0, 0, 0, '%s')"""
                .formatted(t.id(), t.id(), t.depuis().id(), t.vers().id(), t.ordre(), t.code(), t.description(),
                        t.commentaire() ? 1 : 0, ENTITE_TACHE);
    }

    public static List<Statut> permis(Statut depuis) {
        return TRANSITIONS.stream().filter(t -> t.depuis().equals(depuis)).map(Transition::vers).toList();
    }

    public static Transition transition(Statut depuis, Statut vers) {
        return TRANSITIONS.stream().filter(t -> t.depuis().equals(depuis) && t.vers().equals(vers)).findFirst()
                .orElseThrow();
    }

    /** Tables et paramétrage, dans la transaction du test. */
    public void assurer() {
        tachesAdmin.assurerTables();
        SEMIS.forEach(sql -> entityManager.createNativeQuery(sql).executeUpdate());
    }

    // ------------------------------------------------------------------ lignes

    /** Tâche sans statut ({@code StrStatusId} NULL), écrite par OMRAN ; Description = « Tache {code} ». */
    public UUID tache(String code) {
        return ligne(ENTITE_TACHE, code);
    }

    public UUID tache(String code, Statut statut) {
        UUID id = tache(code);
        definir(id, "StrStatusId", statut.id());
        return id;
    }

    public UUID rendezVous(String code) {
        return ligne(ENTITE_RENDEZ_VOUS, code);
    }

    private UUID ligne(UUID entite, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmTask (PKey, Code, Description, StrEntityId, UserCreate, UserCreateDateTime)
                        VALUES (:id, :code, :description, :entite, :auteur, :le)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Tache " + code)
                .setParameter("entite", entite).setParameter("auteur", FixtureTachesAdmin.AUTEUR_OMRAN)
                .setParameter("le", LocalDateTime.of(2022, 1, 2, 3, 4)));
        return id;
    }

    /** {@code colonne} : un nom de colonne d'AdmTask écrit par le test, jamais une saisie. */
    public void definir(UUID tacheId, String colonne, Object valeur) {
        executer(entityManager.createNativeQuery("UPDATE AdmTask SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter("valeur", valeur).setParameter(P_ID, tacheId));
    }

    public void commentaireObligatoire(Transition t, boolean obligatoire) {
        executer(entityManager.createNativeQuery("UPDATE StrWorkFlow SET WithComment = :valeur WHERE PKey = :id")
                .setParameter("valeur", obligatoire).setParameter(P_ID, t.id()));
    }

    public UUID module(String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO AdmModule (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    public UUID projet(String code, UUID entiteId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (:id, :code, :code, :entite)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("entite", entiteId));
        return id;
    }

    // ------------------------------------------------------------------ lecture en base

    public int lignes(UUID tacheId) {
        return nombre("SELECT COUNT(*) FROM AdmTask WHERE PKey = :id", tacheId);
    }

    public UUID guid(UUID tacheId, String colonne) {
        Object v = valeur("SELECT CONVERT(varchar(36), " + colonne + ") FROM AdmTask WHERE PKey = :id", tacheId);
        return v == null ? null : UUID.fromString(v.toString());
    }

    public String texte(UUID tacheId, String colonne) {
        return Objects.toString(valeur("SELECT " + colonne + " FROM AdmTask WHERE PKey = :id", tacheId), null);
    }

    /** {@code colonne} au format {@code yyyy-MM-ddTHH:mm}, nulle si vide. */
    public String date(UUID tacheId, String colonne) {
        return Objects.toString(valeur(
                "SELECT CONVERT(varchar(16), " + colonne + ", 126) FROM AdmTask WHERE PKey = :id", tacheId), null);
    }

    public int valeurSequence() {
        entityManager.flush();
        Object v = entityManager.createNativeQuery("SELECT SeqValue FROM AdmSequence WHERE Code = :code")
                .setParameter(P_CODE, SEQUENCE).getSingleResult();
        return ((Number) v).intValue();
    }

    public record LigneHistorique(UUID statut, UUID entite, String description, String notes, String auteur) {
    }

    /** Lignes {@code StrHistory} de la tâche pour cette transition. */
    public List<LigneHistorique> historique(UUID tacheId, Transition t) {
        entityManager.flush();
        List<?> lignes = entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), StrStatusId), CONVERT(varchar(36), StrConfigEntityId), Description, Notes,
                            AdmUserDescription
                        FROM StrHistory WHERE ObjectId = :id AND StrWorkflowId = :transition""")
                .setParameter(P_ID, tacheId).setParameter("transition", t.id()).getResultList();
        return lignes.stream().map(Object[].class::cast).map(l -> new LigneHistorique(
                UUID.fromString(l[0].toString()), UUID.fromString(l[1].toString()), (String) l[2], (String) l[3],
                (String) l[4])).toList();
    }

    public int nombreHistorique(UUID tacheId) {
        return nombre("SELECT COUNT(*) FROM StrHistory WHERE ObjectId = :id AND StrWorkflowId IS NOT NULL", tacheId);
    }

    private int nombre(String sql, UUID id) {
        return ((Number) Objects.requireNonNull(valeur(sql, id))).intValue();
    }

    private Object valeur(String sql, UUID id) {
        entityManager.flush();
        return entityManager.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
    }
}
