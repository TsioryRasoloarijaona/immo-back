package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_RENDEZ_VOUS;

/**
 * Rendez-vous, FEATURE-admin-rendez-vous.md : lignes {@code AdmTask} de l'entité {@code AdmAppointment}, en SQL natif ;
 * paramétrage de {@link FixtureTaches}, routes et champs = contrat du fil.
 */
@Component
public class FixtureRendezVous {

    public static final String RACINE = "/api/administration/rendez-vous";
    public static final String FICHE = RACINE + "/{id}";
    public static final String CALENDRIER = RACINE + "/calendrier";
    public static final String EXPORT = RACINE + "/export";

    public static final String TIER_ID = "tierId";
    public static final String DEBUT = "debut";
    public static final String FIN = "fin";

    public static final String TYPE_AUDIT = "Yes.Adm.AdmAppointment";
    public static final String LIBELLE_AUDIT = "Rendez-vous";

    /** Les 14 colonnes de la liste OMRAN, dans l'ordre du spec. */
    public static final List<String> ENTETES = List.of("Code", "Description", "Déclencheur Code", "Déclencheur",
            "Date Prévisionnelle", "Durée Prévisionnelle", "Date fin Prévisionnelle", "Personnel Code", "Personnel",
            "Tier Code", "Tier", "Source", "Entité fonctionnelle de la source", "Notes");

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_LIBELLE = "libelle";

    private final EntityManager entityManager;
    private final FixtureTaches taches;

    public FixtureRendezVous(EntityManager entityManager, FixtureTaches taches) {
        this.entityManager = entityManager;
        this.taches = taches;
    }

    /** Écrite par OMRAN : fin une heure après {@code debut}, durée 0, Description = « Tache {code} ». */
    public UUID rendezVous(String code, LocalDateTime debut) {
        UUID id = taches.rendezVous(code);
        taches.definir(id, "StartDatePrev", debut);
        taches.definir(id, "DueDatePrev", debut.plusHours(1));
        taches.definir(id, "DelayPrev", 0);
        return id;
    }

    public UUID tier(String code, String nom) {
        UUID id = UUID.randomUUID();
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO CfgTier (PKey, Code, Description) VALUES (:id, :code, :libelle)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_LIBELLE, nom).executeUpdate();
        return id;
    }

    /** {@code StrEntity} propre au test, pour la colonne {@code ObjectStrEntityId}. */
    public UUID entite(String code, String libelle) {
        UUID id = UUID.randomUUID();
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :libelle)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_LIBELLE, libelle).executeUpdate();
        return id;
    }

    /** {@code nombre} rendez-vous de même Description, à la même date. */
    public void enMasse(int nombre, LocalDateTime debut, String description) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO AdmTask (PKey, Code, Description, StrEntityId, StartDatePrev, DueDatePrev)
                        SELECT TOP (:nombre) NEWID(), :description, :description, :entite, :debut, :debut
                        FROM sys.all_objects a CROSS JOIN sys.all_objects b""")
                .setParameter("nombre", nombre).setParameter("description", description)
                .setParameter("entite", ENTITE_RENDEZ_VOUS).setParameter(DEBUT, debut).executeUpdate();
    }
}
