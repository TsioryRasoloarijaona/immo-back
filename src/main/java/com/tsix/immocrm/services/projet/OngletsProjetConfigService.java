package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ChoixProjetDto;
import com.tsix.immocrm.dto.projet.CritereAvancementDto;
import com.tsix.immocrm.dto.projet.CritereAvancementEcritureDto;
import com.tsix.immocrm.dto.projet.ImportCriteresDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheEcritureDto;
import com.tsix.immocrm.entities.stl.StlCategory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OngletsProjetConfigService {

    public ChoixProjetDto choix(UUID id) {
        throw new UnsupportedOperationException();
    }

    /** Catégorie de dépense ({@code CategoryType.DEBIT} d'OMRAN) : tout ce qui n'est pas de sens crédit. */
    static boolean estDepense(StlCategory c) {
        return !Boolean.TRUE.equals(c.getIsCredit());
    }

    public List<VentilationTrancheDto> ventilations(UUID id) {
        throw new UnsupportedOperationException();
    }

    public VentilationTrancheDto ajouterVentilation(UUID id, VentilationTrancheEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    public VentilationTrancheDto modifierVentilation(UUID id, UUID ligneId, VentilationTrancheEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimerVentilation(UUID id, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public List<CritereAvancementDto> criteres(UUID id) {
        throw new UnsupportedOperationException();
    }

    public CritereAvancementDto ajouterCritere(UUID id, CritereAvancementEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    public CritereAvancementDto modifierCritere(UUID id, UUID ligneId, CritereAvancementEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimerCritere(UUID id, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public List<CritereAvancementDto> importerCriteres(UUID id, ImportCriteresDto saisie) {
        throw new UnsupportedOperationException();
    }
}
