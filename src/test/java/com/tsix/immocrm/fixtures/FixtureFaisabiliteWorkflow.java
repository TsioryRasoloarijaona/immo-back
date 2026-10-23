package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ENTITE_ETUDE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.FICHE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Études de faisabilité, lot P8b-2 (FEATURE-patrimoine-faisabilite-workflow.md §4-§7) : routes et noms du fil des transitions,
 * de l'historique, des pièces jointes, de l'export et de la configuration ; statuts, transitions, rôles, documents et pièces
 * de test en SQL natif (les tests ne dépendent pas des entités que le lot crée). Jamais d'UUID aléatoire dans une FK.
 */
@Component
public class FixtureFaisabiliteWorkflow {

    // ------------------------------------------------------------------ routes (§4.2, §4.3, §5, §7)
    public static final String TRANSITIONS = FICHE + "/transitions";
    public static final String HISTORIQUE = FICHE + "/historique";
    public static final String PIECES = FICHE + "/pieces-jointes";
    public static final String PIECE = PIECES + "/{pieceId}";
    public static final String EXPORT = FICHE + "/export.xlsx";
    public static final String CONFIG = "/api/config-faisabilite";
    public static final String CONFIG_STATUTS = CONFIG + "/statuts";
    public static final String CONFIG_STATUT = CONFIG_STATUTS + "/{id}";
    public static final String CONFIG_TRANSITIONS = CONFIG + "/transitions";
    public static final String CONFIG_TRANSITION = CONFIG_TRANSITIONS + "/{id}";
    public static final String CHAMPS_OBLIGATOIRES = CONFIG + "/champs-obligatoires";

    // ------------------------------------------------------------------ champs du fil
    public static final String TRANSITION_ID = "transitionId";
    public static final String STATUT_DEPART_ID = "statutDepartId";
    public static final String STATUT_ARRIVEE_ID = "statutArriveeId";
    public static final String CHAMPS = "champsObligatoires";
    public static final String DOCUMENTS_REQUIS = "documentsRequis";
    public static final String POSSIBLE = "possible";
    public static final String CHAMP = "champ";
    public static final String LIBELLE = "libelle";
    public static final String RENSEIGNE = "renseigne";
    public static final String TYPE_ID = "typeId";
    public static final String FOURNI = "fourni";
    public static final String NOM = "nom";
    public static final String STATUT_CODE = "statutCode";
    public static final String STATUT_LIBELLE = "statutLibelle";
    public static final String ORDRE = "ordre";
    public static final String UTILISATEUR = "utilisateur";
    public static final String TYPE_ETAT_ID = "typeEtatId";
    public static final String DELAI = "delai";
    public static final String COULEUR = "couleur";
    public static final String NOTES = "notes";
    public static final String DOCUMENTS = "documents";
    public static final String TYPE_DOCUMENT_ID = "typeDocumentId";
    public static final String EST_OBLIGATOIRE = "estObligatoire";
    public static final String AVEC_TITRE = "avecTitre";
    public static final String NOTE_OBLIGATOIRE = "noteObligatoire";
    public static final String AVEC_NOTIFICATION = "avecNotification";
    public static final String ENVOYER_EMAIL = "envoyerEmail";
    public static final String ROLES = "roles";
    public static final String TRANSITIONS_AUTORISEES = "$.transitionsAutorisees";

    // ------------------------------------------------------------------ messages (§4.1, §5)
    public static final String PREFIXE_TRANSITION = "Transition :";
    public static final String PREFIXE_STATUTS = "Statuts :";
    public static final String MSG_DEPART_ARRIVEE = "Le statut de départ et le statut d'arrivé doivent être différents!";
    public static final String CATEGORIE_ETUDE = "Yes.Stk.StkFeasibilityStudy";
    public static final String ROLE_ADMINISTRATEUR = "Administrateur";
    public static final String ROLE_DG = "Direction Générale";

    public static String msgChamp(String libelle) {
        return "Le champ « " + libelle + " » est obligatoire";
    }

    public static String msgDocument(String typeLibelle) {
        return "Le document de type : " + typeLibelle + " est obligatoire";
    }

    // ------------------------------------------------------------------ PKey de production (§4.1, yesRemsProd)
    public static final UUID ETAT_INTERMEDIAIRE = UUID.fromString("B53E0A4F-3D67-4BF9-8C0E-B24155865FD6");
    public static final UUID ETAT_ECHEC = UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97");
    private static final UUID ROLE_ADMINISTRATEUR_ID = UUID.fromString("D845130C-7245-420D-9490-643589081333");
    private static final UUID ROLE_DG_ID = UUID.fromString("7AA1E483-4872-47A9-BE9E-C564F03EC358");

    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    private final EntityManager entityManager;

    public FixtureFaisabiliteWorkflow(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Connexion au nom d'un compte réel avec un seul rôle (nom du rôle = {@code AdmRole.Code}). */
    public static RequestPostProcessor avecRole(AdmUser compte, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), compte.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    // ------------------------------------------------------------------ paramétrage de production

    /** Types d'état « Intermédiaire » et « Terminé avec échec » (yesRemsTest n'a que « Nouveau »). */
    public void typesEtat() {
        typeEtat(ETAT_INTERMEDIAIRE, "Intermédiaire");
        typeEtat(ETAT_ECHEC, "Términé avec échec");
    }

    private void typeEtat(UUID id, String code) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmWorkFlowStateType WHERE PKey = :id)
                            INSERT INTO AdmWorkFlowStateType (PKey, Code, Description) VALUES (:id, :code, :code)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
    }

    public UUID roleAdministrateur() {
        return roleDeProduction(ROLE_ADMINISTRATEUR_ID, ROLE_ADMINISTRATEUR);
    }

    /** « Direction Générale » est absente de yesRemsTest : créée à sa PKey de production si aucun rôle ne porte ce code. */
    public UUID roleDirectionGenerale() {
        return roleDeProduction(ROLE_DG_ID, ROLE_DG);
    }

    private UUID roleDeProduction(UUID id, String code) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmRole WHERE Code = :code) AND NOT EXISTS (SELECT 1 FROM AdmRole WHERE PKey = :id)
                            INSERT INTO AdmRole (PKey, Code, Description) VALUES (:id, :code, :code)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        Object v = entityManager.createNativeQuery(
                        "SELECT TOP 1 CONVERT(varchar(36), PKey) FROM AdmRole WHERE Code = :code ORDER BY PKey ASC")
                .setParameter(P_CODE, code).getSingleResult();
        return UUID.fromString(v.toString());
    }

    /** Rôle OMRAN de test (non lecteur de l'étude). */
    public UUID role(String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO AdmRole (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, code));
        return id;
    }

    // ------------------------------------------------------------------ statuts, transitions, documents

    /** Statut de test (Code = Description) ; {@code champs} au format OMRAN ({@code ",Code,Area"} ou {@code ""}). */
    public UUID statut(String code, int ordre, UUID typeEtat, String champs) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkFeasibilityStudyStatus (PKey, Code, Description, Sorting, Delay, MandatoryFields, Notes,
                            ColorName, AdmWorkFlowStateTypeId)
                        VALUES (:id, :code, :code, :ordre, 0, :champs, N'', N'', :type)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("ordre", ordre)
                .setParameter("champs", champs).setParameter("type", typeEtat));
        return id;
    }

    public UUID statut(String code, int ordre) {
        return statut(code, ordre, ETAT_INTERMEDIAIRE, "");
    }

    /** Transition de test de l'entité étude ; {@code roles} = PKey AdmRole autorisées (peut être vide). */
    public UUID transition(UUID depart, UUID arrivee, String description, String champs, UUID... roles) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkFeasibilityStudyWorkFlow (PKey, Sorting, StartStatusId, EndStatusId, MandatoryFields,
                            NotesMandatory, StrEntityId, WithNotification, Description, SendEmailToUsers)
                        VALUES (:id, 1, :depart, :arrivee, :champs, 0, :entite, 0, :description, 0)""")
                .setParameter(P_ID, id).setParameter("depart", depart).setParameter("arrivee", arrivee)
                .setParameter("champs", champs).setParameter("entite", ENTITE_ETUDE).setParameter("description", description));
        for (UUID role : roles) {
            roleDeTransition(id, role);
        }
        return id;
    }

    public void roleDeTransition(UUID transitionId, UUID roleId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkFeasibilityStudyWorkFlowAdmRole (PKey, WorkFlowId, AdmRoleId)
                        VALUES (:id, :transition, :role)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("transition", transitionId).setParameter("role", roleId));
    }

    /** Document à fournir pour la transition ; {@code obligatoire} = IsMandatory (non lu par OMRAN). */
    public void documentDeTransition(UUID transitionId, UUID typeId, boolean obligatoire) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkFeasibilityStudyWorkFlowDocumentToAttach (PKey, StkFeasibilityStudyWorkFlowId, IsMandatory,
                            WithTitle, AdmAttachedFileTypeId)
                        VALUES (:id, :transition, :obligatoire, 0, :type)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("transition", transitionId)
                .setParameter("obligatoire", obligatoire).setParameter("type", typeId));
    }

    /** Type de document (AdmAttachedFileType), Code = Description = {@code libelle}. */
    public UUID typeDocument(String libelle) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO AdmAttachedFileType (PKey, Code, Description) VALUES (:id, :libelle, :libelle)")
                .setParameter(P_ID, id).setParameter("libelle", libelle));
        return id;
    }

    /** Pièce jointe de l'étude, avec son fichier (AppFileObject : HasFile est calculé dessus). */
    public UUID pieceJointe(UUID etudeId, UUID typeId) {
        UUID piece = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, Description, AdmAttachedFileTypeId)
                        VALUES (:id, :categorie, :etude, N'Piece P8b-2', :type)""")
                .setParameter(P_ID, piece).setParameter("categorie", CATEGORIE_ETUDE).setParameter("etude", etudeId)
                .setParameter("type", typeId));
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AppFileObject (AdmAttachedFileId, StorageKey, SizeBytes, UploadedAt)
                        VALUES (:id, :cle, 1, :maintenant)""")
                .setParameter(P_ID, piece).setParameter("cle", "test/p8b2/" + piece)
                .setParameter("maintenant", LocalDateTime.now()));
        return piece;
    }

    // ------------------------------------------------------------------ étude

    /** Pose le statut courant de l'étude sans passer par une transition. */
    public void placer(UUID etudeId, UUID statutId) {
        definir(etudeId, "StkFeasibilityStudyStatusId", statutId);
    }

    /** {@code colonne} : un nom de colonne de StkFeasibilityStudy écrit par le test, jamais une saisie. */
    public void definir(UUID etudeId, String colonne, Object valeur) {
        executer(entityManager.createNativeQuery("UPDATE StkFeasibilityStudy SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter("valeur", valeur).setParameter(P_ID, etudeId));
    }

    /** Ligne d'historique de statut (sans utilisateur). */
    public void historique(UUID etudeId, UUID statutId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkFeasibilityStudyStatusHistory (PKey, DocDate, StkFeasibilityStudyStatusId,
                            StkFeasibilityStudyId, AdmUserId)
                        VALUES (:id, :date, :statut, :etude, NULL)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("date", LocalDateTime.of(2026, 10, 1, 9, 0))
                .setParameter("statut", statutId).setParameter("etude", etudeId));
    }

    /** Lu en base sans cache : {@code StkFeasibilityStudyStatusId} courant, en majuscules. */
    public String statutCourant(UUID etudeId) {
        entityManager.flush();
        Object v = entityManager.createNativeQuery(
                        "SELECT CONVERT(varchar(36), StkFeasibilityStudyStatusId) FROM StkFeasibilityStudy WHERE PKey = :id")
                .setParameter(P_ID, etudeId).getSingleResult();
        return v == null ? null : v.toString().toUpperCase(java.util.Locale.ROOT);
    }

    /** Flush, exécution, puis vidage du contexte : l'étude relue par l'appel HTTP suivant voit la valeur écrite. */
    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
