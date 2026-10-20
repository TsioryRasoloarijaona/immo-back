package com.tsix.immocrm.services.patrimoine.plans;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PurgePlans {

    @Transactional
    public void purger(UUID trancheId) {
        throw new UnsupportedOperationException();
    }
}
