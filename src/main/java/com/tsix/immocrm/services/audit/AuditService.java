package com.tsix.immocrm.services.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran;
import com.tsix.immocrm.dto.audit.ChangementAuditDto;
import com.tsix.immocrm.dto.audit.LigneAuditDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmTaskRepository;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Journal d'audit d'un objet du patrimoine — FEATURE-patrimoine-audit-actions.md §2.3, §10.1. */
@Service
public class AuditService {

    static final int TAILLE_MAX = 200;

    private final AdmWebHistoricsRepository repository;
    private final PorteursPatrimoine porteurs;
    private final CfgCompanyRepository entites;
    private final AdmTaskRepository taches;
    private final JsonMapper json;

    public AuditService(AdmWebHistoricsRepository repository, PorteursPatrimoine porteurs, CfgCompanyRepository entites,
                        AdmTaskRepository taches, JsonMapper json) {
        this.repository = repository;
        this.porteurs = porteurs;
        this.entites = entites;
        this.taches = taches;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneAuditDto> lister(UUID objetId, int page, int size) {
        if (page < 0 || size < 1 || size > TAILLE_MAX) {
            throw new InvalidRequestException("page doit être ≥ 0 et size entre 1 et " + TAILLE_MAX);
        }
        // Porteur ENTITE : l'écran des entités montre aussi les inactives, leur journal reste lisible (§7.11.3).
        if (!entites.existsById(objetId) && !rendezVousDeLAdministrateur(objetId)) {
            porteurs.exigerVisible(objetId);
        }
        PageRequest demande = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("actionDateTime"), Sort.Order.desc("id")));
        return PageDto.of(repository.journal(objetId, RegistreAuditOmran.TYPES, demande).map(this::versDto));
    }

    /** Hors {@code PorteursPatrimoine}, qui ouvrirait aussi les notes ; rôle de l'écran, sinon 404 comme un objet inconnu. */
    private boolean rendezVousDeLAdministrateur(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_Administrateur".equals(a.getAuthority()))
                && taches.findByIdAndStrEntityId(id, AdmTask.ENTITE_RENDEZ_VOUS).isPresent();
    }

    private LigneAuditDto versDto(AdmWebHistorics h) {
        return new LigneAuditDto(h.getId(), h.getActionDateTime(), h.getUserDescription(), h.getAction(),
                h.getObjectType(), h.getObjectTypeLabel(), h.getObjectLabel(), h.getParentObjectType(),
                details(h.getDetails()));
    }

    /** Détails illisibles (JSON invalide, ancien format OMRAN) : liste vide, jamais une erreur. */
    private List<ChangementAuditDto> details(String brut) {
        if (brut == null || brut.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(json.readValue(brut, String[][].class))
                    .filter(e -> e != null && e.length >= 3)
                    .map(e -> new ChangementAuditDto(e[0], Objects.toString(e[1], ""), Objects.toString(e[2], "")))
                    .toList();
        } catch (JacksonException e) {
            return List.of();
        }
    }
}
