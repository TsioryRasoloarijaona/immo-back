package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seule lecture par id de l'écran Commissions (C8) : tranche d'une entité active et du périmètre commercial, ligne
 * d'une telle tranche ; sinon 404, jamais 403.
 */
@Component
public class CommissionsVisibles {

    static final String LIGNE_INTROUVABLE = "Ligne de commission introuvable: ";

    private final TrancheVisibleRepository tranches;
    private final PerimetreCommercialService perimetre;
    private final ComCommissionLineRepository lignes;

    public CommissionsVisibles(TrancheVisibleRepository tranches, PerimetreCommercialService perimetre,
                               ComCommissionLineRepository lignes) {
        this.tranches = tranches;
        this.perimetre = perimetre;
        this.lignes = lignes;
    }

    public CfgTranche tranche(UUID id) {
        return visible(id).orElseThrow(() -> new ResourceNotFoundException("Tranche introuvable: " + id));
    }

    /** Vide si la tranche est inconnue, d'une entité inactive ou hors du périmètre commercial (projet NULL visible). */
    public Optional<CfgTranche> visible(UUID id) {
        Optional<List<UUID>> projets = perimetre.perimetreCourant();
        return tranches.trancheVisible(id).filter(t -> {
            UUID projet = Champs.de(t.getCfgProject(), CfgProject::getId);
            return projets.isEmpty() || projet == null || projets.get().contains(projet);
        });
    }

    public ComCommissionLine ligne(UUID id) {
        return lignes(List.of(id)).get(0);
    }

    /** Lignes dans l'ordre demandé, doublons retirés ; une seule inconnue ou masquée = 404. */
    public List<ComCommissionLine> lignes(Collection<UUID> ids) {
        List<UUID> uniques = ids.stream().distinct().toList();
        Map<UUID, ComCommissionLine> trouvees = lignes.findAllById(uniques).stream()
                .collect(Collectors.toMap(ComCommissionLine::getId, Function.identity()));
        Set<UUID> tranchesVues = new HashSet<>();
        List<ComCommissionLine> resultat = new ArrayList<>();
        for (UUID id : uniques) {
            ComCommissionLine l = trouvees.get(id);
            if (l == null || l.getCfgTrancheId() == null) {
                throw new ResourceNotFoundException(LIGNE_INTROUVABLE + id);
            }
            if (tranchesVues.add(l.getCfgTrancheId())) {
                tranche(l.getCfgTrancheId());
            }
            resultat.add(l);
        }
        return resultat;
    }
}
