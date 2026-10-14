package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.EtapeMilestone;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.EtapeSuivanteDto;
import com.tsix.immocrm.dto.dossier.TransitionSuggereeDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.stl.VersementsDossierRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Moteur de workflow du dossier de vente, branché sur {@code PATCH …/statut} : transition paramétrée et intervenant
 * (409 / 403), contrôles (400 / 409), puis effets du changement (validité, gestionnaire, alertes). Voir
 * FEATURE-vente-workflow.md.
 */
@Component
public class WorkflowDossier {

    private final TransitionsDossier transitions;
    private final IntervenantCourant intervenantCourant;
    private final ControlesTransition controles;
    private final AlertesStatutDossier alertes;
    private final DateAssociee dateAssociee;
    private final EffetsWorkflow effets;

    public WorkflowDossier(TransitionsDossier transitions, IntervenantCourant intervenantCourant,
                           ControlesTransition controles, AlertesStatutDossier alertes, DateAssociee dateAssociee,
                           EffetsWorkflow effets) {
        this.transitions = transitions;
        this.intervenantCourant = intervenantCourant;
        this.controles = controles;
        this.alertes = alertes;
        this.dateAssociee = dateAssociee;
        this.effets = effets;
    }

    /** Dépendances des effets, regroupées. */
    @Component
    public record EffetsWorkflow(StrStatusRepository statusRepository, VersementsDossierRepository versements,
                                 Clock clock) {
    }

    /**
     * Transition vers {@code cible} : 409 si elle n'existe pas depuis le statut courant (la réponse liste celles que
     * l'appelant peut exécuter), 403 si l'appelant n'en est pas intervenant.
     */
    public TransitionDossier transitionAutorisee(ComFolder dossier, StatutDossier cible) {
        StrStatus depuis = dossier.getStrStatus();
        Intervenant intervenant = intervenantCourant.courant();
        List<TransitionDossier> autorisees = transitions.autorisees(depuis, intervenant);
        TransitionDossier transition = transitions.vers(depuis, cible).orElseThrow(() ->
                new InvalidStatutTransitionException("Transition invalide: " + depuis.getCode() + " -> " + cible.getCode(),
                        autorisees.stream().map(TransitionDossier::codeCible).distinct().toList()));
        boolean autorisee = !transition.parametree() || autorisees.stream()
                .anyMatch(t -> Objects.equals(t.workflowId(), transition.workflowId()));
        if (!autorisee) {
            throw new ForbiddenException("Vos rôles ne permettent pas la transition " + depuis.getCode() + " -> "
                    + cible.getCode() + " (paramétrage des transitions).");
        }
        return transition;
    }

    /**
     * Contrôles avant écriture, dans l'ordre d'OMRAN. Rend vrai si le seuil a été ignoré (Administrateur et
     * {@code ignorerSeuil}), pour la trace.
     */
    public boolean verifier(ComFolder dossier, TransitionDossier transition, StrStatus cible, boolean ignorerSeuil) {
        String manquants = controles.obligatoires(dossier, transition, cible);
        if (!manquants.isEmpty()) {
            throw new InvalidRequestException(manquants);
        }
        if (!cible.estActif()) {
            return false;
        }
        Intervenant intervenant = intervenantCourant.courant();
        boolean seuilIgnore = false;
        if (ignorerSeuil) {
            if (!intervenant.administrateur()) {
                throw new ForbiddenException("Seul un Administrateur peut ignorer le seuil de versement.");
            }
            seuilIgnore = true;
        } else if (intervenantCourant.verifieLeSeuil(intervenant)) {
            controles.seuil(dossier, cible).ifPresent(WorkflowDossier::conflit);
        }
        controles.schema(dossier, cible).ifPresent(WorkflowDossier::conflit);
        return seuilIgnore;
    }

    public void ecrireDateAssociee(ComFolder dossier, StrStatus cible, StatutDossier code, LocalDateTime date) {
        dateAssociee.ecrire(dossier, cible, code, date);
    }

    /**
     * Après le changement ({@code Sale.cs:AfterExecuteWorkflow} et {@code BeforeUpdate}) : validité du statut,
     * gestionnaire auto, extinction des relances, alerte d'expiration si la validité a changé.
     */
    public void apresChangement(ComFolder dossier, StrStatus nouveau, StatutDossier connu, Supplier<CfgTier> employe) {
        LocalDateTime ancienneValidite = dossier.getStatusValidityDate();
        dossier.setStatusValidityDate(dateDeValidite(nouveau));
        if (Boolean.TRUE.equals(nouveau.getAffectationGestionnaire()) && dossier.getCfgSaleManager() == null) {
            dossier.setCfgSaleManager(employe.get());
        }
        alertes.eteindre(dossier, nouveau);
        if (dossier.getStatusValidityDate() != null
                && !dossier.getStatusValidityDate().equals(ancienneValidite)) {
            alertes.alerteExpiration(dossier, nouveau, connu);
        }
    }

    /** {@code Sale.cs:SetStatusValidityDate} : aujourd'hui + durée de vie, nulle sans durée. */
    LocalDateTime dateDeValidite(StrStatus statut) {
        Integer delai = statut.getDelai();
        return delai != null && delai > 0 ? LocalDate.now(effets.clock()).plusDays(delai).atStartOfDay() : null;
    }

    /** Codes des statuts atteignables par l'appelant (sans doublon). */
    public List<String> statutsAtteignables(ComFolder dossier) {
        if (dossier.getStrStatus() == null) {
            return List.of();
        }
        return transitions.autorisees(dossier.getStrStatus(), intervenantCourant.courant()).stream()
                .map(TransitionDossier::codeCible).distinct().toList();
    }

    /** Statuts atteignables par l'appelant, avec la date associée de chacun. */
    public List<EtapeSuivanteDto> etapesSuivantes(ComFolder dossier) {
        if (dossier.getStrStatus() == null) {
            return List.of();
        }
        List<StrStatus> statuts = effets.statusRepository().findByStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        return transitions.autorisees(dossier.getStrStatus(), intervenantCourant.courant()).stream()
                .map(t -> {
                    StrStatus cible = statuts.stream().filter(s -> t.codeCible().equalsIgnoreCase(s.getCode()))
                            .findFirst().orElse(null);
                    EtapeMilestone.Info info = dateAssociee.de(cible, TransitionsDossier.statutConnu(cible)
                            .orElseGet(() -> TransitionsDossier.statutDeCode(t.codeCible()).orElse(null)));
                    return new EtapeSuivanteDto(t.codeCible(), info.champ(), info.libelle(), info.confiance().name(),
                            t.description(), t.automatique(), dateAssociee.valeurActuelle(dossier, info.champ()));
                })
                .toList();
    }

    /**
     * Passage automatique ({@code Front.Sales/Sale.cs:AutomateNextTransition}) : l'unique transition « avec
     * proposition du passage automatique » que l'appelant peut exécuter, si le seuil de son statut cible est atteint
     * (même calcul que le refus) ; date proposée = dernier règlement. Deux candidates ou plus : rien (aide TRANSITIONS).
     */
    public TransitionSuggereeDto suggestion(ComFolder dossier) {
        if (dossier.getStrStatus() == null) {
            return TransitionSuggereeDto.AUCUNE;
        }
        List<TransitionDossier> automatiques = transitions.autorisees(dossier.getStrStatus(), intervenantCourant.courant())
                .stream().filter(TransitionDossier::automatique).toList();
        if (automatiques.size() != 1) {
            return TransitionSuggereeDto.AUCUNE;
        }
        TransitionDossier transition = automatiques.get(0);
        Optional<StrStatus> cible = Optional.ofNullable(effets.statusRepository().findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, transition.codeCible()));
        if (cible.isEmpty() || !controles.seuilAtteint(dossier, cible.get())) {
            return TransitionSuggereeDto.AUCUNE;
        }
        return new TransitionSuggereeDto(transition.codeCible(), transition.description(),
                effets.versements().dernierVersement(dossier.getId()));
    }

    private static void conflit(String message) {
        throw new ConflictException(message);
    }
}
