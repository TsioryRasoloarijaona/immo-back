package com.tsix.immocrm.services.desistement;

import com.tsix.immocrm.domain.CalculDesistement;
import com.tsix.immocrm.dto.desistement.CreerDesistementRequest;
import com.tsix.immocrm.dto.desistement.DesistementDto;
import com.tsix.immocrm.dto.desistement.DesistementsPageDto;
import com.tsix.immocrm.dto.desistement.DesistementsPageDto.TotauxDesistementsDto;
import com.tsix.immocrm.dto.desistement.FiltresDesistements;
import com.tsix.immocrm.dto.desistement.ModifierDesistementRequest;
import com.tsix.immocrm.dto.desistement.SimulationDesistementDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalReason;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSaleWithdrawalCancellationRepository;
import com.tsix.immocrm.repositories.com.ComSaleWithdrawalReasonRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.AffectationsReglement;
import com.tsix.immocrm.services.dossier.LigneEcheancierOmran;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/** Désistements (FEATURE-desistements.md) : lecture au périmètre du dossier, brouillon ; la validation est à part. */
@Service
@Transactional(readOnly = true)
public class DesistementService {

    static final String INTROUVABLE = "Désistement introuvable: ";
    static final String LEGALISE = "Désistement légalisé : il n'est plus modifiable.";
    static final String DEJA_VALIDE = "Désistement déjà validé.";
    static final String NON_LEGALISE_EXISTANT = "Ce dossier a déjà un désistement non légalisé.";
    static final String MONTANTS_FIGES = "Remise et pénalité ne changent plus après la validation : les pièces sont générées.";
    private static final Sort TRI = Sort.by(Sort.Direction.DESC, "claimDate", "userCreateDateTime");

    /** Dépôts des désistements, partagés avec {@link ValidationDesistement}. */
    @Component
    public record Depots(ComSaleWithdrawalCancellationRepository desistements, ComSaleWithdrawalReasonRepository motifs,
                         ComFolderRepository dossiers, ComDocumentRepository pieces,
                         ComSettlementCategoryRepository categories, AdmParamsGuidRepository parametres,
                         StlSettlementTypeRepository types, CfgTierRepository tiers) {
    }

    private final Depots depots;
    private final MontantsDesistement montants;
    private final AffectationsReglement affectations;
    private final PerimetreCommercialService perimetre;
    private final LigneEcheancierOmran ligneEcheancier;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public DesistementService(Depots depots, MontantsDesistement montants, AffectationsReglement affectations,
                              PerimetreCommercialService perimetre, LigneEcheancierOmran ligneEcheancier,
                              UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.depots = depots;
        this.montants = montants;
        this.affectations = affectations;
        this.perimetre = perimetre;
        this.ligneEcheancier = ligneEcheancier;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public DesistementsPageDto lister(FiltresDesistements filtres, Pageable pageable) {
        Specification<ComSaleWithdrawalCancellation> spec =
                perimetre.<ComSaleWithdrawalCancellation>filtreParDossier().and(filtres(filtres));
        Page<ComSaleWithdrawalCancellation> page = depots.desistements().findAll(spec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRI));
        List<ComSaleWithdrawalCancellation> tous = depots.desistements().findAll(spec);
        TotauxDesistementsDto totaux = new TotauxDesistementsDto(
                somme(tous, ComSaleWithdrawalCancellation::getTotalSettled),
                somme(tous, ComSaleWithdrawalCancellation::getTransfertExpenseAmountAfterDiscount),
                somme(tous, ComSaleWithdrawalCancellation::getNewStkItemTotalToPay),
                somme(tous, ComSaleWithdrawalCancellation::getReimburseAmount));
        return new DesistementsPageDto(page.getContent().stream().map(this::versDto).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages(), totaux);
    }

    public DesistementDto detail(UUID id) {
        return versDto(chargerVisible(id));
    }

    /** {@code /api/dossiers/{id}/…} : l'accès au dossier est déjà contrôlé par {@code PerimetreInterceptor}. */
    public DesistementDto duDossier(UUID dossierId) {
        return depots.desistements().findFirstByComFolderIdOrderByClaimDateDescUserCreateDateTimeDesc(dossierId)
                .map(this::versDto)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun désistement pour le dossier: " + dossierId));
    }

    public SimulationDesistementDto simuler(UUID dossierId) {
        CalculDesistement.Entree e = montants.lire(dossier(dossierId), null);
        return new SimulationDesistementDto(e.prix(), e.totalRegle(), e.taux(), e.basePrix(), e.remise(),
                CalculDesistement.penaliteApresRemise(e), e.resteFrais(), CalculDesistement.aRembourser(e));
    }

    @Transactional
    public DesistementDto creer(UUID dossierId, CreerDesistementRequest request) {
        ComFolder folder = dossier(dossierId);
        LocalDateTime dateDemande = Requis.champ(request.dateDemande(), "dateDemande est requise");
        CalculDesistement.Entree e = montants.lire(folder, null);
        if (depots.desistements().existsByComFolderIdAndLegalizationDateIsNull(dossierId)) {
            throw new ConflictException(NON_LEGALISE_EXISTANT);
        }
        ComSaleWithdrawalCancellation d = new ComSaleWithdrawalCancellation();
        d.setId(UUID.randomUUID());
        d.setCode(ligneEcheancier.nouvelleQuittance());
        d.setComFolder(folder);
        d.setStkItem(folder.getStkItem());
        d.setCfgOldCustomer(folder.getCfgCustomer());
        d.setCfgContact(folder.getCfgCustomer());
        d.setClaimDate(dateDemande);
        d.setNotes(requireNonNullElse(request.observations(), ""));
        d.setMotif(request.motifId() == null ? null : motif(request.motifId()));
        d.setIsLegalized(false);
        d.setIsAccounted(false);
        MontantsDesistement.appliquer(d, e);
        signer(d);
        d.setUserCreate(d.getUserUpdate());
        d.setUserCreateDateTime(d.getUserUpdateDateTime());
        return versDto(depots.desistements().save(d));
    }

    /** R9 : brouillon = tout, montants relus ; validé = paiement, dates, motif, observations ; légalisé = rien. */
    @Transactional
    public DesistementDto modifier(UUID id, ModifierDesistementRequest r) {
        ComSaleWithdrawalCancellation d = chargerVisible(id);
        if (d.estLegalise()) {
            throw new ConflictException(LEGALISE);
        }
        boolean montantsSaisis = r.remise() != null || r.penaliteApresRemise() != null;
        if (d.estValide() && montantsSaisis) {
            throw new ConflictException(MONTANTS_FIGES);
        }
        if (!d.estValide() && r.dateLegalisation() != null) {
            throw new ConflictException("La date de légalisation d'un brouillon se pose à sa validation.");
        }
        siPresent(r.observations(), d::setNotes);
        siPresent(r.motifId(), m -> d.setMotif(motif(m)));
        siPresent(r.dateDemande(), d::setClaimDate);
        siPresent(r.dateDecision(), d::setDecisionDate);
        siPresent(r.typeReglementId(), t -> d.setStlSettlementType(type(t)));
        siPresent(r.reference(), d::setReference);
        siPresent(r.banqueId(), b -> d.setCfgBank(tiers(b, "Banque")));
        siPresent(r.contactId(), c -> d.setCfgContact(tiers(c, "Contact")));
        if (!d.estValide()) {
            recalculer(d, r);
        }
        siPresent(r.dateLegalisation(), date -> legaliser(d, date));
        signer(d);
        return versDto(d);
    }

    @Transactional
    public void supprimer(UUID id) {
        ComSaleWithdrawalCancellation d = chargerVisible(id);
        if (d.estValide()) {
            throw new ConflictException(DEJA_VALIDE);
        }
        depots.desistements().delete(d);
    }

    public List<ReferentielItemDto> motifs() {
        return depots.motifs().findAll(Sort.by("sorting", "code")).stream()
                .map(m -> new ReferentielItemDto(m.getId(), m.getCode(), m.getDescription(), m.getSorting()))
                .toList();
    }

    /** Seule lecture par id : 404 si le dossier du désistement n'est pas visible (périmètre, entité inactive). */
    ComSaleWithdrawalCancellation chargerVisible(UUID id) {
        Specification<ComSaleWithdrawalCancellation> parId = (root, query, cb) -> cb.equal(root.get("id"), id);
        return depots.desistements().findOne(perimetre.<ComSaleWithdrawalCancellation>filtreParDossier().and(parId))
                .orElseThrow(() -> new ResourceNotFoundException(INTROUVABLE + id));
    }

    void signer(ComSaleWithdrawalCancellation d) {
        d.setUserUpdate(utilisateurAffiche.courant().nom());
        d.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    static void legaliser(ComSaleWithdrawalCancellation d, LocalDateTime date) {
        d.setLegalizationDate(date);
        d.setIsLegalized(true);
    }

    DesistementDto versDto(ComSaleWithdrawalCancellation d) {
        ComFolder dossier = d.getComFolder();
        CfgTier client = d.getCfgOldCustomer();
        StkItem article = d.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        ComSaleWithdrawalReason motif = d.getMotif();
        StlSettlementType type = d.getStlSettlementType();
        return new DesistementDto(d.getId(), d.getCode(), etat(d),
                Champs.de(dossier, ComFolder::getId), Champs.de(dossier, ComFolder::getCode),
                Champs.de(client, CfgTier::getId), Champs.de(client, TierNomFormatter::nom),
                Champs.de(tranche, CfgTranche::getId), Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(article, StkItem::getId), Champs.de(article, StkItem::getCode),
                d.getClaimDate(), d.getDecisionDate(), d.getValidityDate(), d.getLegalizationDate(),
                Champs.de(motif, ComSaleWithdrawalReason::getId), Champs.de(motif, ComSaleWithdrawalReason::getDescription),
                d.getNotes(), d.getTotalToPay(), d.getTotalSettled(), d.getTotalRest(), d.getTransfertExpenseAmount(),
                d.getDiscount(), d.getTransfertExpenseAmountAfterDiscount(), d.getNewStkItemTotalToPay(),
                d.getReimburseAmount(), resteARembourser(d),
                Champs.de(type, StlSettlementType::getId), Champs.de(type, StlSettlementType::getDescription),
                d.getReference(), Champs.de(d.getCfgBank(), CfgTier::getId), Champs.de(d.getCfgBank(), TierNomFormatter::nom),
                Champs.de(d.getCfgContact(), CfgTier::getId), Champs.de(d.getCfgContact(), TierNomFormatter::nom));
    }

    /** Même plafond que le remboursement (R8) : reste dû de la pièce de résiliation, au débit. */
    BigDecimal resteARembourser(ComSaleWithdrawalCancellation d) {
        if (!d.estValide()) {
            return BigDecimal.ZERO;
        }
        return depots.pieces().findFirstByComSaleWithdrawalCancellationIdAndComSettlementCategoryIsSaleResiliationTrue(d.getId())
                .map(piece -> affectations.resteDu(piece, true))
                .orElse(BigDecimal.ZERO);
    }

    private void recalculer(ComSaleWithdrawalCancellation d, ModifierDesistementRequest r) {
        CalculDesistement.Entree e = montants.lire(d.getComFolder(), d.getDiscount());
        if (r.penaliteApresRemise() != null) {
            if (r.penaliteApresRemise().signum() < 0) {
                throw new InvalidRequestException("penaliteApresRemise doit être positive ou nulle");
            }
            e = e.avecPenaliteSaisie(r.penaliteApresRemise());
        } else if (r.remise() != null) {
            if (r.remise().signum() < 0 || r.remise().compareTo(BigDecimal.ONE) > 0) {
                throw new InvalidRequestException("remise doit être une fraction entre 0 et 1");
            }
            e = e.avecRemise(r.remise());
        } else {
            e = e.avecPenaliteStockee(d.getTransfertExpenseAmountAfterDiscount());
        }
        MontantsDesistement.appliquer(d, e);
    }

    private ComFolder dossier(UUID dossierId) {
        return depots.dossiers().findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
    }

    private ComSaleWithdrawalReason motif(UUID id) {
        return depots.motifs().findById(id).orElseThrow(() -> new InvalidRequestException("Motif introuvable: " + id));
    }

    private StlSettlementType type(UUID id) {
        return depots.types().findById(id)
                .orElseThrow(() -> new InvalidRequestException("Type de règlement introuvable: " + id));
    }

    private CfgTier tiers(UUID id, String role) {
        return depots.tiers().findById(id).orElseThrow(() -> new InvalidRequestException(role + " introuvable: " + id));
    }

    private static String etat(ComSaleWithdrawalCancellation d) {
        if (d.estLegalise()) {
            return "LEGALISE";
        }
        return d.estValide() ? "VALIDE" : "BROUILLON";
    }

    private static <T> void siPresent(T valeur, Consumer<T> ecriture) {
        if (valeur != null) {
            ecriture.accept(valeur);
        }
    }

    private static BigDecimal somme(List<ComSaleWithdrawalCancellation> lignes,
                                    Function<ComSaleWithdrawalCancellation, BigDecimal> montant) {
        return lignes.stream().map(montant).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Specification<ComSaleWithdrawalCancellation> filtres(FiltresDesistements f) {
        return (root, query, cb) -> {
            if (f == null) {
                return cb.conjunction();
            }
            List<Predicate> criteres = new ArrayList<>();
            if (StringUtils.hasText(f.q())) {
                String motif = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                Join<ComSaleWithdrawalCancellation, ComFolder> dossier = root.join("comFolder", JoinType.LEFT);
                Join<ComSaleWithdrawalCancellation, CfgTier> client = root.join("cfgOldCustomer", JoinType.LEFT);
                criteres.add(cb.or(cb.like(cb.lower(root.get("code")), motif),
                        cb.like(cb.lower(dossier.get("code")), motif),
                        cb.like(cb.lower(client.get("lastName")), motif),
                        cb.like(cb.lower(client.get("firstName")), motif),
                        cb.like(cb.lower(client.get("socialReason")), motif)));
            }
            if (f.trancheId() != null) {
                criteres.add(cb.equal(root.join("stkItem", JoinType.LEFT).get("stkHierarchy").get("id"), f.trancheId()));
            }
            entre(criteres, cb, root.get("claimDate"), f.demandeDu(), f.demandeAu());
            entre(criteres, cb, root.get("validityDate"), f.validationDu(), f.validationAu());
            entre(criteres, cb, root.get("legalizationDate"), f.legalisationDu(), f.legalisationAu());
            return cb.and(criteres.toArray(Predicate[]::new));
        };
    }

    private static void entre(List<Predicate> criteres, CriteriaBuilder cb, Path<LocalDateTime> date, LocalDate du,
                              LocalDate au) {
        if (du != null) {
            criteres.add(cb.greaterThanOrEqualTo(date, du.atStartOfDay()));
        }
        if (au != null) {
            criteres.add(cb.lessThan(date, au.plusDays(1).atStartOfDay()));
        }
    }
}
