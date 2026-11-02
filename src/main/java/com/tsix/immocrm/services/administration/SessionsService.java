package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.LigneSessionDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SessionsService {

    public PageDto<LigneSessionDto> lister(FiltresSessions filtres, int page, int size) {
        throw new UnsupportedOperationException();
    }

    public List<String> utilisateurs() {
        throw new UnsupportedOperationException();
    }

    public LigneSessionDto fermer(UUID id) {
        throw new UnsupportedOperationException();
    }
}
