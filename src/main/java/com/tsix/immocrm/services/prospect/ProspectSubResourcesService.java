package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.dossier.AuteurStatutDossier;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierContact;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Sous-ressources de la fiche prospect : prospections, dossiers, règlements, historique, contacts. */
@Service
@Transactional(readOnly = true)
public class ProspectSubResourcesService {

    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final StrFolderStatusHistoryRepository statusHistoryRepository;
    private final CfgTierContactRepository tierContactRepository;
    private final CfgRelationTypeRepository relationTypeRepository;
    private final PerimetreCommercialService perimetreService;
    private final ProspectService prospectService;
    private final AuteurStatutDossier auteurStatut;

    public ProspectSubResourcesService(CfgTierRepository tierRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        StlSettlementRepository settlementRepository,
                                        StlSettlementLineRepository settlementLineRepository,
                                        StrFolderStatusHistoryRepository statusHistoryRepository,
                                        CfgTierContactRepository tierContactRepository,
                                        CfgRelationTypeRepository relationTypeRepository,
                                        PerimetreCommercialService perimetreService,
                                        ProspectService prospectService,
                                        AuteurStatutDossier auteurStatut) {
        this.perimetreService = perimetreService;
        this.auteurStatut = auteurStatut;
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
        this.folderRepository = folderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.tierContactRepository = tierContactRepository;
        this.relationTypeRepository = relationTypeRepository;
        this.prospectService = prospectService;
    }

    private void requireProspect(UUID id) {
        if (!tierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prospect introuvable: " + id);
        }
    }

    /**
     * Sert aussi le bloc « prospections » de la hotline. Périmètre filtré ici : PerimetreInterceptor
     * ne couvre pas ce préfixe, et la liste des prospects (non filtrée) permettrait sinon de
     * reconstituer tout le pipeline. Voir FEATURE-perimetre-commercial.md §9bis.2.
     */
    public Page<ProspectionListItemDto> prospections(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<ComProspection> spec = Specification
                .<ComProspection>where((root, query, cb) -> cb.equal(root.get("cfgProspect").get("id"), prospectId))
                .and(perimetreService.filtreProspections());
        return prospectionRepository.findAll(spec, pageable)
                .map(ProspectSubResourcesService::toProspectionListItem);
    }

    /** Sert aussi {@code GET /api/clients/{id}/dossiers} et la hotline ; périmètre : voir {@link #prospections}. */
    public Page<DossierListItemDto> dossiers(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<ComFolder> spec = Specification
                .<ComFolder>where((root, query, cb) -> cb.equal(root.get("cfgCustomer").get("id"), prospectId))
                .and(perimetreService.filtreDossiers());
        return folderRepository.findAll(spec, pageable)
                .map(ProspectSubResourcesService::toDossierListItem);
    }

    /**
     * Tous dossiers confondus ; périmètre : voir {@link #prospections}. Un {@code Specification}
     * n'a pas d'{@code OrderBy} dérivé, d'où le tri par défaut explicite.
     */
    public Page<ReglementProspectDto> reglements(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<StlSettlement> spec = Specification
                .<StlSettlement>where((root, query, cb) -> cb.equal(root.get("tier").get("id"), prospectId))
                .and(perimetreService.filtreEncaissements());
        return settlementRepository.findAll(spec, parDefautPlusRecentDabord(pageable))
                .map(this::toReglementDto);
    }

    private static Pageable parDefautPlusRecentDabord(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "docDate"));
    }

    /**
     * Changements de statut des dossiers du tiers ({@code StrFolderStatusHistory.CfgTierId}), pas
     * {@code StrHistory} dont aucune ligne ne vise un tiers. Un dossier hors périmètre masque ses
     * lignes, comme {@link #dossiers}. Voir FIX-tracabilite-historique-hotline.md §4.
     */
    public Page<HistoriqueDto> historique(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<ComFolder> dossierVisible = perimetreService.filtreDossiers();
        Specification<StrFolderStatusHistory> spec = (root, query, cb) -> {
            Subquery<UUID> visible = query.subquery(UUID.class);
            Root<ComFolder> dossier = visible.from(ComFolder.class);
            visible.select(dossier.get("id")).where(cb.and(
                    cb.equal(dossier.get("id"), root.get("comFolder").get("id")),
                    dossierVisible.toPredicate(dossier, query, cb)));
            return cb.and(
                    cb.equal(root.get("cfgTier").get("id"), prospectId),
                    cb.or(cb.isNull(root.get("comFolder")), cb.exists(visible)));
        };
        // Tri imposé : un tri client sur un champ du DTO n'existe pas sur l'entité.
        Pageable plusRecentDabord = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "docDate").and(Sort.by("id")));
        Page<StrFolderStatusHistory> lignes = statusHistoryRepository.findAll(spec, plusRecentDabord);
        Function<StrFolderStatusHistory, String> auteur = auteurStatut.resoudre(lignes.getContent());
        return lignes.map(h -> toHistoriqueDto(h, auteur.apply(h)));
    }

    public List<ContactProspectDto> contacts(UUID prospectId) {
        requireProspect(prospectId);
        return tierContactRepository.findByCfgTierIdOrderByUserCreateDateTimeDesc(prospectId).stream()
                .map(ProspectSubResourcesService::toContactDto).toList();
    }

    /**
     * Contact existant ({@code contactProspectId}) ou créé à la volée ({@code nouveauContact}),
     * exactement l'un des deux — comme OMRAN, qui insère le {@code CfgTier} puis le lien.
     */
    @Transactional
    public ContactProspectDto ajouterContact(UUID prospectId, CreerContactProspectRequest request) {
        return creerContact(prospectId, request);
    }

    private ContactProspectDto creerContact(UUID prospectId, CreerContactProspectRequest request) {
        CfgTier prospect = tierRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + prospectId));
        if (request.relationTypeId() == null) {
            throw new InvalidRequestException("relationTypeId est requis");
        }
        boolean lienExistant = request.contactProspectId() != null;
        boolean creation = request.nouveauContact() != null;
        if (lienExistant == creation) {
            throw new InvalidRequestException(
                    "Fournir soit contactProspectId (tiers existant), soit nouveauContact (à créer), mais pas les deux.");
        }
        CfgRelationType relationType = relationTypeRepository.findById(request.relationTypeId())
                .orElseThrow(() -> new InvalidRequestException("CfgRelationType introuvable: " + request.relationTypeId()));

        CfgTier contact = lienExistant
                ? tierRepository.findById(request.contactProspectId())
                        .orElseThrow(() -> new InvalidRequestException(
                                "Contact (CfgTier) introuvable: " + request.contactProspectId()))
                // Même chemin que POST /api/prospects : code séquentiel + drapeaux hérités posés.
                : prospectService.createEntity(request.nouveauContact());

        CfgTierContact link = new CfgTierContact();
        link.setId(UUID.randomUUID());
        link.setCfgTier(prospect);
        link.setCfgContact(contact);
        link.setCfgRelationType(relationType);
        return toContactDto(tierContactRepository.save(link));
    }

    /** Une pré-prospection (sans prospect) rend une liste vide, jamais 400. */
    public List<ContactProspectDto> contactsDeProspection(UUID prospectionId) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));
        if (prospection.getCfgProspect() == null) {
            return List.of();
        }
        return contacts(prospection.getCfgProspect().getId());
    }

    @Transactional
    public ContactProspectDto ajouterContactAProspection(UUID prospectionId, CreerContactProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));
        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException(
                    "Cette prospection n'a pas de prospect : créer d'abord le prospect "
                    + "(POST /api/pre-prospections/{id}/creer-prospect) avant d'ajouter un contact.");
        }
        return creerContact(prospection.getCfgProspect().getId(), request);
    }

    @Transactional
    public void retirerContact(UUID prospectId, UUID contactId) {
        requireProspect(prospectId);
        tierContactRepository.deleteByIdAndCfgTierId(contactId, prospectId);
    }

    private ReglementProspectDto toReglementDto(StlSettlement s) {
        UUID dossierId = null;
        String dossierCode = null;
        List<Object[]> rows = settlementLineRepository.findDossierBySettlementId(s.getId());
        if (!rows.isEmpty()) {
            dossierId = (UUID) rows.get(0)[0];
            dossierCode = (String) rows.get(0)[1];
        }
        return new ReglementProspectDto(
                s.getId(),
                s.getCode(),
                s.getDocDate(),
                s.getSettlementDate(),
                s.getAmount(),
                s.getStlSettlementType() != null ? s.getStlSettlementType().getCode() : null,
                dossierId,
                dossierCode
        );
    }

    private static HistoriqueDto toHistoriqueDto(StrFolderStatusHistory h, String auteur) {
        ComFolder dossier = h.getComFolder();
        StrStatus statut = h.getStrFolderStatus();
        return new HistoriqueDto(
                h.getId(),
                h.getDocDate(),
                h.getDescription(),
                auteur,
                null,
                libelleStatut(statut),
                Champs.de(dossier, ComFolder::getId),
                Champs.de(dossier, ComFolder::getCode),
                Champs.de(dossier, ComFolder::getStkItem, StkItem::getCode)
        );
    }

    private static String libelleStatut(StrStatus statut) {
        if (statut == null) {
            return null;
        }
        return StringUtils.hasText(statut.getDescription()) ? statut.getDescription() : statut.getCode();
    }

    private static ContactProspectDto toContactDto(CfgTierContact link) {
        CfgTier contact = link.getCfgContact();
        return new ContactProspectDto(
                link.getId(),
                contact != null ? contact.getId() : null,
                TierNomFormatter.nom(contact),
                contact != null ? contact.getMobile() : null,
                contact != null ? contact.getPhone() : null,
                contact != null ? contact.getEMail() : null,
                link.getCfgRelationType() != null ? link.getCfgRelationType().getId() : null,
                link.getCfgRelationType() != null ? link.getCfgRelationType().getDescription() : null
        );
    }

    // Champs d'alerte nuls ici : seules les listes de prospections les chargent (requête groupée).
    private static ProspectionListItemDto toProspectionListItem(ComProspection p) {
        return ProspectionService.toListItem(p);
    }

    // Set.of() : aucune colonne optionnelle, pour ne pas payer les jointures du contexte patrimonial.
    private static DossierListItemDto toDossierListItem(ComFolder f) {
        return DossierService.toListItem(f, Set.of());
    }
}
