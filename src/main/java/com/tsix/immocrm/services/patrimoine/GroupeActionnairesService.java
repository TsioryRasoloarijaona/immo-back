package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.dto.patrimoine.GroupeActionnairesDto;
import com.tsix.immocrm.dto.patrimoine.GroupeActionnairesEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderLineRepository;
import com.tsix.immocrm.repositories.cfg.CfgGroupShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Configuration › Autres › Groupe d'actionnaires (FEATURE-entites-juridiques.md §2). Piège : la FK
 * {@code CfgCompany.CfgGroupShareholderId} est ON DELETE CASCADE, d'où le refus de supprimer un groupe utilisé.
 */
@Service
@Transactional(readOnly = true)
public class GroupeActionnairesService {

    static final String GROUPE_INTROUVABLE = "Groupe d'actionnaires introuvable: ";
    static final String CHAMP_LIGNES = "lignes";
    private static final String CHAMP_CODE = "code";

    private final CfgGroupShareholderRepository repository;
    private final CfgGroupShareholderLineRepository ligneRepository;
    private final CfgShareholderRepository actionnaireRepository;
    private final CfgCompanyRepository companyRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public GroupeActionnairesService(CfgGroupShareholderRepository repository,
                                     CfgGroupShareholderLineRepository ligneRepository,
                                     CfgShareholderRepository actionnaireRepository,
                                     CfgCompanyRepository companyRepository,
                                     UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.repository = repository;
        this.ligneRepository = ligneRepository;
        this.actionnaireRepository = actionnaireRepository;
        this.companyRepository = companyRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public List<GroupeActionnairesDto> lister(String recherche) {
        return repository.rechercher(ReglesSaisie.motifSansEspaces(recherche)).stream().map(this::versDto).toList();
    }

    public GroupeActionnairesDto fiche(UUID id) {
        return versDto(trouver(id));
    }

    @Transactional
    public GroupeActionnairesDto creer(GroupeActionnairesEcritureDto demande) {
        CfgGroupShareholder groupe = new CfgGroupShareholder();
        groupe.setId(UUID.randomUUID());
        groupe.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), groupe.getId()));
        groupe.setDescription(ReglesSaisie.requis(demande.description(), "description", "Description"));
        List<CfgGroupShareholderLine> lignes = lignesValides(demande.lignes() == null ? List.of() : demande.lignes());
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        groupe.setUserCreate(auteur);
        groupe.setUserCreateDateTime(maintenant);
        groupe.setUserUpdate(auteur);
        groupe.setUserUpdateDateTime(maintenant);
        CfgGroupShareholder enregistre = repository.save(groupe);
        enregistrerLignes(enregistre, lignes, auteur, maintenant);
        return versDto(enregistre);
    }

    @Transactional
    public GroupeActionnairesDto modifier(UUID id, GroupeActionnairesEcritureDto demande) {
        CfgGroupShareholder groupe = trouver(id);
        if (demande.code() != null) {
            groupe.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            groupe.setDescription(ReglesSaisie.requis(demande.description(), "description", "Description"));
        }
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        if (demande.lignes() != null) {
            List<CfgGroupShareholderLine> lignes = lignesValides(demande.lignes());
            ligneRepository.deleteAll(ligneRepository.duGroupe(id));
            ligneRepository.flush();
            enregistrerLignes(groupe, lignes, auteur, maintenant);
        }
        groupe.setUserUpdate(auteur);
        groupe.setUserUpdateDateTime(maintenant);
        return versDto(repository.save(groupe));
    }

    @Transactional
    public void supprimer(UUID id) {
        CfgGroupShareholder groupe = trouver(id);
        List<String> entites = companyRepository.findByCfgGroupShareholderId(id).stream()
                .map(CfgCompany::getCode).sorted().toList();
        if (!entites.isEmpty()) {
            throw new ConflictException(("Le groupe « %s » ne peut pas être supprimé : il est utilisé par %d entité(s) "
                    + "juridique(s) (%s), que la base supprimerait avec lui.")
                    .formatted(groupe.getCode(), entites.size(), String.join(", ", entites)));
        }
        ligneRepository.deleteAll(ligneRepository.duGroupe(id));
        repository.delete(groupe);
    }

    CfgGroupShareholder trouver(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(GROUPE_INTROUVABLE + id));
    }

    /** Lignes d'un groupe, dans l'ordre de {@code SetRepartitions} : la dernière reçoit le reste. */
    List<RepartitionActionnaires.Pourcentage> pourcentages(UUID groupeId) {
        return ligneRepository.duGroupe(groupeId).stream()
                .filter(l -> l.getCfgShareholder() != null)
                .map(l -> new RepartitionActionnaires.Pourcentage(l.getCfgShareholder().getId(),
                        zeroSiNul(l.getSharingNumber())))
                .toList();
    }

    /** Tout est contrôlé avant la moindre écriture : actionnaires connus, sans doublon, fractions, Σ = 100 %. */
    private List<CfgGroupShareholderLine> lignesValides(List<GroupeActionnairesEcritureDto.Ligne> demandees) {
        List<CfgGroupShareholderLine> lignes = demandees.stream().map(this::ligne).toList();
        if (RepartitionActionnaires.contientUnDoublon(lignes.stream().map(l -> l.getCfgShareholder().getId()).toList())) {
            throw new ChampInvalideException(CHAMP_LIGNES, RepartitionActionnaires.DOUBLON);
        }
        if (!RepartitionActionnaires.fontCentPourCent(lignes.stream().map(CfgGroupShareholderLine::getSharingNumber)
                .toList())) {
            throw new ChampInvalideException(CHAMP_LIGNES, RepartitionActionnaires.CENT_POUR_CENT);
        }
        return lignes;
    }

    private CfgGroupShareholderLine ligne(GroupeActionnairesEcritureDto.Ligne demandee) {
        if (demandee == null || demandee.actionnaireId() == null || demandee.pourcentage() == null) {
            throw new ChampInvalideException(CHAMP_LIGNES, "Chaque ligne exige un actionnaire et un pourcentage.");
        }
        CfgShareholder actionnaire = actionnaireRepository.findById(demandee.actionnaireId())
                .orElseThrow(() -> new ChampInvalideException(CHAMP_LIGNES,
                        "Actionnaire introuvable : " + demandee.actionnaireId()));
        CfgGroupShareholderLine ligne = new CfgGroupShareholderLine();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgShareholder(actionnaire);
        ligne.setSharingNumber(ReglesSaisie.fraction(demandee.pourcentage(), CHAMP_LIGNES));
        return ligne;
    }

    private void enregistrerLignes(CfgGroupShareholder groupe, List<CfgGroupShareholderLine> lignes, String auteur,
                                   LocalDateTime maintenant) {
        lignes.forEach(ligne -> {
            ligne.setCfgGroupShareholder(groupe);
            ligne.setUserCreate(auteur);
            ligne.setUserCreateDateTime(maintenant);
            ligne.setUserUpdate(auteur);
            ligne.setUserUpdateDateTime(maintenant);
        });
        ligneRepository.saveAll(lignes);
    }

    private String codeLibre(String code, UUID id) {
        repository.findFirstByCodeAndIdNot(code, id).ifPresent(existant -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par le groupe « %s »."
                    .formatted(existant.getCode(), existant.getDescription()));
        });
        return code;
    }

    private GroupeActionnairesDto versDto(CfgGroupShareholder g) {
        List<GroupeActionnairesDto.Ligne> lignes = ligneRepository.duGroupe(g.getId()).stream()
                .map(l -> new GroupeActionnairesDto.Ligne(l.getId(),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getId),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getCode),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getDescription),
                        l.getSharingNumber()))
                .toList();
        return new GroupeActionnairesDto(g.getId(), g.getCode(), g.getDescription(), lignes, g.getUserCreate(),
                g.getUserCreateDateTime(), g.getUserUpdate(), g.getUserUpdateDateTime());
    }

    // Une ligne saisie dans OMRAN peut porter un pourcentage NULL : compté 0, comme sa somme décimale.
    private static BigDecimal zeroSiNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
