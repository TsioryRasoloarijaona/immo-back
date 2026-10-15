package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.AffectationReglementDto;
import com.tsix.immocrm.dto.dossier.AffecterReglementRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementDossierRequest;
import com.tsix.immocrm.dto.dossier.ReglementDossierDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementAffectationRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Règlements d'un dossier répartis sur plusieurs échéances, et leur reste à affecter
 * (FEATURE-reglement-multi-echeances.md §2). Un règlement n'appartient au dossier que par ses lignes.
 */
@Service
@Transactional(readOnly = true)
public class ReglementsDossierService {

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";

    private final ComFolderRepository folderRepository;
    private final DepotsReglement depots;
    private final StlSettlementAffectationRepository affectationRepository;
    private final EcritureReglement ecriture;

    public ReglementsDossierService(ComFolderRepository folderRepository, DepotsReglement depots,
                                    StlSettlementAffectationRepository affectationRepository,
                                    EcritureReglement ecriture) {
        this.folderRepository = folderRepository;
        this.depots = depots;
        this.affectationRepository = affectationRepository;
        this.ecriture = ecriture;
    }

    @Transactional
    public ReglementDossierDto creer(UUID dossierId, CreerReglementDossierRequest request,
                                     AuthenticatedUser principal) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        EcritureReglement.ReglementEcrit ecrit =
                ecriture.creer(folder, request.entete(), request.affectations(), principal);
        return relire(dossierId, ecrit.reglement());
    }

    public List<ReglementDossierDto> lister(UUID dossierId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
        }
        // Lignes déjà triées par règlement : l'ordre d'insertion de la map est celui de la liste.
        Map<StlSettlement, List<StlSettlementLine>> parReglement = new LinkedHashMap<>();
        for (StlSettlementLine ligne : depots.lignes().findReglementsDuDossier(dossierId)) {
            parReglement.computeIfAbsent(ligne.getStlSettlement(), s -> new ArrayList<>()).add(ligne);
        }
        Map<UUID, BigDecimal> affectes = montantsAffectes(
                parReglement.keySet().stream().map(StlSettlement::getId).toList());
        return parReglement.entrySet().stream()
                .map(e -> versDto(e.getKey(), e.getValue(), affectes.get(e.getKey().getId())))
                .toList();
    }

    @Transactional
    public ReglementDossierDto affecter(UUID dossierId, UUID reglementId, AffecterReglementRequest request) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
        }
        StlSettlement reglement = depots.reglements().findById(reglementId)
                .orElseThrow(() -> new ResourceNotFoundException("Règlement introuvable: " + reglementId));
        if (depots.lignes().findBySettlementIdAndDossierId(reglementId, dossierId).isEmpty()) {
            throw new ResourceNotFoundException("Ce règlement n'est pas rattaché à ce dossier: " + reglementId);
        }
        if (!StatutReglement.compteDansLesTotaux(reglement.getStatus())) {
            throw new ConflictException("Un règlement annulé (CANCELED) ne peut plus recevoir d'affectation.");
        }
        BigDecimal resteAAffecter = MontantRestant.reglement(reglement.getAmount(),
                montantsAffectes(List.of(reglementId)).get(reglementId));
        ecriture.affecter(reglement, dossierId, request == null ? null : request.affectations(),
                depots.lignes().findEcheanceIdsBySettlementId(reglementId), resteAAffecter);
        return relire(dossierId, reglement);
    }

    /** Après écriture : lignes et part affectée relues en base (la requête vide d'abord la session). */
    private ReglementDossierDto relire(UUID dossierId, StlSettlement reglement) {
        BigDecimal affecte = montantsAffectes(List.of(reglement.getId())).get(reglement.getId());
        return versDto(reglement, depots.lignes().findBySettlementIdAndDossierId(reglement.getId(), dossierId),
                affecte);
    }

    /** Part affectée, absente de la map si le règlement n'a aucune ligne comptée. */
    private Map<UUID, BigDecimal> montantsAffectes(Collection<UUID> reglementIds) {
        Map<UUID, BigDecimal> parId = new HashMap<>();
        if (reglementIds.isEmpty()) {
            return parId;
        }
        for (Object[] ligne : affectationRepository.montantsAffectes(reglementIds)) {
            if (ligne[1] != null) {
                parId.put((UUID) ligne[0], (BigDecimal) ligne[1]);
            }
        }
        return parId;
    }

    static ReglementDossierDto versDto(StlSettlement s, List<StlSettlementLine> lignesDuDossier,
                                       BigDecimal partAffectee) {
        BigDecimal montantAffecte = requireNonNullElse(partAffectee, BigDecimal.ZERO);
        // Même exclusion que la part affectée : un règlement CANCELED n'affecte rien, ici non plus.
        BigDecimal affecteCeDossier = StatutReglement.compteDansLesTotaux(s.getStatus())
                ? lignesDuDossier.stream().map(StlSettlementLine::getAmount).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        StlSettlementType type = s.getStlSettlementType();
        CfgTier banque = s.getCfgBank();
        CfgTier payeur = PayeurReglement.de(s);
        return new ReglementDossierDto(
                s.getId(),
                s.getCode(),
                s.getDocDate(),
                s.getEcheanceDate(),
                s.getSettlementDate(),
                s.getAmount(),
                montantAffecte,
                MontantRestant.reglement(s.getAmount(), partAffectee),
                montantAffecte.subtract(affecteCeDossier),
                Champs.de(type, StlSettlementType::getId),
                Champs.de(type, StlSettlementType::getCode),
                Champs.de(type, StlSettlementType::getDescription),
                Champs.de(banque, CfgTier::getId),
                Champs.de(banque, TierNomFormatter::nom),
                Champs.de(payeur, CfgTier::getId),
                Champs.de(payeur, TierNomFormatter::nom),
                s.getNotes(),
                s.getStatus(),
                lignesDuDossier.stream()
                        .sorted(Comparator.comparing(ReglementsDossierService::dateEcheance,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(ReglementsDossierService::versAffectation)
                        .toList());
    }

    private static LocalDateTime dateEcheance(StlSettlementLine ligne) {
        return Champs.de(ligne.getComDocument(), ComDocument::getDocDate);
    }

    private static AffectationReglementDto versAffectation(StlSettlementLine ligne) {
        ComDocument echeance = ligne.getComDocument();
        return new AffectationReglementDto(
                ligne.getId(),
                Champs.de(echeance, ComDocument::getId),
                echeance == null ? null : AffectationsReglement.libelle(echeance),
                Champs.de(echeance, ComDocument::getDocDate),
                ligne.getAmount());
    }
}
