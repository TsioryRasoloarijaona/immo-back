package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Statuts, transitions, rôles et passages d'historique de pièces d'achat (FEATURE-achat-regle-commune.md §4) : chaque ligne
 * suffixée d'un jeton propre, parents insérés d'abord (FK de la CI), annulés avec la transaction du test.
 */
@Component
public class FixtureConfigurationAchat {

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_DESCRIPTION = "description";
    private static final String TABLE_ROLES = """
            IF OBJECT_ID('dbo.BuyDocumentWorkFlowAdmRole', 'U') IS NULL
            CREATE TABLE dbo.BuyDocumentWorkFlowAdmRole (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, WorkFlowId uniqueidentifier NULL, AdmRoleId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL)""";

    private final EntityManager em;

    public FixtureConfigurationAchat(EntityManager em) {
        this.em = em;
    }

    /** Statut ; {@code typeEtat} et les booléens peuvent être nuls (colonnes nullables en prod). */
    public record Statut(String code, String description, Integer ordre, UUID typeEtat, Boolean okPaiement,
                         Boolean valideCompta, Boolean lectureSeule, String couleur, Integer delai) {

        public static Statut simple(String code, int ordre) {
            return new Statut(code, "Statut " + code, ordre, null, null, null, null, null, null);
        }
    }

    public static String jeton(String prefixe) {
        return prefixe + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /** Catégorie d'état ({@code AdmWorkFlowStateType}) : Code = Description, comme en prod. */
    public UUID typeEtat(String libelle) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO AdmWorkFlowStateType (PKey, Code, Description, Sorting) VALUES (:id, :code, :code, 1)""")
                .setParameter(P_ID, id).setParameter(P_CODE, libelle));
        return id;
    }

    public UUID statut(Statut s) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting, AdmWorkFlowStateTypeId, IsOkForPayement,
                            IsValidatedOnAccounting, IsReadOnly, ColorName, Delay)
                        VALUES (:id, :code, :description, :ordre, :type, :ok, :compta, :lecture, :couleur, :delai)""")
                .setParameter(P_ID, id).setParameter(P_CODE, s.code()).setParameter(P_DESCRIPTION, s.description())
                .setParameter("ordre", s.ordre()).setParameter("type", s.typeEtat()).setParameter("ok", s.okPaiement())
                .setParameter("compta", s.valideCompta()).setParameter("lecture", s.lectureSeule())
                .setParameter("couleur", s.couleur()).setParameter("delai", s.delai()));
        return id;
    }

    /** Gestion ({@code StrEntity}). */
    public UUID gestion(String code, String description) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :description)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, description));
        return id;
    }

    /** Transition ; {@code gestion} nulle = « Sans gestion ». */
    public UUID transition(UUID gestion, Integer ordre, String description, UUID depart, UUID arrivee,
                           Boolean noteObligatoire, Boolean declencheur) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentWorkFlow (PKey, Sorting, StartStatusId, EndStatusId, StrEntityId, Description,
                            NotesMandatory, IsValidatedByCfgTierTrigger)
                        VALUES (:id, :ordre, :depart, :arrivee, :gestion, :description, :note, :declencheur)""")
                .setParameter(P_ID, id).setParameter("ordre", ordre).setParameter("depart", depart)
                .setParameter("arrivee", arrivee).setParameter("gestion", gestion).setParameter(P_DESCRIPTION, description)
                .setParameter("note", noteObligatoire).setParameter("declencheur", declencheur));
        return id;
    }

    public UUID transition(UUID gestion, int ordre, String description, UUID depart, UUID arrivee) {
        return transition(gestion, ordre, description, depart, arrivee, false, false);
    }

    public UUID role(String code) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO AdmRole (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    /** Rôle autorisé sur la transition ({@code BuyDocumentWorkFlowAdmRole}, créée si absente). */
    public void lier(UUID transition, UUID role) {
        executer(em.createNativeQuery(TABLE_ROLES));
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentWorkFlowAdmRole (PKey, WorkFlowId, AdmRoleId) VALUES (:id, :transition, :role)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("transition", transition).setParameter("role", role));
    }

    /** Pièce minimale portant ce statut, dans cette gestion (nulle possible). */
    public UUID piece(UUID statut, UUID gestion) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocument (PKey, Code, DocType, BuyDocumentStatusId, StrEntityId)
                        VALUES (:id, :code, N'INVOICE', :statut, :gestion)""")
                .setParameter(P_ID, id).setParameter(P_CODE, "PA-" + id.toString().substring(0, 8))
                .setParameter("statut", statut).setParameter("gestion", gestion));
        return id;
    }

    /** Ligne d'historique de la pièce ; {@code transition} nulle = ligne de création. */
    public void passage(UUID piece, UUID transition, UUID statut, LocalDateTime quand) {
        executer(em.createNativeQuery("""
                        INSERT INTO BuyDocumentStatusHistory (PKey, BuyDocumentId, BuyDocumentStatusId, DocDate, Notes,
                            buyDocumentWorkFlowId)
                        VALUES (:id, :piece, :statut, :quand, N'', :transition)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("piece", piece).setParameter("statut", statut)
                .setParameter("quand", quand).setParameter("transition", transition));
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
