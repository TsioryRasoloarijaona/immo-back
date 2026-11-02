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
 * Lignes de {@code AdmWebLockEntities} (FEATURE-admin-fiches-verrouillees.md), écrites en SQL : la table n'a pas d'entité
 * avant le lot et manque à yesRemsTest, d'où sa création ici, colonnes de yesRemsProd, sans FK. Toujours dans une transaction.
 */
@Component
public class FixtureFichesVerrouillees {

    public static final String ENTITE = "Code";
    public static final String LIBELLE = "Description";
    public static final String DATE = "LockDate";
    public static final String SESSION = "SessionId";
    public static final String OBJET = "DataObjectId";
    public static final String ETAT = "State";

    public static final int VERROUILLEE = 1;
    public static final int NON_VERROUILLEE = 2;

    private static final String PKEY = "PKey";
    private static final String ID = "id";
    private static final String SETTLEMENT = "Settlement";

    private static final String TABLE = """
            IF OBJECT_ID('dbo.AdmWebLockEntities', 'U') IS NULL
            CREATE TABLE dbo.AdmWebLockEntities (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(max) NULL, Description nvarchar(max) NULL,
                LockDate datetime NOT NULL, SessionId uniqueidentifier NOT NULL, DataObjectId uniqueidentifier NOT NULL,
                State int NOT NULL)""";

    private final EntityManager entityManager;

    public FixtureFichesVerrouillees(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void assurerTable() {
        entityManager.createNativeQuery(TABLE).executeUpdate();
    }

    /** Verrou actif : le libellé porte le marqueur du test, {@code recherche=marqueur} isole ses lignes. */
    public Ligne verrou(UUID session, String marqueur, LocalDateTime date) {
        return new Ligne()
                .avec(ENTITE, SETTLEMENT)
                .avec(LIBELLE, "Fiche " + marqueur)
                .avec(DATE, date)
                .avec(SESSION, session)
                .avec(OBJET, UUID.randomUUID())
                .avec(ETAT, VERROUILLEE);
    }

    /** PKey en minuscules telles que le fil les rend, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> idsParPKey(String marqueur) {
        return entityManager.createNativeQuery(
                        "SELECT CONVERT(varchar(36), PKey) FROM AdmWebLockEntities WHERE Description = :libelle ORDER BY PKey ASC",
                        String.class)
                .setParameter("libelle", "Fiche " + marqueur)
                .getResultList().stream().map(id -> id.toString().toLowerCase(Locale.ROOT)).toList();
    }

    /** Toutes les colonnes de la ligne, en texte, relues en base. */
    public Map<String, String> ligne(UUID id) {
        entityManager.flush();
        Object[] valeurs = (Object[]) entityManager.createNativeQuery("""
                        SELECT CONVERT(nvarchar(4000), Code), CONVERT(nvarchar(4000), Description),
                            CONVERT(varchar(23), LockDate, 126), CONVERT(varchar(36), SessionId),
                            CONVERT(varchar(36), DataObjectId), CONVERT(varchar(11), State)
                        FROM AdmWebLockEntities WHERE PKey = :id""")
                .setParameter(ID, id)
                .getSingleResult();
        List<String> colonnes = List.of(ENTITE, LIBELLE, DATE, SESSION, OBJET, ETAT);
        Map<String, String> ligne = new LinkedHashMap<>();
        for (int i = 0; i < valeurs.length; i++) {
            ligne.put(colonnes.get(i), String.valueOf(valeurs[i]));
        }
        return ligne;
    }

    /** Verrous actifs de toute la table : seules les lignes de ce lot y vivent. */
    public int actifs() {
        return compter("SELECT COUNT(*) FROM AdmWebLockEntities WHERE State = 1");
    }

    public int lignes() {
        return compter("SELECT COUNT(*) FROM AdmWebLockEntities");
    }

    /** Libère les verrous laissés par un autre test, le temps de la transaction du test. */
    public void aucunVerrouActif() {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE AdmWebLockEntities SET State = 2 WHERE State = 1").executeUpdate();
    }

    private int compter(String sql) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).intValue();
    }

    /** Une ligne à insérer, colonnes NOT NULL comprises. */
    public final class Ligne {

        private final Map<String, Object> colonnes = new LinkedHashMap<>();

        private Ligne() {
            colonnes.put(PKEY, UUID.randomUUID());
        }

        public Ligne avec(String colonne, Object valeur) {
            colonnes.put(colonne, valeur);
            return this;
        }

        public UUID inserer() {
            entityManager.flush();
            String noms = String.join(", ", colonnes.keySet());
            String valeurs = ":" + String.join(", :", colonnes.keySet());
            Query insertion = entityManager.createNativeQuery(
                    "INSERT INTO AdmWebLockEntities (" + noms + ") VALUES (" + valeurs + ")");
            colonnes.forEach(insertion::setParameter);
            insertion.executeUpdate();
            return (UUID) colonnes.get(PKEY);
        }
    }
}
