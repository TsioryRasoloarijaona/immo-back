package com.tsix.immocrm.services.audit;

import com.tsix.immocrm.domain.audit.DetailsAudit;
import com.tsix.immocrm.dto.audit.ChangementAuditDto;
import com.tsix.immocrm.dto.audit.LigneAuditDto;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Écran « Audit » d'une fiche (FEATURE-patrimoine-audit-actions.md §2.3), lu sur la copie locale. */
@Service
@Transactional(readOnly = true)
public class AuditService {

    public static final int TAILLE_PAR_DEFAUT = 50;
    public static final int TAILLE_MAX = 200;

    // Second critère : ordre total, sinon deux lignes du même instant changent de page d'un appel à l'autre.
    private static final Sort ORDRE = Sort.by(Sort.Order.desc("actionDateTime"), Sort.Order.desc("id"));

    private final AdmWebHistoricsRepository repository;

    public AuditService(AdmWebHistoricsRepository repository) {
        this.repository = repository;
    }

    public Page<LigneAuditDto> lignes(UUID objetId, int page, int taille) {
        if (page < 0) {
            throw new InvalidRequestException("page doit être positive ou nulle : " + page);
        }
        if (taille < 1 || taille > TAILLE_MAX) {
            throw new InvalidRequestException("size doit être compris entre 1 et " + TAILLE_MAX + " : " + taille);
        }
        return repository.duObjet(objetId, PageRequest.of(page, taille, ORDRE)).map(AuditService::versDto);
    }

    private static LigneAuditDto versDto(AdmWebHistorics h) {
        return new LigneAuditDto(h.getId(), h.getActionDateTime(), h.getUserDescription(), h.getAction(),
                h.getObjectType(), h.getObjectTypeLabel(), h.getObjectLabel(), h.getParentObjectType(),
                DetailsAudit.lire(h.getDetails()).stream()
                        .map(c -> new ChangementAuditDto(c.propriete(), c.ancienneValeur(), c.nouvelleValeur()))
                        .toList());
    }
}
