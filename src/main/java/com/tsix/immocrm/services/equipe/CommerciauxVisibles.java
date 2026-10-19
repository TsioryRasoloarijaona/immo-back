package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Commerciaux (Commercial, Commercial Aide, Commercial Chef) dont l'appelant peut suivre l'activité
 * (FEATURE-suivi-commerciaux.md R8, §7.3) : tous pour un rôle exempté, son équipe et lui-même pour un chef, lui seul sinon.
 */
@Component
public class CommerciauxVisibles {

    private final EquipeCommercialeService equipeService;
    private final PerimetreCommercialService perimetreService;

    public CommerciauxVisibles(EquipeCommercialeService equipeService, PerimetreCommercialService perimetreService) {
        this.equipeService = equipeService;
        this.perimetreService = perimetreService;
    }

    /** Clé du commercial sur le fil : son employé, sinon son compte (§9). */
    public static UUID cle(MembreEquipe m) {
        return m.employeId() != null ? m.employeId() : m.admUserId();
    }

    /**
     * Les commerciaux visibles, restreints aux clés {@code demandees} si elles sont données.
     *
     * @throws ForbiddenException une clé demandée hors de portée (403)
     */
    public List<MembreEquipe> retenus(Collection<UUID> demandees) {
        List<MembreEquipe> visibles = visibles();
        if (CollectionUtils.isEmpty(demandees)) {
            return visibles;
        }
        Set<UUID> cles = visibles.stream().map(CommerciauxVisibles::cle).collect(Collectors.toSet());
        demandees.stream().filter(c -> !cles.contains(c)).findFirst().ifPresent(c -> {
            throw new ForbiddenException("Commercial hors de votre portée : " + c);
        });
        return visibles.stream().filter(m -> demandees.contains(cle(m))).toList();
    }

    private List<MembreEquipe> visibles() {
        UUID appelant = PerimetreCommercialService.admUserIdCourant()
                .orElseThrow(() -> new ForbiddenException("Compte applicatif non identifiable."));
        List<MembreEquipe> commerciaux = equipeService.comptesActifs(RolesEquipe::estCommercialOuChef);
        if (perimetreService.estExempte(appelant)) {
            return commerciaux;
        }
        boolean chef = commerciaux.stream()
                .anyMatch(m -> m.admUserId().equals(appelant) && m.roles().contains(RolesEquipe.CHEF));
        Set<UUID> equipe = chef
                ? equipeService.perimetre().membres().stream().map(MembreEquipe::admUserId).collect(Collectors.toSet())
                : Set.of();
        return commerciaux.stream()
                .filter(m -> m.admUserId().equals(appelant) || equipe.contains(m.admUserId()))
                .toList();
    }
}
