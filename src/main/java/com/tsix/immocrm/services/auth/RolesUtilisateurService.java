package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.ComptesDepotEcritureDto;
import com.tsix.immocrm.dto.auth.RolesUtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RolesUtilisateurService {

    public UtilisateurFicheDto remplacerRoles(UUID id, RolesUtilisateurEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public UtilisateurFicheDto remplacerComptesDepot(UUID id, ComptesDepotEcritureDto corps) {
        throw new UnsupportedOperationException();
    }
}
