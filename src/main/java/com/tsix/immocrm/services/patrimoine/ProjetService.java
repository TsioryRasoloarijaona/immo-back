package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneTranche;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.SaisieCoordonnees;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetDto;
import com.tsix.immocrm.dto.patrimoine.ZoneProjetEcritureDto;
import com.tsix.immocrm.entities.ResponsablesOmran;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.entities.cfg.CfgTier;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.motif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ordre;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

@Service
@Transactional(readOnly = true)
public class ProjetService {

    private static final String INTROUVABLE = "Projet introuvable: ";
    private static final Set<String> OBLIGATOIRES = Set.of("code", "description", "entiteId");

    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgCompanyRepository companyRepository;
    private final AdmCountryRepository countryRepository;
    private final ReferencesPatrimoine references;
    private final SuppressionProjet suppression;
    private final TracePatrimoine trace;

    public ProjetService(CfgProjectRepository projectRepository, CfgTrancheRepository trancheRepository,
                         CfgCompanyRepository companyRepository, AdmCountryRepository countryRepository,
                         ReferencesPatrimoine references, SuppressionProjet suppression, TracePatrimoine trace) {
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.references = references;
        this.suppression = suppression;
        this.trace = trace;
    }

    /** Entités actives par {@code CfgCompany.IsActive} ; {@code search} : code, libellé, ville, entité, sans casse. */
    public Page<ProjetListItemDto> findAll(String search, Pageable pageable) {
        return projectRepository.rechercher(motif(search), pageable).map(ProjetService::toListItem);
    }

    public ProjetDetailDto findById(UUID id) {
        return toDetail(visible(id));
    }

    public Page<TrancheListItemDto> findTranches(UUID projetId, Pageable pageable) {
        visible(projetId);
        Set<ColonneTranche> colonnes = ColonneTranche.resoudre(null);
        return trancheRepository.findByCfgProjectId(projetId, pageable).map(t -> TrancheService.toListItem(t, colonnes));
    }

    /** Programme d'une entité active, sinon 404 : seule recherche par id des routes {@code /api/projets/{id}/…}. */
    CfgProject visible(UUID id) {
        return projectRepository.findVisible(id)
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    /** CfgProject:GetDefaultValuesFromCompany, code et description compris ; entité inconnue ou inactive → 404. */
    public ProjetEcritureDto defautsEntite(UUID entiteId) {
        CfgCompany c = companyRepository.findById(entiteId)
                .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Entité introuvable: " + entiteId));
        return new ProjetEcritureDto(c.getId(), c.getCode(), c.getDescription(), c.getDescriptionLong(),
                c.getAddress(), c.getAddress2(), c.getAddress3(), c.getZipCode(), c.getCity(), c.getSignatureCity(),
                c.getPhone(), c.getMobile(), c.getFax(), c.getEmail(), c.getWebSite(),
                Champs.de(c.getAdmCountry(), AdmCountry::getId), null, null, null, null,
                Champs.de(c.getAdmSignatory(), CfgTier::getId), Champs.de(c.getCfgTechnicalManager(), CfgTier::getId),
                Champs.de(c.getCfgSalesManager(), CfgTier::getId), null);
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
        CfgProject projet = visible(id);
        appliquer(projet, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        projet.setUserUpdate(t.auteur());
        projet.setUserUpdateDateTime(t.le());
        return toDetail(projectRepository.save(projet));
    }

    /** OMRAN RessourceManager:Delete ; une référence restante (tranche, article, autre FK) → 409. */
    @Transactional
    public void supprimerProjet(UUID id) {
        suppression.projet(visible(id));
    }

    public List<ZoneProjetDto> zones(UUID projetId) {
        visible(projetId);
        return suppression.zones().findByCfgProjectIdOrderBySortingAscCodeAsc(projetId).stream()
                .map(ProjetService::toZone).toList();
    }

    @Transactional
    public ZoneProjetDto creerZone(UUID projetId, ZoneProjetEcritureDto saisie) {
        CfgProject projet = visible(projetId);
        CfgProjectZone zone = new CfgProjectZone();
        zone.setId(UUID.randomUUID());
        zone.setCfgProject(projet);
        int niveau = 0;
        if (saisie.parentId() != null) {
            niveau = Objects.requireNonNullElse(zoneDuProjet(projetId, saisie.parentId()).getLeveling(), 0) + 1;
            zone.setParentId(saisie.parentId());
        }
        // ProjectZones.cs : Leveling = parent + 1, Sorting = rang parmi les frères.
        zone.setLeveling(niveau);
        zone.setSorting((int) suppression.zones().findByCfgProjectIdOrderBySortingAscCodeAsc(projetId).stream()
                .filter(z -> Objects.equals(z.getParentId(), saisie.parentId())).count() + 1);
        appliquerZone(zone, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        zone.setUserCreate(t.auteur());
        zone.setUserCreateDateTime(t.le());
        zone.setUserUpdate(t.auteur());
        zone.setUserUpdateDateTime(t.le());
        return toZone(suppression.zones().save(zone));
    }

    @Transactional
    public ZoneProjetDto modifierZone(UUID projetId, UUID zoneId, ZoneProjetEcritureDto saisie) {
        visible(projetId);
        CfgProjectZone zone = zoneDuProjet(projetId, zoneId);
        appliquerZone(zone, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        zone.setUserUpdate(t.auteur());
        zone.setUserUpdateDateTime(t.le());
        return toZone(suppression.zones().save(zone));
    }

    /** 409 si la zone a des enfants ou reste référencée (SavClaim, GrhWorkedDay). */
    @Transactional
    public void supprimerZone(UUID projetId, UUID zoneId) {
        visible(projetId);
        suppression.zone(zoneDuProjet(projetId, zoneId));
    }

    private CfgProjectZone zoneDuProjet(UUID projetId, UUID zoneId) {
        return suppression.zones().findByIdAndCfgProjectId(zoneId, projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable: " + zoneId));
    }

    private void appliquerZone(CfgProjectZone z, ZoneProjetEcritureDto d) {
        z.setCode(obligatoire(d.code(), z.getCode(), "Le code"));
        z.setDescription(obligatoire(d.description(), z.getDescription(), "La description"));
        z.setNotes(texte(d.notes(), z.getNotes(), 1000, "Les notes"));
        if (suppression.zones().doublons(z.getCfgProject().getId(), z.getParentId(), z.getCode(), z.getId()) > 0) {
            throw new ConflictException("Le code %s existe déjà sous ce parent dans le programme.".formatted(z.getCode()));
        }
    }

    private void appliquer(CfgProject p, ProjetEcritureDto d) {
        ChampsAVider v = ChampsAVider.de(d, d.champsAVider(), OBLIGATOIRES);
        p.setCode(codeLibre(obligatoire(d.code(), p.getCode(), "Le code"), p.getId()));
        p.setDescription(obligatoire(d.description(), p.getDescription(), "La description"));
        p.setDescriptionLong(texte(v.texte("descriptionLongue", d.descriptionLongue()), p.getDescriptionLong(), 500,
                "La description longue"));
        coordonnees(p, d, v, references, countryRepository);
        if (d.entiteId() != null) {
            p.setCfgCompany(entiteActive(d.entiteId()));
        }
        p.setWorkStartDate(v.ou("dateDebutTravaux", d.dateDebutTravaux(), p.getWorkStartDate()));
        p.setWorkEndDate(v.ou("dateFinTravaux", d.dateFinTravaux(), p.getWorkEndDate()));
        p.setMarketingStartDate(v.ou("dateDebutCommercialisation", d.dateDebutCommercialisation(),
                p.getMarketingStartDate()));
        p.setMarketingEndDate(v.ou("dateFinCommercialisation", d.dateFinCommercialisation(), p.getMarketingEndDate()));
        ordre(p.getWorkStartDate(), p.getWorkEndDate(), "Travaux");
        ordre(p.getMarketingStartDate(), p.getMarketingEndDate(), "Commercialisation");
    }

    /** Coordonnées, pays, signataire et responsables : communs au programme et à l'entité juridique. */
    static void coordonnees(ResponsablesOmran p, SaisieCoordonnees d, ChampsAVider v, ReferencesPatrimoine references,
                            AdmCountryRepository countryRepository) {
        p.setAdmCountry(v.reference("paysId", d.paysId(), p.getAdmCountry(), id -> pays(countryRepository, id)));
        p.setAdmSignatory(v.reference("signataireId", d.signataireId(), p.getAdmSignatory(),
                id -> references.tiers(id, "signataireId")));
        p.setCfgTechnicalManager(v.reference("responsableTechniqueId", d.responsableTechniqueId(),
                p.getCfgTechnicalManager(), id -> references.tiers(id, "responsableTechniqueId")));
        p.setCfgSalesManager(v.reference("responsableCommercialId", d.responsableCommercialId(),
                p.getCfgSalesManager(), id -> references.tiers(id, "responsableCommercialId")));
        p.setAddress(texte(v.texte("adresse", d.adresse()), p.getAddress(), COURT, "L'adresse"));
        p.setAddress2(texte(v.texte("adresse2", d.adresse2()), p.getAddress2(), COURT, "L'adresse 2"));
        p.setAddress3(texte(v.texte("adresse3", d.adresse3()), p.getAddress3(), COURT, "L'adresse 3"));
        p.setZipCode(texte(v.texte("codePostal", d.codePostal()), p.getZipCode(), COURT, "Le code postal"));
        p.setCity(texte(v.texte("ville", d.ville()), p.getCity(), COURT, "La ville"));
        p.setSignatureCity(texte(v.texte("villeSignature", d.villeSignature()), p.getSignatureCity(), COURT,
                "La ville de signature"));
        p.setPhone(texte(v.texte("telephone", d.telephone()), p.getPhone(), COURT, "Le téléphone"));
        p.setMobile(texte(v.texte("mobile", d.mobile()), p.getMobile(), COURT, "Le mobile"));
        p.setFax(texte(v.texte("fax", d.fax()), p.getFax(), COURT, "Le fax"));
        p.setEmail(texte(v.texte("email", d.email()), p.getEmail(), COURT, "L'email"));
        p.setWebSite(texte(v.texte("siteWeb", d.siteWeb()), p.getWebSite(), COURT, "Le site web"));
    }

    private String codeLibre(String code, UUID id) {
        projectRepository.findFirstByCode(code)
                .filter(existant -> !existant.getId().equals(id))
                .ifPresent(existant -> {
                    throw new ConflictException("Le code %s est déjà pris.".formatted(code));
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
                .orElseThrow(() -> new InvalidRequestException("paysId : pays inconnu (" + paysId + ")."));
    }

    private static ZoneProjetDto toZone(CfgProjectZone z) {
        return new ZoneProjetDto(z.getId(), z.getParentId(), z.getLeveling(), z.getSorting(), z.getCode(),
                z.getDescription(), z.getNotes(), z.isAUneImage());
    }

    private static ProjetListItemDto toListItem(CfgProject p) {
        CfgCompany entite = p.getCfgCompany();
        return new ProjetListItemDto(
                p.getId(), p.getCode(), p.getDescription(), p.getCity(),
                Champs.de(entite, CfgCompany::getCode),
                p.getAddress(), p.getSignatureCity(),
                Champs.de(p.getAdmSignatory(), CfgTier::getDescription),
                Champs.de(Champs.de(entite, CfgCompany::getRepresentantLegal), CfgTier::getDescription),
                Champs.de(p.getCfgSalesManager(), CfgTier::getDescription),
                Champs.de(p.getCfgTechnicalManager(), CfgTier::getDescription),
                p.getDescriptionLong(), p.getEmail(), p.getFax(), p.getMobile(), p.getPhone(), p.getWebSite(),
                p.getWorkStartDate(), p.getWorkEndDate(), p.getMarketingStartDate(), p.getMarketingEndDate(),
                p.getUserCreate(), p.getUserCreateDateTime(), p.getUserUpdate(), p.getUserUpdateDateTime());
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
                project.getUserUpdateDateTime(),
                Champs.de(project.getAdmSignatory(), CfgTier::getId),
                Champs.de(project.getAdmSignatory(), CfgTier::getDescription),
                Champs.de(project.getCfgTechnicalManager(), CfgTier::getId),
                Champs.de(project.getCfgTechnicalManager(), CfgTier::getDescription),
                Champs.de(project.getCfgSalesManager(), CfgTier::getId),
                Champs.de(project.getCfgSalesManager(), CfgTier::getDescription)
        );
    }
}
