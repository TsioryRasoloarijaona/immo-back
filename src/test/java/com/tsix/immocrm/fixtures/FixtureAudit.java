package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Lignes {@code AdmWebHistorics} et {@code AdmMessage} écrites comme OMRAN, en SQL natif (aucun écouteur Hibernate
 * ne les voit). FEATURE-patrimoine-audit-actions.md §2.3, §4, §10, §11.8.
 */
@Component
public class FixtureAudit {

    public static final String TYPE_PROGRAMME = "Yes.Cfg.CfgProject";
    public static final String TYPE_TRANCHE = "Yes.Cfg.CfgTranche";
    public static final String TYPE_TRANCHE_ARTICLE = "Yes.Cfg.CfgTrancheStkItem";
    public static final String TYPE_ARTICLE = "Yes.Stk.Building.Item";
    public static final String UTILISATEUR_OMRAN = "Utilisateur OMRAN";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EntityManager entityManager;

    public FixtureAudit(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Ligne d'audit d'un objet sans parent. */
    public UUID ligne(UUID objetId, String type, String libelleType, String action, LocalDateTime date, String details) {
        return ligne(new LigneAudit(objetId, type, libelleType, null, null, action, date, details));
    }

    public UUID ligne(LigneAudit l) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO AdmWebHistorics (PKey, UserDescription, ActionDateTime, ObjectId, Action, ObjectType,
                            ObjectTypeLabel, ObjectLabel, Description, Details, ParentObjectType, ParentObjectId, SystemVersion)
                        VALUES (CAST(:id AS uniqueidentifier), :utilisateur, CAST(:date AS datetime),
                            CAST(:objet AS uniqueidentifier), :action, :type, :libelleType, :libelleObjet, :libelleObjet,
                            CAST(:details AS varchar(max)), NULLIF(:parentType, N''), CAST(NULLIF(:parent, '') AS uniqueidentifier),
                            'Desktop: Version 4.5.57.0')""")
                .setParameter("id", id.toString())
                .setParameter("utilisateur", UTILISATEUR_OMRAN)
                .setParameter("date", sql(l.date()))
                .setParameter("objet", l.objetId().toString())
                .setParameter("action", l.action())
                .setParameter("type", l.type())
                .setParameter("libelleType", l.libelleType())
                .setParameter("libelleObjet", "OBJ-" + id.toString().substring(0, 8))
                .setParameter("details", l.details())
                .setParameter("parentType", l.parentType() == null ? "" : l.parentType())
                .setParameter("parent", l.parentId() == null ? "" : l.parentId().toString())
                .executeUpdate();
        return id;
    }

    /** Note comme OMRAN l'écrit : HTML brut dans {@code Notes}. */
    public UUID note(UUID porteurId, LocalDateTime date, String notesHtml, LocalDateTime rappel) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO AdmMessage (PKey, DocDate, Notes, CategoryId, Description, UserName, IsIn, Origin, ReminderDate)
                        VALUES (CAST(:id AS uniqueidentifier), CAST(:date AS datetime), :notes,
                            CAST(:porteur AS uniqueidentifier), :description, :auteur, NULL, N'', CAST(NULLIF(:rappel, '') AS datetime))""")
                .setParameter("id", id.toString())
                .setParameter("date", sql(date))
                .setParameter("notes", notesHtml)
                .setParameter("porteur", porteurId.toString())
                .setParameter("description", "01/01/2026 10:00 " + UTILISATEUR_OMRAN)
                .setParameter("auteur", UTILISATEUR_OMRAN)
                .setParameter("rappel", sql(rappel))
                .executeUpdate();
        return id;
    }

    /** Notes d'un porteur, relues en base (la réponse du POST n'est pas imposée par le spec). */
    @SuppressWarnings("unchecked")
    public List<Tuple> notesDuPorteur(UUID porteurId) {
        entityManager.flush();
        return entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) AS PKey, DocDate, Notes, CONVERT(varchar(36), CategoryId) AS CategoryId,
                               Description, UserName, IsIn, Origin, ReminderDate
                        FROM AdmMessage WHERE CategoryId = CAST(:porteur AS uniqueidentifier)""", Tuple.class)
                .setParameter("porteur", porteurId.toString())
                .getResultList();
    }

    /** ISO 8601 avec secondes : le seul format que SQL Server convertit en datetime quelle que soit la langue. */
    private static String sql(LocalDateTime date) {
        return date == null ? "" : date.format(ISO);
    }

    public record LigneAudit(UUID objetId, String type, String libelleType, String parentType, UUID parentId,
                             String action, LocalDateTime date, String details) {
    }
}
