package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.dto.achat.ActionsPieceAchatDto.TransitionPermiseAchatDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.achat.config.ConfigurationAchatService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seul calcul des transitions d'une pièce d'achat (FEATURE-achat-fiche-ecritures.md E-B2, E-B6) : celles de sa gestion depuis
 * son statut courant, autorisées par un rôle du compte ou par la voie déclencheur ; Administrateur sans exemption.
 */
@Component
public class TransitionsPieceAchat {

    private final ConfigurationAchatService configuration;
    private final AdmUserRoleRepository roles;
    private final EntityManager em;

    public TransitionsPieceAchat(ConfigurationAchatService configuration, AdmUserRoleRepository roles, EntityManager em) {
        this.configuration = configuration;
        this.roles = roles;
        this.em = em;
    }

    /** Transitions de la gestion de la pièce depuis son statut courant (aucune si statut NULL), tri Sorting. */
    public List<TransitionAchatDto> candidates(BuyDocument d) {
        UUID statut = d.getBuyDocumentStatusId();
        if (statut == null) {
            return List.of();
        }
        return configuration.transitions(false).stream()
                .filter(t -> statut.equals(t.statutDepartId()) && Objects.equals(t.gestionId(), d.getStrEntityId()))
                .sorted(Comparator.comparing(TransitionAchatDto::ordre, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<TransitionAchatDto> autorisees(BuyDocument d, List<TransitionAchatDto> candidates) {
        UUID compte = PerimetreCommercialService.admUserIdCourant().orElse(null);
        Set<UUID> rolesDuCompte = compte == null ? Set.of() : roles.findByAdmUserId(compte).stream()
                .map(r -> r.getAdmRole().getId()).collect(Collectors.toSet());
        UUID employe = employeCourant();
        boolean declencheur = employe != null && employe.equals(d.getCfgTierTriggerId());
        return candidates.stream().filter(t -> (declencheur && t.valideeParDeclencheur())
                || t.roles().stream().anyMatch(r -> rolesDuCompte.contains(r.id()))).toList();
    }

    /** Statuts d'arrivée distincts des transitions autorisées ; note obligatoire = celle de la 1ʳᵉ, que PATCH retient. */
    public List<TransitionPermiseAchatDto> permises(List<TransitionAchatDto> autorisees) {
        Map<UUID, Boolean> arrivees = new LinkedHashMap<>();
        autorisees.stream().filter(t -> t.statutArriveeId() != null)
                .forEach(t -> arrivees.putIfAbsent(t.statutArriveeId(), t.noteObligatoire()));
        return arrivees.entrySet().stream().map(e -> {
            BuyDocumentStatus s = em.find(BuyDocumentStatus.class, e.getKey());
            return new TransitionPermiseAchatDto(e.getKey(), s == null ? null : s.getCode(),
                    s == null ? null : s.getDescription(), e.getValue());
        }).toList();
    }

    public List<TransitionPermiseAchatDto> permises(BuyDocument d) {
        return permises(autorisees(d, candidates(d)));
    }

    private static UUID employeCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user.employeeId() : null;
    }
}
