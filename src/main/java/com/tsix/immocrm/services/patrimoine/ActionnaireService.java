package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ActionnaireDto;
import com.tsix.immocrm.dto.patrimoine.ActionnaireEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderLineRepository;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Configuration › Autres › Actionnaires (FEATURE-entites-juridiques.md §2) : Code O U, Description O, Notes. */
@Service
@Transactional(readOnly = true)
public class ActionnaireService {

    static final String ACTIONNAIRE_INTROUVABLE = "Actionnaire introuvable: ";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";

    private final CfgShareholderRepository repository;
    private final CfgCompanyShareholderRepository repartitionRepository;
    private final CfgGroupShareholderLineRepository ligneGroupeRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ActionnaireService(CfgShareholderRepository repository,
                              CfgCompanyShareholderRepository repartitionRepository,
                              CfgGroupShareholderLineRepository ligneGroupeRepository,
                              UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.repository = repository;
        this.repartitionRepository = repartitionRepository;
        this.ligneGroupeRepository = ligneGroupeRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public List<ActionnaireDto> lister(String recherche) {
        return repository.rechercher(ReglesSaisie.motifSansEspaces(recherche)).stream()
                .map(ActionnaireService::versDto).toList();
    }

    public ActionnaireDto fiche(UUID id) {
        return versDto(trouver(id));
    }

    @Transactional
    public ActionnaireDto creer(ActionnaireEcritureDto demande) {
        CfgShareholder actionnaire = new CfgShareholder();
        actionnaire.setId(UUID.randomUUID());
        actionnaire.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), actionnaire.getId()));
        actionnaire.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        actionnaire.setNotes("");
        ReglesSaisie.texte(demande.notes(), ReglesSaisie.LONGUEUR_MOYENNE, "notes", actionnaire::setNotes);
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        actionnaire.setUserCreate(auteur);
        actionnaire.setUserCreateDateTime(maintenant);
        actionnaire.setUserUpdate(auteur);
        actionnaire.setUserUpdateDateTime(maintenant);
        return versDto(repository.save(actionnaire));
    }

    @Transactional
    public ActionnaireDto modifier(UUID id, ActionnaireEcritureDto demande) {
        CfgShareholder actionnaire = trouver(id);
        if (demande.code() != null) {
            actionnaire.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            actionnaire.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        }
        ReglesSaisie.texte(demande.notes(), ReglesSaisie.LONGUEUR_MOYENNE, "notes", actionnaire::setNotes);
        actionnaire.setUserUpdate(utilisateurAffiche.courant().nom());
        actionnaire.setUserUpdateDateTime(LocalDateTime.now(clock));
        return versDto(repository.save(actionnaire));
    }

    /**
     * Refusée tant qu'une répartition d'entité (FK NO_ACTION) ou une ligne de groupe le cite : la cascade de production
     * sur {@code CfgGroupShareholderLine} ferait tomber la somme du groupe sous 100 % (voir FEATURE §2, rapport P5).
     */
    @Transactional
    public void supprimer(UUID id) {
        CfgShareholder actionnaire = trouver(id);
        List<String> bloquants = new ArrayList<>();
        long repartitions = repartitionRepository.countByCfgShareholderId(id);
        if (repartitions > 0) {
            bloquants.add(repartitions + " répartition(s) d'entité juridique");
        }
        int lignesGroupe = ligneGroupeRepository.findByCfgShareholderId(id).size();
        if (lignesGroupe > 0) {
            bloquants.add(lignesGroupe + " ligne(s) de groupe d'actionnaires");
        }
        if (!bloquants.isEmpty()) {
            throw new ConflictException("L'actionnaire « %s » ne peut pas être supprimé : il est référencé par %s."
                    .formatted(actionnaire.getCode(), String.join(", ", bloquants)));
        }
        repository.delete(actionnaire);
    }

    CfgShareholder trouver(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ACTIONNAIRE_INTROUVABLE + id));
    }

    private String codeLibre(String code, UUID id) {
        repository.findFirstByCodeAndIdNot(code, id).ifPresent(existant -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par l'actionnaire « %s »."
                    .formatted(existant.getCode(), existant.getDescription()));
        });
        return code;
    }

    static ActionnaireDto versDto(CfgShareholder a) {
        return new ActionnaireDto(a.getId(), a.getCode(), a.getDescription(), a.getNotes(), a.getUserCreate(),
                a.getUserCreateDateTime(), a.getUserUpdate(), a.getUserUpdateDateTime());
    }
}
