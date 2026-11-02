package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
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
 * Lignes de {@code AdmWebSessions} (FEATURE-admin-sessions.md), écrites en SQL : la table n'a pas d'entité avant le lot,
 * et elle est absente de yesRemsTest, d'où sa création ici, en définition de yesRemsProd. Toujours dans une transaction.
 */
@Component
public class FixtureSessions {

    public static final String WEB_SESSION = "WebSession";
    public static final String COMPTE = "UserId";
    public static final String UTILISATEUR = "UserDesc";
    public static final String DEBUT = "StartDate";
    public static final String FIN = "EndDate";
    public static final String ADRESSE_IP = "IPAddress";
    public static final String ETAT = "State";
    public static final String VERSION = "SystemVersion";
    public static final String ORIGINE = "AdmSessionOriginId";

    public static final int OUVERTE = 2;
    public static final int FERMEE = 4;
    public static final int VERROUILLEE = 8;

    private static final String PKEY = "PKey";
    private static final String ID = "id";

    private static final List<String> TABLES = List.of("""
            IF OBJECT_ID('dbo.AdmSessionOrigin', 'U') IS NULL
            CREATE TABLE dbo.AdmSessionOrigin (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL, Code nvarchar(255) NULL,
                Description nvarchar(255) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""", """
            IF OBJECT_ID('dbo.AdmWebSessions', 'U') IS NULL
            CREATE TABLE dbo.AdmWebSessions (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, WebSession nvarchar(max) NULL, UserId uniqueidentifier NULL,
                UserDesc nvarchar(max) NULL, StartDate datetime NOT NULL, EndDate datetime NULL,
                IPAddress nvarchar(max) NULL, State int NOT NULL, SystemVersion varchar(255) NULL,
                AdmSessionOriginId uniqueidentifier NULL)""");

    private final EntityManager entityManager;

    public FixtureSessions(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void assurerTables() {
        TABLES.forEach(table -> entityManager.createNativeQuery(table).executeUpdate());
    }

    /** Session ouverte du compte, toutes colonnes remplies sauf {@code EndDate} et l'origine. */
    public Ligne session(AdmUser compte, LocalDateTime debut) {
        String marque = UUID.randomUUID().toString();
        return new Ligne()
                .avec(WEB_SESSION, "secret-" + marque)
                .avec(COMPTE, compte.getId())
                .avec(UTILISATEUR, "Utilisateur " + marque)
                .avec(DEBUT, debut)
                .avec(ADRESSE_IP, "poste/" + marque)
                .avec(ETAT, OUVERTE)
                .avec(VERSION, "Desktop: Version 4.5.57.0");
    }

    /** Parent de {@code AdmSessionOriginId} : la base de production porte la FK. */
    public UUID origine() {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO AdmSessionOrigin (PKey) VALUES (:id)")
                .setParameter(ID, id).executeUpdate();
        return id;
    }

    /** PKey en minuscules telles que le fil les rend, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> idsParPKey(String adresseIp) {
        return entityManager.createNativeQuery(
                        "SELECT CONVERT(varchar(36), PKey) FROM AdmWebSessions WHERE IPAddress = :ip ORDER BY PKey ASC",
                        String.class)
                .setParameter("ip", adresseIp)
                .getResultList().stream().map(id -> id.toString().toLowerCase(Locale.ROOT)).toList();
    }

    /** Toutes les colonnes de la ligne, en texte ({@code NULL} = valeur absente de la map), relues en base. */
    public Map<String, String> ligne(UUID id) {
        entityManager.flush();
        Object[] valeurs = (Object[]) entityManager.createNativeQuery("""
                        SELECT CONVERT(nvarchar(4000), WebSession), CONVERT(varchar(36), UserId),
                            CONVERT(nvarchar(4000), UserDesc), CONVERT(varchar(23), StartDate, 126),
                            CONVERT(varchar(23), EndDate, 126), CONVERT(nvarchar(4000), IPAddress),
                            CONVERT(varchar(11), State), SystemVersion, CONVERT(varchar(36), AdmSessionOriginId)
                        FROM AdmWebSessions WHERE PKey = :id""")
                .setParameter(ID, id)
                .getSingleResult();
        List<String> colonnes = List.of(WEB_SESSION, COMPTE, UTILISATEUR, DEBUT, FIN, ADRESSE_IP, ETAT, VERSION, ORIGINE);
        Map<String, String> ligne = new LinkedHashMap<>();
        for (int i = 0; i < valeurs.length; i++) {
            if (valeurs[i] != null) {
                ligne.put(colonnes.get(i), valeurs[i].toString());
            }
        }
        return ligne;
    }

    /** 1 si {@code UserId} pointe un compte réel : garde contre la FK de la CI, absente de yesRemsTest. */
    public int comptesReels(UUID session) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM AdmWebSessions s JOIN AdmUser u ON u.PKey = s.UserId WHERE s.PKey = :id""")
                .setParameter(ID, session)
                .getSingleResult()).intValue();
    }

    /** Une ligne à insérer : seules les colonnes posées partent dans l'INSERT, les autres restent NULL. */
    public final class Ligne {

        private final Map<String, Object> colonnes = new LinkedHashMap<>();

        private Ligne() {
            colonnes.put(PKEY, UUID.randomUUID());
        }

        public Ligne avec(String colonne, Object valeur) {
            colonnes.put(colonne, valeur);
            return this;
        }

        public Ligne sans(String colonne) {
            colonnes.remove(colonne);
            return this;
        }

        public UUID inserer() {
            entityManager.flush();
            String noms = String.join(", ", colonnes.keySet());
            String valeurs = ":" + String.join(", :", colonnes.keySet());
            Query insertion = entityManager.createNativeQuery(
                    "INSERT INTO AdmWebSessions (" + noms + ") VALUES (" + valeurs + ")");
            colonnes.forEach(insertion::setParameter);
            insertion.executeUpdate();
            return (UUID) colonnes.get(PKEY);
        }
    }
}
