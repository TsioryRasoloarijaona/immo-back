package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Suppression d'un programme (FEATURE-patrimoine-tranche-fiche.md §3.4) : refusée (409) tant qu'une ligne le
 * référence ; sinon, dans la même transaction, les cascades d'OMRAN (habilitations, zones, pièces jointes, messages).
 */
@Service
public class SuppressionProjetService {

    private final CfgProjectRepository projectRepository;
    private final CfgProjectZoneRepository zoneRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final AdmAttachedFileRepository pieceJointeRepository;
    private final ReferencesBloquantes referencesBloquantes;

    public SuppressionProjetService(CfgProjectRepository projectRepository, CfgProjectZoneRepository zoneRepository,
                                    AdmUserHabilitationRepository habilitationRepository,
                                    AdmAttachedFileRepository pieceJointeRepository,
                                    ReferencesBloquantes referencesBloquantes) {
        this.projectRepository = projectRepository;
        this.zoneRepository = zoneRepository;
        this.habilitationRepository = habilitationRepository;
        this.pieceJointeRepository = pieceJointeRepository;
        this.referencesBloquantes = referencesBloquantes;
    }

    @Transactional
    public void delete(UUID id) {
        CfgProject projet = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ProjetService.PROJET_INTROUVABLE + id));
        List<String> bloquantes = referencesBloquantes.compter(ReferencesBloquantes.PROGRAMME, id);
        if (!bloquantes.isEmpty()) {
            throw new ConflictException("Le programme « %s » ne peut pas être supprimé : il est référencé par %s."
                    .formatted(projet.getCode(), String.join(", ", bloquantes)));
        }
        habilitationRepository.deleteAll(habilitationRepository.findByCfgProjectId(id));
        zoneRepository.deleteAll(zoneRepository.duProgramme(id));
        pieceJointeRepository.deleteAll(pieceJointeRepository.findByCategoryId(id));
        referencesBloquantes.supprimerMessages(id);
        projectRepository.delete(projet);
    }
}
