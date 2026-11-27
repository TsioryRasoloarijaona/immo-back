package com.tsix.immocrm.services.remise;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import com.tsix.immocrm.dto.remise.AccepterRemiseRequest;
import com.tsix.immocrm.dto.remise.DemandeRemiseDto;
import com.tsix.immocrm.dto.remise.DemanderRemiseRequest;
import com.tsix.immocrm.dto.remise.RefuserRemiseRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Demandes de remise des agences — voir FEATURE-agences-vente.md (A3). */
@Service
public class DemandesRemiseService {

    public DemandeRemiseDto demander(UUID dossierId, DemanderRemiseRequest requete) {
        throw new UnsupportedOperationException();
    }

    public List<DemandeRemiseDto> historique(UUID dossierId) {
        throw new UnsupportedOperationException();
    }

    public List<DemandeRemiseDto> file(StatutDemandeRemise statut) {
        throw new UnsupportedOperationException();
    }

    public DemandeRemiseDto accepter(UUID demandeId, AccepterRemiseRequest requete) {
        throw new UnsupportedOperationException();
    }

    public DemandeRemiseDto refuser(UUID demandeId, RefuserRemiseRequest requete) {
        throw new UnsupportedOperationException();
    }

    public DemandeRemiseDto annuler(UUID demandeId) {
        throw new UnsupportedOperationException();
    }
}
