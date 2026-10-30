package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;

/**
 * Type et priorité de tâche (FEATURE-admin-type-priorite-tache.md). Tables absentes de yesRemsTest : créées ici si
 * besoin, en définition de yesRemsProd. {@code AdmTask} porte ses deux FK pour que la règle 5 se joue aussi en local.
 */
@Component
public class FixtureTachesAdmin {

    public static final String TYPES_TACHE = "types-tache";
    public static final String PRIORITES_TACHE = "priorites-tache";
    public static final String TABLE_TYPES = "AdmTaskType";
    public static final String TABLE_PRIORITES = "AdmTaskPriority";
    public static final String NOTES = "notes";
    public static final String AUTEUR_OMRAN = "Migration 4.5.56.0";
    public static final int TRI = 7;

    private static final String TRACE = """
                Notes nvarchar(1000) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL""";
    /** Exécutables hors transaction : les deux tables restent alors dans la base, sans gêner aucun autre lot. */
    public static final List<String> TABLES_REFERENTIELS = List.of("""
            IF OBJECT_ID('dbo.AdmTaskType', 'U') IS NULL
            CREATE TABLE dbo.AdmTaskType (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
            """ + TRACE + ")", """
            IF OBJECT_ID('dbo.AdmTaskPriority', 'U') IS NULL
            CREATE TABLE dbo.AdmTaskPriority (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL, Code nvarchar(255) NULL,
                Description nvarchar(255) NULL,
            """ + TRACE + ")");
    // Toujours dans la transaction du test : une AdmTask réduite à trois colonnes ne doit pas survivre (lots 1.2, 1.3).
    private static final String TABLE_TACHES = """
            IF OBJECT_ID('dbo.AdmTask', 'U') IS NULL
            CREATE TABLE dbo.AdmTask (
                PKey uniqueidentifier NOT NULL PRIMARY KEY,
                AdmTaskTypeId uniqueidentifier NULL REFERENCES dbo.AdmTaskType (PKey),
                AdmTaskPriorityId uniqueidentifier NULL REFERENCES dbo.AdmTaskPriority (PKey))""";

    private static final String P_ID = "id";

    private final EntityManager entityManager;

    public FixtureTachesAdmin(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Un des deux référentiels du lot, tel que le fil et la base le nomment. */
    public record TableTache(String slug, String table, boolean avecTri) {

        /** Corps de création complet. */
        public CorpsJson saisie(String code) {
            CorpsJson saisie = corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code).avec(NOTES, "Notes " + code);
            return avecTri ? saisie.avec(SORTING, TRI) : saisie;
        }

        @Override
        public String toString() {
            return slug;
        }
    }

    public record TraceLigne(String creePar, Object creeLe, String modifiePar, Object modifieLe) {
    }

    public static Stream<TableTache> tables() {
        return Stream.of(new TableTache(TYPES_TACHE, TABLE_TYPES, false),
                new TableTache(PRIORITES_TACHE, TABLE_PRIORITES, true));
    }

    /** À appeler dans la transaction du test. */
    public void assurerTables() {
        TABLES_REFERENTIELS.forEach(ddl -> entityManager.createNativeQuery(ddl).executeUpdate());
        entityManager.createNativeQuery(TABLE_TACHES).executeUpdate();
    }

    /** Ligne déjà en base, écrite par OMRAN : créée, jamais modifiée. */
    public UUID ligne(TableTache t, String code) {
        UUID id = UUID.randomUUID();
        String tri = t.avecTri() ? "Sorting, " : "";
        String valeurTri = t.avecTri() ? TRI + ", " : "";
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO " + t.table() + " (PKey, " + tri
                        + "Code, Description, Notes, UserCreate, UserCreateDateTime) VALUES (:id, " + valeurTri
                        + ":code, :description, :notes, :auteur, :le)")
                .setParameter(P_ID, id).setParameter(CODE, code).setParameter(DESCRIPTION, "Libelle " + code)
                .setParameter(NOTES, "Notes " + code).setParameter("auteur", AUTEUR_OMRAN)
                .setParameter("le", LocalDateTime.of(2020, 1, 2, 3, 4))
                .executeUpdate();
        return id;
    }

    /** Tâche qui référence {@code id} par la colonne {@code AdmTaskTypeId} ou {@code AdmTaskPriorityId}. */
    public void tacheReferencant(TableTache t, UUID id) {
        entityManager.createNativeQuery("INSERT INTO AdmTask (PKey, " + t.table() + "Id) VALUES (:tache, :id)")
                .setParameter("tache", UUID.randomUUID()).setParameter(P_ID, id).executeUpdate();
    }

    public TraceLigne trace(TableTache t, UUID id) {
        entityManager.flush();
        Object[] l = (Object[]) entityManager.createNativeQuery("SELECT UserCreate, UserCreateDateTime, UserUpdate, "
                + "UserUpdateDateTime FROM " + t.table() + " WHERE PKey = :id").setParameter(P_ID, id).getSingleResult();
        return new TraceLigne((String) l[0], l[1], (String) l[2], l[3]);
    }
}
