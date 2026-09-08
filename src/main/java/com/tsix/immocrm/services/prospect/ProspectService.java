package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.dto.prospect.PieceIdentiteDto;
import com.tsix.immocrm.dto.prospect.PieceIdentiteRequest;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmMatrimonialRegime;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmResidenceType;
import com.tsix.immocrm.entities.adm.AdmSalary;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmFamilySituationRepository;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmMatrimonialRegimeRepository;
import com.tsix.immocrm.repositories.adm.AdmNationalityRepository;
import com.tsix.immocrm.repositories.adm.AdmProfessionRepository;
import com.tsix.immocrm.repositories.adm.AdmResidenceTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmSalaryRepository;
import com.tsix.immocrm.repositories.adm.AdmSexTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProspectService {

    /**
     * Codes {@code CfgTierType} qui définissent un "prospect" dans Omran — confirmé sur données
     * réelles: {@code CfgTierType.Code = "PRO"} porte 13 567 tiers (contre seulement 13 avec
     * {@code CfgTier.IsProspect = true}, un flag app-only quasi jamais renseigné côté legacy) et
     * {@code "PRORNT"} ("PROSPECT LOCATION") 4 de plus — cohérent avec les ~13 730 tiers
     * distincts réellement liés à une {@code ComProspection}. {@link #findAll} filtre dessus par
     * défaut; {@link #createEntity} y défaut le type d'un nouveau prospect si non précisé.
     */
    private static final List<String> CODES_TYPE_PROSPECT = List.of("pro", "prornt");
    private static final String CODE_TYPE_PROSPECT_PAR_DEFAUT = "PRO";

    private final CfgTierRepository tierRepository;
    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierKindRepository tierKindRepository;
    private final AdmTierTitleRepository tierTitleRepository;
    private final AdmSexTypeRepository sexTypeRepository;
    private final AdmCountryRepository countryRepository;
    private final AdmNationalityRepository nationalityRepository;
    private final AdmFamilySituationRepository familySituationRepository;
    private final AdmResidenceTypeRepository residenceTypeRepository;
    private final AdmMatrimonialRegimeRepository matrimonialRegimeRepository;
    private final AdmProfessionRepository professionRepository;
    private final AdmSalaryRepository salaryRepository;
    private final AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;
    /** Contrôle indicatif + numéro — voir {@code FEATURE-indicatif-telephone.md}. */
    private final NumeroTelephoneSupport numeroSupport;

    public ProspectService(CfgTierRepository tierRepository,
                            CfgTierTypeRepository tierTypeRepository,
                            CfgTierKindRepository tierKindRepository,
                            AdmTierTitleRepository tierTitleRepository,
                            AdmSexTypeRepository sexTypeRepository,
                            AdmCountryRepository countryRepository,
                            AdmNationalityRepository nationalityRepository,
                            AdmFamilySituationRepository familySituationRepository,
                            AdmResidenceTypeRepository residenceTypeRepository,
                            AdmMatrimonialRegimeRepository matrimonialRegimeRepository,
                            AdmProfessionRepository professionRepository,
                            AdmSalaryRepository salaryRepository,
                            AdmIdentityDocumentTypeRepository identityDocumentTypeRepository,
                            NumeroTelephoneSupport numeroSupport) {
        this.numeroSupport = numeroSupport;
        this.tierRepository = tierRepository;
        this.tierTypeRepository = tierTypeRepository;
        this.tierKindRepository = tierKindRepository;
        this.tierTitleRepository = tierTitleRepository;
        this.sexTypeRepository = sexTypeRepository;
        this.countryRepository = countryRepository;
        this.nationalityRepository = nationalityRepository;
        this.familySituationRepository = familySituationRepository;
        this.residenceTypeRepository = residenceTypeRepository;
        this.matrimonialRegimeRepository = matrimonialRegimeRepository;
        this.professionRepository = professionRepository;
        this.salaryRepository = salaryRepository;
        this.identityDocumentTypeRepository = identityDocumentTypeRepository;
    }

    /**
     * @param search free-text match on code/nom/prenom/raisonSociale/email
     * @param types  {@code CfgTierType.code} filter — OR-match, repeated query param
     *               ({@code ?type=a&type=b}). When provided, replaces the default "is a
     *               prospect" restriction entirely (the caller is deliberately narrowing to
     *               specific type codes). When absent, defaults to {@link #CODES_TYPE_PROSPECT}
     *               — same classification Omran itself uses ({@code CfgTierType.Code = "PRO"},
     *               confirmed on real data: 13 567 tiers, vs. only 13 with the app-only
     *               {@code CfgTier.IsProspect} flag this endpoint filtered on before).
     */
    public Page<ProspectListItemDto> findAll(String search, List<String> types, Pageable pageable) {
        Specification<CfgTier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (types != null && !types.isEmpty()) {
                List<String> typesLower = types.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("cfgTierType").get("code")).in(typesLower));
            } else {
                predicates.add(cb.lower(root.get("cfgTierType").get("code")).in(CODES_TYPE_PROSPECT));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("socialReason")), pattern),
                        cb.like(cb.lower(root.get("eMail")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return tierRepository.findAll(spec, pageable).map(ProspectService::toListItem);
    }

    public ProspectDetailDto findById(UUID id) {
        return tierRepository.findById(id)
                .map(this::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + id));
    }

    @Transactional
    public ProspectDetailDto create(CreerProspectRequest request) {
        return toDetail(createEntity(request));
    }

    /**
     * Same creation logic as {@link #create}, but returns the persisted entity — used by
     * {@link com.tsix.immocrm.services.prospection.ProspectionService} to link a newly
     * created prospect back onto a pré-prospection (POST /api/pre-prospections/{id}/creer-prospect).
     */
    @Transactional
    public CfgTier createEntity(CreerProspectRequest request) {
        if (!StringUtils.hasText(request.nom()) && !StringUtils.hasText(request.raisonSociale())) {
            throw new InvalidRequestException("nom ou raisonSociale est requis");
        }

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setIsProspect(true);
        applyFields(tier, request.prenom(), request.nom(), request.raisonSociale(), request.telephone(),
                request.indicatifTelephone(), request.mobile(), request.indicatifMobile(),
                request.email(), request.adresse(), request.ville(), request.codePostal(),
                request.dateNaissance(), request.lieuNaissance(), request.nombreEnfants(), request.typeId(),
                request.kindId(), request.commercialId(), request.titreId(), request.sexeId(),
                request.paysResidenceId(), request.paysId(), request.nationaliteId(), request.situationFamilialeId(),
                request.typeResidenceId(), request.regimeMatrimonialId(), request.professionId(), request.salaireId(),
                request.employeur(), request.anciennete(), request.filsDe(), request.etDe(), request.adresse2(),
                request.adresse3(), request.pieceIdentite());
        // Sans typeId explicite, défaut sur CfgTierType "PRO" — le type sur lequel GET
        // /api/prospects filtre désormais par défaut (voir CODES_TYPE_PROSPECT). Sans ça, un
        // prospect créé sans typeId n'apparaîtrait plus dans sa propre liste.
        if (request.typeId() == null) {
            tier.setCfgTierType(resolveTypeProspectParDefaut());
        }

        return tierRepository.save(tier);
    }

    /**
     * Type "PRO" par défaut des nouveaux prospects créés sans {@code typeId} explicite.
     * Auto-répare si la ligne de référence venait à manquer (ex. base de test fraîche) plutôt que
     * de bloquer toute création de prospect — même convention que
     * {@code DossierService.genererCodeDossier}.
     */
    private CfgTierType resolveTypeProspectParDefaut() {
        return tierTypeRepository.findByCode(CODE_TYPE_PROSPECT_PAR_DEFAUT).orElseGet(() -> {
            CfgTierType type = new CfgTierType();
            type.setId(UUID.randomUUID());
            type.setCode(CODE_TYPE_PROSPECT_PAR_DEFAUT);
            type.setDescription("PROSPECT");
            return tierTypeRepository.save(type);
        });
    }

    @Transactional
    public ProspectDetailDto update(UUID id, ModifierProspectRequest request) {
        CfgTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + id));

        applyFields(tier, request.prenom(), request.nom(), request.raisonSociale(), request.telephone(),
                request.indicatifTelephone(), request.mobile(), request.indicatifMobile(),
                request.email(), request.adresse(), request.ville(), request.codePostal(),
                request.dateNaissance(), request.lieuNaissance(), request.nombreEnfants(), request.typeId(),
                request.kindId(), request.commercialId(), request.titreId(), request.sexeId(),
                request.paysResidenceId(), request.paysId(), request.nationaliteId(), request.situationFamilialeId(),
                request.typeResidenceId(), request.regimeMatrimonialId(), request.professionId(), request.salaireId(),
                request.employeur(), request.anciennete(), request.filsDe(), request.etDe(), request.adresse2(),
                request.adresse3(), request.pieceIdentite());

        return toDetail(tierRepository.save(tier));
    }

    private void applyFields(CfgTier tier, String prenom, String nom, String raisonSociale, String telephone,
                              String indicatifTelephone, String mobile, String indicatifMobile,
                              String email, String adresse, String ville, String codePostal,
                              LocalDateTime dateNaissance, String lieuNaissance, Integer nombreEnfants,
                              UUID typeId, UUID kindId, UUID commercialId, UUID titreId, UUID sexeId,
                              UUID paysResidenceId, UUID paysId, UUID nationaliteId, UUID situationFamilialeId,
                              UUID typeResidenceId, UUID regimeMatrimonialId, UUID professionId, UUID salaireId,
                              String employeur, Integer anciennete, String filsDe, String etDe, String adresse2,
                              String adresse3, PieceIdentiteRequest pieceIdentite) {
        // Un numéro ne s'écrit jamais seul : indicatif exigé, valeur normalisée au format que
        // porte déjà la base. Voir FEATURE-indicatif-telephone.md §5.
        //
        // Les DEUX contrôles ont lieu avant la moindre écriture, et l'ordre porte le sens : une
        // requête fournissant un mobile valide et un fixe fautif ne doit pas laisser la fiche
        // avec le mobile écrit et le reste non. La transaction annulerait de toute façon, mais
        // s'appuyer là-dessus ferait dépendre la cohérence d'un mécanisme extérieur à la méthode.
        NumeroTelephoneSupport.NumeroNormalise fixe =
                numeroSupport.valider(telephone, indicatifTelephone, "telephone");
        NumeroTelephoneSupport.NumeroNormalise portable =
                numeroSupport.valider(mobile, indicatifMobile, "mobile");

        if (prenom != null) tier.setFirstName(prenom);
        if (nom != null) tier.setLastName(nom);
        if (raisonSociale != null) tier.setSocialReason(raisonSociale);
        if (fixe != null) {
            tier.setPhone(fixe.numero());
            tier.setPhoneIndicatif(fixe.indicatif());
        }
        if (portable != null) {
            tier.setMobile(portable.numero());
            tier.setMobileIndicatif(portable.indicatif());
        }
        if (email != null) tier.setEMail(email);
        if (adresse != null) tier.setAddress(adresse);
        if (adresse2 != null) tier.setAddress2(adresse2);
        if (adresse3 != null) tier.setAddress3(adresse3);
        if (ville != null) tier.setCity(ville);
        if (codePostal != null) tier.setZipCode(codePostal);
        if (dateNaissance != null) tier.setBirthDate(dateNaissance);
        if (lieuNaissance != null) tier.setBirthPlace(lieuNaissance);
        if (nombreEnfants != null) tier.setNbrChildren(nombreEnfants);
        if (employeur != null) tier.setEmployer(employeur);
        if (anciennete != null) tier.setAntiquity(anciennete);
        if (filsDe != null) tier.setFatherName(filsDe);
        if (etDe != null) tier.setMotherName(etDe);
        if (typeId != null) {
            CfgTierType type = tierTypeRepository.findById(typeId)
                    .orElseThrow(() -> new InvalidRequestException("CfgTierType introuvable: " + typeId));
            tier.setCfgTierType(type);
        }
        if (kindId != null) {
            CfgTierKind kind = tierKindRepository.findById(kindId)
                    .orElseThrow(() -> new InvalidRequestException("CfgTierKind introuvable: " + kindId));
            tier.setCfgTierKind(kind);
        }
        if (commercialId != null) {
            CfgTier commercial = tierRepository.findById(commercialId)
                    .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + commercialId));
            tier.setCfgCommercial(commercial);
        }
        if (titreId != null) {
            AdmTierTitle titre = tierTitleRepository.findById(titreId)
                    .orElseThrow(() -> new InvalidRequestException("AdmTierTitle introuvable: " + titreId));
            tier.setAdmTierTitle(titre);
        }
        if (sexeId != null) {
            AdmSexType sexe = sexTypeRepository.findById(sexeId)
                    .orElseThrow(() -> new InvalidRequestException("AdmSexType introuvable: " + sexeId));
            tier.setAdmSexType(sexe);
        }
        if (paysResidenceId != null) {
            AdmCountry pays = countryRepository.findById(paysResidenceId)
                    .orElseThrow(() -> new InvalidRequestException("AdmCountry introuvable: " + paysResidenceId));
            tier.setAdmCountryResidence(pays);
        }
        if (paysId != null) {
            AdmCountry pays = countryRepository.findById(paysId)
                    .orElseThrow(() -> new InvalidRequestException("AdmCountry introuvable: " + paysId));
            tier.setAdmCountry(pays);
        }
        if (nationaliteId != null) {
            AdmNationality nationalite = nationalityRepository.findById(nationaliteId)
                    .orElseThrow(() -> new InvalidRequestException("AdmNationality introuvable: " + nationaliteId));
            tier.setAdmNationality(nationalite);
        }
        if (situationFamilialeId != null) {
            AdmFamilySituation situation = familySituationRepository.findById(situationFamilialeId)
                    .orElseThrow(() -> new InvalidRequestException("AdmFamilySituation introuvable: " + situationFamilialeId));
            tier.setAdmFamilySituation(situation);
        }
        if (typeResidenceId != null) {
            AdmResidenceType typeResidence = residenceTypeRepository.findById(typeResidenceId)
                    .orElseThrow(() -> new InvalidRequestException("AdmResidenceType introuvable: " + typeResidenceId));
            tier.setAdmResidenceType(typeResidence);
        }
        if (regimeMatrimonialId != null) {
            AdmMatrimonialRegime regime = matrimonialRegimeRepository.findById(regimeMatrimonialId)
                    .orElseThrow(() -> new InvalidRequestException("AdmMatrimonialRegime introuvable: " + regimeMatrimonialId));
            tier.setAdmMatrimonialRegime(regime);
        }
        if (professionId != null) {
            AdmProfession profession = professionRepository.findById(professionId)
                    .orElseThrow(() -> new InvalidRequestException("AdmProfession introuvable: " + professionId));
            tier.setAdmProfession(profession);
        }
        if (salaireId != null) {
            AdmSalary salaire = salaryRepository.findById(salaireId)
                    .orElseThrow(() -> new InvalidRequestException("AdmSalary introuvable: " + salaireId));
            tier.setAdmSalary(salaire);
        }
        if (pieceIdentite != null) {
            if (pieceIdentite.typeId() != null) {
                AdmIdentityDocumentType type = identityDocumentTypeRepository.findById(pieceIdentite.typeId())
                        .orElseThrow(() -> new InvalidRequestException("AdmIdentityDocumentType introuvable: " + pieceIdentite.typeId()));
                tier.setAdmIdentityDocumentType(type);
            }
            if (pieceIdentite.numero() != null) tier.setIDNumber(pieceIdentite.numero());
            if (pieceIdentite.delivreeLe() != null) tier.setIDDate(pieceIdentite.delivreeLe());
            if (pieceIdentite.lieuDelivrance() != null) tier.setIDPlace(pieceIdentite.lieuDelivrance());
            if (pieceIdentite.valideJusquau() != null) tier.setIDValidateDate(pieceIdentite.valideJusquau());
            if (pieceIdentite.lieuValidite() != null) tier.setIDValidatePlace(pieceIdentite.lieuValidite());
        }
        // CfgTier.Description est le champ d'affichage canonique côté legacy (lu directement par
        // les rapports/vues SQL et les templates Stimulsoft, ex. ComSaleFicheTropPercu.yes) —
        // confirmé en base que ce n'était jamais renseigné à la création/mise à jour via cette
        // API (contrairement aux données legacy), ce qui le laissait vide même quand
        // prenom/nom/raisonSociale étaient fournis. Recalculé à chaque fois plutôt que seulement
        // à la création, pour rester en phase si le nom change ensuite via PATCH.
        if (prenom != null || nom != null || raisonSociale != null) {
            tier.setDescription(TierNomFormatter.nom(tier));
        }
    }

    private static ProspectListItemDto toListItem(CfgTier tier) {
        return new ProspectListItemDto(
                tier.getId(),
                tier.getCode(),
                tier.getFirstName(),
                tier.getLastName(),
                tier.getSocialReason(),
                tier.getPhone(),
                tier.getMobile(),
                tier.getEMail(),
                tier.getCity(),
                tier.getCfgTierType() != null ? tier.getCfgTierType().getCode() : null
        );
    }

    private ProspectDetailDto toDetail(CfgTier tier) {
        PieceIdentiteDto pieceIdentite = null;
        if (tier.getAdmIdentityDocumentType() != null || StringUtils.hasText(tier.getIDNumber())) {
            pieceIdentite = new PieceIdentiteDto(
                    tier.getAdmIdentityDocumentType() != null ? tier.getAdmIdentityDocumentType().getId() : null,
                    tier.getAdmIdentityDocumentType() != null ? tier.getAdmIdentityDocumentType().getDescription() : null,
                    tier.getIDNumber(),
                    tier.getIDDate(),
                    tier.getIDPlace(),
                    tier.getIDValidateDate(),
                    tier.getIDValidatePlace()
            );
        }

        return new ProspectDetailDto(
                tier.getId(),
                tier.getCode(),
                tier.getFirstName(),
                tier.getLastName(),
                tier.getSocialReason(),
                tier.getPhone(),
                tier.getPhoneIndicatif(),
                tier.getPhone1(),
                tier.getPhone2(),
                tier.getMobile(),
                tier.getMobileIndicatif(),
                tier.getFax(),
                tier.getEMail(),
                tier.getWebSite(),
                tier.getAddress(),
                tier.getAddress2(),
                tier.getAddress3(),
                tier.getZipCode(),
                tier.getCity(),
                tier.getBirthDate(),
                tier.getBirthPlace(),
                tier.getNbrChildren(),
                tier.getQuality(),
                tier.getNotes(),
                tier.getIce(),
                tier.getIsOldCustomer(),
                tier.getCfgTierType() != null ? tier.getCfgTierType().getId() : null,
                tier.getCfgTierType() != null ? tier.getCfgTierType().getDescription() : null,
                tier.getCfgTierKind() != null ? tier.getCfgTierKind().getId() : null,
                tier.getCfgTierKind() != null ? tier.getCfgTierKind().getDescription() : null,
                tier.getCfgCommercial() != null ? tier.getCfgCommercial().getId() : null,
                tier.getCfgCommercial() != null
                        ? ((tier.getCfgCommercial().getFirstName() != null ? tier.getCfgCommercial().getFirstName() + " " : "")
                        + (tier.getCfgCommercial().getLastName() != null ? tier.getCfgCommercial().getLastName() : "")).trim()
                        : null,
                tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getId() : null,
                tier.getAdmTierTitle() != null ? tier.getAdmTierTitle().getDescription() : null,
                tier.getAdmSexType() != null ? tier.getAdmSexType().getId() : null,
                tier.getAdmSexType() != null ? tier.getAdmSexType().getDescription() : null,
                tier.getAdmCountryResidence() != null ? tier.getAdmCountryResidence().getId() : null,
                tier.getAdmCountryResidence() != null ? tier.getAdmCountryResidence().getDescription() : null,
                tier.getAdmCountry() != null ? tier.getAdmCountry().getId() : null,
                tier.getAdmCountry() != null ? tier.getAdmCountry().getDescription() : null,
                tier.getAdmNationality() != null ? tier.getAdmNationality().getId() : null,
                tier.getAdmNationality() != null ? tier.getAdmNationality().getDescription() : null,
                tier.getAdmFamilySituation() != null ? tier.getAdmFamilySituation().getId() : null,
                tier.getAdmFamilySituation() != null ? tier.getAdmFamilySituation().getDescription() : null,
                tier.getAdmResidenceType() != null ? tier.getAdmResidenceType().getId() : null,
                tier.getAdmResidenceType() != null ? tier.getAdmResidenceType().getDescription() : null,
                tier.getAdmMatrimonialRegime() != null ? tier.getAdmMatrimonialRegime().getId() : null,
                tier.getAdmMatrimonialRegime() != null ? tier.getAdmMatrimonialRegime().getDescription() : null,
                tier.getAdmProfession() != null ? tier.getAdmProfession().getId() : null,
                tier.getAdmSalary() != null ? tier.getAdmSalary().getId() : null,
                tier.getAdmSalary() != null ? tier.getAdmSalary().getDescription() : null,
                tier.getEmployer(),
                tier.getAntiquity(),
                tier.getFatherName(),
                tier.getMotherName(),
                pieceIdentite
        );
    }
}
