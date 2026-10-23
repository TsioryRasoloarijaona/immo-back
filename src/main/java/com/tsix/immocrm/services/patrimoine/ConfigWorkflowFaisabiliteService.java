package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ChampsObligatoiresFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.DocumentAJoindreDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.StatutEtudeConfigDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionEtudeConfigDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.stk.DocumentAJoindreOmran;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatus;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatusDocumentToAttach;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlow;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlowAdmRole;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlowDocumentToAttach;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/**
 * Configuration des statuts et transitions d'étude (FEATURE-patrimoine-faisabilite-workflow.md §5) : tout refus avant la
 * première écriture ; sous-listes (rôles, documents) remplacées ligne à ligne pour l'audit.
 */
@Service
@Transactional(readOnly = true)
public class ConfigWorkflowFaisabiliteService {

    private static final String ID = "id";
    private static final String STATUT = "Statut d'étude";
    private static final String TRANSITION = "Transition d'étude";
    private static final String MSG_DEPART_ARRIVEE = "Le statut de départ et le statut d'arrivé doivent être différents!";
    private static final String DOCS_STATUT =
            "SELECT d FROM StkFeasibilityStudyStatusDocumentToAttach d WHERE d.statusId = :id";
    private static final String DOCS_TRANSITION =
            "SELECT d FROM StkFeasibilityStudyWorkFlowDocumentToAttach d WHERE d.workFlowId = :id";
    private static final String ROLES = "SELECT r FROM StkFeasibilityStudyWorkFlowAdmRole r WHERE r.workFlowId = :id";

    private final EntityManager em;
    private final TracePatrimoine trace;

    public ConfigWorkflowFaisabiliteService(EntityManager em, TracePatrimoine trace) {
        this.em = em;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ statuts

    public List<StatutEtudeConfigDto> listerStatuts() {
        Map<UUID, String> types = libellesTypesEtat();
        return em.createQuery("SELECT s FROM StkFeasibilityStudyStatus s ORDER BY s.sorting, s.code",
                StkFeasibilityStudyStatus.class).getResultList().stream().map(s -> dto(s, types)).toList();
    }

    public StatutEtudeConfigDto statut(UUID id) {
        return dto(exiger(StkFeasibilityStudyStatus.class, id, STATUT), libellesTypesEtat());
    }

    @Transactional
    public StatutEtudeConfigDto creerStatut(StatutEtudeConfigDto d) {
        StkFeasibilityStudyStatus s = new StkFeasibilityStudyStatus();
        s.setId(UUID.randomUUID());
        appliquer(s, d);
        tracer(s, true);
        em.persist(s);
        remplacerDocuments(s.getId(), d.documents(), DOCS_STATUT, StkFeasibilityStudyStatusDocumentToAttach::new);
        em.flush();
        return statut(s.getId());
    }

    @Transactional
    public StatutEtudeConfigDto modifierStatut(UUID id, StatutEtudeConfigDto d) {
        StkFeasibilityStudyStatus s = exiger(StkFeasibilityStudyStatus.class, id, STATUT);
        appliquer(s, d);
        tracer(s, false);
        remplacerDocuments(id, d.documents(), DOCS_STATUT, StkFeasibilityStudyStatusDocumentToAttach::new);
        em.flush();
        return statut(id);
    }

    /** FK NO ACTION (étude, transition) et CASCADE (historique) : refus explicites, jamais une cascade silencieuse. */
    @Transactional
    public void supprimerStatut(UUID id) {
        StkFeasibilityStudyStatus s = exiger(StkFeasibilityStudyStatus.class, id, STATUT);
        long usages = compter("SELECT COUNT(e) FROM StkFeasibilityStudy e WHERE e.status.id = :id", id)
                + compter("SELECT COUNT(w) FROM StkFeasibilityStudyWorkFlow w WHERE w.startStatusId = :id OR w.endStatusId = :id", id)
                + compter("SELECT COUNT(h) FROM StkFeasibilityStudyStatusHistory h WHERE h.statusId = :id", id);
        if (usages > 0) {
            throw new ConflictException("Statut %s utilisé par une étude, une transition ou l'historique : suppression impossible."
                    .formatted(s.getCode()));
        }
        lignes(DOCS_STATUT, id, StkFeasibilityStudyStatusDocumentToAttach.class).forEach(em::remove);
        em.remove(s);
    }

    /** Validation complète avant toute écriture sur l'entité gérée ; absent = inchangé. */
    private void appliquer(StkFeasibilityStudyStatus s, StatutEtudeConfigDto d) {
        String code = obligatoire(d.code(), s.getCode(), "code");
        String description = obligatoire(d.description(), s.getDescription(), "description");
        UUID type = exigerId(ou(d.typeEtatId(), s.getAdmWorkFlowStateTypeId()), "typeEtatId");
        if (!libellesTypesEtat().containsKey(type)) {
            throw new InvalidRequestException("typeEtatId : type d'état inconnu " + type);
        }
        String champs = champs(d.champsObligatoires(), s.getMandatoryFields(), 1000);
        exigerTypesDocument(d.documents());
        ReferentielAdminService.verrouiller(em, StkFeasibilityStudyStatus.class, code, STATUT);
        long pris = em.createQuery("SELECT COUNT(s) FROM StkFeasibilityStudyStatus s WHERE LOWER(s.code) = LOWER(:code) "
                + "AND s.id <> :id", Long.class).setParameter("code", code).setParameter(ID, s.getId()).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par un autre statut d'étude.".formatted(code));
        }
        s.setCode(code);
        s.setDescription(description);
        s.setAdmWorkFlowStateTypeId(type);
        s.setMandatoryFields(champs);
        s.setSorting(ou(d.sorting(), Objects.requireNonNullElse(s.getSorting(), 1)));
        s.setDelay(ou(d.delai(), Objects.requireNonNullElse(s.getDelay(), 0)));
        s.setColorName(texte(d.couleur(), s.getColorName(), COURT, "couleur"));
        s.setNotes(texte(d.notes(), s.getNotes(), 1000, "notes"));
    }

    private StatutEtudeConfigDto dto(StkFeasibilityStudyStatus s, Map<UUID, String> types) {
        return new StatutEtudeConfigDto(s.getId(), s.getSorting(), s.getCode(), s.getDescription(),
                s.getAdmWorkFlowStateTypeId(), types.get(s.getAdmWorkFlowStateTypeId()), s.getDelay(), s.getColorName(),
                s.getNotes(), ChampsObligatoiresFaisabilite.noms(s.getMandatoryFields()),
                documentsDto(lignes(DOCS_STATUT, s.getId(), StkFeasibilityStudyStatusDocumentToAttach.class)));
    }

    // ------------------------------------------------------------------ transitions

    public List<TransitionEtudeConfigDto> listerTransitions() {
        return em.createQuery("SELECT w FROM StkFeasibilityStudyWorkFlow w ORDER BY w.sorting, w.description",
                StkFeasibilityStudyWorkFlow.class).getResultList().stream().map(this::dto).toList();
    }

    public TransitionEtudeConfigDto transition(UUID id) {
        return dto(exiger(StkFeasibilityStudyWorkFlow.class, id, TRANSITION));
    }

    @Transactional
    public TransitionEtudeConfigDto creerTransition(TransitionEtudeConfigDto d) {
        StkFeasibilityStudyWorkFlow w = new StkFeasibilityStudyWorkFlow();
        w.setId(UUID.randomUUID());
        w.setStrEntityId(EtudeFaisabiliteService.ENTITE_ETUDE);
        appliquer(w, d);
        tracer(w, true);
        em.persist(w);
        remplacerSousListes(w.getId(), d);
        em.flush();
        return transition(w.getId());
    }

    @Transactional
    public TransitionEtudeConfigDto modifierTransition(UUID id, TransitionEtudeConfigDto d) {
        StkFeasibilityStudyWorkFlow w = exiger(StkFeasibilityStudyWorkFlow.class, id, TRANSITION);
        appliquer(w, d);
        tracer(w, false);
        remplacerSousListes(id, d);
        em.flush();
        return transition(id);
    }

    /** Rôles d'abord ({@code FeasibilityStudyWorkFlow.BeforeDelete}, FK NO ACTION), documents ensuite, puis la transition. */
    @Transactional
    public void supprimerTransition(UUID id) {
        StkFeasibilityStudyWorkFlow w = exiger(StkFeasibilityStudyWorkFlow.class, id, TRANSITION);
        lignes(ROLES, id, StkFeasibilityStudyWorkFlowAdmRole.class).forEach(em::remove);
        lignes(DOCS_TRANSITION, id, StkFeasibilityStudyWorkFlowDocumentToAttach.class).forEach(em::remove);
        em.flush();
        em.remove(w);
    }

    private void appliquer(StkFeasibilityStudyWorkFlow w, TransitionEtudeConfigDto d) {
        String description = obligatoire(d.description(), w.getDescription(), "description");
        UUID depart = statutConnu(ou(d.statutDepartId(), w.getStartStatusId()), "statutDepartId");
        UUID arrivee = statutConnu(ou(d.statutArriveeId(), w.getEndStatusId()), "statutArriveeId");
        if (depart.equals(arrivee)) {
            throw new InvalidRequestException(MSG_DEPART_ARRIVEE);
        }
        String champs = champs(d.champsObligatoires(), w.getMandatoryFields(), COURT);
        if (d.roles() != null) {
            d.roles().stream().distinct().forEach(r -> exigerConnu(AdmRole.class, r, "roles"));
        }
        exigerTypesDocument(d.documents());
        long doublon = em.createQuery("SELECT COUNT(w) FROM StkFeasibilityStudyWorkFlow w WHERE w.startStatusId = :depart "
                        + "AND w.endStatusId = :arrivee AND w.strEntityId = :entite AND w.id <> :id", Long.class)
                .setParameter("depart", depart).setParameter("arrivee", arrivee).setParameter("entite", w.getStrEntityId())
                .setParameter(ID, w.getId()).getSingleResult();
        if (doublon > 0) {
            throw new ConflictException("Une transition relie déjà ces deux statuts.");
        }
        w.setDescription(description);
        w.setStartStatusId(depart);
        w.setEndStatusId(arrivee);
        w.setMandatoryFields(champs);
        w.setSorting(ou(d.sorting(), Objects.requireNonNullElse(w.getSorting(), 1)));
        w.setNotesMandatory(ou(d.noteObligatoire(), Objects.requireNonNullElse(w.getNotesMandatory(), false)));
        w.setWithNotification(ou(d.avecNotification(), Objects.requireNonNullElse(w.getWithNotification(), false)));
        w.setSendEmailToUsers(ou(d.envoyerEmail(), Objects.requireNonNullElse(w.getSendEmailToUsers(), false)));
    }

    private void remplacerSousListes(UUID id, TransitionEtudeConfigDto d) {
        if (d.roles() != null) {
            lignes(ROLES, id, StkFeasibilityStudyWorkFlowAdmRole.class).forEach(em::remove);
            em.flush();
            d.roles().stream().distinct().forEach(role -> {
                StkFeasibilityStudyWorkFlowAdmRole r = new StkFeasibilityStudyWorkFlowAdmRole();
                r.setId(UUID.randomUUID());
                r.setWorkFlowId(id);
                r.setAdmRoleId(role);
                tracer(r, true);
                em.persist(r);
            });
        }
        remplacerDocuments(id, d.documents(), DOCS_TRANSITION, StkFeasibilityStudyWorkFlowDocumentToAttach::new);
    }

    private TransitionEtudeConfigDto dto(StkFeasibilityStudyWorkFlow w) {
        return new TransitionEtudeConfigDto(w.getId(), w.getSorting(), w.getDescription(), w.getStartStatusId(),
                libelleStatut(w.getStartStatusId()), w.getEndStatusId(), libelleStatut(w.getEndStatusId()),
                ChampsObligatoiresFaisabilite.noms(w.getMandatoryFields()), w.getNotesMandatory(), w.getWithNotification(),
                w.getSendEmailToUsers(), lignes(ROLES, w.getId(), StkFeasibilityStudyWorkFlowAdmRole.class).stream()
                .map(StkFeasibilityStudyWorkFlowAdmRole::getAdmRoleId).toList(),
                documentsDto(lignes(DOCS_TRANSITION, w.getId(), StkFeasibilityStudyWorkFlowDocumentToAttach.class)));
    }

    // ------------------------------------------------------------------ commun

    public List<ReferentielItemDto> typesEtat() {
        List<?> lignes = em.createNativeQuery("SELECT CONVERT(varchar(36), PKey), Code, Description "
                + "FROM AdmWorkFlowStateType ORDER BY Sorting ASC, Code ASC").getResultList();
        return lignes.stream().map(Object[].class::cast).map(l -> new ReferentielItemDto(UUID.fromString(l[0].toString()),
                (String) l[1], (String) l[2], null)).toList();
    }

    private Map<UUID, String> libellesTypesEtat() {
        return typesEtat().stream().collect(Collectors.toMap(ReferentielItemDto::id,
                t -> Objects.requireNonNullElse(t.description(), ""), (a, b) -> a));
    }

    /** Liste reçue : noms connus du catalogue (400 sinon), stockés au format OMRAN ; absente = inchangé. */
    private static String champs(List<String> noms, String actuel, int max) {
        if (noms == null) {
            return Objects.requireNonNullElse(actuel, "");
        }
        List<String> inconnus = ChampsObligatoiresFaisabilite.inconnus(noms);
        if (!inconnus.isEmpty()) {
            throw new InvalidRequestException("champsObligatoires : champ(s) inconnu(s) " + String.join(", ", inconnus));
        }
        String stocke = ChampsObligatoiresFaisabilite.stocker(noms);
        if (stocke.length() > max) {
            throw new InvalidRequestException("champsObligatoires : %d caractères au plus une fois stockés.".formatted(max));
        }
        return stocke;
    }

    private void exigerTypesDocument(List<DocumentAJoindreDto> documents) {
        if (documents != null) {
            documents.forEach(doc -> exigerConnu(AdmAttachedFileType.class, exigerId(doc == null ? null : doc.typeDocumentId(),
                    "documents.typeDocumentId"), "documents.typeDocumentId"));
        }
    }

    /** Liste reçue : remplace les lignes du porteur ; absente = inchangées. */
    private <D extends DocumentAJoindreOmran> void remplacerDocuments(UUID porteur, List<DocumentAJoindreDto> documents,
                                                                      String requete, Supplier<D> nouveau) {
        if (documents == null) {
            return;
        }
        lignes(requete, porteur, DocumentAJoindreOmran.class).forEach(em::remove);
        em.flush();
        documents.forEach(doc -> {
            D ligne = nouveau.get();
            ligne.setId(UUID.randomUUID());
            ligne.setPorteurId(porteur);
            ligne.setAdmAttachedFileTypeId(doc.typeDocumentId());
            ligne.setIsMandatory(Boolean.TRUE.equals(doc.estObligatoire()));
            ligne.setWithTitle(Boolean.TRUE.equals(doc.avecTitre()));
            tracer(ligne, true);
            em.persist(ligne);
        });
    }

    private List<DocumentAJoindreDto> documentsDto(List<? extends DocumentAJoindreOmran> lignes) {
        return lignes.stream().map(l -> new DocumentAJoindreDto(l.getAdmAttachedFileTypeId(),
                Champs.de(l.getAdmAttachedFileTypeId() == null ? null : em.find(AdmAttachedFileType.class,
                        l.getAdmAttachedFileTypeId()), AdmAttachedFileType::getDescription),
                l.getIsMandatory(), l.getWithTitle())).toList();
    }

    private <T> List<T> lignes(String requete, UUID id, Class<T> classe) {
        return em.createQuery(requete, classe).setParameter(ID, id).getResultList();
    }

    private long compter(String requete, UUID id) {
        return em.createQuery(requete, Long.class).setParameter(ID, id).getSingleResult();
    }

    private UUID statutConnu(UUID id, String champ) {
        return exigerConnu(StkFeasibilityStudyStatus.class, exigerId(id, champ), champ);
    }

    private String libelleStatut(UUID id) {
        return Champs.de(id == null ? null : em.find(StkFeasibilityStudyStatus.class, id),
                StkFeasibilityStudyStatus::getDescription);
    }

    private UUID exigerConnu(Class<?> classe, UUID id, String champ) {
        if (em.find(classe, id) == null) {
            throw new InvalidRequestException("%s : référence inconnue %s".formatted(champ, id));
        }
        return id;
    }

    private static UUID exigerId(UUID id, String champ) {
        if (id == null) {
            throw new InvalidRequestException(champ + " est obligatoire.");
        }
        return id;
    }

    private <T> T exiger(Class<T> classe, UUID id, String objet) {
        T trouve = em.find(classe, id);
        if (trouve == null) {
            throw new ResourceNotFoundException(objet + " introuvable: " + id);
        }
        return trouve;
    }

    private void tracer(LigneOmran l, boolean creation) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            l.setUserCreate(t.auteur());
            l.setUserCreateDateTime(t.le());
        }
        l.setUserUpdate(t.auteur());
        l.setUserUpdateDateTime(t.le());
    }

    private void tracer(StkFeasibilityStudyStatus s, boolean creation) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            s.setUserCreate(t.auteur());
            s.setUserCreateDateTime(t.le());
        }
        s.setUserUpdate(t.auteur());
        s.setUserUpdateDateTime(t.le());
    }
}
