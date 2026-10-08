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
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
public class ProspectService {

    // Un prospect OMRAN se reconnaît à son CfgTierType (PRO, PRORNT), pas à CfgTier.IsProspect,
    // drapeau que le légataire ne renseigne presque jamais.
    private static final List<String> CODES_TYPE_PROSPECT = List.of("pro", "prornt");
    private static final String CODE_TYPE_PROSPECT_PAR_DEFAUT = "PRO";
    /** Prospects <em>et</em> clients, comme le contrôle d'OMRAN — voir {@code FEATURE-doublons-prospect-projet.md} §2. */
    private static final Set<String> CODES_TYPE_PROSPECT_OU_CLIENT = Set.of("pro", "prornt", "cli", "clirnt");
    private static final int PLAFOND_DOUBLONS = 50;
    /** Un téléphone se compare sur ses 9 derniers chiffres — la normalisation de la hotline. */
    private static final int CHIFFRES_TELEPHONE = 9;

    private final CfgTierRepository tierRepository;
    // Tranches d'intérêt : fiche seulement (une requête par ligne en liste), d'où leur absence de
    // ColonneProspect. Voir FEATURE-audit-tranches-prospect.md §3.
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
    private final NumeroTelephoneSupport numeroSupport;
    private final GenerateurCodeSequence generateurCode;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

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
                            GenerateurCodeSequence generateurCode,
                            UtilisateurAffiche utilisateurAffiche,
                            Clock clock) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
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
     * @param types codes {@code CfgTierType} en OU ; fournis, ils <b>remplacent</b> le filtre par
     *              défaut {@link #CODES_TYPE_PROSPECT} au lieu de s'y ajouter
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
        return toDetail(creerEntite(request));
    }

    /** Comme {@link #create}, mais rend l'entité — sert aussi {@code POST /api/pre-prospections/{id}/creer-prospect}. */
    @Transactional
    public CfgTier createEntity(CreerProspectRequest request) {
        return creerEntite(request);
    }

    private CfgTier creerEntite(CreerProspectRequest request) {
        if (!StringUtils.hasText(request.nom()) && !StringUtils.hasText(request.raisonSociale())) {
            throw new InvalidRequestException("nom ou raisonSociale est requis");
        }
        // 409 tant que l'appelant n'a pas confirmé « nouveau contact » (FEATURE-doublons-prospect-projet.md §2).
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
        // false et non NULL : les écrans OMRAN filtrent en « = 0 », et NULL = 0 n'est jamais vrai —
        // une ligne à NULL disparaîtrait de leurs listes.
        tier.setIsOldCustomer(false);
        tier.setIsExtern(false);
        tier.setIsRntCustomer(false);
        tier.setIsVisibleInList(false);
        tier.setIsFundReader(false);
        // Même compteur qu'OMRAN, sinon codes en collision. Voir FIX-creation-prospect-prospection.md §3.
        tier.setCode(generateurCode.suivant(GenerateurCodeSequence.SEQUENCE_TIER, 2, 5));
        tier.setUserCreate(utilisateurAffiche.courant().nom());
        tier.setUserCreateDateTime(LocalDateTime.now(clock));
        applyFields(tier, ChampsProspect.de(request));
        // Sans type PRO, un prospect créé sans typeId n'apparaîtrait pas dans sa propre liste.
        if (request.typeId() == null) {
            tier.setCfgTierType(resolveTypeProspectParDefaut());
        }

        return tierRepository.save(tier);
    }

    /**
     * Fiches prospect/client partageant au moins un critère d'identité : une liste à arbitrer,
     * pas un refus — voir {@code FEATURE-doublons-prospect-projet.md} §2. Les valeurs bidon
     * (téléphone court ou à chiffres tous égaux, email sans {@code @}) ne sont pas des critères.
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

    // Sans aucun espace, comme OMRAN (REPLACE(FirstName,' ','')) ; les deux ordres prénom/nom
    // sont comparés côté requête.
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

    // Même chaîne de replace que CfgTierRepository.trouverDoublons, sinon une ligne renvoyée par
    // la requête ne retrouverait pas son critère ici.
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

    // Crée la ligne PRO si elle manque (base de test fraîche) plutôt que de bloquer toute création.
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

        applyFields(tier, ChampsProspect.de(request));

        return toDetail(tierRepository.save(tier));
    }

    private void applyFields(CfgTier tier, ChampsProspect c) {
        // Les deux contrôles avant toute écriture : un mobile valide et un fixe fautif n'écrivent
        // rien, sans dépendre du rollback. Voir FEATURE-indicatif-telephone.md §5.
        NumeroTelephoneSupport.NumeroNormalise fixe =
                numeroSupport.valider(c.telephone(), c.indicatifTelephone(), "telephone");
        NumeroTelephoneSupport.NumeroNormalise portable =
                numeroSupport.valider(c.mobile(), c.indicatifMobile(), "mobile");

        appliquerNomEtCoordonnees(tier, c, fixe, portable);
        appliquerEtatCivil(tier, c);
        appliquerClassement(tier, c);
        appliquerSituation(tier, c);
        if (c.pieceIdentite() != null) {
            appliquerPieceIdentite(tier, c.pieceIdentite());
        }
        // CfgTier.Description est le nom qu'affichent les vues SQL et templates Stimulsoft du
        // légataire ; recalculé à chaque changement de nom, pas seulement à la création.
        if (c.prenom() != null || c.nom() != null || c.raisonSociale() != null) {
            tier.setDescription(TierNomFormatter.nom(tier));
        }
    }

    private static void appliquerNomEtCoordonnees(CfgTier tier, ChampsProspect c,
                                                  NumeroTelephoneSupport.NumeroNormalise fixe,
                                                  NumeroTelephoneSupport.NumeroNormalise portable) {
        if (c.prenom() != null) tier.setFirstName(c.prenom());
        if (c.nom() != null) tier.setLastName(c.nom());
        if (c.raisonSociale() != null) tier.setSocialReason(c.raisonSociale());
        if (fixe != null) {
            tier.setPhone(fixe.numero());
            tier.setPhoneIndicatif(fixe.indicatif());
        }
        if (portable != null) {
            tier.setMobile(portable.numero());
            tier.setMobileIndicatif(portable.indicatif());
        }
        if (c.email() != null) tier.setEMail(c.email());
        if (c.adresse() != null) tier.setAddress(c.adresse());
        if (c.adresse2() != null) tier.setAddress2(c.adresse2());
        if (c.adresse3() != null) tier.setAddress3(c.adresse3());
        if (c.ville() != null) tier.setCity(c.ville());
        if (c.codePostal() != null) tier.setZipCode(c.codePostal());
    }

    private static void appliquerEtatCivil(CfgTier tier, ChampsProspect c) {
        if (c.dateNaissance() != null) tier.setBirthDate(c.dateNaissance());
        if (c.lieuNaissance() != null) tier.setBirthPlace(c.lieuNaissance());
        if (c.nombreEnfants() != null) tier.setNbrChildren(c.nombreEnfants());
        if (c.employeur() != null) tier.setEmployer(c.employeur());
        if (c.anciennete() != null) tier.setAntiquity(c.anciennete());
        if (c.filsDe() != null) tier.setFatherName(c.filsDe());
        if (c.etDe() != null) tier.setMotherName(c.etDe());
    }

    private void appliquerClassement(CfgTier tier, ChampsProspect c) {
        if (c.typeId() != null) {
            tier.setCfgTierType(exiger(tierTypeRepository::findById, c.typeId(), "CfgTierType"));
        }
        if (c.kindId() != null) {
            tier.setCfgTierKind(exiger(tierKindRepository::findById, c.kindId(), "CfgTierKind"));
        }
        if (c.commercialId() != null) {
            tier.setCfgCommercial(exiger(tierRepository::findById, c.commercialId(), "Commercial (CfgTier)"));
        }
        if (c.titreId() != null) {
            tier.setAdmTierTitle(exiger(tierTitleRepository::findById, c.titreId(), "AdmTierTitle"));
        }
        if (c.sexeId() != null) {
            tier.setAdmSexType(exiger(sexTypeRepository::findById, c.sexeId(), "AdmSexType"));
        }
        if (c.paysResidenceId() != null) {
            tier.setAdmCountryResidence(exiger(countryRepository::findById, c.paysResidenceId(), "AdmCountry"));
        }
        if (c.paysId() != null) {
            tier.setAdmCountry(exiger(countryRepository::findById, c.paysId(), "AdmCountry"));
        }
    }

    private void appliquerSituation(CfgTier tier, ChampsProspect c) {
        if (c.nationaliteId() != null) {
            tier.setAdmNationality(exiger(nationalityRepository::findById, c.nationaliteId(), "AdmNationality"));
        }
        if (c.situationFamilialeId() != null) {
            tier.setAdmFamilySituation(exiger(familySituationRepository::findById, c.situationFamilialeId(),
                    "AdmFamilySituation"));
        }
        if (c.typeResidenceId() != null) {
            tier.setAdmResidenceType(exiger(residenceTypeRepository::findById, c.typeResidenceId(),
                    "AdmResidenceType"));
        }
        if (c.regimeMatrimonialId() != null) {
            tier.setAdmMatrimonialRegime(exiger(matrimonialRegimeRepository::findById, c.regimeMatrimonialId(),
                    "AdmMatrimonialRegime"));
        }
        if (c.professionId() != null) {
            tier.setAdmProfession(exiger(professionRepository::findById, c.professionId(), "AdmProfession"));
        }
        if (c.salaireId() != null) {
            tier.setAdmSalary(exiger(salaryRepository::findById, c.salaireId(), "AdmSalary"));
        }
    }

    private void appliquerPieceIdentite(CfgTier tier, PieceIdentiteRequest pieceIdentite) {
        if (pieceIdentite.typeId() != null) {
            tier.setAdmIdentityDocumentType(exiger(identityDocumentTypeRepository::findById, pieceIdentite.typeId(),
                    "AdmIdentityDocumentType"));
        }
        if (pieceIdentite.numero() != null) tier.setIDNumber(pieceIdentite.numero());
        if (pieceIdentite.delivreeLe() != null) tier.setIDDate(pieceIdentite.delivreeLe());
        if (pieceIdentite.lieuDelivrance() != null) tier.setIDPlace(pieceIdentite.lieuDelivrance());
        if (pieceIdentite.valideJusquau() != null) tier.setIDValidateDate(pieceIdentite.valideJusquau());
        if (pieceIdentite.lieuValidite() != null) tier.setIDValidatePlace(pieceIdentite.lieuValidite());
    }

    /** Référentiel désigné par l'appelant : absent ⇒ 400 « {@code <libelle>} introuvable: id ». */
    private static <T> T exiger(Function<UUID, Optional<T>> recherche, UUID id, String libelle) {
        return recherche.apply(id)
                .orElseThrow(() -> new InvalidRequestException(libelle + " introuvable: " + id));
    }

    // Un référentiel n'est déréférencé que si sa colonne est demandée : chacun coûte un chargement
    // paresseux par ligne. Les scalaires, déjà chargés, ne sont pas conditionnés.
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
                siDemandee(colonnes, ColonneProspect.KIND, tier::getCfgTierKind, CfgTierKind::getDescription),
                siDemandee(colonnes, ColonneProspect.TITRE, tier::getAdmTierTitle, AdmTierTitle::getDescription),
                siDemandee(colonnes, ColonneProspect.SEXE, tier::getAdmSexType, AdmSexType::getDescription),
                siDemandee(colonnes, ColonneProspect.NATIONALITE, tier::getAdmNationality,
                        AdmNationality::getDescription),
                siDemandee(colonnes, ColonneProspect.SITUATION_FAMILIALE, tier::getAdmFamilySituation,
                        AdmFamilySituation::getDescription),
                siDemandee(colonnes, ColonneProspect.REGIME_MATRIMONIAL, tier::getAdmMatrimonialRegime,
                        AdmMatrimonialRegime::getDescription),
                siDemandee(colonnes, ColonneProspect.PAYS, tier::getAdmCountry, AdmCountry::getDescription),
                siDemandee(colonnes, ColonneProspect.PAYS_RESIDENCE, tier::getAdmCountryResidence,
                        AdmCountry::getDescription),
                siDemandee(colonnes, ColonneProspect.TYPE_RESIDENCE, tier::getAdmResidenceType,
                        AdmResidenceType::getDescription),
                // CfgTier.Quality n'est jamais renseignée en base : toujours nulle.
                tier.getQuality(),
                siDemandee(colonnes, ColonneProspect.SALAIRE, tier::getAdmSalary, AdmSalary::getDescription),
                siDemandee(colonnes, ColonneProspect.COMMERCIAL_ID, tier::getCfgCommercial, CfgTier::getId),
                siDemandee(colonnes, ColonneProspect.COMMERCIAL_NOM, tier::getCfgCommercial, TierNomFormatter::nom),
                tier.getUserCreate(),
                tier.getUserCreateDateTime(),
                tier.getUserUpdate(),
                tier.getUserUpdateDateTime()
        );
    }

    /** Référentiel déréférencé seulement si sa colonne est demandée ; {@code null} sinon ou s'il manque. */
    private static <R, T> T siDemandee(Set<ColonneProspect> colonnes, ColonneProspect colonne,
                                       Supplier<R> reference, Function<R, T> valeur) {
        if (!colonnes.contains(colonne)) {
            return null;
        }
        return siPresent(reference.get(), valeur);
    }

    private static <R, T> T siPresent(R reference, Function<R, T> valeur) {
        return reference != null ? valeur.apply(reference) : null;
    }

    private static PieceIdentiteDto pieceIdentite(CfgTier tier) {
        if (tier.getAdmIdentityDocumentType() == null && !StringUtils.hasText(tier.getIDNumber())) {
            return null;
        }
        return new PieceIdentiteDto(
                siPresent(tier.getAdmIdentityDocumentType(), AdmIdentityDocumentType::getId),
                siPresent(tier.getAdmIdentityDocumentType(), AdmIdentityDocumentType::getDescription),
                tier.getIDNumber(),
                tier.getIDDate(),
                tier.getIDPlace(),
                tier.getIDValidateDate(),
                tier.getIDValidatePlace()
        );
    }

    /** « Prénom Nom », sans espace de bord si l'un des deux manque. */
    private static String nomCommercial(CfgTier commercial) {
        String prenom = commercial.getFirstName() != null ? commercial.getFirstName() + " " : "";
        String nom = commercial.getLastName() != null ? commercial.getLastName() : "";
        return (prenom + nom).trim();
    }

    private ProspectDetailDto toDetail(CfgTier tier) {
        PieceIdentiteDto pieceIdentite = pieceIdentite(tier);

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
                siPresent(tier.getCfgTierType(), CfgTierType::getId),
                siPresent(tier.getCfgTierType(), CfgTierType::getDescription),
                siPresent(tier.getCfgTierKind(), CfgTierKind::getId),
                siPresent(tier.getCfgTierKind(), CfgTierKind::getDescription),
                siPresent(tier.getCfgCommercial(), CfgTier::getId),
                siPresent(tier.getCfgCommercial(), ProspectService::nomCommercial),
                siPresent(tier.getAdmTierTitle(), AdmTierTitle::getId),
                siPresent(tier.getAdmTierTitle(), AdmTierTitle::getDescription),
                siPresent(tier.getAdmSexType(), AdmSexType::getId),
                siPresent(tier.getAdmSexType(), AdmSexType::getDescription),
                siPresent(tier.getAdmCountryResidence(), AdmCountry::getId),
                siPresent(tier.getAdmCountryResidence(), AdmCountry::getDescription),
                siPresent(tier.getAdmCountry(), AdmCountry::getId),
                siPresent(tier.getAdmCountry(), AdmCountry::getDescription),
                siPresent(tier.getAdmNationality(), AdmNationality::getId),
                siPresent(tier.getAdmNationality(), AdmNationality::getDescription),
                siPresent(tier.getAdmFamilySituation(), AdmFamilySituation::getId),
                siPresent(tier.getAdmFamilySituation(), AdmFamilySituation::getDescription),
                siPresent(tier.getAdmResidenceType(), AdmResidenceType::getId),
                siPresent(tier.getAdmResidenceType(), AdmResidenceType::getDescription),
                siPresent(tier.getAdmMatrimonialRegime(), AdmMatrimonialRegime::getId),
                siPresent(tier.getAdmMatrimonialRegime(), AdmMatrimonialRegime::getDescription),
                siPresent(tier.getAdmProfession(), AdmProfession::getId),
                siPresent(tier.getAdmSalary(), AdmSalary::getId),
                siPresent(tier.getAdmSalary(), AdmSalary::getDescription),
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

    /** Forme structurée de la chaîne agrégée de la vue légataire ; jamais {@code null}. */
    private List<TrancheInteretDto> tranchesInteret(CfgTier tier) {
        if (tier.getId() == null) {
            return List.of();
        }
        return trancheRepository.tranchesInteret(tier.getId()).stream()
                .map(t -> new TrancheInteretDto(t.getId(), t.getCode(), t.getDescription()))
                .toList();
    }

    /** Champs communs à la création et à la modification : {@code null} = champ laissé tel quel. */
    private record ChampsProspect(String prenom, String nom, String raisonSociale, String telephone,
                                  String indicatifTelephone, String mobile, String indicatifMobile,
                                  String email, String adresse, String ville, String codePostal,
                                  LocalDateTime dateNaissance, String lieuNaissance, Integer nombreEnfants,
                                  UUID typeId, UUID kindId, UUID commercialId, UUID titreId, UUID sexeId,
                                  UUID paysResidenceId, UUID paysId, UUID nationaliteId, UUID situationFamilialeId,
                                  UUID typeResidenceId, UUID regimeMatrimonialId, UUID professionId, UUID salaireId,
                                  String employeur, Integer anciennete, String filsDe, String etDe, String adresse2,
                                  String adresse3, PieceIdentiteRequest pieceIdentite) {

        static ChampsProspect de(CreerProspectRequest r) {
            return new ChampsProspect(r.prenom(), r.nom(), r.raisonSociale(), r.telephone(),
                    r.indicatifTelephone(), r.mobile(), r.indicatifMobile(),
                    r.email(), r.adresse(), r.ville(), r.codePostal(),
                    r.dateNaissance(), r.lieuNaissance(), r.nombreEnfants(), r.typeId(),
                    r.kindId(), r.commercialId(), r.titreId(), r.sexeId(),
                    r.paysResidenceId(), r.paysId(), r.nationaliteId(), r.situationFamilialeId(),
                    r.typeResidenceId(), r.regimeMatrimonialId(), r.professionId(), r.salaireId(),
                    r.employeur(), r.anciennete(), r.filsDe(), r.etDe(), r.adresse2(),
                    r.adresse3(), r.pieceIdentite());
        }

        static ChampsProspect de(ModifierProspectRequest r) {
            return new ChampsProspect(r.prenom(), r.nom(), r.raisonSociale(), r.telephone(),
                    r.indicatifTelephone(), r.mobile(), r.indicatifMobile(),
                    r.email(), r.adresse(), r.ville(), r.codePostal(),
                    r.dateNaissance(), r.lieuNaissance(), r.nombreEnfants(), r.typeId(),
                    r.kindId(), r.commercialId(), r.titreId(), r.sexeId(),
                    r.paysResidenceId(), r.paysId(), r.nationaliteId(), r.situationFamilialeId(),
                    r.typeResidenceId(), r.regimeMatrimonialId(), r.professionId(), r.salaireId(),
                    r.employeur(), r.anciennete(), r.filsDe(), r.etDe(), r.adresse2(),
                    r.adresse3(), r.pieceIdentite());
        }
    }
}
