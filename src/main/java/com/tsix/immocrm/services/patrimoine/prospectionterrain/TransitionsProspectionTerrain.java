package com.tsix.immocrm.services.patrimoine.prospectionterrain;

import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ExecutionTransitionDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.workflow.Intervenant;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Workflow {@code Str*} de l'entité {@code LndLand} ({@code RessourceWithWorkFlow.cs:ExecuteWorkflow}), distinct de celui
 * des dossiers (FEATURE-patrimoine-prospections-terrain.md §4).
 */
@Component
record TransitionsProspectionTerrain(StrEntityRepository entites, StrStatusRepository statuts,
                                     StrWorkFlowRepository workflows, StrIntervenerRepository intervenants,
                                     StrHistoryRepository historiques, IntervenantCourant intervenantCourant,
                                     UtilisateurAffiche utilisateur, Clock clock) {

    static final String ENTITE = "LndLand";
    private static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    private static final Set<UUID> TYPES_ARCHIVES = Set.of(UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97"),
            UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0"));

    /** Entité {@code LndLand} et son premier statut de type « Nouveau » ({@code StrStatus.GetStartStatus}), sans repli. */
    StrStatus statutInitial(LndLand p) {
        UUID entite = entites.findByCodeIn(List.of(ENTITE)).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity LndLand absente."));
        p.setStrEntityId(entite);
        return statuts.findByStrConfigEntityId(entite).stream()
                .filter(s -> TYPE_NOUVEAU.equals(s.getStrStatusTypeId()))
                .min(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new ConfigurationManquanteException("Aucun statut « Nouveau » pour l'entité LndLand."));
    }

    List<StrWorkFlow> sortantes(LndLand p) {
        return workflows.sortantes(p.getStrEntityId(), Champs.de(p.getStrStatus(), StrStatus::getId));
    }

    Map<UUID, StrStatus> cibles(List<StrWorkFlow> transitions) {
        return statuts.findAllById(transitions.stream().map(StrWorkFlow::getStatusToId).toList()).stream()
                .collect(Collectors.toMap(StrStatus::getId, Function.identity()));
    }

    void executer(LndLand p, ExecutionTransitionDto d) {
        List<StrWorkFlow> permises = sortantes(p);
        Map<UUID, StrStatus> cibles = cibles(permises);
        StrWorkFlow w = permises.stream().filter(t -> t.getId().equals(d.transitionId())).findFirst()
                .orElseThrow(() -> new InvalidStatutTransitionException(
                        "Transition non permise depuis le statut %s.".formatted(libelle(p.getStrStatus())),
                        permises.stream().map(t -> cibles.get(t.getStatusToId())).filter(Objects::nonNull)
                                .map(StrStatus::getCode).toList()));
        StrStatus cible = cibles.get(w.getStatusToId());
        if (Boolean.TRUE.equals(w.getWithPassword())) {
            throw new ConflictException("Cette transition exige un second utilisateur : non prise en charge.");
        }
        exigerIntervenant(w, p.getStrStatus(), cible);
        if (Boolean.TRUE.equals(w.getWithComment()) && (d.commentaire() == null || d.commentaire().isBlank())) {
            throw new InvalidRequestException("commentaire : Veuillez saisir un commentaire obligatoirement !");
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        p.setStrStatus(cible);
        appliquerStatut(p, cible);
        historiser(p, w, Objects.requireNonNullElse(d.commentaire(), ""),
                Boolean.TRUE.equals(w.getWithUserDate()) && d.dateUtilisateur() != null ? d.dateUtilisateur() : maintenant);
    }

    /** Archivage vers un statut d'échec ou d'annulation (jamais désarchivé) et validité = date + durée si positive. */
    static void appliquerStatut(LndLand p, StrStatus statut) {
        if (estArchivant(statut)) {
            p.setIsInArchive(true);
        }
        Integer delai = statut.getDelai();
        if (delai != null && delai > 0 && p.getDocDate() != null) {
            p.setStatusValidityDate(p.getDocDate().plusDays(delai));
        }
    }

    static boolean estArchivant(StrStatus statut) {
        return statut != null && TYPES_ARCHIVES.contains(statut.getStrStatusTypeId());
    }

    /** {@code w} nul : ligne « Création ». */
    void historiser(LndLand p, StrWorkFlow w, String notes, LocalDateTime dateUtilisateur) {
        UtilisateurAffiche.Auteur auteur = utilisateur.courant();
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(LocalDateTime.now(clock));
        h.setDescription(w == null ? "Création" : "%s: %s(%s & )".formatted(w.getDescription(), w.getCode(),
                Objects.toString(Champs.de(auteur.compte(), AdmUser::getCode), "")));
        h.setNotes(notes);
        h.setObjectId(p.getId());
        h.setAdmUserId(auteur.compteId());
        h.setAdmUserDescription(auteur.nom());
        h.setStrWorkflowId(Champs.de(w, StrWorkFlow::getId));
        h.setStrWorkflowDescription(Champs.de(w, StrWorkFlow::getDescription));
        h.setStrStatusId(p.getStrStatus().getId());
        h.setStrStatusDescription(p.getStrStatus().getDescription());
        h.setUserDate(dateUtilisateur);
        h.setStrConfigEntityId(p.getStrEntityId());
        historiques.save(h);
    }

    /** Transition liée à des intervenants : un de leurs rôles exigé ; l'Administrateur passe comme pour les dossiers. */
    private void exigerIntervenant(StrWorkFlow w, StrStatus depuis, StrStatus cible) {
        Intervenant i = intervenantCourant.courant();
        if (i.administrateur()) {
            return;
        }
        List<Object[]> lignes = intervenants.deTransitions(List.of(w.getId()));
        if (!lignes.isEmpty() && lignes.stream().noneMatch(l -> i.correspond((StrIntervener) l[1]))) {
            throw new ForbiddenException("Vos rôles ne permettent pas la transition %s -> %s (paramétrage des transitions)."
                    .formatted(libelle(depuis), libelle(cible)));
        }
    }

    private static String libelle(StrStatus s) {
        return Champs.de(s, StrStatus::getDescription);
    }
}
