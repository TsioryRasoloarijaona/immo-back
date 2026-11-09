package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ProjetConfigDto;
import com.tsix.immocrm.dto.projet.ProjetConfigRequest;
import com.tsix.immocrm.dto.projet.ReferentielsProjetConfigDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Configuration Projet (FEATURE-projet-config.md). */
@Service
@Transactional(readOnly = true)
public class ProjetConfigService {

    public List<ProjetConfigDto> lister() {
        throw new UnsupportedOperationException();
    }

    public ProjetConfigDto lire(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ProjetConfigDto creer(ProjetConfigRequest saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ProjetConfigDto modifier(UUID id, ProjetConfigRequest saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public ReferentielsProjetConfigDto referentiels() {
        throw new UnsupportedOperationException();
    }

    public List<StatutLotDto> statutsLot() {
        throw new UnsupportedOperationException();
    }
}
