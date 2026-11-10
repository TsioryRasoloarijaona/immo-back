package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConfigurationAchatService {

    public List<StatutAchatConfigDto> statuts() {
        throw new UnsupportedOperationException();
    }

    public List<TransitionAchatDto> transitions(boolean utilisees) {
        throw new UnsupportedOperationException();
    }
}
