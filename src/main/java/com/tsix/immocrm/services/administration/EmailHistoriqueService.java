package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.EmailDetailDto;
import com.tsix.immocrm.dto.administration.LigneEmailDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmailHistoriqueService {

    public PageDto<LigneEmailDto> lister(FiltresEmail filtres, int page, int size) {
        throw new UnsupportedOperationException();
    }

    public EmailDetailDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<String> utilisateurs() {
        throw new UnsupportedOperationException();
    }
}
