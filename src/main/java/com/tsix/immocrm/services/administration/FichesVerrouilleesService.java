package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.EtatFicheVerrouillee;
import com.tsix.immocrm.dto.administration.LigneFicheVerrouilleeDto;
import com.tsix.immocrm.dto.administration.ResultatDeverrouillageDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmWebLockEntity;
import com.tsix.immocrm.entities.adm.AdmWebSessions;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmWebLockEntityRepository;
import com.tsix.immocrm.repositories.adm.AdmWebSessionsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Verrous OMRAN : liste et déverrouillage, sans trace d'audit comme OMRAN. FEATURE-admin-fiches-verrouillees.md. */
@Service
public class FichesVerrouilleesService {

    private final AdmWebLockEntityRepository repository;
    private final AdmWebSessionsRepository sessions;

    public FichesVerrouilleesService(AdmWebLockEntityRepository repository, AdmWebSessionsRepository sessions) {
        this.repository = repository;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneFicheVerrouilleeDto> lister(FiltresFichesVerrouillees filtres, int page, int size) {
        filtres.verifierPeriode();
        return PageDto.of(repository.lignes(filtres.stateEgal(), filtres.stateExclu(), filtres.debut(),
                filtres.finExclue(), filtres.motif(), PageRequest.of(page, size)));
    }

    @Transactional
    public LigneFicheVerrouilleeDto deverrouiller(UUID id) {
        AdmWebLockEntity fiche = repository.findPourEcriture(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche verrouillée introuvable : " + id));
        if (EtatFicheVerrouillee.depuis(fiche.getState()) != EtatFicheVerrouillee.VERROUILLEE) {
            throw new ConflictException("Fiche déjà déverrouillée.");
        }
        fiche.setState(EtatFicheVerrouillee.NON_VERROUILLEE.state());
        String utilisateur = sessions.findById(fiche.getSessionId()).map(AdmWebSessions::getUserDesc).orElse(null);
        return new LigneFicheVerrouilleeDto(fiche.getId(), fiche.getCode(), fiche.getDescription(), fiche.getLockDate(),
                utilisateur, fiche.getState());
    }

    // Entités une à une, pas de @Modifying : il échapperait au collecteur Hibernate et la table n'est pas suivie
    // par Change Tracking, la copie de lecture resterait à State = 1.
    @Transactional
    public ResultatDeverrouillageDto deverrouillerTout() {
        List<AdmWebLockEntity> verrous = repository.findByState(EtatFicheVerrouillee.VERROUILLEE.state());
        verrous.forEach(fiche -> fiche.setState(EtatFicheVerrouillee.NON_VERROUILLEE.state()));
        return new ResultatDeverrouillageDto(verrous.size());
    }
}
