package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjetService {

    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;

    public ProjetService(CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository) {
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
    }

    public List<ProjetListItemDto> findAll() {
        return projectRepository.findAll().stream()
                .map(ProjetService::toListItem)
                .toList();
    }

    public ProjetDetailDto findById(UUID id) {
        return projectRepository.findById(id)
                .map(ProjetService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable: " + id));
    }

    public List<TrancheListItemDto> findTranches(UUID projetId) {
        if (!projectRepository.existsById(projetId)) {
            throw new ResourceNotFoundException("Projet introuvable: " + projetId);
        }
        return trancheRepository.findByCfgProjectId(projetId).stream()
                .map(ProjetService::toTrancheListItem)
                .toList();
    }

    private static ProjetListItemDto toListItem(CfgProject project) {
        return new ProjetListItemDto(
                project.getId(),
                project.getCode(),
                project.getDescription(),
                project.getCity()
        );
    }

    private static ProjetDetailDto toDetail(CfgProject project) {
        return new ProjetDetailDto(
                project.getId(),
                project.getCode(),
                project.getDescription(),
                project.getDescriptionLong(),
                project.getAddress(),
                project.getAddress2(),
                project.getAddress3(),
                project.getZipCode(),
                project.getCity(),
                project.getSignatureCity(),
                project.getPhone(),
                project.getMobile(),
                project.getFax(),
                project.getEmail(),
                project.getWebSite(),
                project.getWorkStartDate(),
                project.getWorkEndDate(),
                project.getMarketingStartDate(),
                project.getMarketingEndDate()
        );
    }

    private static TrancheListItemDto toTrancheListItem(CfgTranche tranche) {
        return new TrancheListItemDto(
                tranche.getId(),
                tranche.getCode(),
                tranche.getDescription(),
                tranche.getCity(),
                tranche.getDeliveryDate()
        );
    }
}
