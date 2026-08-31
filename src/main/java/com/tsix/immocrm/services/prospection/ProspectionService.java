package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.CreerProspectionRequest;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ModifierTranchePrincipaleRequest;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
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
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProspectionService {

    /**
     * {@code ComActionMessageSubCategory.Code} values (under the "Prospection" category) that
     * require a "Date du rendez-vous" and create a calendar entry ({@link AdmEvent}) — confirmed
     * empirically against real data. Covers both actual rendez-vous ("Rendez vous pris"/"RDV
     * reporté") and planned site visits ("Le prospect veut faire une visite du projet", 5708 real
     * uses) — same underlying mechanism, distinguished on the calendar by
     * {@code AdmEvent.comActionMessageSubCategory}. See {@code GET /api/calendrier/evenements}.
     */
    private static final Set<String> RESULTATS_AVEC_DATE_PLANIFIEE = Set.of(
            "Rendez vous pris", "RDV reporté", "Le prospect veut faire une visite du projet");

    private final ComProspectionRepository prospectionRepository;
    private final ProspectService prospectService;
    private final CfgTierRepository tierRepository;
    private final ComProspectionKindRepository prospectionKindRepository;
    private final ComProspectionClosingReasonRepository closingReasonRepository;
    private final ComActionMessageRepository actionMessageRepository;
    private final ComProspectionOriginRepository prospectionOriginRepository;
    private final ComProspectionOriginSupportRepository prospectionOriginSupportRepository;
    private final StkVocationRepository vocationRepository;
    private final ComActionMessageTypeRepository actionMessageTypeRepository;
    private final ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository;
    private final AdmEventRepository admEventRepository;
    private final ComActionRepository actionRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;
    private final StrHistoryRepository historyRepository;
    private final AdmSequenceRepository sequenceRepository;
    private final PerimetreCommercialService perimetreService;

    public ProspectionService(ComProspectionRepository prospectionRepository, ProspectService prospectService,
                               CfgTierRepository tierRepository, ComProspectionKindRepository prospectionKindRepository,
                               ComProspectionClosingReasonRepository closingReasonRepository,
                               ComActionMessageRepository actionMessageRepository,
                               ComProspectionOriginRepository prospectionOriginRepository,
                               ComProspectionOriginSupportRepository prospectionOriginSupportRepository,
                               StkVocationRepository vocationRepository,
                               ComActionMessageTypeRepository actionMessageTypeRepository,
                               ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository,
                               AdmEventRepository admEventRepository,
                               ComActionRepository actionRepository,
                               CfgTrancheRepository trancheRepository,
                               StkItemRepository stkItemRepository,
                               StrHistoryRepository historyRepository,
                               AdmSequenceRepository sequenceRepository,
                               PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
        this.prospectionRepository = prospectionRepository;
        this.prospectService = prospectService;
        this.tierRepository = tierRepository;
        this.prospectionKindRepository = prospectionKindRepository;
        this.closingReasonRepository = closingReasonRepository;
        this.actionMessageRepository = actionMessageRepository;
        this.prospectionOriginRepository = prospectionOriginRepository;
        this.prospectionOriginSupportRepository = prospectionOriginSupportRepository;
        this.vocationRepository = vocationRepository;
        this.actionMessageTypeRepository = actionMessageTypeRepository;
        this.actionMessageSubCategoryRepository = actionMessageSubCategoryRepository;
        this.admEventRepository = admEventRepository;
        this.actionRepository = actionRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
        this.historyRepository = historyRepository;
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * {@code GET /api/prospections}. Excludes pré-prospections (CfgProspectId IS NULL AND
     * CfgCommercialId IS NULL) — those are exposed separately by
     * {@link #findPreProspections(List, List, List, List, LocalDateTime, LocalDateTime, String, Boolean, Pageable)}
     * to avoid the two endpoints returning overlapping rows.
     *
     * @param etapes     {@code ComProspectionKind.code} filter ("Nature") — OR-match, repeated
     *                   query param, 0/1/2+ values behave like {@code DossierService.findAll}'s
     *                   {@code statuts}. See {@code instructions/filtres-avances.md}.
     * @param origines   {@code ComProspectionOrigin.code} filter — same OR-match convention.
     * @param commerciaux {@code CfgTier.id} (commercial) filter — same OR-match convention.
     * @param trancheIds {@code CfgTranche.id} filter ({@code ComProspection.stkHierarchy} — the
     *                   tranche visée), OR-match. Unlike Dossier, this is a direct FK on
     *                   {@code ComProspection}, no EXISTS subquery needed.
     * @param dateDebut  inclusive lower bound on {@code docDate} (only date field exposed by
     *                   {@link ProspectionListItemDto} today).
     * @param dateFin    inclusive upper bound on {@code docDate}.
     * @param search     free-text match on code/prospect name/temp contact fields
     * @param ouvertes   when true, only non-closed (IsClosed = false) prospections
     */
    public Page<ProspectionListItemDto> findAll(List<String> etapes, List<String> origines, List<UUID> commerciaux,
            List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin, String search,
            Boolean ouvertes, Pageable pageable) {
        Specification<ComProspection> spec = buildSpec(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin,
                search, ouvertes)
                .and((root, query, cb) -> cb.or(
                        cb.isNotNull(root.get("cfgProspect")),
                        cb.isNotNull(root.get("cfgCommercial"))
                ))
                // Périmètre commercial — prédicat neutre si l'appelant n'est pas restreint.
                .and(perimetreService.filtreProspections());
        return prospectionRepository.findAll(spec, pageable).map(ProspectionService::toListItem);
    }

    /**
     * {@code GET /api/pre-prospections}: CfgProspectId IS NULL AND CfgCommercialId IS NULL. Same
     * filter set as {@link #findAll} — see {@code instructions/filtres-avances.md} point 5.
     */
    public Page<ProspectionListItemDto> findPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, Pageable pageable) {
        Specification<ComProspection> spec = buildSpec(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin,
                search, ouvertes)
                .and((root, query, cb) -> cb.and(
                        cb.isNull(root.get("cfgProspect")),
                        cb.isNull(root.get("cfgCommercial"))
                ))
                // Périmètre commercial : une pré-prospection a par construction un commercial NULL
                // (0 sur 2800 en base réelle), donc seul le projet peut la scoper.
                .and(perimetreService.filtreProspections());
        return prospectionRepository.findAll(spec, pageable).map(ProspectionService::toListItem);
    }

    /** Shared filter predicates for {@link #findAll} and {@link #findPreProspections}. */
    private static Specification<ComProspection> buildSpec(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (etapes != null && !etapes.isEmpty()) {
                List<String> etapesLower = etapes.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("comProspectionKind").get("code")).in(etapesLower));
            }
            if (origines != null && !origines.isEmpty()) {
                List<String> originesLower = origines.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("comProspectionOrigin").get("code")).in(originesLower));
            }
            if (commerciaux != null && !commerciaux.isEmpty()) {
                predicates.add(root.get("cfgCommercial").get("id").in(commerciaux));
            }
            if (trancheIds != null && !trancheIds.isEmpty()) {
                predicates.add(root.get("stkHierarchy").get("id").in(trancheIds));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("docDate"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("docDate"), dateFin));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("tempDescription")), pattern),
                        cb.like(cb.lower(root.get("tempTel")), pattern),
                        cb.like(cb.lower(root.get("tempEMail")), pattern)
                ));
            }
            if (ouvertes != null && ouvertes) {
                predicates.add(cb.or(cb.isFalse(root.get("isClosed")), cb.isNull(root.get("isClosed"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ProspectionDetailDto findById(UUID id) {
        return prospectionRepository.findById(id)
                .map(ProspectionService::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));
    }

    /**
     * {@code GET /api/prospections/{id}/historique}: piste d'audit complète du parcours d'un lead
     * — {@code StrHistory} filtré par {@code ObjectId = prospectionId}, même mécanisme que
     * {@code GET /api/prospects/{id}/historique} et {@code GET /api/dossiers/{id}/historique}.
     * Comble une lacune constatée: avant cet endpoint, rien ne traçait les dates de "devenu
     * prospect" ({@link #creerProspect}) ni "devenu prospection" ({@link #transformer}) — la même
     * ligne {@code ComProspection} était mutée en place sans horodatage, y compris côté Omran
     * (confirmé: {@code ComProspection.UserUpdateDateTime} reste toujours {@code NULL} en base
     * réelle, même sur des lignes déjà transformées). La date de création du pré-prospect reste
     * disponible via {@code ProspectionDetailDto.dateDocument} (pas de doublon ici, aucun
     * endpoint ne permettant de créer une {@code ComProspection} pour intercepter cet évènement).
     * Le stade "Dossier de vente" a son propre historique — voir
     * {@code GET /api/dossiers/{id}/historique}.
     */
    public Page<HistoriqueDto> historique(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return historyRepository.findByObjectIdOrderByDocDateDesc(prospectionId, pageable)
                .map(ProspectionService::toHistoriqueDto);
    }

    private static HistoriqueDto toHistoriqueDto(StrHistory h) {
        return new HistoriqueDto(
                h.getId(),
                h.getDocDate(),
                h.getDescription(),
                h.getAdmUserDescription(),
                h.getStrWorkflowDescription(),
                h.getStrStatusDescription()
        );
    }

    /**
     * Écrit une ligne {@code StrHistory} pour une transition du parcours pré-prospect → prospect
     * → prospection, consultable via {@link #historique}. Voir {@link #creerProspect} et
     * {@link #transformer}.
     */
    private void enregistrerHistorique(UUID prospectionId, String description) {
        StrHistory historique = new StrHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(LocalDateTime.now());
        historique.setObjectId(prospectionId);
        historique.setDescription(description);
        historyRepository.save(historique);
    }

    /**
     * {@code POST /api/prospections}: creates a fully-qualified prospection in one call, for a
     * prospect that already exists — see {@link CreerProspectionRequest}'s javadoc for how this
     * differs from the pré-prospection flow. {@code prospectId} is required; every other field is
     * optional and validated for existence only when provided, same convention as
     * {@link #modifierProspection}.
     */
    @Transactional
    public ProspectionDetailDto creerProspection(CreerProspectionRequest request) {
        if (request.prospectId() == null) {
            throw new InvalidRequestException("prospectId est requis");
        }
        CfgTier prospect = tierRepository.findById(request.prospectId())
                .orElseThrow(() -> new InvalidRequestException("Prospect (CfgTier) introuvable: " + request.prospectId()));

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(genererCodeProspection());
        prospection.setDocDate(LocalDateTime.now());
        prospection.setCfgProspect(prospect);

        if (request.commercialId() != null) {
            CfgTier commercial = tierRepository.findById(request.commercialId())
                    .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));
            prospection.setCfgCommercial(commercial);
        }
        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOrigin introuvable: " + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOriginSupport introuvable: " + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException("StkVocation introuvable: " + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException("ComAction introuvable: " + request.actionCommercialeId()));
            prospection.setComAction(action);
        }
        if (request.etapeId() != null) {
            ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionKind introuvable: " + request.etapeId()));
            prospection.setComProspectionKind(kind);
        }

        // trancheId/articleId: même validation croisée que modifierTranchePrincipale (l'article
        // doit appartenir à la tranche fournie) — voir sa doc pour le raisonnement.
        if (request.trancheId() != null) {
            CfgTranche tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));
            // Périmètre commercial : créer une prospection sur un projet non affecté est refusé (403).
            perimetreService.verifierEcritureSurTranche(tranche,
                    "Création de prospection sur la tranche " + tranche.getCode());
            if (request.articleId() != null) {
                StkItem article = stkItemRepository.findById(request.articleId())
                        .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
                if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
                    throw new InvalidRequestException(
                            "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
                }
                prospection.setStkItem(article);
            }
            prospection.setStkHierarchy(tranche);
        } else if (request.articleId() != null) {
            throw new InvalidRequestException("articleId nécessite trancheId");
        }

        if (request.surface() != null) prospection.setArea(request.surface());
        if (request.surfaceJusqua() != null) prospection.setAreaTo(request.surfaceJusqua());
        if (request.prix() != null) prospection.setPrice(request.prix());
        if (request.prixJusqua() != null) prospection.setPriceTo(request.prixJusqua());
        if (request.avance() != null) prospection.setAdvanceAmount(request.avance());
        if (request.financementFondsPropres() != null) prospection.setFundingOwnBase(request.financementFondsPropres());
        if (request.financementCredit() != null) prospection.setFundingCredit(request.financementCredit());
        if (request.financementAutre() != null) prospection.setFundingOther(request.financementAutre());
        if (request.ancienClient() != null) prospection.setIsOldCustomer(request.ancienClient());
        if (request.notes() != null) prospection.setNotes(request.notes());

        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(prospection.getId(), "Prospection créée : prospect " + nomTier(prospect));
        return toDetail(prospection);
    }

    /**
     * Numbers a new Prospection the same way the legacy app does — confirmed against real data:
     * {@code AdmSequence} (row {@code Code = "Prospection"}) holds the live counter
     * ({@code SeqValue = 5932} at time of writing), format
     * {@code "{année sur 2 chiffres}/{numéro sur 5 chiffres}"} (e.g. "26/05932" — real examples
     * confirmed on {@code ComProspection.Code}). Same mechanism as
     * {@code DossierService#genererCodeDossier}, but with no per-project prefix (a single global
     * counter, no "APF"/"ART"-style prefix on Prospection codes).
     */
    private String genererCodeProspection() {
        AdmSequence sequence = sequenceRepository.findByCode("Prospection").orElseGet(() -> {
            // Self-heals if the seed row is ever missing (e.g. a fresh test database) — bootstraps
            // at 0 rather than blocking Prospection creation entirely.
            AdmSequence nouvelle = new AdmSequence();
            nouvelle.setId(UUID.randomUUID());
            nouvelle.setCode("Prospection");
            nouvelle.setSeqValue(0);
            nouvelle.setSeqValueNumber(5);
            nouvelle.setSeqYearNumber(2);
            nouvelle.setBasedOnSystemYear(true);
            return nouvelle;
        });
        int prochain = (sequence.getSeqValue() != null ? sequence.getSeqValue() : 0) + 1;
        sequence.setSeqValue(prochain);
        sequenceRepository.save(sequence);

        int largeurAnnee = sequence.getSeqYearNumber() != null ? sequence.getSeqYearNumber() : 2;
        int largeurNumero = sequence.getSeqValueNumber() != null ? sequence.getSeqValueNumber() : 5;
        int annee = LocalDateTime.now().getYear() % (int) Math.pow(10, largeurAnnee);
        String anneeFormatee = String.format("%0" + largeurAnnee + "d", annee);
        String numero = String.format("%0" + largeurNumero + "d", prochain);
        return anneeFormatee + "/" + numero;
    }

    /**
     * {@code POST /api/pre-prospections/{id}/creer-prospect}: creates a {@code CfgTier} and
     * links it as the prospection's {@code CfgProspectId}. Only valid on a pré-prospection
     * (CfgProspectId IS NULL) — 400 if the prospection already has a prospect.
     */
    @Transactional
    public ProspectionDetailDto creerProspect(UUID prospectionId, CreerProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (prospection.getCfgProspect() != null) {
            throw new InvalidRequestException("Cette prospection a déjà un prospect lié: " + prospectionId);
        }

        CfgTier tier = prospectService.createEntity(request);
        prospection.setCfgProspect(tier);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(prospectionId, "Prospect créé et lié : " + nomTier(tier));
        return toDetail(prospection);
    }

    /**
     * {@code POST /api/prospections/{id}/transformer}: pré-prospection → prospection, by
     * assigning the commercial. Requires a prospect to already be linked (via
     * {@link #creerProspect}) — 400 otherwise.
     */
    @Transactional
    public ProspectionDetailDto transformer(UUID id, TransformerProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException(
                    "Un prospect doit être créé (POST /api/pre-prospections/{id}/creer-prospect) avant de transformer: " + id);
        }
        if (request.commercialId() == null) {
            throw new InvalidRequestException("commercialId est requis");
        }

        CfgTier commercial = tierRepository.findById(request.commercialId())
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));

        prospection.setCfgCommercial(commercial);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(id, "Transformée en prospection : commercial assigné, " + nomTier(commercial));
        return toDetail(prospection);
    }

    /**
     * {@code PATCH /api/prospections/{id}/etape}: changes {@code ComProspectionKindId}
     * ("Nature"). Only membership is validated — no transition table.
     */
    @Transactional
    public ProspectionDetailDto changerEtape(UUID id, ChangerEtapeRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.etapeId() == null) {
            throw new InvalidRequestException("etapeId est requis");
        }
        ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                .orElseThrow(() -> new InvalidRequestException("ComProspectionKind introuvable: " + request.etapeId()));

        prospection.setComProspectionKind(kind);
        if (request.note() != null) {
            prospection.setNotes(request.note());
        }
        return toDetail(prospectionRepository.save(prospection));
    }

    /**
     * {@code PATCH /api/prospections/{id}}: edits qualification fields (origine, vocation,
     * budget, tranche/article, notes...). Partial update — {@code null} fields are left
     * unchanged, same convention as {@link com.tsix.immocrm.services.prospect.ProspectService#update}.
     */
    @Transactional
    public ProspectionDetailDto modifierProspection(UUID id, ModifierProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOrigin introuvable: " + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOriginSupport introuvable: " + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException("StkVocation introuvable: " + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException("ComAction introuvable: " + request.actionCommercialeId()));
            prospection.setComAction(action);
        }
        if (request.surface() != null) prospection.setArea(request.surface());
        if (request.surfaceJusqua() != null) prospection.setAreaTo(request.surfaceJusqua());
        if (request.prix() != null) prospection.setPrice(request.prix());
        if (request.prixJusqua() != null) prospection.setPriceTo(request.prixJusqua());
        if (request.avance() != null) prospection.setAdvanceAmount(request.avance());
        if (request.financementFondsPropres() != null) prospection.setFundingOwnBase(request.financementFondsPropres());
        if (request.financementCredit() != null) prospection.setFundingCredit(request.financementCredit());
        if (request.financementAutre() != null) prospection.setFundingOther(request.financementAutre());
        if (request.notes() != null) prospection.setNotes(request.notes());

        return toDetail(prospectionRepository.save(prospection));
    }

    /**
     * {@code PATCH /api/prospections/{id}/tranche-principale}: remplace la tranche/l'article
     * principaux en une opération atomique et cohérente — distinct du PATCH générique
     * ({@link #modifierProspection}) et des "autres tranches" ({@code ComProspectionProject},
     * intérêts secondaires indépendants gérés par {@code ProspectionQualificationService}).
     * <p>
     * Contrairement à l'ancien comportement (tranche/article modifiables séparément et sans
     * validation croisée via le PATCH générique), {@code articleId} doit obligatoirement
     * appartenir à {@code trancheId} — confirmé empiriquement: 100% des prospections en base avec
     * un article principal ont une tranche principale qui correspond à celle de l'article
     * (aucune exception sur 807 lignes). Si {@code articleId} est omis et que l'article
     * actuellement assigné n'appartient plus à la nouvelle tranche, il est désassigné
     * automatiquement plutôt que laissé incohérent.
     */
    @Transactional
    public ProspectionDetailDto modifierTranchePrincipale(UUID id, ModifierTranchePrincipaleRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.trancheId() == null) {
            throw new InvalidRequestException("trancheId est requis");
        }
        CfgTranche tranche = trancheRepository.findById(request.trancheId())
                .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));
        // Périmètre commercial : déplacer une prospection vers un projet non affecté est refusé
        // (403) — sinon le périmètre se contournerait en sortant la ressource de son projet.
        perimetreService.verifierEcritureSurTranche(tranche,
                "Rattachement de prospection à la tranche " + tranche.getCode());

        if (request.articleId() != null) {
            StkItem article = stkItemRepository.findById(request.articleId())
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
            if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
                throw new InvalidRequestException(
                        "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
            }
            prospection.setStkItem(article);
        } else if (prospection.getStkItem() != null
                && (prospection.getStkItem().getStkHierarchy() == null
                    || !tranche.getId().equals(prospection.getStkItem().getStkHierarchy().getId()))) {
            // L'article actuellement assigné appartient à l'ancienne tranche : on le désassigne
            // plutôt que de laisser trancheId/articleId incohérents (voir doc de la méthode).
            prospection.setStkItem(null);
        }

        prospection.setStkHierarchy(tranche);
        return toDetail(prospectionRepository.save(prospection));
    }

    /** {@code PATCH /api/prospections/{id}/archiver}: closes the prospection with a reason. */
    @Transactional
    public ProspectionDetailDto archiver(UUID id, ArchiverProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.raisonId() == null && !StringUtils.hasText(request.raisonTexte())) {
            throw new InvalidRequestException("raisonId ou raisonTexte est requis");
        }

        if (request.raisonId() != null) {
            ComProspectionClosingReason reason = closingReasonRepository.findById(request.raisonId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionClosingReason introuvable: " + request.raisonId()));
            prospection.setComProspectionClosingReason(reason);
        }
        if (StringUtils.hasText(request.raisonTexte())) {
            prospection.setClosingReasonText(request.raisonTexte());
        }
        prospection.setIsClosed(true);
        prospection.setClosingDate(LocalDateTime.now());

        return toDetail(prospectionRepository.save(prospection));
    }

    /** {@code GET /api/prospections/{id}/interactions}: journal, most recent first. */
    public Page<InteractionDto> findInteractions(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return actionMessageRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId, pageable)
                .map(ProspectionService::toInteractionDto);
    }

    /** {@code POST /api/prospections/{id}/interactions}. */
    @Transactional
    public InteractionDto addInteraction(UUID prospectionId, CreerInteractionRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (!StringUtils.hasText(request.description())) {
            throw new InvalidRequestException("description est requise");
        }

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription(request.description());
        message.setIsIn(request.entrante() != null ? request.entrante() : true);
        message.setAlertValidityDate(request.dateAlerte());

        if (request.actionTypeId() != null) {
            ComActionMessageType actionType = actionMessageTypeRepository.findById(request.actionTypeId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.actionTypeId()));
            message.setComActionMessageType(actionType);
        }

        if (request.resultatId() != null) {
            ComActionMessageSubCategory resultat = actionMessageSubCategoryRepository.findById(request.resultatId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageSubCategory introuvable: " + request.resultatId()));
            message.setComActionMessageSubCategory(resultat);
            // The category is never client-supplied — always derived from the chosen résultat.
            message.setComActionMessageCategory(resultat.getComActionMessageCategory());

            if (RESULTATS_AVEC_DATE_PLANIFIEE.contains(resultat.getCode())) {
                if (request.dateRdv() == null) {
                    throw new InvalidRequestException(
                            "dateRdv est requise lorsque le résultat est \"" + resultat.getCode() + "\"");
                }
                AdmEvent evenement = new AdmEvent();
                evenement.setId(UUID.randomUUID());
                evenement.setSubject(request.description());
                evenement.setStartHour(request.dateRdv());
                evenement.setComActionMessageSubCategory(resultat);
                message.setAdmEvent(admEventRepository.save(evenement));
            }
        } else if (request.dateRdv() != null) {
            throw new InvalidRequestException("dateRdv nécessite un resultatId (\"Rendez vous pris\", "
                    + "\"RDV reporté\" ou \"Le prospect veut faire une visite du projet\")");
        }

        return toInteractionDto(actionMessageRepository.save(message));
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                m.getCfgEmployee() != null
                        ? ((m.getCfgEmployee().getFirstName() != null ? m.getCfgEmployee().getFirstName() + " " : "")
                        + (m.getCfgEmployee().getLastName() != null ? m.getCfgEmployee().getLastName() : "")).trim()
                        : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getId() : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getDescription() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getId() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getDescription() : null,
                m.getAlertValidityDate(),
                m.getAdmEvent() != null ? m.getAdmEvent().getStartHour() : null
        );
    }

    private static ProspectionListItemDto toListItem(ComProspection p) {
        return new ProspectionListItemDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getCode() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getDescription() : null
        );
    }

    private static ProspectionDetailDto toDetail(ComProspection p) {
        return new ProspectionDetailDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                p.getTempTel(),
                p.getTempEMail(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getCfgProposer() != null ? p.getCfgProposer().getId() : null,
                nomTier(p.getCfgProposer()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getId() : null,
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getDescription() : null,
                p.getComProspectionOriginSupport() != null ? p.getComProspectionOriginSupport().getId() : null,
                p.getComProspectionOriginSupport() != null ? p.getComProspectionOriginSupport().getDescription() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getId() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getDescription() : null,
                p.getStkVocation() != null ? p.getStkVocation().getId() : null,
                p.getStkVocation() != null ? p.getStkVocation().getDescription() : null,
                p.getComActionMessageType() != null ? p.getComActionMessageType().getId() : null,
                p.getComActionMessageType() != null ? p.getComActionMessageType().getDescription() : null,
                p.getComAction() != null ? p.getComAction().getId() : null,
                p.getComAction() != null ? p.getComAction().getDescription() : null,
                p.getNotes(),
                p.getArea(),
                p.getAreaTo(),
                p.getPrice(),
                p.getPriceTo(),
                p.getAdvanceAmount(),
                p.getFundingOwnBase(),
                p.getFundingCredit(),
                p.getFundingOther(),
                p.getIsClosed(),
                p.getClosingDate(),
                p.getComProspectionClosingReason() != null ? p.getComProspectionClosingReason().getId() : null,
                p.getComProspectionClosingReason() != null ? p.getComProspectionClosingReason().getDescription() : null,
                p.getClosingReasonText(),
                p.getIsOldCustomer(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getCode() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getDescription() : null,
                p.getStkItem() != null ? p.getStkItem().getId() : null,
                p.getStkItem() != null ? p.getStkItem().getCode() : null,
                p.getStkItem() != null ? p.getStkItem().getDescription() : null
        );
    }

    private static String nomTier(com.tsix.immocrm.entities.cfg.CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
    }
}
