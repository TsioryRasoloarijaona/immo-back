package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ordre;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

@Service
@Transactional(readOnly = true)
public class ProjetService {

    private static final String INTROUVABLE = "Projet introuvable: ";

    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgCompanyRepository companyRepository;
    private final AdmCountryRepository countryRepository;
    private final TracePatrimoine trace;

    public ProjetService(CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                         CfgCompanyRepository companyRepository, AdmCountryRepository countryRepository,
                         TracePatrimoine trace) {
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.trace = trace;
    }

    /** Entités actives par {@code CfgCompany.IsActive}, jamais une liste de codes figée. */
    public Page<ProjetListItemDto> findAll(Pageable pageable) {
        return projectRepository.findDesEntitesActives(pageable)
                .map(ProjetService::toListItem);
    }

    public ProjetDetailDto findById(UUID id) {
        return projectRepository.findById(id)
                .map(ProjetService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    public Page<TrancheListItemDto> findTranches(UUID projetId, Pageable pageable) {
        if (!projectRepository.existsById(projetId)) {
            throw new ResourceNotFoundException(INTROUVABLE + projetId);
        }
        return trancheRepository.findByCfgProjectId(projetId, pageable).map(TrancheService::toListItem);
    }

    @Transactional
    public ProjetDetailDto creerProjet(ProjetEcritureDto saisie) {
        if (saisie.entiteId() == null) {
            throw new InvalidRequestException("L'entité est obligatoire.");
        }
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        appliquer(projet, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        projet.setUserCreate(t.auteur());
        projet.setUserCreateDateTime(t.le());
        projet.setUserUpdate(t.auteur());
        projet.setUserUpdateDateTime(t.le());
        projectRepository.save(projet);
        trace.habiliterCreateur(projet, null);
        return toDetail(projet);
    }

    @Transactional
    public ProjetDetailDto modifierProjet(UUID id, ProjetEcritureDto saisie) {
        CfgProject projet = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
        appliquer(projet, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        projet.setUserUpdate(t.auteur());
        projet.setUserUpdateDateTime(t.le());
        return toDetail(projectRepository.save(projet));
    }

    private void appliquer(CfgProject p, ProjetEcritureDto d) {
        p.setCode(codeLibre(obligatoire(d.code(), p.getCode(), "Le code"), p.getId()));
        p.setDescription(obligatoire(d.description(), p.getDescription(), "La description"));
        p.setDescriptionLong(texte(d.descriptionLongue(), p.getDescriptionLong(), 500, "La description longue"));
        p.setAddress(texte(d.adresse(), p.getAddress(), COURT, "L'adresse"));
        p.setAddress2(texte(d.adresse2(), p.getAddress2(), COURT, "L'adresse 2"));
        p.setAddress3(texte(d.adresse3(), p.getAddress3(), COURT, "L'adresse 3"));
        p.setZipCode(texte(d.codePostal(), p.getZipCode(), COURT, "Le code postal"));
        p.setCity(texte(d.ville(), p.getCity(), COURT, "La ville"));
        p.setSignatureCity(texte(d.villeSignature(), p.getSignatureCity(), COURT, "La ville de signature"));
        p.setPhone(texte(d.telephone(), p.getPhone(), COURT, "Le téléphone"));
        p.setMobile(texte(d.mobile(), p.getMobile(), COURT, "Le mobile"));
        p.setFax(texte(d.fax(), p.getFax(), COURT, "Le fax"));
        p.setEmail(texte(d.email(), p.getEmail(), COURT, "L'email"));
        p.setWebSite(texte(d.siteWeb(), p.getWebSite(), COURT, "Le site web"));
        if (d.entiteId() != null) {
            p.setCfgCompany(entiteActive(d.entiteId()));
        }
        if (d.paysId() != null) {
            p.setAdmCountry(pays(countryRepository, d.paysId()));
        }
        p.setWorkStartDate(ou(d.dateDebutTravaux(), p.getWorkStartDate()));
        p.setWorkEndDate(ou(d.dateFinTravaux(), p.getWorkEndDate()));
        p.setMarketingStartDate(ou(d.dateDebutCommercialisation(), p.getMarketingStartDate()));
        p.setMarketingEndDate(ou(d.dateFinCommercialisation(), p.getMarketingEndDate()));
        ordre(p.getWorkStartDate(), p.getWorkEndDate(), "Travaux");
        ordre(p.getMarketingStartDate(), p.getMarketingEndDate(), "Commercialisation");
    }

    private String codeLibre(String code, UUID id) {
        projectRepository.findFirstByCode(code)
                .filter(existant -> !existant.getId().equals(id))
                .ifPresent(existant -> {
                    throw new ConflictException("Le code %s est déjà pris par le programme %s — %s."
                            .formatted(code, existant.getCode(), existant.getDescription()));
                });
        return code;
    }

    private CfgCompany entiteActive(UUID entiteId) {
        return companyRepository.findById(entiteId)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> new InvalidRequestException("Entité inconnue ou inactive : " + entiteId));
    }

    static AdmCountry pays(AdmCountryRepository countryRepository, UUID paysId) {
        return countryRepository.findById(paysId)
                .orElseThrow(() -> new InvalidRequestException("Pays inconnu : " + paysId));
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
