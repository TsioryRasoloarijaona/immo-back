package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.UtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import com.tsix.immocrm.dto.auth.UtilisateurReferencesDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UtilisateursService {

    public UtilisateurReferencesDto references() {
        throw new UnsupportedOperationException();
    }

    public UtilisateurFicheDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public UtilisateurFicheDto creer(UtilisateurEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public UtilisateurFicheDto modifier(UUID id, UtilisateurEcritureDto corps) {
        throw new UnsupportedOperationException();
    }
}
