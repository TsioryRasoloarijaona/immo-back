package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Lignes {@code AdmWebHistorics} en SQL natif, annulées avec la transaction du test. FEATURE-admin-historique.md. */
@Component
public class FixtureHistorique {

    public static final String UTILISATEUR = "Utilisateur Historique";
    public static final String AJOUT = "Added";
    public static final String MODIFICATION = "Modified";
    public static final String SUPPRESSION = "Deleted";
    public static final String TYPE_NEUTRE = "Yes.Test.Neutre";
    public static final String LIBELLE_NEUTRE = "Neutre";
    private static final String LIBELLE_TYPE = "Libellé type";

    private final EntityManager entityManager;

    public FixtureHistorique(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String jeton() {
        return "HIST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public UUID ligne(Ligne l) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO AdmWebHistorics (PKey, UserDescription, ActionDateTime, ObjectId, Action, ObjectType,
                            ObjectTypeLabel, ObjectLabel, Description, Details)
                        VALUES (CAST(:id AS uniqueidentifier), :utilisateur, :date, CAST(:objet AS uniqueidentifier),
                            :action, :type, :libelleType, :libelle, :description, CAST(:details AS varchar(max)))""")
                .setParameter("id", id.toString())
                .setParameter("utilisateur", l.utilisateur())
                .setParameter("date", l.date())
                .setParameter("objet", l.objetId().toString())
                .setParameter("action", l.action())
                .setParameter("type", l.type())
                .setParameter("libelleType", l.libelleType())
                .setParameter("libelle", l.libelle())
                .setParameter("description", l.description())
                .setParameter("details", l.details())
                .executeUpdate();
        return id;
    }

    public void sansUtilisateur(UUID id) {
        entityManager.createNativeQuery(
                        "UPDATE AdmWebHistorics SET UserDescription = NULL WHERE PKey = CAST(:id AS uniqueidentifier)")
                .setParameter("id", id.toString())
                .executeUpdate();
    }

    /** Insertion ensembliste : assez de lignes pour dépasser le plafond d'export sans une requête par ligne. */
    public void enMasse(int nombre, LocalDateTime date, String libelle) {
        entityManager.createNativeQuery("""
                        INSERT INTO AdmWebHistorics (PKey, UserDescription, ActionDateTime, ObjectId, Action, ObjectType,
                            ObjectTypeLabel, ObjectLabel)
                        SELECT TOP (:nombre) NEWID(), :utilisateur, :date, NEWID(), :action, :type, :libelleType, :libelle
                        FROM sys.all_objects a CROSS JOIN sys.all_objects b""")
                .setParameter("nombre", nombre)
                .setParameter("utilisateur", UTILISATEUR)
                .setParameter("date", date)
                .setParameter("action", AJOUT)
                .setParameter("type", TYPE_NEUTRE)
                .setParameter("libelleType", LIBELLE_TYPE)
                .setParameter("libelle", libelle)
                .executeUpdate();
    }

    /** Ordre de SQL Server sur {@code PKey}, qui n'est pas l'ordre alphabétique du texte de l'UUID. */
    @SuppressWarnings("unchecked")
    public List<String> idsParPKey(String type) {
        List<String> ids = entityManager.createNativeQuery("""
                        SELECT CONVERT(varchar(36), PKey) FROM AdmWebHistorics WHERE ObjectType = :type ORDER BY PKey ASC""")
                .setParameter("type", type)
                .getResultList();
        return ids.stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
    }

    public record Ligne(UUID objetId, String utilisateur, LocalDateTime date, String action, String type,
                        String libelleType, String libelle, String description, String details) {

        /** Ligne que {@code texte=<jeton>} retrouve par {@code ObjectType} et {@code ObjectLabel}. */
        public static Ligne de(LocalDateTime date, String jeton) {
            return new Ligne(UUID.randomUUID(), UTILISATEUR, date, AJOUT, "Yes.Test." + jeton, LIBELLE_TYPE,
                    "Objet " + jeton, "Description neutre", "Détails neutres");
        }

        public Ligne avecUtilisateur(String valeur) {
            return new Ligne(objetId, valeur, date, action, type, libelleType, libelle, description, details);
        }

        public Ligne avecAction(String valeur) {
            return new Ligne(objetId, utilisateur, date, valeur, type, libelleType, libelle, description, details);
        }

        public Ligne avecType(String valeur) {
            return new Ligne(objetId, utilisateur, date, action, valeur, libelleType, libelle, description, details);
        }

        public Ligne avecLibelleType(String valeur) {
            return new Ligne(objetId, utilisateur, date, action, type, valeur, libelle, description, details);
        }

        public Ligne avecLibelle(String valeur) {
            return new Ligne(objetId, utilisateur, date, action, type, libelleType, valeur, description, details);
        }

        public Ligne avecDescription(String valeur) {
            return new Ligne(objetId, utilisateur, date, action, type, libelleType, libelle, valeur, details);
        }

        public Ligne avecDetails(String valeur) {
            return new Ligne(objetId, utilisateur, date, action, type, libelleType, libelle, description, valeur);
        }
    }
}
