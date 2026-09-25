package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.CritereDoublon;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.dto.prospect.PieceIdentiteDto;
import com.tsix.immocrm.dto.prospect.PieceIdentiteRequest;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectDoublonDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.dto.prospect.TrancheInteretDto;
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
import com.tsix.immocrm.exceptions.DoublonsProspectException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
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
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    /**
     * Les fiches candidates au contrôle de doublon — prospects <em>et</em> clients, comme le
     * contrôle d'OMRAN (types PRO et CLI dans la trace d'audit) : un client qui revient est le
     * doublon le plus coûteux à créer. Voir {@code FEATURE-doublons-prospect-projet.md} §2.
     */
    private static final Set<String> CODES_TYPE_PROSPECT_OU_CLIENT = Set.of("pro", "prornt", "cli", "clirnt");
    /** Plafond de la liste de doublons ({@code GET /api/prospects/doublons} et le 409). */
    private static final int PLAFOND_DOUBLONS = 50;
    /** Un téléphone se compare sur ses 9 derniers chiffres — la normalisation de la hotline. */
    private static final int CHIFFRES_TELEPHONE = 9;

    private final CfgTierRepository tierRepository;
    /**
     * Sert les <strong>tranches d'intérêt</strong> de la fiche — voir
     * {@code instructions/FEATURE-audit-tranches-prospect.md}. Une requête de plus sur la fiche
     * seulement : en liste, elle serait payée <em>par ligne</em>, soit vingt requêtes par page.
     * C'est pour cette raison que {@code tranchesInteret} ne figure pas au catalogue
     * {@link com.tsix.immocrm.domain.ColonneProspect} — l'exposer en liste demanderait un
     * chargement par lot, comme l'enrichissement de la prospection.
     */
    private final CfgTrancheRepository trancheRepository;
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
    private final GenerateurCodeSequence generateurCode;

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
                            CfgTrancheRepository trancheRepository,
                            NumeroTelephoneSupport numeroSupport,
                            GenerateurCodeSequence generateurCode) {
        this.trancheRepository = trancheRepository;
        this.numeroSupport = numeroSupport;
        this.generateurCode = generateurCode;
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
    public Page<ProspectListItemDto> findAll(String search, List<String> types, Pageable pageable,
                                            Set<ColonneProspect> colonnes) {
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
        return tierRepository.findAll(spec, pageable).map(t -> toListItem(t, colonnes));
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
        // Contrôle d'identité avant toute écriture : une fiche existante au même nom, téléphone
        // ou email arrête la création en 409 tant que l'appelant n'a pas confirmé « nouveau
        // contact ». Voir FEATURE-doublons-prospect-projet.md §2.
        if (!Boolean.TRUE.equals(request.ignorerDoublons())) {
            List<ProspectDoublonDto> doublons = rechercherDoublons(request.prenom(), request.nom(),
                    request.raisonSociale(), request.telephone(), request.mobile(), request.email());
            if (!doublons.isEmpty()) {
                throw new DoublonsProspectException(doublons);
            }
        }

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setIsProspect(true);
        // Drapeaux hérités posés à `false` plutôt que laissés à NULL, et ce n'est pas de la
        // cosmétique : les écrans du logiciel de bureau filtrent en « = 0 », et en SQL
        // `NULL = 0` n'est JAMAIS vrai. Une ligne laissée à NULL est donc silencieusement
        // absente de leurs listes — mesuré sur CfgTier comme sur ComProspection.
        // C'est ainsi qu'OMRAN remplit ses propres lignes ; on s'aligne.
        tier.setIsOldCustomer(false);
        tier.setIsExtern(false);
        tier.setIsRntCustomer(false);
        tier.setIsVisibleInList(false);
        tier.setIsFundReader(false);
        // Numérotation sur le compteur du logiciel de bureau. Sans elle, chaque prospect créé
        // par l'API rejoignait une anomalie marginale : sur 13 913 prospects et clients
        // réels, 12 seulement n'ont pas de code. Couvre les deux chemins de création, celui-ci
        // étant partagé par POST /api/prospects et
        // POST /api/pre-prospections/{id}/creer-prospect.
        tier.setCode(generateurCode.suivant(GenerateurCodeSequence.SEQUENCE_TIER, 2, 5));
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
     * {@code GET /api/prospects/doublons} et le 409 de {@link #createEntity} : les fiches
     * prospect/client correspondant à l'identité saisie sur au moins un critère, avec ces
     * critères — voir {@code FEATURE-doublons-prospect-projet.md} §2 pour les règles et les
     * mesures (244 groupes d'homonymes sur 14 040 fiches, mais 6 seulement à nom <em>et</em>
     * téléphone identiques : l'homonyme au contact différent est le cas courant, d'où une liste à
     * arbitrer plutôt qu'un refus).
     *
     * <p>Les valeurs bidon ne sont pas des critères : téléphone de moins de 9 chiffres ou à 9
     * chiffres identiques ({@code 000000000} et {@code 111111111} portent 17 fiches chacun), email
     * sans {@code @} ({@code -}, {@code nd}, {@code .}). Aucun critère utilisable ⟶ liste vide.
     *
     * <p>Tri : les fiches qui correspondent sur le plus de critères d'abord, puis les plus
     * récentes ; 50 au plus.
     */
    public List<ProspectDoublonDto> rechercherDoublons(String prenom, String nom, String raisonSociale,
                                                       String telephone, String mobile, String email) {
        String cle = cleNom(prenom, nom, raisonSociale);
        String tel1 = critereTelephone(telephone);
        String tel2 = critereTelephone(mobile);
        String mail = critereEmail(email);
        if (cle.isEmpty() && tel1.isEmpty() && tel2.isEmpty() && mail.isEmpty()) {
            return List.of();
        }
        return tierRepository.trouverDoublons(cle, tel1, tel2, mail, PageRequest.of(0, PLAFOND_DOUBLONS)).stream()
                .map(t -> toDoublon(t, cle, tel1, tel2, mail))
                .filter(d -> !d.criteres().isEmpty())
                .sorted(Comparator.comparingInt((ProspectDoublonDto d) -> d.criteres().size()).reversed()
                        .thenComparing(ProspectDoublonDto::dateCreation,
                                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();
    }

    /** Vrai si au moins un des six champs d'identité constitue un critère utilisable. */
    public static boolean aUnCritereDeDoublon(String prenom, String nom, String raisonSociale,
                                              String telephone, String mobile, String email) {
        return !cleNom(prenom, nom, raisonSociale).isEmpty() || !critereTelephone(telephone).isEmpty()
                || !critereTelephone(mobile).isEmpty() || !critereEmail(email).isEmpty();
    }

    /** Le sous-ensemble de tiers concerné par le contrôle de doublon et par {@code lier-prospect}. */
    public static boolean estProspectOuClient(CfgTier tier) {
        return tier.getCfgTierType() != null && tier.getCfgTierType().getCode() != null
                && CODES_TYPE_PROSPECT_OU_CLIENT.contains(tier.getCfgTierType().getCode().toLowerCase(Locale.ROOT));
    }

    private ProspectDoublonDto toDoublon(CfgTier t, String cle, String tel1, String tel2, String mail) {
        List<CritereDoublon> criteres = new ArrayList<>();
        if (!cle.isEmpty() && (cle.equals(sansEspaces(chaine(t.getFirstName()) + chaine(t.getLastName())))
                || cle.equals(sansEspaces(chaine(t.getLastName()) + chaine(t.getFirstName())))
                || cle.equals(sansEspaces(t.getDescription()))
                || cle.equals(sansEspaces(t.getSocialReason())))) {
            criteres.add(CritereDoublon.NOM);
        }
        List<String> telephonesFiche = List.of(neufDerniers(t.getPhone()), neufDerniers(t.getPhone1()),
                neufDerniers(t.getPhone2()), neufDerniers(t.getMobile()));
        if ((!tel1.isEmpty() && telephonesFiche.contains(tel1)) || (!tel2.isEmpty() && telephonesFiche.contains(tel2))) {
            criteres.add(CritereDoublon.TELEPHONE);
        }
        if (!mail.isEmpty() && mail.equals(chaine(t.getEMail()).trim().toLowerCase(Locale.ROOT))) {
            criteres.add(CritereDoublon.EMAIL);
        }
        return new ProspectDoublonDto(t.getId(), t.getCode(), TierNomFormatter.nom(t), t.getFirstName(),
                t.getLastName(), t.getSocialReason(), t.getPhone(), t.getMobile(), t.getEMail(), t.getCity(),
                t.getCfgTierType() != null ? t.getCfgTierType().getCode() : null, t.getUserCreateDateTime(),
                List.copyOf(criteres));
    }

    /**
     * Clé de nom : prénom + nom (ou la raison sociale seule) en minuscules et <b>sans aucun
     * espace</b> — la normalisation d'OMRAN ({@code REPLACE(FirstName,' ','')}), qui rend
     * « El Mehdi LAHLOU » et « El MEHDI LAHLOU » égaux et neutralise l'ordre prénom/nom côté
     * requête (les deux concaténations sont comparées).
     */
    private static String cleNom(String prenom, String nom, String raisonSociale) {
        String cle = sansEspaces(chaine(prenom) + chaine(nom));
        return cle.isEmpty() ? sansEspaces(raisonSociale) : cle;
    }

    /** Critère téléphone : 9 derniers chiffres, ou {@code ""} si trop court ou bidon (chiffres tous égaux). */
    private static String critereTelephone(String telephone) {
        String neuf = neufDerniers(telephone);
        if (neuf.length() < CHIFFRES_TELEPHONE || neuf.chars().allMatch(c -> c == neuf.charAt(0))) {
            return "";
        }
        return neuf;
    }

    /** Critère email : minuscules sans espaces de bord, ou {@code ""} sans {@code @} (« - », « nd », « . »). */
    private static String critereEmail(String email) {
        String e = chaine(email).trim().toLowerCase(Locale.ROOT);
        return e.contains("@") ? e : "";
    }

    /**
     * Même chaîne de {@code replace} que {@code CfgTierRepository.trouverDoublons} — les deux
     * côtés doivent se normaliser à l'identique pour que chaque ligne renvoyée par la requête
     * retrouve son critère ici.
     */
    private static String neufDerniers(String telephone) {
        String t = chaine(telephone).replace(" ", "").replace("-", "").replace(".", "")
                .replace("(", "").replace(")", "").replace("+", "");
        return t.length() <= CHIFFRES_TELEPHONE ? t : t.substring(t.length() - CHIFFRES_TELEPHONE);
    }

    private static String sansEspaces(String valeur) {
        return chaine(valeur).replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private static String chaine(String valeur) {
        return valeur == null ? "" : valeur;
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

    /**
     * Mappeur de la liste des prospects.
     *
     * <p>Les <strong>référentiels ne sont déréférencés que si la colonne est demandée</strong> :
     * chacun est un chargement paresseux de plus par ligne, soit vingt requêtes inutiles par page
     * quand personne ne les réclame. Les scalaires de {@code CfgTier}, eux, sont déjà chargés —
     * les conditionner n'apporterait rien et alourdirait la lecture.
     *
     * @param colonnes sélection résolue ; {@code Set.of()} pour les appels hors liste, qui ne
     *                 déréférenceront alors aucun référentiel optionnel.
     */
    private static ProspectListItemDto toListItem(CfgTier tier, Set<ColonneProspect> colonnes) {
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
                tier.getCfgTierType() != null ? tier.getCfgTierType().getCode() : null,
                tier.getIDNumber(),
                tier.getIce(),
                tier.getBirthDate(),
                tier.getBirthPlace(),
                tier.getNbrChildren(),
                tier.getIsOldCustomer(),
                tier.getFatherName(),
                tier.getMotherName(),
                tier.getPhoneIndicatif(),
                tier.getPhone1(),
                tier.getPhone2(),
                tier.getMobileIndicatif(),
                tier.getFax(),
                tier.getWebSite(),
                tier.getAddress(),
                tier.getAddress2(),
                tier.getAddress3(),
                tier.getZipCode(),
                tier.getEmployer(),
                tier.getAntiquity(),
                colonnes.contains(ColonneProspect.KIND) && tier.getCfgTierKind() != null
                        ? tier.getCfgTierKind().getDescription() : null,
                colonnes.contains(ColonneProspect.TITRE) && tier.getAdmTierTitle() != null
                        ? tier.getAdmTierTitle().getDescription() : null,
                colonnes.contains(ColonneProspect.SEXE) && tier.getAdmSexType() != null
                        ? tier.getAdmSexType().getDescription() : null,
                colonnes.contains(ColonneProspect.NATIONALITE) && tier.getAdmNationality() != null
                        ? tier.getAdmNationality().getDescription() : null,
                colonnes.contains(ColonneProspect.SITUATION_FAMILIALE) && tier.getAdmFamilySituation() != null
                        ? tier.getAdmFamilySituation().getDescription() : null,
                colonnes.contains(ColonneProspect.REGIME_MATRIMONIAL) && tier.getAdmMatrimonialRegime() != null
                        ? tier.getAdmMatrimonialRegime().getDescription() : null,
                colonnes.contains(ColonneProspect.PAYS) && tier.getAdmCountry() != null
                        ? tier.getAdmCountry().getDescription() : null,
                colonnes.contains(ColonneProspect.PAYS_RESIDENCE) && tier.getAdmCountryResidence() != null
                        ? tier.getAdmCountryResidence().getDescription() : null,
                colonnes.contains(ColonneProspect.TYPE_RESIDENCE) && tier.getAdmResidenceType() != null
                        ? tier.getAdmResidenceType().getDescription() : null,
                // Alimentée par CfgTier.Quality, mesurée à 0 ligne sur 13 664 : toujours nulle.
                tier.getQuality(),
                colonnes.contains(ColonneProspect.SALAIRE) && tier.getAdmSalary() != null
                        ? tier.getAdmSalary().getDescription() : null,
                colonnes.contains(ColonneProspect.COMMERCIAL_ID) && tier.getCfgCommercial() != null
                        ? tier.getCfgCommercial().getId() : null,
                colonnes.contains(ColonneProspect.COMMERCIAL_NOM) && tier.getCfgCommercial() != null
                        ? TierNomFormatter.nom(tier.getCfgCommercial()) : null,
                tier.getUserCreate(),
                tier.getUserCreateDateTime(),
                tier.getUserUpdate(),
                tier.getUserUpdateDateTime()
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
                pieceIdentite,
                tier.getUserCreate(),
                tier.getUserCreateDateTime(),
                tier.getUserUpdate(),
                tier.getUserUpdateDateTime(),
                tranchesInteret(tier)
        );
    }

    /**
     * Tranches d'intérêt du prospect, dans la forme structurée que l'API sert à la place de la
     * chaîne agrégée de la vue légataire.
     *
     * <p>Un tiers qui n'a pas encore d'identifiant — cas d'un {@code toDetail} appelé sur une
     * entité tout juste construite — n'a par définition aucune prospection : rendre la liste vide
     * sans interroger la base est ici correct, pas une optimisation prématurée.
     *
     * @return liste toujours non nulle, vide quand le prospect n'a aucune prospection.
     */
    private List<TrancheInteretDto> tranchesInteret(CfgTier tier) {
        if (tier.getId() == null) {
            return List.of();
        }
        return trancheRepository.tranchesInteret(tier.getId()).stream()
                .map(t -> new TrancheInteretDto(t.getId(), t.getCode(), t.getDescription()))
                .toList();
    }
}
