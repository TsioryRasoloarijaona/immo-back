package com.tsix.immocrm.services.audit;

import com.tsix.immocrm.domain.audit.DetailsAudit;
import com.tsix.immocrm.domain.audit.RegistreAuditOmran;
import com.tsix.immocrm.dto.audit.ChangementAuditDto;
import com.tsix.immocrm.dto.audit.LigneAuditDto;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Écran « Audit » d'une fiche (FEATURE-patrimoine-audit-actions.md §2.3, §10), lu sur la copie locale. */
@Service
@Transactional(readOnly = true)
public class AuditService {

    public static final int TAILLE_PAR_DEFAUT = 50;
    public static final int TAILLE_MAX = 200;

    // Second critère : ordre total, sinon deux lignes du même instant changent de page d'un appel à l'autre.
    private static final Sort ORDRE = Sort.by(Sort.Order.desc("actionDateTime"), Sort.Order.desc("id"));

    private final AdmWebHistoricsRepository repository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository articleRepository;
    private final CfgCompanyRepository companyRepository;

    public AuditService(AdmWebHistoricsRepository repository, CfgProjectRepository projectRepository,
                        CfgTrancheRepository trancheRepository, StkItemRepository articleRepository,
                        CfgCompanyRepository companyRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.articleRepository = articleRepository;
        this.companyRepository = companyRepository;
    }

    public Page<LigneAuditDto> lignes(UUID objetId, int page, int taille) {
        if (page < 0) {
            throw new InvalidRequestException("page doit être positive ou nulle : " + page);
        }
        if (taille < 1 || taille > TAILLE_MAX) {
            throw new InvalidRequestException("size doit être compris entre 1 et " + TAILLE_MAX + " : " + taille);
        }
        verifierFicheLisible(objetId);
        return repository.duObjet(objetId, RegistreAuditOmran.objectTypes(), PageRequest.of(page, taille, ORDRE))
                .map(AuditService::versDto);
    }

    /**
     * Seules les fiches du registre ont un journal, et il est lisible exactement quand leur {@code GET …/{id}} l'est
     * (simple existence aujourd'hui). Autre id : 404, comme une fiche absente.
     */
    private void verifierFicheLisible(UUID objetId) {
        boolean fiche = projectRepository.existsById(objetId)
                || trancheRepository.existsById(objetId)
                || articleRepository.existsById(objetId)
                || companyRepository.existsById(objetId);
        if (!fiche) {
            throw new ResourceNotFoundException("Fiche introuvable: " + objetId);
        }
    }

    private static LigneAuditDto versDto(AdmWebHistorics h) {
        return new LigneAuditDto(h.getId(), h.getActionDateTime(), h.getUserDescription(), h.getAction(),
                h.getObjectType(), h.getObjectTypeLabel(), h.getObjectLabel(), h.getParentObjectType(),
                DetailsAudit.lire(h.getDetails()).stream()
                        .map(c -> new ChangementAuditDto(c.propriete(), c.ancienneValeur(), c.nouvelleValeur()))
                        .toList());
    }
}
