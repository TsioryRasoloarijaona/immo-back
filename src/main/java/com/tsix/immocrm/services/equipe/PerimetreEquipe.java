package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Périmètre de l'appelant de {@code /api/equipe/*} ({@link EquipeCommercialeService#perimetre()}) : tous les contrôles
 * d'accès de l'écran passent par ses méthodes. Exempté = Administrateur / Direction Générale (FEATURE-equipe-commerciale.md §1).
 */
public record PerimetreEquipe(UUID appelantId, boolean exempte, List<ProjetPerimetreDto> projets,
                              Set<UUID> projetIds, List<MembreEquipe> membres,
                              // Commerciaux actifs hors équipe qu'un chef peut ajouter à ses projets ; vide pour un exempté.
                              List<MembreEquipe> horsEquipe) {

    static final String HORS_PERIMETRE = " : ce projet n'est pas dans votre équipe.";

    /**
     * Projets sur lesquels filtrer une lecture : {@code null} = aucune restriction (exempté sans filtre) ; liste
     * vide = rien à lire. Un {@code projet} demandé hors du périmètre d'un chef ⇒ 403.
     */
    public @Nullable List<UUID> projetsFiltres(@Nullable UUID projet) {
        if (projet != null) {
            verifierProjet(projet, "Projet demandé");
            return List.of(projet);
        }
        return exempte ? null : List.copyOf(projetIds);
    }

    /** 403 si le projet n'est pas à ce chef ; un projet {@code null} n'est à aucun chef. */
    public void verifierProjet(@Nullable UUID projetId, String quoi) {
        if (!exempte && (projetId == null || !projetIds.contains(projetId))) {
            throw new ForbiddenException(quoi + HORS_PERIMETRE);
        }
    }

    /**
     * Le membre qui recevra une ligne du projet {@code projetId} : 409 si {@code commercialId} (CfgTier) n'est pas
     * dans l'équipe, ou n'y est pas affecté au projet de la ligne. Un employé peut porter deux comptes.
     */
    public MembreEquipe destinataire(UUID commercialId, @Nullable UUID projetId, String ligne) {
        List<MembreEquipe> comptes = membres.stream()
                .filter(m -> commercialId != null && commercialId.equals(m.employeId()))
                .toList();
        if (comptes.isEmpty()) {
            throw new ConflictException("Le commercial demandé ne fait pas partie de votre équipe.");
        }
        return comptes.stream()
                .filter(m -> m.peutRecevoir(projetId))
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "Le commercial " + comptes.getFirst().nom() + " n'est pas affecté au projet de la ligne " + ligne + "."));
    }
}
