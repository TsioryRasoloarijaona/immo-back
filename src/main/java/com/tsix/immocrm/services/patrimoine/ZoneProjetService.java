package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ZoneProjetDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectZoneRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_LONGUE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

/** Zones d'un programme (FEATURE-patrimoine-tranche-fiche.md §3.3), règles de {@code CfgProjectZone} d'OMRAN. */
@Service
@Transactional(readOnly = true)
public class ZoneProjetService {

    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_PARENT = "parentId";

    private final CfgProjectRepository projectRepository;
    private final CfgProjectZoneRepository zoneRepository;
    private final ReferencesBloquantes referencesBloquantes;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ZoneProjetService(CfgProjectRepository projectRepository, CfgProjectZoneRepository zoneRepository,
                             ReferencesBloquantes referencesBloquantes, UtilisateurAffiche utilisateurAffiche,
                             Clock clock) {
        this.projectRepository = projectRepository;
        this.zoneRepository = zoneRepository;
        this.referencesBloquantes = referencesBloquantes;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public List<ZoneProjetDto> findAll(UUID projetId) {
        programme(projetId);
        return zoneRepository.duProgramme(projetId).stream().map(ZoneProjetService::toDto).toList();
    }

    @Transactional
    public ZoneProjetDto create(UUID projetId, ZoneProjetEcritureDto demande) {
        CfgProject projet = programme(projetId);
        CfgProjectZone zone = new CfgProjectZone();
        zone.setId(UUID.randomUUID());
        zone.setCfgProject(projet);
        zone.setLeveling(0);
        if (demande.parentId() != null) {
            CfgProjectZone parent = zoneRepository.findByIdAndCfgProjectId(demande.parentId(), projetId)
                    .orElseThrow(() -> new ChampInvalideException(CHAMP_PARENT,
                            "Zone parente introuvable dans ce programme : " + demande.parentId()));
            zone.setParentId(parent.getId());
            zone.setLeveling(parent.getLeveling() == null ? 1 : parent.getLeveling() + 1);
        }
        zone.setCode(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"));
        zone.setDescription(ReglesSaisie.requis(demande.description(), "description", "Description"));
        zone.setNotes("");
        texte(demande.notes(), LONGUEUR_LONGUE, "notes", zone::setNotes);
        zone.setSorting(demande.ordre() != null ? demande.ordre() : rangSuivant(projetId, zone.getParentId()));
        verifierUnicite(zone);

        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        zone.setUserCreate(auteur.nom());
        zone.setUserCreateDateTime(maintenant);
        zone.setUserUpdate(auteur.nom());
        zone.setUserUpdateDateTime(maintenant);
        return toDto(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneProjetDto update(UUID projetId, UUID zoneId, ZoneProjetEcritureDto demande) {
        CfgProjectZone zone = zone(projetId, zoneId);
        if (demande.parentId() != null && !demande.parentId().equals(zone.getParentId())) {
            throw new ChampInvalideException(CHAMP_PARENT, "La zone parente ne se modifie pas.");
        }
        if (demande.code() != null) {
            zone.setCode(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"));
        }
        if (demande.description() != null) {
            zone.setDescription(ReglesSaisie.requis(demande.description(), "description", "Description"));
        }
        texte(demande.notes(), LONGUEUR_LONGUE, "notes", zone::setNotes);
        ReglesSaisie.si(demande.ordre(), zone::setSorting);
        verifierUnicite(zone);

        zone.setUserUpdate(utilisateurAffiche.courant().nom());
        zone.setUserUpdateDateTime(LocalDateTime.now(clock));
        return toDto(zoneRepository.save(zone));
    }

    @Transactional
    public void delete(UUID projetId, UUID zoneId) {
        CfgProjectZone zone = zone(projetId, zoneId);
        List<String> bloquantes = referencesBloquantes.compter(ReferencesBloquantes.ZONE, zoneId);
        if (!bloquantes.isEmpty()) {
            throw new ConflictException("La zone « %s » ne peut pas être supprimée : elle porte %s."
                    .formatted(zone.getCode(), String.join(", ", bloquantes)));
        }
        zoneRepository.delete(zone);
    }

    private CfgProject programme(UUID projetId) {
        return projectRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable: " + projetId));
    }

    private CfgProjectZone zone(UUID projetId, UUID zoneId) {
        programme(projetId);
        return zoneRepository.findByIdAndCfgProjectId(zoneId, projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable: " + zoneId));
    }

    private int rangSuivant(UUID projetId, UUID parentId) {
        long soeurs = parentId == null
                ? zoneRepository.countByCfgProjectIdAndParentIdIsNull(projetId)
                : zoneRepository.countByCfgProjectIdAndParentId(projetId, parentId);
        return (int) soeurs + 1;
    }

    private void verifierUnicite(CfgProjectZone zone) {
        List<CfgProjectZone> homonymes = zoneRepository.homonymes(
                zone.getCfgProject().getId(), zone.getParentId(), zone.getCode(), zone.getId());
        if (!homonymes.isEmpty()) {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par la zone « %s »."
                    .formatted(zone.getCode(), homonymes.getFirst().getDescription()));
        }
    }

    private static ZoneProjetDto toDto(CfgProjectZone z) {
        return new ZoneProjetDto(z.getId(), z.getParentId(), z.getCode(), z.getDescription(), z.getNotes(),
                z.getLeveling(), z.getSorting(), Boolean.TRUE.equals(z.getAUneImage()),
                z.getUserCreate(), z.getUserCreateDateTime(), z.getUserUpdate(), z.getUserUpdateDateTime());
    }
}
