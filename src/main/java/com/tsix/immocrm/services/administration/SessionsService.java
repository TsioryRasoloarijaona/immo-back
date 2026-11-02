package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.EtatSession;
import com.tsix.immocrm.dto.administration.LigneSessionDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmWebSessions;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmWebSessionsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Sessions OMRAN : liste et fermeture, sans trace d'audit comme OMRAN. FEATURE-admin-sessions.md. */
@Service
public class SessionsService {

    private static final Sort TRI = Sort.by(Sort.Order.desc("startDate"), Sort.Order.asc("id"));

    private final AdmWebSessionsRepository repository;
    private final Clock clock;

    public SessionsService(AdmWebSessionsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneSessionDto> lister(FiltresSessions filtres, int page, int size) {
        return PageDto.of(repository.findAll(filtres.specification(), PageRequest.of(page, size, TRI))
                .map(SessionsService::ligne));
    }

    @Transactional(readOnly = true)
    public List<String> utilisateurs() {
        return repository.utilisateurs();
    }

    @Transactional
    public LigneSessionDto fermer(UUID id) {
        AdmWebSessions session = repository.findPourEcriture(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + id));
        if (EtatSession.depuis(session.getState()) == EtatSession.FERMEE) {
            throw new ConflictException("Session déjà fermée.");
        }
        session.setState(EtatSession.FERMEE.state());
        session.setEndDate(LocalDateTime.now(clock));
        return ligne(session);
    }

    private static LigneSessionDto ligne(AdmWebSessions session) {
        return new LigneSessionDto(session.getId(), session.getUserDesc(), session.getStartDate(), session.getEndDate(),
                EtatSession.depuis(session.getState()), session.getIpAddress(), session.getSystemVersion());
    }
}
