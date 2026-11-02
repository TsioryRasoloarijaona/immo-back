package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.LigneActionDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.stereotype.Service;

@Service
public class ActionsService {

    public PageDto<LigneActionDto> lister(FiltresActions filtres, int page, int size) {
        throw new UnsupportedOperationException();
    }
}
