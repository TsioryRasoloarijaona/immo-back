package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seule recherche d'un programme par id pour les routes {@code /api/projets/{id}…} : un programme d'une entité
 * inactive est masqué (404, administrateur compris — FEATURE-patrimoine-tranche-fiche.md §7).
 */
@Component
public class ProgrammeVisible {

    static final String PROJET_INTROUVABLE = "Projet introuvable: ";

    private final CfgProjectRepository projectRepository;

    public ProgrammeVisible(CfgProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    CfgProject trouver(UUID id) {
        return projectRepository.findDUneEntiteActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROJET_INTROUVABLE + id));
    }
}

