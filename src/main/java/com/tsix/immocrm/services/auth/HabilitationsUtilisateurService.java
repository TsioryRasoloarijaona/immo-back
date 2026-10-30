package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.HabilitationsEcritureDto;
import com.tsix.immocrm.dto.auth.NoeudHabilitationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HabilitationsUtilisateurService {

    public List<NoeudHabilitationDto> arbre(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<NoeudHabilitationDto> remplacer(UUID id, HabilitationsEcritureDto corps) {
        throw new UnsupportedOperationException();
    }
}
