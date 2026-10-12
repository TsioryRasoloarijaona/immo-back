package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seule règle de visibilité de l'écran « Entités juridiques » (FEATURE-entites-juridiques.md §2) : actives ou non,
 * mais seulement celles où l'appelant a une habilitation société ({@code CfgCompanyView.GetList}), sauf rôle exempté
 * ({@code AdmUserRole}). Appelant sans compte : rien. Liste et routes par id passent toutes ici.
 */
@Component
public class EntiteJuridiqueVisible {

    static final String ENTITE_INTROUVABLE = "Entité juridique introuvable: ";

    private record Portee(boolean tous, UUID compteId) {
    }

    private final CfgCompanyRepository repository;
    private final PerimetreCommercialService perimetre;

    public EntiteJuridiqueVisible(CfgCompanyRepository repository, PerimetreCommercialService perimetre) {
        this.repository = repository;
        this.perimetre = perimetre;
    }

    public CfgCompany trouver(UUID id) {
        return chercher(id).orElseThrow(() -> new ResourceNotFoundException(ENTITE_INTROUVABLE + id));
    }

    public boolean existe(UUID id) {
        return chercher(id).isPresent();
    }

    List<CfgCompany> rechercher(Boolean actif, String motif) {
        Portee portee = portee();
        return repository.rechercherHabilitees(portee.tous(), portee.compteId(), actif, motif);
    }

    private Optional<CfgCompany> chercher(UUID id) {
        Portee portee = portee();
        return repository.trouverHabilitee(id, portee.tous(), portee.compteId());
    }

    private Portee portee() {
        UUID compteId = PerimetreCommercialService.admUserIdCourant().orElse(null);
        return new Portee(compteId != null && perimetre.estExempte(compteId), compteId);
    }
}
