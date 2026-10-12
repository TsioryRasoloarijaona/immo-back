package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.dto.parcours.DoublonTriDto;
import com.tsix.immocrm.dto.parcours.DoublonTriDto.NatureDoublon;
import com.tsix.immocrm.dto.parcours.LigneTriDto;
import com.tsix.immocrm.dto.prospect.CritereDoublon;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository.DerniereAlerteProjection;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Boîte de tri des pré-prospections du commercial (FEATURE-parcours-commercial.md §3). */
@Service
@Transactional(readOnly = true)
public class BoiteDeTriService {

    static final String TYPE_TELEPHONE_SORTANT = "Téléphone Sortant";
    static final String RESULTAT_INJOIGNABLE = "Prospect injoignable ou Boite vocale";
    static final String MESSAGE_INJOIGNABLE = "Pas joignable";
    /** « Pas joignable » = une relance le lendemain à cette heure, rien d'automatique (décision §1.2). */
    static final LocalTime HEURE_RELANCE = LocalTime.of(10, 0);
    private static final int DOUBLONS_MAX = 5;
    private static final String REFERENTIEL_MANQUANT = "Référentiel des interactions incomplet : « %s » introuvable.";

    private final ComProspectionRepository prospectionRepository;
    private final ComActionMessageRepository messageRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageSubCategoryRepository resultatRepository;
    private final PerimetreCommercialService perimetreService;
    private final ProspectionService prospectionService;
    private final ProspectService prospectService;
    private final PorteeParcours portee;
    private final Clock clock;

    @SuppressWarnings("java:S107") // un dépôt par source de la ligne de tri
    public BoiteDeTriService(ComProspectionRepository prospectionRepository, ComActionMessageRepository messageRepository,
                             ComActionMessageTypeRepository typeRepository,
                             ComActionMessageSubCategoryRepository resultatRepository,
                             PerimetreCommercialService perimetreService, ProspectionService prospectionService,
                             ProspectService prospectService, PorteeParcours portee, Clock clock) {
        this.prospectionRepository = prospectionRepository;
        this.messageRepository = messageRepository;
        this.typeRepository = typeRepository;
        this.resultatRepository = resultatRepository;
        this.perimetreService = perimetreService;
        this.prospectionService = prospectionService;
        this.prospectService = prospectService;
        this.portee = portee;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- lecture

    /** Pré-prospections du commercial appelant, ouvertes, les plus récentes d'abord ; {@code toutes} lève les 6 mois. */
    public Page<LigneTriDto> lister(boolean toutes, Pageable pageable) {
        UUID commercial = portee.commercialLu(null);
        LocalDate aujourdhui = LocalDate.now(clock);
        Specification<ComProspection> spec = perimetreService.filtreProspections()
                .and(LignesParcours.aTrier(commercial, toutes ? null : aujourdhui.minusMonths(LignesParcours.MOIS_RECENTS)));
        Page<ComProspection> page = prospectionRepository.findAll(spec, PageRequest.of(pageable.getPageNumber(),
                pageable.getPageSize(), Sort.by(Sort.Order.desc(LignesParcours.DOC_DATE), Sort.Order.asc("id"))));
        Map<UUID, DerniereAlerteProjection> alertes = page.isEmpty() ? Map.of()
                : messageRepository.dernieresAlertes(page.getContent().stream().map(ComProspection::getId).toList())
                .stream().collect(Collectors.toMap(DerniereAlerteProjection::getProspectionId, Function.identity(),
                        (a, b) -> a));
        return page.map(p -> toLigne(p, alertes.get(p.getId()), aujourdhui));
    }

    private LigneTriDto toLigne(ComProspection p, DerniereAlerteProjection alerte, LocalDate aujourdhui) {
        CfgTranche tranche = p.getStkHierarchy();
        return new LigneTriDto(p.getId(), p.getCode(), LignesParcours.nom(p),
                Champs.de(p.getCfgProspect(), CfgTier::getId), LignesParcours.telephone(p),
                LignesParcours.indicatif(p), ProspectionService.emailProspect(p.getCfgProspect(), p),
                p.getDocDate(), JourneeService.ageJours(p, aujourdhui), p.getNotes(),
                Champs.de(p.getComProspectionOrigin(), ComProspectionOrigin::getCode),
                Champs.de(p.getComProspectionOriginSupport(), ComProspectionOriginSupport::getDescription),
                Champs.de(p.getComAction(), ComAction::getDescription),
                Champs.de(tranche, CfgTranche::getId), LignesParcours.trancheCode(p), LignesParcours.projetCode(p),
                Champs.de(p.getComProspectionKind(), ComProspectionKind::getCode),
                Champs.de(alerte, DerniereAlerteProjection::getDateAlerte),
                doublons(p));
    }

    /**
     * Règle des doublons de la fiche prospect ({@code ProspectService}) plus les autres prospections au même numéro
     * ou e-mail, filtrées par le périmètre : un doublon caché ailleurs en 404 n'est jamais nommé.
     */
    private List<DoublonTriDto> doublons(ComProspection p) {
        String telephone = ProspectService.critereTelephone(p.getTempTel());
        String email = ProspectService.critereEmail(p.getTempEMail());
        if (telephone.isEmpty() && email.isEmpty()) {
            return List.of();
        }
        UUID ficheLiee = Champs.de(p.getCfgProspect(), CfgTier::getId);
        Stream<DoublonTriDto> fiches = prospectService.rechercherDoublons(null, null, null, p.getTempTel(), null,
                        p.getTempEMail()).stream()
                .filter(d -> !d.id().equals(ficheLiee))
                .map(d -> new DoublonTriDto(NatureDoublon.PROSPECT, d.id(), d.code(), d.nom(), d.criteres(), null));
        List<ComProspection> autres = prospectionService.visibles(prospectionRepository.trouverMemeTelephoneOuEmail(
                p.getId(), telephone, email, PageRequest.of(0, DOUBLONS_MAX)));
        Stream<DoublonTriDto> prospections = autres.stream()
                .map(d -> new DoublonTriDto(NatureDoublon.PROSPECTION, d.getId(), d.getCode(), LignesParcours.nom(d),
                        criteres(d, telephone, email), d.getIsClosed()));
        return Stream.concat(fiches, prospections).limit(2L * DOUBLONS_MAX).toList();
    }

    private static List<CritereDoublon> criteres(ComProspection autre, String telephone, String email) {
        List<CritereDoublon> criteres = new ArrayList<>();
        if (!telephone.isEmpty() && telephone.equals(ProspectService.critereTelephone(autre.getTempTel()))) {
            criteres.add(CritereDoublon.TELEPHONE);
        }
        if (!email.isEmpty() && email.equals(ProspectService.critereEmail(autre.getTempEMail()))) {
            criteres.add(CritereDoublon.EMAIL);
        }
        return List.copyOf(criteres);
    }

    // ---------------------------------------------------------------- « Pas joignable »

    /**
     * Interaction « Téléphone Sortant / injoignable » portant une relance demain 10:00 ; les relances précédentes
     * sont soldées par {@code addInteraction} (AlerteRelanceService). La ligne reste dans la boîte.
     */
    @Transactional
    public InteractionDto marquerInjoignable(UUID prospectionId) {
        perimetreService.verifierAccesProspection(prospectionId);
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));
        portee.verifierLigne(prospection);
        if (!DiscriminantPreProspection.estPreProspection(prospection)) {
            throw new ConflictException("La ligne " + prospection.getCode()
                    + " n'est plus une pré-prospection : elle ne se trie plus dans la Boîte de tri.");
        }
        if (Boolean.TRUE.equals(prospection.getIsClosed())) {
            throw new ConflictException("La pré-prospection " + prospection.getCode() + " est archivée.");
        }
        ComActionMessageType type = typeRepository.findFirstByCode(TYPE_TELEPHONE_SORTANT)
                .orElseThrow(() -> new ConflictException(REFERENTIEL_MANQUANT.formatted(TYPE_TELEPHONE_SORTANT)));
        ComActionMessageSubCategory resultat = resultatRepository
                .findFirstByCodeAndComActionMessageCategoryId(RESULTAT_INJOIGNABLE, ContexteInteraction.CATEGORIE_PROSPECTION)
                .orElseThrow(() -> new ConflictException(REFERENTIEL_MANQUANT.formatted(RESULTAT_INJOIGNABLE)));
        LocalDateTime demain = LocalDate.now(clock).plusDays(1).atTime(HEURE_RELANCE);
        return prospectionService.addInteraction(prospectionId, new CreerInteractionRequest(
                MESSAGE_INJOIGNABLE, false, type.getId(), resultat.getId(), demain, null));
    }
}
