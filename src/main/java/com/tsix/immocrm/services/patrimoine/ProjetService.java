package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.AdresseComplete;
import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
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
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_COURTE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.LONGUEUR_MOYENNE;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.si;
import static com.tsix.immocrm.services.patrimoine.ReglesSaisie.texte;

@Service
@Transactional(readOnly = true)
public class ProjetService {

    static final String PROJET_INTROUVABLE = "Projet introuvable: ";
    private static final String CHAMP_ENTITE = "entiteId";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";
    private static final String CHAMP_SIGNATAIRE = "signataireId";
    private static final String CHAMP_RESP_TECHNIQUE = "responsableTechniqueId";
    private static final String CHAMP_RESP_COMMERCIAL = "responsableCommercialId";

    /** FEATURE-patrimoine-tranche-fiche.md §1.1 : textes → {@code ""}, dates et clés étrangères → NULL. */
    private static final ChampsAVider<CfgProject> VIDABLES =
            new ChampsAVider<CfgProject>(Set.of(CHAMP_ENTITE, CHAMP_CODE, CHAMP_DESCRIPTION))
                    .texte("descriptionLongue", CfgProject::setDescriptionLong)
                    .texte("adresse", CfgProject::setAddress)
                    .texte("adresse2", CfgProject::setAddress2)
                    .texte("adresse3", CfgProject::setAddress3)
                    .texte("codePostal", CfgProject::setZipCode)
                    .texte("ville", CfgProject::setCity)
                    .texte("villeSignature", CfgProject::setSignatureCity)
                    .texte("telephone", CfgProject::setPhone)
                    .texte("mobile", CfgProject::setMobile)
                    .texte("fax", CfgProject::setFax)
                    .texte("email", CfgProject::setEmail)
                    .texte("siteWeb", CfgProject::setWebSite)
                    .nul("paysId", CfgProject::setAdmCountry)
                    .nul(CHAMP_SIGNATAIRE, CfgProject::setAdmSignatory)
                    .nul(CHAMP_RESP_TECHNIQUE, CfgProject::setCfgTechnicalManager)
                    .nul(CHAMP_RESP_COMMERCIAL, CfgProject::setCfgSalesManager)
                    .nul("dateDebutTravaux", CfgProject::setWorkStartDate)
                    .nul("dateFinTravaux", CfgProject::setWorkEndDate)
                    .nul("dateDebutCommercialisation", CfgProject::setMarketingStartDate)
                    .nul("dateFinCommercialisation", CfgProject::setMarketingEndDate);

    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgCompanyRepository companyRepository;
    private final ReferencesFiche references;
    private final HabilitationCreateur habilitationCreateur;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ProjetService(CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                         CfgCompanyRepository companyRepository, ReferencesFiche references,
                         HabilitationCreateur habilitationCreateur, UtilisateurAffiche utilisateurAffiche,
                         Clock clock) {
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.companyRepository = companyRepository;
        this.references = references;
        this.habilitationCreateur = habilitationCreateur;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Entités actives par {@code CfgCompany.IsActive}, jamais une liste de codes figée. */
    public Page<ProjetListItemDto> findAll(String recherche, Pageable pageable) {
        return projectRepository.rechercherDesEntitesActives(ReglesSaisie.motif(recherche), pageable)
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

    /** {@code CfgProject.GetDefaultValuesFromCompany} : Code et Description compris, comme OMRAN. */
    public ProjetEcritureDto defautsEntite(UUID entiteId) {
        CfgCompany entite = companyRepository.findById(entiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Entité introuvable: " + entiteId));
        return ProjetEcritureDto.builder()
                .entiteId(entite.getId())
                .code(entite.getCode())
                .description(entite.getDescription())
                .descriptionLongue(tronque(entite.getDescriptionLong(), LONGUEUR_MOYENNE))
                .adresse(entite.getAddress())
                .adresse2(entite.getAddress2())
                .adresse3(entite.getAddress3())
                .codePostal(entite.getZipCode())
                .ville(entite.getCity())
                .villeSignature(entite.getSignatureCity())
                .telephone(entite.getPhone())
                .mobile(entite.getMobile())
                .fax(entite.getFax())
                .email(entite.getEmail())
                .siteWeb(entite.getWebSite())
                .paysId(Champs.de(entite.getAdmCountry(), AdmCountry::getId))
                .signataireId(Champs.de(entite.getAdmSignatory(), CfgTier::getId))
                .responsableTechniqueId(Champs.de(entite.getCfgTechnicalManager(), CfgTier::getId))
                .responsableCommercialId(Champs.de(entite.getCfgSalesManager(), CfgTier::getId))
                .build();
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
        references.pays(d.paysId(), "paysId", projet::setAdmCountry);
        references.tiers(d.signataireId(), CHAMP_SIGNATAIRE, TypeTiersPatrimoine.PERSONNEL, projet::setAdmSignatory);
        references.tiers(d.responsableTechniqueId(), CHAMP_RESP_TECHNIQUE, TypeTiersPatrimoine.PERSONNEL,
                projet::setCfgTechnicalManager);
        references.tiers(d.responsableCommercialId(), CHAMP_RESP_COMMERCIAL, TypeTiersPatrimoine.PERSONNEL,
                projet::setCfgSalesManager);
        si(d.dateDebutTravaux(), projet::setWorkStartDate);
        si(d.dateFinTravaux(), projet::setWorkEndDate);
        si(d.dateDebutCommercialisation(), projet::setMarketingStartDate);
        si(d.dateFinCommercialisation(), projet::setMarketingEndDate);
        VIDABLES.appliquer(d.champsAVider(), d, projet);
    }

    private static void verifierDates(CfgProject projet) {
        ReglesSaisie.periode(projet.getWorkStartDate(), projet.getWorkEndDate(), "dateFinTravaux", "des travaux");
        ReglesSaisie.periode(projet.getMarketingStartDate(), projet.getMarketingEndDate(),
                "dateFinCommercialisation", "de commercialisation");
    }

    private static String tronque(String texte, int max) {
        return texte == null || texte.length() <= max ? texte : texte.substring(0, max);
    }

    private static String nom(CfgTier tiers) {
        return Champs.de(tiers, CfgTier::getDescription);
    }

    private static ProjetListItemDto toListItem(CfgProject p) {
        CfgCompany entite = p.getCfgCompany();
        return ProjetListItemDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .ville(p.getCity())
                .entiteCode(Champs.de(entite, CfgCompany::getCode))
                .entiteLibelle(Champs.de(entite, CfgCompany::getDescription))
                .adresseComplete(AdresseComplete.de(p.getCity(), p.getAddress(), p.getAddress2(), p.getAddress3(),
                        p.getZipCode(), Champs.de(p.getAdmCountry(), AdmCountry::getDescription)))
                .villeSignature(p.getSignatureCity())
                .signataireLibelle(nom(p.getAdmSignatory()))
                .representantLegal(nom(Champs.de(entite, CfgCompany::getCfgTier)))
                .responsableCommercialLibelle(nom(p.getCfgSalesManager()))
                .responsableTechniqueLibelle(nom(p.getCfgTechnicalManager()))
                .descriptionLongue(p.getDescriptionLong())
                .telephone(p.getPhone())
                .mobile(p.getMobile())
                .fax(p.getFax())
                .email(p.getEmail())
                .siteWeb(p.getWebSite())
                .dateDebutTravaux(p.getWorkStartDate())
                .dateFinTravaux(p.getWorkEndDate())
                .dateDebutCommercialisation(p.getMarketingStartDate())
                .dateFinCommercialisation(p.getMarketingEndDate())
                .creePar(p.getUserCreate())
                .creeLe(p.getUserCreateDateTime())
                .modifiePar(p.getUserUpdate())
                .modifieLe(p.getUserUpdateDateTime())
                .build();
    }

    private static ProjetDetailDto toDetail(CfgProject p) {
        CfgCompany entite = p.getCfgCompany();
        AdmCountry pays = p.getAdmCountry();
        return ProjetDetailDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .descriptionLongue(p.getDescriptionLong())
                .adresse(p.getAddress())
                .adresse2(p.getAddress2())
                .adresse3(p.getAddress3())
                .codePostal(p.getZipCode())
                .ville(p.getCity())
                .villeSignature(p.getSignatureCity())
                .telephone(p.getPhone())
                .mobile(p.getMobile())
                .fax(p.getFax())
                .email(p.getEmail())
                .siteWeb(p.getWebSite())
                .dateDebutTravaux(p.getWorkStartDate())
                .dateFinTravaux(p.getWorkEndDate())
                .dateDebutCommercialisation(p.getMarketingStartDate())
                .dateFinCommercialisation(p.getMarketingEndDate())
                .entiteId(Champs.de(entite, CfgCompany::getId))
                .entiteCode(Champs.de(entite, CfgCompany::getCode))
                .entiteLibelle(Champs.de(entite, CfgCompany::getDescription))
                .paysId(Champs.de(pays, AdmCountry::getId))
                .paysLibelle(Champs.de(pays, AdmCountry::getDescription))
                .creePar(p.getUserCreate())
                .creeLe(p.getUserCreateDateTime())
                .modifiePar(p.getUserUpdate())
                .modifieLe(p.getUserUpdateDateTime())
                .signataireId(Champs.de(p.getAdmSignatory(), CfgTier::getId))
                .signataireLibelle(nom(p.getAdmSignatory()))
                .responsableTechniqueId(Champs.de(p.getCfgTechnicalManager(), CfgTier::getId))
                .responsableTechniqueLibelle(nom(p.getCfgTechnicalManager()))
                .responsableCommercialId(Champs.de(p.getCfgSalesManager(), CfgTier::getId))
                .responsableCommercialLibelle(nom(p.getCfgSalesManager()))
                .representantLegal(nom(Champs.de(entite, CfgCompany::getCfgTier)))
                .build();
    }
}
