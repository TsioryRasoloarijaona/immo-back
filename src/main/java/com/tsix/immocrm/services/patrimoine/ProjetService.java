package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
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
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_COURTE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_MOYENNE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.si;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

@Service
@Transactional(readOnly = true)
public class ProjetService {

    private static final String PROJET_INTROUVABLE = "Projet introuvable: ";
    private static final String CHAMP_ENTITE = "entiteId";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";

    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgCompanyRepository companyRepository;
    private final AdmCountryRepository countryRepository;
    private final HabilitationCreateur habilitationCreateur;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ProjetService(CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                         CfgCompanyRepository companyRepository, AdmCountryRepository countryRepository,
                         HabilitationCreateur habilitationCreateur, UtilisateurAffiche utilisateurAffiche,
                         Clock clock) {
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.habilitationCreateur = habilitationCreateur;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Entités actives par {@code CfgCompany.IsActive}, jamais une liste de codes figée. */
    public Page<ProjetListItemDto> findAll(Pageable pageable) {
        return projectRepository.findDesEntitesActives(pageable)
                .map(ProjetService::toListItem);
    }

    public ProjetDetailDto findById(UUID id) {
        return projectRepository.findById(id)
                .map(ProjetService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException(PROJET_INTROUVABLE + id));
    }

    public Page<TrancheListItemDto> findTranches(UUID projetId, Pageable pageable) {
        if (!projectRepository.existsById(projetId)) {
            throw new ResourceNotFoundException(PROJET_INTROUVABLE + projetId);
        }
        return trancheRepository.findByCfgProjectId(projetId, pageable).map(TrancheService::toListItem);
    }

    @Transactional
    public ProjetDetailDto create(ProjetEcritureDto demande) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCfgCompany(entiteActive(ReglesSaisie.requis(demande.entiteId(), CHAMP_ENTITE, "Entité")));
        projet.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), projet.getId()));
        projet.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        initialiserTextes(projet);
        appliquerFacultatifs(projet, demande);
        verifierDates(projet);

        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        projet.setUserCreate(auteur.nom());
        projet.setUserCreateDateTime(maintenant);
        projet.setUserUpdate(auteur.nom());
        projet.setUserUpdateDateTime(maintenant);
        CfgProject enregistre = projectRepository.save(projet);
        habilitationCreateur.surProjet(auteur.compte(), enregistre);
        return toDetail(enregistre);
    }

    @Transactional
    public ProjetDetailDto update(UUID id, ProjetEcritureDto demande) {
        CfgProject projet = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROJET_INTROUVABLE + id));
        if (demande.entiteId() != null) {
            projet.setCfgCompany(entiteActive(demande.entiteId()));
        }
        if (demande.code() != null) {
            projet.setCode(codeLibre(ReglesSaisie.requis(demande.code(), CHAMP_CODE, "Code"), id));
        }
        if (demande.description() != null) {
            projet.setDescription(ReglesSaisie.requis(demande.description(), CHAMP_DESCRIPTION, "Description"));
        }
        appliquerFacultatifs(projet, demande);
        verifierDates(projet);

        projet.setUserUpdate(utilisateurAffiche.courant().nom());
        projet.setUserUpdateDateTime(LocalDateTime.now(clock));
        return toDetail(projectRepository.save(projet));
    }

    private CfgCompany entiteActive(UUID entiteId) {
        CfgCompany entite = companyRepository.findById(entiteId)
                .orElseThrow(() -> new ChampInvalideException(CHAMP_ENTITE, "Entité introuvable : " + entiteId));
        if (!Boolean.TRUE.equals(entite.getIsActive())) {
            throw new ChampInvalideException(CHAMP_ENTITE,
                    "L'entité « %s » n'est pas active.".formatted(entite.getCode()));
        }
        return entite;
    }

    private String codeLibre(String code, UUID id) {
        projectRepository.findFirstByCodeAndIdNot(code, id).ifPresent(existant -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé par le programme « %s »."
                    .formatted(existant.getCode(), existant.getDescription()));
        });
        return code;
    }

    // OMRAN initialise ses chaînes à string.Empty : jamais NULL dans une colonne texte qu'il écrit.
    private static void initialiserTextes(CfgProject projet) {
        projet.setDescriptionLong("");
        projet.setAddress("");
        projet.setAddress2("");
        projet.setAddress3("");
        projet.setZipCode("");
        projet.setCity("");
        projet.setSignatureCity("");
        projet.setPhone("");
        projet.setMobile("");
        projet.setFax("");
        projet.setEmail("");
        projet.setWebSite("");
    }

    private void appliquerFacultatifs(CfgProject projet, ProjetEcritureDto d) {
        texte(d.descriptionLongue(), LONGUEUR_MOYENNE, "descriptionLongue", projet::setDescriptionLong);
        texte(d.adresse(), LONGUEUR_COURTE, "adresse", projet::setAddress);
        texte(d.adresse2(), LONGUEUR_COURTE, "adresse2", projet::setAddress2);
        texte(d.adresse3(), LONGUEUR_COURTE, "adresse3", projet::setAddress3);
        texte(d.codePostal(), LONGUEUR_COURTE, "codePostal", projet::setZipCode);
        texte(d.ville(), LONGUEUR_COURTE, "ville", projet::setCity);
        texte(d.villeSignature(), LONGUEUR_COURTE, "villeSignature", projet::setSignatureCity);
        texte(d.telephone(), LONGUEUR_COURTE, "telephone", projet::setPhone);
        texte(d.mobile(), LONGUEUR_COURTE, "mobile", projet::setMobile);
        texte(d.fax(), LONGUEUR_COURTE, "fax", projet::setFax);
        texte(d.email(), LONGUEUR_COURTE, "email", projet::setEmail);
        texte(d.siteWeb(), LONGUEUR_COURTE, "siteWeb", projet::setWebSite);
        if (d.paysId() != null) {
            projet.setAdmCountry(pays(countryRepository, d.paysId()));
        }
        si(d.dateDebutTravaux(), projet::setWorkStartDate);
        si(d.dateFinTravaux(), projet::setWorkEndDate);
        si(d.dateDebutCommercialisation(), projet::setMarketingStartDate);
        si(d.dateFinCommercialisation(), projet::setMarketingEndDate);
    }

    static AdmCountry pays(AdmCountryRepository countryRepository, UUID paysId) {
        return countryRepository.findById(paysId)
                .orElseThrow(() -> new ChampInvalideException("paysId", "Pays introuvable : " + paysId));
    }

    private static void verifierDates(CfgProject projet) {
        ReglesSaisie.periode(projet.getWorkStartDate(), projet.getWorkEndDate(), "dateFinTravaux", "des travaux");
        ReglesSaisie.periode(projet.getMarketingStartDate(), projet.getMarketingEndDate(),
                "dateFinCommercialisation", "de commercialisation");
    }

    private static ProjetListItemDto toListItem(CfgProject project) {
        return new ProjetListItemDto(
                project.getId(),
                project.getCode(),
                project.getDescription(),
                project.getCity(),
                Champs.de(project.getCfgCompany(), CfgCompany::getCode)
        );
    }

    private static ProjetDetailDto toDetail(CfgProject project) {
        CfgCompany entite = project.getCfgCompany();
        AdmCountry pays = project.getAdmCountry();
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
                project.getMarketingEndDate(),
                Champs.de(entite, CfgCompany::getId),
                Champs.de(entite, CfgCompany::getCode),
                Champs.de(entite, CfgCompany::getDescription),
                Champs.de(pays, AdmCountry::getId),
                Champs.de(pays, AdmCountry::getDescription),
                project.getUserCreate(),
                project.getUserCreateDateTime(),
                project.getUserUpdate(),
                project.getUserUpdateDateTime()
        );
    }
}
