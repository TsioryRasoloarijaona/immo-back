package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lignes de {@code AdmActionsView} (FEATURE-admin-actions.md). En production c'est une vue sur 32 tables, absente de
 * yesRemsTest : une <b>table</b> de mêmes colonnes, sans contrainte, la double ici. Toujours dans une transaction.
 */
@Component
public class FixtureActions {

    public static final String PKEY = "PKey";
    public static final String CODE = "Code";
    public static final String DESCRIPTION = "Description";
    public static final String ACTION = "ActionLabel";
    public static final String CREATEUR = "UserCreate";
    public static final String CREE_LE = "UserCreateDateTime";
    public static final String MODIFICATEUR = "UserUpdate";
    public static final String MODIFIE_LE = "UserUpdateDateTime";

    /** Sans clé primaire : la vue rend plusieurs lignes de même {@code PKey}. */
    private static final String DOUBLURE = """
            IF OBJECT_ID('dbo.AdmActionsView') IS NULL
            CREATE TABLE dbo.AdmActionsView (
                PKey uniqueidentifier NOT NULL, Code nvarchar(255) NULL, Description nvarchar(max) NULL,
                ActionLabel nvarchar(261) NULL, UserCreateDateTime datetime NULL, UserCreate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL, UserUpdate nvarchar(255) NULL)""";

    private final EntityManager entityManager;

    public FixtureActions(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void assurerVue() {
        entityManager.createNativeQuery(DOUBLURE).executeUpdate();
    }

    /** Ligne dont aucune colonne vide : {@code marque} est dans le {@code Code} seul, {@code recherche=marque} l'isole. */
    public Ligne ligne(String marque, LocalDateTime creeLe) {
        return new Ligne()
                .avec(CODE, "ACT-" + marque)
                .avec(DESCRIPTION, "Appel client")
                .avec(CREATEUR, "Karim")
                .avec(CREE_LE, creeLe)
                .avec(MODIFICATEUR, "Salma")
                .avec(MODIFIE_LE, creeLe.plusDays(1));
    }

    /** Ligne réduite à sa clé et à son {@code ActionLabel}, jamais vide en production. */
    public Ligne ligneVide() {
        return new Ligne();
    }

    /** {@code PKey} en minuscules des lignes marquées, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> pkeysTriees(String marque) {
        entityManager.flush();
        return entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) FROM AdmActionsView
                        WHERE CHARINDEX(:marque, Code) > 0 ORDER BY PKey ASC""", String.class)
                .setParameter("marque", marque)
                .getResultList().stream().map(pkey -> pkey.toString().toLowerCase(Locale.ROOT)).toList();
    }

    /** Une ligne à insérer : seules les colonnes posées partent dans l'INSERT, les autres restent NULL. */
    public final class Ligne {

        private final Map<String, Object> colonnes = new LinkedHashMap<>();

        private Ligne() {
            colonnes.put(PKEY, UUID.randomUUID());
            colonnes.put(ACTION, "ACTION");
        }

        public Ligne avec(String colonne, Object valeur) {
            colonnes.put(colonne, valeur);
            return this;
        }

        public Ligne sans(String colonne) {
            colonnes.remove(colonne);
            return this;
        }

        /** Rend l'{@code id} du fil, {@code <PKey>:<ActionLabel>}, en minuscules : le spec ne fixe pas la casse. */
        public String inserer() {
            entityManager.flush();
            String noms = String.join(", ", colonnes.keySet());
            String valeurs = ":" + String.join(", :", colonnes.keySet());
            Query insertion = entityManager.createNativeQuery(
                    "INSERT INTO AdmActionsView (" + noms + ") VALUES (" + valeurs + ")");
            colonnes.forEach(insertion::setParameter);
            insertion.executeUpdate();
            return (colonnes.get(PKEY) + ":" + colonnes.get(ACTION)).toLowerCase(Locale.ROOT);
        }
    }
}
