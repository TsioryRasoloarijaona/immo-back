package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.referentiel.DescripteurReferentielDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Référentiels administrables (FEATURE-patrimoine-referentiels.md §1) : une implémentation pour les 19 tables. */
@Service
@Transactional(readOnly = true)
public class ReferentielAdminService {

    public List<DescripteurReferentielDto> descripteurs() {
        throw new UnsupportedOperationException();
    }

    public List<ReferentielItemDto> choix(String liste) {
        throw new UnsupportedOperationException(liste);
    }

    public List<Map<String, Object>> lignes(String slug, String search, UUID trancheId, boolean global) {
        throw new UnsupportedOperationException(slug + search + trancheId + global);
    }

    public Map<String, Object> ligne(String slug, UUID id) {
        throw new UnsupportedOperationException(slug + id);
    }

    @Transactional
    public Map<String, Object> creer(String slug, Map<String, Object> corps) {
        throw new UnsupportedOperationException(slug + corps);
    }

    @Transactional
    public Map<String, Object> modifier(String slug, UUID id, Map<String, Object> corps) {
        throw new UnsupportedOperationException(slug + id + corps);
    }

    @Transactional
    public void supprimer(String slug, UUID id) {
        throw new UnsupportedOperationException(slug + id);
    }
}
