package com.tsix.immocrm.services.workflow;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.auth.AuthService;
import com.tsix.immocrm.services.dossier.workflow.Intervenant;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seul moteur des workflows {@code Str*} ({@code RessourceWithWorkFlow.cs}) : transitions permises (R10, R11), exécution (R12),
 * second valideur (R13), suppression selon {@code CanDelete} (R14) — FEATURE-n6-demandes-achat.md.
 */
@Component
public record MoteurWorkflowStr(StrStatusRepository statuts, StrWorkFlowRepository workflows, StrIntervenerRepository intervenants,
                                StrHistoryRepository historiques, IntervenantCourant intervenant, UtilisateurAffiche utilisateur,
                                AuthService authentification, Clock clock) {

    public static final String CREATION = "Création";
    public static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    public static final UUID TYPE_FIN_SUCCES = UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA");
    public static final UUID TYPE_ANNULATION = UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0");
    private static final String IDENTIFIANTS_INCORRECTS = "Login ou mot de passe incorrecte !";

    /** Une transition exécutée : {@code dateUtilisateur} et {@code second} facultatifs. */
    public record Execution(UUID transitionId, String notes, LocalDateTime dateUtilisateur, SecondValideur second) {
    }

    /** Seul calcul des transitions permises : lu par le GET et par l'exécution (invariant 1). */
    public List<StrWorkFlow> permises(ObjetWorkflow o) {
        Intervenant moi = intervenant.courant();
        return permises(o, moi, intervenant.libre(moi), false);
    }

    /** {@code secondValideur} : intervenants {@code WithPassword = 1} (R13) au lieu de {@code WithPassword = 0} (R10). */
    private List<StrWorkFlow> permises(ObjetWorkflow o, Intervenant qui, boolean libre, boolean secondValideur) {
        UUID entite = Champs.de(o.getStrEntity(), StrEntity::getId);
        UUID courant = Champs.de(o.getStrStatus(), StrStatus::getId);
        if (libre) {
            Map<UUID, StrWorkFlow> parArrivee = new LinkedHashMap<>();
            workflows.deLEntite(entite).forEach(w -> parArrivee.putIfAbsent(w.getStatusToId(), w));
            parArrivee.remove(courant);
            return List.copyOf(parArrivee.values());
        }
        List<StrWorkFlow> sortantes = workflows.sortantes(entite, courant);
        if (sortantes.isEmpty()) {
            return sortantes;
        }
        List<UUID> ids = sortantes.stream().map(StrWorkFlow::getId).toList();
        Set<UUID> ouvertes = new HashSet<>();
        for (Object[] r : secondValideur ? intervenants.secondsValideurs(ids) : intervenants.deTransitions(ids)) {
            if (qui.correspond((StrIntervener) r[1])) {
                ouvertes.add((UUID) r[0]);
            }
        }
        return sortantes.stream().filter(w -> ouvertes.contains(w.getId())).toList();
    }

    public Map<UUID, StrStatus> cibles(List<StrWorkFlow> transitions) {
        return statuts.findAllById(transitions.stream().map(StrWorkFlow::getStatusToId).toList()).stream()
                .collect(Collectors.toMap(StrStatus::getId, Function.identity()));
    }

    /** Tous les refus avant la première écriture ; {@code hook} reçoit le statut d'arrivée et peut refuser (R12). */
    public void executer(ObjetWorkflow o, Execution e, Consumer<StrStatus> hook) {
        Intervenant moi = intervenant.courant();
        boolean libre = intervenant.libre(moi);
        List<StrWorkFlow> permises = permises(o, moi, libre, false);
        Map<UUID, StrStatus> cibles = cibles(permises);
        StrWorkFlow w = permises.stream().filter(t -> t.getId().equals(e.transitionId())).findFirst()
                .orElseThrow(() -> new InvalidStatutTransitionException(
                        "Transition non permise depuis le statut %s.".formatted(libelle(o.getStrStatus())),
                        permises.stream().map(t -> cibles.get(t.getStatusToId())).filter(Objects::nonNull)
                                .map(StrStatus::getCode).toList()));
        String second = secondValideur(o, w, libre, e.second());
        boolean commentaire = Boolean.TRUE.equals(w.getWithComment());
        if (commentaire && (e.notes() == null || e.notes().isBlank())) {
            throw new InvalidRequestException("Veuillez saisir un commentaire obligatoirement !");
        }
        StrStatus cible = cibles.get(w.getStatusToId());
        hook.accept(cible);
        StrStatus depart = Objects.equals(w.getStatusFromId(), Champs.de(o.getStrStatus(), StrStatus::getId))
                ? o.getStrStatus() : statuts.findById(w.getStatusFromId()).orElse(null);
        o.setStrStatus(cible);
        boolean avecDate = Boolean.TRUE.equals(w.getWithUserDate());
        LocalDateTime maintenant = LocalDateTime.now(clock);
        historiser(o, "%s: %s=>%s(%s & %s)".formatted(w.getDescription(), libelle(depart), libelle(cible), loginCourant(), second),
                w, commentaire || avecDate ? e.notes() : "",
                avecDate && e.dateUtilisateur() != null ? e.dateUtilisateur() : maintenant);
    }

    /** R13 ; renvoie le login du second ({@code ""} sans second valideur). Mot de passe jamais conservé. */
    private String secondValideur(ObjetWorkflow o, StrWorkFlow w, boolean libre, SecondValideur sv) {
        if (!Boolean.TRUE.equals(w.getWithPassword())) {
            return "";
        }
        AdmUser second = Optional.ofNullable(sv).flatMap(v -> authentification.verifierIdentifiants(v.login(), v.motDePasse()))
                .orElseThrow(() -> new InvalidRequestException(IDENTIFIANTS_INCORRECTS));
        if (intervenants.secondsValideurs(List.of(w.getId())).isEmpty()) {
            if (second.getCode().equalsIgnoreCase(loginCourant())) {
                throw new ForbiddenException("Le 2ème utilisateur doit être différent de l'utilisateur connecté !");
            }
            return second.getCode();
        }
        // Quirk OMRAN : drapeau de restriction de l'utilisateur connecté, intervenants du second.
        boolean habilite = permises(o, intervenant.de(second.getId()), libre, true).stream()
                .anyMatch(t -> t.getId().equals(w.getId()));
        if (!habilite) {
            throw new ForbiddenException("Le 2ème utilisateur n'a pas les habilitations requises pour cette action !");
        }
        return second.getCode();
    }

    /** R14 : statut stocké sans {@code CanDelete} et appelant restreint → 409. */
    public void verifierSuppression(ObjetWorkflow o) {
        StrStatus s = o.getStrStatus();
        if (s != null && !Boolean.TRUE.equals(s.getSuppressionPermise()) && !intervenant.libre(intervenant.courant())) {
            throw new ConflictException("Vous ne pouvez pas supprimer cette entité dans le statut : [%s] !"
                    .formatted(s.getDescription()));
        }
    }

    /** Statut de départ (R2) : premier statut de type Nouveau de l'entité, par {@code Sorting}, visible de l'appelant. */
    public Optional<StrStatus> statutDeDepart(UUID entite) {
        Optional<Set<UUID>> visibles = intervenant.statutsDeLotsEtProjets();
        return statuts.findByStrConfigEntityId(entite).stream()
                .filter(s -> TYPE_NOUVEAU.equals(s.getStrStatusTypeId()))
                .filter(s -> visibles.map(v -> v.contains(s.getId())).orElse(true))
                .min(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** {@code w} nul : ligne « Création » ({@code RessourceWithWorkFlow.cs:AfterInsert}). */
    public void historiser(ObjetWorkflow o, String description, StrWorkFlow w, String notes) {
        historiser(o, description, w, notes, LocalDateTime.now(clock));
    }

    private void historiser(ObjetWorkflow o, String description, StrWorkFlow w, String notes, LocalDateTime dateUtilisateur) {
        UtilisateurAffiche.Auteur auteur = utilisateur.courant();
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(LocalDateTime.now(clock));
        h.setDescription(description);
        h.setNotes(notes);
        h.setObjectId(o.getId());
        h.setAdmUserId(auteur.compteId());
        h.setAdmUserDescription(auteur.nom());
        h.setStrWorkflowId(Champs.de(w, StrWorkFlow::getId));
        h.setStrWorkflowDescription(Champs.de(w, StrWorkFlow::getDescription));
        h.setStrStatusId(Champs.de(o.getStrStatus(), StrStatus::getId));
        h.setStrStatusDescription(libelle(o.getStrStatus()));
        h.setUserDate(dateUtilisateur);
        h.setStrConfigEntityId(Champs.de(o.getStrEntity(), StrEntity::getId));
        historiques.save(h);
    }

    private String loginCourant() {
        return Objects.toString(Champs.de(utilisateur.courant().compte(), AdmUser::getCode), "");
    }

    public static String libelle(StrStatus s) {
        return Champs.de(s, StrStatus::getDescription);
    }
}
