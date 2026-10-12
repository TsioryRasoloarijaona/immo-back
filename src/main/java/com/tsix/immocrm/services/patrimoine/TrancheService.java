package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TrancheService {

    private static final String TRANCHE_INTROUVABLE = "Tranche introuvable: ";
    private static final String CHAMP_PROJET = "projetId";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";

    private final CfgTrancheRepository trancheRepository;
    private final CfgProjectRepository projectRepository;
    private final ReferencesFiche references;
    private final HabilitationCreateur habilitationCreateur;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public TrancheService(CfgTrancheRepository trancheRepository, CfgProjectRepository projectRepository,
                          ReferencesFiche references, HabilitationCreateur habilitationCreateur,
                          UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.trancheRepository = trancheRepository;
        this.projectRepository = projectRepository;
        this.references = references;
        this.habilitationCreateur = habilitationCreateur;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Tranches des entités actives ; {@code colonnes} décide des colonnes coûteuses calculées. */
    public Page<TrancheListItemDto> findAll(UUID projetId, String recherche, Set<ColonneTranche> colonnes,
                                            Pageable pageable) {
        return trancheRepository.rechercherDesEntitesActives(projetId, ReglesSaisie.motif(recherche), pageable)
                .map(t -> VueTranche.ligne(t, colonnes));
    }

    /** Tranches d'un programme ({@code GET /api/projets/{id}/tranches}) : toutes colonnes, la page est petite. */
    static TrancheListItemDto toListItem(CfgTranche tranche) {
        return VueTranche.ligne(tranche, EnumSet.allOf(ColonneTranche.class));
    }

    public TrancheDetailDto findById(UUID id) {
        return trancheRepository.findById(id)
                .map(VueTranche::fiche)
                .orElseThrow(() -> new ResourceNotFoundException(TRANCHE_INTROUVABLE + id));
    }

    @Transactional
    public TrancheDetailDto create(TrancheEcritureDto demande) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        UUID projetId = ReglesSaisie.requis(demande.projetId(), CHAMP_PROJET, "Programme");
        tranche.setCfgProject(projectRepository.findById(projetId)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_PROJET, "Programme introuvable : " + projetId)));
        tranche.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), tranche.getId()));
        tranche.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        SaisieTranche.initialiserDefauts(tranche);
        appliquerFacultatifs(tranche, demande);

        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        tranche.setUserCreate(auteur.nom());
        tranche.setUserCreateDateTime(maintenant);
        tranche.setUserUpdate(auteur.nom());
        tranche.setUserUpdateDateTime(maintenant);
        CfgTranche enregistree = trancheRepository.save(tranche);
        habilitationCreateur.surTranche(auteur.compte(), enregistree);
        return VueTranche.fiche(enregistree);
    }

    @Transactional
    public TrancheDetailDto update(UUID id, TrancheEcritureDto demande) {
        CfgTranche tranche = trancheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANCHE_INTROUVABLE + id));
        UUID projetActuel = Champs.de(tranche.getCfgProject(), CfgProject::getId);
        if (demande.projetId() != null && !demande.projetId().equals(projetActuel)) {
            throw new ChampInvalideException(CHAMP_PROJET, "Le programme d'une tranche ne se modifie pas.");
        }
        if (demande.code() != null) {
            tranche.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            tranche.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        }
        appliquerFacultatifs(tranche, demande);

        tranche.setUserUpdate(utilisateurAffiche.courant().nom());
        tranche.setUserUpdateDateTime(LocalDateTime.now(clock));
        return VueTranche.fiche(trancheRepository.save(tranche));
    }

    private void appliquerFacultatifs(CfgTranche t, TrancheEcritureDto d) {
        references.pays(d.paysId(), "paysId", t::setAdmCountry);
        references.standing(d.standingId(), "standingId", t::setCfgTrancheStanding);
        references.niveau(d.niveauTerrainId(), "niveauTerrainId", t::setLndLevel);
        references.rubrique(d.rubriqueId(), "rubriqueId", t::setStkTrancheHeading);
        references.tiers(d.responsableSavId(), "responsableSavId", TypeTiersPatrimoine.PERSONNEL,
                t::setCfgClaimsManager);
        references.tiers(d.tiersAutruiId(), "tiersAutruiId", TypeTiersPatrimoine.FOURNISSEUR, t::setCfgTierOther);
        references.projetChantier(d.projetFacturationId(), "projetFacturationId", t::setPrjProject);
        references.compte(d.planComptableId(), "planComptableId", t::setActAccount);
        SaisieTranche.appliquer(t, d);
    }

    private String codeLibre(String code, UUID id) {
        trancheRepository.findFirstByCodeAndIdNot(code, id).ifPresent(existante -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par la tranche « %s »."
                    .formatted(existante.getCode(), existante.getDescription()));
        });
        return code;
    }
}
