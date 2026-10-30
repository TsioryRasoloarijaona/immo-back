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
 * Lignes de {@code AdmEMailHistory} (FEATURE-admin-email-historique.md), écrites en SQL : la table n'a pas d'entité
 * avant le lot, et elle est absente de yesRemsTest, d'où sa création ici, en définition de yesRemsProd.
 */
@Component
public class FixtureEmailHistorique {

    public static final String UTILISATEUR = "AdmUserDescription";
    public static final String NOM_EXPEDITEUR = "DisplayFromName";
    public static final String EXPEDITEUR = "EmailFrom";
    public static final String DESTINATAIRES = "EmailsTo";
    public static final String COPIE = "EmailsCc";
    public static final String COPIE_CACHEE = "EmailsBcc";
    public static final String OBJET = "Subject";
    public static final String CORPS = "Body";
    public static final String HTML = "IsBodyHtml";
    public static final String LIBELLE_OBJET = "ObjectLabel";
    public static final String AUTOMATIQUE = "IsAutoMatic";
    public static final String ENVOYE = "EmailsSent";
    public static final String ERREUR = "ErrorDescription";

    public static final String SMTP_HOST = "smtp.interne.test";

    private static final String PKEY = "PKey";

    // Dans la transaction du test : la table ne survit pas en local, le schéma CI du lot reste la seule source.
    private static final String TABLE = """
            IF OBJECT_ID('dbo.AdmEMailHistory', 'U') IS NULL
            CREATE TABLE dbo.AdmEMailHistory (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, MailDate datetime NULL, AdmUserId uniqueidentifier NULL,
                AdmUserDescription nvarchar(255) NULL, DisplayFromName nvarchar(255) NULL, EmailFrom nvarchar(255) NULL,
                EmailsTo nvarchar(1000) NULL, EmailsCc nvarchar(1000) NULL, EmailsBcc nvarchar(1000) NULL,
                Subject nvarchar(1000) NULL, Priority int NULL, IsBodyHtml bit NULL, Body nvarchar(max) NULL,
                SmtpHost nvarchar(255) NULL, SmtpPort int NULL, EnableSsl bit NULL, ObjectId uniqueidentifier NULL,
                ObjectLabel nvarchar(1000) NULL, ObjectStrEntityId uniqueidentifier NULL, SessionId uniqueidentifier NULL,
                IsAutoMatic bit NULL, EmailsSent bit NULL, ErrorDescription nvarchar(max) NULL)""";

    private final EntityManager entityManager;

    public FixtureEmailHistorique(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** À appeler dans la transaction du test. */
    public void assurerTable() {
        entityManager.createNativeQuery(TABLE).executeUpdate();
    }

    /** E-mail envoyé, non automatique, en texte, sans aucune colonne de texte cherchable. */
    public Ligne email(LocalDateTime date) {
        return new Ligne()
                .avec("MailDate", date)
                .avec(HTML, false)
                .avec(AUTOMATIQUE, false)
                .avec(ENVOYE, true)
                .avec("SmtpHost", SMTP_HOST)
                .avec("SmtpPort", 587)
                .avec("EnableSsl", true);
    }

    /** PKey en minuscules telles que le fil les rend, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> idsParPKey(String objet) {
        return entityManager.createNativeQuery(
                        "SELECT CONVERT(varchar(36), PKey) FROM AdmEMailHistory WHERE Subject = :objet ORDER BY PKey ASC",
                        String.class)
                .setParameter("objet", objet)
                .getResultList().stream().map(id -> id.toString().toLowerCase(Locale.ROOT)).toList();
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

        public UUID inserer() {
            String noms = String.join(", ", colonnes.keySet());
            String valeurs = ":" + String.join(", :", colonnes.keySet());
            Query insertion = entityManager.createNativeQuery(
                    "INSERT INTO AdmEMailHistory (" + noms + ") VALUES (" + valeurs + ")");
            colonnes.forEach(insertion::setParameter);
            insertion.executeUpdate();
            return (UUID) colonnes.get(PKEY);
        }
    }
}
