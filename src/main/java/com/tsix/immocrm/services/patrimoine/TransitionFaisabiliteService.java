package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.ChampsObligatoiresFaisabilite;
import com.tsix.immocrm.domain.faisabilite.ChampsObligatoiresFaisabilite.ChampObligatoire;
import com.tsix.immocrm.dto.patrimoine.faisabilite.DemandeTransitionEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.HistoriqueStatutEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionPossibleDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionPossibleDto.ChampTransitionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionPossibleDto.DocumentTransitionDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatus;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlow;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlowDocumentToAttach;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Statuts et transitions d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §4.1, invariant I9) : une transition est
 * permise si elle part du statut courant et qu'un de ses rôles ({@code AdmRole.Code}) est un rôle du jeton — seul contrôle
 * des rôles, partagé par la liste et l'exécution.
 */
@Service
@Transactional(readOnly = true)
public class TransitionFaisabiliteService {

    private static final String PREFIXE_ROLE = "ROLE_";

    private final StkFeasibilityStudyRepository etudes;
    private final EtudeFaisabiliteService fiches;
    private final RecalculFaisabiliteService recalcul;
    private final PieceJointeSupport pieces;
    private final TracePatrimoine trace;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;

    public TransitionFaisabiliteService(StkFeasibilityStudyRepository etudes, EtudeFaisabiliteService fiches,
                                        RecalculFaisabiliteService recalcul, PieceJointeSupport pieces, TracePatrimoine trace,
                                        UtilisateurAffiche utilisateur, EntityManager em) {
        this.etudes = etudes;
        this.fiches = fiches;
        this.recalcul = recalcul;
        this.pieces = pieces;
        this.trace = trace;
        this.utilisateur = utilisateur;
        this.em = em;
    }

    /** Statuts d'arrivée distincts, par ordre ({@code FeasibilityStudyStatus.GetNextStatusList}). */
    public List<TransitionPossibleDto> lister(UUID etudeId) {
        StkFeasibilityStudy e = etudes.exigerVisible(etudeId);
        Map<UUID, StkFeasibilityStudyWorkFlow> parArrivee = new LinkedHashMap<>();
        permises(e).forEach(w -> parArrivee.putIfAbsent(w.getEndStatusId(), w));
        boolean bloquee = Boolean.TRUE.equals(e.getIsLocked());
        return parArrivee.values().stream()
                .map(w -> dto(e, w, bloquee))
                .sorted(Comparator.comparing(t -> ordre(t.statutArriveeId()), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Contrôles OMRAN dans l'ordre (§4.1) ; tout refus passe avant la moindre écriture. */
    @Transactional
    public EtudeFaisabiliteDto modifierStatut(UUID etudeId, DemandeTransitionEtudeDto demande) {
        StkFeasibilityStudy e = FicheEtude.modifiable(etudes, etudeId);
        UUID transitionId = demande == null ? null : demande.transitionId();
        if (transitionId == null) {
            throw new InvalidRequestException("transitionId : la transition est obligatoire.");
        }
        StkFeasibilityStudyWorkFlow w = trouver(StkFeasibilityStudyWorkFlow.class, transitionId, "Transition d'étude");
        List<StkFeasibilityStudyWorkFlow> permises = permises(e);
        if (permises.stream().noneMatch(p -> p.getId().equals(transitionId))) {
            throw new InvalidStatutTransitionException("Transition non permise depuis le statut %s."
                    .formatted(Champs.de(e.getStatus(), StkFeasibilityStudyStatus::getDescription)),
                    permises.stream().map(StkFeasibilityStudyWorkFlow::getDescription).distinct().toList());
        }
        exigerChampsEtDocuments(e, w);
        StkFeasibilityStudyStatus cible = trouver(StkFeasibilityStudyStatus.class, w.getEndStatusId(), "Statut d'étude");
        FicheEtude.exigerChampsDuStatut(cible, e.getCode(), e.getDescription(), e.getDocDate());
        TracePatrimoine.Trace t = trace.maintenant();
        e.setStatus(cible);
        e.setUserUpdate(t.auteur());
        e.setUserUpdateDateTime(t.le());
        em.persist(FicheEtude.historique(e.getId(), cible.getId(), t.le(),
                Champs.de(utilisateur.courant().compte(), AdmUser::getId)));
        recalcul.recalculer(e);
        return fiches.fiche(etudeId);
    }

    public List<HistoriqueStatutEtudeDto> historique(UUID etudeId) {
        etudes.exigerVisible(etudeId);
        return em.createQuery("""
                        SELECT new com.tsix.immocrm.dto.patrimoine.faisabilite.HistoriqueStatutEtudeDto(h.id, h.docDate, s.code,
                            s.description, s.sorting, u.description)
                        FROM StkFeasibilityStudyStatusHistory h
                        LEFT JOIN StkFeasibilityStudyStatus s ON s.id = h.statusId
                        LEFT JOIN AdmUser u ON u.id = h.admUserId
                        WHERE h.feasibilityStudyId = :etude ORDER BY h.docDate, h.id""", HistoriqueStatutEtudeDto.class)
                .setParameter("etude", etudeId).getResultList();
    }

    // ------------------------------------------------------------------ règle partagée

    /** Transitions au départ du statut courant dont un rôle autorisé porte le code d'un rôle du jeton. */
    private List<StkFeasibilityStudyWorkFlow> permises(StkFeasibilityStudy e) {
        Set<String> codes = rolesDuJeton();
        UUID statut = Champs.de(e.getStatus(), StkFeasibilityStudyStatus::getId);
        if (codes.isEmpty() || statut == null) {
            return List.of();
        }
        return em.createQuery("""
                        SELECT DISTINCT w FROM StkFeasibilityStudyWorkFlow w, StkFeasibilityStudyWorkFlowAdmRole wr, AdmRole r
                        WHERE wr.workFlowId = w.id AND r.id = wr.admRoleId AND w.startStatusId = :statut AND r.code IN :codes
                        ORDER BY w.sorting, w.description""", StkFeasibilityStudyWorkFlow.class)
                .setParameter("statut", statut).setParameter("codes", codes).getResultList();
    }

    private static Set<String> rolesDuJeton() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(a -> a.startsWith(PREFIXE_ROLE)).map(a -> a.substring(PREFIXE_ROLE.length()))
                .collect(Collectors.toSet());
    }

    /** {@code VerifWorkflowMandatories} puis les documents ({@code statusBtn_Click}) : tous les messages, champs d'abord. */
    private void exigerChampsEtDocuments(StkFeasibilityStudy e, StkFeasibilityStudyWorkFlow w) {
        List<String> messages = new ArrayList<>();
        List<String> champs = ChampsObligatoiresFaisabilite.manquants(w.getMandatoryFields(), renseignes(e));
        if (!champs.isEmpty()) {
            messages.add("Transition :");
            messages.addAll(champs);
        }
        documents(e, w).stream().filter(d -> !d.fourni())
                .forEach(d -> messages.add("Le document de type : " + d.typeLibelle() + " est obligatoire"));
        if (!messages.isEmpty()) {
            throw new InvalidRequestException(String.join("\n", messages));
        }
    }

    private static Predicate<ChampObligatoire> renseignes(StkFeasibilityStudy e) {
        return ch -> ChampsObligatoiresFaisabilite.renseigne(ch, e.getCode(), e.getDescription(), e.getDocDate());
    }

    /** Toute ligne est exigée ({@code IsMandatory} non lu) ; une pièce supprimée logiquement ne compte pas. */
    private List<DocumentTransitionDto> documents(StkFeasibilityStudy e, StkFeasibilityStudyWorkFlow w) {
        return em.createQuery("SELECT d FROM StkFeasibilityStudyWorkFlowDocumentToAttach d WHERE d.workFlowId = :w",
                        StkFeasibilityStudyWorkFlowDocumentToAttach.class).setParameter("w", w.getId()).getResultList().stream()
                .map(StkFeasibilityStudyWorkFlowDocumentToAttach::getAdmAttachedFileTypeId).filter(Objects::nonNull).distinct()
                .map(type -> new DocumentTransitionDto(type,
                        Champs.de(em.find(AdmAttachedFileType.class, type), AdmAttachedFileType::getDescription),
                        pieces.pieceDuType(e.getId(), type).isPresent()))
                .toList();
    }

    private TransitionPossibleDto dto(StkFeasibilityStudy e, StkFeasibilityStudyWorkFlow w, boolean bloquee) {
        Predicate<ChampObligatoire> renseigne = renseignes(e);
        List<ChampTransitionDto> champs = ChampsObligatoiresFaisabilite.coches(w.getMandatoryFields()).stream()
                .map(ch -> new ChampTransitionDto(ch.champ(), ch.libelle(), renseigne.test(ch))).toList();
        List<DocumentTransitionDto> documents = documents(e, w);
        boolean possible = !bloquee && champs.stream().allMatch(ChampTransitionDto::renseigne)
                && documents.stream().allMatch(DocumentTransitionDto::fourni);
        return new TransitionPossibleDto(w.getId(), w.getDescription(), w.getStartStatusId(), libelle(w.getStartStatusId()),
                w.getEndStatusId(), libelle(w.getEndStatusId()), champs, documents, possible);
    }

    private String libelle(UUID statutId) {
        return Champs.de(statutId == null ? null : em.find(StkFeasibilityStudyStatus.class, statutId),
                StkFeasibilityStudyStatus::getDescription);
    }

    private Integer ordre(UUID statutId) {
        return Champs.de(statutId == null ? null : em.find(StkFeasibilityStudyStatus.class, statutId),
                StkFeasibilityStudyStatus::getSorting);
    }

    private <T> T trouver(Class<T> classe, UUID id, String objet) {
        T trouve = id == null ? null : em.find(classe, id);
        if (trouve == null) {
            throw new ResourceNotFoundException(objet + " introuvable: " + id);
        }
        return trouve;
    }
}
